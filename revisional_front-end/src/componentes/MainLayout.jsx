import * as React from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
    useTheme, useMediaQuery, AppBar, Box, CssBaseline, Drawer, IconButton,
    List, ListItem, ListItemButton, ListItemIcon, ListItemText, Toolbar,
    Typography, Breadcrumbs, Link, Collapse, Avatar, Menu, MenuItem, Tooltip
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import NavigateNextIcon from '@mui/icons-material/NavigateNext';
import ExpandLess from '@mui/icons-material/ExpandLess';
import ExpandMore from '@mui/icons-material/ExpandMore';
import Logout from '@mui/icons-material/Logout';
import GavelIcon from '@mui/icons-material/Gavel';
import DashboardOutlinedIcon from '@mui/icons-material/DashboardOutlined';
import FolderSpecialOutlinedIcon from '@mui/icons-material/FolderSpecialOutlined';
import GavelOutlinedIcon from '@mui/icons-material/GavelOutlined';
import SettingsOutlinedIcon from '@mui/icons-material/SettingsOutlined';
import PersonOutlinedIcon from '@mui/icons-material/PersonOutlined';
import TuneOutlinedIcon from '@mui/icons-material/TuneOutlined';

import { logoutUsuario } from '../services/api';
import { removeSessionToken, getNomeCompleto, getUserRole, isAdmin } from '../services/auth';
import { confirmAcao, toastSuccess } from '../services/alerts';

const drawerWidth = 240;

export default function MainLayout() {
    const theme = useTheme();
    const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
    const navigate = useNavigate();
    const location = useLocation();

    const [mobileOpen, setMobileOpen] = React.useState(false);
    const [anchorEl, setAnchorEl] = React.useState(null);
    const openUserMenu = Boolean(anchorEl);

    const nome = getNomeCompleto() || 'Usuário';
    const role = (getUserRole() || '').replace('ROLE_', '');
    const initial = nome.charAt(0).toUpperCase();

    const configMenuItems = isAdmin()
        ? [
            { text: 'Usuários do Sistema', icon: <PersonOutlinedIcon />, path: '/usuarios' },
            { text: 'Parâmetros', icon: <TuneOutlinedIcon />, path: '/parametros' },
        ]
        : [];

    const [openConfig, setOpenConfig] = React.useState(
        ['/usuarios', '/parametros'].some((p) => location.pathname.startsWith(p))
    );

    const mainMenuItems = [
        { text: 'Visão Geral', icon: <DashboardOutlinedIcon />, path: '/dashboard' },
        { text: 'Casos Revisionais', icon: <FolderSpecialOutlinedIcon />, path: '/casos' },
        { text: 'Matriz Normativa', icon: <GavelOutlinedIcon />, path: '/normas' },
    ];

    const getPageTitle = () => {
        const all = [...mainMenuItems, ...configMenuItems];
        const item = all.find((i) => location.pathname.startsWith(i.path));
        return item ? item.text : 'MPG SISTEMAS';
    };

    const handleLogout = async () => {
        setAnchorEl(null);
        const ok = await confirmAcao({
            title: 'Sair do sistema',
            text: 'Deseja encerrar sua sessão?',
            icon: 'question',
            confirmButtonText: 'Sair',
        });
        if (!ok) return;
        try { await logoutUsuario(); } catch { /* segue logout local */ }
        removeSessionToken();
        toastSuccess('Sessão encerrada.');
        navigate('/login');
    };

    const itemSx = (isActive) => ({
        borderRadius: '8px',
        minHeight: 48,
        color: isActive ? 'primary.main' : 'rgba(255,255,255,0.7)',
        bgcolor: isActive ? 'rgba(255,255,255,0.1)' : 'transparent',
        '&:hover': {
            bgcolor: isActive ? 'rgba(255,255,255,0.1)' : 'rgba(255,255,255,0.05)',
            color: 'primary.main',
        },
    });

    const drawerContent = (
        <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
            <Box sx={{ height: 80, display: 'flex', alignItems: 'center', px: 3, gap: 1.5 }}>
                <GavelIcon sx={{ color: 'primary.main', fontSize: 30 }} />
                <Typography variant="h6" sx={{ fontWeight: 800, color: '#fff', letterSpacing: 1 }}>
                    MPG <Box component="span" sx={{ fontWeight: 300, opacity: 0.8 }}>SISTEMAS</Box>
                </Typography>
            </Box>

            <List sx={{ px: 1.5, flexGrow: 1 }}>
                {mainMenuItems.map((item) => {
                    const isActive = location.pathname.startsWith(item.path);
                    return (
                        <ListItem key={item.text} disablePadding sx={{ display: 'block', mb: 0.5 }}>
                            <ListItemButton onClick={() => { navigate(item.path); setMobileOpen(false); }} sx={itemSx(isActive)}>
                                <ListItemIcon sx={{ color: 'inherit', minWidth: 40 }}>{item.icon}</ListItemIcon>
                                <ListItemText primary={item.text} primaryTypographyProps={{ fontSize: '0.9rem', fontWeight: isActive ? 600 : 400 }} />
                            </ListItemButton>
                        </ListItem>
                    );
                })}

                {configMenuItems.length > 0 && (
                    <ListItem disablePadding sx={{ display: 'block', mb: 0.5, mt: 1 }}>
                        <ListItemButton onClick={() => setOpenConfig(!openConfig)} sx={itemSx(false)}>
                            <ListItemIcon sx={{ color: 'inherit', minWidth: 40 }}><SettingsOutlinedIcon /></ListItemIcon>
                            <ListItemText primary="Configurações" primaryTypographyProps={{ fontSize: '0.9rem' }} />
                            {openConfig ? <ExpandLess sx={{ fontSize: '1.2rem' }} /> : <ExpandMore sx={{ fontSize: '1.2rem' }} />}
                        </ListItemButton>
                        <Collapse in={openConfig} timeout="auto" unmountOnExit>
                            <List component="div" disablePadding>
                                {configMenuItems.map((item) => {
                                    const isActive = location.pathname.startsWith(item.path);
                                    return (
                                        <ListItemButton
                                            key={item.text}
                                            onClick={() => { navigate(item.path); setMobileOpen(false); }}
                                            sx={{ ...itemSx(isActive), minHeight: 40, pl: 5, mb: 0.5 }}
                                        >
                                            <ListItemIcon sx={{ color: 'inherit', minWidth: 35 }}>
                                                {React.cloneElement(item.icon, { sx: { fontSize: '1.1rem' } })}
                                            </ListItemIcon>
                                            <ListItemText primary={item.text} primaryTypographyProps={{ fontSize: '0.85rem', fontWeight: isActive ? 600 : 400 }} />
                                        </ListItemButton>
                                    );
                                })}
                            </List>
                        </Collapse>
                    </ListItem>
                )}
            </List>

            <Box sx={{ p: 2, textAlign: 'center' }}>
                <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.4)' }}>
                    Revisional Bancário
                </Typography>
            </Box>
        </Box>
    );

    return (
        <Box sx={{ display: 'flex' }}>
            <CssBaseline />

            {/* AppBar só no mobile */}
            <AppBar
                position="fixed"
                elevation={1}
                sx={{
                    width: { sm: `calc(100% - ${drawerWidth}px)` },
                    ml: { sm: `${drawerWidth}px` },
                    display: { sm: 'none' },
                    bgcolor: 'background.paper',
                    color: 'text.primary',
                }}
            >
                <Toolbar>
                    <IconButton color="inherit" edge="start" onClick={() => setMobileOpen(!mobileOpen)} sx={{ mr: 2 }}>
                        <MenuIcon />
                    </IconButton>
                    <Typography variant="h6" noWrap sx={{ flexGrow: 1 }}>{getPageTitle()}</Typography>
                </Toolbar>
            </AppBar>

            {/* Sidebar */}
            <Box component="nav" sx={{ width: { sm: drawerWidth }, flexShrink: { sm: 0 } }}>
                <Drawer
                    variant="temporary"
                    open={mobileOpen}
                    onClose={() => setMobileOpen(false)}
                    ModalProps={{ keepMounted: true }}
                    sx={{
                        display: { xs: 'block', sm: 'none' },
                        '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth, bgcolor: 'secondary.main', border: 'none' },
                    }}
                >
                    {drawerContent}
                </Drawer>
                <Drawer
                    variant="permanent"
                    open
                    sx={{
                        display: { xs: 'none', sm: 'block' },
                        '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth, bgcolor: 'secondary.main', borderRight: 'none' },
                    }}
                >
                    {drawerContent}
                </Drawer>
            </Box>

            {/* Conteúdo */}
            <Box
                component="main"
                sx={{
                    flexGrow: 1, minWidth: 0,
                    width: { xs: '100%', sm: `calc(100% - ${drawerWidth}px)` },
                    minHeight: '100vh', bgcolor: 'background.default',
                    display: 'flex', flexDirection: 'column',
                }}
            >
                <Toolbar sx={{ display: { sm: 'none' } }} />

                {/* Header interno: breadcrumb + perfil */}
                <Box sx={{ px: { xs: 2, sm: 4 }, pt: { xs: 2, sm: 4 }, pb: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Box sx={{ display: { xs: 'none', sm: 'block' } }}>
                        <Breadcrumbs separator={<NavigateNextIcon fontSize="small" />} aria-label="breadcrumb">
                            <Link underline="hover" color="inherit" onClick={() => navigate('/dashboard')} sx={{ cursor: 'pointer' }}>
                                MPG SISTEMAS
                            </Link>
                            <Typography color="text.primary">{getPageTitle()}</Typography>
                        </Breadcrumbs>
                    </Box>

                    <Box sx={{ display: 'flex', alignItems: 'center', ml: 'auto' }}>
                        <Tooltip title="Opções da conta">
                            <IconButton onClick={(e) => setAnchorEl(e.currentTarget)} size={isMobile ? 'small' : 'medium'}>
                                <Avatar sx={{ width: isMobile ? 34 : 40, height: isMobile ? 34 : 40, bgcolor: 'primary.main', fontWeight: 'bold' }}>
                                    {initial}
                                </Avatar>
                            </IconButton>
                        </Tooltip>

                        <Menu
                            anchorEl={anchorEl}
                            open={openUserMenu}
                            onClose={() => setAnchorEl(null)}
                            slotProps={{ paper: { sx: { mt: 1.5, width: 300, overflow: 'visible' } } }}
                            transformOrigin={{ horizontal: 'right', vertical: 'top' }}
                            anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
                        >
                            <Box sx={{ bgcolor: 'background.default', px: 2.5, py: 2.5, display: 'flex', gap: 1.5, alignItems: 'center', borderBottom: 1, borderColor: 'divider' }}>
                                <Avatar sx={{ width: 50, height: 50, bgcolor: 'primary.main', fontWeight: 'bold', fontSize: '1.5rem' }}>{initial}</Avatar>
                                <Box sx={{ flex: 1, minWidth: 0 }}>
                                    <Typography variant="subtitle2" sx={{ fontWeight: 700, lineHeight: 1.2 }}>{nome}</Typography>
                                    <Typography variant="caption" color="text.secondary">{role}</Typography>
                                </Box>
                            </Box>
                            <MenuItem onClick={handleLogout} sx={{ py: 1.5, px: 2.5, color: 'error.main', display: 'flex', gap: 1.5 }}>
                                <Logout sx={{ fontSize: '1.3rem' }} />
                                <Box>
                                    <Typography variant="body2" sx={{ fontWeight: 600 }}>Sair do Sistema</Typography>
                                    <Typography variant="caption" sx={{ opacity: 0.8 }}>Encerrar sessão</Typography>
                                </Box>
                            </MenuItem>
                        </Menu>
                    </Box>
                </Box>

                <Box sx={{ px: { xs: 2, sm: 4 }, pb: 4, flexGrow: 1 }}>
                    <Outlet />
                </Box>
            </Box>
        </Box>
    );
}
