'use strict';

// Todas as chamadas passam pelo gateway (/api/...), que roteia para o microserviço certo
const API = '/api';
const ATUALIZACAO_MS = 30_000;

const estado = {
    tarifa: null,          // { valorFracao, minutosFracao, planos[] }
    patio: [],             // estadias abertas
    assinaturas: [],
    vigentesPorPlaca: new Map(),
    planoSelecionado: 'MENSAL',
    ultimaEntradaId: null,
};

const CORES = [
    { nome: 'Branco', hex: '#FFFFFF' },
    { nome: 'Preto', hex: '#111827' },
    { nome: 'Prata', hex: '#C0C6CF' },
    { nome: 'Cinza', hex: '#6B7280' },
    { nome: 'Vermelho', hex: '#DC2626' },
    { nome: 'Azul', hex: '#2563EB' },
];

const $ = (seletor) => document.querySelector(seletor);

// ---------- Utilidades ----------

async function api(caminho, opcoes = {}) {
    const resposta = await fetch(API + caminho, {
        headers: { 'Content-Type': 'application/json' },
        ...opcoes,
        body: opcoes.body ? JSON.stringify(opcoes.body) : undefined,
    });
    const corpo = resposta.status === 204 ? null : await resposta.json().catch(() => null);
    if (!resposta.ok) {
        const mensagem = corpo?.detail
            || (resposta.status >= 502 ? 'Serviço temporariamente indisponível. Tente novamente em instantes.' : 'Não foi possível concluir a operação.');
        throw new Error(mensagem);
    }
    return corpo;
}

function esc(valor) {
    return String(valor ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const fmtMoeda = (v) => moeda.format(Number(v ?? 0));
const fmtHora = (d) => new Date(d).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
const fmtData = (d) => new Date(d + (d.length === 10 ? 'T00:00:00' : '')).toLocaleDateString('pt-BR');
const fmtDataHora = (d) => `${fmtData(d)} ${fmtHora(d)}`;

function fmtDuracao(minutos) {
    const h = Math.floor(minutos / 60);
    const m = Math.floor(minutos % 60);
    if (h === 0) return `${m} min`;
    return `${h}h ${String(m).padStart(2, '0')}min`;
}

const minutosDesde = (inicio) => Math.max(0, (Date.now() - new Date(inicio).getTime()) / 60000);

// Placa antiga (ABC1234) ganha hífen; Mercosul (ABC1D23) fica como está
function fmtPlaca(placa) {
    return /^[A-Z]{3}\d{4}$/.test(placa) ? `${placa.slice(0, 3)}-${placa.slice(3)}` : placa;
}

function fmtTelefone(digitos) {
    const d = String(digitos).replace(/\D/g, '').replace(/^55(?=\d{10,11}$)/, '');
    if (d.length === 11) return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`;
    if (d.length === 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`;
    return d;
}

function linkWhatsapp(digitos, mensagem) {
    const numero = String(digitos).length <= 11 ? `55${digitos}` : digitos;
    return `https://wa.me/${numero}?text=${encodeURIComponent(mensagem)}`;
}

const placaValida = (p) => /^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$/.test(p);
const limparPlaca = (p) => p.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 7);

function placaHtml(placa) {
    return `<span class="placa" aria-label="Placa ${esc(placa)}"><span class="placa__faixa">BRASIL</span><span class="placa__numero">${esc(fmtPlaca(placa))}</span></span>`;
}

function avisar(mensagem, erro = false) {
    const el = document.createElement('div');
    el.className = 'aviso' + (erro ? ' aviso--erro' : '');
    el.setAttribute('role', erro ? 'alert' : 'status');
    el.textContent = mensagem;
    $('#avisos').append(el);
    setTimeout(() => el.remove(), erro ? 6000 : 3500);
}

function mostrarErro(seletor, mensagem) {
    const el = $(seletor);
    el.textContent = mensagem || '';
    el.hidden = !mensagem;
}

async function comBotaoOcupado(botao, textoOcupado, acao) {
    const original = botao.textContent;
    botao.disabled = true;
    botao.textContent = textoOcupado;
    try {
        return await acao();
    } finally {
        botao.disabled = false;
        botao.textContent = original;
    }
}

// ---------- Navegação ----------

function navegar() {
    const secao = (location.hash || '#patio').slice(1);
    const valida = ['patio', 'planos', 'historico'].includes(secao) ? secao : 'patio';
    document.querySelectorAll('[data-secao]').forEach((s) => { s.hidden = s.dataset.secao !== valida; });
    document.querySelectorAll('[data-aba]').forEach((a) => {
        if (a.dataset.aba === valida) a.setAttribute('aria-current', 'page');
        else a.removeAttribute('aria-current');
    });
    if (valida === 'planos') carregarAssinaturas();
    if (valida === 'historico') carregarHistorico();
}

function atualizarRelogio() {
    const agora = new Date();
    $('#relogio-hora').textContent = agora.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
    const data = agora.toLocaleDateString('pt-BR', { weekday: 'long', day: '2-digit', month: 'long' });
    $('#relogio-data').textContent = data.charAt(0).toUpperCase() + data.slice(1);
}

// ---------- Resumo e tarifa ----------

async function carregarResumo() {
    try {
        const r = await api('/estadias/resumo');
        $('#kpi-patio').textContent = r.veiculosNoPatio;
        $('#kpi-entradas').textContent = r.entradasHoje;
        $('#kpi-faturamento').textContent = fmtMoeda(r.faturamentoHoje);
    } catch { /* indicadores são secundários; a lista mostra o erro */ }
}

async function carregarTarifa() {
    try {
        estado.tarifa = await api('/cobrancas/tarifa');
        const t = estado.tarifa;
        $('#tarifa-resumo').textContent = `Avulso: ${fmtMoeda(t.valorFracao)} a cada ${t.minutosFracao} min`;
        renderizarPrecos();
        renderizarOpcoesPlano();
    } catch (e) {
        $('#tarifa-resumo').textContent = 'Tarifa indisponível no momento';
    }
}

function estimarAvulso(minutos) {
    if (!estado.tarifa) return null;
    const fracoes = Math.max(1, Math.ceil((minutos * 60) / (estado.tarifa.minutosFracao * 60)));
    return fracoes * Number(estado.tarifa.valorFracao);
}

// ---------- Pátio ----------

const formEntrada = $('#form-entrada');

function montarCores() {
    const grupo = $('#cores');
    grupo.innerHTML = CORES.map((c) => `
        <button type="button" class="cor" role="radio" aria-checked="false" data-cor="${c.nome}">
            <span class="cor__amostra" style="background:${c.hex}"></span>${c.nome}
        </button>`).join('');
    grupo.addEventListener('click', (ev) => {
        const botao = ev.target.closest('.cor');
        if (!botao) return;
        $('#cor').value = botao.dataset.cor;
        marcarCor(botao.dataset.cor);
    });
    $('#cor').addEventListener('input', (ev) => marcarCor(ev.target.value));
}

function marcarCor(nome) {
    document.querySelectorAll('.cor').forEach((b) => {
        b.setAttribute('aria-checked', String(b.dataset.cor.toLowerCase() === nome.trim().toLowerCase()));
    });
}

function aplicarMascaras() {
    document.querySelectorAll('.entrada-placa').forEach((input) => {
        input.addEventListener('input', () => {
            input.value = limparPlaca(input.value);
            input.removeAttribute('aria-invalid');
        });
    });
    const whats = $('#whatsapp');
    whats.addEventListener('input', () => {
        const d = whats.value.replace(/\D/g, '').slice(0, 11);
        if (d.length > 6) whats.value = `(${d.slice(0, 2)}) ${d.slice(2, d.length - 4)}-${d.slice(-4)}`;
        else if (d.length > 2) whats.value = `(${d.slice(0, 2)}) ${d.slice(2)}`;
        else whats.value = d;
        whats.removeAttribute('aria-invalid');
    });
}

function validarEntrada(dados) {
    const erros = [];
    const marcar = (id, msg) => { $(id).setAttribute('aria-invalid', 'true'); erros.push(msg); };
    if (!placaValida(dados.placa)) marcar('#placa', 'Informe uma placa válida (ex: ABC1D23).');
    if (!dados.modelo) marcar('#modelo', 'Informe o modelo.');
    if (!dados.cor) marcar('#cor', 'Selecione ou digite a cor.');
    const digitos = dados.whatsappCondutor.replace(/\D/g, '');
    if (digitos.length < 10 || digitos.length > 11) marcar('#whatsapp', 'Informe o WhatsApp com DDD.');
    return erros;
}

formEntrada.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    formEntrada.querySelectorAll('[aria-invalid]').forEach((el) => el.removeAttribute('aria-invalid'));
    const dados = {
        placa: limparPlaca($('#placa').value),
        modelo: $('#modelo').value.trim(),
        cor: $('#cor').value.trim(),
        whatsappCondutor: $('#whatsapp').value.trim(),
    };
    const erros = validarEntrada(dados);
    if (erros.length) {
        mostrarErro('#erro-entrada', erros[0]);
        formEntrada.querySelector('[aria-invalid="true"]').focus();
        return;
    }
    mostrarErro('#erro-entrada', '');

    await comBotaoOcupado($('#botao-entrada'), 'Registrando…', async () => {
        try {
            const estadia = await api('/estadias/entrada', { method: 'POST', body: dados });
            estado.ultimaEntradaId = estadia.id;
            avisar(`Entrada de ${fmtPlaca(estadia.placa)} registrada às ${fmtHora(estadia.entrada)}`);
            formEntrada.reset();
            marcarCor('');
            $('#placa').focus();
            await Promise.all([carregarPatio(), carregarResumo()]);
        } catch (e) {
            mostrarErro('#erro-entrada', e.message);
        }
    });
});

async function carregarPatio() {
    try {
        estado.patio = await api('/estadias?status=ABERTA');
        renderizarPatio();
    } catch (e) {
        avisar(e.message, true);
    }
}

async function carregarVigentes() {
    try {
        estado.assinaturas = await api('/assinaturas');
        estado.vigentesPorPlaca = new Map(estado.assinaturas.filter((a) => a.vigente).map((a) => [a.placa, a]));
    } catch { /* sem selo de plano se o serviço falhar */ }
}

function renderizarPatio() {
    const filtro = limparPlaca($('#busca-patio').value);
    const lista = estado.patio.filter((e) => e.placa.includes(filtro));
    $('#patio-vazio').hidden = lista.length > 0;
    if (!estado.patio.length) $('#patio-vazio').querySelector('p').innerHTML = '<strong>Pátio vazio</strong><br>As entradas registradas aparecem aqui.';
    else if (!lista.length) $('#patio-vazio').querySelector('p').innerHTML = `<strong>Nenhuma placa com “${esc(filtro)}”</strong>`;

    $('#lista-patio').innerHTML = lista.map((e) => {
        const minutos = minutosDesde(e.entrada);
        const plano = estado.vigentesPorPlaca.get(e.placa);
        const estimado = plano ? 'Plano' : fmtMoeda(estimarAvulso(minutos));
        const selo = plano
            ? `<span class="selo selo--plano">★ ${esc(plano.descricaoPlano)}</span>`
            : '<span class="selo selo--avulso">Avulso</span>';
        return `
        <li class="vaga${e.id === estado.ultimaEntradaId ? ' vaga--nova' : ''}">
            <div class="vaga__topo">
                <div>
                    ${placaHtml(e.placa)}
                    <div class="vaga__veiculo">${esc(e.modelo)} · ${esc(e.cor)}</div>
                </div>
                ${selo}
            </div>
            <div class="vaga__tempos">
                <div class="vaga__tempo"><span class="vaga__tempo-rotulo">Entrada</span><span class="vaga__tempo-valor">${fmtHora(e.entrada)}</span></div>
                <div class="vaga__tempo"><span class="vaga__tempo-rotulo">Tempo</span><span class="vaga__tempo-valor">${fmtDuracao(minutos)}</span></div>
                <div class="vaga__tempo"><span class="vaga__tempo-rotulo">Estimado</span><span class="vaga__tempo-valor">${estimado}</span></div>
            </div>
            <button class="botao botao--escuro" data-saida="${e.id}">Registrar saída</button>
        </li>`;
    }).join('');
}

$('#busca-patio').addEventListener('input', renderizarPatio);
$('#lista-patio').addEventListener('click', (ev) => {
    const botao = ev.target.closest('[data-saida]');
    if (botao) abrirSaida(Number(botao.dataset.saida));
});

// ---------- Saída (prévia → confirmação → recibo) ----------

const dialogo = $('#dialogo-saida');

async function abrirSaida(id) {
    $('#saida-conteudo').innerHTML = '<div class="recibo__topo"><span class="recibo__titulo">Saída</span></div><p class="carregando">Calculando valor…</p>';
    dialogo.showModal();
    try {
        const previa = await api(`/estadias/${id}/previa`);
        renderizarRecibo(previa, false);
    } catch (e) {
        $('#saida-conteudo').innerHTML = `
            <div class="recibo__topo"><span class="recibo__titulo">Saída</span></div>
            <div class="recibo__corpo">
                <p class="erro-form">${esc(e.message)}</p>
                <div class="recibo__acoes recibo__acoes--uma"><button class="botao botao--secundario" data-fechar>Fechar</button></div>
            </div>`;
    }
}

function renderizarRecibo(estadia, finalizada) {
    const plano = estadia.tipoCobranca === 'PLANO';
    const nomePlano = estado.tarifa?.planos.find((p) => p.tipo === estadia.plano)?.descricao ?? estadia.plano;
    const valorFracao = estado.tarifa ? fmtMoeda(estado.tarifa.valorFracao) : '';

    const cobranca = plano
        ? `<p class="recibo__aviso">★ Coberto pelo plano ${esc(nomePlano)}. Nada a cobrar.</p>`
        : `<dt>Frações de ${estado.tarifa?.minutosFracao ?? 15} min</dt><dd>${estadia.fracoes} × ${valorFracao}</dd>`;

    const acoes = finalizada
        ? `<p class="recibo__sucesso">✓ Saída registrada às ${fmtHora(estadia.saida)}</p>
           <div class="recibo__acoes">
               <button class="botao botao--secundario" data-fechar>Concluir</button>
               <a class="botao botao--whatsapp" target="_blank" rel="noopener" href="${esc(linkWhatsapp(estadia.whatsappCondutor, mensagemRecibo(estadia, nomePlano)))}">Enviar recibo no WhatsApp</a>
           </div>`
        : `<div class="recibo__acoes">
               <button class="botao botao--secundario" data-fechar>Cancelar</button>
               <button class="botao botao--primario" data-confirmar="${estadia.id}">${plano ? 'Confirmar saída' : 'Confirmar e cobrar'}</button>
           </div>`;

    $('#saida-conteudo').innerHTML = `
        <div class="recibo__topo">
            <span class="recibo__titulo" id="saida-titulo">${finalizada ? 'Recibo' : 'Registrar saída'}</span>
            ${placaHtml(estadia.placa)}
        </div>
        <div class="recibo__corpo">
            <dl class="recibo__linhas">
                <dt>Veículo</dt><dd>${esc(estadia.modelo)} · ${esc(estadia.cor)}</dd>
                <dt>Condutor</dt><dd>${esc(fmtTelefone(estadia.whatsappCondutor))}</dd>
                <dt>Entrada</dt><dd>${fmtDataHora(estadia.entrada)}</dd>
                <dt>Saída</dt><dd>${fmtDataHora(estadia.saida)}${finalizada ? '' : ' (agora)'}</dd>
                <dt>Permanência</dt><dd>${fmtDuracao(estadia.minutosPermanencia)}</dd>
                ${plano ? '' : cobranca}
            </dl>
            ${plano ? cobranca : ''}
            <div class="recibo__total">
                <span class="recibo__total-rotulo">Total</span>
                <span class="recibo__total-valor">${fmtMoeda(estadia.valorTotal)}</span>
            </div>
            ${acoes}
        </div>`;

    $('#saida-conteudo').querySelector(finalizada ? '.botao--whatsapp' : '[data-confirmar]').focus();
}

function mensagemRecibo(e, nomePlano) {
    const linhas = [
        '*VagaCerta · Recibo de estacionamento*',
        `Placa: ${fmtPlaca(e.placa)} (${e.modelo} ${e.cor})`,
        `Entrada: ${fmtDataHora(e.entrada)}`,
        `Saída: ${fmtDataHora(e.saida)}`,
        `Permanência: ${fmtDuracao(e.minutosPermanencia)}`,
        e.tipoCobranca === 'PLANO' ? `Plano ${nomePlano}: sem cobrança` : `Valor: ${fmtMoeda(e.valorTotal)}`,
        'Obrigado e volte sempre!',
    ];
    return linhas.join('\n');
}

dialogo.addEventListener('click', async (ev) => {
    if (ev.target === dialogo || ev.target.closest('[data-fechar]')) {
        dialogo.close();
        return;
    }
    const confirmar = ev.target.closest('[data-confirmar]');
    if (!confirmar) return;
    await comBotaoOcupado(confirmar, 'Registrando…', async () => {
        try {
            const fechada = await api(`/estadias/${confirmar.dataset.confirmar}/saida`, { method: 'POST' });
            renderizarRecibo(fechada, true);
            await Promise.all([carregarPatio(), carregarResumo()]);
        } catch (e) {
            avisar(e.message, true);
        }
    });
});

// ---------- Planos ----------

function renderizarPrecos() {
    const t = estado.tarifa;
    const mensal = t.planos.find((p) => p.tipo === 'MENSAL');
    const melhor = t.planos.reduce((a, b) => (b.valor / b.meses < a.valor / a.meses ? b : a));
    const avulso = `
        <div class="preco preco--avulso">
            <span class="preco__nome">Avulso</span>
            <span class="preco__valor">${fmtMoeda(t.valorFracao)}</span>
            <span class="preco__detalhe">a cada ${t.minutosFracao} min iniciados</span>
            <span class="preco__detalhe">1 hora = ${fmtMoeda(estimarAvulso(60))}</span>
        </div>`;
    const planos = t.planos.map((p) => {
        const porMes = p.valor / p.meses;
        const economia = mensal && p.meses > 1 ? Math.round((1 - porMes / mensal.valor) * 100) : 0;
        return `
        <div class="preco${p.tipo === melhor.tipo && p.meses > 1 ? ' preco--melhor' : ''}">
            ${p.tipo === melhor.tipo && p.meses > 1 ? '<span class="preco__fita">Melhor valor</span>' : ''}
            <span class="preco__nome">${esc(p.descricao)}</span>
            <span class="preco__valor">${fmtMoeda(p.valor)}</span>
            <span class="preco__detalhe">${p.meses === 1 ? '1 mês' : `${p.meses} meses · ${fmtMoeda(porMes)}/mês`}</span>
            ${economia > 0 ? `<span class="preco__economia">Economia de ${economia}%</span>` : ''}
        </div>`;
    }).join('');
    $('#precos').innerHTML = avulso + planos;
}

function renderizarOpcoesPlano() {
    $('#opcoes-plano').innerHTML = estado.tarifa.planos.map((p) => `
        <button type="button" class="opcao-plano" role="radio" data-plano="${p.tipo}" aria-checked="${p.tipo === estado.planoSelecionado}">
            <span class="opcao-plano__nome">${esc(p.descricao)}</span>
            <span class="opcao-plano__valor">${fmtMoeda(p.valor)}</span>
        </button>`).join('');
}

$('#opcoes-plano').addEventListener('click', (ev) => {
    const opcao = ev.target.closest('[data-plano]');
    if (!opcao) return;
    estado.planoSelecionado = opcao.dataset.plano;
    renderizarOpcoesPlano();
    $(`[data-plano="${estado.planoSelecionado}"]`).focus();
});

$('#form-plano').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    const placa = limparPlaca($('#plano-placa').value);
    const nomeCliente = $('#plano-nome').value.trim();
    if (!placaValida(placa)) {
        $('#plano-placa').setAttribute('aria-invalid', 'true');
        mostrarErro('#erro-plano', 'Informe uma placa válida (ex: ABC1D23).');
        $('#plano-placa').focus();
        return;
    }
    if (!nomeCliente) {
        mostrarErro('#erro-plano', 'Informe o nome do cliente.');
        $('#plano-nome').focus();
        return;
    }
    mostrarErro('#erro-plano', '');

    await comBotaoOcupado($('#botao-plano'), 'Contratando…', async () => {
        try {
            const a = await api('/assinaturas', { method: 'POST', body: { placa, nomeCliente, tipoPlano: estado.planoSelecionado } });
            avisar(`Plano ${a.descricaoPlano} de ${fmtPlaca(a.placa)} válido de ${fmtData(a.inicio)} a ${fmtData(a.fim)}`);
            $('#form-plano').reset();
            await carregarAssinaturas();
            renderizarPatio();
        } catch (e) {
            mostrarErro('#erro-plano', e.message);
        }
    });
});

function statusAssinatura(a) {
    if (a.cancelada) return ['cancelada', 'Cancelada'];
    if (a.vigente) return ['vigente', 'Vigente'];
    const hoje = new Date().toLocaleDateString('sv-SE'); // AAAA-MM-DD no fuso local
    return a.inicio > hoje ? ['agendada', 'Agendada'] : ['vencida', 'Vencida'];
}

async function carregarAssinaturas() {
    await carregarVigentes();
    renderizarAssinaturas();
}

function renderizarAssinaturas() {
    const filtro = $('#busca-assinaturas').value.trim().toUpperCase();
    const filtroPlaca = filtro.replace(/[^A-Z0-9]/g, '');
    const lista = estado.assinaturas.filter((a) => !filtro
        || (filtroPlaca && a.placa.includes(filtroPlaca))
        || a.nomeCliente.toUpperCase().includes(filtro));
    $('#assinaturas-vazio').hidden = lista.length > 0;
    $('#lista-assinaturas').innerHTML = lista.map((a) => {
        const [classe, rotulo] = statusAssinatura(a);
        const podeCancelar = classe === 'vigente' || classe === 'agendada';
        return `
        <tr>
            <td class="placa-texto">${esc(fmtPlaca(a.placa))}</td>
            <td>${esc(a.nomeCliente)}</td>
            <td>${esc(a.descricaoPlano)} · ${fmtMoeda(a.valorPago)}</td>
            <td>${fmtData(a.inicio)} a ${fmtData(a.fim)}</td>
            <td><span class="selo selo--${classe}">${rotulo}</span></td>
            <td class="num">${podeCancelar ? `<button class="botao botao--perigo-texto" data-cancelar="${a.id}" data-placa="${esc(a.placa)}">Cancelar</button>` : ''}</td>
        </tr>`;
    }).join('');
}

$('#busca-assinaturas').addEventListener('input', renderizarAssinaturas);
$('#lista-assinaturas').addEventListener('click', async (ev) => {
    const botao = ev.target.closest('[data-cancelar]');
    if (!botao) return;
    if (!confirm(`Cancelar o plano da placa ${fmtPlaca(botao.dataset.placa)}? Ela passará a pagar como avulso.`)) return;
    try {
        await api(`/assinaturas/${botao.dataset.cancelar}`, { method: 'DELETE' });
        avisar('Assinatura cancelada');
        await carregarAssinaturas();
        renderizarPatio();
    } catch (e) {
        avisar(e.message, true);
    }
});

// ---------- Histórico ----------

async function carregarHistorico() {
    try {
        const lista = await api('/estadias?status=FINALIZADA');
        $('#historico-vazio').hidden = lista.length > 0;
        $('#lista-historico').innerHTML = lista.map((e) => `
            <tr>
                <td class="placa-texto">${esc(fmtPlaca(e.placa))}</td>
                <td>${esc(e.modelo)} · ${esc(e.cor)}</td>
                <td>${fmtDataHora(e.entrada)}</td>
                <td>${fmtDataHora(e.saida)}</td>
                <td>${fmtDuracao(e.minutosPermanencia)}</td>
                <td>${e.tipoCobranca === 'PLANO'
                    ? '<span class="selo selo--plano">Plano</span>'
                    : `<span class="selo selo--avulso">${e.fracoes} fraç${e.fracoes === 1 ? 'ão' : 'ões'}</span>`}</td>
                <td class="num"><strong>${fmtMoeda(e.valorTotal)}</strong></td>
            </tr>`).join('');
    } catch (e) {
        avisar(e.message, true);
    }
}

// ---------- Início ----------

async function iniciar() {
    montarCores();
    aplicarMascaras();
    atualizarRelogio();
    setInterval(atualizarRelogio, 1000);
    window.addEventListener('hashchange', navegar);
    navegar();

    await Promise.all([carregarTarifa(), carregarVigentes()]);
    await Promise.all([carregarPatio(), carregarResumo()]);

    // Mantém tempo decorrido, estimativas e indicadores atualizados
    setInterval(() => {
        carregarResumo();
        carregarPatio();
    }, ATUALIZACAO_MS);
}

iniciar();
