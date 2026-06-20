import React, { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
    AppBar, Box, CssBaseline, Drawer, IconButton, List, ListItem,
    ListItemButton, ListItemIcon, ListItemText, Toolbar, Typography,
    Divider, Avatar, Tooltip
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import DashboardIcon from '@mui/icons-material/Dashboard';
import FolderSpecialIcon from '@mui/icons-material/FolderSpecial';
import PeopleIcon from '@mui/icons-material/People';
import LogoutIcon from '@mui/icons-material/Logout';
import GavelIcon from '@mui/icons-material/Gavel';

import { logoutUsuario } from '../services/api';
import { removeSessionToken, getNomeCompleto, isAdmin } from '../services/auth';
import { confirmAcao, toastSuccess } from '../services/alerts';

const drawerWidth = 250;

const MainLayout = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const [mobileOpen, setMobileOpen] = useState(false);

    const nome = getNomeCompleto() || 'Usuário';

    const menu = [
        { label: 'Dashboard', icon: <DashboardIcon />, path: '/dashboard' },
        { label: 'Casos Revisionais', icon: <FolderSpecialIcon />, path: '/casos' },
        ...(isAdmin() ? [{ label: 'Usuários', icon: <PeopleIcon />, path: '/usuarios' }] : []),
    ];

    const handleLogout = async () => {
        const ok = await confirmAcao({
            title: 'Sair do sistema',
            text: 'Deseja encerrar sua sessão?',
            icon: 'question',
            confirmButtonText: 'Sair',
        });
        if (!ok) return;
        try { await logoutUsuario(); } catch { /* segue o logout local mesmo se falhar */ }
        removeSessionToken();
        toastSuccess('Sessão encerrada.');
        navigate('/login');
    };

    const drawer = (
        <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
            <Toolbar sx={{ gap: 1 }}>
                <GavelIcon color="primary" />
                <Typography variant="h6" fontWeight="800" color="secondary.main">MPG</Typography>
                <Typography variant="h6" fontWeight="300" color="text.secondary">SISTEMAS</Typography>
            </Toolbar>
            <Divider />
            <List sx={{ flexGrow: 1 }}>
                {menu.map((item) => (
                    <ListItem key={item.path} disablePadding>
                        <ListItemButton
                            selected={location.pathname === item.path}
                            onClick={() => { navigate(item.path); setMobileOpen(false); }}
                        >
                            <ListItemIcon>{item.icon}</ListItemIcon>
                            <ListItemText primary={item.label} />
                        </ListItemButton>
                    </ListItem>
                ))}
            </List>
            <Divider />
            <List>
                <ListItem disablePadding>
                    <ListItemButton onClick={handleLogout}>
                        <ListItemIcon><LogoutIcon /></ListItemIcon>
                        <ListItemText primary="Sair" />
                    </ListItemButton>
                </ListItem>
            </List>
        </Box>
    );

    return (
        <Box sx={{ display: 'flex' }}>
            <CssBaseline />
            <AppBar
                position="fixed"
                color="secondary"
                sx={{ width: { md: `calc(100% - ${drawerWidth}px)` }, ml: { md: `${drawerWidth}px` } }}
            >
                <Toolbar>
                    <IconButton color="inherit" edge="start" onClick={() => setMobileOpen(!mobileOpen)} sx={{ mr: 2, display: { md: 'none' } }}>
                        <MenuIcon />
                    </IconButton>
                    <Typography variant="h6" noWrap sx={{ flexGrow: 1 }}>
                        MPG SISTEMAS · Revisional Bancário
                    </Typography>
                    <Tooltip title={nome}>
                        <Avatar sx={{ bgcolor: 'primary.main', width: 36, height: 36 }}>
                            {nome.charAt(0).toUpperCase()}
                        </Avatar>
                    </Tooltip>
                </Toolbar>
            </AppBar>

            <Box component="nav" sx={{ width: { md: drawerWidth }, flexShrink: { md: 0 } }}>
                <Drawer
                    variant="temporary"
                    open={mobileOpen}
                    onClose={() => setMobileOpen(false)}
                    ModalProps={{ keepMounted: true }}
                    sx={{ display: { xs: 'block', md: 'none' }, '& .MuiDrawer-paper': { width: drawerWidth } }}
                >
                    {drawer}
                </Drawer>
                <Drawer
                    variant="permanent"
                    open
                    sx={{ display: { xs: 'none', md: 'block' }, '& .MuiDrawer-paper': { width: drawerWidth, boxSizing: 'border-box' } }}
                >
                    {drawer}
                </Drawer>
            </Box>

            <Box component="main" sx={{ flexGrow: 1, p: 3, width: { md: `calc(100% - ${drawerWidth}px)` }, minHeight: '100vh', bgcolor: 'background.default' }}>
                <Toolbar />
                <Outlet />
            </Box>
        </Box>
    );
};

export default MainLayout;
