<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>  
<!DOCTYPE html>
<html>
<head>
  <link rel="icon" href="https://fanapeel.com/wp-content/uploads/logo_-university-of-maryland-terrapins-testudo-turtle-hold-red-white-m.png">
  <meta charset="ISO-8859-1">
  <title>Suspicious Activity- Testudo Bank</title>
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
<script>
  function drawTable() {

      var transactions = '${user.susTransactionHist}'.split("*,");
      
      console.log("adding rows...")
      // console.log(user.susTransactionHist.length);
      var transactionsLen = transactions.length;
      var totalRows = transactionsLen;
      var cellsInRow = 2;
      var min = 1;
      var max = 10;
      // get the reference for the body
      var div1 = document.getElementById('div1');

      // creates a <table> element
      var tbl = document.createElement("table");
      
      //add header
      var header = document.createElement("tr");
      header.insertCell(0).innerHTML= '<b>Transactions</b><';
      header.insertCell(1).innerHTML= '<b>Not Fraud</b><';
      tbl.appendChild(header); 

      // creating rows
      for (var r = 0; r < totalRows; r++) {
            var row = document.createElement("tr");

        // create cells in row

        row.insertCell(0).innerHTML= transactions[r];
        //row.insertCell(1).innerHTML='<button type="button" onclick="formAJAX(this)">Delete</button>'
        row.insertCell(1).innerHTML= '<input type="checkbox" name="name2"/>';
        tbl.appendChild(row); // add the row to the end of the table body
        }

      div1.appendChild(tbl); // appends <table> into <div1>
}
window.onload=drawTable; 
</script>
</head>
<body>
	<!-- <div align="center">
		<h2>Suspicious Activity</h2>
    <span>Suspicious Transactions: </span>
    <span>${user.susTransactionHist[0]}</span><br/>
    <a href='/'>Home</a>
	</div> -->
  <!-- <div id="mydata">
    <b>Current data in the system ...</b>
    <table id="myTableData"  border="1" cellpadding="2">
    <tr>
    <td><b>Transactions</b></td>
    <td><b>Flag as Fraud</b></td>
    </tr>
    </table>
    </div>
    <div>
      <input type="button" id="add" value="Add" onclick="Javascript:addRows()"></td>
    </div>
    <script>
      function addRows() {
        var table = document.getElementById("myTableData");
        var transactions = '${user.susTransactionHist}'.split("*,")
        console.log("adding rows...")
        console.log('${user.susTransactionHist}');
        var transactionsLen = transactions.length;
        console.log(transactionsLen);
        for(var i=0; i<transactionsLen; i++){
          console.log(transactions[i]);
          var rowCount = table.rows.length;
          var row = table.insertRow(rowCount);
       
          row.insertCell(0).innerHTML= transactions[i];
          row.insertCell(1).innerHTML='<button type="button" onclick="formAJAX(this)">Delete</button>'
          //row.insertCell(1).innerHTML= '<form action="suspiciousActivity" method="post" modelAttribute="user"> <button>Delete</button> </form>';
        }
      }

      function formAJAX(btn){
        var $form = $(btn).closest('[action]');
        var str = $form.find('[name]').serialize();
        $.post($form.attr('action'), str, function(data){
            //do stuff
        });
    }
    </script> -->
    <body>
      <b>Current data in the system ...</b>
      <div id="div1"></div>
    
    </body>
</body>
</html>