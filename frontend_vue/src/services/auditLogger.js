/**
 * Tamper-proof, secure worker activity Audit Trail service.
 */

// Generate a unique session ID for this tab session if not already present
if (typeof sessionStorage !== 'undefined' && !sessionStorage.getItem('pgu_audit_session_id')) {
  sessionStorage.setItem('pgu_audit_session_id', 'sess-' + Math.random().toString(36).substring(2, 15) + '-' + Date.now());
}

const getSessionId = () => {
  if (typeof sessionStorage !== 'undefined') {
    return sessionStorage.getItem('pgu_audit_session_id') || 'sess-fallback';
  }
  return 'sess-non-browser';
};

export const auditLogger = {
  /**
   * Log a mutating user action securely in the session buffer.
   *
   * @param {string} acaoTipo - Type of action, e.g. 'ADICIONAR_AUTOCARRO'
   * @param {string} detalhes - Description of the action
   */
  logAction(acaoTipo, detalhes) {
    if (typeof sessionStorage === 'undefined') return;

    const adminUserStr = sessionStorage.getItem('pgu_admin_user');
    const adminUser = adminUserStr ? JSON.parse(adminUserStr) : null;
    
    const email = adminUser?.email || 'desconhecido@uminho.pt';
    const nome = adminUser?.nome || 'Operador Backoffice';

    const logEntry = {
      sessionId: getSessionId(),
      utilizadorEmail: email,
      utilizadorNome: nome,
      acaoTipo: acaoTipo,
      detalhes: detalhes,
      timestamp: new Date().toISOString()
    };

    console.log(`[Audit Logger] Attempting to send action immediately: ${acaoTipo}`);

    // Enviar imediatamente para o servidor
    fetch('/api/audit-logs/batch', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify([logEntry])
    }).then(res => {
      if (res.ok) {
        console.log(`[Audit Logger] Action sent immediately and saved to database: ${acaoTipo}`);
      } else {
        console.warn(`[Audit Logger] Server rejected immediate save, buffering locally...`);
        this.bufferLog(logEntry);
      }
    }).catch(err => {
      console.warn(`[Audit Logger] Network error while sending immediately, buffering locally...`, err);
      this.bufferLog(logEntry);
    });
  },

  /**
   * Buffers a log entry in sessionStorage to be sent in batch during session close/logout.
   * @param {Object} logEntry 
   */
  bufferLog(logEntry) {
    if (typeof localStorage === 'undefined') return;
    const pendingLogs = this.getPendingLogs();
    pendingLogs.push(logEntry);
    localStorage.setItem('pgu_pending_audit_logs', JSON.stringify(pendingLogs));
    console.log(`[Audit Logger] Buffered action locally in localStorage: ${logEntry.acaoTipo}`);
  },

  /**
   * Retrieves pending logs from the buffer.
   * @returns {Array}
   */
  getPendingLogs() {
    if (typeof localStorage === 'undefined') return [];
    const logsStr = localStorage.getItem('pgu_pending_audit_logs');
    return logsStr ? JSON.parse(logsStr) : [];
  },

  /**
   * Clears the pending logs from the buffer.
   */
  clearPendingLogs() {
    if (typeof localStorage === 'undefined') return;
    localStorage.removeItem('pgu_pending_audit_logs');
  },

  /**
   * Sends buffered logs to the server.
   * Uses fetch with keepalive: true or sendBeacon to ensure delivery during tab close/page unload.
   */
  async sendBatch() {
    const logs = this.getPendingLogs();
    if (logs.length === 0) return;

    this.clearPendingLogs();

    const url = '/api/audit-logs/batch';
    const payload = JSON.stringify(logs);

    try {
      if (typeof navigator !== 'undefined' && navigator.sendBeacon) {
        const blob = new Blob([payload], { type: 'application/json' });
        const success = navigator.sendBeacon(url, blob);
        if (success) {
          console.log('[Audit Logger] Dispatched batch via sendBeacon');
          return;
        }
      }
      
      // Fallback or if sendBeacon failed
      await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: payload,
        keepalive: true
      });
      console.log('[Audit Logger] Dispatched batch via fetch keepalive');
    } catch (err) {
      console.error('[Audit Logger] Failed to dispatch audit logs:', err);
      // Restore logs back to localStorage to prevent loss
      const currentLogs = this.getPendingLogs();
      localStorage.setItem('pgu_pending_audit_logs', JSON.stringify([...logs, ...currentLogs]));
    }
  }
};

// --- Setup Lifecycle Listeners for Automatic Unload Logging ---
if (typeof window !== 'undefined') {
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'hidden') {
      auditLogger.sendBatch();
    }
  });

  window.addEventListener('pagehide', () => {
    auditLogger.sendBatch();
  });
}
