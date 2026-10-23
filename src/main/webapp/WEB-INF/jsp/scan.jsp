<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Scan attendance - Smart Attendance</title>
<link href="https://fonts.googleapis.com/css2?family=Public+Sans:wght@400;500;600;700&display=swap" rel="stylesheet">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css">
</head>
<body class="login-body">
<main class="login wide">
  <h1>Scan your attendance QR</h1>
  <p class="muted">Hold the QR given by the admin in front of the camera. First scan = check-in, next scan = check-out.</p>
  <div id="reader"></div>
  <div id="result" class="scan-result" role="status"></div>
  <details><summary>Camera not working? Enter the code manually</summary>
    <label class="fld"><span>QR code text</span><input id="manual" placeholder="paste QR code text"></label>
    <button class="btn" id="go" type="button">Submit</button>
  </details>
  <p class="muted small"><a href="${pageContext.request.contextPath}/login">Back to sign in</a> &middot; Camera needs https or localhost.</p>
</main>
<script src="https://cdnjs.cloudflare.com/ajax/libs/html5-qrcode/2.3.8/html5-qrcode.min.js"></script>
<script>
var base = '${pageContext.request.contextPath}', busy = false, box = document.getElementById('result');
function send(token) {
  if (busy) return; busy = true;
  fetch(base + '/api/attendance/scan', {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({token: token})})
    .then(function (r) { return r.json().then(function (d) { return {ok: r.ok, d: d}; }); })
    .then(function (x) {
      if (x.ok) { box.className = 'scan-result good'; box.textContent = x.d.employee + ' (' + x.d.empCode + ') - ' + x.d.message; }
      else { box.className = 'scan-result bad'; box.textContent = x.d.error || 'Scan failed'; }
    })
    .catch(function () { box.className = 'scan-result bad'; box.textContent = 'Server not reachable'; })
    .then(function () { setTimeout(function () { busy = false; }, 4000); });
}
try { new Html5QrcodeScanner('reader', {fps: 10, qrbox: 240}, false).render(send, function () {}); } catch (e) {}
document.getElementById('go').onclick = function () { send(document.getElementById('manual').value); };
</script>
</body>
</html>
