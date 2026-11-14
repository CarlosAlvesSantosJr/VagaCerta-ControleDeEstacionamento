package controle_estacionamento.cobranca.service;

import controle_estacionamento.cobranca.config.TarifaAvulsaProperties;
import controle_estacionamento.cobranca.dto.CalculoDto;
import controle_estacionamento.cobranca.dto.CalculoRequest;
import controle_estacionamento.cobranca.dto.PlanoDto;
import controle_estacionamento.cobranca.dto.TarifaDto;
import controle_estacionamento.cobranca.entity.Assinatura;
import controle_estacionamento.cobranca.entity.TipoPlano;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CobrancaServiceTest {

    private final TarifaAvulsaProperties tarifa = new TarifaAvulsaProperties(new BigDecimal("3.00"), 15);
    private final LocalDateTime entrada = LocalDateTime.of(2026, 11, 2, 8, 0);
    private final LocalDateTime saida = entrada.plusMinutes(50);
    private AssinaturaService assinaturaService;
    private CobrancaService service;

    @BeforeEach
    void setUp() {
        assinaturaService = mock(AssinaturaService.class);
        service = new CobrancaService(new CalculadoraTarifaAvulsa(tarifa), assinaturaService, mock(PlanoService.class), tarifa);
    }

    @Test
    void clienteEsporadicoPagaPorFracao() {
        when(assinaturaService.buscarVigente("ABC1D23", saida.toLocalDate())).thenReturn(Optional.empty());

        CalculoDto calculo = service.calcular(new CalculoRequest("ABC1D23", entrada, saida));

        assertThat(calculo.tipoCobranca()).isEqualTo(CalculoDto.AVULSO);
        assertThat(calculo.fracoes()).isEqualTo(4);
        assertThat(calculo.valorTotal()).isEqualByComparingTo("12.00");
    }

    @Test
    void placaComPlanoVigenteNaoPaga() {
        Assinatura assinatura = new Assinatura();
        assinatura.setTipoPlano(TipoPlano.TRIMESTRAL);
        assinatura.setFim(LocalDate.of(2027, 1, 31));
        when(assinaturaService.buscarVigente("ABC1D23", saida.toLocalDate())).thenReturn(Optional.of(assinatura));

        CalculoDto calculo = service.calcular(new CalculoRequest("ABC1D23", entrada, saida));

        assertThat(calculo.tipoCobranca()).isEqualTo(CalculoDto.PLANO);
        assertThat(calculo.valorTotal()).isEqualByComparingTo("0.00");
        assertThat(calculo.plano()).isEqualTo(TipoPlano.TRIMESTRAL);
        assertThat(calculo.minutosPermanencia()).isEqualTo(50);
        assertThat(calculo.planoValidoAte()).isEqualTo(LocalDate.of(2027, 1, 31));
    }

    @Test
    void vigenciaEConsultadaNaDataDaSaida() {
        LocalDateTime entradaNaVespera = LocalDateTime.of(2026, 11, 1, 23, 50);
        LocalDateTime saidaNoDiaSeguinte = LocalDateTime.of(2026, 11, 2, 0, 20);
        when(assinaturaService.buscarVigente("ABC1D23", LocalDate.of(2026, 11, 2))).thenReturn(Optional.empty());

        CalculoDto calculo = service.calcular(new CalculoRequest("ABC1D23", entradaNaVespera, saidaNoDiaSeguinte));

        assertThat(calculo.fracoes()).isEqualTo(2);
        verify(assinaturaService).buscarVigente("ABC1D23", LocalDate.of(2026, 11, 2));
    }

    @Test
    void tarifaReuneValorAvulsoEPlanos() {
        PlanoService planoService = mock(PlanoService.class);
        List<PlanoDto> planos = List.of(new PlanoDto(TipoPlano.MENSAL, "Mensal", 1, new BigDecimal("250.00")));
        when(planoService.listarTodos()).thenReturn(planos);
        CobrancaService comPlanos = new CobrancaService(new CalculadoraTarifaAvulsa(tarifa), assinaturaService, planoService, tarifa);

        TarifaDto tabela = comPlanos.tarifa();

        assertThat(tabela.valorFracao()).isEqualByComparingTo("3.00");
        assertThat(tabela.minutosFracao()).isEqualTo(15);
        assertThat(tabela.planos()).isEqualTo(planos);
    }
}
