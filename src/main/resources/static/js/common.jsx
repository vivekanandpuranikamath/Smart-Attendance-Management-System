/* Shared helpers for all three React apps (admin / manager / employee). */
const BASE = window.APP_BASE;

async function api(path, method, body) {
  const res = await fetch(BASE + path, {
    method: method || 'GET', credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  if (res.status === 401) { window.location.href = BASE + '/login'; throw new Error('Please sign in again'); }
  let data = null;
  try { data = await res.json(); } catch (e) { /* empty body */ }
  if (!res.ok) throw new Error((data && data.error) || 'Request failed');
  return data;
}

/* Loads data and re-polls so manager and employee screens stay in sync without a manual refresh. */
function useLive(path, ms) {
  const [data, setData] = React.useState(null);
  const [err, setErr] = React.useState('');
  const load = React.useCallback(() =>
    api(path).then(d => { setData(d); setErr(''); }).catch(e => setErr(e.message)), [path]);
  React.useEffect(() => { load(); const t = setInterval(load, ms || 15000); return () => clearInterval(t); }, [load, ms]);
  return [data, load, err];
}

function useToast() {
  const [t, setT] = React.useState(null);
  const notify = (text, bad) => { setT({ text, bad }); setTimeout(() => setT(null), 4000); };
  return [t ? <div className={'toast ' + (t.bad ? 'bad' : 'ok')} role="status">{t.text}</div> : null, notify];
}

const fmtD = s => s ? new Date(s + (String(s).length === 10 ? 'T00:00:00' : '')).toLocaleDateString([], { day: '2-digit', month: 'short', year: 'numeric' }) : '-';
const fmtT = s => s ? new Date(s).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '-';
const fmtDT = s => s ? new Date(s).toLocaleString([], { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' }) : '-';
const todayISO = () => { const d = new Date(); d.setMinutes(d.getMinutes() - d.getTimezoneOffset()); return d.toISOString().slice(0, 10); };

function Badge({ v }) { return <span className={'badge b-' + String(v).toLowerCase()}>{String(v).replace(/_/g, ' ').toLowerCase()}</span>; }
function Field({ label, children }) { return <label className="fld"><span>{label}</span>{children}</label>; }
function Empty({ text }) { return <p className="empty">{text}</p>; }

function Modal({ title, onClose, children }) {
  return (
    <div className="overlay" onClick={onClose}>
      <div className="modal" role="dialog" aria-label={title} onClick={e => e.stopPropagation()}>
        <div className="mh"><h3>{title}</h3><button className="x" onClick={onClose} aria-label="Close">&times;</button></div>
        {children}
      </div>
    </div>
  );
}

function Tabs({ items, value, onChange }) {
  return <div className="tabs" role="tablist">{items.map(([k, label]) =>
    <button key={k} role="tab" className={value === k ? 'on' : ''} onClick={() => onChange(k)}>{label}</button>)}</div>;
}

/* Header + change-password dialog shared by every role. */
function Shell({ title, children }) {
  const [me, reloadMe] = useLive('/api/auth/me', 60000);
  const [pw, setPw] = React.useState(false);
  const [f, setF] = React.useState({ currentPassword: '', newPassword: '' });
  const [toast, notify] = useToast();
  async function logout() { try { await api('/api/auth/logout', 'POST'); } catch (e) {} window.location.href = BASE + '/login'; }
  async function change(ev) {
    ev.preventDefault();
    try {
      await api('/api/auth/change-password', 'POST', f);
      notify('Password changed'); setPw(false); setF({ currentPassword: '', newPassword: '' }); reloadMe();
    } catch (e) { notify(e.message, true); }
  }
  return (
    <div>
      <header className="topbar">
        <div><span className="brand">Smart Attendance</span> <span className="who">/ {title}</span></div>
        <div className="acts">
          <span className="who">{me ? me.fullName + ' (' + me.empCode + ')' : ''}</span>
          <button className="btn sm" onClick={() => setPw(true)}>Change password</button>
          <button className="btn sm" onClick={logout}>Sign out</button>
        </div>
      </header>
      {me && me.mustChangePassword && <div className="banner">You are using a temporary password. <a href="#" onClick={e => { e.preventDefault(); setPw(true); }}>Change it now</a>.</div>}
      <main className="page">{children}</main>
      {pw && <Modal title="Change password" onClose={() => setPw(false)}>
        <form onSubmit={change} className="grid" style={{ gridTemplateColumns: '1fr' }}>
          <Field label="Current password"><input type="password" required value={f.currentPassword} onChange={e => setF({ ...f, currentPassword: e.target.value })} /></Field>
          <Field label="New password (min 8 characters)"><input type="password" required minLength={8} value={f.newPassword} onChange={e => setF({ ...f, newPassword: e.target.value })} /></Field>
          <div className="row end"><button className="btn primary">Save password</button></div>
        </form>
      </Modal>}
      {toast}
    </div>
  );
}
