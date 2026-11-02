package controle_estacionamento.veiculo.dto;

import controle_estacionamento.veiculo.entity.Veiculo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class VeiculoDto {

    public VeiculoDto(Veiculo veiculo) {
        BeanUtils.copyProperties(veiculo, this);
    }

    private Long id;

    // Aceita padrão antigo (ABC-1234) e Mercosul (ABC1D23), com ou sem hífen
    @NotBlank(message = "Placa é obrigatória")
    @Pattern(regexp = "(?i)^[A-Z]{3}-?[0-9][A-Z0-9][0-9]{2}$", message = "Placa inválida")
    private String placa;

    @NotBlank(message = "Modelo é obrigatório")
    private String modelo;

    @NotBlank(message = "Cor é obrigatória")
    private String cor;

    @NotBlank(message = "WhatsApp do condutor é obrigatório")
    private String whatsappCondutor;

    private LocalDateTime cadastradoEm;
}
