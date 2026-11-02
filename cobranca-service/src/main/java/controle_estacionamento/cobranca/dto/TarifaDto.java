package controle_estacionamento.cobranca.dto;

import java.math.BigDecimal;
import java.util.List;

// Tabela de preços exibida no balcão: avulso + planos
public record TarifaDto(BigDecimal valorFracao, int minutosFracao, List<PlanoDto> planos) {
}
