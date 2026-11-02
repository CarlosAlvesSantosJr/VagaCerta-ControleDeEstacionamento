package controle_estacionamento.estacionamento.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CalculoResponse(String tipoCobranca, long minutosPermanencia, long fracoes, BigDecimal valorFracao,
                              BigDecimal valorTotal, String plano, LocalDate planoValidoAte) {
}
