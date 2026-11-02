package controle_estacionamento.cobranca.service;

import controle_estacionamento.cobranca.config.TarifaAvulsaProperties;
import controle_estacionamento.cobranca.dto.CalculoDto;
import controle_estacionamento.cobranca.exception.RegraNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CalculadoraTarifaAvulsa {

    private final TarifaAvulsaProperties tarifa;

    // Cada fração de 15 minutos iniciada é cobrada inteira, com mínimo de uma fração.
    // Ex: 15min00s = 1 fração; 15min01s = 2 frações; 1h = 4 frações
    public CalculoDto calcular(LocalDateTime entrada, LocalDateTime saida) {
        if (saida.isBefore(entrada)) {
            throw new RegraNegocioException("Horário de saída não pode ser anterior à entrada");
        }
        long segundos = Duration.between(entrada, saida).toSeconds();
        long segundosPorFracao = tarifa.minutosFracao() * 60L;
        long fracoes = Math.max(1, (segundos + segundosPorFracao - 1) / segundosPorFracao);

        BigDecimal total = tarifa.valorFracao()
                .multiply(BigDecimal.valueOf(fracoes))
                .setScale(2, RoundingMode.HALF_UP);

        return new CalculoDto(CalculoDto.AVULSO, segundos / 60, fracoes, tarifa.valorFracao(), total, null, null);
    }
}
