package controle_estacionamento.estacionamento.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VeiculoResponse(Long id, String placa, String modelo, String cor, String whatsappCondutor) {
}
