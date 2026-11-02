package controle_estacionamento.cobranca.service;

import controle_estacionamento.cobranca.config.TarifaAvulsaProperties;
import controle_estacionamento.cobranca.dto.CalculoDto;
import controle_estacionamento.cobranca.dto.CalculoRequest;
import controle_estacionamento.cobranca.dto.TarifaDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CobrancaService {

    private final CalculadoraTarifaAvulsa calculadora;
    private final AssinaturaService assinaturaService;
    private final PlanoService planoService;
    private final TarifaAvulsaProperties tarifaAvulsa;

    // Placa com plano vigente na data da saída não paga a permanência;
    // as demais (clientes esporádicos) pagam por fração de 15 minutos
    public CalculoDto calcular(CalculoRequest request) {
        CalculoDto avulso = calculadora.calcular(request.entrada(), request.saida());

        return assinaturaService.buscarVigente(request.placa(), request.saida().toLocalDate())
                .map(assinatura -> new CalculoDto(CalculoDto.PLANO, avulso.minutosPermanencia(), avulso.fracoes(),
                        avulso.valorFracao(), BigDecimal.ZERO.setScale(2), assinatura.getTipoPlano(), assinatura.getFim()))
                .orElse(avulso);
    }

    public TarifaDto tarifa() {
        return new TarifaDto(tarifaAvulsa.valorFracao(), tarifaAvulsa.minutosFracao(), planoService.listarTodos());
    }
}
