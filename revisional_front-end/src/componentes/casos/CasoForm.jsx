import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
    Box, Typography, Paper, Grid, TextField, Button, Stack, Divider,
    CircularProgress, MenuItem
} from '@mui/material';
import SaveIcon from '@mui/icons-material/Save';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';

import { getCaso, criarCaso, atualizarCaso } from '../../services/casos';
import { toastSuccess, alertValidacao } from '../../services/alerts';

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
    instituicao: '', contratoNumero: '',
    modalidade: 'Financiamento de veículos - Pessoa Física', dataContrato: '',
    veiculoDescricao: '', valorVeiculo: '', valorEntrada: '', valorFinanciado: '',
    prazoMeses: '', valorParcela: '',
    taxaJurosMensalPct: '', taxaJurosAnualPct: '',
};

export default function CasoForm() {
    const navigate = useNavigate();
    const { id } = useParams();
    const editando = id && id !== 'novo';

    const [titulo, setTitulo] = useState('');
    const [contrato, setContrato] = useState(contratoVazio);
    const [loading, setLoading] = useState(!!editando);
    const [salvando, setSalvando] = useState(false);

    useEffect(() => {
        if (!editando) return;
        (async () => {
            try {
                const caso = await getCaso(id);
                setTitulo(caso.titulo || '');
                setContrato({ ...contratoVazio, ...(caso.contrato || {}) });
            } finally {
                setLoading(false);
            }
        })();
    }, [id, editando]);

    const set = (campo) => (e) => setContrato({ ...contrato, [campo]: e.target.value });

    // Converte "" -> null e strings numéricas -> Number nos campos de valor
    const montarContrato = () => {
        const numericos = ['valorVeiculo', 'valorEntrada', 'valorFinanciado', 'prazoMeses', 'valorParcela', 'taxaJurosMensalPct', 'taxaJurosAnualPct'];
        const out = {};
        Object.entries(contrato).forEach(([k, v]) => {
            if (numericos.includes(k)) out[k] = v === '' || v === null ? null : Number(v);
            else out[k] = v;
        });
        return out;
    };

    const handleSalvar = async () => {
        if (!titulo.trim()) {
            alertValidacao('Informe um título para o caso.');
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

                <Divider sx={{ my: 2 }} />
                <SectionTitle>Cliente</SectionTitle>
                <Grid container spacing={2}>
                    <Grid size={{ xs: 12, sm: 8 }}><TextField label="Nome do cliente" fullWidth value={contrato.clienteNome} onChange={set('clienteNome')} /></Grid>
                    <Grid size={{ xs: 12, sm: 4 }}><TextField label="CPF do cliente" fullWidth value={contrato.clienteCpf} onChange={set('clienteCpf')} /></Grid>
                </Grid>

                <SectionTitle>Contrato</SectionTitle>
                <Grid container spacing={2}>
                    <Grid size={{ xs: 12, sm: 6 }}><TextField label="Instituição financeira" fullWidth value={contrato.instituicao} onChange={set('instituicao')} /></Grid>
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
                    <Grid size={{ xs: 6, sm: 3 }}><TextField label="Valor do veículo (R$)" type="number" fullWidth value={contrato.valorVeiculo} onChange={set('valorVeiculo')} /></Grid>
                    <Grid size={{ xs: 6, sm: 3 }}><TextField label="Entrada (R$)" type="number" fullWidth value={contrato.valorEntrada} onChange={set('valorEntrada')} /></Grid>
                    <Grid size={{ xs: 6, sm: 4 }}><TextField label="Valor financiado (R$)" type="number" fullWidth value={contrato.valorFinanciado} onChange={set('valorFinanciado')} /></Grid>
                    <Grid size={{ xs: 6, sm: 4 }}><TextField label="Prazo (meses)" type="number" fullWidth value={contrato.prazoMeses} onChange={set('prazoMeses')} /></Grid>
                    <Grid size={{ xs: 6, sm: 4 }}><TextField label="Valor da parcela (R$)" type="number" fullWidth value={contrato.valorParcela} onChange={set('valorParcela')} /></Grid>
                </Grid>

                <SectionTitle>Taxas (se constarem no contrato)</SectionTitle>
                <Grid container spacing={2}>
                    <Grid size={{ xs: 6, sm: 3 }}><TextField label="Juros mensal (%)" type="number" fullWidth value={contrato.taxaJurosMensalPct} onChange={set('taxaJurosMensalPct')} /></Grid>
                    <Grid size={{ xs: 6, sm: 3 }}><TextField label="Juros anual (%)" type="number" fullWidth value={contrato.taxaJurosAnualPct} onChange={set('taxaJurosAnualPct')} /></Grid>
                </Grid>

                <Divider sx={{ my: 3 }} />
                <Stack direction="row" justifyContent="flex-end" spacing={2}>
                    <Button onClick={() => navigate('/casos')} color="inherit" disabled={salvando}>Cancelar</Button>
                    <Button variant="contained" startIcon={<SaveIcon />} onClick={handleSalvar} disabled={salvando}>
                        {salvando ? <CircularProgress size={22} color="inherit" /> : 'Salvar'}
                    </Button>
                </Stack>
            </Paper>
        </Box>
    );
}
