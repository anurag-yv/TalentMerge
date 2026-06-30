import React, { useEffect, useState } from 'react';
import { 
  getCandidates, 
  getCandidateById, 
  deleteCandidate, 
  getUniqueLocations, 
  getUniqueSkills,
  projectCandidate
} from '../api';
import type { CanonicalProfileDto } from '../api';
import { 
  Search, Filter, SlidersHorizontal, ArrowUpDown, ChevronLeft, ChevronRight, 
  Briefcase, GraduationCap, Code2, Link as LinkIcon, Trash2, Mail, Phone, MapPin, 
  Calendar, ShieldCheck, Award, Info, ArrowLeft
} from 'lucide-react';

const CandidateDetails: React.FC = () => {
  // Query States
  const [candidates, setCandidates] = useState<any[]>([]);
  const [selectedCandidate, setSelectedCandidate] = useState<CanonicalProfileDto | null>(null);
  const [locations, setLocations] = useState<string[]>([]);
  const [skills, setSkills] = useState<string[]>([]);
  
  const [search, setSearch] = useState('');
  const [selectedSkill, setSelectedSkill] = useState('');
  const [selectedLocation, setSelectedLocation] = useState('');
  const [sortBy, setSortBy] = useState('fullName');
  const [direction, setDirection] = useState('ASC');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // UI States
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [activeProvenanceField, setActiveProvenanceField] = useState<string | null>(null);

  // Projection States
  const [configJson, setConfigJson] = useState<string>(JSON.stringify({
    fields: [
      { path: "name", from: "fullName", type: "string", required: true },
      { path: "primary_email", from: "emails[0]", type: "string", required: true },
      { path: "phone", from: "phones[0]", type: "string", normalize: "E164" },
      { path: "skills", from: "skills[*].name", type: "string[]", normalize: "canonical" }
    ],
    include_confidence: true,
    include_provenance: false,
    on_missing: "null"
  }, null, 2));
  const [projectedResult, setProjectedResult] = useState<string | null>(null);
  const [projecting, setProjecting] = useState<boolean>(false);
  const [projectionError, setProjectionError] = useState<string | null>(null);

  useEffect(() => {
    fetchOptions();
    fetchCandidatesList();
  }, [search, selectedSkill, selectedLocation, sortBy, direction, page]);

  const fetchCandidatesList = async () => {
    setLoading(true);
    try {
      const res = await getCandidates({
        search,
        skill: selectedSkill,
        location: selectedLocation,
        page,
        size: 10,
        sortBy,
        direction,
      });
      if (res.success) {
        setCandidates(res.data.content);
        setTotalPages(res.data.totalPages);
        setTotalElements(res.data.totalElements);
      }
    } catch (err) {
      console.error('Failed to load candidate profiles:', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchOptions = async () => {
    try {
      const [locsRes, skillsRes] = await Promise.all([
        getUniqueLocations(),
        getUniqueSkills(),
      ]);
      if (locsRes.success) setLocations(locsRes.data);
      if (skillsRes.success) setSkills(skillsRes.data);
    } catch (err) {
      console.error('Failed to load filter options', err);
    }
  };

  const handleApplyProjection = async () => {
    if (!selectedCandidate) return;
    setProjecting(true);
    setProjectionError(null);
    setProjectedResult(null);
    try {
      const parsedConfig = JSON.parse(configJson);
      const res = await projectCandidate(selectedCandidate.id, parsedConfig);
      if (res.success) {
        setProjectedResult(JSON.stringify(res.data, null, 2));
      } else {
        setProjectionError(res.message);
      }
    } catch (err: any) {
      console.error(err);
      setProjectionError(err.response?.data?.message || err.message || 'Failed to project. Verify JSON format.');
    } finally {
      setProjecting(false);
    }
  };

  const handleSelectCandidate = async (id: number) => {
    setDetailLoading(true);
    setSelectedCandidate(null);
    setActiveProvenanceField(null);
    try {
      const res = await getCandidateById(id);
      if (res.success) {
        setSelectedCandidate(res.data);
      }
    } catch (err) {
      console.error(err);
      alert('Failed to load candidate details.');
    } finally {
      setDetailLoading(false);
    }
  };

  const handleDeleteCandidate = async (id: number, e: React.MouseEvent) => {
    e.stopPropagation();
    if (!confirm('Are you sure you want to permanently delete this candidate profile?')) return;

    try {
      const res = await deleteCandidate(id);
      if (res.success) {
        alert('Candidate profile deleted successfully.');
        if (selectedCandidate?.id === id) {
          setSelectedCandidate(null);
        }
        fetchCandidatesList();
        fetchOptions();
      }
    } catch (err) {
      console.error(err);
      alert('Failed to delete candidate.');
    }
  };

  const toggleSort = (field: string) => {
    if (sortBy === field) {
      setDirection(direction === 'ASC' ? 'DESC' : 'ASC');
    } else {
      setSortBy(field);
      setDirection('ASC');
    }
    setPage(0);
  };

  // Helper: Find field level provenance information
  const getFieldProvenance = (fieldName: string) => {
    if (!selectedCandidate) return null;
    return selectedCandidate.fieldProvenances.find(p => p.fieldName === fieldName);
  };

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8 fade-in">
      {!selectedCandidate ? (
        /* LIST VIEW */
        <>
          <div className="mb-8">
            <h1 className="text-3xl font-extrabold tracking-tight text-white font-display">Candidate Directory</h1>
            <p className="mt-2 text-sm text-slate-400">Search, filter, and inspect verified canonical profiles compiled by the merging engine.</p>
          </div>

          {/* Search and Filters panel */}
          <div className="glass-panel rounded-2xl p-6 mb-6">
            <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
              {/* Search */}
              <div className="relative">
                <Search className="absolute left-3 top-3 h-4 w-4 text-slate-400" />
                <input
                  type="text"
                  placeholder="Search name, skills, location..."
                  value={search}
                  onChange={(e) => { setSearch(e.target.value); setPage(0); }}
                  className="w-full rounded-xl bg-dark-900/60 border border-white/5 pl-10 pr-4 py-2.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-brand-500"
                />
              </div>

              {/* Skills Filter */}
              <div className="relative">
                <Filter className="absolute left-3 top-3 h-4 w-4 text-slate-400" />
                <select
                  value={selectedSkill}
                  onChange={(e) => { setSelectedSkill(e.target.value); setPage(0); }}
                  className="w-full rounded-xl bg-dark-900/60 border border-white/5 pl-10 pr-4 py-2.5 text-xs text-white focus:outline-none focus:border-brand-500 appearance-none"
                >
                  <option value="">All Skills</option>
                  {skills.map(s => <option key={s} value={s}>{s}</option>)}
                </select>
              </div>

              {/* Location Filter */}
              <div className="relative">
                <MapPin className="absolute left-3 top-3 h-4 w-4 text-slate-400" />
                <select
                  value={selectedLocation}
                  onChange={(e) => { setSelectedLocation(e.target.value); setPage(0); }}
                  className="w-full rounded-xl bg-dark-900/60 border border-white/5 pl-10 pr-4 py-2.5 text-xs text-white focus:outline-none focus:border-brand-500 appearance-none"
                >
                  <option value="">All Locations</option>
                  {locations.map(l => <option key={l} value={l}>{l}</option>)}
                </select>
              </div>

              {/* Sort selector */}
              <div className="relative">
                <SlidersHorizontal className="absolute left-3 top-3 h-4 w-4 text-slate-400" />
                <select
                  value={`${sortBy}-${direction}`}
                  onChange={(e) => {
                    const [field, dir] = e.target.value.split('-');
                    setSortBy(field);
                    setDirection(dir);
                    setPage(0);
                  }}
                  className="w-full rounded-xl bg-dark-900/60 border border-white/5 pl-10 pr-4 py-2.5 text-xs text-white focus:outline-none focus:border-brand-500 appearance-none"
                >
                  <option value="fullName-ASC">Name (A-Z)</option>
                  <option value="fullName-DESC">Name (Z-A)</option>
                  <option value="overallConfidence-DESC">Confidence (High-Low)</option>
                  <option value="yearsOfExperience-DESC">Experience (High-Low)</option>
                  <option value="createdAt-DESC">Date Created (Newest)</option>
                </select>
              </div>
            </div>
          </div>

          {/* Candidates List Table */}
          {loading ? (
            <div className="flex h-64 items-center justify-center">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-500 border-t-transparent"></div>
            </div>
          ) : candidates.length > 0 ? (
            <div className="overflow-hidden border border-white/5 bg-dark-900/20 rounded-2xl">
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-white/5 text-left text-xs">
                  <thead className="bg-dark-950/40 text-slate-400 font-semibold uppercase tracking-wider text-[10px]">
                    <tr>
                      <th scope="col" className="px-6 py-4 cursor-pointer hover:text-white" onClick={() => toggleSort('fullName')}>
                        Candidate <ArrowUpDown className="inline h-3 w-3 ml-1" />
                      </th>
                      <th scope="col" className="px-6 py-4">Contact Details</th>
                      <th scope="col" className="px-6 py-4 cursor-pointer hover:text-white" onClick={() => toggleSort('yearsOfExperience')}>
                        YOE <ArrowUpDown className="inline h-3 w-3 ml-1" />
                      </th>
                      <th scope="col" className="px-6 py-4 cursor-pointer hover:text-white" onClick={() => toggleSort('overallConfidence')}>
                        Confidence <ArrowUpDown className="inline h-3 w-3 ml-1" />
                      </th>
                      <th scope="col" className="px-6 py-4 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/5 bg-dark-900/10">
                    {candidates.map((cand) => (
                      <tr 
                        key={cand.id} 
                        onClick={() => handleSelectCandidate(cand.id)}
                        className="cursor-pointer hover:bg-white/5 transition-colors duration-150"
                      >
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-3">
                            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-500/10 text-brand-400 font-bold uppercase">
                              {cand.fullName.substring(0, 2)}
                            </div>
                            <div>
                              <div className="font-bold text-white">{cand.fullName}</div>
                              <div className="text-[10px] text-slate-500 mt-0.5 truncate max-w-[200px]">{cand.headline || 'Software Engineer'}</div>
                            </div>
                          </div>
                        </td>
                        <td className="px-6 py-4 text-slate-400">
                          <div className="flex flex-col gap-1">
                            {cand.emails.length > 0 && (
                              <div className="flex items-center gap-1.5 truncate max-w-[200px]">
                                <Mail className="h-3 w-3 text-slate-500" />
                                <span>{cand.emails[0].email}</span>
                              </div>
                            )}
                            {cand.phones.length > 0 && (
                              <div className="flex items-center gap-1.5">
                                <Phone className="h-3 w-3 text-slate-500" />
                                <span>{cand.phones[0].phone}</span>
                              </div>
                            )}
                          </div>
                        </td>
                        <td className="px-6 py-4 font-bold text-white">{cand.yearsOfExperience} yrs</td>
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-2">
                            <span className="font-bold text-white">{Math.round(cand.overallConfidence * 100)}%</span>
                            <div className="h-1.5 w-16 rounded-full bg-white/5">
                              <div 
                                className="h-full rounded-full bg-gradient-to-r from-brand-600 to-indigo-400"
                                style={{ width: `${cand.overallConfidence * 100}%` }}
                              ></div>
                            </div>
                          </div>
                        </td>
                        <td className="px-6 py-4 text-right">
                          <button
                            onClick={(e) => handleDeleteCandidate(cand.id, e)}
                            className="rounded-lg p-2 text-slate-400 hover:bg-red-500/10 hover:text-red-400 transition-colors"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Pagination Controls */}
              <div className="flex items-center justify-between border-t border-white/5 bg-dark-950/20 px-6 py-4 text-xs text-slate-400">
                <div>
                  Showing Page <span className="font-bold text-white">{page + 1}</span> of <span className="font-bold text-white">{totalPages}</span> ({totalElements} total candidates)
                </div>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setPage(p => Math.max(0, p - 1))}
                    disabled={page === 0}
                    className="flex h-8 w-8 items-center justify-center rounded-lg border border-white/5 bg-white/5 hover:bg-white/10 disabled:opacity-40"
                  >
                    <ChevronLeft className="h-4 w-4" />
                  </button>
                  <button
                    onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                    disabled={page >= totalPages - 1}
                    className="flex h-8 w-8 items-center justify-center rounded-lg border border-white/5 bg-white/5 hover:bg-white/10 disabled:opacity-40"
                  >
                    <ChevronRight className="h-4 w-4" />
                  </button>
                </div>
              </div>
            </div>
          ) : (
            <div className="rounded-2xl border border-white/5 bg-dark-900/10 p-12 text-center text-slate-400">
              <SlidersHorizontal className="mx-auto h-12 w-12 text-slate-600 mb-4" />
              <h3 className="text-sm font-bold text-white">No candidates found</h3>
              <p className="mt-1 text-xs text-slate-500">Try adjusting your filters or upload candidate CSV/PDF files to get started.</p>
            </div>
          )}
        </>
      ) : (
        /* DETAILS VIEW */
        <div className="fade-in">
          {/* Back button */}
          <button 
            onClick={() => setSelectedCandidate(null)}
            className="flex items-center gap-2 rounded-xl border border-white/5 bg-white/5 px-4 py-2 text-xs font-bold text-slate-300 hover:bg-white/10 mb-6 transition-all"
          >
            <ArrowLeft className="h-4 w-4" /> Back to Directory
          </button>

          {detailLoading ? (
            <div className="flex h-64 items-center justify-center">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-500 border-t-transparent"></div>
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
              {/* LEFT SIDE: Core Candidate Details Card & Provenance Inspector */}
              <div className="lg:col-span-1 flex flex-col gap-6">
                {/* Core Details */}
                <div className="glass-panel rounded-2xl p-6 relative">
                  <div className="flex items-center gap-4">
                    <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-brand-500/10 text-brand-400 font-extrabold text-lg uppercase shadow-inner">
                      {selectedCandidate.fullName.substring(0, 2)}
                    </div>
                    <div>
                      {/* Name with Provenance trigger */}
                      <div className="flex items-center gap-1.5 group/item">
                        <h2 className="text-xl font-extrabold text-white font-display leading-tight">{selectedCandidate.fullName}</h2>
                        <button 
                          onClick={() => setActiveProvenanceField(activeProvenanceField === 'fullName' ? null : 'fullName')}
                          className="text-slate-500 hover:text-brand-400"
                        >
                          <Info className="h-3.5 w-3.5" />
                        </button>
                      </div>
                      <div className="flex items-center gap-1.5 text-xs text-slate-400 mt-1">
                        <span>{selectedCandidate.headline || 'Software Engineer'}</span>
                        <button 
                          onClick={() => setActiveProvenanceField(activeProvenanceField === 'headline' ? null : 'headline')}
                          className="text-slate-500 hover:text-brand-400"
                        >
                          <Info className="h-3 w-3" />
                        </button>
                      </div>
                    </div>
                  </div>

                  {/* Quick Metadata */}
                  <div className="mt-6 space-y-3.5 border-t border-white/5 pt-4 text-xs">
                    {/* Location */}
                    {selectedCandidate.location && (
                      <div className="flex items-center justify-between text-slate-400">
                        <div className="flex items-center gap-2">
                          <MapPin className="h-4 w-4 text-slate-500" />
                          <span>{selectedCandidate.location}</span>
                        </div>
                        <button 
                          onClick={() => setActiveProvenanceField(activeProvenanceField === 'location' ? null : 'location')}
                          className="text-slate-500 hover:text-brand-400"
                        >
                          <Info className="h-3 w-3" />
                        </button>
                      </div>
                    )}
                    {/* Experience Count */}
                    <div className="flex items-center justify-between text-slate-400">
                      <div className="flex items-center gap-2">
                        <Calendar className="h-4 w-4 text-slate-500" />
                        <span>Years of Experience: <strong className="text-white">{selectedCandidate.yearsOfExperience} yrs</strong></span>
                      </div>
                      <button 
                        onClick={() => setActiveProvenanceField(activeProvenanceField === 'yearsOfExperience' ? null : 'yearsOfExperience')}
                        className="text-slate-500 hover:text-brand-400"
                      >
                        <Info className="h-3 w-3" />
                      </button>
                    </div>

                    {/* Overall Confidence */}
                    <div className="flex items-center justify-between border-t border-white/5 pt-4">
                      <div className="flex items-center gap-2">
                        <Award className="h-4 w-4 text-amber-400" />
                        <span className="font-bold text-white">Overall Confidence:</span>
                      </div>
                      <span className="font-bold text-emerald-400">{Math.round(selectedCandidate.overallConfidence * 100)}%</span>
                    </div>
                  </div>
                </div>

                {/* Provenance Card Popup Overlay/Panel */}
                {activeProvenanceField && getFieldProvenance(activeProvenanceField) && (
                  <div className="glass-panel border-brand-500/20 bg-brand-900/5 rounded-2xl p-5 fade-in">
                    <div className="flex items-center justify-between border-b border-white/5 pb-2.5 mb-3 text-xs font-bold text-white">
                      <div className="flex items-center gap-1.5">
                        <ShieldCheck className="h-4 w-4 text-brand-400" />
                        <span>Provenance Inspector: <span className="text-brand-400">"{activeProvenanceField}"</span></span>
                      </div>
                      <button 
                        onClick={() => setActiveProvenanceField(null)} 
                        className="text-slate-500 hover:text-white"
                      >
                        Close
                      </button>
                    </div>

                    <div className="space-y-3.5 text-xs">
                      <div>
                        <span className="block text-slate-500 uppercase font-semibold text-[10px] tracking-wider">Source File Name</span>
                        <span className="block text-white font-medium break-all mt-0.5">{getFieldProvenance(activeProvenanceField)?.provenance.sourceName}</span>
                      </div>
                      <div className="grid grid-cols-2 gap-4">
                        <div>
                          <span className="block text-slate-500 uppercase font-semibold text-[10px] tracking-wider">Source Type</span>
                          <span className="block text-white font-semibold mt-0.5">{getFieldProvenance(activeProvenanceField)?.provenance.sourceType}</span>
                        </div>
                        <div>
                          <span className="block text-slate-500 uppercase font-semibold text-[10px] tracking-wider">Extraction Method</span>
                          <span className="block text-white mt-0.5">{getFieldProvenance(activeProvenanceField)?.provenance.extractionMethod}</span>
                        </div>
                      </div>
                      <div className="grid grid-cols-2 gap-4">
                        <div>
                          <span className="block text-slate-500 uppercase font-semibold text-[10px] tracking-wider">Timestamp</span>
                          <span className="block text-white mt-0.5">{new Date(getFieldProvenance(activeProvenanceField)?.provenance.extractedAt || '').toLocaleString()}</span>
                        </div>
                        <div>
                          <span className="block text-slate-500 uppercase font-semibold text-[10px] tracking-wider">Confidence</span>
                          <span className="block text-white font-bold mt-0.5">{Math.round((getFieldProvenance(activeProvenanceField)?.provenance.confidenceScore || 0) * 100)}%</span>
                        </div>
                      </div>
                      <div>
                        <span className="block text-slate-500 uppercase font-semibold text-[10px] tracking-wider">Raw Extracted Value</span>
                        <div className="mt-1 font-mono text-[10.5px] text-brand-300 break-words rounded bg-brand-500/5 p-2 border border-brand-500/10">
                          {getFieldProvenance(activeProvenanceField)?.rawValue || 'null'}
                        </div>
                      </div>
                    </div>
                  </div>
                )}

                {/* Canonical Contacts */}
                <div className="glass-panel rounded-2xl p-6">
                  <h3 className="text-sm font-bold text-white uppercase tracking-wider mb-4">Ingested Contacts</h3>
                  
                  <div className="space-y-3.5 text-xs text-slate-300">
                    {/* Emails */}
                    {selectedCandidate.emails.map((e) => (
                      <div key={e.id} className="flex items-center justify-between border-b border-white/5 pb-2.5">
                        <div className="flex items-center gap-2 overflow-hidden pr-2">
                          <Mail className="h-4 w-4 text-slate-500 shrink-0" />
                          <span className="truncate">{e.email}</span>
                        </div>
                        <span className="rounded bg-brand-500/10 px-1 py-0.5 text-[9px] font-semibold text-brand-400 shrink-0" title={e.provenance.sourceName}>
                          {e.provenance.sourceType} ({Math.round(e.provenance.confidenceScore * 100)}%)
                        </span>
                      </div>
                    ))}

                    {/* Phones */}
                    {selectedCandidate.phones.map((p) => (
                      <div key={p.id} className="flex items-center justify-between border-b border-white/5 pb-2.5">
                        <div className="flex items-center gap-2">
                          <Phone className="h-4 w-4 text-slate-500" />
                          <span>{p.phone}</span>
                        </div>
                        <span className="rounded bg-brand-500/10 px-1 py-0.5 text-[9px] font-semibold text-brand-400 shrink-0" title={p.provenance.sourceName}>
                          {p.provenance.sourceType} ({Math.round(p.provenance.confidenceScore * 100)}%)
                        </span>
                      </div>
                    ))}

                    {/* Links */}
                    {selectedCandidate.links.map((l) => (
                      <div key={l.id} className="flex items-center justify-between border-b border-white/5 pb-2.5">
                        <div className="flex items-center gap-2 overflow-hidden pr-2">
                          <LinkIcon className="h-4 w-4 text-slate-500 shrink-0" />
                          <a href={l.url.startsWith('http') ? l.url : `https://${l.url}`} target="_blank" rel="noreferrer" className="truncate text-brand-400 hover:underline">
                            {l.url}
                          </a>
                        </div>
                        <span className="rounded bg-brand-500/10 px-1 py-0.5 text-[9px] font-semibold text-brand-400 shrink-0" title={l.provenance.sourceName}>
                          {l.provenance.sourceType} ({Math.round(l.provenance.confidenceScore * 100)}%)
                        </span>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Candidate Skills */}
                <div className="glass-panel rounded-2xl p-6">
                  <h3 className="text-sm font-bold text-white uppercase tracking-wider mb-4">Standardized Skills</h3>
                  <div className="flex flex-wrap gap-2">
                    {selectedCandidate.skills.map((s) => (
                      <span 
                        key={s.id} 
                        className="flex items-center gap-1 rounded-xl bg-white/5 border border-white/5 px-3 py-1 text-xs text-white group relative cursor-help"
                        title={`Source: ${s.provenance.sourceName}\nConfidence: ${Math.round(s.provenance.confidenceScore * 100)}%`}
                      >
                        {s.skillName}
                        <span className="inline-block h-1.5 w-1.5 rounded-full bg-emerald-500"></span>
                      </span>
                    ))}
                  </div>
                </div>
              </div>

              {/* RIGHT SIDE: Interactive Timelines (Experience, Education, Projects) */}
              <div className="lg:col-span-2 flex flex-col gap-6">
                {/* Timeline: Experience */}
                <div className="glass-panel rounded-2xl p-6">
                  <h3 className="text-base font-bold text-white font-display mb-6 flex items-center gap-2">
                    <Briefcase className="h-5 w-5 text-brand-400" />
                    Employment History
                  </h3>
                  
                  {selectedCandidate.experiences.length > 0 ? (
                    <div className="relative border-l border-white/5 ml-3 pl-6 space-y-8">
                      {selectedCandidate.experiences.map((exp) => (
                        <div key={exp.id} className="relative group">
                          {/* Dot indicator */}
                          <div className="absolute -left-[31px] top-1.5 flex h-4 w-4 items-center justify-center rounded-full bg-dark-950 border-2 border-brand-500"></div>
                          
                          <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
                            <div>
                              <h4 className="text-sm font-bold text-white">{exp.role}</h4>
                              <p className="text-xs text-brand-400 font-medium mt-0.5">{exp.company}</p>
                            </div>
                            <span className="inline-flex items-center gap-1.5 text-[10px] text-slate-500 font-semibold bg-white/5 rounded-md px-2 py-0.5 mt-1 sm:mt-0 shrink-0">
                              <Calendar className="h-3 w-3" />
                              {exp.startDate || 'N/A'} — {exp.endDate || 'N/A'}
                            </span>
                          </div>
                          
                          {exp.description && (
                            <p className="mt-3 text-xs text-slate-400 leading-relaxed font-sans">{exp.description}</p>
                          )}

                          <div className="mt-3 flex items-center gap-2">
                            <span className="rounded bg-brand-500/10 px-1.5 py-0.5 text-[9px] font-semibold text-brand-400" title={`File: ${exp.provenance.sourceName}`}>
                              Source: {exp.provenance.sourceType} ({Math.round(exp.provenance.confidenceScore * 100)}%)
                            </span>
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="text-xs text-slate-500 italic">No experience records found on profile.</p>
                  )}
                </div>

                {/* Timeline: Education */}
                <div className="glass-panel rounded-2xl p-6">
                  <h3 className="text-base font-bold text-white font-display mb-6 flex items-center gap-2">
                    <GraduationCap className="h-5 w-5 text-emerald-400" />
                    Academic Timeline
                  </h3>

                  {selectedCandidate.educations.length > 0 ? (
                    <div className="relative border-l border-white/5 ml-3 pl-6 space-y-8">
                      {selectedCandidate.educations.map((edu) => (
                        <div key={edu.id} className="relative group">
                          {/* Dot indicator */}
                          <div className="absolute -left-[31px] top-1.5 flex h-4 w-4 items-center justify-center rounded-full bg-dark-950 border-2 border-emerald-500"></div>

                          <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
                            <div>
                              <h4 className="text-sm font-bold text-white">{edu.institution}</h4>
                              <p className="text-xs text-emerald-400 font-medium mt-0.5">{edu.degree} {edu.fieldOfStudy ? `in ${edu.fieldOfStudy}` : ''}</p>
                            </div>
                            <span className="inline-flex items-center gap-1.5 text-[10px] text-slate-500 font-semibold bg-white/5 rounded-md px-2 py-0.5 mt-1 sm:mt-0 shrink-0">
                              <Calendar className="h-3 w-3" />
                              {edu.startDate || 'N/A'} — {edu.endDate || 'N/A'}
                            </span>
                          </div>

                          <div className="mt-3 flex items-center gap-2">
                            <span className="rounded bg-emerald-500/10 px-1.5 py-0.5 text-[9px] font-semibold text-emerald-400" title={`File: ${edu.provenance.sourceName}`}>
                              Source: {edu.provenance.sourceType} ({Math.round(edu.provenance.confidenceScore * 100)}%)
                            </span>
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="text-xs text-slate-500 italic">No academic history records found.</p>
                  )}
                </div>

                {/* Section: Projects */}
                <div className="glass-panel rounded-2xl p-6">
                  <h3 className="text-base font-bold text-white font-display mb-6 flex items-center gap-2">
                    <Code2 className="h-5 w-5 text-amber-400" />
                    Key Projects
                  </h3>

                  {selectedCandidate.projects.length > 0 ? (
                    <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                      {selectedCandidate.projects.map((proj) => (
                        <div key={proj.id} className="rounded-xl border border-white/5 bg-dark-900/30 p-4 flex flex-col justify-between group">
                          <div>
                            <h4 className="text-sm font-bold text-white">{proj.title}</h4>
                            {proj.description && (
                              <p className="mt-2 text-xs text-slate-400 leading-relaxed">{proj.description}</p>
                            )}
                          </div>
                          
                          <div className="mt-4 border-t border-white/5 pt-3">
                            {proj.technologies && (
                              <div className="flex flex-wrap gap-1.5 mb-2.5">
                                {proj.technologies.split(',').map(t => (
                                  <span key={t} className="rounded-md bg-white/5 px-1.5 py-0.5 text-[9.5px] text-slate-300">
                                    {t.trim()}
                                  </span>
                                ))}
                              </div>
                            )}
                            <div className="flex items-center justify-between text-[9px] text-slate-500">
                              <span>Source: {proj.provenance.sourceType}</span>
                              <span className="font-semibold text-amber-400">{Math.round(proj.provenance.confidenceScore * 100)}% Conf</span>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="text-xs text-slate-500 italic">No project listings found.</p>
                  )}
                </div>

                {/* Dynamic Schema Projection Playground */}
                <div className="glass-panel rounded-2xl p-6">
                  <h3 className="text-base font-bold text-white font-display mb-4 flex items-center gap-2">
                    <SlidersHorizontal className="h-5 w-5 text-brand-400" />
                    Dynamic Schema Projection Layer
                  </h3>
                  <p className="text-xs text-slate-400 mb-6">
                    Configure and execute dynamic projections at runtime. Reshape the canonical candidate profile, map/rename properties, normalize formats, toggle metadata, and control missing values.
                  </p>

                  <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
                    {/* Left: JSON Input */}
                    <div>
                      <span className="block text-[10px] uppercase font-bold text-slate-500 tracking-wider mb-2">1. Define Projection Config (JSON)</span>
                      <textarea
                        value={configJson}
                        onChange={(e) => setConfigJson(e.target.value)}
                        className="w-full h-80 font-mono text-xs text-white bg-dark-950/60 border border-white/5 rounded-xl p-4 focus:outline-none focus:border-brand-500 focus:ring-1 focus:ring-brand-500"
                        spellCheck="false"
                      />
                      <button
                        onClick={handleApplyProjection}
                        disabled={projecting}
                        className="mt-4 flex items-center justify-center gap-2 rounded-xl bg-gradient-to-tr from-brand-600 to-indigo-500 px-6 py-2.5 font-bold text-white hover:from-brand-500 hover:to-indigo-400 disabled:opacity-50 shadow-lg shadow-brand-500/25 transition-all duration-200 w-full"
                      >
                        {projecting ? (
                          <>
                            <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent"></div>
                            Projecting Schema...
                          </>
                        ) : (
                          <>
                            <SlidersHorizontal className="h-4 w-4" />
                            Apply Dynamic Projection
                          </>
                        )}
                      </button>
                    </div>

                    {/* Right: Reshaped JSON Output */}
                    <div>
                      <span className="block text-[10px] uppercase font-bold text-slate-500 tracking-wider mb-2">2. Projected Canonical Profile Output</span>
                      <div className="h-80 w-full bg-dark-950/40 border border-white/5 rounded-xl p-4 overflow-auto font-mono text-[11px]">
                        {projectedResult ? (
                          <pre className="text-emerald-400">{projectedResult}</pre>
                        ) : projectionError ? (
                          <pre className="text-red-400 whitespace-pre-wrap">{projectionError}</pre>
                        ) : (
                          <div className="flex h-full flex-col items-center justify-center text-center text-slate-500 text-xs">
                            <Code2 className="h-10 w-10 text-slate-600 mb-2" />
                            <span>Click "Apply Dynamic Projection" to generate the reshaped candidate JSON.</span>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default CandidateDetails;
