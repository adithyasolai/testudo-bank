<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
         pageEncoding="ISO-8859-1" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:genericpage>
    <jsp:attribute name="header">
      <title>Dispute Form</title>
      <style type="text/css">
          label {
              display: inline-block;
              width: 200px;
              margin: 5px;
              text-align: left;
          }

          input[type=text], input[type=password], select {
              width: 200px;
          }

          input[type=radio] {
              display: inline-block;
              margin-left: 45px;
          }

          input[type=checkbox] {
              display: inline-block;
              margin-right: 190px;
          }

          button {
              padding: 10px;
              margin: 10px;
          }
      </style>
    </jsp:attribute>
    <jsp:body>
        <div class="container text-center">
            <form:form action="dispute" method="post" modelAttribute="user">
                <form:label
                        path="numTransactionsAgo">Transaction to Reverse (1=Most Recent, 2=2 Transactions Ago, Max = 3):</form:label>
                <form:input path="numTransactionsAgo"/><br/>

                <form:button type="submit" class="btn btn-primary">Reverse</form:button>
            </form:form>
        </div>
    </jsp:body>
</t:genericpage>