package controle_estacionamento.veiculo.service;

import controle_estacionamento.veiculo.dto.VeiculoDto;
import controle_estacionamento.veiculo.entity.Veiculo;
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

    private VeiculoDto dto(String placa, String whatsapp) {
        VeiculoDto dto = new VeiculoDto();
        dto.setPlaca(placa);
        dto.setModelo("Onix");
        dto.setCor("Prata");
        dto.setWhatsappCondutor(whatsapp);
        return dto;
    }
}
