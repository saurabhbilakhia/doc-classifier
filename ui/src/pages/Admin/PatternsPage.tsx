import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams, Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { AdminAPI } from '../../lib/api';
import type { ClassificationPattern } from '../../lib/types';
import { useForm } from 'react-hook-form';

export default function PatternsPage() {
  const { id } = useParams();
  const classificationId = Number(id);
  const qc = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['patterns', classificationId],
    queryFn: async () => (await AdminAPI.listPatterns(classificationId)).data as ClassificationPattern[],
  });

  const { register, handleSubmit, reset } = useForm<{ pattern: string; flags?: string }>({
    defaultValues: { flags: '' },
  });

  const createMut = useMutation({
    mutationFn: (payload: { pattern: string; flags?: string }) =>
      AdminAPI.addPatterns(classificationId, [payload]),
    onSuccess: () => {
      toast.success('Pattern added');
      qc.invalidateQueries({ queryKey: ['patterns', classificationId] });
      reset();
    },
    onError: () => toast.error('Failed to add pattern'),
  });

  const deleteMut = useMutation({
    mutationFn: (patternId: number) => AdminAPI.deletePattern(classificationId, patternId),
    onSuccess: () => {
      toast.success('Pattern deleted');
      qc.invalidateQueries({ queryKey: ['patterns', classificationId] });
    },
    onError: () => toast.error('Failed to delete'),
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-2 text-sm">
        <Link to="/app/admin/classifications" className="text-brand-700 hover:underline">Classifications</Link>
        <span className="text-slate-400">/</span>
        <span className="text-slate-600">Patterns</span>
      </div>

      <div className="bg-white border rounded p-4">
        <h2 className="text-lg font-semibold mb-3">Patterns for Classification #{classificationId}</h2>
        {isLoading ? <p>Loading…</p> : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead className="bg-slate-50">
                <tr>
                  <th className="text-left p-2">Pattern</th>
                  <th className="text-left p-2">Flags</th>
                  <th className="text-left p-2">Actions</th>
                </tr>
              </thead>
              <tbody>
                {data?.map(p => (
                  <tr key={p.id} className="border-t">
                    <td className="p-2 font-mono text-xs">{p.pattern}</td>
                    <td className="p-2">{p.flags || '-'}</td>
                    <td className="p-2">
                      <button onClick={() => deleteMut.mutate(p.id)} className="text-rose-700 hover:underline">Delete</button>
                    </td>
                  </tr>
                ))}
                {(!data || data.length === 0) && (
                  <tr><td className="p-3 text-slate-500" colSpan={3}>No patterns defined</td></tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="bg-white border rounded p-4">
        <h3 className="font-medium mb-3">Add new pattern</h3>
        <form onSubmit={handleSubmit((v) => createMut.mutate(v))} className="space-y-3">
          <div>
            <label className="block text-sm mb-1">Pattern (regex)</label>
            <input {...register('pattern', { required: true })} className="w-full border rounded px-3 py-2 font-mono text-sm" placeholder="e.g., invoice|bill" />
          </div>
          <div>
            <label className="block text-sm mb-1">Flags (optional)</label>
            <input {...register('flags')} className="w-full border rounded px-3 py-2" placeholder="e.g., i (case-insensitive)" />
          </div>
          <button className="rounded bg-brand-600 text-white px-4 py-2 hover:bg-brand-700">Add Pattern</button>
        </form>
      </div>
    </div>
  );
}
