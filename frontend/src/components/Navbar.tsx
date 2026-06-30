import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Database, UploadCloud, Cpu, Users, History } from 'lucide-react';

const Navbar: React.FC = () => {
  const location = useLocation();

  const navItems = [
    { path: '/', label: 'Dashboard', icon: Database },
    { path: '/upload', label: 'Upload Files', icon: UploadCloud },
    { path: '/transform', label: 'Transformation', icon: Cpu },
    { path: '/candidates', label: 'Candidates', icon: Users },
    { path: '/history', label: 'Audit Logs', icon: History },
  ];

  return (
    <nav className="sticky top-0 z-50 w-full border-b border-white/5 bg-dark-950/80 backdrop-blur-md">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex h-16 items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-dark-900 border border-brand-500/20 text-white shadow-lg shadow-brand-500/10">
              <svg viewBox="0 0 100 100" className="h-6 w-6 animate-pulse">
                <defs>
                  <linearGradient x1="0%" y1="0%" x2="100%" y2="100%" id="navBrandGradient">
                    <stop stopColor="#187aba" offset="0%"></stop>
                    <stop stopColor="#00b2a9" offset="100%"></stop>
                  </linearGradient>
                </defs>
                <circle cx="38" cy="50" r="26" stroke="url(#navBrandGradient)" strokeWidth="7" fill="none" opacity="0.7" />
                <circle cx="62" cy="50" r="26" stroke="url(#navBrandGradient)" strokeWidth="7" fill="none" opacity="0.7" />
                <circle cx="50" cy="50" r="8" fill="url(#navBrandGradient)" />
              </svg>
            </div>
            <div>
              <span className="font-display text-lg font-extrabold tracking-tight text-white">Talent<span className="text-brand-500 font-semibold">Merge</span></span>
              <span className="ml-1.5 rounded-md bg-brand-500/10 px-1.5 py-0.5 text-[10px] font-medium text-brand-400">Candidate Data Transformer</span>
            </div>
          </div>

          <div className="hidden md:flex items-center gap-1">
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = location.pathname === item.path;
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  className={`flex items-center gap-2 rounded-xl px-4 py-2 text-sm font-medium transition-all duration-200 ${
                    isActive
                      ? 'bg-brand-600 text-white shadow-md shadow-brand-500/10'
                      : 'text-slate-400 hover:bg-white/5 hover:text-white'
                  }`}
                >
                  <Icon className="h-4 w-4" />
                  {item.label}
                </Link>
              );
            })}
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
