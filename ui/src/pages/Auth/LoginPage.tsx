import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { useAuth } from '../../providers/AuthProvider';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import toast from 'react-hot-toast';

const schema = z.object({
  email: z.string().email(),
  password: z.string().min(6),
});
type FormData = z.infer<typeof schema>;

export default function LoginPage() {
  const { register: reg, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({ resolver: zodResolver(schema) });
  const auth = useAuth();
  const nav = useNavigate();
  const loc = useLocation() as any;

  async function onSubmit(data: FormData) {
    try {
      await auth.login(data.email, data.password);
      toast.success('Welcome back!');
      const to = loc.state?.from?.pathname ?? '/app';
      nav(to, { replace: true });
    } catch (e: any) {
      toast.error('Invalid credentials');
    }
  }

  return (
    <div className="min-h-dvh grid place-items-center bg-slate-50 px-4">
      <form onSubmit={handleSubmit(onSubmit)} className="w-full max-w-sm bg-white p-6 rounded-lg border">
        <h1 className="text-xl font-semibold mb-4">Sign in</h1>
        <label className="block text-sm mb-1">Email</label>
        <input {...reg('email')} className="w-full mb-2 rounded border px-3 py-2" placeholder="you@company.com" />
        {errors.email && <p className="text-sm text-rose-600">{errors.email.message}</p>}
        <label className="block text-sm mt-3 mb-1">Password</label>
        <input type="password" {...reg('password')} className="w-full mb-2 rounded border px-3 py-2" />
        {errors.password && <p className="text-sm text-rose-600">{errors.password.message}</p>}

        <button disabled={isSubmitting} className="mt-4 w-full rounded bg-brand-600 text-white px-4 py-2 hover:bg-brand-700 disabled:opacity-50">
          {isSubmitting ? 'Signing in…' : 'Sign in'}
        </button>

        <div className="mt-4 flex justify-between text-sm">
          <Link to="/auth/forgot" className="text-brand-700 hover:underline">Forgot password?</Link>
          <Link to="/auth/register" className="text-slate-600 hover:underline">Create account</Link>
        </div>
      </form>
    </div>
  );
}
