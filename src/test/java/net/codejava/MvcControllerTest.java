package net.codejava;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.ui.Model;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class MvcControllerTest {
	@Mock
	private static JdbcTemplate jdbcTemplate;

  @Mock
  Model mockModel;

	private MvcController controller;

  private static String CUSTOMER1_USERNAME;
  private static List<Map<String, Object>> CUSTOMER1_DATA;
  private static List<Map<String, Object>> TRANSACTION_HIST;
  private static List<Map<String, Object>> TRANSACTION_HIST_WITHDRAW;
  private static List<Map<String, Object>> OVERDRAFT_LOGS;

  @BeforeAll
  public static void init() {
    CUSTOMER1_USERNAME = "123456789";

    // prepare what the updateAccountInfo() helper method should return when stubbed
    CUSTOMER1_DATA = new ArrayList<>();
    CUSTOMER1_DATA.add(new HashMap<>());
    CUSTOMER1_DATA.get(0).put("FirstName", "John");
    CUSTOMER1_DATA.get(0).put("LastName", "Doe");
    CUSTOMER1_DATA.get(0).put("Balance", 10000);
	  CUSTOMER1_DATA.get(0).put("OverdraftBalance", 0);
    // prepare what seaerch for transaction history with deposit should return
    TRANSACTION_HIST = new ArrayList<>();
    TRANSACTION_HIST.add(new HashMap<>());
    TRANSACTION_HIST.get(0).put("CustomerID", "123456789");
    TRANSACTION_HIST.get(0).put("Timestamp", "2021-11-03 11:35:45");
    TRANSACTION_HIST.get(0).put("Action", "Deposit");
    TRANSACTION_HIST.get(0).put("Amount", 10000);
    // prepare what seaerch for transaction history with withdraw should return
    TRANSACTION_HIST_WITHDRAW = new ArrayList<>();
    TRANSACTION_HIST_WITHDRAW.add(new HashMap<>());
    TRANSACTION_HIST_WITHDRAW.get(0).put("CustomerID", "123456789");
    TRANSACTION_HIST_WITHDRAW.get(0).put("Timestamp", "2021-11-03 11:35:45");
    TRANSACTION_HIST_WITHDRAW.get(0).put("Action", "Withdraw");
    TRANSACTION_HIST_WITHDRAW.get(0).put("Amount", 10000);

    OVERDRAFT_LOGS = new ArrayList<>();
  }

  @BeforeEach
  public void setup() {
    MockitoAnnotations.initMocks(this);
    controller = new MvcController(jdbcTemplate);
  }

	@Test
	public void testControllerIsCreated() {
		assertThat(controller, is(notNullValue()));
	}

	@Test
	public void testShowWelcomeSuccess() {
		assertEquals("welcome", controller.showWelcome(null));
	}

	@Test
	public void testShowLoginForm() {
		assertEquals("login_form", controller.showLoginForm(mockModel));
	}

	@Test
	public void testSubmitLoginFormSuccessWithCorrectPassword() {
		User customer1 = new User();
		customer1.setUsername(CUSTOMER1_USERNAME);
		customer1.setPassword("password");

    // stub jdbc calls
    // successful login
    String getCustomer1PasswordSql=String.format("SELECT Password FROM Passwords WHERE CustomerID='%s';", customer1.getUsername());
		when(jdbcTemplate.queryForObject(eq(getCustomer1PasswordSql), eq(String.class))).thenReturn("password");
    // handles updateAccountInfo() helper method
    when(jdbcTemplate.queryForList(anyString())).thenReturn(CUSTOMER1_DATA);

    // send login request
    String pageReturned = controller.submitLoginForm(customer1);

    // Verify that the SELECT SQL command executed to retrieve user's password uses the customer's ID
    Mockito.verify(jdbcTemplate, Mockito.times(1)).queryForObject(eq(getCustomer1PasswordSql), eq(String.class));

    // verify "account_info" page is returned
		assertEquals("account_info", pageReturned);
	}

	@Test
	public void testSubmitLoginFormFailureWithIncorrectPassword() {
		User customer1 = new User();
		customer1.setUsername(CUSTOMER1_USERNAME);
		customer1.setPassword("not password");

    // stub jdbc calls
    // successful login
    String getCustomer1PasswordSql=String.format("SELECT Password FROM Passwords WHERE CustomerID='%s';", customer1.getUsername());
		when(jdbcTemplate.queryForObject(eq(getCustomer1PasswordSql), eq(String.class))).thenReturn("password");
    // handles updateAccountInfo() helper method
    when(jdbcTemplate.queryForList(anyString())).thenReturn(CUSTOMER1_DATA);

    // send login request
    String pageReturned = controller.submitLoginForm(customer1);

    // Verify that the SELECT SQL command executed to retrieve user's password uses the customer's ID
    Mockito.verify(jdbcTemplate, Mockito.times(1)).queryForObject(eq(getCustomer1PasswordSql), eq(String.class));

    // verify that customer is re-directed to "welcome" page
		assertEquals("welcome", pageReturned);
	}

	@Test
	public void testShowDepositFormSuccess() {
		assertEquals("deposit_form", controller.showDepositForm(mockModel));
	}

	@Test
	public void testDepositFailurewithIncorrectPassword() {
		User customer1 = new User();
		customer1.setUsername(CUSTOMER1_USERNAME);
		customer1.setPassword("not password");
		customer1.setAmountToDeposit(100);

    // stub jdbc calls
    // unsuccessful login
    String getCustomer1PasswordSql=String.format("SELECT Password FROM Passwords WHERE CustomerID='%s';", customer1.getUsername());
		when(jdbcTemplate.queryForObject(eq(getCustomer1PasswordSql), eq(String.class))).thenReturn("password");

    // send deposit request
    String pageReturned = controller.submitDeposit(customer1);

    // Verify that no SQL Update commands are sent
    Mockito.verify(jdbcTemplate, Mockito.times(0)).update(anyString());

    // verify that customer is re-directed to "welcome" page
		assertEquals("welcome", pageReturned);
	}

	@Test
	public void testShowWithdrawFormSuccess() {
		assertEquals("withdraw_form", controller.showWithdrawForm(mockModel));
	}

	@Test
	public void testWithdrawFailurewithIncorrectPassword() {
		User customer1 = new User();
		customer1.setUsername(CUSTOMER1_USERNAME);
		customer1.setPassword("not password");
		customer1.setAmountToWithdraw(100);

    // stub jdbc calls
    // unsuccessful login
    String getCustomer1PasswordSql=String.format("SELECT Password FROM Passwords WHERE CustomerID='%s';", customer1.getUsername());
		when(jdbcTemplate.queryForObject(eq(getCustomer1PasswordSql), eq(String.class))).thenReturn("password");

    // send withdraw request
    String pageReturned = controller.submitWithdraw(customer1);

    // Verify that no SQL Update commands are sent
    Mockito.verify(jdbcTemplate, Mockito.times(0)).update(anyString());

    // verify that customer is re-directed to "welcome" page
		assertEquals("welcome", pageReturned);
	}

	@Test
	public void testWithdrawOverDraftBalanceFailure() {
		User customer1 = new User();
		customer1.setUsername(CUSTOMER1_USERNAME);
		customer1.setPassword("password");
		customer1.setAmountToWithdraw(2000); // try to withdraw $2000 in overdraft, but the max allowed is $1000

		String getCustomer1PasswordSql = String.format("SELECT Password FROM Passwords WHERE CustomerID='%s';", CUSTOMER1_USERNAME);
		String getCustomer1BalanceSql =  String.format("SELECT Balance FROM Customers WHERE CustomerID='%s';", CUSTOMER1_USERNAME);
		
    // stub jdbc calls
    // successful login
		when(jdbcTemplate.queryForObject(eq(getCustomer1PasswordSql), eq(String.class))).thenReturn("password");
    // handles updateAccountInfo() helper method
    when(jdbcTemplate.queryForList(anyString())).thenReturn(CUSTOMER1_DATA);
    // not working with live DB
		when(jdbcTemplate.update(anyString())).thenReturn(1);
    // handles account not being locked
    String getNumReversals = String.format("SELECT NumFraudReversals FROM Customers WHERE CustomerID='%s';", customer1.getUsername());
    when(jdbcTemplate.queryForObject(eq(getNumReversals), eq(Integer.class))).thenReturn(0);
    // start customer with balance of $0
		when(jdbcTemplate.queryForObject(eq(getCustomer1BalanceSql), eq(Integer.class))).thenReturn(0);

    // send withdraw request
    String pageReturned = controller.submitWithdraw(customer1);
    
    Mockito.verify(jdbcTemplate, Mockito.times(1)).queryForObject(eq(getCustomer1PasswordSql), eq(String.class));
    Mockito.verify(jdbcTemplate, Mockito.times(1)).queryForObject(eq(getCustomer1BalanceSql), eq(Integer.class));
    // no update due to failing on customer.getAmountToWithdraw() > MAX_AMOUNT
    Mockito.verify(jdbcTemplate, Mockito.times(0)).update(anyString());

    // verify "welcome" page is returned
		assertEquals("welcome", pageReturned);
	}


	@Test
	public void testDepositOverDraftBalanceNotCleared() {
		User customer1 = new User();
		customer1.setUsername(CUSTOMER1_USERNAME);
		customer1.setPassword("password");
		customer1.setAmountToDeposit(100); // deposit $100 to pay off part of a $500 overdraft balance

		String getCustomer1PasswordSql = String.format("SELECT Password FROM Passwords WHERE CustomerID='%s';", CUSTOMER1_USERNAME);
		String getCustomer1OverdraftBalanceSql = String.format("SELECT OverdraftBalance FROM Customers WHERE CustomerID='%s';", CUSTOMER1_USERNAME);
		
    // stub jdbc calls
		// successful login
		when(jdbcTemplate.queryForObject(eq(getCustomer1PasswordSql), eq(String.class))).thenReturn("password");
    // handles updateAccountInfo() helper method
    when(jdbcTemplate.queryForList(anyString())).thenReturn(CUSTOMER1_DATA);
    // not working with live DB
		when(jdbcTemplate.update(anyString())).thenReturn(1);
    // handles account not being locked
    String getNumReversals = String.format("SELECT NumFraudReversals FROM Customers WHERE CustomerID='%s';", customer1.getUsername());
    when(jdbcTemplate.queryForObject(eq(getNumReversals), eq(Integer.class))).thenReturn(0);
    // start customer with overdraft balance of $500 (represented as 50000 pennies in the DB)
		when(jdbcTemplate.queryForObject(eq(getCustomer1OverdraftBalanceSql), eq(Integer.class))).thenReturn(50000); 

    // send deposit request
    String pageReturned = controller.submitDeposit(customer1);

    // verify queries for password and overdraft balance
    Mockito.verify(jdbcTemplate, Mockito.times(1)).queryForObject(eq(getCustomer1PasswordSql), eq(String.class));
    Mockito.verify(jdbcTemplate, Mockito.times(1)).queryForObject(eq(getCustomer1OverdraftBalanceSql), eq(Integer.class));

		// overdraft balance > customer deposit, so new overdraft balance must be $400 (represented as 40000 pennies in DB)
    String overDraftBalanceUpdateSql = String.format("UPDATE Customers SET OverdraftBalance = %d WHERE CustomerID='%s';", 40000, CUSTOMER1_USERNAME);
    Mockito.verify(jdbcTemplate, Mockito.times(1)).update(eq(overDraftBalanceUpdateSql));

    // main balance should remain unchanged
    String balanceUpdateSql = String.format("UPDATE Customers SET Balance = Balance + %d WHERE CustomerID='%s';", 0, CUSTOMER1_USERNAME);
    Mockito.verify(jdbcTemplate, Mockito.times(1)).update(eq(balanceUpdateSql));

    // verify "account_info" page is returned
		assertEquals("account_info", pageReturned);
	}

  @Test
	public void testWithdrawLockedAccount() {
		User customer1 = new User();
		customer1.setUsername(CUSTOMER1_USERNAME);
		customer1.setPassword("password");
		customer1.setAmountToWithdraw(100);

    // stub jdbc calls
    String getCustomer1PasswordSql=String.format("SELECT Password FROM Passwords WHERE CustomerID='%s';", customer1.getUsername());
		when(jdbcTemplate.queryForObject(eq(getCustomer1PasswordSql), eq(String.class))).thenReturn("password");
    // handles account being locked
    String getNumReversals = String.format("SELECT NumFraudReversals FROM Customers WHERE CustomerID='%s';", customer1.getUsername());
    when(jdbcTemplate.queryForObject(eq(getNumReversals), eq(Integer.class))).thenReturn(2);
    // send withdraw request
    String pageReturned = controller.submitWithdraw(customer1);

    // Verify that no SQL Update commands are sent
    Mockito.verify(jdbcTemplate, Mockito.times(0)).update(anyString());

    // verify that customer is re-directed to "welcome" page
		assertEquals("welcome", pageReturned);
	}

  @Test
	public void testDepositLockedAccount() {
		User customer1 = new User();
		customer1.setUsername(CUSTOMER1_USERNAME);
		customer1.setPassword("password");
		customer1.setAmountToDeposit(100);

    // stub jdbc calls
    // unsuccessful login
    String getCustomer1PasswordSql=String.format("SELECT Password FROM Passwords WHERE CustomerID='%s';", customer1.getUsername());
		when(jdbcTemplate.queryForObject(eq(getCustomer1PasswordSql), eq(String.class))).thenReturn("password");
    // handles account being locked
    String getNumReversals = String.format("SELECT NumFraudReversals FROM Customers WHERE CustomerID='%s';", customer1.getUsername());
    when(jdbcTemplate.queryForObject(eq(getNumReversals), eq(Integer.class))).thenReturn(2);
    // send deposit request
    String pageReturned = controller.submitDeposit(customer1);

    // Verify that no SQL Update commands are sent
    Mockito.verify(jdbcTemplate, Mockito.times(0)).update(anyString());

    // verify that customer is re-directed to "welcome" page
		assertEquals("welcome", pageReturned);
	}

  @Test
	public void testDepositDisputeSuccess() {
    // initialize user input to the deposit form
		User customer1 = new User();
		customer1.setUsername(CUSTOMER1_USERNAME);
		customer1.setPassword("password");
    //tests reverting most recent deposit
    customer1.setNumTransactionsAgo(1);

    // stub jdbc calls
    // successful login
    String getCustomer1PasswordSql=String.format("SELECT Password FROM Passwords WHERE CustomerID='%s';", customer1.getUsername());
		when(jdbcTemplate.queryForObject(eq(getCustomer1PasswordSql), eq(String.class))).thenReturn("password");
    // no overdraft
    String getCustomer1OverdraftBalanceSql = String.format("SELECT OverdraftBalance FROM Customers WHERE CustomerID='%s';", customer1.getUsername());
    when(jdbcTemplate.queryForObject(eq(getCustomer1OverdraftBalanceSql), eq(Integer.class))).thenReturn(0);
    // has balance of 200
    String getCustomer1BalanceSql=String.format("SELECT Balance FROM Customers WHERE CustomerID='%s';", customer1.getUsername());
    when(jdbcTemplate.queryForObject(eq(getCustomer1BalanceSql), eq(Integer.class))).thenReturn(20000);
    // handles updateAccountInfo() helper method
    String getUserNameAndBalanceAndOverDraftBalanceSql = String.format("SELECT FirstName, LastName, Balance, OverdraftBalance FROM Customers WHERE CustomerID='%s';", customer1.getUsername());
    when(jdbcTemplate.queryForList(eq(getUserNameAndBalanceAndOverDraftBalanceSql))).thenReturn(CUSTOMER1_DATA);
    // handles getting 3 most recent logs from transaction history
    String getTransactionHistorySql = String.format("Select * from TransactionHistory WHERE CustomerId='%s' ORDER BY Timestamp DESC LIMIT %d;", customer1.getUsername(), 3);
    when(jdbcTemplate.queryForList(eq(getTransactionHistorySql))).thenReturn(TRANSACTION_HIST);
    // sends empty overdraft log when fetching overdraft logs for customer that match timestamp of reversed transaction
    String getOverDraftLogsSql = String.format("SELECT * FROM OverdraftLogs WHERE CustomerID='%s' AND Timestamp='%s';", customer1.getUsername(), TRANSACTION_HIST.get(0).get("Timestamp"));
    when(jdbcTemplate.queryForList(eq(getOverDraftLogsSql))).thenReturn(OVERDRAFT_LOGS);
    // handles account not being locked
    String getNumReversals = String.format("SELECT NumFraudReversals FROM Customers WHERE CustomerID='%s';", customer1.getUsername());
    when(jdbcTemplate.queryForObject(eq(getNumReversals), eq(Integer.class))).thenReturn(0);
    // not working with live DB
		when(jdbcTemplate.update(anyString())).thenReturn(1);

    // send dispute request
    String pageReturned = controller.submitDispute(customer1);

    // Verify that the SQL Update command executed uses dispute amount
    int expectedDepositAmtInPennies = (int) (100*100);
    String balanceDecreaseSqlCustomer1=String.format("UPDATE Customers SET Balance = Balance - %d WHERE CustomerID='%s';",
                                                     expectedDepositAmtInPennies,
                                                     customer1.getUsername());
    Mockito.verify(jdbcTemplate, Mockito.times(1)).update(eq(balanceDecreaseSqlCustomer1));

    // verify "account_info" page is returned
		assertEquals("account_info", pageReturned);
	}

  @Test
	public void testDepositCausesOverdraft() {
    // initialize user input to the deposit form
		User customer1 = new User();
		customer1.setUsername(CUSTOMER1_USERNAME);
		customer1.setPassword("password");
    //tests reverting most recent deposit
    customer1.setNumTransactionsAgo(1);

    // stub jdbc calls
    // successful login
    String getCustomer1PasswordSql=String.format("SELECT Password FROM Passwords WHERE CustomerID='%s';", customer1.getUsername());
		when(jdbcTemplate.queryForObject(eq(getCustomer1PasswordSql), eq(String.class))).thenReturn("password");
    // no overdraft
    String getCustomer1OverdraftBalanceSql = String.format("SELECT OverdraftBalance FROM Customers WHERE CustomerID='%s';", customer1.getUsername());
    when(jdbcTemplate.queryForObject(eq(getCustomer1OverdraftBalanceSql), eq(Integer.class))).thenReturn(0);
    // has balance of 0
    String getCustomer1BalanceSql=String.format("SELECT Balance FROM Customers WHERE CustomerID='%s';", customer1.getUsername());
    when(jdbcTemplate.queryForObject(eq(getCustomer1BalanceSql), eq(Integer.class))).thenReturn(0);
    // handles updateAccountInfo() helper method
    String getUserNameAndBalanceAndOverDraftBalanceSql = String.format("SELECT FirstName, LastName, Balance, OverdraftBalance FROM Customers WHERE CustomerID='%s';", customer1.getUsername());
    when(jdbcTemplate.queryForList(eq(getUserNameAndBalanceAndOverDraftBalanceSql))).thenReturn(CUSTOMER1_DATA);
    // handles getting 3 most recent logs from transaction history
    String getTransactionHistorySql = String.format("Select * from TransactionHistory WHERE CustomerId='%s' ORDER BY Timestamp DESC LIMIT %d;", customer1.getUsername(), 3);
    when(jdbcTemplate.queryForList(eq(getTransactionHistorySql))).thenReturn(TRANSACTION_HIST);
    // sends empty overdraft log when fetching overdraft logs for customer that match timestamp of reversed transaction
    String getOverDraftLogsSql = String.format("SELECT * FROM OverdraftLogs WHERE CustomerID='%s' AND Timestamp='%s';", customer1.getUsername(), TRANSACTION_HIST.get(0).get("Timestamp"));
    when(jdbcTemplate.queryForList(eq(getOverDraftLogsSql))).thenReturn(OVERDRAFT_LOGS);
    // handles account not being locked
    String getNumReversals = String.format("SELECT NumFraudReversals FROM Customers WHERE CustomerID='%s';", customer1.getUsername());
    when(jdbcTemplate.queryForObject(eq(getNumReversals), eq(Integer.class))).thenReturn(0);
    // not working with live DB
		when(jdbcTemplate.update(anyString())).thenReturn(1);

    // send dispute request
    String pageReturned = controller.submitDispute(customer1);

    // Verify that the SQL Update command executed sets balance to 0
    String balanceZeroSqlCustomer1=String.format("UPDATE Customers SET Balance = 0 WHERE CustomerID='%s';",
                                                     customer1.getUsername());
    Mockito.verify(jdbcTemplate, Mockito.times(1)).update(eq(balanceZeroSqlCustomer1));
    //makes sure overdraft balance is increased by 10000*1.02 (the tax)
    String overdraftBalanceUpdateSql = String.format("UPDATE Customers SET OverdraftBalance = OverdraftBalance + 10200 WHERE CustomerID='%s';", customer1.getUsername());
    Mockito.verify(jdbcTemplate, Mockito.times(1)).update(eq(overdraftBalanceUpdateSql));

    // verify "account_info" page is returned
		assertEquals("account_info", pageReturned);
	}

  @Test
	public void testWithdrawDisputeSuccess() {
    // initialize user input to the deposit form
		User customer1 = new User();
		customer1.setUsername(CUSTOMER1_USERNAME);
		customer1.setPassword("password");
    //tests reverting most recent deposit
    customer1.setNumTransactionsAgo(1);

    // stub jdbc calls
    // successful login
    String getCustomer1PasswordSql=String.format("SELECT Password FROM Passwords WHERE CustomerID='%s';", customer1.getUsername());
		when(jdbcTemplate.queryForObject(eq(getCustomer1PasswordSql), eq(String.class))).thenReturn("password");
    // no overdraft
    String getCustomer1OverdraftBalanceSql = String.format("SELECT OverdraftBalance FROM Customers WHERE CustomerID='%s';", customer1.getUsername());
    when(jdbcTemplate.queryForObject(eq(getCustomer1OverdraftBalanceSql), eq(Integer.class))).thenReturn(0);
    // has balance of 200
    String getCustomer1BalanceSql=String.format("SELECT Balance FROM Customers WHERE CustomerID='%s';", customer1.getUsername());
    when(jdbcTemplate.queryForObject(eq(getCustomer1BalanceSql), eq(Integer.class))).thenReturn(20000);
    // handles updateAccountInfo() helper method
    String getUserNameAndBalanceAndOverDraftBalanceSql = String.format("SELECT FirstName, LastName, Balance, OverdraftBalance FROM Customers WHERE CustomerID='%s';", customer1.getUsername());
    when(jdbcTemplate.queryForList(eq(getUserNameAndBalanceAndOverDraftBalanceSql))).thenReturn(CUSTOMER1_DATA);
    // handles getting 3 most recent logs from transaction history
    String getTransactionHistorySql = String.format("Select * from TransactionHistory WHERE CustomerId='%s' ORDER BY Timestamp DESC LIMIT %d;", customer1.getUsername(), 3);
    when(jdbcTemplate.queryForList(eq(getTransactionHistorySql))).thenReturn(TRANSACTION_HIST_WITHDRAW);
    // sends empty overdraft log when fetching overdraft logs for customer that match timestamp of reversed transaction
    String getOverDraftLogsSql = String.format("SELECT * FROM OverdraftLogs WHERE CustomerID='%s' AND Timestamp='%s';", customer1.getUsername(), TRANSACTION_HIST.get(0).get("Timestamp"));
    when(jdbcTemplate.queryForList(eq(getOverDraftLogsSql))).thenReturn(OVERDRAFT_LOGS);
    // handles account not being locked
    String getNumReversals = String.format("SELECT NumFraudReversals FROM Customers WHERE CustomerID='%s';", customer1.getUsername());
    when(jdbcTemplate.queryForObject(eq(getNumReversals), eq(Integer.class))).thenReturn(0);
    // not working with live DB
		when(jdbcTemplate.update(anyString())).thenReturn(1);

    // send dispute request
    String pageReturned = controller.submitDispute(customer1);

    // Verify that the SQL Update command executed uses dispute amount
    int expectedDepositAmtInPennies = (int) (100*100);
    String balanceDecreaseSqlCustomer1=String.format("UPDATE Customers SET Balance = Balance + %d WHERE CustomerID='%s';",
                                                     expectedDepositAmtInPennies,
                                                     customer1.getUsername());
    Mockito.verify(jdbcTemplate, Mockito.times(1)).update(eq(balanceDecreaseSqlCustomer1));

    // verify "account_info" page is returned
		assertEquals("account_info", pageReturned);
	}

  @Test
	public void testWithdrawDisputeInOverdraft() {
    // initialize user input to the deposit form
		User customer1 = new User();
		customer1.setUsername(CUSTOMER1_USERNAME);
		customer1.setPassword("password");
    //tests reverting most recent deposit
    customer1.setNumTransactionsAgo(1);

    // stub jdbc calls
    // successful login
    String getCustomer1PasswordSql=String.format("SELECT Password FROM Passwords WHERE CustomerID='%s';", customer1.getUsername());
		when(jdbcTemplate.queryForObject(eq(getCustomer1PasswordSql), eq(String.class))).thenReturn("password");
    // no overdraft
    String getCustomer1OverdraftBalanceSql = String.format("SELECT OverdraftBalance FROM Customers WHERE CustomerID='%s';", customer1.getUsername());
    when(jdbcTemplate.queryForObject(eq(getCustomer1OverdraftBalanceSql), eq(Integer.class))).thenReturn(5000);
    // has balance of 200
    String getCustomer1BalanceSql=String.format("SELECT Balance FROM Customers WHERE CustomerID='%s';", customer1.getUsername());
    when(jdbcTemplate.queryForObject(eq(getCustomer1BalanceSql), eq(Integer.class))).thenReturn(0);
    // handles updateAccountInfo() helper method
    String getUserNameAndBalanceAndOverDraftBalanceSql = String.format("SELECT FirstName, LastName, Balance, OverdraftBalance FROM Customers WHERE CustomerID='%s';", customer1.getUsername());
    when(jdbcTemplate.queryForList(eq(getUserNameAndBalanceAndOverDraftBalanceSql))).thenReturn(CUSTOMER1_DATA);
    // handles getting 3 most recent logs from transaction history
    String getTransactionHistorySql = String.format("Select * from TransactionHistory WHERE CustomerId='%s' ORDER BY Timestamp DESC LIMIT %d;", customer1.getUsername(), 3);
    when(jdbcTemplate.queryForList(eq(getTransactionHistorySql))).thenReturn(TRANSACTION_HIST_WITHDRAW);
    // sends empty overdraft log when fetching overdraft logs for customer that match timestamp of reversed transaction
    String getOverDraftLogsSql = String.format("SELECT * FROM OverdraftLogs WHERE CustomerID='%s' AND Timestamp='%s';", customer1.getUsername(), TRANSACTION_HIST.get(0).get("Timestamp"));
    when(jdbcTemplate.queryForList(eq(getOverDraftLogsSql))).thenReturn(OVERDRAFT_LOGS);
    // handles account not being locked
    String getNumReversals = String.format("SELECT NumFraudReversals FROM Customers WHERE CustomerID='%s';", customer1.getUsername());
    when(jdbcTemplate.queryForObject(eq(getNumReversals), eq(Integer.class))).thenReturn(0);
    // not working with live DB
		when(jdbcTemplate.update(anyString())).thenReturn(1);

    // send dispute request
    String pageReturned = controller.submitDispute(customer1);

    // Verify that the SQL Update command executed uses dispute amount. 10000 is withdraw amount and
    // 5000 is amount in overdraft so when depositing 10000 5000 should go into balance
    int expectedDepositAmtInPennies = (100*100) - (50*100);
    String balanceIncreaseSqlCustomer1=String.format("UPDATE Customers SET Balance = Balance + %d WHERE CustomerID='%s';",
                                                     expectedDepositAmtInPennies,
                                                     customer1.getUsername());
    Mockito.verify(jdbcTemplate, Mockito.times(1)).update(eq(balanceIncreaseSqlCustomer1));
    // as overdraft is completly paid off it should be set to 0
    String overdraftBalanceUpdateSql = String.format("UPDATE Customers SET OverdraftBalance = 0 WHERE CustomerID='%s';", customer1.getUsername());
    Mockito.verify(jdbcTemplate, Mockito.times(1)).update(eq(overdraftBalanceUpdateSql));
    // verify "account_info" page is returned
		assertEquals("account_info", pageReturned);
	}
}
