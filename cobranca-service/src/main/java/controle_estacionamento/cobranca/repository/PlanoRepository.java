package controle_estacionamento.cobranca.repository;

import controle_estacionamento.cobranca.entity.Plano;
import controle_estacionamento.cobranca.entity.TipoPlano;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanoRepository extends JpaRepository<Plano, TipoPlano> {
}
