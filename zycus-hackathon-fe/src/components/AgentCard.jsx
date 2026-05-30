export default function AgentCard({ agent }) {
  const isAvailable = agent.status === 'AVAILABLE'

  return (
    <div className={`agent-card ${isAvailable ? 'agent-available' : 'agent-busy'}`}>
      <div className="agent-card-header">
        <div className="agent-avatar">{agent.name.charAt(0)}</div>
        <div className="agent-info">
          <h4 className="agent-name">{agent.name}</h4>
          <span className="agent-id">{agent.id}</span>
        </div>
        <span className={`badge ${isAvailable ? 'badge-available' : 'badge-busy'}`}>
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
          <span className="stat-value">{agent.rating != null ? `⭐ ${agent.rating.toFixed(1)}` : '—'}</span>
        </div>
      </div>
    </div>
  )
}
