import api from './api';

// Administração da plataforma (SUPER_ADMIN): escritórios/tenants

export const listarTenants = async () => {
    const response = await api.get('/tenants');
    return response.data;
};

export const listarUsuariosDoTenant = async (id) => {
    const response = await api.get(`/tenants/${id}/usuarios`);
    return response.data;
};

// Desativar bloqueia o login e as requisições de todos os membros
export const alterarAtivoTenant = async (id, ativo) => {
    const response = await api.patch(`/tenants/${id}/ativo?ativo=${ativo}`);
    return response.data;
};
