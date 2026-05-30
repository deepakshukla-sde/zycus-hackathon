export default function OrderCard({ order }) {
  const statusColors = {
    PENDING: 'badge-pending',
    ASSIGNED: 'badge-assigned',
    COMPLETED: 'badge-completed',
    CANCELLED: 'badge-cancelled',
  }

  const formatDate = (dateStr) => {
    if (!dateStr) return ''
    return new Date(dateStr).toLocaleDateString('en-US', {
      year: 'numeric', month: 'short', day: 'numeric',
      hour: '2-digit', minute: '2-digit'
    })
  }

  return (
    <div className="order-card">
      <div className="order-card-header">
        <div>
          <span className="order-id">{order.id}</span>
          <p className="order-description">{order.description}</p>
        </div>
        <span className={`badge ${statusColors[order.status] || 'badge-pending'}`}>
          {order.status}
        </span>
      </div>
      <div className="order-card-footer">
        <div className="order-agent">
          {order.assignedAgentId ? (
            <span>👤 <strong>{order.assignedAgentName || order.assignedAgentId}</strong></span>
          ) : (
            <span className="unassigned">Unassigned</span>
          )}
        </div>
        <span className="order-date">{formatDate(order.createdAt)}</span>
      </div>
    </div>
  )
}
