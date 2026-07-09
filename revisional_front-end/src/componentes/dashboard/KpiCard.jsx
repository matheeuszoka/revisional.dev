import React from 'react';
import { Box, Typography, Paper, Avatar, Chip, CircularProgress } from '@mui/material';

const PRIMARY = '#1565C0';
const DARK = '#1f2937';

// Card de KPI reutilizável (padrão herdado do sigapsi).
export default function KpiCard({
    title,
    value,
    subtitle,
    icon,
    accent = PRIMARY,
    valueColor = DARK,
    badge,
    onClick,
    loading = false,
}) {
    const clickable = typeof onClick === 'function';

    return (
        <Paper
            component="article"
            elevation={0}
            onClick={onClick}
            onKeyDown={clickable ? (e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); onClick(); } } : undefined}
            tabIndex={clickable ? 0 : undefined}
            role={clickable ? 'button' : undefined}
            sx={{
                p: { xs: 2, sm: 2.5 },
                borderRadius: 2,
                height: '100%',
                boxShadow: '0 2px 10px rgba(0,0,0,0.05)',
                cursor: clickable ? 'pointer' : 'default',
                transition: 'all 0.2s ease',
                outline: 'none',
                '&:hover': clickable ? { boxShadow: '0 6px 20px rgba(0,0,0,0.12)', transform: 'translateY(-2px)' } : undefined,
                '&:focus-visible': clickable ? { boxShadow: `0 0 0 2px ${accent}` } : undefined,
            }}
        >
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Typography variant="body2" color="text.secondary" gutterBottom fontWeight={500}>
                        {title}
                    </Typography>

                    {loading ? (
                        <CircularProgress size={26} sx={{ color: accent, my: 0.5 }} />
                    ) : (
                        <Typography
                            fontWeight={700}
                            sx={{
                                color: valueColor, my: 0.5, lineHeight: 1.1,
                                fontSize: { xs: '1.4rem', sm: '1.7rem', md: '2rem' },
                                wordBreak: 'break-word',
                            }}
                        >
                            {value}
                        </Typography>
                    )}

                    {(subtitle || badge) && (
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75, flexWrap: 'wrap', mt: 0.25 }}>
                            {badge && (
                                <Chip
                                    label={badge.label}
                                    size="small"
                                    sx={{
                                        height: 18, fontSize: '0.68rem', fontWeight: 700,
                                        bgcolor: badge.color === 'error' ? '#fef2f2' : '#eef4fc',
                                        color: badge.color === 'error' ? '#dc2626' : PRIMARY,
                                    }}
                                />
                            )}
                            {subtitle && (
                                <Typography variant="caption" color="text.secondary">{subtitle}</Typography>
                            )}
                        </Box>
                    )}
                </Box>

                <Avatar
                    aria-hidden="true"
                    sx={{
                        bgcolor: accent + '18', color: accent,
                        width: { xs: 40, sm: 46 }, height: { xs: 40, sm: 46 },
                        flexShrink: 0, ml: 1,
                    }}
                >
                    {icon}
                </Avatar>
            </Box>
        </Paper>
    );
}
