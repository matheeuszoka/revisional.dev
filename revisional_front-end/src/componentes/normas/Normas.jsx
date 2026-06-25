import React, { useEffect, useState } from 'react';
import {
    Box, Typography, Paper, Stack, CircularProgress, Link, Chip, Divider,
} from '@mui/material';
import GavelIcon from '@mui/icons-material/Gavel';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';

import { listarNormas } from '../../services/normas';
import { toastError } from '../../services/alerts';

export default function Normas() {
    const [normas, setNormas] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        (async () => {
            try {
                setNormas(await listarNormas());
            } catch {
                toastError('Falha ao carregar a matriz normativa.');
            } finally {
                setLoading(false);
            }
        })();
    }, []);

    if (loading) return <Box sx={{ p: 6, textAlign: 'center' }}><CircularProgress /></Box>;

    return (
        <Box>
            <Stack direction="row" alignItems="center" spacing={1.5} sx={{ mb: 3 }}>
                <GavelIcon color="secondary" />
                <Typography variant="h4" fontWeight="bold" color="#333">Matriz Normativa</Typography>
            </Stack>
            <Typography color="text.secondary" sx={{ mb: 3 }}>
                Leis, súmulas e resoluções que fundamentam a auditoria e os laudos. Parâmetros técnicos
                de comparação — não constituem teto legal automático.
            </Typography>

            <Stack spacing={2}>
                {normas.map((n, idx) => (
                    <Paper key={idx} elevation={0} sx={{ p: { xs: 2, sm: 3 }, boxShadow: '0 2px 10px rgba(0,0,0,0.05)' }}>
                        <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={2} flexWrap="wrap">
                            <Box sx={{ minWidth: 0 }}>
                                <Typography variant="subtitle1" fontWeight={700} color="secondary.main">{n.tema}</Typography>
                                <Chip size="small" label={n.fonte} sx={{ mt: 0.5 }} />
                            </Box>
                            <Link href={n.url} target="_blank" rel="noopener" underline="hover"
                                  sx={{ display: 'flex', alignItems: 'center', gap: 0.5, fontSize: '0.85rem', whiteSpace: 'nowrap' }}>
                                Fonte oficial <OpenInNewIcon sx={{ fontSize: '1rem' }} />
                            </Link>
                        </Stack>
                        <Divider sx={{ my: 1.5 }} />
                        <Typography variant="body2" color="text.secondary">{n.usoNoSistema}</Typography>
                    </Paper>
                ))}
            </Stack>
        </Box>
    );
}
