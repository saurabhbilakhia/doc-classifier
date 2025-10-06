import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { DocumentAPI } from '../../lib/api';
import type { DocumentDto } from '../../lib/types';
import StatusBadge from '../../components/StatusBadge';

export default function DocumentListPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['documents'],
    queryFn: async () => {
      const response = await DocumentAPI.list();
      // Handle Spring Page response - extract content array
      return (response.data.content || response.data) as DocumentDto[];
    },
  });

  if (isLoading) return <p>Loading…</p>;

  return (
    <div>
      <div className="flex items-center justify-between mb-3">
        <h2 className="text-lg font-semibold">Your documents</h2>
        <Link to="/app/upload" className="rounded border px-3 py-1 hover:bg-slate-50">Upload</Link>
      </div>
      <div className="bg-white border rounded overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-slate-600">
            <tr>
              <th className="text-left p-3">File</th>
              <th className="text-left p-3">Status</th>
              <th className="text-left p-3">Classification</th>
              <th className="text-left p-3">Updated</th>
            </tr>
          </thead>
          <tbody>
            {data?.map(d => (
              <tr key={d.id} className="border-t hover:bg-slate-50">
                <td className="p-3"><Link className="text-brand-700 hover:underline" to={`/app/documents/${d.id}`}>{d.filename}</Link></td>
                <td className="p-3"><StatusBadge status={d.status} /></td>
                <td className="p-3">{d.classificationName ?? <span className="text-slate-400 italic">undefined</span>}</td>
                <td className="p-3">{d.updatedAt ? new Date(d.updatedAt).toLocaleString() : '-'}</td>
              </tr>
            ))}
            {(!data || data.length === 0) && (
              <tr><td className="p-4 text-slate-500" colSpan={4}>No documents yet.</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
