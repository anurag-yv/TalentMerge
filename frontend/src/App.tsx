import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import Dashboard from './pages/Dashboard';
import UploadPage from './pages/Upload';
import TransformationPage from './pages/Transformation';
import CandidateDetails from './pages/CandidateDetails';
import HistoryPage from './pages/History';

const App: React.FC = () => {
  return (
    <Router>
      <div className="min-h-screen bg-dark-950 text-slate-100 flex flex-col font-sans">
        {/* Navigation Bar */}
        <Navbar />

        {/* Content Container */}
        <main className="flex-1 w-full bg-radial from-slate-900 to-dark-950">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/upload" element={<UploadPage />} />
            <Route path="/transform" element={<TransformationPage />} />
            <Route path="/candidates" element={<CandidateDetails />} />
            <Route path="/history" element={<HistoryPage />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
};

export default App;
