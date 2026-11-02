package controle_estacionamento.cobranca.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum TipoPlano {

    MENSAL("Mensal", 1, new BigDecimal("250.00")),
    TRIMESTRAL("Trimestral", 3, new BigDecimal("690.00")),
    SEMESTRAL("Semestral", 6, new BigDecimal("1300.00")),
    ANUAL("Anual", 12, new BigDecimal("2400.00"));

    private final String descricao;
    private final int meses;
    // Preço usado só na primeira subida; depois vale o que está na tabela "plano"
    private final BigDecimal valorPadrao;
}
