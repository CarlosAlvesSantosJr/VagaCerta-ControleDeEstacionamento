package controle_estacionamento.cobranca.service;

import controle_estacionamento.cobranca.dto.AssinaturaDto;
import controle_estacionamento.cobranca.dto.AssinaturaRequest;
import controle_estacionamento.cobranca.entity.Assinatura;
import controle_estacionamento.cobranca.entity.Plano;
import controle_estacionamento.cobranca.entity.TipoPlano;
import controle_estacionamento.cobranca.exception.RecursoNaoEncontradoException;
import controle_estacionamento.cobranca.exception.RegraNegocioException;
import controle_estacionamento.cobranca.repository.AssinaturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AssinaturaServiceTest {

    // 02/11/2026 10:00 em São Paulo
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-02T13:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private AssinaturaRepository repository;
    private PlanoService planoService;
    private AssinaturaService service;

    @BeforeEach
    void setUp() {
        repository = mock(AssinaturaRepository.class);
        planoService = mock(PlanoService.class);
        when(repository.save(any(Assinatura.class))).thenAnswer(inv -> inv.getArgument(0));
        for (TipoPlano tipo : TipoPlano.values()) {
            when(planoService.buscar(tipo)).thenReturn(new Plano(tipo, tipo.getValorPadrao()));
        }
        service = new AssinaturaService(repository, planoService, clock);
    }

    @Test
    void planosComecamHojeETerminamNaVesperaDoMesmoDiaNoPeriodo() {
        when(repository.findFirstByPlacaAndCanceladaFalseOrderByFimDesc("ABC1D23")).thenReturn(Optional.empty());

        assertThat(contratar(TipoPlano.MENSAL).fim()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(contratar(TipoPlano.TRIMESTRAL).fim()).isEqualTo(LocalDate.of(2027, 2, 1));
        assertThat(contratar(TipoPlano.SEMESTRAL).fim()).isEqualTo(LocalDate.of(2027, 5, 1));
        AssinaturaDto anual = contratar(TipoPlano.ANUAL);
        assertThat(anual.inicio()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(anual.fim()).isEqualTo(LocalDate.of(2027, 11, 1));
        assertThat(anual.valorPago()).isEqualByComparingTo("2400.00");
        assertThat(anual.vigente()).isTrue();
    }

    @Test
    void renovacaoComPlanoAindaVigenteComecaNoDiaSeguinteAoFim() {
        Assinatura atual = new Assinatura();
        atual.setFim(LocalDate.of(2026, 11, 20));
        when(repository.findFirstByPlacaAndCanceladaFalseOrderByFimDesc("ABC1D23")).thenReturn(Optional.of(atual));

        AssinaturaDto renovada = contratar(TipoPlano.MENSAL);

        assertThat(renovada.inicio()).isEqualTo(LocalDate.of(2026, 11, 21));
        assertThat(renovada.fim()).isEqualTo(LocalDate.of(2026, 12, 20));
    }

    @Test
    void planoVencidoNaoEmendaNovaAssinatura() {
        Assinatura vencida = new Assinatura();
        vencida.setFim(LocalDate.of(2026, 10, 1));
        when(repository.findFirstByPlacaAndCanceladaFalseOrderByFimDesc("ABC1D23")).thenReturn(Optional.of(vencida));

        assertThat(contratar(TipoPlano.MENSAL).inicio()).isEqualTo(LocalDate.of(2026, 11, 2));
    }

    @Test
    void guardaPrecoDoPlanoNoMomentoDaContratacao() {
        when(repository.findFirstByPlacaAndCanceladaFalseOrderByFimDesc("ABC1D23")).thenReturn(Optional.empty());
        when(planoService.buscar(TipoPlano.MENSAL)).thenReturn(new Plano(TipoPlano.MENSAL, new BigDecimal("199.90")));

        AssinaturaDto mensal = contratar(TipoPlano.MENSAL);

        assertThat(mensal.valorPago()).isEqualByComparingTo("199.90");
        assertThat(mensal.placa()).isEqualTo("ABC1D23");
        assertThat(mensal.nomeCliente()).isEqualTo("Maria Souza");
    }

    @Test
    void cancelarMarcaAssinaturaComoNaoVigente() {
        Assinatura ativa = assinatura(10L, false);
        when(repository.findById(10L)).thenReturn(Optional.of(ativa));

        AssinaturaDto cancelada = service.cancelar(10L);

        assertThat(cancelada.cancelada()).isTrue();
        assertThat(cancelada.vigente()).isFalse();
        verify(repository).save(ativa);
    }

    @Test
    void naoCancelaDuasVezes() {
        when(repository.findById(10L)).thenReturn(Optional.of(assinatura(10L, true)));

        assertThatThrownBy(() -> service.cancelar(10L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("já está cancelada");
        verify(repository, never()).save(any());
    }

    @Test
    void cancelarAssinaturaInexistenteLancaNaoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelar(99L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void listarSemPlacaTrazTodasEComPlacaFiltraNormalizada() {
        when(repository.findAllByOrderByContratadaEmDesc()).thenReturn(List.of(assinatura(1L, false), assinatura(2L, false)));
        when(repository.findByPlacaOrderByInicioDesc("ABC1D23")).thenReturn(List.of(assinatura(1L, false)));

        assertThat(service.listar(null)).hasSize(2);
        assertThat(service.listar("  ")).hasSize(2);
        assertThat(service.listar("abc-1d23")).extracting(AssinaturaDto::id).containsExactly(1L);
    }

    @Test
    void buscarVigenteHojeSemPlanoLancaNaoEncontrado() {
        LocalDate hoje = LocalDate.of(2026, 11, 2);
        when(repository.findFirstByPlacaAndCanceladaFalseAndInicioLessThanEqualAndFimGreaterThanEqual("ABC1D23", hoje, hoje))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarVigenteHoje("abc-1d23"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private Assinatura assinatura(Long id, boolean cancelada) {
        Assinatura assinatura = new Assinatura();
        assinatura.setId(id);
        assinatura.setPlaca("ABC1D23");
        assinatura.setTipoPlano(TipoPlano.MENSAL);
        assinatura.setInicio(LocalDate.of(2026, 11, 1));
        assinatura.setFim(LocalDate.of(2026, 11, 30));
        assinatura.setCancelada(cancelada);
        return assinatura;
    }

    private AssinaturaDto contratar(TipoPlano tipo) {
        return service.contratar(new AssinaturaRequest("abc-1d23", "Maria Souza", tipo));
    }
}
