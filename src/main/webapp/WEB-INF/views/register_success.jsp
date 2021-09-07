<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>    
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Account</title>
<style type="text/css">
	label {
		display: inline-block;
		width: 200px;
		margin: 5px;
		text-align: left;
	}
	/* input[type=text], input[type=password], select {
		width: 200px;	
	}
	input[type=radio] {
		display: inline-block;
		margin-left: 45px;
	} */
	
	/* input[type=checkbox] {
		display: inline-block;
		margin-right: 190px;
	}	
	 */
	button {
		padding: 10px;
		margin: 10px;
	}
</style>
</head>
<body>
	<div align="center">
		<form:form action="account" method="post" modelAttribute="user">
		<h2>Log In Successful!</h2>
		<span>Balance is:</span><span>${user.balance}</span><br/>
		<!-- <span>Password:</span><span>${user.password}</span><br/> -->
		<!-- <form:button>Deposit</form:button>
		<form:button>Withdraw</form:button> -->
		
			<!-- <form:label path="username">Username:</form:label>
			<form:input path="username"/><br/>
			
			<form:label path="password">Password:</form:label>
			<form:password path="password"/><br/>		 -->
				
			<form:button>Deposit</form:button>
			<form:button>Withdraw</form:button>
		</form:form>
	</div>
</body>
</html>