<head>
<jsp:directive.include file="/WEB-INF/jsp/prelude/include-head-meta.jspf" />
	<title>My Home Page</title>
    <style>
      .bd-placeholder-img {
        font-size: 1.125rem;
        text-anchor: middle;
        -webkit-user-select: none;
        -moz-user-select: none;
        -ms-user-select: none;
        user-select: none;
      }

      @media (min-width: 768px) {
        .bd-placeholder-img-lg {
          font-size: 3.5rem;
        }
      }
    </style>
</head>
<body>
	<div class="container">
		<form class="form-signin" novalidate>
			<h1 class="h3 mb-3 font-weight-normal">登录</h1>
			<label for="inputEmail" class="sr-only">请输出电子邮件</label> <input
				type="email" id="inputEmail" class="form-control"
				placeholder="请输入电子邮件" required autofocus> <label
				for="inputPassword" class="sr-only">Password</label> <input
				type="password" id="inputPassword" class="form-control"
				placeholder="请输入密码" required>
			<div class="checkbox mb-3">
				<label> <input type="checkbox" value="remember-me">
					Remember me
				</label>
			</div>
			<button class="btn btn-lg btn-primary btn-block" id="submitBtn" type="submit" on>Sign in</button>
			<p class="mt-5 mb-3 text-muted">&copy; 2017-2021</p>
		</form>
	</div>
<script >

	$(document).ready(function(){
		var forms = document.getElementsByClassName('form-signin');
		var validation = Array.prototype.filter.call(forms, function(form) {
			form.addEventListener('submit', function(event) {
				console.log(form);
				console.log(event);
				if (form.checkValidity() === false) {
					event.preventDefault();
					event.stopPropagation();
					form.classList.add('was-validated');
					return;
				}
				sendLoginRequest();
				event.preventDefault();

			}, false);
		});

		function sendLoginRequest(){
			var param = {name:"", password:""};
			param.name = document.getElementById("inputEmail").value;
			param.password = document.getElementById("inputPassword").value;
			console.log(param);
			$.post("/rest/login", param,function(data, status) {
				alert(status);
			});
		}

	});

</script>
</body>