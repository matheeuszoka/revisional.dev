// CNPJ — suporta o padrão numérico clássico e o novo alfanumérico da Receita
// Federal (12 primeiras posições [0-9A-Z] + 2 dígitos verificadores numéricos).
// Máscara única para ambos: XX.XXX.XXX/XXXX-XX.

export const cleanCnpj = (v) => (v || '').toUpperCase().replace(/[^0-9A-Z]/g, '').slice(0, 14);

// Máscara progressiva (aplicar no onChange do input).
export const formatCnpj = (v) => {
    const s = cleanCnpj(v);
    let out = s.slice(0, 2);
    if (s.length > 2) out += '.' + s.slice(2, 5);
    if (s.length > 5) out += '.' + s.slice(5, 8);
    if (s.length > 8) out += '/' + s.slice(8, 12);
    if (s.length > 12) out += '-' + s.slice(12, 14);
    return out;
};

// Valor de cálculo por caractere (padrão RFB): código ASCII - 48.
// '0'..'9' → 0..9; 'A'..'Z' → 17..42.
const valorChar = (c) => c.charCodeAt(0) - 48;

const digitoVerificador = (base) => {
    const pesos = base.length === 12
        ? [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]
        : [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
    const soma = base.split('').reduce((acc, c, i) => acc + valorChar(c) * pesos[i], 0);
    const resto = soma % 11;
    return resto < 2 ? 0 : 11 - resto;
};

export const isValidCnpj = (v) => {
    const s = cleanCnpj(v);
    if (s.length !== 14) return false;
    // DVs são sempre numéricos, mesmo no CNPJ alfanumérico.
    if (!/^[0-9A-Z]{12}[0-9]{2}$/.test(s)) return false;
    if (/^(.)\1{13}$/.test(s)) return false; // sequência repetida (ex.: 00000000000000)
    return digitoVerificador(s.slice(0, 12)) === valorChar(s[12])
        && digitoVerificador(s.slice(0, 13)) === valorChar(s[13]);
};
