<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Admin - Smart Attendance</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=Public+Sans:wght@400;500;600;700&display=swap" rel="stylesheet">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css">
</head>
<body>
<div id="root"><p class="loading">Loading...</p></div>
<%@ include file="_scripts.jspf" %>
<script type="text/babel" src="${pageContext.request.contextPath}/js/admin.jsx"></script>
</body>
</html>
