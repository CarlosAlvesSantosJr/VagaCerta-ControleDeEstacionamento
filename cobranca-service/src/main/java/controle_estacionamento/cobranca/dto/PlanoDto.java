package controle_estacionamento.cobranca.dto;

import controle_estacionamento.cobranca.entity.Plano;
import controle_estacionamento.cobranca.entity.TipoPlano;

import java.math.BigDecimal;

public record PlanoDto(TipoPlano tipo, String descricao, int meses, BigDecimal valor) {

    public PlanoDto(Plano plano) {
        this(plano.getTipo(), plano.getTipo().getDescricao(), plano.getTipo().getMeses(), plano.getValor());
    }
}
