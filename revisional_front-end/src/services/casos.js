import api from './api';

// Paginado/ordenado pelo back. params: { page, size, sort, dir, q }.
// Retorna { content, page, size, totalElements, totalPages, last }.
export const listarCasos = async (params = {}) => (await api.get('/casos', { params })).data;
export const getCaso = async (id) => (await api.get(`/casos/${id}`)).data;
export const criarCaso = async (payload) => (await api.post('/casos', payload)).data;
export const atualizarCaso = async (id, payload) => (await api.put(`/casos/${id}`, payload)).data;
export const excluirCaso = async (id) => (await api.delete(`/casos/${id}`)).data;
export const analisarCaso = async (id, payload) => (await api.post(`/casos/${id}/analisar`, payload)).data;

// Pacote de auditoria pericial (score, matriz de itens, fingerprint, normas).
export const getAuditoria = async (id) => (await api.get(`/casos/${id}/auditoria`)).data;

// auditoria.json standalone (Blob p/ download).
export const getAuditoriaJson = async (id) =>
    (await api.get(`/casos/${id}/auditoria/download`, { responseType: 'blob' })).data;

// Taxa média BCB (SGS 25471) para preencher a referência de mercado.
export const getReferenciaBcb = async () => (await api.get('/casos/referencia-bcb')).data;

// Números do dashboard (total, laudoPronto, emAnalise, indicioForte, indicioModerado).
export const getEstatisticas = async () => (await api.get('/casos/estatisticas')).data;

// Laudo PDF (Blob). tipo: parecer | gerencial | notificacao | elementos.
export const getRelatorio = async (id, tipo) =>
    (await api.get(`/casos/${id}/relatorio/${tipo}`, { responseType: 'blob' })).data;

// Pacote ZIP do caso (4 PDFs + auditoria.json).
export const getPacoteZip = async (id) =>
    (await api.get(`/casos/${id}/pacote`, { responseType: 'blob' })).data;

// Progresso do upload/extração em andamento (etapa, mensagem, percentual) — polling.
export const getProgressoUpload = async (id) => (await api.get(`/casos/${id}/upload/progresso`)).data;

// Anexa documento (PDF/imagem/TXT). Extração OCR + IA preenche campos vazios no back.
export const uploadDocumento = async (id, arquivo, forcarOcr = false) => {
    const form = new FormData();
    form.append('documento', arquivo);
    form.append('forcarOcr', forcarOcr);
    return (await api.post(`/casos/${id}/upload`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
    })).data;
};

// Aplica os candidatos de extração escolhidos na conferência. escolhas: [{campo, origem}].
export const aplicarExtracao = async (id, escolhas) =>
    (await api.post(`/casos/${id}/extracao/aplicar`, escolhas)).data;

export const listarDocumentos = async (id) => (await api.get(`/casos/${id}/documentos`)).data;

// Conteúdo do documento (Blob) para visualização inline. Auth vai pelo header do axios.
export const getDocumentoArquivo = async (id, docId) =>
    (await api.get(`/casos/${id}/documentos/${docId}/arquivo`, { responseType: 'blob' })).data;
