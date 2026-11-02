package controle_estacionamento.estacionamento.exception;

// Um microserviço de que dependemos (veículo ou cobrança) não respondeu
public class ServicoIndisponivelException extends RuntimeException {
    public ServicoIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
