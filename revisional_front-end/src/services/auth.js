// Gestão de sessão/JWT — versão melhorada em relação ao sigapsi:
//  - cookie com SameSite=Strict + Secure (em https) e Max-Age derivado do exp do token
//    (antes: cookie de sessão sem expiração nem flags de segurança)
//  - validação client-side da expiração (isTokenValid) — não espera o 401 do servidor
//  - helpers de papel/identidade a partir do payload do JWT

const TOKEN_KEY = 'revisional_token';

const decodeToken = (token) => {
    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
            .padEnd(Math.ceil(base64Url.length / 4) * 4, '=');
        const jsonPayload = decodeURIComponent(
            atob(base64).split('')
                .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
                .join('')
        );
        return JSON.parse(jsonPayload);
    } catch {
        return null;
    }
};

// --- Cookie helpers ---
export const setSessionToken = (token) => {
    const decoded = decodeToken(token);
    // Max-Age = segundos até expirar (fallback 2h). Cookie morre junto com o token.
    let maxAge = 2 * 60 * 60;
    if (decoded?.exp) {
        const secs = decoded.exp - Math.floor(Date.now() / 1000);
        maxAge = secs > 0 ? secs : 0;
    }
    const secure = window.location.protocol === 'https:' ? '; Secure' : '';
    document.cookie = `${TOKEN_KEY}=${token}; path=/; max-age=${maxAge}; SameSite=Strict${secure}`;
};

export const getSessionToken = () => {
    const name = `${TOKEN_KEY}=`;
    const cookies = decodeURIComponent(document.cookie).split(';');
    for (let c of cookies) {
        c = c.trim();
        if (c.indexOf(name) === 0) return c.substring(name.length);
    }
    return null;
};

export const removeSessionToken = () => {
    document.cookie = `${TOKEN_KEY}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/; SameSite=Strict`;
};

// --- Decodificação / validação ---
export const getDecodedToken = () => {
    const token = getSessionToken();
    return token ? decodeToken(token) : null;
};

// Melhoria: valida a expiração no cliente (com 5s de margem)
export const isTokenValid = () => {
    const decoded = getDecodedToken();
    if (!decoded?.exp) return false;
    return decoded.exp * 1000 > Date.now() + 5000;
};

// Se houver token mas estiver expirado, limpa e retorna false
export const isAuthenticated = () => {
    if (!getSessionToken()) return false;
    if (!isTokenValid()) {
        removeSessionToken();
        return false;
    }
    return true;
};

// --- Identidade / papéis ---
export const getUserRole = () => getDecodedToken()?.role || null;
export const getIdUsuario = () => getDecodedToken()?.id_usuario || null;
export const getNomeCompleto = () => getDecodedToken()?.nome_completo || null;

export const isAdmin = () => getUserRole() === 'ROLE_ADMIN';
export const isAuditor = () => ['ROLE_AUDITOR', 'ROLE_ADMIN'].includes(getUserRole());
export const isVisualizador = () => getUserRole() === 'ROLE_VISUALIZADOR';
