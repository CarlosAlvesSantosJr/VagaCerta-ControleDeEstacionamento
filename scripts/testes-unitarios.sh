#!/usr/bin/env bash
# Roda os testes unitários dos microserviços dentro de um contêiner Maven,
# sem precisar de JDK ou Maven instalados na máquina.
#
# Uso:  ./scripts/testes-unitarios.sh                  (todos os serviços)
#       ./scripts/testes-unitarios.sh cobranca-service (só um serviço)

set -euo pipefail
cd "$(dirname "$0")/.."

IMAGEM="maven:3.9.9-eclipse-temurin-21"
SERVICOS=("$@")
[[ ${#SERVICOS[@]} -eq 0 ]] && SERVICOS=(veiculo-service estacionamento-service cobranca-service)

# No Git Bash do Windows, o Docker precisa do caminho no formato C:/...
if pwd -W > /dev/null 2>&1; then
    RAIZ="$(pwd -W)"; export MSYS_NO_PATHCONV=1
else
    RAIZ="$(pwd)"
fi

# Volume nomeado reaproveita as dependências baixadas entre execuções
docker volume create vagacerta-m2 > /dev/null

FALHAS=()
for servico in "${SERVICOS[@]}"; do
    echo "▸ $servico"
    if ! docker run --rm \
            -v "$RAIZ/$servico":/app \
            -v vagacerta-m2:/root/.m2 \
            -w /app "$IMAGEM" mvn -B -q test; then
        FALHAS+=("$servico")
    fi
done

if [[ ${#FALHAS[@]} -gt 0 ]]; then
    echo "✘ Falharam: ${FALHAS[*]}"
    exit 1
fi
echo "✔ Todos os testes unitários passaram"
