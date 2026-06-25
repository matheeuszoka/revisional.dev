import React, { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
    Box, Typography, Paper, Grid, TextField, Button, Stack, Divider,
    CircularProgress, MenuItem, FormControlLabel, Checkbox, Chip, Alert, AlertTitle,
    List, ListItem, ListItemText, ListItemIcon, InputAdornment, IconButton, Tooltip,
    Dialog, DialogTitle, DialogContent, Link,
    Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
} from '@mui/material';
import SaveIcon from '@mui/icons-material/Save';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import AssessmentIcon from '@mui/icons-material/Assessment';
import FactCheckIcon from '@mui/icons-material/FactCheck';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import FolderZipIcon from '@mui/icons-material/FolderZip';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import VisibilityIcon from '@mui/icons-material/Visibility';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import CloseIcon from '@mui/icons-material/Close';
import DownloadIcon from '@mui/icons-material/Download';

import {
    getCaso, criarCaso, atualizarCaso, analisarCaso,
    uploadDocumento, listarDocumentos, getDocumentoArquivo, getAuditoria, getRelatorio, getPacoteZip,
} from '../../services/casos';
import { toastSuccess, toastError, alertValidacao } from '../../services/alerts';
import { formatCpf, isValidCpf, onlyDigits } from '../../services/cpf';
import { formatMoeda, numberToMoeda, parseMoeda } from '../../services/moeda';

const CAMPOS_MOEDA = [
    'valorVeiculo', 'valorEntrada', 'valorFinanciado', 'valorLiquidoLiberado', 'valorParcela',
    'iof', 'tarifaCadastro', 'tarifaAvaliacaoBem', 'tarifaRegistroContrato', 'gravame', 'seguro', 'outrosEncargos',
];

const MODALIDADES = [
    'Financiamento de veículos - Pessoa Física',
    'Financiamento de veículos - Pessoa Jurídica',
    'Crédito pessoal',
    'Outro',
];

const SectionTitle = ({ children }) => (
    <Typography variant="subtitle1" fontWeight={700} color="secondary.main" sx={{ mt: 1, mb: 1 }}>
        {children}
    </Typography>
);

const contratoVazio = {
    clienteNome: '', clienteCpf: '',
    instituicao: '', instituicaoCnpj: '', contratoNumero: '',
    modalidade: 'Financiamento de veículos - Pessoa Física', dataContrato: '',
    veiculoDescricao: '', valorVeiculo: '', valorEntrada: '', valorFinanciado: '', valorLiquidoLiberado: '',
    prazoMeses: '', valorParcela: '',
    taxaJurosMensalPct: '', taxaJurosAnualPct: '', cetMensalPct: '', cetAnualPct: '',
    iof: '', tarifaCadastro: '', tarifaAvaliacaoBem: '', tarifaRegistroContrato: '',
    gravame: '', seguro: '', outrosEncargos: '', descricaoOutrosEncargos: '',
    observacoes: '',
};

export default function CasoForm() {
    const navigate = useNavigate();
    const { id } = useParams();
    const editando = id && id !== 'novo';

    const [titulo, setTitulo] = useState('');
    const [contrato, setContrato] = useState(contratoVazio);
    const [loading, setLoading] = useState(!!editando);
    const [salvando, setSalvando] = useState(false);

    // --- Análise (motor financeiro) ---
    const [resultado, setResultado] = useState(null);
    const [usarBcb, setUsarBcb] = useState(true);
    const [mercado, setMercado] = useState({
        fonte: '', dataReferencia: '',
        taxaMensalPct: '', taxaAnualPct: '',
        taxaMensalInstituicaoPct: '', taxaAnualInstituicaoPct: '',
        observacao: '',
    });
    const [analisando, setAnalisando] = useState(false);

    // --- Auditoria pericial (AuditPackage) ---
    const [auditoria, setAuditoria] = useState(null);
    const [gerandoAudit, setGerandoAudit] = useState(false);

    // --- Relatórios PDF ---
    const [gerandoRel, setGerandoRel] = useState('');
    const [gerandoZip, setGerandoZip] = useState(false);

    // --- Upload de documento (OCR + IA) ---
    const [enviandoDoc, setEnviandoDoc] = useState(false);
    const [forcarOcr, setForcarOcr] = useState(false);
    const fileRef = useRef(null);
    const [documentos, setDocumentos] = useState([]);

    // --- Conferência OCR: campos extraídos com confiança (fora do form, só leitura) ---
    const [extraidos, setExtraidos] = useState({});

    // --- Visualizador de documento anexado ---
    const [viewer, setViewer] = useState({ aberto: false, url: '', tipo: '', nome: '', carregando: false });

    const carregarDocumentos = async () => {
        try {
            setDocumentos(await listarDocumentos(id));
        } catch {
            /* silencioso */
        }
    };

    const abrirVisualizador = async (doc) => {
        setViewer({ aberto: true, url: '', tipo: '', nome: doc.nomeOriginal, carregando: true });
        try {
            const blob = await getDocumentoArquivo(id, doc.id);
            const url = URL.createObjectURL(blob);
            setViewer({ aberto: true, url, tipo: blob.type, nome: doc.nomeOriginal, carregando: false });
        } catch {
            toastError('Falha ao abrir o documento.');
            setViewer({ aberto: false, url: '', tipo: '', nome: '', carregando: false });
        }
    };

    const fecharVisualizador = () => {
        if (viewer.url) URL.revokeObjectURL(viewer.url);
        setViewer({ aberto: false, url: '', tipo: '', nome: '', carregando: false });
    };

    // Aplica um contrato vindo da API ao estado do form (máscaras de CPF/moeda).
    const aplicarContrato = (contratoApi) => {
        const c = { ...contratoVazio, ...(contratoApi || {}) };
        // camposExtraidos/metadadosExtracao não são editáveis: separa p/ conferência, tira do form.
        setExtraidos((contratoApi && contratoApi.camposExtraidos) || {});
        delete c.camposExtraidos;
        delete c.metadadosExtracao;
        c.clienteCpf = formatCpf(c.clienteCpf);
        CAMPOS_MOEDA.forEach((k) => { c[k] = numberToMoeda(c[k]); });
        setContrato(c);
    };

    // Referência de mercado vinda da API → estado do form (números viram string p/ inputs).
    const aplicarMercado = (m) => {
        if (!m) return;
        const txt = (v) => (v == null ? '' : String(v));
        setMercado({
            fonte: m.fonte || '',
            dataReferencia: m.dataReferencia || '',
            taxaMensalPct: txt(m.taxaMensalPct),
            taxaAnualPct: txt(m.taxaAnualPct),
            taxaMensalInstituicaoPct: txt(m.taxaMensalInstituicaoPct),
            taxaAnualInstituicaoPct: txt(m.taxaAnualInstituicaoPct),
            observacao: m.observacao || '',
        });
    };

    // "" → null; percentuais → Number. Devolve null se nada preenchido (deixa o back decidir BCB).
    const montarMercado = () => {
        const num = (v) => (v === '' || v == null ? null : Number(v));
        const out = {
            fonte: mercado.fonte?.trim() || null,
            dataReferencia: mercado.dataReferencia?.trim() || null,
            taxaMensalPct: num(mercado.taxaMensalPct),
            taxaAnualPct: num(mercado.taxaAnualPct),
            taxaMensalInstituicaoPct: num(mercado.taxaMensalInstituicaoPct),
            taxaAnualInstituicaoPct: num(mercado.taxaAnualInstituicaoPct),
            observacao: mercado.observacao?.trim() || null,
        };
        const algumPreenchido = Object.values(out).some((v) => v != null);
        return algumPreenchido ? out : null;
    };

    const setMercadoCampo = (campo) => (e) => setMercado({ ...mercado, [campo]: e.target.value });

    useEffect(() => {
        if (!editando) return;
        (async () => {
            try {
                const caso = await getCaso(id);
                setTitulo(caso.titulo || '');
                aplicarContrato(caso.contrato);
                aplicarMercado(caso.mercado);
                if (caso.resultado) setResultado(caso.resultado);
                carregarDocumentos();
            } finally {
                setLoading(false);
            }
        })();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [id, editando]);

    const handleUpload = async (e) => {
        const arquivo = e.target.files?.[0];
        if (fileRef.current) fileRef.current.value = '';
        if (!arquivo) return;
        setEnviandoDoc(true);
        try {
            const caso = await uploadDocumento(id, arquivo, forcarOcr);
            aplicarContrato(caso.contrato);
            if (caso.resultado) setResultado(caso.resultado);
            carregarDocumentos();
            toastSuccess('Documento processado. Confira os campos extraídos antes de analisar.');
        } catch (err) {
            const msg = err.response?.data?.message || err.response?.data?.error;
            toastError(msg || 'Falha ao processar o documento.');
        } finally {
            setEnviandoDoc(false);
        }
    };

    const set = (campo) => (e) => setContrato({ ...contrato, [campo]: e.target.value });
    const setCpf = (e) => setContrato({ ...contrato, clienteCpf: formatCpf(e.target.value) });
    const setMoeda = (campo) => (e) => setContrato({ ...contrato, [campo]: formatMoeda(e.target.value) });

    // Converte "" -> null; moeda "1.234,56" -> Number; demais numéricos -> Number
    const montarContrato = () => {
        const numericos = ['prazoMeses', 'taxaJurosMensalPct', 'taxaJurosAnualPct', 'cetMensalPct', 'cetAnualPct'];
        const out = {};
        Object.entries(contrato).forEach(([k, v]) => {
            if (CAMPOS_MOEDA.includes(k)) out[k] = parseMoeda(v);
            else if (numericos.includes(k)) out[k] = v === '' || v === null ? null : Number(v);
            else out[k] = v;
        });
        out.clienteCpf = onlyDigits(contrato.clienteCpf); // armazena só dígitos
        return out;
    };

    const handleSalvar = async () => {
        if (!titulo.trim()) {
            alertValidacao('Informe um título para o caso.');
            return;
        }
        if (contrato.clienteCpf && !isValidCpf(contrato.clienteCpf)) {
            alertValidacao('CPF do cliente inválido. Verifique os dígitos.');
            return;
        }
        setSalvando(true);
        try {
            const payload = { titulo: titulo.trim(), contrato: montarContrato() };
            if (editando) await atualizarCaso(id, payload);
            else await criarCaso(payload);
            toastSuccess(editando ? 'Caso atualizado.' : 'Caso criado.');
            navigate('/casos');
        } catch {
            /* interceptor mostra toast */
        } finally {
            setSalvando(false);
        }
    };

    const handleAnalisar = async () => {
        if (contrato.clienteCpf && !isValidCpf(contrato.clienteCpf)) {
            alertValidacao('CPF do cliente inválido. Verifique os dígitos.');
            return;
        }
        const mercadoPayload = montarMercado();
        setAnalisando(true);
        try {
            // Salva o contrato antes de analisar, garantindo consistência com o que foi auditado.
            await atualizarCaso(id, { titulo: titulo.trim(), contrato: montarContrato() });
            const caso = await analisarCaso(id, { contrato: montarContrato(), mercado: mercadoPayload, usarBcb });
            setResultado(caso.resultado);
            aplicarMercado(caso.mercado); // reflete taxa BCB buscada ou o que foi salvo
            setAuditoria(null); // invalida auditoria antiga; gere de novo sobre o novo resultado
            toastSuccess('Análise concluída.');
        } catch (err) {
            const msg = err.response?.data?.message || err.response?.data?.error;
            toastError(msg || 'Falha ao executar a análise.');
        } finally {
            setAnalisando(false);
        }
    };

    const handleAuditoria = async () => {
        setGerandoAudit(true);
        try {
            setAuditoria(await getAuditoria(id));
        } catch (err) {
            const msg = err.response?.data?.message || err.response?.data?.error;
            toastError(msg || 'Falha ao gerar a auditoria.');
        } finally {
            setGerandoAudit(false);
        }
    };

    const handleRelatorio = async (tipo) => {
        setGerandoRel(tipo);
        setViewer({ aberto: true, url: '', tipo: '', nome: `${tipo}.pdf`, carregando: true });
        try {
            const blob = await getRelatorio(id, tipo);
            const url = URL.createObjectURL(blob);
            setViewer({ aberto: true, url, tipo: blob.type || 'application/pdf', nome: `${tipo}_caso_${id}.pdf`, carregando: false });
        } catch {
            toastError('Falha ao gerar o relatório.');
            setViewer({ aberto: false, url: '', tipo: '', nome: '', carregando: false });
        } finally {
            setGerandoRel('');
        }
    };

    const handlePacoteZip = async () => {
        setGerandoZip(true);
        try {
            const blob = await getPacoteZip(id);
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `pacote_caso_${id}.zip`;
            document.body.appendChild(a);
            a.click();
            a.remove();
            URL.revokeObjectURL(url);
            toastSuccess('Pacote gerado.');
        } catch {
            toastError('Falha ao gerar o pacote ZIP.');
        } finally {
            setGerandoZip(false);
        }
    };

    const RELATORIOS = [
        { tipo: 'parecer', label: 'Parecer técnico-jurídico' },
        { tipo: 'gerencial', label: 'Gerencial executivo' },
        { tipo: 'notificacao', label: 'Notificação extrajudicial' },
        { tipo: 'elementos', label: 'Elementos p/ inicial' },
    ];

    const humanizar = (k) => k.replace(/_/g, ' ').replace(/\bpct\b/g, '(%)').replace(/^./, (c) => c.toUpperCase());
    const confColor = (c) => (c >= 0.7 ? 'success' : c >= 0.4 ? 'warning' : 'error');
    const fmtMemoria = (v) => {
        if (v == null) return '—';
        if (typeof v === 'number') return v.toLocaleString('pt-BR', { maximumFractionDigits: 6 });
        return String(v);
    };

    const fmtMoney = (v) => (v == null ? '—' : Number(v).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }));
    const fmtPct = (v) => (v == null ? '—' : `${Number(v).toFixed(2)}%`);
    const riscoColor = (c) => {
        if (!c) return 'default';
        if (c.startsWith('Indício técnico forte') || c.startsWith('Indicio tecnico forte')) return 'error';
        if (c.startsWith('Indício técnico moderado') || c.startsWith('Indicio tecnico moderado')) return 'warning';
        if (c.startsWith('Taxa acima')) return 'warning';
        if (c.startsWith('Sem indício') || c.startsWith('Sem indicio')) return 'success';
        return 'default';
    };

    // Auditoria: cores por score, status e severidade.
    const scoreColor = (s) => (s >= 80 ? 'success.main' : s >= 50 ? 'warning.main' : 'error.main');
    const statusColor = (st) => (st === 'OK' ? 'success' : st === 'PENDENTE' ? 'error' : 'warning');
    const sevColor = (sv) => ({ alto: 'error', medio: 'warning', info: 'info', ok: 'success' }[sv] || 'default');

    if (loading) return <Box sx={{ p: 6, textAlign: 'center' }}><CircularProgress /></Box>;

    return (
        <Box>
            <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 3 }}>
                <Button startIcon={<ArrowBackIcon />} onClick={() => navigate('/casos')} color="inherit">Voltar</Button>
                <Typography variant="h4" fontWeight="bold" color="#333">
                    {editando ? 'Editar Caso' : 'Novo Caso'}
                </Typography>
            </Stack>

            <Paper elevation={0} sx={{ p: { xs: 2, sm: 4 }, boxShadow: '0 2px 10px rgba(0,0,0,0.05)' }}>
                <TextField
                    label="Título do caso *"
                    fullWidth
                    value={titulo}
                    onChange={(e) => setTitulo(e.target.value)}
                    placeholder="Ex: Cliente X vs. Banco Y - Financiamento veículo"
                    sx={{ mb: 2 }}
                />

                {editando && (
                    <>
                        <Divider sx={{ my: 2 }} />
                        <SectionTitle>Documento do contrato</SectionTitle>
                        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }}>
                            <input
                                ref={fileRef}
                                type="file"
                                hidden
                                accept=".pdf,.jpg,.jpeg,.png,.tif,.tiff,.bmp,.txt"
                                onChange={handleUpload}
                            />
                            <Button
                                variant="outlined"
                                startIcon={!enviandoDoc && <UploadFileIcon />}
                                onClick={() => fileRef.current?.click()}
                                disabled={enviandoDoc}
                            >
                                {enviandoDoc ? <CircularProgress size={22} /> : 'Anexar e extrair (OCR + IA)'}
                            </Button>
                            <FormControlLabel
                                control={<Checkbox checked={forcarOcr} onChange={(e) => setForcarOcr(e.target.checked)} disabled={enviandoDoc} />}
                                label="Forçar OCR (PDF escaneado)"
                            />
                            <Typography variant="caption" color="text.secondary">
                                PDF, imagem ou TXT. A extração preenche só os campos vazios.
                            </Typography>
                        </Stack>

                        {documentos.length > 0 && (
                            <List dense sx={{ mt: 1 }}>
                                {documentos.map((d) => (
                                    <ListItem
                                        key={d.id}
                                        secondaryAction={
                                            <Tooltip title="Visualizar">
                                                <IconButton edge="end" onClick={() => abrirVisualizador(d)}>
                                                    <VisibilityIcon />
                                                </IconButton>
                                            </Tooltip>
                                        }
                                        sx={{ bgcolor: '#f7f9fc', borderRadius: 1, mb: 0.5 }}
                                    >
                                        <ListItemIcon sx={{ minWidth: 36 }}><DescriptionOutlinedIcon color="action" /></ListItemIcon>
                                        <ListItemText
                                            primary={d.nomeOriginal}
                                            secondary={d.criadoEm ? new Date(d.criadoEm).toLocaleString('pt-BR') : null}
                                            primaryTypographyProps={{ fontSize: '0.9rem' }}
                                        />
                                    </ListItem>
                                ))}
                            </List>
                        )}
                    </>
                )}

                <Divider sx={{ my: 2 }} />
                <SectionTitle>Cliente</SectionTitle>
                <Grid container spacing={2}>
                    <Grid size={{ xs: 12, sm: 8 }}><TextField label="Nome do cliente" fullWidth value={contrato.clienteNome} onChange={set('clienteNome')} /></Grid>
                    <Grid size={{ xs: 12, sm: 4 }}><TextField label="CPF do cliente" fullWidth value={contrato.clienteCpf} onChange={setCpf} inputProps={{ inputMode: 'numeric', maxLength: 14 }} /></Grid>
                </Grid>

                <SectionTitle>Contrato</SectionTitle>
                <Grid container spacing={2}>
                    <Grid size={{ xs: 12, sm: 6 }}><TextField label="Instituição financeira" fullWidth value={contrato.instituicao} onChange={set('instituicao')} /></Grid>
                    <Grid size={{ xs: 12, sm: 6 }}><TextField label="CNPJ da instituição" fullWidth value={contrato.instituicaoCnpj} onChange={set('instituicaoCnpj')} /></Grid>
                    <Grid size={{ xs: 12, sm: 3 }}><TextField label="Nº do contrato" fullWidth value={contrato.contratoNumero} onChange={set('contratoNumero')} /></Grid>
                    <Grid size={{ xs: 12, sm: 3 }}><TextField label="Data do contrato" type="date" InputLabelProps={{ shrink: true }} fullWidth value={contrato.dataContrato} onChange={set('dataContrato')} /></Grid>
                    <Grid size={{ xs: 12, sm: 6 }}>
                        <TextField select label="Modalidade" fullWidth value={contrato.modalidade} onChange={set('modalidade')}>
                            {MODALIDADES.map((m) => <MenuItem key={m} value={m}>{m}</MenuItem>)}
                        </TextField>
                    </Grid>
                </Grid>

                <SectionTitle>Veículo e valores</SectionTitle>
                <Grid container spacing={2}>
                    <Grid size={{ xs: 12, sm: 6 }}><TextField label="Descrição do veículo" fullWidth value={contrato.veiculoDescricao} onChange={set('veiculoDescricao')} /></Grid>
                    <Grid size={{ xs: 6, sm: 3 }}><TextField label="Valor do veículo" fullWidth value={contrato.valorVeiculo} onChange={setMoeda('valorVeiculo')} inputProps={{ inputMode: 'numeric' }} InputProps={{ startAdornment: <InputAdornment position="start">R$</InputAdornment> }} /></Grid>
                    <Grid size={{ xs: 6, sm: 3 }}><TextField label="Entrada" fullWidth value={contrato.valorEntrada} onChange={setMoeda('valorEntrada')} inputProps={{ inputMode: 'numeric' }} InputProps={{ startAdornment: <InputAdornment position="start">R$</InputAdornment> }} /></Grid>
                    <Grid size={{ xs: 6, sm: 4 }}><TextField label="Valor financiado" fullWidth value={contrato.valorFinanciado} onChange={setMoeda('valorFinanciado')} inputProps={{ inputMode: 'numeric' }} InputProps={{ startAdornment: <InputAdornment position="start">R$</InputAdornment> }} /></Grid>
                    <Grid size={{ xs: 6, sm: 4 }}><TextField label="Valor líquido liberado" fullWidth value={contrato.valorLiquidoLiberado} onChange={setMoeda('valorLiquidoLiberado')} inputProps={{ inputMode: 'numeric' }} InputProps={{ startAdornment: <InputAdornment position="start">R$</InputAdornment> }} /></Grid>
                    <Grid size={{ xs: 6, sm: 4 }}><TextField label="Prazo (meses)" type="number" fullWidth value={contrato.prazoMeses} onChange={set('prazoMeses')} /></Grid>
                    <Grid size={{ xs: 6, sm: 4 }}><TextField label="Valor da parcela" fullWidth value={contrato.valorParcela} onChange={setMoeda('valorParcela')} inputProps={{ inputMode: 'numeric' }} InputProps={{ startAdornment: <InputAdornment position="start">R$</InputAdornment> }} /></Grid>
                </Grid>

                <SectionTitle>Taxas e CET (se constarem no contrato)</SectionTitle>
                <Grid container spacing={2}>
                    <Grid size={{ xs: 6, sm: 3 }}><TextField label="Juros mensal (%)" type="number" fullWidth value={contrato.taxaJurosMensalPct} onChange={set('taxaJurosMensalPct')} /></Grid>
                    <Grid size={{ xs: 6, sm: 3 }}><TextField label="Juros anual (%)" type="number" fullWidth value={contrato.taxaJurosAnualPct} onChange={set('taxaJurosAnualPct')} /></Grid>
                    <Grid size={{ xs: 6, sm: 3 }}><TextField label="CET mensal (%)" type="number" fullWidth value={contrato.cetMensalPct} onChange={set('cetMensalPct')} /></Grid>
                    <Grid size={{ xs: 6, sm: 3 }}><TextField label="CET anual (%)" type="number" fullWidth value={contrato.cetAnualPct} onChange={set('cetAnualPct')} /></Grid>
                </Grid>

                <SectionTitle>Encargos, tarifas e seguros</SectionTitle>
                <Grid container spacing={2}>
                    <Grid size={{ xs: 6, sm: 3 }}><TextField label="IOF" fullWidth value={contrato.iof} onChange={setMoeda('iof')} inputProps={{ inputMode: 'numeric' }} InputProps={{ startAdornment: <InputAdornment position="start">R$</InputAdornment> }} /></Grid>
                    <Grid size={{ xs: 6, sm: 3 }}><TextField label="Tarifa de cadastro" fullWidth value={contrato.tarifaCadastro} onChange={setMoeda('tarifaCadastro')} inputProps={{ inputMode: 'numeric' }} InputProps={{ startAdornment: <InputAdornment position="start">R$</InputAdornment> }} /></Grid>
                    <Grid size={{ xs: 6, sm: 3 }}><TextField label="Avaliação do bem" fullWidth value={contrato.tarifaAvaliacaoBem} onChange={setMoeda('tarifaAvaliacaoBem')} inputProps={{ inputMode: 'numeric' }} InputProps={{ startAdornment: <InputAdornment position="start">R$</InputAdornment> }} /></Grid>
                    <Grid size={{ xs: 6, sm: 3 }}><TextField label="Registro do contrato" fullWidth value={contrato.tarifaRegistroContrato} onChange={setMoeda('tarifaRegistroContrato')} inputProps={{ inputMode: 'numeric' }} InputProps={{ startAdornment: <InputAdornment position="start">R$</InputAdornment> }} /></Grid>
                    <Grid size={{ xs: 6, sm: 3 }}><TextField label="Gravame" fullWidth value={contrato.gravame} onChange={setMoeda('gravame')} inputProps={{ inputMode: 'numeric' }} InputProps={{ startAdornment: <InputAdornment position="start">R$</InputAdornment> }} /></Grid>
                    <Grid size={{ xs: 6, sm: 3 }}><TextField label="Seguro" fullWidth value={contrato.seguro} onChange={setMoeda('seguro')} inputProps={{ inputMode: 'numeric' }} InputProps={{ startAdornment: <InputAdornment position="start">R$</InputAdornment> }} /></Grid>
                    <Grid size={{ xs: 6, sm: 3 }}><TextField label="Outros encargos" fullWidth value={contrato.outrosEncargos} onChange={setMoeda('outrosEncargos')} inputProps={{ inputMode: 'numeric' }} InputProps={{ startAdornment: <InputAdornment position="start">R$</InputAdornment> }} /></Grid>
                    <Grid size={{ xs: 12, sm: 3 }}><TextField label="Descrição outros encargos" fullWidth value={contrato.descricaoOutrosEncargos} onChange={set('descricaoOutrosEncargos')} /></Grid>
                </Grid>

                <SectionTitle>Observações</SectionTitle>
                <TextField label="Observações" fullWidth multiline minRows={2} value={contrato.observacoes} onChange={set('observacoes')} />

                {Object.keys(extraidos).length > 0 && (
                    <>
                        <SectionTitle>Conferência da extração (OCR / IA)</SectionTitle>
                        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1.5 }}>
                            Campos detectados no documento com nível de confiança. Confira e ajuste manualmente acima antes de analisar.
                        </Typography>
                        <TableContainer component={Box} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 2 }}>
                            <Table size="small">
                                <TableHead>
                                    <TableRow sx={{ bgcolor: '#f7f9fc' }}>
                                        <TableCell sx={{ fontWeight: 700 }}>Campo</TableCell>
                                        <TableCell sx={{ fontWeight: 700 }}>Valor extraído</TableCell>
                                        <TableCell sx={{ fontWeight: 700 }}>Confiança</TableCell>
                                        <TableCell sx={{ fontWeight: 700 }}>Origem</TableCell>
                                        <TableCell sx={{ fontWeight: 700 }}>Evidência</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {Object.entries(extraidos).map(([k, campo]) => {
                                        const conf = campo?.confianca ?? 0;
                                        return (
                                            <TableRow key={k} hover>
                                                <TableCell sx={{ fontWeight: 600 }}>{humanizar(campo?.nome || k)}</TableCell>
                                                <TableCell>{campo?.valor || '—'}</TableCell>
                                                <TableCell>
                                                    <Chip size="small" color={confColor(conf)} variant="outlined"
                                                          label={`${(conf * 100).toFixed(0)}%`} />
                                                </TableCell>
                                                <TableCell sx={{ color: 'text.secondary', fontSize: '0.8rem' }}>{campo?.origem || '—'}</TableCell>
                                                <TableCell sx={{ color: 'text.secondary', fontSize: '0.78rem', maxWidth: 220 }}>
                                                    <Tooltip title={campo?.evidencia || ''}>
                                                        <Typography noWrap variant="inherit">{campo?.evidencia || '—'}</Typography>
                                                    </Tooltip>
                                                </TableCell>
                                            </TableRow>
                                        );
                                    })}
                                </TableBody>
                            </Table>
                        </TableContainer>
                    </>
                )}

                <Divider sx={{ my: 3 }} />
                <Stack direction="row" justifyContent="flex-end" spacing={2}>
                    <Button onClick={() => navigate('/casos')} color="inherit" disabled={salvando}>Cancelar</Button>
                    <Button variant="contained" startIcon={<SaveIcon />} onClick={handleSalvar} disabled={salvando}>
                        {salvando ? <CircularProgress size={22} color="inherit" /> : 'Salvar'}
                    </Button>
                </Stack>
            </Paper>

            {editando && (
                <Paper elevation={0} sx={{ p: { xs: 2, sm: 4 }, mt: 3, boxShadow: '0 2px 10px rgba(0,0,0,0.05)' }}>
                    <SectionTitle>Análise financeira</SectionTitle>
                    <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1.5 }}>
                        Referência de mercado manual (opcional). Em branco com BCB marcado, busca a taxa média na série SGS 25471.
                    </Typography>
                    <Grid container spacing={2}>
                        <Grid size={{ xs: 12, sm: 6 }}><TextField label="Fonte da referência" fullWidth value={mercado.fonte} onChange={setMercadoCampo('fonte')} placeholder="BCB SGS 25471" /></Grid>
                        <Grid size={{ xs: 12, sm: 6 }}><TextField label="Data de referência" type="date" InputLabelProps={{ shrink: true }} fullWidth value={mercado.dataReferencia} onChange={setMercadoCampo('dataReferencia')} /></Grid>
                        <Grid size={{ xs: 6, sm: 3 }}><TextField label="Taxa mensal (%)" type="number" fullWidth value={mercado.taxaMensalPct} onChange={setMercadoCampo('taxaMensalPct')} /></Grid>
                        <Grid size={{ xs: 6, sm: 3 }}><TextField label="Taxa anual (%)" type="number" fullWidth value={mercado.taxaAnualPct} onChange={setMercadoCampo('taxaAnualPct')} /></Grid>
                        <Grid size={{ xs: 6, sm: 3 }}><TextField label="Taxa mensal instituição (%)" type="number" fullWidth value={mercado.taxaMensalInstituicaoPct} onChange={setMercadoCampo('taxaMensalInstituicaoPct')} /></Grid>
                        <Grid size={{ xs: 6, sm: 3 }}><TextField label="Taxa anual instituição (%)" type="number" fullWidth value={mercado.taxaAnualInstituicaoPct} onChange={setMercadoCampo('taxaAnualInstituicaoPct')} /></Grid>
                        <Grid size={{ xs: 12 }}><TextField label="Observação" fullWidth value={mercado.observacao} onChange={setMercadoCampo('observacao')} placeholder="Ex: print/consulta oficial anexada, fonte alternativa, etc." /></Grid>
                    </Grid>
                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }} justifyContent="space-between" sx={{ mt: 2 }}>
                        <FormControlLabel
                            control={<Checkbox checked={usarBcb} onChange={(e) => setUsarBcb(e.target.checked)} />}
                            label="Buscar taxa no BCB (SGS 25471) quando sem taxa manual"
                        />
                        <Button
                            variant="contained"
                            color="secondary"
                            startIcon={!analisando && <AssessmentIcon />}
                            onClick={handleAnalisar}
                            disabled={analisando}
                            sx={{ minWidth: 160 }}
                        >
                            {analisando ? <CircularProgress size={22} color="inherit" /> : 'Analisar'}
                        </Button>
                    </Stack>

                    {resultado && (
                        <Box sx={{ mt: 3 }}>
                            <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 2 }} flexWrap="wrap">
                                <Chip label={resultado.classificacaoRisco} color={riscoColor(resultado.classificacaoRisco)} />
                                <Chip variant="outlined" label={`Status: ${resultado.status}`} />
                            </Stack>

                            <Grid container spacing={2}>
                                <Grid size={{ xs: 6, sm: 3 }}><Metric label="Taxa contrato (mês)" value={fmtPct(resultado.taxaMensalContratoApuradaPct)} /></Grid>
                                <Grid size={{ xs: 6, sm: 3 }}><Metric label="Taxa mercado (mês)" value={fmtPct(resultado.taxaMensalMercadoPct)} /></Grid>
                                <Grid size={{ xs: 6, sm: 3 }}><Metric label="Spread s/ mercado" value={fmtPct(resultado.spreadPercentualSobreMercado)} /></Grid>
                                <Grid size={{ xs: 6, sm: 3 }}><Metric label="CET anual apurado" value={fmtPct(resultado.cetAnualApuradoPct)} /></Grid>
                                <Grid size={{ xs: 6, sm: 3 }}><Metric label="Parcela contratada" value={fmtMoney(resultado.parcelaContratual)} /></Grid>
                                <Grid size={{ xs: 6, sm: 3 }}><Metric label="Parcela recalculada" value={fmtMoney(resultado.parcelaRecalculadaMercado)} /></Grid>
                                <Grid size={{ xs: 6, sm: 3 }}><Metric label="Diferença mensal" value={fmtMoney(resultado.diferencaMensal)} /></Grid>
                                <Grid size={{ xs: 6, sm: 3 }}><Metric label="Diferença total" value={fmtMoney(resultado.diferencaTotalNominal)} /></Grid>
                            </Grid>

                            {resultado.conclusaoTecnica && (
                                <Alert severity="info" sx={{ mt: 3 }}>
                                    <AlertTitle>Conclusão técnica</AlertTitle>
                                    {resultado.conclusaoTecnica}
                                </Alert>
                            )}

                            {resultado.inconsistencias?.length > 0 && (
                                <Alert severity="warning" sx={{ mt: 2 }}>
                                    <AlertTitle>Inconsistências</AlertTitle>
                                    <List dense disablePadding>
                                        {resultado.inconsistencias.map((i, idx) => (
                                            <ListItem key={idx} disableGutters><ListItemText primary={`• ${i}`} /></ListItem>
                                        ))}
                                    </List>
                                </Alert>
                            )}

                            {resultado.recomendacoes?.length > 0 && (
                                <Alert severity="success" sx={{ mt: 2 }}>
                                    <AlertTitle>Recomendações</AlertTitle>
                                    <List dense disablePadding>
                                        {resultado.recomendacoes.map((i, idx) => (
                                            <ListItem key={idx} disableGutters><ListItemText primary={`• ${i}`} /></ListItem>
                                        ))}
                                    </List>
                                </Alert>
                            )}

                            {resultado.premissas?.length > 0 && (
                                <Box sx={{ mt: 3 }}>
                                    <SectionTitle>Premissas técnicas</SectionTitle>
                                    <List dense disablePadding>
                                        {resultado.premissas.map((p, idx) => (
                                            <ListItem key={idx} disableGutters><ListItemText primary={`• ${p}`} /></ListItem>
                                        ))}
                                    </List>
                                </Box>
                            )}

                            {resultado.memoriaCalculo && Object.keys(resultado.memoriaCalculo).length > 0 && (
                                <Box sx={{ mt: 3 }}>
                                    <SectionTitle>Memória de cálculo</SectionTitle>
                                    <TableContainer component={Box} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 2 }}>
                                        <Table size="small">
                                            <TableBody>
                                                {Object.entries(resultado.memoriaCalculo).map(([k, v]) => (
                                                    <TableRow key={k} hover>
                                                        <TableCell sx={{ fontWeight: 600, width: '45%' }}>{humanizar(k)}</TableCell>
                                                        <TableCell sx={{ fontFamily: 'monospace' }}>{fmtMemoria(v)}</TableCell>
                                                    </TableRow>
                                                ))}
                                            </TableBody>
                                        </Table>
                                    </TableContainer>
                                </Box>
                            )}

                            {resultado.tabelaPrice?.length > 0 && (
                                <Box sx={{ mt: 3 }}>
                                    <SectionTitle>Tabela PRICE ({resultado.tabelaPrice.length} parcelas)</SectionTitle>
                                    <TableContainer component={Box} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 2, maxHeight: 380 }}>
                                        <Table size="small" stickyHeader>
                                            <TableHead>
                                                <TableRow>
                                                    <TableCell sx={{ fontWeight: 700, bgcolor: '#f7f9fc' }}>Nº</TableCell>
                                                    <TableCell sx={{ fontWeight: 700, bgcolor: '#f7f9fc' }} align="right">Saldo inicial</TableCell>
                                                    <TableCell sx={{ fontWeight: 700, bgcolor: '#f7f9fc' }} align="right">Juros</TableCell>
                                                    <TableCell sx={{ fontWeight: 700, bgcolor: '#f7f9fc' }} align="right">Amortização</TableCell>
                                                    <TableCell sx={{ fontWeight: 700, bgcolor: '#f7f9fc' }} align="right">Prestação</TableCell>
                                                    <TableCell sx={{ fontWeight: 700, bgcolor: '#f7f9fc' }} align="right">Saldo final</TableCell>
                                                </TableRow>
                                            </TableHead>
                                            <TableBody>
                                                {resultado.tabelaPrice.map((l) => (
                                                    <TableRow key={l.numero} hover>
                                                        <TableCell>{l.numero}</TableCell>
                                                        <TableCell align="right">{fmtMoney(l.saldoInicial)}</TableCell>
                                                        <TableCell align="right">{fmtMoney(l.juros)}</TableCell>
                                                        <TableCell align="right">{fmtMoney(l.amortizacao)}</TableCell>
                                                        <TableCell align="right">{fmtMoney(l.prestacao)}</TableCell>
                                                        <TableCell align="right">{fmtMoney(l.saldoFinal)}</TableCell>
                                                    </TableRow>
                                                ))}
                                            </TableBody>
                                        </Table>
                                    </TableContainer>
                                </Box>
                            )}
                        </Box>
                    )}
                </Paper>
            )}

            {editando && resultado && (
                <Paper elevation={0} sx={{ p: { xs: 2, sm: 4 }, mt: 3, boxShadow: '0 2px 10px rgba(0,0,0,0.05)' }}>
                    <Stack direction="row" justifyContent="space-between" alignItems="center" flexWrap="wrap" gap={1}>
                        <SectionTitle>Auditoria pericial</SectionTitle>
                        <Button
                            variant="outlined"
                            color="secondary"
                            startIcon={!gerandoAudit && <FactCheckIcon />}
                            onClick={handleAuditoria}
                            disabled={gerandoAudit}
                        >
                            {gerandoAudit ? <CircularProgress size={22} /> : (auditoria ? 'Recarregar auditoria' : 'Gerar auditoria')}
                        </Button>
                    </Stack>

                    {auditoria && (
                        <Box sx={{ mt: 2 }}>
                            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={3} alignItems={{ sm: 'center' }} sx={{ mb: 2 }}>
                                <Box sx={{ textAlign: 'center', minWidth: 120 }}>
                                    <Typography variant="caption" color="text.secondary">Score de conformidade</Typography>
                                    <Typography variant="h2" fontWeight={800} sx={{ color: scoreColor(auditoria.score), lineHeight: 1 }}>
                                        {auditoria.score}
                                    </Typography>
                                    <Typography variant="caption" color="text.secondary">de 100</Typography>
                                </Box>
                                <Box sx={{ minWidth: 0 }}>
                                    <Typography variant="body2" color="text.secondary">
                                        Fingerprint do cálculo: <code>{auditoria.calculationFingerprint?.slice(0, 16)}…</code>
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary">
                                        Snapshot de entrada: <code>{auditoria.inputSnapshotHash?.slice(0, 16)}…</code>
                                    </Typography>
                                    <Typography variant="caption" color="text.secondary">
                                        Versão {auditoria.softwareVersion} · {auditoria.items?.length || 0} itens auditados
                                    </Typography>
                                </Box>
                            </Stack>

                            <TableContainer component={Box} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 2 }}>
                                <Table size="small">
                                    <TableHead>
                                        <TableRow sx={{ bgcolor: '#f7f9fc' }}>
                                            <TableCell sx={{ fontWeight: 700 }}>Código</TableCell>
                                            <TableCell sx={{ fontWeight: 700 }}>Item</TableCell>
                                            <TableCell sx={{ fontWeight: 700 }}>Status</TableCell>
                                            <TableCell sx={{ fontWeight: 700 }}>Detalhe</TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {auditoria.items?.map((it) => (
                                            <TableRow key={it.codigo} hover>
                                                <TableCell sx={{ fontFamily: 'monospace', whiteSpace: 'nowrap' }}>{it.codigo}</TableCell>
                                                <TableCell>{it.item}</TableCell>
                                                <TableCell>
                                                    <Chip size="small" label={it.status} color={statusColor(it.status)} variant="outlined" />
                                                    {it.severidade && it.severidade !== 'ok' && (
                                                        <Chip size="small" label={it.severidade} color={sevColor(it.severidade)} sx={{ ml: 0.5 }} />
                                                    )}
                                                </TableCell>
                                                <TableCell sx={{ color: 'text.secondary', fontSize: '0.8rem' }}>{it.detalhe}</TableCell>
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            </TableContainer>

                            {auditoria.warnings?.length > 0 && (
                                <Alert severity="warning" sx={{ mt: 2 }}>
                                    <AlertTitle>Avisos do motor de cálculo</AlertTitle>
                                    <List dense disablePadding>
                                        {auditoria.warnings.map((w, idx) => (
                                            <ListItem key={idx} disableGutters><ListItemText primary={`• ${w}`} /></ListItem>
                                        ))}
                                    </List>
                                </Alert>
                            )}

                            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 2 }}>
                                Fundamentação: {auditoria.sources?.length || 0} fontes normativas.{' '}
                                <Link component="button" type="button" onClick={() => navigate('/normas')}>Ver matriz normativa</Link>
                            </Typography>
                        </Box>
                    )}

                    <Divider sx={{ my: 3 }} />
                    <SectionTitle>Laudos PDF</SectionTitle>
                    <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1.5 }}>
                        Gerados sobre os dados atuais do caso, com auditoria e matriz normativa embutidas.
                    </Typography>
                    <Stack direction="row" spacing={1.5} flexWrap="wrap" useFlexGap>
                        {RELATORIOS.map((r) => (
                            <Button
                                key={r.tipo}
                                variant="outlined"
                                startIcon={gerandoRel === r.tipo ? <CircularProgress size={18} /> : <PictureAsPdfIcon />}
                                onClick={() => handleRelatorio(r.tipo)}
                                disabled={!!gerandoRel}
                            >
                                {r.label}
                            </Button>
                        ))}
                        <Button
                            variant="contained"
                            color="secondary"
                            startIcon={gerandoZip ? <CircularProgress size={18} color="inherit" /> : <FolderZipIcon />}
                            onClick={handlePacoteZip}
                            disabled={gerandoZip || !!gerandoRel}
                        >
                            Baixar pacote (ZIP)
                        </Button>
                    </Stack>
                </Paper>
            )}

            <Dialog open={viewer.aberto} onClose={fecharVisualizador} maxWidth="md" fullWidth>
                <DialogTitle sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', pr: 1 }}>
                    <Typography noWrap variant="subtitle1" fontWeight={700}>{viewer.nome}</Typography>
                    <Stack direction="row" spacing={0.5}>
                        {viewer.url && (
                            <Tooltip title="Baixar">
                                <IconButton component="a" href={viewer.url} download={viewer.nome}><DownloadIcon /></IconButton>
                            </Tooltip>
                        )}
                        <IconButton onClick={fecharVisualizador}><CloseIcon /></IconButton>
                    </Stack>
                </DialogTitle>
                <DialogContent dividers sx={{ p: 0, height: '75vh' }}>
                    {viewer.carregando ? (
                        <Box sx={{ p: 6, textAlign: 'center' }}><CircularProgress /></Box>
                    ) : viewer.tipo.startsWith('image/') ? (
                        <Box sx={{ p: 2, textAlign: 'center', height: '100%', overflow: 'auto' }}>
                            <img src={viewer.url} alt={viewer.nome} style={{ maxWidth: '100%' }} />
                        </Box>
                    ) : (
                        <iframe title={viewer.nome} src={viewer.url} style={{ width: '100%', height: '100%', border: 'none' }} />
                    )}
                </DialogContent>
            </Dialog>
        </Box>
    );
}

const Metric = ({ label, value }) => (
    <Box sx={{ p: 1.5, bgcolor: '#f7f9fc', borderRadius: 2, height: '100%' }}>
        <Typography variant="caption" color="text.secondary">{label}</Typography>
        <Typography variant="h6" fontWeight={700} color="#333">{value}</Typography>
    </Box>
);
