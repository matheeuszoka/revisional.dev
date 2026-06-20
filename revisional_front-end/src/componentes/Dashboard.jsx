import React from 'react';
import {
    Box, Grid, Card, CardContent, Typography, Avatar, Stack, Paper, Chip
} from '@mui/material';
import FolderSpecialIcon from '@mui/icons-material/FolderSpecial';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import DescriptionIcon from '@mui/icons-material/Description';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';

import { getNomeCompleto, getUserRole } from '../services/auth';

const cards = [
    { label: 'Casos em aberto', value: '—', icon: <FolderSpecialIcon />, color: '#1565C0' },
    { label: 'Laudos gerados', value: '—', icon: <DescriptionIcon />, color: '#2E7D32' },
    { label: 'Indícios fortes', value: '—', icon: <WarningAmberIcon />, color: '#C62828' },
    { label: 'Spread médio', value: '—', icon: <TrendingUpIcon />, color: '#ED6C02' },
];

const Dashboard = () => {
    const nome = getNomeCompleto() || 'Usuário';
    const role = (getUserRole() || '').replace('ROLE_', '');

    return (
        <Box>
            <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 3 }}>
                <Box>
                    <Typography variant="h4" fontWeight="bold" color="secondary.main">
                        Olá, {nome.split(' ')[0]} 👋
                    </Typography>
                    <Typography variant="body1" color="text.secondary">
                        Painel do Sistema Revisional Bancário · MPG SISTEMAS
                    </Typography>
                </Box>
                {role && <Chip label={role} color="primary" variant="outlined" />}
            </Stack>

            <Grid container spacing={3}>
                {cards.map((c) => (
                    <Grid key={c.label} size={{ xs: 12, sm: 6, md: 3 }}>
                        <Card>
                            <CardContent>
                                <Stack direction="row" spacing={2} alignItems="center">
                                    <Avatar sx={{ bgcolor: c.color, width: 48, height: 48 }}>{c.icon}</Avatar>
                                    <Box>
                                        <Typography variant="h5" fontWeight="bold">{c.value}</Typography>
                                        <Typography variant="body2" color="text.secondary">{c.label}</Typography>
                                    </Box>
                                </Stack>
                            </CardContent>
                        </Card>
                    </Grid>
                ))}
            </Grid>

            <Paper sx={{ mt: 4, p: 4, textAlign: 'center' }}>
                <Typography variant="h6" color="text.secondary" gutterBottom>
                    Bem-vindo ao Revisional
                </Typography>
                <Typography variant="body2" color="text.secondary">
                    Cadastre um caso revisional para iniciar a auditoria: extração de dados do contrato,
                    engenharia reversa das taxas e comparação com o Banco Central.
                </Typography>
            </Paper>
        </Box>
    );
};

export default Dashboard;
