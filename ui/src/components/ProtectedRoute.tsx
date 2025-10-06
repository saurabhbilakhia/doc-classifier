import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../providers/AuthProvider';
import type { Role } from '../lib/types';

export const ProtectedRoute: React.FC<{ role?: Role; children: React.ReactNode }> = ({ role, children }) => {
  const { isAuthenticated, role: userRole } = useAuth();
  const loc = useLocation();

  if (!isAuthenticated) return <Navigate to="/auth/login" replace state={{ from: loc }} />;
  if (role && userRole !== role) return <Navigate to="/app" replace />;

  return <>{children}</>;
};
