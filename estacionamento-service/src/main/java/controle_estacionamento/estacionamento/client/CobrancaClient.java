package controle_estacionamento.estacionamento.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.Map;

// Cliente HTTP do cobranca-service
@Component
public class CobrancaClient {

    private final RestClient restClient;

    public CobrancaClient(RestClient.Builder builder, @Value("${servicos.cobranca.url}") String url) {
        this.restClient = builder.baseUrl(url).requestFactory(Timeouts.requestFactory()).build();
    }

    public CalculoResponse calcular(String placa, LocalDateTime entrada, LocalDateTime saida) {
        try {
            return restClient.post()
                    .uri("/cobrancas/calcular")
                    .body(Map.of("placa", placa, "entrada", entrada, "saida", saida))
                    .retrieve()
                    .body(CalculoResponse.class);
        } catch (RestClientException e) {
            throw ErroRemoto.traduzir(e, "cobrança");
        }
    }
}
