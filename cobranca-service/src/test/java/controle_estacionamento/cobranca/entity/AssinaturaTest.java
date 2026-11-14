package controle_estacionamento.cobranca.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AssinaturaTest {

    @ParameterizedTest(name = "{0} vigente? {1}")
    @CsvSource({
            "2026-10-31, false", // véspera do início
            "2026-11-01, true",  // primeiro dia
            "2026-11-15, true",
            "2026-11-30, true",  // último dia (inclusive)
            "2026-12-01, false"  // dia seguinte ao fim
    })
    void vigenciaIncluiInicioEFim(LocalDate data, boolean vigente) {
        assertThat(novembro(false).vigenteEm(data)).isEqualTo(vigente);
    }

    @Test
    void assinaturaCanceladaNuncaEstaVigente() {
        assertThat(novembro(true).vigenteEm(LocalDate.of(2026, 11, 15))).isFalse();
    }

    private Assinatura novembro(boolean cancelada) {
        Assinatura assinatura = new Assinatura();
        assinatura.setInicio(LocalDate.of(2026, 11, 1));
        assinatura.setFim(LocalDate.of(2026, 11, 30));
        assinatura.setCancelada(cancelada);
        return assinatura;
    }
}
