package controle_estacionamento.cobranca.controller;

import controle_estacionamento.cobranca.dto.CalculoDto;
import controle_estacionamento.cobranca.dto.CalculoRequest;
import controle_estacionamento.cobranca.dto.TarifaDto;
import controle_estacionamento.cobranca.service.CobrancaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cobrancas")
@RequiredArgsConstructor
public class CobrancaController {

    private final CobrancaService cobrancaService;

    @PostMapping("/calcular")
    public CalculoDto calcular(@Valid @RequestBody CalculoRequest request) {
        return cobrancaService.calcular(request);
    }

    @GetMapping("/tarifa")
    public TarifaDto tarifa() {
        return cobrancaService.tarifa();
    }
}
