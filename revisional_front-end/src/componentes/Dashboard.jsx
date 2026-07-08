import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Box, Typography, Paper, Grid, Button, useTheme, useMediaQuery,
    List, ListItemButton, ListItemText, Chip, Stack,
} from '@mui/material';
import FolderSpecialOutlinedIcon from '@mui/icons-material/FolderSpecialOutlined';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import ReportProblemOutlinedIcon from '@mui/icons-material/ReportProblemOutlined';
import AddIcon from '@mui/icons-material/Add';

import KpiCard from './dashboard/KpiCard';
import { getNomeCompleto } from '../services/auth';
import { getEstatisticas, listarCasos } from '../services/casos';

const PRIMARY = '#1565C0';

const SectionTitle = ({ children }) => (
    <Typography variant="h6" component="h2" sx={{ color: PRIMARY, fontWeight: 600, display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
        <Box aria-hidden="true" sx={{ width: 4, height: 22, bgcolor: PRIMARY, borderRadius: 1 }} />
        {children}
    </Typography>
);

const dataHoje = () => {
    const d = new Date().toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
    return d.charAt(0).toUpperCase() + d.slice(1);
};

const fmtData = (iso) => (iso ? new Date(iso).toLocaleDateString('pt-BR') : '');

export default function Dashboard() {
    const theme = useTheme();
    const navigate = useNavigate();
    const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
    const primeiroNome = (getNomeCompleto() || 'Usuário').split(' ')[0];

    const [stats, setStats] = useState(null);
    const [recentes, setRecentes] = useState([]);

    useEffect(() => {
        (async () => {
            try {
                const [estatisticas, pagina] = await Promise.all([
                    getEstatisticas(),
                    listarCasos({ page: 0, size: 5, sort: 'atualizadoEm', dir: 'desc' }),
                ]);
                setStats(estatisticas);
                setRecentes(pagina.content || []);
            } catch {
                /* interceptor mostra toast */
            }
        })();
    }, []);

    const kpi = (v) => (stats ? String(v) : '—');

    return (
        <Box sx={{ flexGrow: 1 }}>
            {/* HEADER */}
            <Box component="header" sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: { xs: 'flex-start', sm: 'center' }, flexWrap: 'wrap', gap: 2 }}>
                <Box>
                    <Typography variant={isMobile ? 'h5' : 'h4'} component="h1" fontWeight="bold" color="#333">
                        Olá, {primeiroNome} 👋
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                        {dataHoje()}
                    </Typography>
                </Box>
                <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate('/casos')}>
                    Novo Caso
                </Button>
            </Box>

            {/* KPIs */}
            <Grid container spacing={{ xs: 2, sm: 3 }} alignItems="stretch" sx={{ mb: 3 }}>
                <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                    <KpiCard title="Casos em aberto" value={kpi(stats?.emAnalise)} subtitle="Aguardando auditoria"
                        icon={<FolderSpecialOutlinedIcon />} onClick={() => navigate('/casos')} />
                </Grid>
                <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                    <KpiCard title="Laudos prontos" value={kpi(stats?.laudoPronto)} subtitle="Casos analisados"
                        icon={<DescriptionOutlinedIcon />} accent="#2E7D32" />
                </Grid>
                <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                    <KpiCard title="Indícios fortes" value={kpi(stats?.indicioForte)} subtitle="Spread ≥ 2.0×"
                        icon={<WarningAmberOutlinedIcon />} accent="#C62828" valueColor="#C62828" />
                </Grid>
                <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                    <KpiCard title="Indícios moderados" value={kpi(stats?.indicioModerado)} subtitle="Spread ≥ 1.5×"
                        icon={<ReportProblemOutlinedIcon />} accent="#ED6C02" valueColor="#ED6C02" />
                </Grid>
            </Grid>

            {/* SEÇÕES */}
            <Grid container spacing={{ xs: 2, sm: 3 }} alignItems="stretch">
                <Grid size={{ xs: 12, md: 7 }}>
                    <Paper elevation={0} sx={{ p: 3, boxShadow: '0 2px 10px rgba(0,0,0,0.05)', height: '100%' }}>
                        <SectionTitle>Casos Recentes</SectionTitle>
                        {recentes.length === 0 ? (
                            <Box sx={{ py: 6, textAlign: 'center' }}>
                                <FolderSpecialOutlinedIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
                                <Typography color="text.secondary" gutterBottom>Nenhum caso cadastrado ainda.</Typography>
                                <Button variant="outlined" startIcon={<AddIcon />} sx={{ mt: 1 }} onClick={() => navigate('/casos')}>
                                    Cadastrar primeiro caso
                                </Button>
                            </Box>
                        ) : (
                            <List dense disablePadding>
                                {recentes.map((c) => (
                                    <ListItemButton key={c.id} onClick={() => navigate(`/casos/${c.id}`)} sx={{ borderRadius: 1 }}>
                                        <ListItemText
                                            primary={c.titulo || `Caso #${c.id}`}
                                            secondary={[c.clienteNome, fmtData(c.atualizadoEm)].filter(Boolean).join(' · ')}
                                            primaryTypographyProps={{ fontWeight: 600, noWrap: true }}
                                        />
                                        <Stack direction="row" spacing={1} sx={{ ml: 1 }}>
                                            <Chip
                                                size="small"
                                                label={c.temResultado ? 'Laudo pronto' : 'Em análise'}
                                                color={c.temResultado ? 'success' : 'warning'}
                                                variant={c.temResultado ? 'filled' : 'outlined'}
                                            />
                                        </Stack>
                                    </ListItemButton>
                                ))}
                            </List>
                        )}
                    </Paper>
                </Grid>

                <Grid size={{ xs: 12, md: 5 }}>
                    <Paper elevation={0} sx={{ p: 3, boxShadow: '0 2px 10px rgba(0,0,0,0.05)', height: '100%' }}>
                        <SectionTitle>Como funciona</SectionTitle>
                        <Box component="ol" sx={{ pl: 2.5, m: 0, color: 'text.secondary', '& li': { mb: 1.5 } }}>
                            <li><b>Cadastre o caso</b> e anexe o contrato de financiamento.</li>
                            <li><b>Extração</b> dos dados via OCR.</li>
                            <li><b>Engenharia reversa</b> das taxas ocultas (bisseção/CET).</li>
                            <li><b>Comparação</b> com o Banco Central (SGS 25471).</li>
                            <li><b>Laudo</b> técnico-jurídico em PDF.</li>
                        </Box>
                    </Paper>
                </Grid>
            </Grid>
        </Box>
    );
}
