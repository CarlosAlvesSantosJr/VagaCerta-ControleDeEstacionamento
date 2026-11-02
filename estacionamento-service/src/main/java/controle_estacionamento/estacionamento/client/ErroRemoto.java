package controle_estacionamento.estacionamento.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import controle_estacionamento.estacionamento.exception.RegraNegocioException;
import controle_estacionamento.estacionamento.exception.ServicoIndisponivelException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

// Corpo de erro (ProblemDetail) devolvido pelos outros microserviços
@JsonIgnoreProperties(ignoreUnknown = true)
record ErroRemoto(String detail) {

    // 4xx vira erro de negócio com a mensagem original; o resto (timeout, 5xx) vira 503
    static RuntimeException traduzir(RestClientException e, String servico) {
        if (e instanceof HttpClientErrorException erroCliente) {
            String detalhe = null;
            try {
                ErroRemoto erro = erroCliente.getResponseBodyAs(ErroRemoto.class);
                detalhe = erro != null ? erro.detail() : null;
            } catch (RuntimeException ignorada) {
                // corpo fora do padrão: usa a mensagem genérica abaixo
            }
            return new RegraNegocioException(detalhe != null ? detalhe : "Requisição recusada pelo " + servico);
        }
        return new ServicoIndisponivelException("Serviço de " + servico + " indisponível. Tente novamente.", e);
    }
}
