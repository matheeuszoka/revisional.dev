import api from './api';

// Matriz normativa estática (leis/súmulas/resoluções) usada na auditoria e nos laudos.
export const listarNormas = async () => (await api.get('/normas')).data;
