package controle_estacionamento.cobranca.service;

import controle_estacionamento.cobranca.config.TarifaAvulsaProperties;
import controle_estacionamento.cobranca.dto.CalculoDto;
import controle_estacionamento.cobranca.dto.CalculoRequest;
import controle_estacionamento.cobranca.entity.Assinatura;
import controle_estacionamento.cobranca.entity.TipoPlano;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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
    }
}
