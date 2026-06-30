import React, { useState } from 'react';
import { triggerTransform } from '../api';
import type { TransformationSummaryDto } from '../api';
import { Cpu, Play, CheckCircle2, ChevronRight, Info, AlertTriangle, ShieldCheck } from 'lucide-react';

interface PipelineStep {
  label: string;
  desc: string;
  original: string;
  normalized: string;
  winner: string;
  source: 'CSV' | 'PDF';
  confidence: number;
}

const TransformationPage: React.FC = () => {
  const [running, setRunning] = useState(false);
  const [summary, setSummary] = useState<TransformationSummaryDto | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Interactive Demo Step State
  const [activeStep, setActiveStep] = useState(0);

  const handleRunTransformation = async () => {
    setRunning(true);
    setSummary(null);
    setError(null);
    try {
      const res = await triggerTransform();
      if (res.success) {
        setSummary(res.data);
      } else {
        setError(res.message);
      }
    } catch (err: any) {
      console.error(err);
      setError(err.response?.data?.message || 'Transformation failed. Ensure raw candidates exist to transform.');
    } finally {
      setRunning(false);
    }
  };

  // Static visualization pipeline steps to show: Original -> Normalized -> Merged Canonical Profile
  const pipelineSteps: PipelineStep[] = [
    {
      label: 'Full Name',
      desc: 'Cleans whitespace, standardizes title casing.',
      original: '  alexander   WRIGHT  ',
      normalized: 'Alexander Wright',
      winner: 'Alexander Wright',
      source: 'CSV',
      confidence: 1.0,
    },
    {
      label: 'Skills',
      desc: 'Dictionary alias standardizer standardizes skill tags.',
      original: 'JAVA, reactjs, Spring Boot, SQL, MachineLearning',
      normalized: 'Java, React, Spring Boot, SQL, Machine Learning',
      winner: 'Java, React, Spring Boot, SQL, Machine Learning',
      source: 'PDF',
      confidence: 0.90,
    },
    {
      label: 'Phone Number',
      desc: 'Strips formatting characters, formats to standard digits.',
      original: '+1 (123) 456-7890',
      normalized: '+11234567890',
      winner: '+11234567890',
      source: 'CSV',
      confidence: 1.0,
    },
    {
      label: 'Experience (Conflict)',
      desc: 'Conflict resolution picks PDF over CSV based on Source Priority.',
      original: 'CSV: "Backend Developer" vs PDF: "Lead Software Architect"',
      normalized: 'CSV: "Backend Developer" (Conf: 1.0) | PDF: "Lead Software Architect" (Conf: 0.9)',
      winner: 'Lead Software Architect',
      source: 'PDF',
      confidence: 0.90,
    },
  ];

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8 fade-in">
      <div className="mb-8 flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight text-white font-display">Transformation Engine</h1>
          <p className="mt-2 text-sm text-slate-400">Trigger deduplication matching and run the conflict resolution engines on pending imports.</p>
        </div>
        <button
          onClick={handleRunTransformation}
          disabled={running}
          className="flex items-center justify-center gap-2 rounded-xl bg-gradient-to-tr from-brand-600 to-indigo-500 px-6 py-3 font-bold text-white hover:from-brand-500 hover:to-indigo-400 disabled:opacity-50 shadow-lg shadow-brand-500/25 transition-all duration-200 shrink-0"
        >
          {running ? (
            <>
              <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent"></div>
              Processing...
            </>
          ) : (
            <>
              <Play className="h-4 w-4 fill-white" />
              Execute Pipeline
            </>
          )}
        </button>
      </div>

      {/* Real Ingest Transformation Results */}
      {summary && (
        <div className="mb-8 rounded-2xl border border-emerald-500/10 bg-emerald-500/5 p-6 flex gap-4 fade-in">
          <CheckCircle2 className="h-8 w-8 text-emerald-400 shrink-0" />
          <div className="w-full">
            <h3 className="text-sm font-bold text-white font-display">Pipeline Execution Success</h3>
            <p className="text-xs text-emerald-400/80 mt-1">
              Raw candidate profiles have been matched and unified.
            </p>
            <div className="mt-6 grid grid-cols-2 gap-4 sm:grid-cols-4 border-t border-emerald-500/10 pt-4 text-xs">
              <div>
                <span className="block text-slate-500 uppercase font-semibold">Raw Processed</span>
                <span className="text-white font-bold text-base">{summary.totalRawProcessed}</span>
              </div>
              <div>
                <span className="block text-slate-500 uppercase font-semibold">Canonical Created</span>
                <span className="text-white font-bold text-base">{summary.newProfilesCreated}</span>
              </div>
              <div>
                <span className="block text-slate-500 uppercase font-semibold">Duplicates Merged</span>
                <span className="text-white font-bold text-base">{summary.profilesMerged}</span>
              </div>
              <div>
                <span className="block text-slate-500 uppercase font-semibold">Duration</span>
                <span className="text-white font-bold text-base">{summary.durationMs} ms</span>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Error display */}
      {error && (
        <div className="mb-8 rounded-2xl border border-red-500/15 bg-red-500/5 p-6 flex gap-4 fade-in">
          <AlertTriangle className="h-8 w-8 text-red-500 shrink-0" />
          <div>
            <h3 className="text-sm font-bold text-white font-display">Transformation Failed</h3>
            <p className="text-xs text-red-400/90 mt-1">{error}</p>
          </div>
        </div>
      )}

      {/* Interactive Transformation Visualizer */}
      <div className="grid grid-cols-1 gap-8 lg:grid-cols-3">
        {/* Left Side: Pipeline Steps Selection */}
        <div className="glass-panel rounded-2xl p-6 lg:col-span-1">
          <div className="mb-6 flex items-center gap-2">
            <Cpu className="h-5 w-5 text-brand-400" />
            <h3 className="text-base font-bold text-white font-display">Transformation Steps</h3>
          </div>
          <div className="flex flex-col gap-3">
            {pipelineSteps.map((step, idx) => (
              <button
                key={step.label}
                onClick={() => setActiveStep(idx)}
                className={`flex items-center justify-between rounded-xl p-4 text-left border transition-all duration-200 ${
                  activeStep === idx
                    ? 'bg-brand-500/10 border-brand-500/30 text-white'
                    : 'bg-white/5 border-transparent text-slate-400 hover:bg-white/10 hover:text-white'
                }`}
              >
                <div>
                  <h4 className="text-sm font-bold">{step.label}</h4>
                  <p className="text-[10px] text-slate-500 mt-0.5">{step.desc}</p>
                </div>
                <ChevronRight className={`h-4 w-4 transition-transform duration-200 ${activeStep === idx ? 'translate-x-1' : ''}`} />
              </button>
            ))}
          </div>
        </div>

        {/* Right Side: Step Execution Details (Raw -> Normalized -> Winner) */}
        <div className="glass-panel rounded-2xl p-6 lg:col-span-2 flex flex-col justify-between">
          <div>
            <div className="mb-6 flex items-center justify-between border-b border-white/5 pb-4">
              <h3 className="text-base font-bold text-white font-display">Engine Output Details</h3>
              <span className="rounded-full bg-brand-500/10 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider text-brand-400">
                Step {activeStep + 1} of 4
              </span>
            </div>

            <div className="grid grid-cols-1 gap-6 md:grid-cols-3">
              {/* 1. Original */}
              <div className="rounded-xl border border-white/5 bg-dark-900/30 p-4">
                <span className="text-[10px] uppercase font-bold text-slate-500 tracking-wider">1. Messy Original Input</span>
                <div className="mt-3 font-mono text-xs text-red-300 break-words rounded bg-red-500/5 p-2 border border-red-500/10">
                  {pipelineSteps[activeStep].original}
                </div>
                <p className="mt-3 text-[10px] text-slate-500">Raw unformatted values from files.</p>
              </div>

              {/* 2. Normalized */}
              <div className="rounded-xl border border-white/5 bg-dark-900/30 p-4">
                <span className="text-[10px] uppercase font-bold text-slate-500 tracking-wider">2. Normalized State</span>
                <div className="mt-3 font-mono text-xs text-brand-300 break-words rounded bg-brand-500/5 p-2 border border-brand-500/10">
                  {pipelineSteps[activeStep].normalized}
                </div>
                <p className="mt-3 text-[10px] text-slate-500">Cleansed, standardised, and formatted state.</p>
              </div>

              {/* 3. Merged Canonical */}
              <div className="rounded-xl border border-white/5 bg-dark-900/30 p-4">
                <span className="text-[10px] uppercase font-bold text-slate-500 tracking-wider">3. Canonical Value</span>
                <div className="mt-3 font-mono text-xs text-emerald-300 break-words rounded bg-emerald-500/5 p-2 border border-emerald-500/10">
                  {pipelineSteps[activeStep].winner}
                </div>
                <p className="mt-3 text-[10px] text-slate-500">Final profile values resolved by the engine.</p>
              </div>
            </div>

            {/* Provenance and Confidence metadata for selected step */}
            <div className="mt-8 rounded-xl border border-white/5 bg-dark-900/20 p-5">
              <h4 className="text-xs font-bold text-white uppercase tracking-wider mb-4 flex items-center gap-1.5">
                <ShieldCheck className="h-4 w-4 text-emerald-400" />
                Provenance & Confidence Tracking
              </h4>
              <div className="grid grid-cols-2 gap-6 sm:grid-cols-4 text-xs">
                <div>
                  <span className="block text-slate-500 uppercase font-semibold text-[10px] tracking-wider">Winning Source</span>
                  <span className={`mt-1.5 inline-block rounded-md px-1.5 py-0.5 text-[10px] font-extrabold uppercase ${
                    pipelineSteps[activeStep].source === 'PDF' ? 'bg-indigo-500/10 text-indigo-400' : 'bg-brand-500/10 text-brand-400'
                  }`}>
                    {pipelineSteps[activeStep].source} Resume
                  </span>
                </div>
                <div>
                  <span className="block text-slate-500 uppercase font-semibold text-[10px] tracking-wider">Extraction Method</span>
                  <span className="block mt-1.5 text-white font-medium">Heuristic Engine</span>
                </div>
                <div>
                  <span className="block text-slate-500 uppercase font-semibold text-[10px] tracking-wider">Field Confidence</span>
                  <span className="block mt-1.5 text-white font-bold">{(pipelineSteps[activeStep].confidence * 100).toFixed(0)}%</span>
                </div>
                <div>
                  <span className="block text-slate-500 uppercase font-semibold text-[10px] tracking-wider">Confidence Bar</span>
                  <div className="mt-2.5 h-1.5 w-full rounded-full bg-white/5">
                    <div 
                      className="h-full rounded-full bg-gradient-to-r from-emerald-500 to-teal-400" 
                      style={{ width: `${pipelineSteps[activeStep].confidence * 100}%` }}
                    ></div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div className="mt-8 border-t border-white/5 pt-4 flex items-center gap-2 text-xs text-slate-400">
            <Info className="h-4 w-4 text-brand-400 shrink-0" />
            <p>During execution, names similarity checks (Jaro-Winkler) prevent duplicates, and values are chosen by priority weights.</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default TransformationPage;
