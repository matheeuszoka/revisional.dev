import api from './api';

export const listarCasos = async () => (await api.get('/casos')).data;
export const getCaso = async (id) => (await api.get(`/casos/${id}`)).data;
export const criarCaso = async (payload) => (await api.post('/casos', payload)).data;
export const atualizarCaso = async (id, payload) => (await api.put(`/casos/${id}`, payload)).data;
export const excluirCaso = async (id) => (await api.delete(`/casos/${id}`)).data;
