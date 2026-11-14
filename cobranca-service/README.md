# cobranca-service

Tabela de preços, planos, assinaturas e cálculo da cobrança de cada permanência. Porta **8083**, banco `cobranca_db`.

Pelo gateway, as rotas ficam em `/api/cobrancas`, `/api/planos` e `/api/assinaturas`.

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/cobrancas/calcular` | Calcula a cobrança de uma permanência (`placa`, `entrada`, `saida`) |
| `GET` | `/cobrancas/tarifa` | Valor da fração avulsa e lista de planos |
| `GET` | `/planos` | Planos do menor para o maior período |
| `PUT` | `/planos/{TIPO}` | Altera o preço de um plano (`{ "valor": 275.00 }`) |
| `POST` | `/assinaturas` | Contrata um plano (`placa`, `nomeCliente`, `tipoPlano`) — 201 |
| `GET` | `/assinaturas?placa=` | Lista todas as assinaturas, ou só as da placa |
| `GET` | `/assinaturas/vigente/{placa}` | Assinatura vigente hoje (404 se não houver) |
| `DELETE` | `/assinaturas/{id}` | Cancela a assinatura |

## Regras

- **Avulso:** cada fração de 15 minutos iniciada é cobrada inteira, com mínimo de uma fração (15min00s = 1; 15min01s = 2).
- **Plano:** placa com assinatura vigente na **data da saída** paga R$ 0,00 (`tipoCobranca = PLANO`).
- **Vigência:** do dia da contratação até a véspera do mesmo dia no fim do período (ex.: mensal de 02/11 a 01/12).
- **Renovação antecipada:** se a placa já tem plano em vigor, o novo começa no dia seguinte ao fim do atual.
- **Preço pago:** a assinatura guarda o valor do plano no momento da contratação; reajustes não alteram contratos.
- Na subida, os planos que não existem no banco são criados com o preço padrão; preços já alterados são mantidos.

## Configuração

| Variável | Padrão | Descrição |
|---|---|---|
| `COBRANCA_VALOR_FRACAO` | `3.00` | Valor de cada fração avulsa |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/cobranca_db` | Conexão com o banco |

O tamanho da fração fica em `cobranca.avulso.minutos-fracao` (padrão 15).

## Testes

```bash
../scripts/testes-unitarios.sh cobranca-service
```
