package controle_estacionamento.veiculo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    // Relógio do sistema no fuso do estacionamento; nos testes é trocado por um Clock fixo
    @Bean
    public Clock clock(@Value("${estacionamento.fuso-horario:America/Sao_Paulo}") String fusoHorario) {
        return Clock.system(ZoneId.of(fusoHorario));
    }
}
