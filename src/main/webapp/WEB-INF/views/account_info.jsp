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
            <span>Balance: </span><span>${user.balance}</span><br>
            <span>Overdraft Balance: </span><span>${user.overDraftBalance}</span><br>
            <span>Crypto Balance in USD: </span><span>${user.cryptoBalanceUSD}</span><br>
            <span>Ethereum Coins Owned: </span><span>${user.ethBalance}</span><br>
            <span>Solana Coins Owned: </span><span>${user.solBalance}</span><br>
            <span>Current ETH Price: </span><span>${user.ethPrice}</span><br>
            <span>Current SOL Price: </span><span>${user.solPrice}</span><br>

            <c:if test="${!empty user.overdraftHist}">
                <h4>Repayment History</h4>
                <table class="table">
                    <thead>
                    <tr>
                        <th scope="col">Time</th>
                        <th scope="col">Amount</th>
                        <th scope="col">Old Overdraft Balance</th>
                        <th scope="col">New Overdraft Balance</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="transaction" items="${user.overdraftHist}">
                        <tr>
                            <td>${transaction.time}</td>
                            <td>${transaction.amount}</td>
                            <td>${transaction.oldBalance}</td>
                            <td>${transaction.newBalance}</td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:if>

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

            <c:if test="${!empty user.transferHist}">
                <h4>Transfer History</h4>
                <table class="table">
                    <thead>
                    <tr>
                        <th scope="col">Time</th>
                        <th scope="col">To</th>
                        <th scope="col">From</th>
                        <th scope="col">Amount</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="transaction" items="${user.transferHist}">
                        <tr>
                            <td>${transaction.time}</td>
                            <td>${transaction.to}</td>
                            <td>${transaction.from}</td>
                            <td>${transaction.amount}</td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:if>

            <c:if test="${!empty user.cryptoHist}">
                <h4>Crypto History</h4>
                <table class="table">
                    <thead>
                    <tr>
                        <th scope="col">Time</th>
                        <th scope="col">Action</th>
                        <th scope="col">Cryptocurrency</th>
                        <th scope="col">Amount</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="transaction" items="${user.cryptoHist}">
                        <tr>
                            <td>${transaction.time}</td>
                            <td>${transaction.action}</td>
                            <td>${transaction.cryptoName}</td>
                            <td>${transaction.amount}</td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:if>
            <br>
        </div>
    </jsp:body>
</t:genericpage>