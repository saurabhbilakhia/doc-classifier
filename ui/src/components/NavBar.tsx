import { Link, NavLink } from 'react-router-dom';
import { useAuth } from '../providers/AuthProvider';

export default function NavBar() {
  const { email, logout, role } = useAuth();
  return (
    <header className="w-full border-b bg-white">
      <div className="mx-auto max-w-6xl flex items-center justify-between px-4 py-3">
        <Link to="/app" className="font-semibold text-brand-700">Document AI</Link>
        <nav className="flex items-center gap-4 text-sm">
          <NavLink to="/app/upload" className="hover:text-brand-600">Upload</NavLink>
          <NavLink to="/app/documents" className="hover:text-brand-600">Documents</NavLink>
          {role === 'ADMIN' && <NavLink to="/app/admin/classifications" className="hover:text-brand-600">Admin</NavLink>}
          <div className="flex items-center gap-3 pl-3 ml-3 border-l">
            <span className="hidden sm:inline text-slate-600">{email}</span>
            <NavLink to="/app/account/change-password" className="hover:text-brand-600">Change Password</NavLink>
            <button onClick={logout} className="rounded px-3 py-1 border hover:bg-slate-50">Logout</button>
          </div>
        </nav>
      </div>
    </header>
  );
}
