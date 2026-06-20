import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Box, Typography, Paper, Button, Table, TableBody, TableCell, TableContainer,
    TableHead, TableRow, Chip, IconButton, Tooltip, CircularProgress, Stack
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import FolderSpecialOutlinedIcon from '@mui/icons-material/FolderSpecialOutlined';

import { listarCasos, excluirCaso } from '../../services/casos';
import { confirmExclusao, toastSuccess } from '../../services/alerts';

const fmtData = (d) => d ? new Date(d).toLocaleDateString('pt-BR') : '-';

export default function Casos() {
    const navigate = useNavigate();
    const [casos, setCasos] = useState([]);
    const [loading, setLoading] = useState(true);

    const carregar = async () => {
        setLoading(true);
        try {
            setCasos(await listarCasos());
        } catch {
            /* interceptor já mostra toast */
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { carregar(); }, []);

    const handleExcluir = async (caso) => {
        const ok = await confirmExclusao({ text: `Excluir o caso "${caso.titulo || 'sem título'}"? Esta ação não pode ser desfeita.` });
        if (!ok) return;
        await excluirCaso(caso.id);
        toastSuccess('Caso excluído.');
        carregar();
    };

    return (
        <Box>
            <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 3 }}>
                <Typography variant="h4" fontWeight="bold" color="#333">Casos Revisionais</Typography>
                <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate('/casos/novo')}>
                    Novo Caso
                </Button>
            </Stack>

            <Paper elevation={0} sx={{ boxShadow: '0 2px 10px rgba(0,0,0,0.05)', overflow: 'hidden' }}>
                {loading ? (
                    <Box sx={{ p: 6, textAlign: 'center' }}><CircularProgress /></Box>
                ) : casos.length === 0 ? (
                    <Box sx={{ py: 8, textAlign: 'center' }}>
                        <FolderSpecialOutlinedIcon sx={{ fontSize: 56, color: 'text.disabled', mb: 1 }} />
                        <Typography color="text.secondary" gutterBottom>Nenhum caso cadastrado ainda.</Typography>
                        <Button variant="outlined" startIcon={<AddIcon />} sx={{ mt: 1 }} onClick={() => navigate('/casos/novo')}>
                            Cadastrar primeiro caso
                        </Button>
                    </Box>
                ) : (
                    <TableContainer>
                        <Table>
                            <TableHead>
                                <TableRow sx={{ '& th': { fontWeight: 700, color: 'text.secondary', bgcolor: '#f8fafc' } }}>
                                    <TableCell>Título</TableCell>
                                    <TableCell>Cliente</TableCell>
                                    <TableCell>Instituição</TableCell>
                                    <TableCell>Atualizado</TableCell>
                                    <TableCell>Status</TableCell>
                                    <TableCell align="right">Ações</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {casos.map((c) => (
                                    <TableRow
                                        key={c.id}
                                        hover
                                        sx={{ cursor: 'pointer' }}
                                        onClick={() => navigate(`/casos/${c.id}`)}
                                    >
                                        <TableCell sx={{ fontWeight: 600 }}>{c.titulo || '—'}</TableCell>
                                        <TableCell>{c.clienteNome || '—'}</TableCell>
                                        <TableCell>{c.instituicao || '—'}</TableCell>
                                        <TableCell>{fmtData(c.atualizadoEm)}</TableCell>
                                        <TableCell>
                                            <Chip
                                                size="small"
                                                label={c.temResultado ? 'Laudo pronto' : 'Em análise'}
                                                color={c.temResultado ? 'success' : 'warning'}
                                                variant={c.temResultado ? 'filled' : 'outlined'}
                                            />
                                        </TableCell>
                                        <TableCell align="right" onClick={(e) => e.stopPropagation()}>
                                            <Tooltip title="Editar">
                                                <IconButton size="small" onClick={() => navigate(`/casos/${c.id}`)}>
                                                    <EditOutlinedIcon fontSize="small" />
                                                </IconButton>
                                            </Tooltip>
                                            <Tooltip title="Excluir">
                                                <IconButton size="small" color="error" onClick={() => handleExcluir(c)}>
                                                    <DeleteOutlineIcon fontSize="small" />
                                                </IconButton>
                                            </Tooltip>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>
                )}
            </Paper>
        </Box>
    );
}
