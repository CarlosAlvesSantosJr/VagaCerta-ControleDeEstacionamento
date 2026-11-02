package controle_estacionamento.estacionamento.dto;

import java.math.BigDecimal;

// Indicadores do dia exibidos no topo da tela do atendente
public record ResumoDto(long veiculosNoPatio, long entradasHoje, BigDecimal faturamentoHoje) {
}
