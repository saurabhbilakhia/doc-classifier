import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { AuthAPI } from '../../lib/api';

const schema = z.object({
  email: z.string().email(),
});
type FormData = z.infer<typeof schema>;

export default function ForgotPasswordPage() {
  const { register, handleSubmit, formState: { errors, isSubmitting }, reset } = useForm<FormData>({ resolver: zodResolver(schema) });

  async function onSubmit(data: FormData) {
    try {
      await AuthAPI.forgot(data);
      toast.success('Password reset email sent!');
      reset();
    } catch (e: any) {
      toast.error('Failed to send reset email');
    }
  }

  return (
    <div className="min-h-dvh grid place-items-center bg-slate-50 px-4">
      <form onSubmit={handleSubmit(onSubmit)} className="w-full max-w-sm bg-white p-6 rounded-lg border">
        <h1 className="text-xl font-semibold mb-4">Forgot password</h1>
        <p className="text-sm text-slate-600 mb-4">Enter your email to receive a password reset link.</p>
        <label className="block text-sm mb-1">Email</label>
        <input {...register('email')} className="w-full mb-2 rounded border px-3 py-2" placeholder="you@company.com" />
        {errors.email && <p className="text-sm text-rose-600">{errors.email.message}</p>}

        <button disabled={isSubmitting} className="mt-4 w-full rounded bg-brand-600 text-white px-4 py-2 hover:bg-brand-700 disabled:opacity-50">
          {isSubmitting ? 'Sending…' : 'Send reset link'}
        </button>

        <div className="mt-4 text-center text-sm">
          <Link to="/auth/login" className="text-slate-600 hover:underline">Back to sign in</Link>
        </div>
      </form>
    </div>
  );
}
