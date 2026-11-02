package controle_estacionamento.cobranca.controller;

import controle_estacionamento.cobranca.dto.AlterarValorPlanoRequest;
import controle_estacionamento.cobranca.dto.PlanoDto;
import controle_estacionamento.cobranca.entity.TipoPlano;
import controle_estacionamento.cobranca.service.PlanoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/planos")
@RequiredArgsConstructor
public class PlanoController {

    private final PlanoService planoService;

    @GetMapping
    public List<PlanoDto> listarTodos() {
        return planoService.listarTodos();
    }

    @PutMapping("/{tipo}")
    public PlanoDto alterarValor(@PathVariable("tipo") TipoPlano tipo, @Valid @RequestBody AlterarValorPlanoRequest request) {
        return planoService.alterarValor(tipo, request.valor());
    }
}
