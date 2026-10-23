const EMPTY_EMP = { empCode: '', fullName: '', companyEmail: '', personalEmail: '', phone: '', address: '', gender: 'Male',
  emergencyContact: '', department: '', designation: '', dateOfBirth: '', joiningDate: '', role: 'EMPLOYEE', shift: 'DAY', managerId: '', tempPassword: '' };

function AdminApp() {
  const [emps, reload] = useLive('/api/admin/employees', 20000);
  const [form, setForm] = React.useState(EMPTY_EMP);
  const [editId, setEditId] = React.useState(null);
  const [created, setCreated] = React.useState(null);
  const [qr, setQr] = React.useState(null);
  const [q, setQ] = React.useState('');
  const [toast, notify] = useToast();
  const list = emps || [];
  const managers = list.filter(e => e.role === 'MANAGER' && e.active);
  const set = k => e => setForm({ ...form, [k]: e.target.value });

  async function submit(ev) {
    ev.preventDefault();
    const body = { ...form, managerId: form.managerId ? Number(form.managerId) : null,
      dateOfBirth: form.dateOfBirth || null, joiningDate: form.joiningDate || null };
    try {
      if (editId) { await api('/api/admin/employees/' + editId, 'PUT', body); notify('Employee updated'); }
      else { const r = await api('/api/admin/employees', 'POST', body); setCreated({ code: r.employee.empCode, name: r.employee.fullName, pw: r.tempPassword }); notify('Employee added'); }
      setForm(EMPTY_EMP); setEditId(null); reload();
    } catch (e) { notify(e.message, true); }
  }
  function edit(e) {
    const f = {}; Object.keys(EMPTY_EMP).forEach(k => { f[k] = e[k] == null ? '' : e[k]; }); f.tempPassword = '';
    setForm(f); setEditId(e.id); setCreated(null); window.scrollTo({ top: 0, behavior: 'smooth' });
  }
  async function toggle(e) { try { await api('/api/admin/employees/' + e.id + '/toggle', 'POST'); reload(); } catch (x) { notify(x.message, true); } }
  async function reset(e) {
    if (!window.confirm('Reset password for ' + e.fullName + '?')) return;
    try { const r = await api('/api/admin/employees/' + e.id + '/reset-password', 'POST'); setCreated({ code: e.empCode, name: e.fullName, pw: r.tempPassword }); }
    catch (x) { notify(x.message, true); }
  }
  const shown = list.filter(e => (e.empCode + e.fullName + e.companyEmail + (e.department || '')).toLowerCase().includes(q.toLowerCase()));

  return (
    <Shell title="Admin">
      <div className="stats">
        <div className="stat"><b>{list.length}</b><span>Total people</span></div>
        <div className="stat"><b>{list.filter(e => e.role === 'MANAGER').length}</b><span>Managers</span></div>
        <div className="stat"><b>{list.filter(e => e.role === 'EMPLOYEE').length}</b><span>Employees</span></div>
        <div className="stat"><b>{list.filter(e => e.shift === 'NIGHT').length}</b><span>On night shift</span></div>
      </div>

      {created && <div className="cred">
        Login for <b>{created.name}</b> &mdash; Employee ID: <code>{created.code}</code> &nbsp; Temporary password: <code>{created.pw}</code>
        <div className="small muted">Shown only once. It was also e-mailed to the company e-mail when mail is configured.</div>
      </div>}

      <section className="card">
        <h2>{editId ? 'Edit employee' : 'Add employee'}</h2>
        <form onSubmit={submit}>
          <div className="grid">
            <Field label="Employee ID *"><input required disabled={!!editId} value={form.empCode} onChange={set('empCode')} placeholder="EMP101" /></Field>
            <Field label="Full name *"><input required value={form.fullName} onChange={set('fullName')} /></Field>
            <Field label="Company e-mail *"><input required type="email" value={form.companyEmail} onChange={set('companyEmail')} /></Field>
            <Field label="Personal e-mail"><input type="email" value={form.personalEmail} onChange={set('personalEmail')} /></Field>
            <Field label="Phone"><input value={form.phone} onChange={set('phone')} /></Field>
            <Field label="Emergency contact"><input value={form.emergencyContact} onChange={set('emergencyContact')} /></Field>
            <Field label="Gender"><select value={form.gender} onChange={set('gender')}><option>Male</option><option>Female</option><option>Other</option></select></Field>
            <Field label="Date of birth"><input type="date" value={form.dateOfBirth} onChange={set('dateOfBirth')} /></Field>
            <Field label="Joining date"><input type="date" value={form.joiningDate} onChange={set('joiningDate')} /></Field>
            <Field label="Department"><input value={form.department} onChange={set('department')} /></Field>
            <Field label="Designation"><input value={form.designation} onChange={set('designation')} /></Field>
            <Field label="Role"><select value={form.role} onChange={set('role')}><option value="EMPLOYEE">Employee</option><option value="MANAGER">Manager</option></select></Field>
            <Field label="Shift"><select value={form.shift} onChange={set('shift')}><option value="DAY">Day shift (09:00 - 18:00)</option><option value="NIGHT">Night shift (21:00 - 06:00)</option></select></Field>
            <Field label="Reporting manager">
              <select value={form.managerId} disabled={form.role !== 'EMPLOYEE'} onChange={set('managerId')}>
                <option value="">- none -</option>{managers.map(m => <option key={m.id} value={m.id}>{m.fullName} ({m.empCode})</option>)}
              </select>
            </Field>
            {!editId && <Field label="Temporary password (blank = auto-generate)"><input value={form.tempPassword} onChange={set('tempPassword')} /></Field>}
            <Field label="Address"><textarea value={form.address} onChange={set('address')} style={{ minHeight: 40 }} /></Field>
          </div>
          <div className="row sp">
            <button className="btn primary">{editId ? 'Save changes' : 'Add employee'}</button>
            {editId && <button type="button" className="btn" onClick={() => { setEditId(null); setForm(EMPTY_EMP); }}>Cancel edit</button>}
          </div>
        </form>
      </section>

      <section className="card">
        <div className="row" style={{ justifyContent: 'space-between' }}>
          <h2>People</h2>
          <input style={{ maxWidth: 260 }} placeholder="Search by ID, name, e-mail" value={q} onChange={e => setQ(e.target.value)} aria-label="Search" />
        </div>
        <div className="tw"><table>
          <thead><tr><th>ID</th><th>Name</th><th>Company e-mail</th><th>Role</th><th>Shift</th><th>Manager</th><th>Status</th><th></th></tr></thead>
          <tbody>{shown.map(e => <tr key={e.id}>
            <td><b>{e.empCode}</b></td><td>{e.fullName}<div className="small muted">{e.designation}</div></td><td>{e.companyEmail}</td>
            <td><Badge v={e.role} /></td><td><Badge v={e.shift} /></td><td>{e.managerName || '-'}</td>
            <td><Badge v={e.active ? 'ACTIVE' : 'REJECTED'} /></td>
            <td><div className="row">
              <button className="btn sm" onClick={() => edit(e)}>Edit</button>
              <button className="btn sm" onClick={() => setQr(e)}>QR</button>
              <button className="btn sm" onClick={() => reset(e)}>Reset password</button>
              <button className="btn sm" onClick={() => toggle(e)}>{e.active ? 'Deactivate' : 'Activate'}</button>
            </div></td></tr>)}</tbody>
        </table></div>
        {emps && shown.length === 0 && <Empty text="No matching people." />}
      </section>

      {qr && <Modal title={'Attendance QR - ' + qr.fullName} onClose={() => setQr(null)}>
        <img className="qr" alt={'QR for ' + qr.empCode} src={BASE + '/api/admin/employees/' + qr.id + '/qr'} />
        <p className="muted small">Give or print this QR for {qr.empCode}. Scanning it at <b>/scan</b> records check-in, then check-out.</p>
        <div className="row end"><a className="btn primary" download={qr.empCode + '-qr.png'} href={BASE + '/api/admin/employees/' + qr.id + '/qr'}>Download QR</a></div>
      </Modal>}
      {toast}
    </Shell>
  );
}
ReactDOM.createRoot(document.getElementById('root')).render(<AdminApp />);
