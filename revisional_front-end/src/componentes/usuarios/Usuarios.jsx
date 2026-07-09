import React, { useCallback, useEffect, useState } from 'react';
import {
    Box, Button, Card, CardContent, Chip, CircularProgress, Dialog, DialogActions,
    DialogContent, DialogTitle, IconButton, InputAdornment, MenuItem, Switch,
    Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TextField,
    Tooltip, Typography
} from '@mui/material';
import PersonAddAltOutlinedIcon from '@mui/icons-material/PersonAddAltOutlined';
import Visibility from '@mui/icons-material/Visibility';
import VisibilityOff from '@mui/icons-material/VisibilityOff';

import { listarUsuarios, convidarUsuario, alterarAtivoUsuario } from '../../services/usuarios';
import { getIdUsuario } from '../../services/auth';
import { toastSuccess, toastError, alertValidacao, confirmAcao } from '../../services/alerts';
import { formatCpf, isValidCpf, onlyDigits } from '../../services/cpf';

const ROLES = [
    { value: 'ROLE_AUDITOR', label: 'Auditor' },
    { value: 'ROLE_ADMIN', label: 'Administrador' },
    { value: 'ROLE_VISUALIZADOR', label: 'Visualizador' },
];

const roleLabel = (role) => {
    if (role === 'ROLE_SUPER_ADMIN') return 'Plataforma';
    return ROLES.find((r) => r.value === role)?.label || role;
};

const roleColor = (role) => {
    if (role === 'ROLE_SUPER_ADMIN') return 'secondary';
    if (role === 'ROLE_ADMIN') return 'primary';
    if (role === 'ROLE_VISUALIZADOR') return 'default';
    return 'info';
};

const CONVITE_VAZIO = { nomeCompleto: '', email: '', cpf: '', oab: '', senhaTemporaria: '', role: 'ROLE_AUDITOR' };

export default function Usuarios() {
    const [usuarios, setUsuarios] = useState([]);
    const [loading, setLoading] = useState(true);
    const [dialogAberto, setDialogAberto] = useState(false);
    const [convite, setConvite] = useState(CONVITE_VAZIO);
    const [salvando, setSalvando] = useState(false);
    const [mostrarSenha, setMostrarSenha] = useState(false);

    const meuId = getIdUsuario();

    const carregar = useCallback(async () => {
        setLoading(true);
        try {
            setUsuarios(await listarUsuarios());
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => { carregar(); }, [carregar]);

    const handleConviteChange = (campo) => (event) => {
        let valor = event.target.value;
        if (campo === 'cpf') valor = formatCpf(valor);
        setConvite((c) => ({ ...c, [campo]: valor }));
    };

    const enviarConvite = async () => {
        if (!convite.nomeCompleto.trim() || !convite.oab.trim() || !convite.senhaTemporaria) {
            alertValidacao('Preencha nome, OAB e senha temporária.');
            return;
        }
        const cpfDigits = onlyDigits(convite.cpf);
        if (cpfDigits && !isValidCpf(cpfDigits)) {
            alertValidacao('CPF inválido. Verifique os dígitos.');
            return;
        }
        if (convite.senhaTemporaria.length < 6) {
            alertValidacao('A senha temporária deve ter ao menos 6 caracteres.');
            return;
        }

        setSalvando(true);
        try {
            await convidarUsuario({
                nomeCompleto: convite.nomeCompleto.trim(),
                email: convite.email.trim() || null,
                cpf: cpfDigits || null,
                oab: convite.oab.trim(),
                senhaTemporaria: convite.senhaTemporaria,
                role: convite.role,
            });
            toastSuccess('Membro convidado. Ele deverá trocar a senha no primeiro acesso.');
            setDialogAberto(false);
            setConvite(CONVITE_VAZIO);
            carregar();
        } catch {
            // erro já exibido pelo interceptor global
        } finally {
            setSalvando(false);
        }
    };

    const alternarAtivo = async (usuario) => {
        const desativar = usuario.ativo;
        const ok = await confirmAcao({
            title: desativar ? 'Desativar membro' : 'Reativar membro',
            text: desativar
                ? `${usuario.nomeCompleto} perderá o acesso imediatamente (a sessão ativa é derrubada).`
                : `${usuario.nomeCompleto} voltará a ter acesso ao sistema.`,
            icon: 'warning',
            confirmButtonText: desativar ? 'Desativar' : 'Reativar',
            danger: desativar,
        });
        if (!ok) return;
        try {
            await alterarAtivoUsuario(usuario.id, !usuario.ativo);
            toastSuccess(desativar ? 'Membro desativado.' : 'Membro reativado.');
            carregar();
        } catch {
            toastError('Não foi possível alterar o membro.');
        }
    };

    return (
        <Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2, flexWrap: 'wrap', gap: 1 }}>
                <Box>
                    <Typography variant="h5" fontWeight="bold" color="secondary.main">Usuários do Escritório</Typography>
                    <Typography variant="body2" color="text.secondary">
                        Membros do seu escritório. Novos membros entram por convite com senha temporária.
                    </Typography>
                </Box>
                <Button variant="contained" startIcon={<PersonAddAltOutlinedIcon />} onClick={() => setDialogAberto(true)}>
                    Convidar membro
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
                                        <TableCell>Nome</TableCell>
                                        <TableCell>E-mail</TableCell>
                                        <TableCell>OAB</TableCell>
                                        <TableCell>Papel</TableCell>
                                        <TableCell>Último acesso</TableCell>
                                        <TableCell align="center">Ativo</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {usuarios.map((u) => (
                                        <TableRow key={u.id} hover>
                                            <TableCell>
                                                <Typography variant="body2" fontWeight={600}>{u.nomeCompleto}</Typography>
                                                {u.forcarTrocaSenha && (
                                                    <Chip label="Aguardando 1º acesso" size="small" color="warning" variant="outlined" sx={{ mt: 0.3 }} />
                                                )}
                                            </TableCell>
                                            <TableCell>{u.email || '—'}</TableCell>
                                            <TableCell>{u.oab || '—'}</TableCell>
                                            <TableCell>
                                                <Chip label={roleLabel(u.role)} size="small" color={roleColor(u.role)} />
                                            </TableCell>
                                            <TableCell>
                                                {u.dataUltimoLogin ? new Date(u.dataUltimoLogin).toLocaleString('pt-BR') : 'Nunca acessou'}
                                            </TableCell>
                                            <TableCell align="center">
                                                <Tooltip title={u.id === meuId ? 'Você não pode desativar a própria conta' : (u.ativo ? 'Desativar' : 'Reativar')}>
                                                    <span>
                                                        <Switch
                                                            checked={u.ativo}
                                                            onChange={() => alternarAtivo(u)}
                                                            disabled={u.id === meuId || u.role === 'ROLE_SUPER_ADMIN'}
                                                            size="small"
                                                        />
                                                    </span>
                                                </Tooltip>
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                    {usuarios.length === 0 && (
                                        <TableRow>
                                            <TableCell colSpan={6} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                                                Nenhum membro encontrado.
                                            </TableCell>
                                        </TableRow>
                                    )}
                                </TableBody>
                            </Table>
                        </TableContainer>
                    )}
                </CardContent>
            </Card>

            {/* Convite de membro */}
            <Dialog open={dialogAberto} onClose={() => !salvando && setDialogAberto(false)} maxWidth="sm" fullWidth>
                <DialogTitle>Convidar membro</DialogTitle>
                <DialogContent dividers>
                    <TextField label="Nome completo" fullWidth required margin="normal"
                        value={convite.nomeCompleto} onChange={handleConviteChange('nomeCompleto')} />
                    <TextField label="E-mail" fullWidth margin="normal" type="email"
                        value={convite.email} onChange={handleConviteChange('email')} />
                    <Box sx={{ display: 'flex', gap: 2 }}>
                        <TextField label="CPF" fullWidth margin="normal"
                            value={convite.cpf} onChange={handleConviteChange('cpf')} />
                        <TextField label="OAB" fullWidth required margin="normal"
                            value={convite.oab} onChange={handleConviteChange('oab')} />
                    </Box>
                    <Box sx={{ display: 'flex', gap: 2 }}>
                        <TextField label="Papel" select fullWidth margin="normal"
                            value={convite.role} onChange={handleConviteChange('role')}>
                            {ROLES.map((r) => <MenuItem key={r.value} value={r.value}>{r.label}</MenuItem>)}
                        </TextField>
                        <TextField
                            label="Senha temporária" fullWidth required margin="normal"
                            type={mostrarSenha ? 'text' : 'password'}
                            value={convite.senhaTemporaria} onChange={handleConviteChange('senhaTemporaria')}
                            helperText="O convidado troca no primeiro acesso"
                            InputProps={{
                                endAdornment: (
                                    <InputAdornment position="end">
                                        <IconButton onClick={() => setMostrarSenha(!mostrarSenha)} edge="end" size="small">
                                            {mostrarSenha ? <VisibilityOff /> : <Visibility />}
                                        </IconButton>
                                    </InputAdornment>
                                ),
                            }}
                        />
                    </Box>
                </DialogContent>
                <DialogActions sx={{ px: 3, py: 2 }}>
                    <Button onClick={() => setDialogAberto(false)} disabled={salvando} color="inherit">Cancelar</Button>
                    <Button onClick={enviarConvite} variant="contained" disabled={salvando}
                        startIcon={salvando ? <CircularProgress size={16} color="inherit" /> : <PersonAddAltOutlinedIcon />}>
                        Convidar
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
}
