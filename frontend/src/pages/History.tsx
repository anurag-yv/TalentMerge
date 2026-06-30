import React, { useEffect, useState } from 'react';
import { getHistory } from '../api';
import type { UploadHistoryDto } from '../api';
import { History, FileText, CheckCircle2, XCircle, Clock, Database, AlertCircle } from 'lucide-react';

const HistoryPage: React.FC = () => {
  const [history, setHistory] = useState<UploadHistoryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchHistoryData();
  }, []);

  const fetchHistoryData = async () => {
    setLoading(true);
    try {
      const res = await getHistory();
      if (res.success) {
        setHistory(res.data);
      }
    } catch (err) {
      console.error(err);
      setError('Failed to retrieve system audit logs.');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex h-[calc(100vh-4rem)] items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-500 border-t-transparent"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-12">
        <div className="rounded-2xl border border-red-500/10 bg-red-500/5 p-6 text-center text-red-200">
          <AlertCircle className="mx-auto h-12 w-12 text-red-500" />
          <h3 className="mt-4 text-lg font-bold">Failed to load audit logs</h3>
          <p className="mt-2 text-sm text-red-400">{error}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8 fade-in">
      <div className="mb-8">
        <h1 className="text-3xl font-extrabold tracking-tight text-white font-display">System Audit Logs</h1>
        <p className="mt-2 text-sm text-slate-400">Chronological ledger of file uploads, ingestion durations, processing status, and error logs.</p>
      </div>

      {history.length > 0 ? (
        <div className="overflow-hidden border border-white/5 bg-dark-900/20 rounded-2xl">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-white/5 text-left text-xs">
              <thead className="bg-dark-950/40 text-slate-400 font-semibold uppercase tracking-wider text-[10px]">
                <tr>
                  <th scope="col" className="px-6 py-4">File Details</th>
                  <th scope="col" className="px-6 py-4">Format</th>
                  <th scope="col" className="px-6 py-4">Upload Timestamp</th>
                  <th scope="col" className="px-6 py-4">Processing Time</th>
                  <th scope="col" className="px-6 py-4">Records Imported</th>
                  <th scope="col" className="px-6 py-4">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5 bg-dark-900/10">
                {history.map((log) => (
                  <React.Fragment key={log.id}>
                    <tr className="hover:bg-white/5 transition-colors">
                      <td className="px-6 py-4 font-medium text-white max-w-xs truncate" title={log.fileName}>
                        <div className="flex items-center gap-2">
                          <FileText className="h-4 w-4 text-slate-400 shrink-0" />
                          <span className="truncate">{log.fileName}</span>
                        </div>
                        {log.fileSize && (
                          <span className="text-[10px] text-slate-500 block mt-0.5 font-semibold">
                            Size: {Math.round(log.fileSize / 1024)} KB
                          </span>
                        )}
                      </td>
                      <td className="px-6 py-4">
                        <span className={`inline-block rounded-md px-1.5 py-0.5 text-[10px] font-extrabold uppercase ${
                          log.fileType === 'PDF' ? 'bg-indigo-500/10 text-indigo-400' : 'bg-brand-500/10 text-brand-400'
                        }`}>
                          {log.fileType}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-slate-400">
                        {new Date(log.uploadTime).toLocaleString()}
                      </td>
                      <td className="px-6 py-4 text-slate-400">
                        {log.processingDurationMs !== undefined ? (
                          <div className="flex items-center gap-1">
                            <Clock className="h-3.5 w-3.5 text-slate-500" />
                            <span>{log.processingDurationMs} ms</span>
                          </div>
                        ) : (
                          'N/A'
                        )}
                      </td>
                      <td className="px-6 py-4 text-slate-400">
                        {log.recordsProcessed !== undefined ? (
                          <div className="flex items-center gap-1">
                            <Database className="h-3.5 w-3.5 text-slate-500" />
                            <span>{log.recordsProcessed} recs</span>
                          </div>
                        ) : (
                          '0 recs'
                        )}
                      </td>
                      <td className="px-6 py-4">
                        {log.status === 'PROCESSED' || log.status === 'PENDING' ? (
                          <span className="inline-flex items-center gap-1 rounded-full bg-emerald-500/10 px-2.5 py-0.5 text-[10px] font-bold text-emerald-400">
                            <CheckCircle2 className="h-3 w-3" /> Ingested
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 rounded-full bg-red-500/10 px-2.5 py-0.5 text-[10px] font-bold text-red-400">
                            <XCircle className="h-3 w-3" /> Error
                          </span>
                        )}
                      </td>
                    </tr>
                    {/* If error status, render inline error log details below the row */}
                    {log.status === 'ERROR' && log.errorMessage && (
                      <tr className="bg-red-500/5">
                        <td colSpan={6} className="px-6 py-3 border-t border-red-500/10 text-xs text-red-300 font-mono">
                          <div className="flex items-start gap-2 bg-black/10 p-3 rounded-lg border border-red-500/10 max-w-5xl">
                            <AlertCircle className="h-4 w-4 text-red-500 shrink-0 mt-0.5" />
                            <div className="break-all whitespace-pre-wrap">
                              <strong>Parser Error Log:</strong> {log.errorMessage}
                            </div>
                          </div>
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      ) : (
        <div className="rounded-2xl border border-white/5 bg-dark-900/10 p-12 text-center text-slate-400">
          <History className="mx-auto h-12 w-12 text-slate-600 mb-4" />
          <h3 className="text-sm font-bold text-white">Audit trail is empty</h3>
          <p className="mt-1 text-xs text-slate-500">Go to the "Upload Files" screen and submit your candidate data sheets or resumes.</p>
        </div>
      )}
    </div>
  );
};

export default HistoryPage;
