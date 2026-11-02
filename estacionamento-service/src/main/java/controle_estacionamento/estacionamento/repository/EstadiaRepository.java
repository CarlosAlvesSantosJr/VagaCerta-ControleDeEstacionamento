package controle_estacionamento.estacionamento.repository;

import controle_estacionamento.estacionamento.entity.Estadia;
import controle_estacionamento.estacionamento.entity.StatusEstadia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface EstadiaRepository extends JpaRepository<Estadia, Long> {

    boolean existsByPlacaAndStatus(String placa, StatusEstadia status);

    long countByStatus(StatusEstadia status);

    long countByEntradaGreaterThanEqual(LocalDateTime inicio);

    List<Estadia> findByStatusOrderByEntradaAsc(StatusEstadia status);

    List<Estadia> findTop50ByStatusOrderBySaidaDesc(StatusEstadia status);

    @Query("select coalesce(sum(e.valorTotal), 0) from Estadia e where e.saida >= :inicio")
    BigDecimal somarFaturamentoDesde(@Param("inicio") LocalDateTime inicio);
}
