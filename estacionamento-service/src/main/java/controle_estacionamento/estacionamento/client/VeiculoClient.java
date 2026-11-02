package controle_estacionamento.estacionamento.client;

import controle_estacionamento.estacionamento.dto.EntradaRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// Cliente HTTP do veiculo-service
@Component
public class VeiculoClient {

    private final RestClient restClient;

    public VeiculoClient(RestClient.Builder builder, @Value("${servicos.veiculo.url}") String url) {
        this.restClient = builder.baseUrl(url).requestFactory(Timeouts.requestFactory()).build();
    }

    // Cadastra o veículo (ou atualiza o contato, se a placa já existir)
    public VeiculoResponse registrar(EntradaRequest entrada) {
        try {
            return restClient.post()
                    .uri("/veiculos")
                    .body(entrada)
                    .retrieve()
                    .body(VeiculoResponse.class);
        } catch (RestClientException e) {
            throw ErroRemoto.traduzir(e, "veículos");
        }
    }
}
