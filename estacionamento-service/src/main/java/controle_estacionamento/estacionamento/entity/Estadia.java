package controle_estacionamento.estacionamento.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

// Ticket de permanência: da entrada até a saída do veículo.
// Guarda uma cópia dos dados do veículo do momento da entrada (o cadastro pode mudar depois).
@Entity
@Table(name = "estadia", indexes = @Index(name = "idx_estadia_placa_status", columnList = "placa,status"))
@Getter
@Setter
@NoArgsConstructor
public class Estadia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "veiculo_id", nullable = false)
    private Long veiculoId;

    @Column(nullable = false, length = 7)
    private String placa;

    @Column(nullable = false)
    private String modelo;

    @Column(nullable = false)
    private String cor;

    @Column(name = "whatsapp_condutor", nullable = false, length = 13)
    private String whatsappCondutor;

    // Preenchido pelo próprio sistema no momento do registro
    @Column(nullable = false)
    private LocalDateTime entrada;

    private LocalDateTime saida;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private StatusEstadia status;

    // AVULSO ou PLANO, conforme o cobranca-service
    @Column(name = "tipo_cobranca", length = 10)
    private String tipoCobranca;

    @Column(length = 20)
    private String plano;

    @Column(name = "minutos_permanencia")
    private Long minutosPermanencia;

    private Long fracoes;

    @Column(name = "valor_total", precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Estadia estadia = (Estadia) o;
        return Objects.equals(id, estadia.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
