<%--@elvariable id="user" type="net.testudobank.User"--%>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<t:genericpage>
    <jsp:attribute name="header">
      <title>${user.firstName} ${user.lastName} - Testudo Bank</title>
    </jsp:attribute>
    <jsp:body>
        <div class="container text-center">
            <h2><span>${user.firstName}</span> <span>${user.lastName}</span> Bank Account Info</h2>
            <span>Username: </span><span>${user.username}</span><br>
            <span>Balance: $</span><span>${user.balance}</span><br>
            <span>Overdraft Balance: $</span><span>${user.overDraftBalance}</span><br>
            <span>Crypto Balance in USD: $</span><span>${user.cryptoBalanceUSD}</span><br>
            <span>Ethereum Coins Owned: </span><span>${user.ethBalance}</span><br>
            <span>Solana Coins Owned: </span><span>${user.solBalance}</span><br>
            <span>Current $ETH Price: </span><span>${user.ethPrice}</span><br>
            <span>Current $SOL Price: </span><span>${user.solPrice}</span><br>
            <span>Re-payment logs: </span><span>${user.logs}</span><br>

            <c:if test="${!empty user.transactionHist}">
            <h4>Transaction History</h4>
            <table class="table">
                <thead>
                <tr>
                    <th scope="col">Time</th>
                    <th scope="col">Action</th>
                    <th scope="col">Amount</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="transaction" items="${user.transactionHist}">
                    <tr>
                        <td>${transaction.time}</td>
                        <td>${transaction.action}</td>
                        <td>${transaction.amount}</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
            </c:if>
            <span>Transfer History: </span><span>${user.transferHist}</span><br>
            <span>Crypto History: </span><span>${user.cryptoHist}</span><br>
            <br>
        </div>
    </jsp:body>
</t:genericpage>