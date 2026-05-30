const STATUSES = ['AVAILABLE', 'BUSY', 'OFFLINE']

export default function AgentCard({ agent, onStatusChange }) {
  const isAvailable = agent.status === 'AVAILABLE'
  const isOffline = agent.status === 'OFFLINE'

  const statusClass = isAvailable
    ? 'agent-available'
    : isOffline
    ? 'agent-offline'
    : 'agent-busy'

  const badgeClass = isAvailable
    ? 'badge-available'
    : isOffline
    ? 'badge-offline'
    : 'badge-busy'

  return (
    <div className={`agent-card ${statusClass}`}>
      <div className="agent-card-header">
        <div className="agent-avatar">{agent.name.charAt(0)}</div>
        <div className="agent-info">
          <h4 className="agent-name">{agent.name}</h4>
          <span className="agent-id">{agent.id}</span>
        </div>
        <span className={`badge ${badgeClass}`}>
          {agent.status}
        </span>
      </div>
      <div className="agent-card-footer">
        <div className="agent-stat">
          <span className="stat-label">Active Orders</span>
          <span className="stat-value">{agent.activeOrderCount}</span>
        </div>
        <div className="agent-stat">
          <span className="stat-label">Rating</span>
          <span className="stat-value">
            {agent.rating != null ? `⭐ ${agent.rating.toFixed(1)}` : '—'}
          </span>
        </div>
        <div className="agent-stat">
          <span className="stat-label">Change Status</span>
          <select
            className="status-select"
            value={agent.status}
            onChange={(e) => onStatusChange(agent.id, e.target.value)}
          >
            {STATUSES.map(s => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>
        </div>
      </div>
    </div>
  )
}