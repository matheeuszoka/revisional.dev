import { createTheme } from '@mui/material/styles';

// Identidade MPG SISTEMAS — azul corporativo (confiança/jurídico-financeiro)
const theme = createTheme({
    palette: {
        primary: {
            main: '#1565C0',   // Azul MPG
            dark: '#0D47A1',
            contrastText: '#fff',
        },
        secondary: {
            main: '#102A43',   // Azul-marinho da barra lateral
            contrastText: '#fff',
        },
        success: { main: '#2E7D32' },
        error: { main: '#C62828' },
        warning: { main: '#ED6C02' },
        background: {
            default: '#F4F6F8',
        },
    },
    typography: {
        fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
        button: {
            textTransform: 'none',
            fontWeight: 600,
        },
    },
    components: {
        MuiButton: {
            styleOverrides: {
                root: {
                    borderRadius: '8px',
                    boxShadow: 'none',
                    '&:hover': { boxShadow: '0 4px 8px rgba(0,0,0,0.1)' },
                },
            },
        },
        MuiPaper: {
            styleOverrides: {
                root: {
                    borderRadius: '12px',
                    boxShadow: '0 2px 10px rgba(0,0,0,0.04)',
                },
            },
        },
        MuiDrawer: {
            styleOverrides: { paper: { borderRadius: 0 } },
        },
        MuiTextField: {
            defaultProps: { size: 'small' },
            styleOverrides: {
                root: {
                    '& .MuiOutlinedInput-root': {
                        borderRadius: '8px',
                        backgroundColor: '#fafafa',
                    },
                },
            },
        },
    },
});

export default theme;
