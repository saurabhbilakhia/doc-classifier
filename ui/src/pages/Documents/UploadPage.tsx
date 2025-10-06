import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { DocumentAPI } from '../../lib/api';

export default function UploadPage() {
  const [file, setFile] = useState<File | null>(null);
  const [progress, setProgress] = useState(0);
  const nav = useNavigate();

  const { mutate, isPending } = useMutation({
    mutationFn: async () => {
      if (!file) throw new Error('No file');
      const { data } = await DocumentAPI.upload(file, setProgress);
      return data;
    },
    onSuccess: (doc) => {
      toast.success('Uploaded');
      nav(`/app/documents/${doc.id}`);
    },
    onError: () => toast.error('Upload failed'),
  });

  return (
    <div className="max-w-lg">
      <h2 className="text-lg font-semibold mb-3">Upload a document</h2>
      <div
        onDrop={(e) => {
          e.preventDefault();
          const f = e.dataTransfer.files?.[0];
          if (f) setFile(f);
        }}
        onDragOver={(e) => e.preventDefault()}
        className="border-2 border-dashed rounded p-6 mb-3 bg-white"
      >
        <p className="text-sm text-slate-600">Drag & drop a file here, or click to choose</p>
        <input
          type="file"
          accept=".pdf,.doc,.docx,.png,.jpg,.jpeg,.txt"
          className="mt-3"
          onChange={(e) => setFile(e.target.files?.[0] ?? null)}
        />
      </div>
      {file && <p className="text-sm mb-3">Selected: <strong>{file.name}</strong> ({Math.round((file.size/1024/1024)*10)/10} MB)</p>}
      <button
        disabled={!file || isPending}
        onClick={() => mutate()}
        className="rounded bg-brand-600 text-white px-4 py-2 hover:bg-brand-700 disabled:opacity-50"
      >
        {isPending ? 'Uploading…' : 'Upload'}
      </button>

      {isPending && (
        <div className="mt-4">
          <div className="h-2 bg-slate-200 rounded">
            <div className="h-2 bg-brand-600 rounded" style={{ width: `${progress}%` }} />
          </div>
          <p className="text-xs mt-1 text-slate-600">{progress}%</p>
        </div>
      )}
    </div>
  );
}
