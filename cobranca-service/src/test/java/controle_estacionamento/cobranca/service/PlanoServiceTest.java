package controle_estacionamento.cobranca.service;

import controle_estacionamento.cobranca.dto.PlanoDto;
import controle_estacionamento.cobranca.entity.Plano;
import controle_estacionamento.cobranca.entity.TipoPlano;
import controle_estacionamento.cobranca.exception.RecursoNaoEncontradoException;
import controle_estacionamento.cobranca.repository.PlanoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PlanoServiceTest {

    private PlanoRepository repository;
    private PlanoService service;

    @BeforeEach
    void setUp() {
        repository = mock(PlanoRepository.class);
        when(repository.save(any(Plano.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new PlanoService(repository);
    }

    @Test
    void naSubidaCriaApenasOsPlanosQueFaltam() {
        when(repository.existsById(any())).thenReturn(true);
        when(repository.existsById(TipoPlano.SEMESTRAL)).thenReturn(false);

        service.run(null);

        ArgumentCaptor<Plano> criado = ArgumentCaptor.forClass(Plano.class);
        verify(repository, times(1)).save(criado.capture());
        assertThat(criado.getValue().getTipo()).isEqualTo(TipoPlano.SEMESTRAL);
        assertThat(criado.getValue().getValor()).isEqualByComparingTo("1300.00");
    }

    @Test
    void naSubidaNaoSobrescrevePrecoJaAlterado() {
        when(repository.existsById(any())).thenReturn(true);

        service.run(null);

        verify(repository, never()).save(any());
    }

    @Test
    void listaPlanosDoMenorParaOMaiorPeriodo() {
        when(repository.findAll()).thenReturn(List.of(
                new Plano(TipoPlano.ANUAL, new BigDecimal("2400.00")),
                new Plano(TipoPlano.MENSAL, new BigDecimal("250.00")),
                new Plano(TipoPlano.SEMESTRAL, new BigDecimal("1300.00")),
                new Plano(TipoPlano.TRIMESTRAL, new BigDecimal("690.00"))));

        assertThat(service.listarTodos())
                .extracting(PlanoDto::tipo)
                .containsExactly(TipoPlano.MENSAL, TipoPlano.TRIMESTRAL, TipoPlano.SEMESTRAL, TipoPlano.ANUAL);
    }

    @Test
    void alterarValorAtualizaPreco() {
        when(repository.findById(TipoPlano.MENSAL)).thenReturn(Optional.of(new Plano(TipoPlano.MENSAL, new BigDecimal("250.00"))));

        PlanoDto alterado = service.alterarValor(TipoPlano.MENSAL, new BigDecimal("275.00"));

        assertThat(alterado.valor()).isEqualByComparingTo("275.00");
        assertThat(alterado.meses()).isEqualTo(1);
        assertThat(alterado.descricao()).isEqualTo("Mensal");
    }

    @Test
    void buscarPlanoInexistenteLancaNaoEncontrado() {
        when(repository.findById(TipoPlano.ANUAL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(TipoPlano.ANUAL)).isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
