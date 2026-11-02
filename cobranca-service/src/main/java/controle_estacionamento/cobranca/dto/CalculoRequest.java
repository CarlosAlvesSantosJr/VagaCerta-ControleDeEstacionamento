package controle_estacionamento.cobranca.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CalculoRequest(
        @NotBlank(message = "Placa é obrigatória") String placa,
        @NotNull(message = "Horário de entrada é obrigatório") LocalDateTime entrada,
        @NotNull(message = "Horário de saída é obrigatório") LocalDateTime saida) {
}
