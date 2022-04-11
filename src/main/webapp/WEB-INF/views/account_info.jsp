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
<<<<<<< HEAD
		<span>Balance is: $</span><span>${user.balance}</span><br/>
    <span>Overdraft Balance is: $</span><span>${user.overDraftBalance}</span><br/>
    <span>Current $ETH Price: </span><span>${user.ethPrice}</span><br/>
    <span>ETH Amount: </span><span>${user.ethAmount}</span><br/>
    <span>ETH Value $</span><span>${user.ethValue}</span><br/>
    <span>Re-payment logs: </span><span>${user.logs}</span><br/>
    <span>Transaction History: </span><span>${user.transactionHist}</span><br/>
    <span>Transfer History: </span><span>${user.transferHist}</span><br/>
    <span>Crypto History: </span><span>${user.cryptoHist}</span>
=======
    <span>Username: </span><span>${user.username}</span><br/>
		<span>Balance: $</span><span>${user.balance}</span><br/>
    <span>Overdraft Balance: $</span><span>${user.overDraftBalance}</span><br/>
    <span>Crypto Balance in USD: $</span><span>${user.cryptoBalanceUSD}</span><br/>
    <span>Ethereum Coins Owned: </span><span>${user.ethBalance}</span><br/>
    <span>Solana Coins Owned: </span><span>${user.solBalance}</span><br/>
    <span>Current $ETH Price: </span><span>${user.ethPrice}</span><br/>
    <span>Current $SOL Price: </span><span>${user.solPrice}</span><br/>
    <span>Re-payment logs: </span><span>${user.logs}</span><br/>
    <span>Transaction History: </span><span>${user.transactionHist}</span><br/>
    <span>Transfer History: </span><span>${user.transferHist}</span><br/>
    <span>Crypto History: </span><span>${user.cryptoHist}</span><br/>
>>>>>>> 187b9a6bb23f62da2e0176c5b4b5f8beb907a85c
    <br/>
    <a href='/deposit'>Deposit</a>
    <a href='/withdraw'>Withdraw</a>
    <a href='/dispute'>Dispute</a>
    <a href='/transfer'>Transfer</a>
    <a href='/'>Logout</a>
	</div>
</body>
</html>