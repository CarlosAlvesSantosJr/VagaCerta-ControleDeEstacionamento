package controle_estacionamento.veiculo.service;

import controle_estacionamento.veiculo.dto.VeiculoDto;
import controle_estacionamento.veiculo.entity.Veiculo;
import controle_estacionamento.veiculo.exception.RecursoNaoEncontradoException;
import controle_estacionamento.veiculo.exception.RegraNegocioException;
import controle_estacionamento.veiculo.repository.VeiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VeiculoServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-02T13:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private VeiculoRepository repository;
    private VeiculoService service;

    @BeforeEach
    void setUp() {
        repository = mock(VeiculoRepository.class);
        when(repository.save(any(Veiculo.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new VeiculoService(repository, clock);
    }

    @Test
    void cadastraNovoVeiculoNormalizandoPlacaEWhatsapp() {
        when(repository.findByPlaca("ABC1D23")).thenReturn(Optional.empty());

        VeiculoDto salvo = service.salvar(dto("abc-1d23", "(11) 98765-4321"));

        assertThat(salvo.getPlaca()).isEqualTo("ABC1D23");
        assertThat(salvo.getWhatsappCondutor()).isEqualTo("11987654321");
        assertThat(salvo.getCadastradoEm()).isEqualTo(LocalDateTime.of(2026, 11, 2, 10, 0));
    }

    @Test
    void placaExistenteAtualizaDadosDoCondutor() {
        Veiculo existente = new Veiculo();
        existente.setId(7L);
        existente.setPlaca("ABC1D23");
        existente.setWhatsappCondutor("11900000000");
        when(repository.findByPlaca("ABC1D23")).thenReturn(Optional.of(existente));

        VeiculoDto salvo = service.salvar(dto("ABC1D23", "21 99999-8888"));

        assertThat(salvo.getId()).isEqualTo(7L);
        assertThat(salvo.getWhatsappCondutor()).isEqualTo("21999998888");
    }

    @Test
    void rejeitaWhatsappSemDdd() {
        when(repository.findByPlaca(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.salvar(dto("ABC1234", "98765-4321")))
                .isInstanceOf(RegraNegocioException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void aceitaWhatsappComDdi55() {
        when(repository.findByPlaca(any())).thenReturn(Optional.empty());

        VeiculoDto salvo = service.salvar(dto("ABC1234", "+55 (11) 98765-4321"));

        assertThat(salvo.getWhatsappCondutor()).isEqualTo("5511987654321");
    }

    @Test
    void removeEspacosDoModeloEDaCor() {
        when(repository.findByPlaca(any())).thenReturn(Optional.empty());
        VeiculoDto entrada = dto("ABC1234", "11987654321");
        entrada.setModelo("  Onix  ");
        entrada.setCor(" Prata ");

        VeiculoDto salvo = service.salvar(entrada);

        assertThat(salvo.getModelo()).isEqualTo("Onix");
        assertThat(salvo.getCor()).isEqualTo("Prata");
    }

    @Test
    void alterarRecusaPlacaDeOutroVeiculo() {
        when(repository.findById(1L)).thenReturn(Optional.of(veiculo(1L, "AAA1111")));
        when(repository.findByPlaca("BBB2222")).thenReturn(Optional.of(veiculo(2L, "BBB2222")));

        assertThatThrownBy(() -> service.alterar(1L, dto("BBB-2222", "11987654321")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("BBB2222");
        verify(repository, never()).save(any());
    }

    @Test
    void alterarPermiteManterAPropriaPlaca() {
        Veiculo atual = veiculo(1L, "AAA1111");
        when(repository.findById(1L)).thenReturn(Optional.of(atual));
        when(repository.findByPlaca("AAA1111")).thenReturn(Optional.of(atual));

        VeiculoDto alterado = service.alterar(1L, dto("aaa-1111", "21999998888"));

        assertThat(alterado.getPlaca()).isEqualTo("AAA1111");
        assertThat(alterado.getWhatsappCondutor()).isEqualTo("21999998888");
    }

    @Test
    void alterarVeiculoInexistenteLancaNaoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.alterar(99L, dto("ABC1234", "11987654321")))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void buscaPorPlacaNormalizaAntesDeConsultar() {
        when(repository.findByPlaca("ABC1D23")).thenReturn(Optional.of(veiculo(3L, "ABC1D23")));

        assertThat(service.buscarPorPlaca(" abc-1d23 ").getId()).isEqualTo(3L);
    }

    @Test
    void buscaPorPlacaInexistenteLancaNaoEncontrado() {
        when(repository.findByPlaca("ZZZ9999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorPlaca("ZZZ9999"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("ZZZ9999");
    }

    @Test
    void excluirRemoveVeiculoEncontrado() {
        Veiculo existente = veiculo(4L, "ABC1234");
        when(repository.findById(4L)).thenReturn(Optional.of(existente));

        service.excluir(4L);

        verify(repository).delete(existente);
    }

    @Test
    void excluirInexistenteNaoApagaNada() {
        when(repository.findById(4L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.excluir(4L)).isInstanceOf(RecursoNaoEncontradoException.class);
        verify(repository, never()).delete(any());
    }

    private Veiculo veiculo(Long id, String placa) {
        Veiculo veiculo = new Veiculo();
        veiculo.setId(id);
        veiculo.setPlaca(placa);
        return veiculo;
    }

    private VeiculoDto dto(String placa, String whatsapp) {
        VeiculoDto dto = new VeiculoDto();
        dto.setPlaca(placa);
        dto.setModelo("Onix");
        dto.setCor("Prata");
        dto.setWhatsappCondutor(whatsapp);
        return dto;
    }
}
