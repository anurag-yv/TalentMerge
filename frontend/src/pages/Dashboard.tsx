import React, { useEffect, useState } from 'react';
import { getDashboardStats, getHistory } from '../api';
import type { DashboardStats, UploadHistoryDto } from '../api';
import { Database, Copy, Award, FileText, AlertCircle, TrendingUp, BarChart3 } from 'lucide-react';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, BarChart, Bar, Cell } from 'recharts';

const Dashboard: React.FC = () => {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [history, setHistory] = useState<UploadHistoryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [statsRes, historyRes] = await Promise.all([
          getDashboardStats(),
          getHistory(),
        ]);
        if (statsRes.success) setStats(statsRes.data);
        if (historyRes.success) setHistory(historyRes.data.slice(0, 10)); // Keep last 10 logs
      } catch (err: any) {
        logError(err);
        setError('Could not connect to the backend server. Make sure the Spring Boot application is running.');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const logError = (err: any) => {
    console.error('Error fetching dashboard data:', err);
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
          <h3 className="mt-4 text-lg font-bold">Server Connection Offline</h3>
          <p className="mt-2 text-sm text-red-400">{error}</p>
        </div>
      </div>
    );
  }

  // Formatting chart data: upload volume over time
  const chartData = [...history]
    .reverse()
    .map((log) => ({
      name: log.fileName.length > 15 ? log.fileName.substring(0, 15) + '...' : log.fileName,
      processed: log.recordsProcessed || 0,
      sizeKB: Math.round((log.fileSize || 0) / 1024),
    }));

  const sourceTypeData = [
    { name: 'CSV (Structured)', value: history.filter(h => h.fileType === 'CSV').length },
    { name: 'PDF (Resumes)', value: history.filter(h => h.fileType === 'PDF').length },
  ];

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8 fade-in">
      <div className="mb-8">
        <h1 className="text-3xl font-extrabold tracking-tight text-white font-display">System Overview</h1>
        <p className="mt-2 text-sm text-slate-400">Monitor candidate data transformations, file merges, and parsing confidence metrics.</p>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
        {/* Total Candidates */}
        <div className="glass-card rounded-2xl p-6 relative overflow-hidden group">
          <div className="absolute right-0 top-0 -mr-6 -mt-6 h-24 w-24 rounded-full bg-brand-500/5 blur-2xl group-hover:bg-brand-500/10 transition-all duration-300"></div>
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Canonical Candidates</p>
              <h3 className="mt-2 text-3xl font-extrabold text-white tracking-tight">{stats?.totalCandidates || 0}</h3>
            </div>
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-brand-500/10 text-brand-400">
              <Database className="h-6 w-6" />
            </div>
          </div>
          <p className="mt-4 text-xs text-slate-500">Aggregated profiles in standard form</p>
        </div>

        {/* Duplicate Candidates */}
        <div className="glass-card rounded-2xl p-6 relative overflow-hidden group">
          <div className="absolute right-0 top-0 -mr-6 -mt-6 h-24 w-24 rounded-full bg-emerald-500/5 blur-2xl group-hover:bg-emerald-500/10 transition-all duration-300"></div>
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Duplicates Merged</p>
              <h3 className="mt-2 text-3xl font-extrabold text-white tracking-tight">{stats?.duplicateCandidates || 0}</h3>
            </div>
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-500/10 text-emerald-400">
              <Copy className="h-6 w-6" />
            </div>
          </div>
          <p className="mt-4 text-xs text-slate-500">Resolved conflicts and saved space</p>
        </div>

        {/* Average Confidence */}
        <div className="glass-card rounded-2xl p-6 relative overflow-hidden group">
          <div className="absolute right-0 top-0 -mr-6 -mt-6 h-24 w-24 rounded-full bg-amber-500/5 blur-2xl group-hover:bg-amber-500/10 transition-all duration-300"></div>
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Avg Confidence Score</p>
              <h3 className="mt-2 text-3xl font-extrabold text-white tracking-tight">{(stats?.averageConfidence ? stats.averageConfidence * 100 : 0).toFixed(0)}%</h3>
            </div>
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-amber-500/10 text-amber-400">
              <Award className="h-6 w-6" />
            </div>
          </div>
          {/* Confidence visual bar */}
          <div className="mt-4">
            <div className="h-1.5 w-full rounded-full bg-white/5">
              <div 
                className="h-full rounded-full bg-gradient-to-r from-amber-500 to-yellow-400" 
                style={{ width: `${(stats?.averageConfidence || 0) * 100}%` }}
              ></div>
            </div>
          </div>
        </div>

        {/* Files Processed */}
        <div className="glass-card rounded-2xl p-6 relative overflow-hidden group">
          <div className="absolute right-0 top-0 -mr-6 -mt-6 h-24 w-24 rounded-full bg-indigo-500/5 blur-2xl group-hover:bg-indigo-500/10 transition-all duration-300"></div>
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Uploaded Data Sources</p>
              <h3 className="mt-2 text-3xl font-extrabold text-white tracking-tight">{stats?.filesUploaded || 0}</h3>
            </div>
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-indigo-500/10 text-indigo-400">
              <FileText className="h-6 w-6" />
            </div>
          </div>
          <p className="mt-4 text-xs text-slate-500">Total PDF resumes and recruiter CSVs</p>
        </div>
      </div>

      {/* Chart Layout */}
      <div className="mt-8 grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Main Processing volume Chart */}
        <div className="glass-panel rounded-2xl p-6 lg:col-span-2">
          <div className="mb-6 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <TrendingUp className="h-5 w-5 text-brand-400" />
              <h3 className="text-base font-bold text-white font-display">Data Ingestion Volume</h3>
            </div>
            <span className="text-[10px] uppercase font-bold text-slate-500">records parsed per file</span>
          </div>

          <div className="h-72 w-full">
            {chartData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                  <defs>
                    <linearGradient id="colorProcessed" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#00b2a9" stopOpacity={0.2}/>
                      <stop offset="95%" stopColor="#00b2a9" stopOpacity={0}/>
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.03)" />
                  <XAxis dataKey="name" stroke="rgba(255,255,255,0.3)" fontSize={10} tickLine={false} />
                  <YAxis stroke="rgba(255,255,255,0.3)" fontSize={10} tickLine={false} />
                  <Tooltip 
                    contentStyle={{ backgroundColor: '#0e1828', border: '1px solid rgba(255,255,255,0.05)', borderRadius: '12px' }}
                    labelStyle={{ color: '#e1e8f0', fontSize: '12px', fontWeight: 'bold' }}
                    itemStyle={{ color: '#fff', fontSize: '12px' }}
                  />
                  <Area type="monotone" dataKey="processed" stroke="#00b2a9" strokeWidth={2} fillOpacity={1} fill="url(#colorProcessed)" />
                </AreaChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex h-full items-center justify-center text-sm text-slate-500">
                Upload files to visualize record volumes.
              </div>
            )}
          </div>
        </div>

        {/* Source breakdown chart */}
        <div className="glass-panel rounded-2xl p-6">
          <div className="mb-6 flex items-center gap-2">
            <BarChart3 className="h-5 w-5 text-emerald-400" />
            <h3 className="text-base font-bold text-white font-display">Source Breakdown</h3>
          </div>

          <div className="h-72 w-full flex flex-col justify-between">
            {chartData.length > 0 ? (
              <>
                <div className="h-52 w-full">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={sourceTypeData} barSize={40}>
                      <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.03)" />
                      <XAxis dataKey="name" stroke="rgba(255,255,255,0.3)" fontSize={10} tickLine={false} />
                      <YAxis stroke="rgba(255,255,255,0.3)" fontSize={10} tickLine={false} />
                      <Tooltip 
                        contentStyle={{ backgroundColor: '#1e293b', border: '1px solid rgba(255,255,255,0.05)', borderRadius: '12px' }}
                        itemStyle={{ color: '#fff', fontSize: '12px' }}
                      />
                      <Bar dataKey="value">
                        <Cell fill="#187aba" />
                        <Cell fill="#00b2a9" />
                      </Bar>
                    </BarChart>
                  </ResponsiveContainer>
                </div>
                <div className="mt-4 flex justify-around text-xs border-t border-white/5 pt-4">
                  <div className="flex items-center gap-1.5">
                    <div className="h-2 w-2 rounded bg-indigo-500"></div>
                    <span className="text-slate-400">CSV Sources ({sourceTypeData[0].value})</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <div className="h-2 w-2 rounded bg-brand-500"></div>
                    <span className="text-slate-400">PDF Resumes ({sourceTypeData[1].value})</span>
                  </div>
                </div>
              </>
            ) : (
              <div className="flex h-full items-center justify-center text-sm text-slate-500">
                No uploads found.
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
