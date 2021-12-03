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
    <span>Cryptocurrency Balance is: ETH </span><span>${user.ethbalance}</span><br/>
    <span>Total Amount currently invested in Cryptocurrency: $</span><span>${user.totalCryptoInvestment}</span><br/>
    <span>Total Profits/Losses in Cryptocurrency Investments: $</span><span>${user.profits}</span><br>
    <span>Overdraft Balance is: $</span><span>${user.overDraftBalance}</span><br/>
    <span>Re-payment logs: </span><span>${user.logs}</span><br/>
    <span>Transaction History: </span><span>${user.transactionHist}</span><br/>
    <br/>
    <a href='/deposit'>Deposit</a>
    <a href='/withdraw'>Withdraw</a>
    <a href='/buycrypto'>Buy Cryptocurrency</a>
    <a href='/sellcrypto'>Sell Cryptocurrency</a> 
    <a href='/dispute'>Dispute</a>
    <a href='/'>Logout</a>
	</div>
</body>
</html>