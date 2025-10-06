import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams, Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { AdminAPI } from '../../lib/api';
import type { DataPointDefinition, DataType, RuleType } from '../../lib/types';
import { useForm } from 'react-hook-form';

export default function DataPointsPage() {
  const { id } = useParams();
  const classificationId = Number(id);
  const qc = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['datapoints', classificationId],
    queryFn: async () => (await AdminAPI.listDataPoints(classificationId)).data as DataPointDefinition[],
  });

  const { register, handleSubmit, reset } = useForm<{
    key: string;
    label?: string;
    type: DataType;
    ruleType: RuleType;
    expression: string;
    required: boolean;
  }>({
    defaultValues: { type: 'STRING', ruleType: 'REGEX', required: false },
  });

  const createMut = useMutation({
    mutationFn: (payload: any) => AdminAPI.createDataPoint(classificationId, [payload]),
    onSuccess: () => {
      toast.success('Data point created');
      qc.invalidateQueries({ queryKey: ['datapoints', classificationId] });
      reset();
    },
    onError: () => toast.error('Failed to create'),
  });

  const deleteMut = useMutation({
    mutationFn: (dpId: number) => AdminAPI.deleteDataPoint(dpId),
    onSuccess: () => {
      toast.success('Data point deleted');
      qc.invalidateQueries({ queryKey: ['datapoints', classificationId] });
    },
    onError: () => toast.error('Failed to delete'),
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-2 text-sm">
        <Link to="/app/admin/classifications" className="text-brand-700 hover:underline">Classifications</Link>
        <span className="text-slate-400">/</span>
        <span className="text-slate-600">Data Points</span>
      </div>

      <div className="bg-white border rounded p-4">
        <h2 className="text-lg font-semibold mb-3">Data Points for Classification #{classificationId}</h2>
        {isLoading ? <p>Loading…</p> : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead className="bg-slate-50">
                <tr>
                  <th className="text-left p-2">Key</th>
                  <th className="text-left p-2">Label</th>
                  <th className="text-left p-2">Type</th>
                  <th className="text-left p-2">Rule Type</th>
                  <th className="text-left p-2">Expression</th>
                  <th className="text-left p-2">Required</th>
                  <th className="text-left p-2">Actions</th>
                </tr>
              </thead>
              <tbody>
                {data?.map(dp => (
                  <tr key={dp.id} className="border-t">
                    <td className="p-2 font-medium">{dp.key}</td>
                    <td className="p-2">{dp.label || '-'}</td>
                    <td className="p-2">{dp.type}</td>
                    <td className="p-2">{dp.ruleType}</td>
                    <td className="p-2 font-mono text-xs max-w-xs truncate">{dp.expression}</td>
                    <td className="p-2">{dp.required ? 'Yes' : 'No'}</td>
                    <td className="p-2">
                      <button onClick={() => deleteMut.mutate(dp.id)} className="text-rose-700 hover:underline">Delete</button>
                    </td>
                  </tr>
                ))}
                {(!data || data.length === 0) && (
                  <tr><td className="p-3 text-slate-500" colSpan={7}>No data points defined</td></tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="bg-white border rounded p-4">
        <h3 className="font-medium mb-3">Create new data point</h3>
        <form onSubmit={handleSubmit((v) => createMut.mutate(v))} className="grid sm:grid-cols-2 gap-3">
          <div>
            <label className="block text-sm mb-1">Key</label>
            <input {...register('key', { required: true })} className="w-full border rounded px-3 py-2" placeholder="e.g., invoice_number" />
          </div>
          <div>
            <label className="block text-sm mb-1">Label (optional)</label>
            <input {...register('label')} className="w-full border rounded px-3 py-2" placeholder="e.g., Invoice Number" />
          </div>
          <div>
            <label className="block text-sm mb-1">Type</label>
            <select {...register('type')} className="w-full border rounded px-3 py-2">
              <option value="STRING">STRING</option>
              <option value="NUMBER">NUMBER</option>
              <option value="DATE">DATE</option>
              <option value="BOOLEAN">BOOLEAN</option>
              <option value="CURRENCY">CURRENCY</option>
            </select>
          </div>
          <div>
            <label className="block text-sm mb-1">Rule Type</label>
            <select {...register('ruleType')} className="w-full border rounded px-3 py-2">
              <option value="REGEX">REGEX</option>
              <option value="JSON_PATH">JSON_PATH</option>
              <option value="XPATH">XPATH</option>
            </select>
          </div>
          <div className="sm:col-span-2">
            <label className="block text-sm mb-1">Expression</label>
            <textarea {...register('expression', { required: true })} className="w-full border rounded px-3 py-2 font-mono text-sm" rows={2} placeholder="e.g., Invoice #(\d+)" />
          </div>
          <div className="sm:col-span-2">
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" {...register('required')} className="rounded" />
              Required field
            </label>
          </div>
          <div className="sm:col-span-2">
            <button className="rounded bg-brand-600 text-white px-4 py-2 hover:bg-brand-700">Create Data Point</button>
          </div>
        </form>
      </div>
    </div>
  );
}
