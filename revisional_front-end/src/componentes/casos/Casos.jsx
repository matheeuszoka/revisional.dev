import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Box, Typography, Paper, Button, Table, TableBody, TableCell, TableContainer,
    TableHead, TableRow, TableSortLabel, TablePagination, Chip, IconButton, Tooltip,
    CircularProgress, Stack, TextField, InputAdornment
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import SearchIcon from '@mui/icons-material/Search';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import FolderSpecialOutlinedIcon from '@mui/icons-material/FolderSpecialOutlined';

import { listarCasos, excluirCaso } from '../../services/casos';
import { confirmExclusao, toastSuccess } from '../../services/alerts';

const fmtData = (d) => (d ? new Date(d).toLocaleDateString('pt-BR') : '—');

// Cor do chip de risco a partir da classificação do motor financeiro.
const riscoColor = (c) => {
    if (!c) return 'default';
    const s = c.toLowerCase();
    if (s.includes('forte')) return 'error';
    if (s.includes('moderado') || s.startsWith('taxa acima')) return 'warning';
    if (s.includes('sem ind')) return 'success';
    return 'default';
};

// Célula com texto truncado (não quebra a linha; mostra completo no tooltip).
const CelulaTexto = ({ children, max = 240, bold = false }) => (
    <TableCell sx={{ maxWidth: max }}>
        <Tooltip title={children && children !== '—' ? children : ''} placement="top-start">
            <Typography noWrap variant="body2" sx={{ fontWeight: bold ? 600 : 400 }}>
                {children}
            </Typography>
        </Tooltip>
    </TableCell>
);

// Colunas que o back consegue ordenar (titulo/criadoEm/atualizadoEm são colunas reais).
const COLUNAS_ORDENAVEIS = { titulo: 'Título', atualizadoEm: 'Atualizado' };

export default function Casos() {
    const navigate = useNavigate();
    const [casos, setCasos] = useState([]);
    const [total, setTotal] = useState(0);
    const [loading, setLoading] = useState(true);

    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(10);
    const [orderBy, setOrderBy] = useState('atualizadoEm');
    const [order, setOrder] = useState('desc');
    const [busca, setBusca] = useState('');
    const [buscaAplicada, setBuscaAplicada] = useState('');

    const carregar = useCallback(async () => {
        setLoading(true);
        try {
            const resp = await listarCasos({
                page, size: rowsPerPage, sort: orderBy, dir: order, q: buscaAplicada || undefined,
            });
            setCasos(resp.content || []);
            setTotal(resp.totalElements || 0);
        } catch {
            /* interceptor já mostra toast */
        } finally {
            setLoading(false);
        }
    }, [page, rowsPerPage, orderBy, order, buscaAplicada]);

    useEffect(() => { carregar(); }, [carregar]);

    // Debounce da busca: aplica 400ms após parar de digitar e volta p/ a 1ª página.
    useEffect(() => {
        const t = setTimeout(() => { setBuscaAplicada(busca.trim()); setPage(0); }, 400);
        return () => clearTimeout(t);
    }, [busca]);

    const handleSort = (campo) => {
        if (orderBy === campo) {
            setOrder(order === 'asc' ? 'desc' : 'asc');
        } else {
            setOrderBy(campo);
            setOrder('asc');
        }
        setPage(0);
    };

    const handleExcluir = async (caso) => {
        const ok = await confirmExclusao({ text: `Excluir o caso "${caso.titulo || 'sem título'}"? Esta ação não pode ser desfeita.` });
        if (!ok) return;
        await excluirCaso(caso.id);
        toastSuccess('Caso excluído.');
        // Se era o último item da página, recua uma página.
        if (casos.length === 1 && page > 0) setPage(page - 1);
        else carregar();
    };

    const semCasos = !loading && total === 0 && !buscaAplicada;

    return (
        <Box>
            <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'center' }} spacing={2} sx={{ mb: 3 }}>
                <Box>
                    <Typography variant="h4" fontWeight="bold" color="#333">Casos Revisionais</Typography>
                    <Typography variant="body2" color="text.secondary">
                        {total} {total === 1 ? 'caso' : 'casos'}{buscaAplicada ? ' (filtrados)' : ''}
                    </Typography>
                </Box>
                <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate('/casos/novo')} sx={{ alignSelf: { xs: 'stretch', sm: 'auto' } }}>
                    Novo Caso
                </Button>
            </Stack>

            <Paper elevation={0} sx={{ boxShadow: '0 2px 10px rgba(0,0,0,0.05)', overflow: 'hidden' }}>
                {!semCasos && (
                    <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
                        <TextField
                            size="small"
                            fullWidth
                            placeholder="Buscar por título…"
                            value={busca}
                            onChange={(e) => setBusca(e.target.value)}
                            InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> }}
                            sx={{ maxWidth: 460 }}
                        />
                    </Box>
                )}

                {loading ? (
                    <Box sx={{ p: 6, textAlign: 'center' }}><CircularProgress /></Box>
                ) : semCasos ? (
                    <Box sx={{ py: 8, textAlign: 'center' }}>
                        <FolderSpecialOutlinedIcon sx={{ fontSize: 56, color: 'text.disabled', mb: 1 }} />
                        <Typography color="text.secondary" gutterBottom>Nenhum caso cadastrado ainda.</Typography>
                        <Button variant="outlined" startIcon={<AddIcon />} sx={{ mt: 1 }} onClick={() => navigate('/casos/novo')}>
                            Cadastrar primeiro caso
                        </Button>
                    </Box>
                ) : total === 0 ? (
                    <Box sx={{ py: 6, textAlign: 'center' }}>
                        <Typography color="text.secondary">Nenhum caso encontrado para “{buscaAplicada}”.</Typography>
                    </Box>
                ) : (
                    <>
                        <TableContainer sx={{ overflowX: 'auto' }}>
                            <Table sx={{ minWidth: 820 }} aria-label="Casos revisionais">
                                <TableHead>
                                    <TableRow sx={{ '& th': { fontWeight: 700, color: 'text.secondary', bgcolor: '#f8fafc', whiteSpace: 'nowrap' } }}>
                                        <TableCell sortDirection={orderBy === 'titulo' ? order : false}>
                                            <TableSortLabel
                                                active={orderBy === 'titulo'}
                                                direction={orderBy === 'titulo' ? order : 'asc'}
                                                onClick={() => handleSort('titulo')}
                                            >
                                                {COLUNAS_ORDENAVEIS.titulo}
                                            </TableSortLabel>
                                        </TableCell>
                                        <TableCell>Cliente</TableCell>
                                        <TableCell>Instituição</TableCell>
                                        <TableCell sortDirection={orderBy === 'atualizadoEm' ? order : false}>
                                            <TableSortLabel
                                                active={orderBy === 'atualizadoEm'}
                                                direction={orderBy === 'atualizadoEm' ? order : 'asc'}
                                                onClick={() => handleSort('atualizadoEm')}
                                            >
                                                {COLUNAS_ORDENAVEIS.atualizadoEm}
                                            </TableSortLabel>
                                        </TableCell>
                                        <TableCell>Risco</TableCell>
                                        <TableCell>Status</TableCell>
                                        <TableCell align="right">Ações</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {casos.map((c) => (
                                        <TableRow key={c.id} hover sx={{ cursor: 'pointer' }} onClick={() => navigate(`/casos/${c.id}`)}>
                                            <CelulaTexto max={260} bold>{c.titulo || '—'}</CelulaTexto>
                                            <CelulaTexto max={200}>{c.clienteNome || '—'}</CelulaTexto>
                                            <CelulaTexto max={200}>{c.instituicao || '—'}</CelulaTexto>
                                            <TableCell sx={{ whiteSpace: 'nowrap' }}>{fmtData(c.atualizadoEm)}</TableCell>
                                            <TableCell sx={{ whiteSpace: 'nowrap', maxWidth: 200 }}>
                                                {c.classificacaoRisco ? (
                                                    <Chip size="small" label={c.classificacaoRisco} color={riscoColor(c.classificacaoRisco)}
                                                          variant="outlined" sx={{ maxWidth: 190 }} />
                                                ) : '—'}
                                            </TableCell>
                                            <TableCell sx={{ whiteSpace: 'nowrap' }}>
                                                <Chip
                                                    size="small"
                                                    label={c.temResultado ? 'Laudo pronto' : 'Em análise'}
                                                    color={c.temResultado ? 'success' : 'warning'}
                                                    variant={c.temResultado ? 'filled' : 'outlined'}
                                                />
                                            </TableCell>
                                            <TableCell align="right" sx={{ whiteSpace: 'nowrap' }} onClick={(e) => e.stopPropagation()}>
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

                        <TablePagination
                            component="div"
                            count={total}
                            page={page}
                            onPageChange={(_, novaPagina) => setPage(novaPagina)}
                            rowsPerPage={rowsPerPage}
                            onRowsPerPageChange={(e) => { setRowsPerPage(parseInt(e.target.value, 10)); setPage(0); }}
                            rowsPerPageOptions={[5, 10, 25, 50]}
                            labelRowsPerPage="Por página:"
                            labelDisplayedRows={({ from, to, count }) => `${from}–${to} de ${count}`}
                        />
                    </>
                )}
            </Paper>
        </Box>
    );
}
