package controle_estacionamento.cobranca.service;

import controle_estacionamento.cobranca.dto.AssinaturaDto;
import controle_estacionamento.cobranca.dto.AssinaturaRequest;
import controle_estacionamento.cobranca.entity.Assinatura;
import controle_estacionamento.cobranca.entity.Plano;
import controle_estacionamento.cobranca.exception.RecursoNaoEncontradoException;
import controle_estacionamento.cobranca.exception.RegraNegocioException;
import controle_estacionamento.cobranca.repository.AssinaturaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AssinaturaService {

    private final AssinaturaRepository assinaturaRepository;
    private final PlanoService planoService;
    private final Clock clock;

    // Contrata um plano para a placa. Se ela já tiver plano em vigor, a nova assinatura
    // começa no dia seguinte ao fim da atual (renovação antecipada sem perder dias).
    @Transactional
    public AssinaturaDto contratar(AssinaturaRequest request) {
        LocalDate hoje = LocalDate.now(clock);
        String placa = normalizarPlaca(request.placa());
        Plano plano = planoService.buscar(request.tipoPlano());

        LocalDate inicio = assinaturaRepository.findFirstByPlacaAndCanceladaFalseOrderByFimDesc(placa)
                .filter(ultima -> !ultima.getFim().isBefore(hoje))
                .map(ultima -> ultima.getFim().plusDays(1))
                .orElse(hoje);

        Assinatura assinatura = new Assinatura();
        assinatura.setPlaca(placa);
        assinatura.setNomeCliente(request.nomeCliente().trim());
        assinatura.setTipoPlano(plano.getTipo());
        assinatura.setValorPago(plano.getValor());
        assinatura.setInicio(inicio);
        assinatura.setFim(inicio.plusMonths(plano.getTipo().getMeses()).minusDays(1));
        assinatura.setContratadaEm(LocalDateTime.now(clock));

        return new AssinaturaDto(assinaturaRepository.save(assinatura), hoje);
    }

    @Transactional(readOnly = true)
    public List<AssinaturaDto> listar(String placa) {
        LocalDate hoje = LocalDate.now(clock);
        List<Assinatura> assinaturas = (placa == null || placa.isBlank())
                ? assinaturaRepository.findAllByOrderByContratadaEmDesc()
                : assinaturaRepository.findByPlacaOrderByInicioDesc(normalizarPlaca(placa));
        return assinaturas.stream().map(a -> new AssinaturaDto(a, hoje)).toList();
    }

    @Transactional(readOnly = true)
    public Optional<Assinatura> buscarVigente(String placa, LocalDate data) {
        String placaNormalizada = normalizarPlaca(placa);
        return assinaturaRepository
                .findFirstByPlacaAndCanceladaFalseAndInicioLessThanEqualAndFimGreaterThanEqual(placaNormalizada, data, data);
    }

    @Transactional(readOnly = true)
    public AssinaturaDto buscarVigenteHoje(String placa) {
        LocalDate hoje = LocalDate.now(clock);
        return buscarVigente(placa, hoje)
                .map(a -> new AssinaturaDto(a, hoje))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Placa " + placa + " não possui plano vigente"));
    }

    @Transactional
    public AssinaturaDto cancelar(Long id) {
        Assinatura assinatura = assinaturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Assinatura " + id + " não encontrada"));
        if (assinatura.isCancelada()) {
            throw new RegraNegocioException("Assinatura " + id + " já está cancelada");
        }
        assinatura.setCancelada(true);
        return new AssinaturaDto(assinaturaRepository.save(assinatura), LocalDate.now(clock));
    }

    static String normalizarPlaca(String placa) {
        return placa.trim().toUpperCase().replace("-", "");
    }
}
