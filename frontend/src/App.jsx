import { Suspense, lazy } from 'react'
import { BrowserRouter, Routes, Route, NavLink } from 'react-router-dom'
import './App.css'

const Dashboard = lazy(() => import('./pages/Dashboard'))
const Analytics = lazy(() => import('./pages/Analytics'))

function App() {
  return (
    <BrowserRouter>
      <div className="header">
        <h1>Asset Radar</h1>
        <nav className="nav-links">
          <NavLink to="/" end className={({ isActive }) => isActive ? 'nav-active' : ''}>
            Dashboard
          </NavLink>
          <NavLink to="/analytics" className={({ isActive }) => isActive ? 'nav-active' : ''}>
            Analytics
          </NavLink>
        </nav>
      </div>

      <Suspense fallback={<div className="page-loading">Loading page...</div>}>
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/analytics" element={<Analytics />} />
        </Routes>
      </Suspense>

      <div className="footer">
        <span>Asset Radar v0.2.0</span>
        <a href="/swagger-ui.html">API Docs</a>
      </div>
    </BrowserRouter>
  )
}

export default App
