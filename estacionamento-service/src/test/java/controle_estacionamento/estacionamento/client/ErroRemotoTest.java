package controle_estacionamento.estacionamento.client;

import controle_estacionamento.estacionamento.exception.RegraNegocioException;
import controle_estacionamento.estacionamento.exception.ServicoIndisponivelException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;

import static org.assertj.core.api.Assertions.assertThat;

class ErroRemotoTest {

    @Test
    void erro4xxRepassaMensagemDoServicoRemoto() {
        HttpClientErrorException erro = HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", null, null, null);
        erro.setBodyConvertFunction(tipo -> new ErroRemoto("WhatsApp inválido: informe DDD + número"));

        RuntimeException traduzida = ErroRemoto.traduzir(erro, "veículos");

        assertThat(traduzida)
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("WhatsApp inválido: informe DDD + número");
    }

    @Test
    void erro4xxSemDetalheUsaMensagemGenerica() {
        HttpClientErrorException erro = HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", null, null, null);
        erro.setBodyConvertFunction(tipo -> new ErroRemoto(null));

        assertThat(ErroRemoto.traduzir(erro, "veículos"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Requisição recusada pelo veículos");
    }

    @Test
    void erro4xxComCorpoIlegivelUsaMensagemGenerica() {
        HttpClientErrorException erro = HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null);
        erro.setBodyConvertFunction(tipo -> {
            throw new IllegalStateException("corpo não é JSON");
        });

        assertThat(ErroRemoto.traduzir(erro, "cobrança"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Requisição recusada pelo cobrança");
    }

    @Test
    void erro5xxViraServicoIndisponivel() {
        HttpServerErrorException erro = HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "Erro", null, null, null);

        RuntimeException traduzida = ErroRemoto.traduzir(erro, "cobrança");

        assertThat(traduzida)
                .isInstanceOf(ServicoIndisponivelException.class)
                .hasMessage("Serviço de cobrança indisponível. Tente novamente.")
                .hasCause(erro);
    }

    @Test
    void falhaDeConexaoViraServicoIndisponivel() {
        ResourceAccessException erro = new ResourceAccessException("Connection refused", new ConnectException());

        assertThat(ErroRemoto.traduzir(erro, "veículos")).isInstanceOf(ServicoIndisponivelException.class);
    }
}
