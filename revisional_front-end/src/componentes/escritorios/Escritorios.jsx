import React, { useCallback, useEffect, useState } from 'react';
import {
    Box, Card, CardContent, Chip, CircularProgress, Dialog, DialogContent,
    DialogTitle, IconButton, Switch, Table, TableBody, TableCell, TableContainer,
    TableHead, TableRow, Tooltip, Typography
} from '@mui/material';
import GroupOutlinedIcon from '@mui/icons-material/GroupOutlined';
import BusinessOutlinedIcon from '@mui/icons-material/BusinessOutlined';

import { listarTenants, listarUsuariosDoTenant, alterarAtivoTenant } from '../../services/tenants';
import { toastSuccess, confirmAcao } from '../../services/alerts';

const formatCnpj = (cnpj) => {
    if (!cnpj) return '—';
    const d = cnpj.replace(/\D/g, '');
    if (d.length !== 14) return cnpj;
    return d.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, '$1.$2.$3/$4-$5');
};

const roleLabel = (role) => ({
    ROLE_SUPER_ADMIN: 'Plataforma',
    ROLE_ADMIN: 'Administrador',
    ROLE_AUDITOR: 'Auditor',
    ROLE_VISUALIZADOR: 'Visualizador',
}[role] || role);

export default function Escritorios() {
    const [tenants, setTenants] = useState([]);
    const [loading, setLoading] = useState(true);

    // Dialog de membros do escritório selecionado
    const [tenantSelecionado, setTenantSelecionado] = useState(null);
    const [membros, setMembros] = useState([]);
    const [carregandoMembros, setCarregandoMembros] = useState(false);

    const carregar = useCallback(async () => {
        setLoading(true);
        try {
            setTenants(await listarTenants());
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => { carregar(); }, [carregar]);

    const abrirMembros = async (tenant) => {
        setTenantSelecionado(tenant);
        setCarregandoMembros(true);
        try {
            setMembros(await listarUsuariosDoTenant(tenant.id));
        } catch {
            setMembros([]);
        } finally {
            setCarregandoMembros(false);
        }
    };

    const alternarAtivo = async (tenant) => {
        const desativar = tenant.ativo;
        const ok = await confirmAcao({
            title: desativar ? 'Desativar escritório' : 'Reativar escritório',
            text: desativar
                ? `TODOS os membros de "${tenant.nome}" perderão o acesso imediatamente (sessões ativas param de funcionar). Os dados são preservados.`
                : `Os membros de "${tenant.nome}" voltarão a ter acesso ao sistema.`,
            icon: 'warning',
            confirmButtonText: desativar ? 'Desativar' : 'Reativar',
            danger: desativar,
        });
        if (!ok) return;
        try {
            await alterarAtivoTenant(tenant.id, !tenant.ativo);
            toastSuccess(desativar ? 'Escritório desativado.' : 'Escritório reativado.');
            carregar();
        } catch {
            // erro já exibido pelo interceptor global (ex.: escritório da plataforma)
        }
    };

    return (
        <Box>
            <Box sx={{ mb: 2 }}>
                <Typography variant="h5" fontWeight="bold" color="secondary.main">Escritórios</Typography>
                <Typography variant="body2" color="text.secondary">
                    Administração da plataforma: todos os escritórios (tenants) cadastrados no sistema.
                </Typography>
            </Box>

            <Card>
                <CardContent sx={{ p: 0, '&:last-child': { pb: 0 } }}>
                    {loading ? (
                        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}><CircularProgress /></Box>
                    ) : (
                        <TableContainer>
                            <Table size="small">
                                <TableHead>
                                    <TableRow>
                                        <TableCell>Escritório</TableCell>
                                        <TableCell>CNPJ</TableCell>
                                        <TableCell>Criado em</TableCell>
                                        <TableCell align="center">Membros</TableCell>
                                        <TableCell align="center">Ativo</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {tenants.map((t) => (
                                        <TableRow key={t.id} hover>
                                            <TableCell>
                                                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                                    <BusinessOutlinedIcon fontSize="small" color={t.ativo ? 'primary' : 'disabled'} />
                                                    <Typography variant="body2" fontWeight={600}>{t.nome}</Typography>
                                                    {!t.ativo && <Chip label="Desativado" size="small" color="error" variant="outlined" />}
                                                </Box>
                                            </TableCell>
                                            <TableCell>{formatCnpj(t.cnpj)}</TableCell>
                                            <TableCell>
                                                {t.dataCriacao ? new Date(t.dataCriacao).toLocaleDateString('pt-BR') : '—'}
                                            </TableCell>
                                            <TableCell align="center">
                                                <Tooltip title="Ver membros">
                                                    <IconButton size="small" onClick={() => abrirMembros(t)}>
                                                        <GroupOutlinedIcon fontSize="small" />
                                                    </IconButton>
                                                </Tooltip>
                                            </TableCell>
                                            <TableCell align="center">
                                                <Switch checked={t.ativo} onChange={() => alternarAtivo(t)} size="small" />
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                    {tenants.length === 0 && (
                                        <TableRow>
                                            <TableCell colSpan={5} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                                                Nenhum escritório cadastrado.
                                            </TableCell>
                                        </TableRow>
                                    )}
                                </TableBody>
                            </Table>
                        </TableContainer>
                    )}
                </CardContent>
            </Card>

            {/* Membros do escritório */}
            <Dialog open={!!tenantSelecionado} onClose={() => setTenantSelecionado(null)} maxWidth="md" fullWidth>
                <DialogTitle>
                    Membros — {tenantSelecionado?.nome}
                </DialogTitle>
                <DialogContent dividers sx={{ p: 0 }}>
                    {carregandoMembros ? (
                        <Box sx={{ display: 'flex', justifyContent: 'center', py: 5 }}><CircularProgress /></Box>
                    ) : (
                        <TableContainer>
                            <Table size="small">
                                <TableHead>
                                    <TableRow>
                                        <TableCell>Nome</TableCell>
                                        <TableCell>E-mail</TableCell>
                                        <TableCell>OAB</TableCell>
                                        <TableCell>Papel</TableCell>
                                        <TableCell>Último acesso</TableCell>
                                        <TableCell align="center">Ativo</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {membros.map((u) => (
                                        <TableRow key={u.id} hover>
                                            <TableCell>{u.nomeCompleto}</TableCell>
                                            <TableCell>{u.email || '—'}</TableCell>
                                            <TableCell>{u.oab || '—'}</TableCell>
                                            <TableCell>
                                                <Chip label={roleLabel(u.role)} size="small"
                                                    color={u.role === 'ROLE_SUPER_ADMIN' ? 'secondary' : u.role === 'ROLE_ADMIN' ? 'primary' : 'default'} />
                                            </TableCell>
                                            <TableCell>
                                                {u.dataUltimoLogin ? new Date(u.dataUltimoLogin).toLocaleString('pt-BR') : 'Nunca acessou'}
                                            </TableCell>
                                            <TableCell align="center">
                                                <Chip label={u.ativo ? 'Sim' : 'Não'} size="small"
                                                    color={u.ativo ? 'success' : 'default'} variant="outlined" />
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                    {membros.length === 0 && (
                                        <TableRow>
                                            <TableCell colSpan={6} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                                                Nenhum membro neste escritório.
                                            </TableCell>
                                        </TableRow>
                                    )}
                                </TableBody>
                            </Table>
                        </TableContainer>
                    )}
                </DialogContent>
            </Dialog>
        </Box>
    );
}
