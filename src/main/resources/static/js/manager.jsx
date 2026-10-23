function ManagerApp() {
  const [tab, setTab] = React.useState('attendance');
  const [dash] = useLive('/api/manager/dashboard', 10000);
  const pend = dash ? { l: dash.pendingLeaves, w: dash.pendingWfh } : { l: 0, w: 0 };
  return (
    <Shell title="Manager">
      <Tabs value={tab} onChange={setTab} items={[
        ['attendance', 'Attendance'], ['leaves', 'Leaves' + (pend.l ? ' (' + pend.l + ')' : '')],
        ['wfh', 'Work from home' + (pend.w ? ' (' + pend.w + ')' : '')], ['projects', 'Projects'], ['team', 'Team']]} />
      {tab === 'attendance' && <AttendanceTab />}
      {tab === 'leaves' && <RequestsTab kind="leaves" />}
      {tab === 'wfh' && <RequestsTab kind="wfh" />}
      {tab === 'projects' && <ProjectsTab />}
      {tab === 'team' && <TeamTab />}
    </Shell>
  );
}

function AttendanceTab() {
  const [d] = useLive('/api/manager/dashboard', 10000);
  const [date, setDate] = React.useState(todayISO());
  const [hist] = useLive('/api/manager/attendance?date=' + date, 30000);
  if (!d) return <p className="loading">Loading...</p>;
  const c = d.counts;
  return (
    <div>
      <div className="stats">
        <div className="stat"><b>{d.teamSize}</b><span>Team size</span></div>
        <div className="stat"><b>{c.ACTIVE}</b><span>Active now (checked in)</span></div>
        <div className="stat"><b>{c.CHECKED_OUT}</b><span>Checked out</span></div>
        <div className="stat"><b>{c.NOT_CHECKED_IN}</b><span>Not checked in</span></div>
        <div className="stat"><b>{c.ON_LEAVE}</b><span>On leave</span></div>
        <div className="stat"><b>{c.WFH}</b><span>Work from home</span></div>
      </div>
      <section className="card">
        <h2>Today (live)</h2>
        <div className="tw"><table>
          <thead><tr><th>Employee</th><th>Shift</th><th>Status</th><th>Check-in</th><th>Check-out</th><th>Hours</th></tr></thead>
          <tbody>{d.rows.map(r => <tr key={r.id}>
            <td><b>{r.fullName}</b><div className="small muted">{r.empCode} &middot; {r.designation}</div></td>
            <td><Badge v={r.shift} /></td><td><Badge v={r.state} /></td>
            <td>{fmtT(r.checkIn)}</td><td>{fmtT(r.checkOut)}</td><td>{r.workedHours == null ? '-' : r.workedHours}</td></tr>)}</tbody>
        </table></div>
        {d.rows.length === 0 && <Empty text="No employees are assigned to you yet. Ask the admin to set you as their manager." />}
      </section>
      <section className="card">
        <div className="row" style={{ justifyContent: 'space-between' }}>
          <h2>Attendance by date</h2>
          <input type="date" style={{ maxWidth: 190 }} value={date} onChange={e => setDate(e.target.value)} aria-label="Date" />
        </div>
        <div className="tw"><table>
          <thead><tr><th>Employee</th><th>Status</th><th>Check-in</th><th>Check-out</th><th>Hours</th></tr></thead>
          <tbody>{(hist || []).map(r => <tr key={r.empCode}>
            <td>{r.fullName}<div className="small muted">{r.empCode}</div></td><td><Badge v={r.status} /></td>
            <td>{fmtDT(r.checkIn)}</td><td>{fmtDT(r.checkOut)}</td><td>{r.workedHours == null ? '-' : r.workedHours}</td></tr>)}</tbody>
        </table></div>
      </section>
    </div>
  );
}

/* Leave applications and WFH applications share one list + one decision dialog. */
function RequestsTab({ kind }) {
  const [items, reload] = useLive('/api/manager/' + kind, 10000);
  const [filter, setFilter] = React.useState('PENDING');
  const [sel, setSel] = React.useState(null);
  const isLeave = kind === 'leaves';
  const list = (items || []).filter(i => filter === 'ALL' || i.status === filter);
  return (
    <section className="card">
      <div className="row" style={{ justifyContent: 'space-between' }}>
        <h2>{isLeave ? 'Leave applications' : 'Work from home applications'}</h2>
        <select style={{ maxWidth: 170 }} value={filter} onChange={e => setFilter(e.target.value)} aria-label="Filter">
          <option value="PENDING">Pending</option><option value="APPROVED">Approved</option><option value="REJECTED">Not approved</option><option value="ALL">All</option>
        </select>
      </div>
      <p className="muted small">Click a row to see the full reason and decide.</p>
      <div className="tw"><table>
        <thead><tr><th>Employee</th>{isLeave ? <><th>Type</th><th>From</th><th>To</th><th>Days</th></> : <th>Date</th>}<th>Applied</th><th>Status</th></tr></thead>
        <tbody>{list.map(i => <tr key={i.id} className="click" onClick={() => setSel(i)}>
          <td><b>{i.employeeName}</b><div className="small muted">{i.empCode}</div></td>
          {isLeave ? <><td><Badge v={i.type} /></td><td>{fmtD(i.fromDate)}</td><td>{fmtD(i.toDate)}</td><td>{i.days}</td></> : <td>{fmtD(i.date)}</td>}
          <td>{fmtDT(i.createdAt)}</td><td><Badge v={i.status} /></td></tr>)}</tbody>
      </table></div>
      {items && list.length === 0 && <Empty text="Nothing here." />}
      {sel && <DecisionModal kind={kind} item={sel} onClose={() => setSel(null)} onDone={() => { setSel(null); reload(); }} />}
    </section>
  );
}

function DecisionModal({ kind, item, onClose, onDone }) {
  const [remark, setRemark] = React.useState('');
  const [err, setErr] = React.useState('');
  const isLeave = kind === 'leaves';
  async function decide(approve) {
    try { await api('/api/manager/' + kind + '/' + item.id, 'PUT', { approve, remark }); onDone(); }
    catch (e) { setErr(e.message); }
  }
  return (
    <Modal title={isLeave ? 'Leave application' : 'Work from home application'} onClose={onClose}>
      <dl className="kv">
        <dt>Employee</dt><dd>{item.employeeName} ({item.empCode})</dd>
        {isLeave ? <><dt>Leave type</dt><dd><Badge v={item.type} /></dd><dt>From</dt><dd>{fmtD(item.fromDate)}</dd><dt>To</dt><dd>{fmtD(item.toDate)} ({item.days} working day{item.days === 1 ? '' : 's'})</dd>
          </> : <><dt>WFH date</dt><dd>{fmtD(item.date)}</dd></>}
        <dt>Reason</dt><dd>{item.reason}</dd>
        <dt>Applied on</dt><dd>{fmtDT(item.createdAt)}</dd>
        <dt>Status</dt><dd><Badge v={item.status} /></dd>
        {item.managerRemark && <><dt>Your remark</dt><dd>{item.managerRemark}</dd></>}
      </dl>
      {item.status === 'PENDING' && <div className="sp">
        <Field label="Remark / reason (required when not approving)"><textarea value={remark} onChange={e => setRemark(e.target.value)} /></Field>
        <div className="err">{err}</div>
        <div className="row end"><button className="btn danger" onClick={() => decide(false)}>Not approve</button><button className="btn ok" onClick={() => decide(true)}>Approve</button></div>
      </div>}
    </Modal>
  );
}

const EMPTY_PROJ = { name: '', description: '', status: 'ACTIVE', startDate: '', endDate: '', memberIds: [] };

function ProjectsTab() {
  const [projects, reload] = useLive('/api/manager/projects', 15000);
  const [team] = useLive('/api/manager/team', 30000);
  const [status, setStatus] = React.useState('ACTIVE');
  const [form, setForm] = React.useState(null);
  const [editId, setEditId] = React.useState(null);
  const [toast, notify] = useToast();
  const all = projects || [];
  const list = all.filter(p => p.status === status);
  const set = k => e => setForm({ ...form, [k]: e.target.value });
  const toggleMember = id => setForm({ ...form, memberIds: form.memberIds.includes(id) ? form.memberIds.filter(x => x !== id) : [...form.memberIds, id] });
  function open(p) {
    setEditId(p ? p.id : null);
    setForm(p ? { name: p.name, description: p.description || '', status: p.status, startDate: p.startDate || '', endDate: p.endDate || '', memberIds: p.team.map(t => t.id) } : { ...EMPTY_PROJ, status });
  }
  async function save(ev) {
    ev.preventDefault();
    const body = { ...form, startDate: form.startDate || null, endDate: form.endDate || null };
    try { await api('/api/manager/projects' + (editId ? '/' + editId : ''), editId ? 'PUT' : 'POST', body); notify('Project saved'); setForm(null); reload(); }
    catch (e) { notify(e.message, true); }
  }
  return (
    <div>
      <div className="row" style={{ justifyContent: 'space-between', marginBottom: '.8rem' }}>
        <Tabs value={status} onChange={setStatus} items={[['ACTIVE', 'Active (' + all.filter(p => p.status === 'ACTIVE').length + ')'],
          ['COMPLETED', 'Completed (' + all.filter(p => p.status === 'COMPLETED').length + ')'], ['FUTURE', 'Future (' + all.filter(p => p.status === 'FUTURE').length + ')']]} />
        <button className="btn primary" onClick={() => open(null)}>New project</button>
      </div>
      <div className="projects">{list.map(p => <article key={p.id} className="card">
        <div className="row" style={{ justifyContent: 'space-between' }}><h3>{p.name}</h3><Badge v={p.status} /></div>
        <p className="small muted">{fmtD(p.startDate)} to {fmtD(p.endDate)}</p>
        <p>{p.description}</p>
        <b className="small">Team ({p.team.length})</b>
        <ul className="team">{p.team.map(t => <li key={t.id}>{t.fullName} <span className="muted">&middot; {t.empCode} &middot; {t.designation}</span></li>)}</ul>
        <div className="row end sp"><button className="btn sm" onClick={() => open(p)}>Edit</button></div>
      </article>)}</div>
      {projects && list.length === 0 && <Empty text={'No ' + status.toLowerCase() + ' projects.'} />}
      {form && <Modal title={editId ? 'Edit project' : 'New project'} onClose={() => setForm(null)}>
        <form onSubmit={save} className="grid" style={{ gridTemplateColumns: '1fr' }}>
          <Field label="Project name *"><input required value={form.name} onChange={set('name')} /></Field>
          <Field label="Description"><textarea value={form.description} onChange={set('description')} /></Field>
          <Field label="Status"><select value={form.status} onChange={set('status')}><option value="ACTIVE">Active</option><option value="COMPLETED">Completed</option><option value="FUTURE">Future</option></select></Field>
          <div className="grid"><Field label="Start date"><input type="date" value={form.startDate} onChange={set('startDate')} /></Field>
            <Field label="End date"><input type="date" value={form.endDate} onChange={set('endDate')} /></Field></div>
          <div><b className="small">Team members</b><div className="checks">{(team || []).map(t => <label key={t.id}>
            <input type="checkbox" checked={form.memberIds.includes(t.id)} onChange={() => toggleMember(t.id)} />{t.fullName} ({t.empCode})</label>)}</div></div>
          <div className="row end"><button className="btn primary">Save project</button></div>
        </form>
      </Modal>}
      {toast}
    </div>
  );
}

function TeamTab() {
  const [team, reload] = useLive('/api/manager/team', 15000);
  const [sel, setSel] = React.useState(null);
  const [toast, notify] = useToast();
  async function save(ev) {
    ev.preventDefault();
    try { await api('/api/manager/team/' + sel.id, 'PUT', { phone: sel.phone, address: sel.address, department: sel.department, designation: sel.designation, shift: sel.shift });
      notify('Updated - the employee sees this immediately'); setSel(null); reload(); } catch (e) { notify(e.message, true); }
  }
  const set = k => e => setSel({ ...sel, [k]: e.target.value });
  return (
    <section className="card">
      <h2>My team</h2>
      <div className="tw"><table>
        <thead><tr><th>Employee</th><th>Designation</th><th>Department</th><th>Shift</th><th>Phone</th><th>Company e-mail</th><th></th></tr></thead>
        <tbody>{(team || []).map(e => <tr key={e.id}>
          <td><b>{e.fullName}</b><div className="small muted">{e.empCode}</div></td><td>{e.designation}</td><td>{e.department}</td>
          <td><Badge v={e.shift} /></td><td>{e.phone}</td><td>{e.companyEmail}</td>
          <td><button className="btn sm" onClick={() => setSel({ ...e })}>Update</button></td></tr>)}</tbody>
      </table></div>
      {team && team.length === 0 && <Empty text="No employees report to you yet." />}
      {sel && <Modal title={'Update ' + sel.fullName} onClose={() => setSel(null)}>
        <form onSubmit={save} className="grid" style={{ gridTemplateColumns: '1fr' }}>
          <Field label="Designation"><input value={sel.designation || ''} onChange={set('designation')} /></Field>
          <Field label="Department"><input value={sel.department || ''} onChange={set('department')} /></Field>
          <Field label="Phone"><input value={sel.phone || ''} onChange={set('phone')} /></Field>
          <Field label="Address"><textarea value={sel.address || ''} onChange={set('address')} /></Field>
          <Field label="Shift"><select value={sel.shift} onChange={set('shift')}><option value="DAY">Day shift</option><option value="NIGHT">Night shift</option></select></Field>
          <div className="row end"><button className="btn primary">Save changes</button></div>
        </form>
      </Modal>}
      {toast}
    </section>
  );
}
ReactDOM.createRoot(document.getElementById('root')).render(<ManagerApp />);
