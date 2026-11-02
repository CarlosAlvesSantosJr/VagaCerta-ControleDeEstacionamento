package controle_estacionamento.cobranca.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "plano")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Plano {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TipoPlano tipo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;
}
