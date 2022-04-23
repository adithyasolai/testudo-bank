<%--@elvariable id="user" type="net.testudobank.User"--%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
         pageEncoding="ISO-8859-1" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:genericpage>
    <jsp:attribute name="header">
          <title>Buy Cryptocurrency Form</title>
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
            <form:form action="buycrypto" method="post" modelAttribute="user">

                <div class="form-check">
                    <form:radiobutton path="whichCryptoToBuy" value="ETH" required="required" checked="checked"/>
                    <form:label path="whichCryptoToBuy">ETH</form:label>
                </div>
                <div class="form-check">
                    <form:radiobutton path="whichCryptoToBuy" value="SOL" required="required"/>
                    <form:label path="whichCryptoToBuy">SOL</form:label>
                </div>

                <form:label path="amountToBuyCrypto">Amount to buy (# of Coins, Fractional Allowed):</form:label>
                <form:input path="amountToBuyCrypto"/><br>

                <span>Current ETH Price: </span><span>${user.ethPrice}</span><br>
                <span>Current SOL Price: </span><span>${user.solPrice}</span><br>

                <form:button type="submit" class="btn btn-primary">Buy Crypto</form:button>
            </form:form>
        </div>
    </jsp:body>
</t:genericpage>