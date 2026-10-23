function EmployeeApp() {
  const [tab, setTab] = React.useState('profile');
  return (
    <Shell title="Employee">
      <Tabs value={tab} onChange={setTab} items={[['profile', 'My details'], ['projects', 'My projects'], ['attendance', 'Attendance'], ['wfh', 'Work from home'], ['leaves', 'Leaves']]} />
      {tab === 'profile' && <ProfileTab />}
      {tab === 'projects' && <MyProjectsTab />}
      {tab === 'attendance' && <MyAttendanceTab />}
      {tab === 'wfh' && <MyWfhTab />}
      {tab === 'leaves' && <MyLeavesTab />}
    </Shell>
  );
}

function ProfileTab() {
  const [p] = useLive('/api/employee/profile', 10000);   // refreshes when the manager edits something
  if (!p) return <p className="loading">Loading...</p>;
  const rows = [['Employee ID', p.empCode], ['Full name', p.fullName], ['Designation', p.designation], ['Department', p.department],
    ['Company e-mail', p.companyEmail], ['Personal e-mail', p.personalEmail], ['Phone', p.phone], ['Emergency contact', p.emergencyContact],
    ['Gender', p.gender], ['Date of birth', fmtD(p.dateOfBirth)], ['Joining date', fmtD(p.joiningDate)], ['Address', p.address],
    ['Reporting manager', p.managerName], ['Shift', p.shiftTiming], ['Working days', 'Monday to Friday, 8-9 hours (Saturday & Sunday are company holidays)']];
  return <section className="card"><h2>Personal details</h2><dl className="kv">{rows.map(([k, v]) => <React.Fragment key={k}><dt>{k}</dt><dd>{v || '-'}</dd></React.Fragment>)}</dl></section>;
}

function MyProjectsTab() {
  const [list] = useLive('/api/employee/projects', 15000);
  const [status, setStatus] = React.useState('ACTIVE');
  const all = list || [];
  const shown = all.filter(p => p.status === status);
  return (
    <div>
      <Tabs value={status} onChange={setStatus} items={[['ACTIVE', 'Active (' + all.filter(p => p.status === 'ACTIVE').length + ')'],
        ['COMPLETED', 'Completed (' + all.filter(p => p.status === 'COMPLETED').length + ')'], ['FUTURE', 'Upcoming (' + all.filter(p => p.status === 'FUTURE').length + ')']]} />
      <div className="projects">{shown.map(p => <article key={p.id} className="card">
        <div className="row" style={{ justifyContent: 'space-between' }}><h3>{p.name}</h3><Badge v={p.status} /></div>
        <p className="small muted">Manager: {p.managerName || '-'} &middot; {fmtD(p.startDate)} to {fmtD(p.endDate)}</p>
        <p>{p.description}</p>
        <b className="small">Project team ({p.team.length})</b>
        <ul className="team">{p.team.map(t => <li key={t.id}>{t.fullName} <span className="muted">&middot; {t.designation} &middot; {t.companyEmail}</span></li>)}</ul>
      </article>)}</div>
      {list && shown.length === 0 && <Empty text="No projects in this list." />}
    </div>
  );
}

function MyAttendanceTab() {
  const [d] = useLive('/api/employee/attendance', 10000);
  if (!d) return <p className="loading">Loading...</p>;
  const t = d.today;
  return (
    <div>
      <section className="card">
        <h2>Today</h2>
        <div className="row"><Badge v={t.state} /><span>Check-in: <b>{fmtT(t.checkIn)}</b></span><span>Check-out: <b>{fmtT(t.checkOut)}</b></span></div>
        <p className="muted small sp">Scan the QR given by the admin on the <a href={BASE + '/scan'} target="_blank" rel="noreferrer">scan page</a> when you arrive and when you leave. Work 8-9 hours, Monday to Friday.</p>
      </section>
      <section className="card">
        <h2>Recent attendance</h2>
        <div className="tw"><table>
          <thead><tr><th>Date</th><th>Check-in</th><th>Check-out</th><th>Hours</th><th>Status</th></tr></thead>
          <tbody>{d.history.map(h => <tr key={h.workDate}><td>{fmtD(h.workDate)}</td><td>{fmtDT(h.checkIn)}</td><td>{fmtDT(h.checkOut)}</td>
            <td>{h.workedHours == null ? '-' : h.workedHours}</td><td><Badge v={h.status || '-'} /></td></tr>)}</tbody>
        </table></div>
        {d.history.length === 0 && <Empty text="No attendance recorded yet." />}
      </section>
    </div>
  );
}

function MyWfhTab() {
  const [list, reload] = useLive('/api/employee/wfh', 10000);
  const [f, setF] = React.useState({ date: '', reason: '' });
  const [toast, notify] = useToast();
  async function submit(ev) {
    ev.preventDefault();
    try { await api('/api/employee/wfh', 'POST', f); notify('WFH request sent to your manager'); setF({ date: '', reason: '' }); reload(); }
    catch (e) { notify(e.message, true); }
  }
  return (
    <div>
      <section className="card">
        <h2>Apply for work from home</h2>
        <form onSubmit={submit} className="grid">
          <Field label="Date *"><input type="date" required min={todayISO()} value={f.date} onChange={e => setF({ ...f, date: e.target.value })} /></Field>
          <Field label="Reason *"><textarea required value={f.reason} onChange={e => setF({ ...f, reason: e.target.value })} /></Field>
          <div className="row" style={{ alignItems: 'flex-end' }}><button className="btn primary">Send request</button></div>
        </form>
      </section>
      <section className="card">
        <h2>My WFH requests</h2>
        <div className="tw"><table>
          <thead><tr><th>Date</th><th>Reason</th><th>Status</th><th>Manager's remark</th></tr></thead>
          <tbody>{(list || []).map(w => <tr key={w.id}><td>{fmtD(w.date)}</td><td>{w.reason}</td><td><Badge v={w.status} /></td><td>{w.managerRemark || '-'}</td></tr>)}</tbody>
        </table></div>
        {list && list.length === 0 && <Empty text="No requests yet." />}
      </section>
      {toast}
    </div>
  );
}

function MyLeavesTab() {
  const [d, reload] = useLive('/api/employee/leaves', 10000);
  const [f, setF] = React.useState({ type: 'SICK', fromDate: '', toDate: '', reason: '' });
  const [toast, notify] = useToast();
  async function submit(ev) {
    ev.preventDefault();
    try { await api('/api/employee/leaves', 'POST', f); notify('Leave request sent to your manager'); setF({ type: 'SICK', fromDate: '', toDate: '', reason: '' }); reload(); }
    catch (e) { notify(e.message, true); }
  }
  const b = d && d.balance;
  return (
    <div>
      {b && <div className="balance">
        {['sick', 'personal'].map(k => <div key={k} className="stat"><b>{b[k].remaining} / {b[k].quota}</b><span>{k === 'sick' ? 'Sick' : 'Personal'} leave left in {b.month}</span></div>)}
      </div>}
      <section className="card">
        <h2>Apply for leave</h2>
        <p className="muted small">Each month you get 1 sick leave and 1 personal leave. Saturday and Sunday are company holidays and are not counted.</p>
        <form onSubmit={submit} className="grid">
          <Field label="Leave type"><select value={f.type} onChange={e => setF({ ...f, type: e.target.value })}><option value="SICK">Sick leave</option><option value="PERSONAL">Personal leave</option></select></Field>
          <Field label="From *"><input type="date" required value={f.fromDate} onChange={e => setF({ ...f, fromDate: e.target.value, toDate: f.toDate || e.target.value })} /></Field>
          <Field label="To *"><input type="date" required min={f.fromDate} value={f.toDate} onChange={e => setF({ ...f, toDate: e.target.value })} /></Field>
          <Field label="Reason *"><textarea required value={f.reason} onChange={e => setF({ ...f, reason: e.target.value })} /></Field>
          <div className="row" style={{ alignItems: 'flex-end' }}><button className="btn primary">Send request</button></div>
        </form>
      </section>
      <section className="card">
        <h2>My leave requests</h2>
        <div className="tw"><table>
          <thead><tr><th>Type</th><th>From</th><th>To</th><th>Reason</th><th>Status</th><th>Manager's remark</th></tr></thead>
          <tbody>{(d ? d.requests : []).map(l => <tr key={l.id}><td><Badge v={l.type} /></td><td>{fmtD(l.fromDate)}</td><td>{fmtD(l.toDate)}</td>
            <td>{l.reason}</td><td><Badge v={l.status} /></td><td>{l.managerRemark || '-'}</td></tr>)}</tbody>
        </table></div>
        {d && d.requests.length === 0 && <Empty text="No leave requests yet." />}
      </section>
      {toast}
    </div>
  );
}
ReactDOM.createRoot(document.getElementById('root')).render(<EmployeeApp />);
