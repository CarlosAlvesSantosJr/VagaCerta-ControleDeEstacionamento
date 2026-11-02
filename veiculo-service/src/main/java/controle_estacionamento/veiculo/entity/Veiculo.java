package controle_estacionamento.veiculo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "veiculo")
@Getter
@Setter
@NoArgsConstructor
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Placa normalizada: maiúscula e sem hífen (ex: ABC1D23)
    @Column(nullable = false, unique = true, length = 7)
    private String placa;

    @Column(nullable = false)
    private String modelo;

    @Column(nullable = false)
    private String cor;

    // Somente dígitos, com DDD (ex: 11987654321)
    @Column(name = "whatsapp_condutor", nullable = false, length = 13)
    private String whatsappCondutor;

    @Column(name = "cadastrado_em", nullable = false, updatable = false)
    private LocalDateTime cadastradoEm;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Veiculo veiculo = (Veiculo) o;
        return Objects.equals(id, veiculo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
