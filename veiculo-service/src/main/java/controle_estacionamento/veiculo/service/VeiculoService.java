package controle_estacionamento.veiculo.service;

import controle_estacionamento.veiculo.dto.VeiculoDto;
import controle_estacionamento.veiculo.entity.Veiculo;
import controle_estacionamento.veiculo.exception.RecursoNaoEncontradoException;
import controle_estacionamento.veiculo.exception.RegraNegocioException;
import controle_estacionamento.veiculo.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final Clock clock;

    // Cadastra o veículo ou, se a placa já existir, atualiza modelo, cor e WhatsApp
    // (o mesmo carro pode voltar outro dia com outro condutor)
    @Transactional
    public VeiculoDto salvar(VeiculoDto dto) {
        String placa = normalizarPlaca(dto.getPlaca());
        Veiculo veiculo = veiculoRepository.findByPlaca(placa).orElseGet(() -> {
            Veiculo novo = new Veiculo();
            novo.setPlaca(placa);
            novo.setCadastradoEm(LocalDateTime.now(clock));
            return novo;
        });
        preencher(veiculo, dto);
        return new VeiculoDto(veiculoRepository.save(veiculo));
    }

    @Transactional
    public VeiculoDto alterar(Long id, VeiculoDto dto) {
        Veiculo veiculo = buscarEntidade(id);
        String placa = normalizarPlaca(dto.getPlaca());
        veiculoRepository.findByPlaca(placa)
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new RegraNegocioException("Já existe outro veículo com a placa " + placa);
                });
        veiculo.setPlaca(placa);
        preencher(veiculo, dto);
        return new VeiculoDto(veiculoRepository.save(veiculo));
    }

    @Transactional(readOnly = true)
    public List<VeiculoDto> listarTodos() {
        return veiculoRepository.findAll().stream().map(VeiculoDto::new).toList();
    }

    @Transactional(readOnly = true)
    public VeiculoDto buscarPorId(Long id) {
        return new VeiculoDto(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public VeiculoDto buscarPorPlaca(String placa) {
        String placaNormalizada = normalizarPlaca(placa);
        return veiculoRepository.findByPlaca(placaNormalizada)
                .map(VeiculoDto::new)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo " + placaNormalizada + " não encontrado"));
    }

    @Transactional
    public void excluir(Long id) {
        veiculoRepository.delete(buscarEntidade(id));
    }

    private Veiculo buscarEntidade(Long id) {
        return veiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo " + id + " não encontrado"));
    }

    private void preencher(Veiculo veiculo, VeiculoDto dto) {
        veiculo.setModelo(dto.getModelo().trim());
        veiculo.setCor(dto.getCor().trim());
        veiculo.setWhatsappCondutor(normalizarWhatsapp(dto.getWhatsappCondutor()));
    }

    static String normalizarPlaca(String placa) {
        return placa.trim().toUpperCase().replace("-", "");
    }

    // Mantém só os dígitos: DDD + número (10 ou 11) ou com DDI 55 na frente (12 ou 13)
    static String normalizarWhatsapp(String whatsapp) {
        String digitos = whatsapp.replaceAll("\\D", "");
        if (digitos.length() < 10 || digitos.length() > 13) {
            throw new RegraNegocioException("WhatsApp inválido: informe DDD + número");
        }
        return digitos;
    }
}
