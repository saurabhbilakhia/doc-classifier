import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { AuthAPI } from '../../lib/api';

const schema = z.object({
  newPassword: z.string().min(8),
});
type FormData = z.infer<typeof schema>;

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({ resolver: zodResolver(schema) });
  const nav = useNavigate();

  async function onSubmit(data: FormData) {
    if (!token) {
      toast.error('Invalid reset token');
      return;
    }
    try {
      await AuthAPI.reset({ token, newPassword: data.newPassword });
      toast.success('Password reset successful!');
      nav('/auth/login', { replace: true });
    } catch (e: any) {
      toast.error('Failed to reset password');
    }
  }

  return (
    <div className="min-h-dvh grid place-items-center bg-slate-50 px-4">
      <form onSubmit={handleSubmit(onSubmit)} className="w-full max-w-sm bg-white p-6 rounded-lg border">
        <h1 className="text-xl font-semibold mb-4">Reset password</h1>
        <label className="block text-sm mb-1">New password</label>
        <input type="password" {...register('newPassword')} className="w-full mb-2 rounded border px-3 py-2" />
        {errors.newPassword && <p className="text-sm text-rose-600">{errors.newPassword.message}</p>}

        <button disabled={isSubmitting} className="mt-4 w-full rounded bg-brand-600 text-white px-4 py-2 hover:bg-brand-700 disabled:opacity-50">
          {isSubmitting ? 'Resetting…' : 'Reset password'}
        </button>

        <div className="mt-4 text-center text-sm">
          <Link to="/auth/login" className="text-slate-600 hover:underline">Back to sign in</Link>
        </div>
      </form>
    </div>
  );
}
