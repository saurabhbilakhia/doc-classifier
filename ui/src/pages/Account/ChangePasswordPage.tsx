import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import toast from 'react-hot-toast';
import { AuthAPI } from '../../lib/api';

const schema = z.object({
  currentPassword: z.string().min(6),
  newPassword: z.string().min(8),
});
type FormData = z.infer<typeof schema>;

export default function ChangePasswordPage() {
  const { register, handleSubmit, formState: { errors, isSubmitting }, reset } = useForm<FormData>({ resolver: zodResolver(schema) });

  async function onSubmit(data: FormData) {
    try {
      await AuthAPI.change(data);
      toast.success('Password updated');
      reset();
    } catch {
      toast.error('Failed to update password');
    }
  }

  return (
    <div className="max-w-md">
      <h2 className="text-lg font-semibold mb-4">Change password</h2>
      <form onSubmit={handleSubmit(onSubmit)} className="bg-white border rounded p-4">
        <label className="block text-sm mb-1">Current password</label>
        <input type="password" {...register('currentPassword')} className="w-full border rounded px-3 py-2" />
        {errors.currentPassword && <p className="text-sm text-rose-600">{errors.currentPassword.message}</p>}
        <label className="block text-sm mt-3 mb-1">New password</label>
        <input type="password" {...register('newPassword')} className="w-full border rounded px-3 py-2" />
        {errors.newPassword && <p className="text-sm text-rose-600">{errors.newPassword.message}</p>}
        <button disabled={isSubmitting} className="mt-4 rounded bg-brand-600 text-white px-4 py-2 hover:bg-brand-700">
          Save
        </button>
      </form>
    </div>
  );
}
