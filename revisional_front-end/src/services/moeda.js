// Utilitários de moeda BRL: máscara progressiva (centavos) e parse para Number.

const digits = (v) => String(v ?? '').replace(/\D/g, '');

// Aplica máscara a partir dos dígitos, tratando os 2 últimos como centavos.
// Ex.: "123456" -> "1.234,56"
export const formatMoeda = (v) => {
    const d = digits(v);
    if (!d) return '';
    const n = Number(d) / 100;
    return n.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
};

// Formata um Number já existente (ex.: vindo da API) para a máscara "1.234,56".
export const numberToMoeda = (n) => {
    if (n === null || n === undefined || n === '') return '';
    return Number(n).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
};

// Converte máscara "1.234,56" -> 1234.56 (Number) ou null se vazio.
export const parseMoeda = (v) => {
    const d = digits(v);
    if (!d) return null;
    return Number(d) / 100;
};
