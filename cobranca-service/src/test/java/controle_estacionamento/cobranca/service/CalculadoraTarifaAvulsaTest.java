package controle_estacionamento.cobranca.service;

import controle_estacionamento.cobranca.config.TarifaAvulsaProperties;
import controle_estacionamento.cobranca.dto.CalculoDto;
import controle_estacionamento.cobranca.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraTarifaAvulsaTest {

    private final CalculadoraTarifaAvulsa calculadora =
            new CalculadoraTarifaAvulsa(new TarifaAvulsaProperties(new BigDecimal("3.00"), 15));

    private final LocalDateTime entrada = LocalDateTime.of(2026, 11, 2, 8, 0, 0);

    @ParameterizedTest(name = "{0}s de permanência = {1} fração(ões), R$ {2}")
    @CsvSource({
            "0,     1, 3.00",
            "600,   1, 3.00",   // 10 min
            "900,   1, 3.00",   // 15 min exatos
            "901,   2, 6.00",   // 15 min e 1 s já inicia a 2ª fração
            "3600,  4, 12.00",  // 1 h
            "3660,  5, 15.00",  // 1 h 01 min
            "28800, 32, 96.00"  // 8 h
    })
    void cobraCadaFracaoDe15MinutosIniciada(long segundos, long fracoes, String valor) {
        CalculoDto calculo = calculadora.calcular(entrada, entrada.plusSeconds(segundos));

        assertThat(calculo.tipoCobranca()).isEqualTo(CalculoDto.AVULSO);
        assertThat(calculo.fracoes()).isEqualTo(fracoes);
        assertThat(calculo.valorTotal()).isEqualByComparingTo(valor);
    }

    @Test
    void rejeitaSaidaAntesDaEntrada() {
        assertThatThrownBy(() -> calculadora.calcular(entrada, entrada.minusMinutes(1)))
                .isInstanceOf(RegraNegocioException.class);
    }
}
