package controle_estacionamento.estacionamento.service;

import controle_estacionamento.estacionamento.client.CalculoResponse;
import controle_estacionamento.estacionamento.client.CobrancaClient;
import controle_estacionamento.estacionamento.client.VeiculoClient;
import controle_estacionamento.estacionamento.client.VeiculoResponse;
import controle_estacionamento.estacionamento.dto.EntradaRequest;
import controle_estacionamento.estacionamento.dto.EstadiaDto;
import controle_estacionamento.estacionamento.dto.ResumoDto;
import controle_estacionamento.estacionamento.entity.Estadia;
import controle_estacionamento.estacionamento.entity.StatusEstadia;
import controle_estacionamento.estacionamento.exception.ConflitoException;
import controle_estacionamento.estacionamento.exception.RecursoNaoEncontradoException;
import controle_estacionamento.estacionamento.repository.EstadiaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EstadiaService {

    private final EstadiaRepository estadiaRepository;
    private final VeiculoClient veiculoClient;
    private final CobrancaClient cobrancaClient;
    private final Clock clock;

    // 1. Garante que a placa não está no pátio
    // 2. Cadastra/atualiza o veículo no veiculo-service
    // 3. Abre a estadia com o horário atual do servidor
    @Transactional
    public EstadiaDto registrarEntrada(EntradaRequest request) {
        String placa = normalizarPlaca(request.placa());
        verificarSeNaoEstaNoPatio(placa);

        VeiculoResponse veiculo = veiculoClient.registrar(request);

        Estadia estadia = new Estadia();
        estadia.setVeiculoId(veiculo.id());
        estadia.setPlaca(veiculo.placa());
        estadia.setModelo(veiculo.modelo());
        estadia.setCor(veiculo.cor());
        estadia.setWhatsappCondutor(veiculo.whatsappCondutor());
        estadia.setEntrada(agora());
        estadia.setStatus(StatusEstadia.ABERTA);

        return new EstadiaDto(estadiaRepository.save(estadia));
    }

    // Quanto o condutor pagaria se saísse agora (não fecha a estadia)
    @Transactional(readOnly = true)
    public EstadiaDto previaSaida(Long id) {
        Estadia estadia = buscarAberta(id);
        aplicarCobranca(estadia, agora());
        return new EstadiaDto(estadia);
    }

    @Transactional
    public EstadiaDto registrarSaida(Long id) {
        Estadia estadia = buscarAberta(id);
        aplicarCobranca(estadia, agora());
        estadia.setStatus(StatusEstadia.FINALIZADA);
        return new EstadiaDto(estadiaRepository.save(estadia));
    }

    @Transactional(readOnly = true)
    public List<EstadiaDto> listar(StatusEstadia status) {
        List<Estadia> estadias = status == StatusEstadia.FINALIZADA
                ? estadiaRepository.findTop50ByStatusOrderBySaidaDesc(StatusEstadia.FINALIZADA)
                : estadiaRepository.findByStatusOrderByEntradaAsc(StatusEstadia.ABERTA);
        return estadias.stream().map(EstadiaDto::new).toList();
    }

    @Transactional(readOnly = true)
    public EstadiaDto buscarPorId(Long id) {
        return new EstadiaDto(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public ResumoDto resumoDoDia() {
        LocalDateTime inicioDoDia = LocalDate.now(clock).atStartOfDay();
        return new ResumoDto(
                estadiaRepository.countByStatus(StatusEstadia.ABERTA),
                estadiaRepository.countByEntradaGreaterThanEqual(inicioDoDia),
                estadiaRepository.somarFaturamentoDesde(inicioDoDia));
    }

    private void aplicarCobranca(Estadia estadia, LocalDateTime saida) {
        CalculoResponse calculo = cobrancaClient.calcular(estadia.getPlaca(), estadia.getEntrada(), saida);
        estadia.setSaida(saida);
        estadia.setTipoCobranca(calculo.tipoCobranca());
        estadia.setPlano(calculo.plano());
        estadia.setMinutosPermanencia(calculo.minutosPermanencia());
        estadia.setFracoes(calculo.fracoes());
        estadia.setValorTotal(calculo.valorTotal());
    }

    private void verificarSeNaoEstaNoPatio(String placa) {
        if (estadiaRepository.existsByPlacaAndStatus(placa, StatusEstadia.ABERTA)) {
            throw new ConflitoException("O veículo " + placa + " já está no estacionamento");
        }
    }

    private Estadia buscarAberta(Long id) {
        Estadia estadia = buscarEntidade(id);
        if (estadia.getStatus() != StatusEstadia.ABERTA) {
            throw new ConflitoException("A saída do ticket " + id + " já foi registrada");
        }
        return estadia;
    }

    private Estadia buscarEntidade(Long id) {
        return estadiaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ticket " + id + " não encontrado"));
    }

    // Horário do servidor, sem frações de segundo
    private LocalDateTime agora() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }

    static String normalizarPlaca(String placa) {
        return placa.trim().toUpperCase().replace("-", "");
    }
}
