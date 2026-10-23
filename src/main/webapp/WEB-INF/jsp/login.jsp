<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Sign in - Smart Attendance</title>
<link href="https://fonts.googleapis.com/css2?family=Public+Sans:wght@400;500;600;700&display=swap" rel="stylesheet">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css">
</head>
<body class="login-body">
<main class="login">
  <h1>Smart Attendance</h1>
  <p class="muted">Sign in with your employee ID. New joiners use the temporary password from the admin.</p>
  <form id="f">
    <label class="fld"><span>Employee ID</span><input id="u" autocomplete="username" required placeholder="e.g. EMP001"></label>
    <label class="fld"><span>Password</span><input id="p" type="password" autocomplete="current-password" required></label>
    <div id="err" class="err" role="alert"></div>
    <button class="btn primary" type="submit">Sign in</button>
  </form>
  <p class="muted small">Arriving at the office? <a href="${pageContext.request.contextPath}/scan">Scan your attendance QR</a></p>
</main>
<script>
var base = '${pageContext.request.contextPath}';
document.getElementById('f').addEventListener('submit', function (ev) {
  ev.preventDefault();
  var err = document.getElementById('err'); err.textContent = '';
  fetch(base + '/api/auth/login', {
    method: 'POST', headers: {'Content-Type': 'application/json'}, credentials: 'same-origin',
    body: JSON.stringify({empCode: document.getElementById('u').value, password: document.getElementById('p').value})
  }).then(function (r) { return r.json().then(function (d) { return {ok: r.ok, d: d}; }); })
    .then(function (x) { if (!x.ok) { err.textContent = x.d.error || 'Sign in failed'; return; } location.href = base + x.d.redirect; })
    .catch(function () { err.textContent = 'Server not reachable'; });
});
</script>
</body>
</html>
