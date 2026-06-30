import React, { useState, useRef } from 'react';
import { uploadCSV, uploadResume } from '../api';
import type { UploadHistoryDto } from '../api';
import { Upload, FileText, CheckCircle2, AlertTriangle, RefreshCw, X } from 'lucide-react';

const UploadPage: React.FC = () => {
  const [dragActive, setDragActive] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileType, setFileType] = useState<'CSV' | 'PDF' | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [result, setResult] = useState<UploadHistoryDto | null>(null);
  const [error, setError] = useState<string | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      const file = e.dataTransfer.files[0];
      validateAndSetFile(file);
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      validateAndSetFile(file);
    }
  };

  const validateAndSetFile = (file: File) => {
    setError(null);
    setResult(null);
    const name = file.name.toLowerCase();

    if (name.endsWith('.csv')) {
      setSelectedFile(file);
      setFileType('CSV');
    } else if (name.endsWith('.pdf')) {
      setSelectedFile(file);
      setFileType('PDF');
    } else {
      setSelectedFile(null);
      setFileType(null);
      setError('Unsupported file type. Please upload a structured Recruiter .csv file or a Resume .pdf file.');
    }
  };

  const triggerFileSelect = () => {
    if (fileInputRef.current) {
      fileInputRef.current.click();
    }
  };

  const clearSelection = () => {
    setSelectedFile(null);
    setFileType(null);
    setResult(null);
    setError(null);
    setUploadProgress(0);
  };

  const handleUploadSubmit = async () => {
    if (!selectedFile || !fileType) return;

    setUploading(true);
    setUploadProgress(20);
    setError(null);

    // Mock progress stepping for visuals
    const progressInterval = setInterval(() => {
      setUploadProgress((prev) => {
        if (prev >= 80) {
          clearInterval(progressInterval);
          return 80;
        }
        return prev + 15;
      });
    }, 150);

    try {
      let res;
      if (fileType === 'CSV') {
        res = await uploadCSV(selectedFile);
      } else {
        res = await uploadResume(selectedFile);
      }

      clearInterval(progressInterval);
      setUploadProgress(100);

      if (res.success) {
        setResult(res.data);
      } else {
        setError(res.message);
      }
    } catch (err: any) {
      clearInterval(progressInterval);
      console.error(err);
      setError(err.response?.data?.message || 'File ingestion failed. The file structure may be corrupted or invalid.');
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="mx-auto max-w-3xl px-4 py-8 sm:px-6 lg:px-8 fade-in">
      <div className="mb-8">
        <h1 className="text-3xl font-extrabold tracking-tight text-white font-display">Ingest Candidate Data</h1>
        <p className="mt-2 text-sm text-slate-400">Drag and drop files to ingest structured CSV tables or parse unstructured resumes via PDFBox.</p>
      </div>

      <div className="grid grid-cols-1 gap-6">
        {/* Upload Box */}
        {!selectedFile ? (
          <div
            onDragEnter={handleDrag}
            onDragOver={handleDrag}
            onDragLeave={handleDrag}
            onDrop={handleDrop}
            onClick={triggerFileSelect}
            className={`flex flex-col items-center justify-center rounded-2xl border-2 border-dashed p-12 text-center cursor-pointer transition-all duration-300 ${
              dragActive 
                ? 'border-brand-500 bg-brand-500/5' 
                : 'border-white/10 bg-dark-900/40 hover:border-brand-500/50 hover:bg-dark-900/60'
            }`}
          >
            <input
              ref={fileInputRef}
              type="file"
              onChange={handleFileChange}
              accept=".csv,.pdf"
              className="hidden"
            />
            <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-white/5 text-slate-400 mb-4 border border-white/5 shadow-inner">
              <Upload className="h-8 w-8 animate-bounce text-brand-400" />
            </div>
            <h3 className="text-lg font-bold text-white font-display">Drag and Drop File</h3>
            <p className="mt-2 text-xs text-slate-400 max-w-sm">
              Support structured recruiter candidate spreadsheet <span className="text-brand-400 font-semibold">.CSV</span> or unstructured candidate resume <span className="text-brand-400 font-semibold">.PDF</span> documents.
            </p>
            <button className="mt-6 rounded-xl bg-brand-600 px-4 py-2 text-xs font-bold text-white hover:bg-brand-500 shadow-lg shadow-brand-500/10 transition-all duration-200">
              Browse Local Files
            </button>
          </div>
        ) : (
          /* File Preview and Actions */
          <div className="glass-panel rounded-2xl p-6 relative">
            <button 
              onClick={clearSelection} 
              disabled={uploading}
              className="absolute right-4 top-4 text-slate-400 hover:text-white disabled:opacity-55"
            >
              <X className="h-5 w-5" />
            </button>

            <div className="flex items-center gap-4">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-brand-500/10 text-brand-400 border border-brand-500/20">
                <FileText className="h-6 w-6" />
              </div>
              <div className="overflow-hidden pr-8">
                <h4 className="text-sm font-bold text-white truncate">{selectedFile.name}</h4>
                <p className="text-[10px] text-slate-500 mt-1 uppercase font-semibold">
                  Type: <span className="text-brand-400">{fileType}</span> • Size: {Math.round(selectedFile.size / 1024)} KB
                </p>
              </div>
            </div>

            {/* Ingress Progress Bar */}
            {uploading && (
              <div className="mt-6">
                <div className="flex justify-between text-xs font-semibold text-slate-400 mb-2">
                  <span>Parsing File Data...</span>
                  <span>{uploadProgress}%</span>
                </div>
                <div className="h-1.5 w-full rounded-full bg-white/5 overflow-hidden">
                  <div 
                    className="h-full rounded-full bg-gradient-to-r from-brand-600 to-indigo-400 transition-all duration-300"
                    style={{ width: `${uploadProgress}%` }}
                  ></div>
                </div>
              </div>
            )}

            {/* Actions */}
            {!uploading && !result && (
              <div className="mt-6 flex justify-end gap-3 border-t border-white/5 pt-4">
                <button
                  onClick={clearSelection}
                  className="rounded-xl border border-white/5 bg-white/5 px-4 py-2 text-xs font-bold text-slate-300 hover:bg-white/10"
                >
                  Cancel
                </button>
                <button
                  onClick={handleUploadSubmit}
                  className="rounded-xl bg-brand-600 px-5 py-2 text-xs font-bold text-white hover:bg-brand-500 shadow-md shadow-brand-500/10"
                >
                  Upload & Ingest
                </button>
              </div>
            )}
          </div>
        )}

        {/* Results Banner */}
        {result && (
          <div className="rounded-2xl border border-emerald-500/10 bg-emerald-500/5 p-6 flex gap-4 fade-in">
            <CheckCircle2 className="h-8 w-8 text-emerald-400 shrink-0" />
            <div>
              <h3 className="text-sm font-bold text-white font-display">Ingestion Successful</h3>
              <p className="text-xs text-emerald-400/80 mt-1">
                The file <span className="font-semibold text-white">"{result.fileName}"</span> was successfully ingested and parsed.
              </p>
              <div className="mt-4 grid grid-cols-2 gap-4 border-t border-emerald-500/10 pt-4 text-[11px] text-slate-400">
                <div>
                  <span className="block text-slate-500 uppercase font-semibold">Records Parsed</span>
                  <span className="text-white font-bold text-sm">{result.recordsProcessed}</span>
                </div>
                <div>
                  <span className="block text-slate-500 uppercase font-semibold">Parsing Duration</span>
                  <span className="text-white font-bold text-sm">{result.processingDurationMs} ms</span>
                </div>
              </div>
              <p className="mt-4 text-[10px] text-slate-500 italic">
                Note: Ingested profiles are saved as raw candidate records. Go to the "Transformation" tab to run deduplication and build canonical profiles.
              </p>
            </div>
          </div>
        )}

        {/* Error Alert */}
        {error && (
          <div className="rounded-2xl border border-red-500/15 bg-red-500/5 p-6 flex gap-4 fade-in">
            <AlertTriangle className="h-8 w-8 text-red-500 shrink-0" />
            <div>
              <h3 className="text-sm font-bold text-white font-display">File Validation Error</h3>
              <p className="text-xs text-red-400/90 mt-1">{error}</p>
              <button 
                onClick={clearSelection}
                className="mt-4 flex items-center gap-1 text-[11px] font-bold text-red-400 hover:text-white"
              >
                <RefreshCw className="h-3 w-3" /> Retry Selection
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default UploadPage;
