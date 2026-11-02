package controle_estacionamento.veiculo.controller;

import controle_estacionamento.veiculo.dto.VeiculoDto;
import controle_estacionamento.veiculo.service.VeiculoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/veiculos")
@RequiredArgsConstructor
public class VeiculoController {

    private final VeiculoService veiculoService;

    // Cadastra ou atualiza pelo número da placa
    @PostMapping
    public VeiculoDto salvar(@Valid @RequestBody VeiculoDto veiculo) {
        return veiculoService.salvar(veiculo);
    }

    @GetMapping
    public List<VeiculoDto> listarTodos() {
        return veiculoService.listarTodos();
    }

    @GetMapping("/{id}")
    public VeiculoDto buscarPorId(@PathVariable("id") Long id) {
        return veiculoService.buscarPorId(id);
    }

    @GetMapping("/placa/{placa}")
    public VeiculoDto buscarPorPlaca(@PathVariable("placa") String placa) {
        return veiculoService.buscarPorPlaca(placa);
    }

    @PutMapping("/{id}")
    public VeiculoDto alterar(@PathVariable("id") Long id, @Valid @RequestBody VeiculoDto veiculo) {
        return veiculoService.alterar(id, veiculo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable("id") Long id) {
        veiculoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
