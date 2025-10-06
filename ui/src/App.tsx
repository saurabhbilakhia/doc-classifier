import { Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { ProtectedRoute } from './components/ProtectedRoute';
import AppLayout from './components/AppLayout';

import LoginPage from './pages/Auth/LoginPage';
import RegisterPage from './pages/Auth/RegisterPage';
import ForgotPasswordPage from './pages/Auth/ForgotPasswordPage';
import ResetPasswordPage from './pages/Auth/ResetPasswordPage';

import UploadPage from './pages/Documents/UploadPage';
import DocumentListPage from './pages/Documents/DocumentListPage';
import DocumentDetailPage from './pages/Documents/DocumentDetailPage';
import ChangePasswordPage from './pages/Account/ChangePasswordPage';

import ClassificationsPage from './pages/Admin/ClassificationsPage';
import PatternsPage from './pages/Admin/PatternsPage';
import DataPointsPage from './pages/Admin/DataPointsPage';

export default function App() {
  return (
    <>
      <Routes>
        {/* Public auth routes */}
        <Route path="/auth/login" element={<LoginPage />} />
        <Route path="/auth/register" element={<RegisterPage />} />
        <Route path="/auth/forgot" element={<ForgotPasswordPage />} />
        <Route path="/auth/reset" element={<ResetPasswordPage />} />

        {/* App routes */}
        <Route path="/app" element={<ProtectedRoute><AppLayout /></ProtectedRoute>}>
          <Route index element={<Navigate to="documents" replace />} />
          <Route path="upload" element={<UploadPage />} />
          <Route path="documents" element={<DocumentListPage />} />
          <Route path="documents/:id" element={<DocumentDetailPage />} />
          <Route path="account/change-password" element={<ChangePasswordPage />} />

          {/* Admin */}
          <Route path="admin/classifications" element={<ProtectedRoute role="ADMIN"><ClassificationsPage /></ProtectedRoute>} />
          <Route path="admin/classifications/:id/patterns" element={<ProtectedRoute role="ADMIN"><PatternsPage /></ProtectedRoute>} />
          <Route path="admin/classifications/:id/data-points" element={<ProtectedRoute role="ADMIN"><DataPointsPage /></ProtectedRoute>} />
        </Route>

        {/* Fallback */}
        <Route path="*" element={<Navigate to="/auth/login" replace />} />
      </Routes>
      <Toaster position="top-right" />
    </>
  );
}
