package controle_estacionamento.veiculo.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class VeiculoDtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void criarValidador() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void fecharValidador() {
        factory.close();
    }

    @ParameterizedTest(name = "placa válida: {0}")
    @ValueSource(strings = {"ABC1234", "ABC-1234", "abc1d23", "ABC1D23", "abc-1d23"})
    void aceitaPadraoAntigoEMercosul(String placa) {
        assertThat(validar(placa)).isEmpty();
    }

    @ParameterizedTest(name = "placa inválida: {0}")
    @ValueSource(strings = {"AB1234", "ABCD123", "ABC12345", "1BC1234", "ABC-12D4", "ABC_1234"})
    void recusaPlacaForaDoPadrao(String placa) {
        assertThat(validar(placa))
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Placa inválida");
    }

    @Test
    void exigeTodosOsCamposObrigatorios() {
        Set<ConstraintViolation<VeiculoDto>> violacoes = validator.validate(new VeiculoDto());

        assertThat(violacoes)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "Placa é obrigatória",
                        "Modelo é obrigatório",
                        "Cor é obrigatória",
                        "WhatsApp do condutor é obrigatório");
    }

    private Set<ConstraintViolation<VeiculoDto>> validar(String placa) {
        VeiculoDto dto = new VeiculoDto();
        dto.setPlaca(placa);
        dto.setModelo("Onix");
        dto.setCor("Prata");
        dto.setWhatsappCondutor("11987654321");
        return validator.validate(dto);
    }
}
