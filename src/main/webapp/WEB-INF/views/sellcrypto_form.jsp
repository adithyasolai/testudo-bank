<%--@elvariable id="user" type="net.testudobank.User"--%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
         pageEncoding="ISO-8859-1" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:genericpage>
    <jsp:attribute name="header">
         <title>Sell Cryptocurrency Form</title>
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
        <div align="center">
            <form:form action="sellcrypto" method="post" modelAttribute="user">
                <form:label path="whichCryptoToBuy">Which Crypto to buy (Type 'ETH' or 'SOL'):</form:label>
                <form:input path="whichCryptoToBuy"/><br/>

                <form:label path="amountToSellCrypto">Amount to buy (# of Coins, Fractional Allowed):</form:label>
                <form:input path="amountToSellCrypto"/><br/>

                <span>Current $ETH Price: </span><span>${user.ethPrice}</span><br/>
                <span>Current $SOL Price: </span><span>${user.solPrice}</span><br/>

                <form:button type="submit" class="btn btn-primary">Sell Crypto</form:button>
            </form:form>
        </div>
    </jsp:body>
</t:genericpage>