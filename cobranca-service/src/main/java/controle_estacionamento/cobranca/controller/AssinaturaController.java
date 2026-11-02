package controle_estacionamento.cobranca.controller;

import controle_estacionamento.cobranca.dto.AssinaturaDto;
import controle_estacionamento.cobranca.dto.AssinaturaRequest;
import controle_estacionamento.cobranca.service.AssinaturaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/assinaturas")
@RequiredArgsConstructor
public class AssinaturaController {

    private final AssinaturaService assinaturaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssinaturaDto contratar(@Valid @RequestBody AssinaturaRequest request) {
        return assinaturaService.contratar(request);
    }

    // Lista todas, ou só as da placa quando ?placa= for informado
    @GetMapping
    public List<AssinaturaDto> listar(@RequestParam(name = "placa", required = false) String placa) {
        return assinaturaService.listar(placa);
    }

    @GetMapping("/vigente/{placa}")
    public AssinaturaDto vigente(@PathVariable("placa") String placa) {
        return assinaturaService.buscarVigenteHoje(placa);
    }

    @DeleteMapping("/{id}")
    public AssinaturaDto cancelar(@PathVariable("id") Long id) {
        return assinaturaService.cancelar(id);
    }
}
