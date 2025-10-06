import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { DocumentAPI } from '../../lib/api';
import type { DocumentDto, ExtractedDataPoint } from '../../lib/types';
import StatusBadge from '../../components/StatusBadge';

export default function DocumentDetailPage() {
  const { id } = useParams();
  const docId = Number(id);

  const docQuery = useQuery({
    queryKey: ['document', docId],
    queryFn: async () => (await DocumentAPI.get(docId)).data as DocumentDto,
    refetchInterval: (query) => (query.state.data?.status === 'PROCESSING' ? 1500 : false),
  });

  const extractedQuery = useQuery({
    queryKey: ['extracted', docId],
    queryFn: async () => {
      const response = await DocumentAPI.extracted(docId);
      // Handle Spring Page response - extract content array
      return (response.data.content || response.data) as ExtractedDataPoint[];
    },
    enabled: !!docQuery.data && docQuery.data.status === 'COMPLETED',
  });

  if (docQuery.isLoading) return <p>Loading…</p>;
  if (!docQuery.data) return <p>Not found</p>;
  const doc = docQuery.data;

  return (
    <div className="space-y-4">
      <div className="bg-white border rounded p-4">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div>
            <h2 className="text-lg font-semibold">{doc.filename}</h2>
            <p className="text-sm text-slate-600">ID #{doc.id}</p>
          </div>
          <StatusBadge status={doc.status} />
        </div>
        <div className="mt-3 text-sm text-slate-700">
          <p><strong>Classification:</strong> {doc.classificationName ?? <em className="text-slate-400">undefined</em>}</p>
          <p><strong>Updated:</strong> {doc.updatedAt ? new Date(doc.updatedAt).toLocaleString() : '-'}</p>
        </div>
      </div>

      <div className="bg-white border rounded p-4">
        <h3 className="font-medium mb-2">Summary</h3>
        {doc.summary ? <p className="text-slate-700 leading-relaxed">{doc.summary}</p>
          : <p className="text-slate-500 italic">No summary available.</p>}
      </div>

      <div className="bg-white border rounded p-4">
        <h3 className="font-medium mb-3">Extracted data</h3>
        {doc.status !== 'COMPLETED' ? (
          <p className="text-slate-600">Waiting for processing to complete…</p>
        ) : extractedQuery.isLoading ? (
          <p>Loading…</p>
        ) : extractedQuery.data?.length ? (
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead className="bg-slate-50">
                <tr>
                  <th className="text-left p-2">Key</th>
                  <th className="text-left p-2">Value</th>
                  <th className="text-left p-2">Confidence</th>
                </tr>
              </thead>
              <tbody>
                {extractedQuery.data.map(x => (
                  <tr key={x.id} className="border-t">
                    <td className="p-2 font-medium">{x.key}</td>
                    <td className="p-2">{x.valueString ?? x.valueNumber ?? x.valueDate ?? ''}</td>
                    <td className="p-2">{x.confidence != null ? (x.confidence * 100).toFixed(0) + '%' : '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="text-slate-500 italic">No data points extracted.</p>
        )}
      </div>
    </div>
  );
}
