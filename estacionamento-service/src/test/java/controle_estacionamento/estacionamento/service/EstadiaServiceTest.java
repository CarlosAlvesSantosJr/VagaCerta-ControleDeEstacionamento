package controle_estacionamento.estacionamento.service;

import controle_estacionamento.estacionamento.client.CalculoResponse;
import controle_estacionamento.estacionamento.client.CobrancaClient;
import controle_estacionamento.estacionamento.client.VeiculoClient;
import controle_estacionamento.estacionamento.client.VeiculoResponse;
import controle_estacionamento.estacionamento.dto.EntradaRequest;
import controle_estacionamento.estacionamento.dto.EstadiaDto;
import controle_estacionamento.estacionamento.dto.ResumoDto;
import controle_estacionamento.estacionamento.entity.Estadia;
import controle_estacionamento.estacionamento.entity.StatusEstadia;
import controle_estacionamento.estacionamento.exception.ConflitoException;
import controle_estacionamento.estacionamento.exception.RecursoNaoEncontradoException;
import controle_estacionamento.estacionamento.exception.ServicoIndisponivelException;
import controle_estacionamento.estacionamento.repository.EstadiaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EstadiaServiceTest {

    // 02/11/2026 10:00:00 em São Paulo
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-02T13:00:00.750Z"), ZoneId.of("America/Sao_Paulo"));
    private final LocalDateTime agora = LocalDateTime.of(2026, 11, 2, 10, 0, 0);

    private EstadiaRepository repository;
    private VeiculoClient veiculoClient;
    private CobrancaClient cobrancaClient;
    private EstadiaService service;

    @BeforeEach
    void setUp() {
        repository = mock(EstadiaRepository.class);
        veiculoClient = mock(VeiculoClient.class);
        cobrancaClient = mock(CobrancaClient.class);
        when(repository.save(any(Estadia.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new EstadiaService(repository, veiculoClient, cobrancaClient, clock);
    }

    @Test
    void entradaUsaHorarioDoSistema() {
        EntradaRequest request = new EntradaRequest("abc-1d23", "Onix", "Prata", "11987654321");
        when(repository.existsByPlacaAndStatus("ABC1D23", StatusEstadia.ABERTA)).thenReturn(false);
        when(veiculoClient.registrar(request))
                .thenReturn(new VeiculoResponse(5L, "ABC1D23", "Onix", "Prata", "11987654321"));

        EstadiaDto estadia = service.registrarEntrada(request);

        assertThat(estadia.entrada()).isEqualTo(agora);
        assertThat(estadia.status()).isEqualTo(StatusEstadia.ABERTA);
        assertThat(estadia.veiculoId()).isEqualTo(5L);
    }

    @Test
    void naoPermiteDuasEntradasDaMesmaPlaca() {
        when(repository.existsByPlacaAndStatus("ABC1D23", StatusEstadia.ABERTA)).thenReturn(true);

        assertThatThrownBy(() -> service.registrarEntrada(new EntradaRequest("ABC1D23", "Onix", "Prata", "11987654321")))
                .isInstanceOf(ConflitoException.class);
        verifyNoInteractions(veiculoClient);
    }

    @Test
    void saidaFechaEstadiaComValorDoCobrancaService() {
        Estadia aberta = estadiaAberta(agora.minusMinutes(40));
        when(repository.findById(1L)).thenReturn(Optional.of(aberta));
        when(cobrancaClient.calcular("ABC1D23", aberta.getEntrada(), agora))
                .thenReturn(new CalculoResponse("AVULSO", 40, 3, new BigDecimal("3.00"), new BigDecimal("9.00"), null, null));

        EstadiaDto fechada = service.registrarSaida(1L);

        assertThat(fechada.status()).isEqualTo(StatusEstadia.FINALIZADA);
        assertThat(fechada.saida()).isEqualTo(agora);
        assertThat(fechada.valorTotal()).isEqualByComparingTo("9.00");
    }

    @Test
    void previaNaoFechaEstadia() {
        Estadia aberta = estadiaAberta(agora.minusMinutes(10));
        when(repository.findById(1L)).thenReturn(Optional.of(aberta));
        when(cobrancaClient.calcular(any(), any(), any()))
                .thenReturn(new CalculoResponse("PLANO", 10, 1, new BigDecimal("3.00"), new BigDecimal("0.00"), "MENSAL", null));

        EstadiaDto previa = service.previaSaida(1L);

        assertThat(previa.status()).isEqualTo(StatusEstadia.ABERTA);
        assertThat(previa.plano()).isEqualTo("MENSAL");
        verify(repository, never()).save(any());
    }

    @Test
    void naoRegistraSaidaDuasVezes() {
        Estadia fechada = estadiaAberta(agora.minusHours(1));
        fechada.setStatus(StatusEstadia.FINALIZADA);
        when(repository.findById(1L)).thenReturn(Optional.of(fechada));

        assertThatThrownBy(() -> service.registrarSaida(1L)).isInstanceOf(ConflitoException.class);
    }

    @Test
    void entradaGuardaCopiaDosDadosDoVeiculo() {
        EntradaRequest request = new EntradaRequest("abc1234", "gol", "branco", "(21) 99999-8888");
        when(repository.existsByPlacaAndStatus("ABC1234", StatusEstadia.ABERTA)).thenReturn(false);
        when(veiculoClient.registrar(request))
                .thenReturn(new VeiculoResponse(9L, "ABC1234", "Gol", "Branco", "21999998888"));

        EstadiaDto estadia = service.registrarEntrada(request);

        assertThat(estadia.placa()).isEqualTo("ABC1234");
        assertThat(estadia.modelo()).isEqualTo("Gol");
        assertThat(estadia.cor()).isEqualTo("Branco");
        assertThat(estadia.whatsappCondutor()).isEqualTo("21999998888");
        assertThat(estadia.saida()).isNull();
        assertThat(estadia.valorTotal()).isNull();
    }

    @Test
    void falhaNoVeiculoServiceNaoAbreEstadia() {
        EntradaRequest request = new EntradaRequest("ABC1234", "Gol", "Branco", "21999998888");
        when(repository.existsByPlacaAndStatus("ABC1234", StatusEstadia.ABERTA)).thenReturn(false);
        when(veiculoClient.registrar(request))
                .thenThrow(new ServicoIndisponivelException("Serviço de veículos indisponível. Tente novamente.", null));

        assertThatThrownBy(() -> service.registrarEntrada(request)).isInstanceOf(ServicoIndisponivelException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void falhaNoCobrancaServiceMantemEstadiaAberta() {
        Estadia aberta = estadiaAberta(agora.minusMinutes(30));
        when(repository.findById(1L)).thenReturn(Optional.of(aberta));
        when(cobrancaClient.calcular(any(), any(), any()))
                .thenThrow(new ServicoIndisponivelException("Serviço de cobrança indisponível. Tente novamente.", null));

        assertThatThrownBy(() -> service.registrarSaida(1L)).isInstanceOf(ServicoIndisponivelException.class);
        assertThat(aberta.getStatus()).isEqualTo(StatusEstadia.ABERTA);
        verify(repository, never()).save(any());
    }

    @Test
    void saidaComPlanoRegistraValorZeroEPlano() {
        Estadia aberta = estadiaAberta(agora.minusHours(2));
        when(repository.findById(1L)).thenReturn(Optional.of(aberta));
        when(cobrancaClient.calcular("ABC1D23", aberta.getEntrada(), agora))
                .thenReturn(new CalculoResponse("PLANO", 120, 8, new BigDecimal("3.00"), new BigDecimal("0.00"), "ANUAL", null));

        EstadiaDto fechada = service.registrarSaida(1L);

        assertThat(fechada.tipoCobranca()).isEqualTo("PLANO");
        assertThat(fechada.plano()).isEqualTo("ANUAL");
        assertThat(fechada.minutosPermanencia()).isEqualTo(120);
        assertThat(fechada.fracoes()).isEqualTo(8);
        assertThat(fechada.valorTotal()).isEqualByComparingTo("0.00");
    }

    @Test
    void ticketInexistenteLancaNaoEncontrado() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(42L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("42");
        assertThatThrownBy(() -> service.registrarSaida(42L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void listarAbertasOuUltimasFinalizadas() {
        when(repository.findByStatusOrderByEntradaAsc(StatusEstadia.ABERTA)).thenReturn(List.of(estadiaAberta(agora)));
        when(repository.findTop50ByStatusOrderBySaidaDesc(StatusEstadia.FINALIZADA)).thenReturn(List.of());

        assertThat(service.listar(StatusEstadia.ABERTA)).hasSize(1);
        assertThat(service.listar(StatusEstadia.FINALIZADA)).isEmpty();
    }

    @Test
    void resumoDoDiaContaAPartirDaMeiaNoite() {
        LocalDateTime meiaNoite = LocalDateTime.of(2026, 11, 2, 0, 0);
        when(repository.countByStatus(StatusEstadia.ABERTA)).thenReturn(3L);
        when(repository.countByEntradaGreaterThanEqual(meiaNoite)).thenReturn(12L);
        when(repository.somarFaturamentoDesde(meiaNoite)).thenReturn(new BigDecimal("87.00"));

        ResumoDto resumo = service.resumoDoDia();

        assertThat(resumo.veiculosNoPatio()).isEqualTo(3);
        assertThat(resumo.entradasHoje()).isEqualTo(12);
        assertThat(resumo.faturamentoHoje()).isEqualByComparingTo("87.00");
    }

    @Test
    void normalizaPlaca() {
        assertThat(EstadiaService.normalizarPlaca(" abc-1d23 ")).isEqualTo("ABC1D23");
    }

    private Estadia estadiaAberta(LocalDateTime entrada) {
        Estadia estadia = new Estadia();
        estadia.setId(1L);
        estadia.setPlaca("ABC1D23");
        estadia.setEntrada(entrada);
        estadia.setStatus(StatusEstadia.ABERTA);
        return estadia;
    }
}
