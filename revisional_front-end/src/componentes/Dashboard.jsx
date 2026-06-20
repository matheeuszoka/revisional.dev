import React from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Box, Typography, Paper, Grid, Button, useTheme, useMediaQuery
} from '@mui/material';
import FolderSpecialOutlinedIcon from '@mui/icons-material/FolderSpecialOutlined';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import TrendingUpOutlinedIcon from '@mui/icons-material/TrendingUpOutlined';
import AddIcon from '@mui/icons-material/Add';

import KpiCard from './dashboard/KpiCard';
import { getNomeCompleto } from '../services/auth';

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

export default function Dashboard() {
    const theme = useTheme();
    const navigate = useNavigate();
    const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
    const primeiroNome = (getNomeCompleto() || 'Usuário').split(' ')[0];

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
                    <KpiCard title="Casos em aberto" value="—" subtitle="Aguardando auditoria"
                        icon={<FolderSpecialOutlinedIcon />} onClick={() => navigate('/casos')} />
                </Grid>
                <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                    <KpiCard title="Laudos gerados" value="—" subtitle="No total"
                        icon={<DescriptionOutlinedIcon />} accent="#2E7D32" />
                </Grid>
                <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                    <KpiCard title="Indícios fortes" value="—" subtitle="Spread ≥ 2.0×"
                        icon={<WarningAmberOutlinedIcon />} accent="#C62828" valueColor="#C62828" />
                </Grid>
                <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                    <KpiCard title="Spread médio" value="—" subtitle="Banco vs. mercado"
                        icon={<TrendingUpOutlinedIcon />} accent="#ED6C02" />
                </Grid>
            </Grid>

            {/* SEÇÕES */}
            <Grid container spacing={{ xs: 2, sm: 3 }} alignItems="stretch">
                <Grid size={{ xs: 12, md: 7 }}>
                    <Paper elevation={0} sx={{ p: 3, boxShadow: '0 2px 10px rgba(0,0,0,0.05)', height: '100%' }}>
                        <SectionTitle>Casos Recentes</SectionTitle>
                        <Box sx={{ py: 6, textAlign: 'center' }}>
                            <FolderSpecialOutlinedIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
                            <Typography color="text.secondary" gutterBottom>Nenhum caso cadastrado ainda.</Typography>
                            <Button variant="outlined" startIcon={<AddIcon />} sx={{ mt: 1 }} onClick={() => navigate('/casos')}>
                                Cadastrar primeiro caso
                            </Button>
                        </Box>
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
