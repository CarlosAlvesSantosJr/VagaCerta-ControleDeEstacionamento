package controle_estacionamento.cobranca.service;

import controle_estacionamento.cobranca.dto.PlanoDto;
import controle_estacionamento.cobranca.entity.Plano;
import controle_estacionamento.cobranca.entity.TipoPlano;
import controle_estacionamento.cobranca.exception.RecursoNaoEncontradoException;
import controle_estacionamento.cobranca.repository.PlanoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanoService implements ApplicationRunner {

    private final PlanoRepository planoRepository;

    // Na subida, cria na tabela os planos que ainda não existem com o preço padrão
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Arrays.stream(TipoPlano.values())
                .filter(tipo -> !planoRepository.existsById(tipo))
                .forEach(tipo -> planoRepository.save(new Plano(tipo, tipo.getValorPadrao())));
    }

    @Transactional(readOnly = true)
    public List<PlanoDto> listarTodos() {
        return planoRepository.findAll().stream()
                .sorted(Comparator.comparing(plano -> plano.getTipo().getMeses()))
                .map(PlanoDto::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public Plano buscar(TipoPlano tipo) {
        return planoRepository.findById(tipo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plano " + tipo + " não encontrado"));
    }

    @Transactional
    public PlanoDto alterarValor(TipoPlano tipo, BigDecimal valor) {
        Plano plano = buscar(tipo);
        plano.setValor(valor);
        return new PlanoDto(planoRepository.save(plano));
    }
}
