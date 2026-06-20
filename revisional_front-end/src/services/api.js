import axios from 'axios';
import { getSessionToken, isTokenValid, removeSessionToken } from './auth';
import { toastError } from './alerts';

const api = axios.create({
    baseURL: '/api',
});

// Request: anexa o JWT. Melhoria: se o token existe mas já expirou (checagem
// client-side), faz logout local antes de disparar a chamada — não gasta um
// round-trip só para receber 401.
api.interceptors.request.use((config) => {
    const token = getSessionToken();
    if (token) {
        if (!isTokenValid() && !config.url?.includes('/auth/login')) {
            removeSessionToken();
            if (!window.location.pathname.includes('/login')) {
                window.location.href = '/login';
            }
            return Promise.reject(new axios.Cancel('Sessão expirada.'));
        }
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// Response: 401 → logout e redireciona; demais erros → toast global
api.interceptors.response.use(
    (response) => response,
    async (error) => {
        if (axios.isCancel(error)) return Promise.reject(error);

        const status = error.response?.status;

        if (status === 401) {
            if (!window.location.pathname.includes('/login')) {
                removeSessionToken();
                window.location.href = '/login';
            }
            return Promise.reject(error);
        }

        if (!error.config?.skipGlobalError) {
            toastError(await extractErrorMessage(error));
        }
        return Promise.reject(error);
    }
);

const extractErrorMessage = async (error) => {
    const res = error.response;
    if (!res) return 'Falha de conexão com o servidor.';
    let data = res.data;
    if (data instanceof Blob) {
        try { data = JSON.parse(await data.text()); } catch { data = null; }
    }
    if (typeof data === 'string' && data.trim()) return data;
    const msg = data && (data.message || data.error || data.erro || data.mensagem);
    return msg || `Erro ${res.status}${res.statusText ? ': ' + res.statusText : ''}`;
};

// --- Autenticação ---
export const loginUsuario = async (credentials, force = false) => {
    const response = await api.post(`/auth/login?force=${force}`, credentials, { skipGlobalError: true });
    return response.data;
};

export const logoutUsuario = async () => {
    const response = await api.post('/auth/logout');
    return response.data;
};

export const validarSessao = async () => {
    const response = await api.get('/auth/validate');
    return response.data;
};

export const registrarUsuario = async (userData) => {
    const response = await api.post('/auth/register', userData);
    return response.data;
};

export default api;
