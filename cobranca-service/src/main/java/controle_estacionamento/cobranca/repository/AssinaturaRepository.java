package controle_estacionamento.cobranca.repository;

import controle_estacionamento.cobranca.entity.Assinatura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AssinaturaRepository extends JpaRepository<Assinatura, Long> {

    // Assinatura que cobre a data informada (inicio <= data <= fim)
    Optional<Assinatura> findFirstByPlacaAndCanceladaFalseAndInicioLessThanEqualAndFimGreaterThanEqual(
            String placa, LocalDate data, LocalDate mesmaData);

    // Última assinatura não cancelada, usada para emendar a renovação
    Optional<Assinatura> findFirstByPlacaAndCanceladaFalseOrderByFimDesc(String placa);

    List<Assinatura> findByPlacaOrderByInicioDesc(String placa);

    List<Assinatura> findAllByOrderByContratadaEmDesc();
}
