// Utilitários de CPF: formatação (máscara) e validação (dígitos verificadores).

export const onlyDigits = (v) => (v || '').replace(/\D/g, '');

// Aplica máscara 000.000.000-00 progressivamente
export const formatCpf = (v) => {
    const d = onlyDigits(v).slice(0, 11);
    if (d.length <= 3) return d;
    if (d.length <= 6) return `${d.slice(0, 3)}.${d.slice(3)}`;
    if (d.length <= 9) return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6)}`;
    return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9)}`;
};

// Valida CPF pelos dígitos verificadores
export const isValidCpf = (v) => {
    const cpf = onlyDigits(v);
    if (cpf.length !== 11) return false;
    if (/^(\d)\1{10}$/.test(cpf)) return false; // todos iguais

    const calcDigito = (base) => {
        let soma = 0;
        for (let i = 0; i < base.length; i++) {
            soma += Number(base[i]) * (base.length + 1 - i);
        }
        const resto = (soma * 10) % 11;
        return resto === 10 ? 0 : resto;
    };

    const d1 = calcDigito(cpf.slice(0, 9));
    const d2 = calcDigito(cpf.slice(0, 10));
    return d1 === Number(cpf[9]) && d2 === Number(cpf[10]);
};

// Heurística: o texto digitado "parece" um CPF? (só dígitos/pontuação de CPF, sem letras nem @)
export const looksLikeCpf = (v) => {
    if (!v) return false;
    if (/[a-zA-Z@]/.test(v)) return false;
    return onlyDigits(v).length >= 1;
};
