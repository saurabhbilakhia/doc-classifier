import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { useAuth } from '../../providers/AuthProvider';
import { useNavigate, Link } from 'react-router-dom';
import toast from 'react-hot-toast';

const schema = z.object({
  email: z.string().email(),
  password: z.string().min(6),
});
type FormData = z.infer<typeof schema>;

export default function RegisterPage() {
  const { register: reg, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({ resolver: zodResolver(schema) });
  const auth = useAuth();
  const nav = useNavigate();

  async function onSubmit(data: FormData) {
    try {
      await auth.register(data.email, data.password);
      toast.success('Account created!');
      nav('/app', { replace: true });
    } catch (e: any) {
      toast.error('Registration failed');
    }
  }

  return (
    <div className="min-h-dvh grid place-items-center bg-slate-50 px-4">
      <form onSubmit={handleSubmit(onSubmit)} className="w-full max-w-sm bg-white p-6 rounded-lg border">
        <h1 className="text-xl font-semibold mb-4">Create account</h1>
        <label className="block text-sm mb-1">Email</label>
        <input {...reg('email')} className="w-full mb-2 rounded border px-3 py-2" placeholder="you@company.com" />
        {errors.email && <p className="text-sm text-rose-600">{errors.email.message}</p>}
        <label className="block text-sm mt-3 mb-1">Password</label>
        <input type="password" {...reg('password')} className="w-full mb-2 rounded border px-3 py-2" />
        {errors.password && <p className="text-sm text-rose-600">{errors.password.message}</p>}

        <button disabled={isSubmitting} className="mt-4 w-full rounded bg-brand-600 text-white px-4 py-2 hover:bg-brand-700 disabled:opacity-50">
          {isSubmitting ? 'Creating account…' : 'Create account'}
        </button>

        <div className="mt-4 text-center text-sm">
          <Link to="/auth/login" className="text-slate-600 hover:underline">Already have an account? Sign in</Link>
        </div>
      </form>
    </div>
  );
}
