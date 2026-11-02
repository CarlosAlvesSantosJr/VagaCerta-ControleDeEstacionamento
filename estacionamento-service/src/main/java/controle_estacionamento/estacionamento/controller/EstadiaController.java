package controle_estacionamento.estacionamento.controller;

import controle_estacionamento.estacionamento.dto.EntradaRequest;
import controle_estacionamento.estacionamento.dto.EstadiaDto;
import controle_estacionamento.estacionamento.dto.ResumoDto;
import controle_estacionamento.estacionamento.entity.StatusEstadia;
import controle_estacionamento.estacionamento.service.EstadiaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/estadias")
@RequiredArgsConstructor
public class EstadiaController {

    private final EstadiaService estadiaService;

    @PostMapping("/entrada")
    @ResponseStatus(HttpStatus.CREATED)
    public EstadiaDto registrarEntrada(@Valid @RequestBody EntradaRequest request) {
        return estadiaService.registrarEntrada(request);
    }

    // ABERTA (padrão) = veículos no pátio; FINALIZADA = últimas 50 saídas
    @GetMapping
    public List<EstadiaDto> listar(@RequestParam(name = "status", defaultValue = "ABERTA") StatusEstadia status) {
        return estadiaService.listar(status);
    }

    @GetMapping("/resumo")
    public ResumoDto resumo() {
        return estadiaService.resumoDoDia();
    }

    @GetMapping("/{id}")
    public EstadiaDto buscarPorId(@PathVariable("id") Long id) {
        return estadiaService.buscarPorId(id);
    }

    @GetMapping("/{id}/previa")
    public EstadiaDto previaSaida(@PathVariable("id") Long id) {
        return estadiaService.previaSaida(id);
    }

    @PostMapping("/{id}/saida")
    public EstadiaDto registrarSaida(@PathVariable("id") Long id) {
        return estadiaService.registrarSaida(id);
    }
}
