package controle_estacionamento.cobranca.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

// Contrato de um plano para uma placa. Vigente de "inicio" até "fim" (inclusive)
@Entity
@Table(name = "assinatura", indexes = @Index(name = "idx_assinatura_placa", columnList = "placa"))
@Getter
@Setter
@NoArgsConstructor
public class Assinatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 7)
    private String placa;

    @Column(name = "nome_cliente", nullable = false)
    private String nomeCliente;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_plano", nullable = false, length = 20)
    private TipoPlano tipoPlano;

    // Preço do plano no momento da contratação
    @Column(name = "valor_pago", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorPago;

    @Column(nullable = false)
    private LocalDate inicio;

    @Column(nullable = false)
    private LocalDate fim;

    @Column(nullable = false)
    private boolean cancelada;

    @Column(name = "contratada_em", nullable = false, updatable = false)
    private LocalDateTime contratadaEm;

    public boolean vigenteEm(LocalDate data) {
        return !cancelada && !data.isBefore(inicio) && !data.isAfter(fim);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Assinatura that = (Assinatura) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
