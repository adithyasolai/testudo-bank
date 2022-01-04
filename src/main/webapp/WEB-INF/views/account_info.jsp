<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>  
<!DOCTYPE html>
<html>
<head>
  <link rel="icon" href="https://fanapeel.com/wp-content/uploads/logo_-university-of-maryland-terrapins-testudo-turtle-hold-red-white-m.png">
  <meta charset="ISO-8859-1">
  <title>${user.firstName} ${user.lastName} - Testudo Bank</title>
  <style type="text/css">
    label {
      display: inline-block;
      width: 200px;
      margin: 5px;
      text-align: left;
    }
    button {
      padding: 10px;
      margin: 10px;
    }
    a.button {
      -webkit-appearance: button;
      -moz-appearance: button;
      appearance: button;

      text-decoration: none;
      color: initial;
    }
  </style>
</head>
<body>
	<div align="center">
		<h2><span>${user.firstName}</span> <span>${user.lastName}</span> Bank Account Info</h2>
		<span>Balance is: $</span><span>${user.balance}</span><br/>
    <span>Overdraft Balance is: $</span><span>${user.overDraftBalance}</span><br/>
    <span>Re-payment logs: </span><span>${user.logs}</span><br/>
    <span>Transaction History: </span><span>${user.transactionHist}</span><br/>
    <span>Large Loan Balance: </span><span>${user.largeLoanBalance}</span><br/>
    <span>Large Loan Installments: </span><span>${user.largeLoanInstallments}</span><br/>
    <span>Large Loan Logs (cents): </span><span>${user.largeLoanLogs}</span><br/>
    <br/>
    <a href='/deposit'>Deposit</a>
    <a href='/withdraw'>Withdraw</a>
    <a href='/dispute'>Dispute</a> <br/>
    <a href='/large-loan-request'>Large Loan Request</a> <br/>
    <a href='/large-loan-repayment'>Large Loan Repayment</a> <br/>
    <a href='/'>Logout</a>
	</div>
</body>
</html>