import React, { useEffect, useState } from 'react';
import {
    Box, Typography, Paper, Grid, TextField, Button, Stack, Divider,
    CircularProgress, Chip, InputAdornment, IconButton, FormControlLabel, Checkbox, Alert
} from '@mui/material';
import SaveIcon from '@mui/icons-material/Save';
import Visibility from '@mui/icons-material/Visibility';
import VisibilityOff from '@mui/icons-material/VisibilityOff';

import { getConfiguracoes, salvarConfiguracoes } from '../../services/configuracoes';
import { toastSuccess, toastError } from '../../services/alerts';

const SectionTitle = ({ children }) => (
    <Typography variant="subtitle1" fontWeight={700} color="secondary.main" sx={{ mt: 1, mb: 1 }}>
        {children}
    </Typography>
);

export default function Parametros() {
    const [cfg, setCfg] = useState(null);
    const [loading, setLoading] = useState(true);
    const [salvando, setSalvando] = useState(false);

    // Campos de chave (segredo) tratados à parte: só enviam se preenchidos.
    const [apiKey, setApiKey] = useState('');
    const [mostrarKey, setMostrarKey] = useState(false);
    const [limparKey, setLimparKey] = useState(false);

    useEffect(() => {
        (async () => {
            try {
                setCfg(await getConfiguracoes());
            } catch {
                toastError('Falha ao carregar os parâmetros.');
            } finally {
                setLoading(false);
            }
        })();
    }, []);

    const set = (campo) => (e) => setCfg({ ...cfg, [campo]: e.target.value });
    const setNum = (campo) => (e) => setCfg({ ...cfg, [campo]: e.target.value === '' ? null : Number(e.target.value) });

    const handleSalvar = async () => {
        setSalvando(true);
        try {
            const payload = {
                ocrIdioma: cfg.ocrIdioma,
                ocrDpi: cfg.ocrDpi,
                ocrMaxPaginas: cfg.ocrMaxPaginas,
                ocrTessdataPath: cfg.ocrTessdataPath,
                openRouterBaseUrl: cfg.openRouterBaseUrl,
                openRouterModel: cfg.openRouterModel,
                openRouterTimeoutSegundos: cfg.openRouterTimeoutSegundos,
                openRouterMaxChars: cfg.openRouterMaxChars,
                openRouterReferer: cfg.openRouterReferer,
                openRouterTitulo: cfg.openRouterTitulo,
                openRouterApiKey: limparKey ? null : (apiKey || null),
                limparApiKey: limparKey,
            };
            const atualizado = await salvarConfiguracoes(payload);
            setCfg(atualizado);
            setApiKey('');
            setLimparKey(false);
            toastSuccess('Parâmetros salvos.');
        } catch (err) {
            const msg = err.response?.data?.message || err.response?.data?.error;
            toastError(msg || 'Falha ao salvar os parâmetros.');
        } finally {
            setSalvando(false);
        }
    };

    if (loading) return <Box sx={{ p: 6, textAlign: 'center' }}><CircularProgress /></Box>;
    if (!cfg) return null;

    return (
        <Box>
            <Typography variant="h4" fontWeight="bold" color="#333" sx={{ mb: 3 }}>
                Parâmetros do Sistema
            </Typography>

            <Paper elevation={0} sx={{ p: { xs: 2, sm: 4 }, boxShadow: '0 2px 10px rgba(0,0,0,0.05)' }}>
                <SectionTitle>Inteligência Artificial (OpenRouter)</SectionTitle>
                <Alert severity="info" sx={{ mb: 2 }}>
                    A chave é armazenada criptografada e nunca é exibida. Deixe em branco para manter a atual.
                </Alert>
                <Grid container spacing={2}>
                    <Grid size={{ xs: 12, sm: 8 }}>
                        <TextField
                            label="Chave da API (OpenRouter)"
                            fullWidth
                            type={mostrarKey ? 'text' : 'password'}
                            value={apiKey}
                            onChange={(e) => setApiKey(e.target.value)}
                            disabled={limparKey}
                            placeholder={cfg.openRouterApiKeyDefinida ? '•••••••• (definida)' : 'Nenhuma chave definida'}
                            InputProps={{
                                endAdornment: (
                                    <InputAdornment position="end">
                                        <IconButton onClick={() => setMostrarKey(!mostrarKey)} edge="end">
                                            {mostrarKey ? <VisibilityOff /> : <Visibility />}
                                        </IconButton>
                                    </InputAdornment>
                                ),
                            }}
                        />
                    </Grid>
                    <Grid size={{ xs: 12, sm: 4 }} sx={{ display: 'flex', alignItems: 'center' }}>
                        <FormControlLabel
                            control={<Checkbox checked={limparKey} onChange={(e) => setLimparKey(e.target.checked)} />}
                            label="Remover chave"
                        />
                        <Chip
                            size="small"
                            label={cfg.openRouterApiKeyDefinida ? 'IA ativa' : 'IA inativa'}
                            color={cfg.openRouterApiKeyDefinida ? 'success' : 'default'}
                            sx={{ ml: 1 }}
                        />
                    </Grid>
                    <Grid size={{ xs: 12, sm: 6 }}><TextField label="Modelo" fullWidth value={cfg.openRouterModel || ''} onChange={set('openRouterModel')} /></Grid>
                    <Grid size={{ xs: 12, sm: 6 }}><TextField label="Base URL" fullWidth value={cfg.openRouterBaseUrl || ''} onChange={set('openRouterBaseUrl')} /></Grid>
                    <Grid size={{ xs: 6, sm: 3 }}><TextField label="Timeout (s)" type="number" fullWidth value={cfg.openRouterTimeoutSegundos ?? ''} onChange={setNum('openRouterTimeoutSegundos')} /></Grid>
                    <Grid size={{ xs: 6, sm: 3 }}><TextField label="Máx. caracteres" type="number" fullWidth value={cfg.openRouterMaxChars ?? ''} onChange={setNum('openRouterMaxChars')} /></Grid>
                    <Grid size={{ xs: 12, sm: 6 }}><TextField label="Título (X-Title)" fullWidth value={cfg.openRouterTitulo || ''} onChange={set('openRouterTitulo')} /></Grid>
                    <Grid size={{ xs: 12 }}><TextField label="Referer (HTTP-Referer)" fullWidth value={cfg.openRouterReferer || ''} onChange={set('openRouterReferer')} /></Grid>
                </Grid>

                <Divider sx={{ my: 3 }} />
                <SectionTitle>OCR (Tesseract)</SectionTitle>
                <Grid container spacing={2}>
                    <Grid size={{ xs: 6, sm: 3 }}><TextField label="Idioma" fullWidth value={cfg.ocrIdioma || ''} onChange={set('ocrIdioma')} helperText="ex.: por" /></Grid>
                    <Grid size={{ xs: 6, sm: 3 }}><TextField label="DPI" type="number" fullWidth value={cfg.ocrDpi ?? ''} onChange={setNum('ocrDpi')} /></Grid>
                    <Grid size={{ xs: 6, sm: 3 }}><TextField label="Máx. páginas" type="number" fullWidth value={cfg.ocrMaxPaginas ?? ''} onChange={setNum('ocrMaxPaginas')} /></Grid>
                    <Grid size={{ xs: 12, sm: 3 }}><TextField label="Tessdata path" fullWidth value={cfg.ocrTessdataPath || ''} onChange={set('ocrTessdataPath')} /></Grid>
                </Grid>

                <Divider sx={{ my: 3 }} />
                <Stack direction="row" justifyContent="flex-end">
                    <Button variant="contained" startIcon={<SaveIcon />} onClick={handleSalvar} disabled={salvando}>
                        {salvando ? <CircularProgress size={22} color="inherit" /> : 'Salvar'}
                    </Button>
                </Stack>

                {cfg.atualizadoEm && (
                    <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 2, textAlign: 'right' }}>
                        Última atualização: {new Date(cfg.atualizadoEm).toLocaleString('pt-BR')}
                    </Typography>
                )}
            </Paper>
        </Box>
    );
}
