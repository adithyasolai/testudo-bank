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
  <data id="Dollar" type="text" value="${user.balance.toString()}"></data>
  <data id="ETH" type="text" value="${user.ethValue.toString()}"></data>
  <data id="SOL" type="text" value="${user.solValue.toString()}"></data>
  <script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
  <script type="text/javascript">

    // Load the Visualization API and the corechart package.
    google.charts.load('current', {'packages':['corechart']});

    // Set a callback to run when the Google Visualization API is loaded.
    google.charts.setOnLoadCallback(drawChart);

    // Callback that creates and populates a data table,
    // instantiates the pie chart, passes in the data and
    // draws it.
    function drawChart() {

      // Create the data table.
      var data = new google.visualization.DataTable();
      data.addColumn('string', 'Asset');
      data.addColumn('number', 'Dollar Value');
      data.addRows([
        ['USD', Number(document.getElementById('Dollar').value)],
        ['ETH', Number(document.getElementById('ETH').value)],
        ['SOL', Number(document.getElementById('SOL').value)]
      ]);

      // Set chart options
      var options = {'title':'Pie Chart for Asset Classes',
                    'width':400,
                    'height':300};

      // Instantiate and draw our chart, passing in some options.
      var chart = new google.visualization.PieChart(document.getElementById('chart_div'));
      chart.draw(data, options);
    }
  </script>
</head>
<body>
	<div align="center">
		<h2><span>${user.firstName}</span> <span>${user.lastName}</span> Bank Account Info</h2>
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
    <div id="chart_div"></div>
    <br/>
    <a href='/deposit'>Deposit</a>
    <a href='/withdraw'>Withdraw</a>
    <a href='/dispute'>Dispute</a>
    <a href='/transfer'>Transfer</a>
    <a href='/'>Logout</a>
	</div>
</body>
</html>