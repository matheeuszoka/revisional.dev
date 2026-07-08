import axios from 'axios';
import { getSessionToken, getDecodedToken, setSessionToken, isTokenValid, removeSessionToken } from './auth';
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

// --- Renovação de sessão (sliding): se o token expira em < 30min, renova em
// background no próximo uso da API. Single-flight (uma renovação por vez);
// o back preserva a sessão única (novo token substitui token_ativo).
let refreshEmAndamento = null;
const REFRESH_LIMIAR_MS = 30 * 60 * 1000;

const renovarSeNecessario = (urlOriginal) => {
    if (urlOriginal?.includes('/auth/')) return; // evita loop no próprio refresh/login
    const decoded = getDecodedToken();
    if (!decoded?.exp) return;
    const restanteMs = decoded.exp * 1000 - Date.now();
    if (restanteMs > REFRESH_LIMIAR_MS || restanteMs <= 0) return;
    if (!refreshEmAndamento) {
        refreshEmAndamento = api.post('/auth/refresh', null, { skipGlobalError: true })
            .then((r) => { if (r.data?.token) setSessionToken(r.data.token); })
            .catch(() => { /* renovação é best-effort; 401 real cai no fluxo de logout */ })
            .finally(() => { refreshEmAndamento = null; });
    }
};

// Response: 401 → logout e redireciona; demais erros → toast global
api.interceptors.response.use(
    (response) => {
        renovarSeNecessario(response.config?.url);
        return response;
    },
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
