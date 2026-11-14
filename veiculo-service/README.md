# veiculo-service

Cadastro de veículos e do contato (WhatsApp) do condutor. Porta **8081**, banco `veiculo_db`.

Pelo gateway, as rotas ficam em `/api/veiculos`.

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/veiculos` | Cadastra o veículo ou, se a placa já existir, atualiza modelo, cor e WhatsApp |
| `GET` | `/veiculos` | Lista todos os veículos |
| `GET` | `/veiculos/{id}` | Busca por id |
| `GET` | `/veiculos/placa/{placa}` | Busca por placa (aceita minúsculas e hífen) |
| `PUT` | `/veiculos/{id}` | Altera os dados; recusa placa já usada por outro veículo |
| `DELETE` | `/veiculos/{id}` | Exclui o veículo (204) |

Exemplo de corpo:

```json
{ "placa": "ABC-1D23", "modelo": "Onix", "cor": "Prata", "whatsappCondutor": "(11) 98765-4321" }
```

## Regras

- **Placa:** padrão antigo (`ABC1234`) ou Mercosul (`ABC1D23`), com ou sem hífen. É gravada em maiúsculas e sem hífen.
- **WhatsApp:** só os dígitos são guardados; precisa ter DDD (10 ou 11 dígitos) ou DDI 55 (12 ou 13).
- **Erros:** respostas no formato RFC 7807 (`ProblemDetail`); 400 para validação e 404 para recurso inexistente.

## Configuração

| Variável | Padrão |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/veiculo_db` |
| `SPRING_DATASOURCE_USERNAME` / `PASSWORD` | `postgres` / `root` |

## Testes

```bash
../scripts/testes-unitarios.sh veiculo-service
```
