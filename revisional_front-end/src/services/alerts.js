import Swal from 'sweetalert2';

const BRAND = '#1565C0';   // Azul MPG
const DANGER = '#C62828';

const toast = Swal.mixin({
    toast: true,
    position: 'top-end',
    showConfirmButton: false,
    showCloseButton: true,
    timer: 3500,
    timerProgressBar: true,
    width: 'auto',
    didOpen: (t) => {
        t.addEventListener('mouseenter', Swal.stopTimer);
        t.addEventListener('mouseleave', Swal.resumeTimer);
    },
});

export const toastSuccess = (title) => toast.fire({ icon: 'success', title });
export const toastError = (title) => toast.fire({ icon: 'error', title });
export const toastWarning = (title) => toast.fire({ icon: 'warning', title });
export const toastInfo = (title) => toast.fire({ icon: 'info', title });

export const alertError = (text, title = 'Erro') =>
    Swal.fire({ icon: 'error', title, text, confirmButtonColor: BRAND, showCloseButton: true });

export const alertValidacao = (text, title = 'Dados inválidos') =>
    Swal.fire({ icon: 'warning', title, text, confirmButtonColor: BRAND, showCloseButton: true });

export const alertSuccess = (text, title = 'Sucesso') =>
    Swal.fire({ icon: 'success', title, text, confirmButtonColor: BRAND, showCloseButton: true });

export const confirmAcao = async ({
    title = 'Confirmar',
    text = 'Deseja continuar?',
    confirmButtonText = 'Confirmar',
    cancelButtonText = 'Cancelar',
    icon = 'question',
    danger = false,
} = {}) => {
    const result = await Swal.fire({
        title, text, icon,
        showCancelButton: true,
        showCloseButton: true,
        confirmButtonText,
        cancelButtonText,
        confirmButtonColor: danger ? DANGER : BRAND,
        cancelButtonColor: '#6b7280',
        reverseButtons: true,
        focusCancel: danger,
    });
    return result.isConfirmed;
};

// Painel de progresso (spinner + mensagem atualizável). O chamador alimenta
// update() com o progresso real do back (polling). Retorna { close, update }.
export const alertProcessando = ({ title = 'Processando…', text = '' } = {}) => {
    Swal.fire({
        title,
        html: '<span></span>',
        allowOutsideClick: false,
        allowEscapeKey: false,
        showConfirmButton: false,
        didOpen: () => {
            Swal.showLoading();
            const el = Swal.getHtmlContainer();
            if (el) el.textContent = text;
        },
    });
    return {
        close: () => Swal.close(),
        update: (mensagem) => {
            const el = Swal.getHtmlContainer();
            if (el) el.textContent = mensagem;
        },
    };
};

export const confirmExclusao = ({ text = 'Esta ação não pode ser desfeita.', title = 'Confirmar exclusão' } = {}) =>
    confirmAcao({ title, text, icon: 'warning', confirmButtonText: 'Excluir', danger: true });

// Diálogo de seleção com HTML custom (checkboxes de conferência da extração).
// preConfirm coleta a escolha; retorna o result do Swal (value = retorno do preConfirm).
export const alertSelecao = ({ title, html, didOpen, preConfirm, confirmButtonText = 'Aplicar seleção' }) =>
    Swal.fire({
        title,
        html,
        width: 720,
        showCancelButton: true,
        showCloseButton: true,
        confirmButtonText,
        cancelButtonText: 'Agora não',
        confirmButtonColor: BRAND,
        cancelButtonColor: '#6b7280',
        focusConfirm: false,
        didOpen,
        preConfirm,
    });

export default Swal;
