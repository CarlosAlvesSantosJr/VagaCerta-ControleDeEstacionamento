# estacionamento-service

Entrada e saída de veículos (tickets) e indicadores do dia. Orquestra os outros dois serviços: cadastra o veículo no **veiculo-service** e pede o valor ao **cobranca-service**. Porta **8082**, banco `estacionamento_db`.

Pelo gateway, as rotas ficam em `/api/estadias`.

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/estadias/entrada` | Registra a entrada com o horário do servidor — 201 |
| `GET` | `/estadias?status=ABERTA` | Veículos no pátio (padrão) |
| `GET` | `/estadias?status=FINALIZADA` | Últimas 50 saídas |
| `GET` | `/estadias/{id}` | Busca o ticket |
| `GET` | `/estadias/{id}/previa` | Quanto pagaria se saísse agora (não fecha o ticket) |
| `POST` | `/estadias/{id}/saida` | Registra a saída e grava a cobrança |
| `GET` | `/estadias/resumo` | Veículos no pátio, entradas e faturamento do dia |

Exemplo de entrada (o horário **não** é enviado):

```json
{ "placa": "ABC1D23", "modelo": "Onix", "cor": "Prata", "whatsappCondutor": "11987654321" }
```

## Regras

- Uma placa só pode ter **um ticket aberto** por vez (409).
- Saída de ticket já finalizado retorna 409; ticket inexistente, 404.
- O ticket guarda uma cópia dos dados do veículo no momento da entrada.
- Chamadas aos outros serviços têm timeout (2 s para conectar, 5 s para ler). Erros 4xx remotos viram 400 com a mesma mensagem; timeout ou 5xx viram **503** e nada é gravado.

## Configuração

| Variável | Padrão |
|---|---|
| `SERVICOS_VEICULO_URL` | `http://localhost:8081` |
| `SERVICOS_COBRANCA_URL` | `http://localhost:8083` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/estacionamento_db` |

## Testes

```bash
../scripts/testes-unitarios.sh estacionamento-service
```
