package controle_estacionamento.cobranca.dto;

import controle_estacionamento.cobranca.entity.TipoPlano;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AssinaturaRequest(
        @NotBlank(message = "Placa é obrigatória")
        @Pattern(regexp = "(?i)^[A-Z]{3}-?[0-9][A-Z0-9][0-9]{2}$", message = "Placa inválida")
        String placa,

        @NotBlank(message = "Nome do cliente é obrigatório")
        String nomeCliente,

        @NotNull(message = "Tipo do plano é obrigatório")
        TipoPlano tipoPlano) {
}
