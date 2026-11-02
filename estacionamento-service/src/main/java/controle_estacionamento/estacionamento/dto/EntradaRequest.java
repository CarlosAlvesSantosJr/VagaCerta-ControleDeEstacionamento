package controle_estacionamento.estacionamento.dto;

import jakarta.validation.constraints.NotBlank;

// Dados que o atendente informa na chegada do veículo. O horário de entrada NÃO vem
// da requisição: é capturado pelo próprio sistema.
public record EntradaRequest(
        @NotBlank(message = "Placa é obrigatória") String placa,
        @NotBlank(message = "Modelo é obrigatório") String modelo,
        @NotBlank(message = "Cor é obrigatória") String cor,
        @NotBlank(message = "WhatsApp do condutor é obrigatório") String whatsappCondutor) {
}
