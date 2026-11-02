package controle_estacionamento.cobranca;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CobrancaServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CobrancaServiceApplication.class, args);
	}

}
