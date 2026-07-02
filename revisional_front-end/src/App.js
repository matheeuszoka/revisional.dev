import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';

import theme from './theme';
import MainLayout from './componentes/MainLayout';
import LoginPage from './componentes/login/Login';
import Dashboard from './componentes/Dashboard';
import Casos from './componentes/casos/Casos';
import CasoForm from './componentes/casos/CasoForm';
import Normas from './componentes/normas/Normas';
import Parametros from './componentes/config/Parametros';
import Usuarios from './componentes/usuarios/Usuarios';
import Escritorios from './componentes/escritorios/Escritorios';
import { isAuthenticated, isAdmin, isSuperAdmin } from './services/auth';

const RotaProtegida = ({ children }) => {
    if (!isAuthenticated()) return <Navigate to="/login" replace />;
    return children;
};

const RotaPublica = ({ children }) => {
    if (isAuthenticated()) return <Navigate to="/dashboard" replace />;
    return children;
};

const RotaAdmin = ({ children }) => {
    if (!isAuthenticated()) return <Navigate to="/login" replace />;
    if (!isAdmin()) return <Navigate to="/dashboard" replace />;
    return children;
};

const RotaSuperAdmin = ({ children }) => {
    if (!isAuthenticated()) return <Navigate to="/login" replace />;
    if (!isSuperAdmin()) return <Navigate to="/dashboard" replace />;
    return children;
};

function App() {
    return (
        <ThemeProvider theme={theme}>
            <CssBaseline />
            <BrowserRouter>
                <Routes>
                    <Route path="/login" element={<RotaPublica><LoginPage /></RotaPublica>} />

                    <Route path="/" element={<RotaProtegida><MainLayout /></RotaProtegida>}>
                        <Route index element={<Navigate to="/dashboard" replace />} />
                        <Route path="/dashboard" element={<Dashboard />} />
                        <Route path="/casos" element={<Casos />} />
                        <Route path="/casos/novo" element={<CasoForm />} />
                        <Route path="/casos/:id" element={<CasoForm />} />
                        <Route path="/normas" element={<Normas />} />
                        <Route path="/usuarios" element={<RotaAdmin><Usuarios /></RotaAdmin>} />
                        <Route path="/escritorios" element={<RotaSuperAdmin><Escritorios /></RotaSuperAdmin>} />
                        <Route path="/parametros" element={<RotaAdmin><Parametros /></RotaAdmin>} />
                    </Route>

                    <Route path="*" element={<Navigate to="/dashboard" replace />} />
                </Routes>
            </BrowserRouter>
        </ThemeProvider>
    );
}

export default App;
