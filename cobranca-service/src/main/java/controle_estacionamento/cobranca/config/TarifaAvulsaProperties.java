package controle_estacionamento.cobranca.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

// Tarifa do cliente esporádico: valor cobrado por fração de tempo iniciada
@ConfigurationProperties(prefix = "cobranca.avulso")
public record TarifaAvulsaProperties(BigDecimal valorFracao, int minutosFracao) {
}
