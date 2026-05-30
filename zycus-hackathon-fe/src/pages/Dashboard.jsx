import { useState, useEffect, useCallback } from 'react'
import { useAuth } from '../context/AuthContext'
import client from '../api/client'
import AgentCard from '../components/AgentCard'
import OrderCard from '../components/OrderCard'
import AssignmentResult from '../components/AssignmentResult'

export default function Dashboard() {
  const { user, logout } = useAuth()
  const [activeTab, setActiveTab] = useState('orders')
  const [agents, setAgents] = useState([])
  const [orders, setOrders] = useState([])
  const [assignmentResult, setAssignmentResult] = useState(null)
  const [assigning, setAssigning] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [newDescription, setNewDescription] = useState('')
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState('')

  const fetchAgents = useCallback(async () => {
    try {
      const { data } = await client.get('/orders/agents')
      setAgents(data)
    } catch {
      setError('Failed to load agents.')
    }
  }, [])

  const fetchOrders = useCallback(async () => {
    try {
      const { data } = await client.get('/orders')
      setOrders(data)
    } catch {
      setError('Failed to load orders.')
    }
  }, [])

  const fetchAll = useCallback(async () => {
    setLoading(true)
    await Promise.all([fetchAgents(), fetchOrders()])
    setLoading(false)
  }, [fetchAgents, fetchOrders])

  useEffect(() => {
    fetchAll()
  }, [fetchAll])

  const handleAssign = async () => {
    setAssigning(true)
    setError('')
    setAssignmentResult(null)
    try {
      const { data } = await client.post('/orders/assign')
      setAssignmentResult(data)
      await fetchAll()
    } catch (err) {
      const errData = err.response?.data
      if (errData) setAssignmentResult(errData)
      else setError('Assignment request failed. Please try again.')
    } finally {
      setAssigning(false)
    }
  }

  const handleCreateOrder = async (e) => {
    e.preventDefault()
    setCreateError('')
    setCreating(true)
    try {
      await client.post('/orders/create', { description: newDescription })
      setShowCreateModal(false)
      setNewDescription('')
      await fetchOrders()
    } catch (err) {
      setCreateError(err.response?.data?.message || 'Failed to create order.')
    } finally {
      setCreating(false)
    }
  }

  const handleStatusChange = async (agentId, newStatus) => {
    try {
      await client.put(`/orders/agents/${agentId}/status`, { status: newStatus })
      await fetchAgents()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update agent status.')
    }
  }

  const pendingCount = orders.filter(o => o.status === 'PENDING').length
  const assignedCount = orders.filter(o => o.status === 'ASSIGNED').length
  const availableAgents = agents.filter(a => a.status === 'AVAILABLE').length
  const busyAgents = agents.filter(a => a.status === 'BUSY').length

  return (
    <div className="dashboard">
      <nav className="navbar">
        <div className="navbar-brand">📦 Zycus Procurement</div>
        <div className="navbar-user">
          <span>Welcome, <strong>{user?.name}</strong></span>
          <button className="btn btn-secondary btn-sm" onClick={logout}>Logout</button>
        </div>
      </nav>

      <div className="dashboard-content">
        {error && <div className="alert alert-error">{error}</div>}

        {/* Stats Row */}
        <div className="stats-row">
          <div className="stat-card">
            <span className="stat-card-value">{orders.length}</span>
            <span className="stat-card-label">Total Orders</span>
          </div>
          <div className="stat-card stat-card-warning">
            <span className="stat-card-value">{pendingCount}</span>
            <span className="stat-card-label">Pending</span>
          </div>
          <div className="stat-card stat-card-success">
            <span className="stat-card-value">{assignedCount}</span>
            <span className="stat-card-label">Assigned</span>
          </div>
          <div className="stat-card">
            <span className="stat-card-value">{agents.length}</span>
            <span className="stat-card-label">Total Agents</span>
          </div>
          <div className="stat-card stat-card-success">
            <span className="stat-card-value">{availableAgents}</span>
            <span className="stat-card-label">Available</span>
          </div>
          <div className="stat-card stat-card-busy">
            <span className="stat-card-value">{busyAgents}</span>
            <span className="stat-card-label">Busy</span>
          </div>
        </div>

        {/* Action Bar */}
        <div className="assign-bar">
          <div>
            <h2 className="assign-title">AI Order Assignment</h2>
            <p className="assign-subtitle">
              {pendingCount > 0
                ? `${pendingCount} pending order(s) ready to be assigned by AI`
                : 'No pending orders at this time'}
            </p>
          </div>
          <div className="action-buttons">
            <button
              className="btn btn-secondary btn-assign"
              onClick={() => { setShowCreateModal(true); setCreateError('') }}
            >
              + New Order
            </button>
            <button
              className="btn btn-primary btn-assign"
              onClick={handleAssign}
              disabled={assigning}
            >
              {assigning
                ? <><span className="spinner" /> Assigning…</>
                : '🤖 Assign Orders via AI'}
            </button>
          </div>
        </div>

        {/* Assignment Result */}
        {assignmentResult && (
          <AssignmentResult
            result={assignmentResult}
            onClose={() => setAssignmentResult(null)}
          />
        )}

        {/* Tabs */}
        <div className="tabs">
          <button
            className={`tab ${activeTab === 'orders' ? 'tab-active' : ''}`}
            onClick={() => setActiveTab('orders')}
          >
            Orders ({orders.length})
          </button>
          <button
            className={`tab ${activeTab === 'agents' ? 'tab-active' : ''}`}
            onClick={() => setActiveTab('agents')}
          >
            Agents ({agents.length})
          </button>
        </div>

        {/* Content */}
        {loading ? (
          <div className="loading-state">Loading…</div>
        ) : (
          <div className="card-grid">
            {activeTab === 'orders' && (
              orders.length === 0
                ? <p className="empty-state">No orders found.</p>
                : orders.map(order => <OrderCard key={order.id} order={order} />)
            )}
            {activeTab === 'agents' && (
              agents.length === 0
                ? <p className="empty-state">No agents found.</p>
                : agents.map(agent => (
                    <AgentCard
                      key={agent.id}
                      agent={agent}
                      onStatusChange={handleStatusChange}
                    />
                  ))
            )}
          </div>
        )}
      </div>

      {/* Create Order Modal */}
      {showCreateModal && (
        <div className="modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Create New Order</h3>
              <button className="btn-close" onClick={() => setShowCreateModal(false)}>✕</button>
            </div>
            {createError && <div className="alert alert-error">{createError}</div>}
            <form onSubmit={handleCreateOrder}>
              <div className="form-group">
                <label>Description</label>
                <textarea
                  value={newDescription}
                  onChange={(e) => setNewDescription(e.target.value)}
                  placeholder="e.g. Purchase office chairs for new employees"
                  required
                  rows={4}
                />
              </div>
              <div className="modal-actions">
                <button type="submit" className="btn btn-primary" disabled={creating}>
                  {creating ? 'Creating…' : 'Create Order'}
                </button>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setShowCreateModal(false)}
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}