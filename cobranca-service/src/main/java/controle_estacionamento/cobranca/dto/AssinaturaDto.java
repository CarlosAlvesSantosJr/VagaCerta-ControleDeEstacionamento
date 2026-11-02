package controle_estacionamento.cobranca.dto;

import controle_estacionamento.cobranca.entity.Assinatura;
import controle_estacionamento.cobranca.entity.TipoPlano;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AssinaturaDto(Long id, String placa, String nomeCliente, TipoPlano tipoPlano, String descricaoPlano,
                            BigDecimal valorPago, LocalDate inicio, LocalDate fim, boolean cancelada,
                            boolean vigente, LocalDateTime contratadaEm) {

    public AssinaturaDto(Assinatura a, LocalDate hoje) {
        this(a.getId(), a.getPlaca(), a.getNomeCliente(), a.getTipoPlano(), a.getTipoPlano().getDescricao(),
                a.getValorPago(), a.getInicio(), a.getFim(), a.isCancelada(), a.vigenteEm(hoje), a.getContratadaEm());
    }
}
