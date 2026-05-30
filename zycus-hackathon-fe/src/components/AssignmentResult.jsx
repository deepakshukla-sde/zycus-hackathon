export default function AssignmentResult({ result, onClose }) {
  if (!result) return null

  return (
    <div className="assignment-result">
      <div className={`result-banner ${result.success ? 'result-success' : 'result-error'}`}>
        <div className="result-banner-content">
          <span className="result-icon">{result.success ? '✅' : '❌'}</span>
          <div>
            <p className="result-message">{result.message}</p>
            {result.totalAssigned > 0 && (
              <p className="result-stats">
                {result.totalAssigned} assigned &nbsp;·&nbsp; {result.totalFailed} failed
              </p>
            )}
          </div>
          <button className="btn-close" onClick={onClose}>✕</button>
        </div>
      </div>

      {result.assignments && result.assignments.length > 0 && (
        <div className="assignment-details">
          <h4>Assignment Details</h4>
          <table className="assignment-table">
            <thead>
              <tr>
                <th>Order ID</th>
                <th>Assigned Agent</th>
                <th>Status</th>
                <th>Reason</th>
              </tr>
            </thead>
            <tbody>
              {result.assignments.map((a) => (
                <tr key={a.orderId} className={a.success ? 'row-success' : 'row-failed'}>
                  <td>{a.orderId}</td>
                  <td>{a.agentName || a.assignedAgentId || '—'}</td>
                  <td>
                    <span className={`badge ${a.success ? 'badge-assigned' : 'badge-cancelled'}`}>
                      {a.success ? 'Assigned' : 'Failed'}
                    </span>
                  </td>
                  <td className="reason-cell">{a.reason || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
