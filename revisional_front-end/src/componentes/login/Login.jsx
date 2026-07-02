import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Box, Button, CssBaseline, TextField, Paper, Typography,
    InputAdornment, IconButton, CircularProgress
} from '@mui/material';

import GavelIcon from '@mui/icons-material/Gavel';
import Visibility from '@mui/icons-material/Visibility';
import VisibilityOff from '@mui/icons-material/VisibilityOff';
import LoginIcon from '@mui/icons-material/Login';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';

import { loginUsuario, logoutUsuario } from '../../services/api';
import { setSessionToken, removeSessionToken } from '../../services/auth';
import { alterarMinhaSenha } from '../../services/usuarios';
import Swal, { toastSuccess, toastError, alertValidacao, confirmAcao } from '../../services/alerts';
import { formatCpf, isValidCpf, looksLikeCpf, onlyDigits } from '../../services/cpf';

const Login = () => {
    const navigate = useNavigate();

    const [credentials, setCredentials] = useState({ login: '', senha: '' });
    const [showPassword, setShowPassword] = useState(false);
    const [loading, setLoading] = useState(false);

    const handleChange = (prop) => (event) => {
        setCredentials({ ...credentials, [prop]: event.target.value });
    };

    // Login aceita email/cpf/oab. Se o valor for numérico, aplica máscara de CPF.
    const handleLoginChange = (event) => {
        const raw = event.target.value;
        setCredentials({ ...credentials, login: looksLikeCpf(raw) ? formatCpf(raw) : raw });
    };

    // Primeiro acesso via convite: senha temporária deve ser trocada agora.
    // Cancelar = não entra (token descartado pelo chamador).
    const trocarSenhaObrigatoria = async (senhaTemporaria) => {
        const result = await Swal.fire({
            title: 'Defina sua nova senha',
            text: 'Você entrou com uma senha temporária. Escolha uma senha definitiva para continuar.',
            icon: 'info',
            html: `
                <input id="swal-nova-senha" type="password" class="swal2-input" placeholder="Nova senha (mín. 6 caracteres)">
                <input id="swal-confirma-senha" type="password" class="swal2-input" placeholder="Confirme a nova senha">
            `,
            focusConfirm: false,
            showCancelButton: true,
            confirmButtonText: 'Salvar e entrar',
            cancelButtonText: 'Cancelar',
            confirmButtonColor: '#1565C0',
            allowOutsideClick: false,
            preConfirm: () => {
                const nova = document.getElementById('swal-nova-senha').value;
                const confirma = document.getElementById('swal-confirma-senha').value;
                if (!nova || nova.length < 6) {
                    Swal.showValidationMessage('A nova senha deve ter ao menos 6 caracteres.');
                    return false;
                }
                if (nova !== confirma) {
                    Swal.showValidationMessage('As senhas não conferem.');
                    return false;
                }
                if (nova === senhaTemporaria) {
                    Swal.showValidationMessage('A nova senha deve ser diferente da temporária.');
                    return false;
                }
                return nova;
            },
        });

        if (!result.isConfirmed || !result.value) return false;

        try {
            await alterarMinhaSenha(senhaTemporaria, result.value);
            toastSuccess('Senha alterada com sucesso.');
            return true;
        } catch {
            toastError('Não foi possível alterar a senha. Tente novamente.');
            return false;
        }
    };

    const handleLogin = async (event, force = false) => {
        if (event) event.preventDefault();

        if (!credentials.login || !credentials.senha) {
            alertValidacao('Por favor, preencha todos os campos.');
            return;
        }

        // Se for CPF (numérico), valida e envia só os dígitos; email/oab vão como digitados
        let loginToSend = credentials.login.trim();
        if (looksLikeCpf(credentials.login)) {
            const digits = onlyDigits(credentials.login);
            if (digits.length === 11 && !isValidCpf(digits)) {
                alertValidacao('CPF inválido. Verifique os dígitos.');
                return;
            }
            loginToSend = digits;
        }

        setLoading(true);
        try {
            const data = await loginUsuario(
                { login: loginToSend, senha: credentials.senha },
                force
            );

            const token = data.token || (typeof data === 'string' ? data : null);
            if (token) setSessionToken(token);

            // Convite com senha temporária: obriga a troca antes de entrar
            if (data.forcarTrocaSenha) {
                const trocou = await trocarSenhaObrigatoria(credentials.senha);
                if (!trocou) {
                    // Desiste: derruba a sessão no servidor e descarta o token local
                    try { await logoutUsuario(); } catch { /* segue logout local */ }
                    removeSessionToken();
                    return;
                }
            }

            toastSuccess('Login efetuado com sucesso');
            navigate('/dashboard');
        } catch (err) {
            const status = err.response?.status;
            if (status === 409) {
                setLoading(false);
                const confirmar = await confirmAcao({
                    title: 'Sessão já ativa',
                    text: 'Este usuário já tem uma sessão ativa em outro dispositivo/navegador. Deseja desconectar a sessão anterior e entrar aqui?',
                    icon: 'warning',
                    confirmButtonText: 'Desconectar e entrar',
                    cancelButtonText: 'Cancelar',
                });
                if (confirmar) handleLogin(null, true);
                return;
            } else if (status === 401 || status === 403) {
                toastError(status === 403 ? 'Conta temporariamente bloqueada.' : 'Credenciais inválidas.');
            } else if (err.code === 'ERR_NETWORK') {
                toastError('Erro de conexão. Verifique se o servidor está no ar.');
            } else {
                toastError('Ocorreu um erro ao entrar. Tente novamente.');
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <Box component="main" sx={{ display: 'flex', minHeight: '100vh', overflow: 'hidden' }}>
            <CssBaseline />

            {/* --- LADO ESQUERDO: Marca MPG --- */}
            <Box
                sx={{
                    flexGrow: 1,
                    background: (theme) => `linear-gradient(135deg, ${theme.palette.primary.main} 20%, ${theme.palette.secondary.main} 90%)`,
                    display: { xs: 'none', md: 'flex' },
                    flexDirection: 'column',
                    justifyContent: 'center',
                    alignItems: 'center',
                    color: '#fff',
                    position: 'relative',
                }}
            >
                <Box sx={{ position: 'relative', textAlign: 'center', p: 4 }}>
                    <Box sx={{ display: 'inline-flex', p: 3, borderRadius: '50%', bgcolor: 'rgba(255,255,255,0.12)', mb: 3, backdropFilter: 'blur(10px)' }}>
                        <GavelIcon sx={{ fontSize: 96, color: '#fff' }} />
                    </Box>
                    <Typography component="h1" variant="h2" fontWeight="800" sx={{ letterSpacing: 4 }}>
                        MPG SISTEMAS
                    </Typography>
                    <Typography variant="h6" sx={{ mt: 1, fontWeight: 300, opacity: 0.9, letterSpacing: 1 }}>
                        Sistema Revisional Bancário
                    </Typography>
                    <Typography variant="body2" sx={{ mt: 3, opacity: 0.7 }}>
                        Auditoria de contratos · Engenharia reversa de juros · Laudos técnicos
                    </Typography>
                </Box>
            </Box>

            {/* --- LADO DIREITO: Formulário --- */}
            <Paper
                elevation={0}
                square
                sx={{
                    width: { xs: '100%', md: '450px', lg: '500px' },
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    justifyContent: 'center',
                    boxShadow: '-10px 0 30px rgba(0,0,0,0.1)',
                    bgcolor: '#fff',
                }}
            >
                <Box sx={{ width: '100%', maxWidth: '400px', p: { xs: 4, sm: 5 }, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                    <Box sx={{ display: { xs: 'flex', md: 'none' }, mb: 2 }}>
                        <GavelIcon sx={{ fontSize: 56, color: 'primary.main' }} />
                    </Box>

                    <Box sx={{ m: 1, bgcolor: 'rgba(21, 101, 192, 0.1)', p: 1.5, borderRadius: '16px', color: 'primary.main', mb: 2 }}>
                        <LockOutlinedIcon fontSize="large" />
                    </Box>

                    <Typography component="h1" variant="h5" fontWeight="bold" color="text.primary" sx={{ mb: 1 }}>
                        Acesso ao Sistema
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 4, textAlign: 'center' }}>
                        Entre com seu e-mail, CPF ou OAB.
                    </Typography>

                    <Box component="form" noValidate onSubmit={(e) => handleLogin(e, false)} sx={{ width: '100%' }}>
                        <TextField
                            margin="normal"
                            required
                            fullWidth
                            id="login"
                            label="E-mail, CPF ou OAB"
                            name="login"
                            autoFocus
                            value={credentials.login}
                            onChange={handleLoginChange}
                            disabled={loading}
                        />
                        <TextField
                            margin="normal"
                            required
                            fullWidth
                            name="senha"
                            label="Senha"
                            type={showPassword ? 'text' : 'password'}
                            id="senha"
                            value={credentials.senha}
                            onChange={handleChange('senha')}
                            disabled={loading}
                            InputProps={{
                                endAdornment: (
                                    <InputAdornment position="end">
                                        <IconButton onClick={() => setShowPassword(!showPassword)} edge="end">
                                            {showPassword ? <VisibilityOff /> : <Visibility />}
                                        </IconButton>
                                    </InputAdornment>
                                ),
                            }}
                        />
                        <Button
                            type="submit"
                            fullWidth
                            variant="contained"
                            color="primary"
                            size="large"
                            disabled={loading}
                            sx={{ mt: 3, py: 1.5, fontSize: '1.1rem' }}
                            startIcon={!loading && <LoginIcon />}
                        >
                            {loading ? <CircularProgress size={24} color="inherit" /> : 'Entrar'}
                        </Button>
                    </Box>

                    <Typography variant="caption" color="text.secondary" sx={{ mt: 5 }}>
                        © {new Date().getFullYear()} MPG SISTEMAS
                    </Typography>
                </Box>
            </Paper>
        </Box>
    );
};

export default Login;
