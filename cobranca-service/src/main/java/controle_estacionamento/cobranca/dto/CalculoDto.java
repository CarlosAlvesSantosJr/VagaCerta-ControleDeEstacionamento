package controle_estacionamento.cobranca.dto;

import controle_estacionamento.cobranca.entity.TipoPlano;

import java.math.BigDecimal;
import java.time.LocalDate;

// Resultado da cobrança de uma permanência.
// tipoCobranca = AVULSO (frações de 15 min) ou PLANO (coberto por assinatura vigente, valor zero)
public record CalculoDto(String tipoCobranca, long minutosPermanencia, long fracoes, BigDecimal valorFracao,
                         BigDecimal valorTotal, TipoPlano plano, LocalDate planoValidoAte) {

    public static final String AVULSO = "AVULSO";
    public static final String PLANO = "PLANO";
}
