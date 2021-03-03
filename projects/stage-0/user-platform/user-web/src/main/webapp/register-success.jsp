<head>
<jsp:directive.include
	file="/WEB-INF/jsp/prelude/include-head-meta.jspf" />
<title>My Home Page</title>
</head>
<body>
	<div class="container-lg">
		<!-- Content here -->
		<H3>
		<%= request.getAttribute("userName")%> 注册 <%= request.getAttribute("statusName") %> !
		</H3>
	</div>
</body>