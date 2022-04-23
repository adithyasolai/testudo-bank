<%--@elvariable id="user" type="net.testudobank.User"--%>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<t:genericpage>
    <jsp:attribute name="header">
    <title>Deposit Form</title>
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
            <form:form action="deposit" method="post" modelAttribute="user">
                <div class="mb-3">
                    <form:label path="amountToDeposit">Amount to Deposit ($):</form:label>
                    <form:input path="amountToDeposit"/>
                </div>
                <form:button type="submit" class="btn btn-primary">Deposit</form:button>
            </form:form>
        </div>
    </jsp:body>
</t:genericpage>