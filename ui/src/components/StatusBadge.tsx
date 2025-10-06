import type { DocumentStatus } from '../lib/types';

export default function StatusBadge({ status }: { status: DocumentStatus }) {
  const map: Record<DocumentStatus, string> = {
    PENDING: 'bg-yellow-50 text-yellow-700 ring-yellow-200',
    PROCESSING: 'bg-blue-50 text-blue-700 ring-blue-200',
    COMPLETED: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
    FAILED: 'bg-rose-50 text-rose-700 ring-rose-200',
  };
  return (
    <span className={`px-2 py-1 text-xs rounded ring-1 ${map[status]}`}>{status}</span>
  );
}
