import React, { useCallback, useEffect, useState } from 'react';
import {
    Box, Button, Card, CardContent, Chip, CircularProgress, Dialog, DialogActions,
    DialogContent, DialogTitle, Divider, Grid, IconButton, Switch, Table, TableBody,
    TableCell, TableContainer, TableHead, TableRow, TextField, Tooltip, Typography
} from '@mui/material';
import GroupOutlinedIcon from '@mui/icons-material/GroupOutlined';
import BusinessOutlinedIcon from '@mui/icons-material/BusinessOutlined';
import AddBusinessOutlinedIcon from '@mui/icons-material/AddBusinessOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';

import { listarTenants, listarUsuariosDoTenant, alterarAtivoTenant, criarTenant, editarTenant } from '../../services/tenants';
import { toastSuccess, confirmAcao, alertValidacao } from '../../services/alerts';
import { formatCnpj, cleanCnpj, isValidCnpj } from '../../services/cnpj';

const exibirCnpj = (cnpj) => (cnpj ? formatCnpj(cnpj) : '—');

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

    // Dialogs de criação/edição de escritório
    const NOVO_VAZIO = {
        nome: '', cnpj: '',
        adminNome: '', adminCpf: '', adminEmail: '', adminOab: '', senhaTemporaria: '',
    };
    const [novoAberto, setNovoAberto] = useState(false);
    const [novo, setNovo] = useState(NOVO_VAZIO);
    const [salvandoNovo, setSalvandoNovo] = useState(false);
    const [edicao, setEdicao] = useState(null); // { id, nome, cnpj } | null
    const [salvandoEdicao, setSalvandoEdicao] = useState(false);

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

    const setNovoCampo = (campo) => (e) => setNovo({ ...novo, [campo]: e.target.value });

    const salvarNovo = async () => {
        if (novo.cnpj && !isValidCnpj(novo.cnpj)) {
            alertValidacao('CNPJ inválido. Verifique os dígitos (aceita o padrão numérico e o alfanumérico).');
            return;
        }
        setSalvandoNovo(true);
        try {
            await criarTenant({ ...novo, cnpj: cleanCnpj(novo.cnpj) });
            toastSuccess('Escritório criado. Informe a senha temporária ao administrador do cliente.');
            setNovoAberto(false);
            setNovo(NOVO_VAZIO);
            carregar();
        } catch {
            /* interceptor mostra toast (validações do back) */
        } finally {
            setSalvandoNovo(false);
        }
    };

    const salvarEdicao = async () => {
        if (edicao.cnpj && !isValidCnpj(edicao.cnpj)) {
            alertValidacao('CNPJ inválido. Verifique os dígitos (aceita o padrão numérico e o alfanumérico).');
            return;
        }
        setSalvandoEdicao(true);
        try {
            await editarTenant(edicao.id, { nome: edicao.nome, cnpj: cleanCnpj(edicao.cnpj) });
            toastSuccess('Escritório atualizado.');
            setEdicao(null);
            carregar();
        } catch {
            /* interceptor mostra toast */
        } finally {
            setSalvandoEdicao(false);
        }
    };

    return (
        <Box>
            <Box sx={{ mb: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 2 }}>
                <Box>
                    <Typography variant="h5" fontWeight="bold" color="secondary.main">Escritórios</Typography>
                    <Typography variant="body2" color="text.secondary">
                        Administração da plataforma: todos os escritórios (tenants) cadastrados no sistema.
                    </Typography>
                </Box>
                <Button variant="contained" startIcon={<AddBusinessOutlinedIcon />} onClick={() => setNovoAberto(true)}>
                    Novo escritório
                </Button>
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
                                            <TableCell>{exibirCnpj(t.cnpj)}</TableCell>
                                            <TableCell>
                                                {t.dataCriacao ? new Date(t.dataCriacao).toLocaleDateString('pt-BR') : '—'}
                                            </TableCell>
                                            <TableCell align="center">
                                                <Tooltip title="Ver membros">
                                                    <IconButton size="small" onClick={() => abrirMembros(t)}>
                                                        <GroupOutlinedIcon fontSize="small" />
                                                    </IconButton>
                                                </Tooltip>
                                                <Tooltip title="Editar nome/CNPJ">
                                                    <IconButton size="small" onClick={() => setEdicao({ id: t.id, nome: t.nome || '', cnpj: t.cnpj || '' })}>
                                                        <EditOutlinedIcon fontSize="small" />
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

            {/* Novo escritório: tenant + admin inicial do cliente (senha temporária) */}
            <Dialog open={novoAberto} onClose={() => setNovoAberto(false)} maxWidth="sm" fullWidth>
                <DialogTitle>Novo escritório</DialogTitle>
                <DialogContent dividers>
                    <Grid container spacing={2} sx={{ mt: 0 }}>
                        <Grid size={{ xs: 12, sm: 7 }}>
                            <TextField label="Nome do escritório" fullWidth required value={novo.nome} onChange={setNovoCampo('nome')} />
                        </Grid>
                        <Grid size={{ xs: 12, sm: 5 }}>
                            <TextField label="CNPJ" fullWidth value={novo.cnpj}
                                onChange={(e) => setNovo({ ...novo, cnpj: formatCnpj(e.target.value) })}
                                placeholder="00.000.000/0000-00"
                                helperText="Numérico ou alfanumérico (novo padrão)." />
                        </Grid>
                        <Grid size={{ xs: 12 }}>
                            <Divider textAlign="left">
                                <Typography variant="caption" color="text.secondary">Administrador do cliente</Typography>
                            </Divider>
                        </Grid>
                        <Grid size={{ xs: 12 }}>
                            <TextField label="Nome completo" fullWidth required value={novo.adminNome} onChange={setNovoCampo('adminNome')} />
                        </Grid>
                        <Grid size={{ xs: 12, sm: 6 }}>
                            <TextField label="CPF" fullWidth value={novo.adminCpf} onChange={setNovoCampo('adminCpf')} />
                        </Grid>
                        <Grid size={{ xs: 12, sm: 6 }}>
                            <TextField label="OAB" fullWidth required value={novo.adminOab} onChange={setNovoCampo('adminOab')} />
                        </Grid>
                        <Grid size={{ xs: 12, sm: 6 }}>
                            <TextField label="E-mail" type="email" fullWidth value={novo.adminEmail} onChange={setNovoCampo('adminEmail')} />
                        </Grid>
                        <Grid size={{ xs: 12, sm: 6 }}>
                            <TextField label="Senha temporária" fullWidth required value={novo.senhaTemporaria}
                                onChange={setNovoCampo('senhaTemporaria')}
                                helperText="O admin será obrigado a trocá-la no 1º login." />
                        </Grid>
                    </Grid>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setNovoAberto(false)} disabled={salvandoNovo}>Cancelar</Button>
                    <Button variant="contained" onClick={salvarNovo}
                        disabled={salvandoNovo || !novo.nome.trim() || !novo.adminNome.trim() || !novo.adminOab.trim() || !novo.senhaTemporaria.trim()}>
                        {salvandoNovo ? <CircularProgress size={20} color="inherit" /> : 'Criar escritório'}
                    </Button>
                </DialogActions>
            </Dialog>

            {/* Editar nome/CNPJ */}
            <Dialog open={!!edicao} onClose={() => setEdicao(null)} maxWidth="xs" fullWidth>
                <DialogTitle>Editar escritório</DialogTitle>
                <DialogContent dividers>
                    <Grid container spacing={2} sx={{ mt: 0 }}>
                        <Grid size={{ xs: 12 }}>
                            <TextField label="Nome do escritório" fullWidth required value={edicao?.nome || ''}
                                onChange={(e) => setEdicao({ ...edicao, nome: e.target.value })} />
                        </Grid>
                        <Grid size={{ xs: 12 }}>
                            <TextField label="CNPJ" fullWidth value={formatCnpj(edicao?.cnpj || '')}
                                onChange={(e) => setEdicao({ ...edicao, cnpj: formatCnpj(e.target.value) })}
                                placeholder="00.000.000/0000-00"
                                helperText="Numérico ou alfanumérico (novo padrão)." />
                        </Grid>
                    </Grid>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setEdicao(null)} disabled={salvandoEdicao}>Cancelar</Button>
                    <Button variant="contained" onClick={salvarEdicao} disabled={salvandoEdicao || !edicao?.nome?.trim()}>
                        {salvandoEdicao ? <CircularProgress size={20} color="inherit" /> : 'Salvar'}
                    </Button>
                </DialogActions>
            </Dialog>

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
