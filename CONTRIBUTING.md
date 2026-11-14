# Como contribuir

## Fluxo

1. Crie um branch a partir de `main`: `git switch -c feat/nome-curto`.
2. Faça **commits pequenos**, cada um com uma mudança só (um teste, uma correção, uma seção da documentação).
3. Rode os testes antes de abrir o pull request:
   ```bash
   ./scripts/testes-unitarios.sh          # unitários (sem precisar de JDK)
   docker compose up -d --build && ./testes/e2e.sh   # ponta a ponta
   ```
4. Abra o pull request. O GitHub Actions roda os testes de cada serviço e o build das imagens.

## Mensagens de commit

Seguimos o padrão [Conventional Commits](https://www.conventionalcommits.org/pt-br/), com a descrição em português:

```
<tipo>(<escopo opcional>): <descrição no imperativo>
```

| Tipo | Quando usar | Exemplo |
|---|---|---|
| `feat` | Nova funcionalidade | `feat(cobranca): permite desconto em plano anual` |
| `fix` | Correção de bug | `fix(estacionamento): impede saída antes da entrada` |
| `test` | Testes novos ou ajustados | `test(veiculo): cobre exclusão de veículo` |
| `docs` | Só documentação | `docs: explica variáveis de ambiente` |
| `refactor` | Mudança de código sem alterar comportamento | `refactor(cobranca): extrai cálculo de vigência` |
| `build` | Dockerfile, pom.xml, dependências | `build: atualiza Spring Boot` |
| `ci` | Workflows do GitHub Actions | `ci: publica relatório de testes` |
| `chore` | Manutenção geral | `chore: amplia .gitignore` |

Escopos usados: `veiculo`, `estacionamento`, `cobranca`, `gateway`, `readme`.

## Convenções de código

- Java 21, Spring Boot 4, Lombok para getters/setters e construtores.
- Nomes de classes, métodos e mensagens em **português**, seguindo o domínio (estadia, assinatura, fração).
- Horários sempre via `Clock` injetado (nunca `LocalDateTime.now()` sem relógio), para os testes usarem um relógio fixo.
- Erros de negócio lançam `RegraNegocioException` (400), `RecursoNaoEncontradoException` (404) ou `ConflitoException` (409); o `GlobalExceptionHandler` converte para RFC 7807.
- Testes unitários com JUnit 5, Mockito e AssertJ, um método por comportamento e nome descritivo (`naoRegistraSaidaDuasVezes`).
