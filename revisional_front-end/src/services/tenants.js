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

// Cria escritório + admin inicial do cliente (senha temporária, troca no 1º login).
// payload: { nome, cnpj, adminNome, adminCpf, adminEmail, adminOab, senhaTemporaria }
export const criarTenant = async (payload) => {
    const response = await api.post('/tenants', payload);
    return response.data;
};

// Altera nome/CNPJ de um escritório existente.
export const editarTenant = async (id, payload) => {
    const response = await api.put(`/tenants/${id}`, payload);
    return response.data;
};

// Desativar bloqueia o login e as requisições de todos os membros
export const alterarAtivoTenant = async (id, ativo) => {
    const response = await api.patch(`/tenants/${id}/ativo?ativo=${ativo}`);
    return response.data;
};
