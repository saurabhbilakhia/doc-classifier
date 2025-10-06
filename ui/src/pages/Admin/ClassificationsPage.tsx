import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { AdminAPI } from '../../lib/api';
import type { Classification } from '../../lib/types';
import { useForm } from 'react-hook-form';

export default function ClassificationsPage() {
  const qc = useQueryClient();
  const { data, isLoading } = useQuery({
    queryKey: ['classifications'],
    queryFn: async () => (await AdminAPI.listClassifications()).data as Classification[],
  });

  const { register, handleSubmit, reset } = useForm<{name: string; description?: string; priority?: number; threshold?: number}>({
    defaultValues: { priority: 0, threshold: 0.5 },
  });

  const createMut = useMutation({
    mutationFn: (payload: any) => AdminAPI.createClassification(payload),
    onSuccess: () => { toast.success('Classification created'); qc.invalidateQueries({ queryKey: ['classifications'] }); reset(); },
    onError: () => toast.error('Failed to create'),
  });

  const deleteMut = useMutation({
    mutationFn: (id: number) => AdminAPI.deleteClassification(id),
    onSuccess: () => { toast.success('Deleted'); qc.invalidateQueries({ queryKey: ['classifications'] }); },
    onError: () => toast.error('Failed to delete'),
  });

  return (
    <div className="space-y-6">
      <div className="bg-white border rounded p-4">
        <h2 className="text-lg font-semibold mb-3">Classifications</h2>
        {isLoading ? <p>Loading…</p> : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead className="bg-slate-50">
                <tr>
                  <th className="text-left p-2">Name</th>
                  <th className="text-left p-2">Priority</th>
                  <th className="text-left p-2">Threshold</th>
                  <th className="text-left p-2">Actions</th>
                </tr>
              </thead>
              <tbody>
                {data?.map(c => (
                  <tr key={c.id} className="border-t">
                    <td className="p-2 font-medium">{c.name}</td>
                    <td className="p-2">{c.priority}</td>
                    <td className="p-2">{c.threshold}</td>
                    <td className="p-2 space-x-2">
                      <Link to={`/app/admin/classifications/${c.id}/patterns`} className="text-brand-700 hover:underline">Patterns</Link>
                      <Link to={`/app/admin/classifications/${c.id}/data-points`} className="text-brand-700 hover:underline">Data points</Link>
                      {c.name !== 'undefined' && (
                        <button onClick={() => deleteMut.mutate(c.id)} className="text-rose-700 hover:underline">Delete</button>
                      )}
                    </td>
                  </tr>
                ))}
                {(!data || data.length === 0) && (
                  <tr><td className="p-3 text-slate-500" colSpan={4}>No classifications</td></tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="bg-white border rounded p-4">
        <h3 className="font-medium mb-3">Create new classification</h3>
        <form onSubmit={handleSubmit((v) => createMut.mutate(v))} className="grid sm:grid-cols-2 gap-3">
          <div>
            <label className="block text-sm mb-1">Name</label>
            <input {...register('name', { required: true })} className="w-full border rounded px-3 py-2" />
          </div>
          <div>
            <label className="block text-sm mb-1">Priority</label>
            <input type="number" {...register('priority', { valueAsNumber: true })} className="w-full border rounded px-3 py-2" />
          </div>
          <div>
            <label className="block text-sm mb-1">Threshold</label>
            <input step="0.05" type="number" {...register('threshold', { valueAsNumber: true })} className="w-full border rounded px-3 py-2" />
          </div>
          <div className="sm:col-span-2">
            <label className="block text-sm mb-1">Description</label>
            <textarea {...register('description')} className="w-full border rounded px-3 py-2" rows={3} />
          </div>
          <div className="sm:col-span-2">
            <button className="rounded bg-brand-600 text-white px-4 py-2 hover:bg-brand-700">Create</button>
          </div>
        </form>
      </div>
    </div>
  );
}
