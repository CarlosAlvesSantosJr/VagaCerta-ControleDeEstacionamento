#!/usr/bin/env bash
# Testes de ponta a ponta do VagaCerta.
# Exercita todas as funcionalidades pela API do gateway, com a stack rodando no Docker.
#
# Uso:  ./testes/e2e.sh                 (gateway em http://localhost:8090)
#       BASE=http://outro:porta ./testes/e2e.sh
#       PULAR_RESILIENCIA=1 ./testes/e2e.sh   (não derruba o cobranca-service)

set -u
BASE="${BASE:-http://localhost:8090}"
API="$BASE/api"
cd "$(dirname "$0")/.."

PASSOU=0
FALHOU=0
VERDE=$'\e[32m'; VERMELHO=$'\e[31m'; NEGRITO=$'\e[1m'; RESET=$'\e[0m'

secao() { echo; echo "${NEGRITO}▸ $1${RESET}"; }

checar() { # descricao esperado obtido
    if [[ "$3" == "$2" ]]; then
        PASSOU=$((PASSOU + 1)); echo "  ${VERDE}✔${RESET} $1"
    else
        FALHOU=$((FALHOU + 1)); echo "  ${VERMELHO}✘ $1${RESET} (esperado: '$2', obtido: '$3')"
    fi
}

contem() { # descricao texto trecho
    if [[ "$2" == *"$3"* ]]; then
        PASSOU=$((PASSOU + 1)); echo "  ${VERDE}✔${RESET} $1"
    else
        FALHOU=$((FALHOU + 1)); echo "  ${VERMELHO}✘ $1${RESET} (não contém '$3')"
    fi
}

# req METODO CAMINHO [JSON] -> preenche CODIGO e CORPO
req() {
    local resposta
    if [[ $# -ge 3 ]]; then
        # corpo via stdin: no Windows o curl converteria o argumento -d para o codepage local
        resposta=$(printf '%s' "$3" | curl -s -w $'\n%{http_code}' -X "$1" "$API$2" -H 'Content-Type: application/json; charset=utf-8' --data-binary @-)
    else
        resposta=$(curl -s -w $'\n%{http_code}' -X "$1" "$API$2")
    fi
    CODIGO="${resposta##*$'\n'}"
    CORPO="${resposta%$'\n'*}"
}

# valor de um campo simples do JSON (primeira ocorrência)
campo() { echo "$CORPO" | grep -o "\"$1\":[^,}]*" | head -1 | sed -E 's/^"[^"]*"://; s/^"//; s/"$//'; }

esperar_api() { # caminho
    for _ in $(seq 1 60); do
        curl -sf "$API$1" > /dev/null && return 0
        sleep 2
    done
    return 1
}

# Placas aleatórias para o teste poder rodar várias vezes sobre o mesmo banco
n4() { printf '%04d' $((RANDOM % 10000)); }
n2() { printf '%02d' $((RANDOM % 100)); }
PLACA_AVULSA="TST$(n4)"                 # padrão antigo
PLACA_MERCOSUL="TSM1A$(n2)"             # padrão Mercosul
PLACA_PLANO="PLN2B$(n2)"
PLACA_CANCELADA="CAN3C$(n2)"
HOJE=$(date +%F)

echo "${NEGRITO}VagaCerta · testes de ponta a ponta em $BASE${RESET}"
esperar_api /estadias/resumo || { echo "API indisponível em $API"; exit 1; }

# ---------------------------------------------------------------
secao "Gateway e frontend"
for arquivo in "" "js/app.js" "css/app.css" "img/icone.svg"; do
    checar "GET /$arquivo responde 200" 200 "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/$arquivo")"
done
contem "Página tem a marca VagaCerta" "$(curl -s "$BASE/")" "VagaCerta"

# ---------------------------------------------------------------
secao "Tarifa e tabela de planos"
req GET /cobrancas/tarifa
checar "GET /cobrancas/tarifa responde 200" 200 "$CODIGO"
checar "Fração de 15 minutos" 15 "$(campo minutosFracao)"
checar "Valor da fração R\$ 3,00" "3.00" "$(campo valorFracao)"
for plano in MENSAL TRIMESTRAL SEMESTRAL ANUAL; do
    contem "Plano $plano disponível" "$CORPO" "\"tipo\":\"$plano\""
done
req GET /planos
checar "GET /planos responde 200" 200 "$CODIGO"

# ---------------------------------------------------------------
secao "Entrada de cliente esporádico"
req POST /estadias/entrada "{\"placa\":\"${PLACA_AVULSA:0:3}-${PLACA_AVULSA:3}\",\"modelo\":\" Onix \",\"cor\":\"Prata\",\"whatsappCondutor\":\"(11) 98765-4321\"}"
checar "Entrada responde 201" 201 "$CODIGO"
ID_AVULSO=$(campo id)
checar "Status ABERTA" ABERTA "$(campo status)"
checar "Placa normalizada (sem hífen)" "$PLACA_AVULSA" "$(campo placa)"
checar "Modelo sem espaços extras" Onix "$(campo modelo)"
checar "WhatsApp só com dígitos" 11987654321 "$(campo whatsappCondutor)"
contem "Horário de entrada gerado pelo sistema (hoje)" "$(campo entrada)" "$HOJE"
checar "Saída ainda vazia" null "$(campo saida)"

req POST /estadias/entrada "{\"placa\":\"$PLACA_MERCOSUL\",\"modelo\":\"HB20\",\"cor\":\"Branco\",\"whatsappCondutor\":\"5521999998888\"}"
checar "Entrada com placa Mercosul responde 201" 201 "$CODIGO"
ID_MERCOSUL=$(campo id)
checar "WhatsApp com DDI 55 aceito" 5521999998888 "$(campo whatsappCondutor)"

secao "Regras de entrada"
req POST /estadias/entrada "{\"placa\":\"$PLACA_AVULSA\",\"modelo\":\"Onix\",\"cor\":\"Prata\",\"whatsappCondutor\":\"11987654321\"}"
checar "Mesma placa no pátio é recusada (409)" 409 "$CODIGO"
contem "Mensagem explica o conflito" "$(campo detail)" "já está no estacionamento"

req POST /estadias/entrada '{"placa":"XX12","modelo":"Gol","cor":"Preto","whatsappCondutor":"11987654321"}'
checar "Placa inválida (400)" 400 "$CODIGO"
contem "Mensagem de placa inválida" "$(campo detail)" "Placa inválida"

req POST /estadias/entrada '{"placa":"QWE9876","modelo":"Gol","cor":"Preto","whatsappCondutor":"98765-4321"}'
checar "WhatsApp sem DDD (400)" 400 "$CODIGO"
contem "Mensagem de WhatsApp inválido" "$(campo detail)" "WhatsApp"

req POST /estadias/entrada '{"placa":"","modelo":"","cor":"","whatsappCondutor":""}'
checar "Campos obrigatórios vazios (400)" 400 "$CODIGO"

req POST /estadias/entrada '{"placa":"QWE9876",'
checar "JSON malformado (400)" 400 "$CODIGO"
contem "Mensagem de JSON inválido" "$(campo detail)" "inválido"

req GET /estadias
checar "Entradas inválidas não ficam no pátio" "" "$(echo "$CORPO" | grep -o 'QWE9876')"

# ---------------------------------------------------------------
secao "Cadastro de veículos (veiculo-service)"
req GET "/veiculos/placa/$PLACA_AVULSA"
checar "Veículo cadastrado automaticamente na entrada" 200 "$CODIGO"
ID_VEICULO=$(campo id)
checar "Cor gravada" Prata "$(campo cor)"
req GET "/veiculos/placa/${PLACA_AVULSA,,}"
checar "Busca por placa ignora maiúsculas/minúsculas" 200 "$CODIGO"
req GET "/veiculos/$ID_VEICULO"
checar "GET /veiculos/{id}" 200 "$CODIGO"
req GET /veiculos
contem "Listagem contém o veículo" "$CORPO" "$PLACA_AVULSA"
req PUT "/veiculos/$ID_VEICULO" "{\"placa\":\"$PLACA_AVULSA\",\"modelo\":\"Onix Plus\",\"cor\":\"Prata\",\"whatsappCondutor\":\"11911112222\"}"
checar "Alterar veículo (200)" 200 "$CODIGO"
checar "Modelo alterado" "Onix Plus" "$(campo modelo)"
req PUT "/veiculos/$ID_VEICULO" "{\"placa\":\"$PLACA_MERCOSUL\",\"modelo\":\"X\",\"cor\":\"Y\",\"whatsappCondutor\":\"11911112222\"}"
checar "Não permite placa de outro veículo (400)" 400 "$CODIGO"
req GET /veiculos/placa/ZZZ9999
checar "Placa inexistente (404)" 404 "$CODIGO"
req POST /veiculos '{"placa":"DEL1D11","modelo":"Uno","cor":"Azul","whatsappCondutor":"11933334444"}'
ID_DEL=$(campo id)
req DELETE "/veiculos/$ID_DEL"
checar "Excluir veículo (204)" 204 "$CODIGO"
req GET "/veiculos/$ID_DEL"
checar "Veículo excluído não é mais encontrado" 404 "$CODIGO"

# ---------------------------------------------------------------
secao "Pátio, prévia e saída avulsa"
req GET /estadias
contem "Pátio lista o veículo avulso" "$CORPO" "$PLACA_AVULSA"
req GET "/estadias/$ID_AVULSO"
checar "GET /estadias/{id}" 200 "$CODIGO"

req GET "/estadias/$ID_AVULSO/previa"
checar "Prévia responde 200" 200 "$CODIGO"
checar "Prévia: cobrança avulsa" AVULSO "$(campo tipoCobranca)"
checar "Prévia: 1 fração (menos de 15 min)" 1 "$(campo fracoes)"
checar "Prévia: R\$ 3,00" "3.00" "$(campo valorTotal)"
checar "Prévia não fecha o ticket" ABERTA "$(campo status)"

req POST "/estadias/$ID_AVULSO/saida"
checar "Saída responde 200" 200 "$CODIGO"
checar "Ticket finalizado" FINALIZADA "$(campo status)"
checar "Valor cobrado R\$ 3,00" "3.00" "$(campo valorTotal)"
contem "Horário de saída gerado pelo sistema" "$(campo saida)" "$HOJE"

req POST "/estadias/$ID_AVULSO/saida"
checar "Saída repetida é recusada (409)" 409 "$CODIGO"
req GET "/estadias/$ID_AVULSO/previa"
checar "Prévia de ticket fechado é recusada (409)" 409 "$CODIGO"
req POST /estadias/999999/saida
checar "Ticket inexistente (404)" 404 "$CODIGO"

req POST /estadias/entrada "{\"placa\":\"$PLACA_AVULSA\",\"modelo\":\"Onix\",\"cor\":\"Prata\",\"whatsappCondutor\":\"11987654321\"}"
checar "Mesma placa pode entrar de novo após sair" 201 "$CODIGO"
ID_AVULSO2=$(campo id)

# ---------------------------------------------------------------
secao "Planos e assinaturas"
FIM_MENSAL=$(date -d "$HOJE +1 month -1 day" +%F)
req POST /assinaturas "{\"placa\":\"$PLACA_PLANO\",\"nomeCliente\":\"Maria Souza\",\"tipoPlano\":\"MENSAL\"}"
checar "Contratar plano mensal (201)" 201 "$CODIGO"
ID_ASSINATURA=$(campo id)
checar "Começa hoje" "$HOJE" "$(campo inicio)"
checar "Termina na véspera do mesmo dia do mês seguinte" "$FIM_MENSAL" "$(campo fim)"
checar "Vigente" true "$(campo vigente)"
checar "Valor pago R\$ 250,00" "250.00" "$(campo valorPago)"

INICIO_RENOVACAO=$(date -d "$FIM_MENSAL +1 day" +%F)
FIM_RENOVACAO=$(date -d "$INICIO_RENOVACAO +3 month -1 day" +%F)
req POST /assinaturas "{\"placa\":\"$PLACA_PLANO\",\"nomeCliente\":\"Maria Souza\",\"tipoPlano\":\"TRIMESTRAL\"}"
checar "Renovação antecipada (201)" 201 "$CODIGO"
ID_RENOVACAO=$(campo id)
checar "Renovação começa no dia seguinte ao fim do plano atual" "$INICIO_RENOVACAO" "$(campo inicio)"
checar "Renovação trimestral dura 3 meses" "$FIM_RENOVACAO" "$(campo fim)"
checar "Renovação ainda não vigente (agendada)" false "$(campo vigente)"

for plano in SEMESTRAL:6 ANUAL:12; do
    tipo=${plano%%:*}; meses=${plano##*:}
    placa="P${tipo:0:2}$(n4)"
    req POST /assinaturas "{\"placa\":\"$placa\",\"nomeCliente\":\"Teste\",\"tipoPlano\":\"$tipo\"}"
    checar "Plano $tipo termina em $meses meses" "$(date -d "$HOJE +$meses month -1 day" +%F)" "$(campo fim)"
done

req POST /assinaturas "{\"placa\":\"$PLACA_PLANO\",\"nomeCliente\":\"\",\"tipoPlano\":\"MENSAL\"}"
checar "Nome do cliente obrigatório (400)" 400 "$CODIGO"
req POST /assinaturas '{"placa":"ABC","nomeCliente":"X","tipoPlano":"MENSAL"}'
checar "Placa inválida na assinatura (400)" 400 "$CODIGO"

req GET "/assinaturas/vigente/$PLACA_PLANO"
checar "Consulta de plano vigente (200)" 200 "$CODIGO"
checar "Plano vigente é o mensal" MENSAL "$(campo tipoPlano)"
req GET "/assinaturas?placa=$PLACA_PLANO"
checar "Lista assinaturas por placa" 200 "$CODIGO"
checar "Placa tem 2 assinaturas" 2 "$(echo "$CORPO" | grep -o '"id":' | wc -l | tr -d ' ')"
req GET /assinaturas/vigente/SEM0P00
checar "Placa sem plano (404)" 404 "$CODIGO"

secao "Mensalista não paga a permanência"
req POST /estadias/entrada "{\"placa\":\"$PLACA_PLANO\",\"modelo\":\"Compass\",\"cor\":\"Cinza\",\"whatsappCondutor\":\"11922223333\"}"
ID_PLANO=$(campo id)
req GET "/estadias/$ID_PLANO/previa"
checar "Prévia: cobrança por plano" PLANO "$(campo tipoCobranca)"
checar "Prévia: plano mensal" MENSAL "$(campo plano)"
req POST "/estadias/$ID_PLANO/saida"
checar "Saída do mensalista: R\$ 0,00" "0.00" "$(campo valorTotal)"
checar "Saída registra o tipo PLANO" PLANO "$(campo tipoCobranca)"

secao "Cancelamento de plano"
req POST /assinaturas "{\"placa\":\"$PLACA_CANCELADA\",\"nomeCliente\":\"João\",\"tipoPlano\":\"MENSAL\"}"
ID_CANCELAR=$(campo id)
req DELETE "/assinaturas/$ID_CANCELAR"
checar "Cancelar assinatura (200)" 200 "$CODIGO"
checar "Nome com acento preservado" "João" "$(campo nomeCliente)"
checar "Marcada como cancelada" true "$(campo cancelada)"
checar "Deixa de ser vigente" false "$(campo vigente)"
req DELETE "/assinaturas/$ID_CANCELAR"
checar "Cancelar de novo é recusado (400)" 400 "$CODIGO"
req POST /estadias/entrada "{\"placa\":\"$PLACA_CANCELADA\",\"modelo\":\"Kwid\",\"cor\":\"Vermelho\",\"whatsappCondutor\":\"11944445555\"}"
ID_CANC_EST=$(campo id)
req POST "/estadias/$ID_CANC_EST/saida"
checar "Placa com plano cancelado volta a pagar avulso" AVULSO "$(campo tipoCobranca)"
checar "Valor avulso R\$ 3,00" "3.00" "$(campo valorTotal)"
req DELETE "/assinaturas/$ID_RENOVACAO"
checar "Cancelar renovação agendada" 200 "$CODIGO"

secao "Alteração de preço de plano"
req PUT /planos/MENSAL '{"valor":275.50}'
checar "Alterar preço (200)" 200 "$CODIGO"
checar "Novo preço aplicado" "275.50" "$(campo valor)"
req GET /cobrancas/tarifa
contem "Tarifa reflete o novo preço" "$CORPO" "275.50"
req PUT /planos/MENSAL '{"valor":0}'
checar "Preço zero é recusado (400)" 400 "$CODIGO"
req PUT /planos/BIENAL '{"valor":100}'
checar "Plano inexistente é recusado (400)" 400 "$CODIGO"
contem "Mensagem aponta o parâmetro inválido" "$(campo detail)" "BIENAL"
req PUT /planos/MENSAL '{"valor":250.00}'
checar "Preço original restaurado" "250.00" "$(campo valor)"
req GET "/assinaturas?placa=$PLACA_PLANO"
contem "Assinatura antiga mantém o valor pago" "$CORPO" '"valorPago":250.00'

# ---------------------------------------------------------------
secao "Histórico e resumo do dia"
req GET "/estadias?status=FINALIZADA"
checar "Histórico responde 200" 200 "$CODIGO"
contem "Histórico contém a saída avulsa" "$CORPO" "$PLACA_AVULSA"
contem "Histórico contém a saída do mensalista" "$CORPO" "$PLACA_PLANO"
req GET "/estadias?status=QUALQUER"
checar "Filtro de status inválido (400)" 400 "$CODIGO"
req GET /estadias/resumo
checar "Resumo responde 200" 200 "$CODIGO"
contem "Resumo traz veículos no pátio" "$CORPO" "veiculosNoPatio"
contem "Resumo traz entradas do dia" "$CORPO" "entradasHoje"
contem "Resumo traz faturamento do dia" "$CORPO" "faturamentoHoje"

# ---------------------------------------------------------------
if [[ -z "${PULAR_RESILIENCIA:-}" ]]; then
    secao "Resiliência (cobranca-service fora do ar)"
    docker compose stop cobranca-service > /dev/null 2>&1
    inicio=$(date +%s%N)
    req GET "/estadias/$ID_MERCOSUL/previa"
    ms=$(( ($(date +%s%N) - inicio) / 1000000 ))
    checar "Responde 503 quando a cobrança cai" 503 "$CODIGO"
    contem "Mensagem amigável" "$(campo detail)" "indisponível"
    checar "Falha rápida (menos de 4 s)" true "$([[ $ms -lt 4000 ]] && echo true || echo false)"
    req POST /estadias/entrada '{"placa":"RES1L00","modelo":"Gol","cor":"Preto","whatsappCondutor":"11987654321"}'
    checar "Entrada continua funcionando sem a cobrança" 201 "$CODIGO"
    ID_RES=$(campo id)
    docker compose start cobranca-service > /dev/null 2>&1
    esperar_api /cobrancas/tarifa
    req GET "/estadias/$ID_MERCOSUL/previa"
    checar "Gateway reencontra o serviço após reinício" 200 "$CODIGO"
    req POST "/estadias/$ID_RES/saida" > /dev/null
fi

# limpeza dos tickets que ficaram abertos
for id in $ID_MERCOSUL $ID_AVULSO2; do req POST "/estadias/$id/saida"; done

echo
TOTAL=$((PASSOU + FALHOU))
if [[ $FALHOU -eq 0 ]]; then
    echo "${VERDE}${NEGRITO}✔ $PASSOU de $TOTAL verificações passaram${RESET}"
else
    echo "${VERMELHO}${NEGRITO}✘ $FALHOU de $TOTAL verificações falharam${RESET}"
fi
exit $(( FALHOU > 0 ))
