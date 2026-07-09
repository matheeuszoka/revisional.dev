import api from './api';

// Parâmetros operacionais (OCR + IA). Somente admin. A chave da IA nunca volta em
// claro: o GET traz openRouterApiKeyDefinida (boolean).
export const getConfiguracoes = async () => (await api.get('/configuracoes')).data;
export const salvarConfiguracoes = async (payload) => (await api.put('/configuracoes', payload)).data;
