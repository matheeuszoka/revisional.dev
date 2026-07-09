import api from './api';

// Membros do escritório (tenant do admin autenticado)
export const listarUsuarios = async () => {
    const response = await api.get('/usuarios');
    return response.data;
};

// Convite: cria membro com senha temporária (troca obrigatória no 1º login)
export const convidarUsuario = async (dados) => {
    const response = await api.post('/usuarios', dados);
    return response.data;
};

export const alterarAtivoUsuario = async (id, ativo) => {
    const response = await api.patch(`/usuarios/${id}/ativo?ativo=${ativo}`);
    return response.data;
};

// Troca da própria senha (limpa forcar_troca_senha)
export const alterarMinhaSenha = async (senhaAtual, novaSenha) => {
    const response = await api.put('/usuarios/senha', { senhaAtual, novaSenha }, { skipGlobalError: true });
    return response.data;
};
