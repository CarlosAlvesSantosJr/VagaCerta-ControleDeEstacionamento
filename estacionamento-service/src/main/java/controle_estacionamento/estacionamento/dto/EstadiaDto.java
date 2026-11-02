package controle_estacionamento.estacionamento.dto;

import controle_estacionamento.estacionamento.entity.Estadia;
import controle_estacionamento.estacionamento.entity.StatusEstadia;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EstadiaDto(Long id, Long veiculoId, String placa, String modelo, String cor, String whatsappCondutor,
                         LocalDateTime entrada, LocalDateTime saida, StatusEstadia status, String tipoCobranca,
                         String plano, Long minutosPermanencia, Long fracoes, BigDecimal valorTotal) {

    public EstadiaDto(Estadia e) {
        this(e.getId(), e.getVeiculoId(), e.getPlaca(), e.getModelo(), e.getCor(), e.getWhatsappCondutor(),
                e.getEntrada(), e.getSaida(), e.getStatus(), e.getTipoCobranca(), e.getPlano(),
                e.getMinutosPermanencia(), e.getFracoes(), e.getValorTotal());
    }
}
