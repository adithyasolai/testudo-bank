package net.codejava;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.script.ScriptException;
import javax.sql.DataSource;

import com.mysql.cj.jdbc.MysqlDataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.delegate.DatabaseDelegate;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
public class MvcControllerIntegTest {
  // Spins up small MySQL DB in local Docker container
  @Container
  public static MySQLContainer db = new MySQLContainer<>("mysql:5.5")
    .withUsername("root")
    .withPassword("db_password")
    .withDatabaseName("testudo_bank");

  private static MvcController controller;
  private static JdbcTemplate jdbcTemplate;
  private static DatabaseDelegate dbDelegate;

  private static String CUSTOMER1_ID = "123456789";
  private static String CUSTOMER1_PASSWORD = "password";
  private static String CUSTOMER1_FIRST_NAME = "Foo";
  private static String CUSTOMER1_LAST_NAME = "Bar";
  private static String TRANSACTION_HISTORY_DEPOSIT_ACTION = "Deposit";
  private static String TRANSACTION_HISTORY_WITHDRAW_ACTION = "Withdraw";
  private static long REASONABLE_TIMESTAMP_EPSILON_IN_SECONDS = 1L;

  @BeforeAll
  public static void init() throws SQLException {
    dbDelegate = new JdbcDatabaseDelegate(db, "");
    ScriptUtils.runInitScript(dbDelegate, "createDB.sql");
    jdbcTemplate = new JdbcTemplate(dataSource());
    jdbcTemplate.getDataSource().getConnection().setCatalog(db.getDatabaseName());
    controller = new MvcController(jdbcTemplate);
  }

  @AfterEach
  public void clearDB() throws ScriptException {
    // runInitScript() pulls all the String text from the SQL file and just calls executeDatabaseScript(),
    // so it is OK to use runInitScript() again even though we aren't initializing the DB for the first time here.
    // runInitScript() is a poorly-named function.
    ScriptUtils.runInitScript(dbDelegate, "clearDB.sql");
  }

  //// HELPER FUNCTIONS ////

  // Fetches DB credentials to initialize jdbcTemplate client
  private static DataSource dataSource() {
    MysqlDataSource dataSource = new MysqlDataSource();
    dataSource.setUrl(db.getJdbcUrl());
    dataSource.setUser(db.getUsername());
    dataSource.setPassword(db.getPassword());
    return dataSource;
  }

  // Uses given customer details to initialize the customer in the Customers and Passwords table in the MySQL DB.
  private static void addCustomerToDB(String ID, String password, String firstName, String lastName, int balance, int overdraftBalance, int numFraudReversals) throws ScriptException {
    String insertCustomerSql = String.format("INSERT INTO Customers VALUES ('%s', '%s', '%s', %d, %d, %d)", ID, firstName, lastName, balance, overdraftBalance, numFraudReversals);
    ScriptUtils.executeDatabaseScript(dbDelegate, null, insertCustomerSql);

    String insertCustomerPasswordSql = String.format("INSERT INTO Passwords VALUES ('%s', '%s')", ID, password);
    ScriptUtils.executeDatabaseScript(dbDelegate, null, insertCustomerPasswordSql);
  }

  // Adds a customer to the MySQL DB with no overdraft balance or fraud disputes
  private static void addCustomerToDB(String ID, String password, String firstName, String lastName, int balance) throws ScriptException {
    addCustomerToDB(ID, password, firstName, lastName, balance, 0, 0);
  }

  // Verifies that a single transaction log in the TransactionHistory table matches the expected customerID, timestamp, action, and amount
  private static void checkTransactionLog(Map<String,Object> transactionLog, LocalDateTime timeWhenRequestSent, String expectedCustomerID, String expectedAction, int expectedAmountInPennies) {
    assertEquals(expectedCustomerID, (String)transactionLog.get("CustomerID"));
    assertEquals(expectedAction, (String)transactionLog.get("Action"));
    assertEquals(expectedAmountInPennies, (int)transactionLog.get("Amount"));
    // verify that the timestamp for the Deposit is within a reasonable range from when the request was first sent
    LocalDateTime transactionLogTimestamp = (LocalDateTime)transactionLog.get("Timestamp");
    LocalDateTime transactionLogTimestampAllowedUpperBound = timeWhenRequestSent.plusSeconds(REASONABLE_TIMESTAMP_EPSILON_IN_SECONDS);
    assertTrue(transactionLogTimestamp.compareTo(timeWhenRequestSent) >= 0 && transactionLogTimestamp.compareTo(transactionLogTimestampAllowedUpperBound) <= 0);
    System.out.println("Timestamp stored in TransactionHistory table for the request: " + transactionLogTimestamp);
  }

  // Verifies that a single overdraft repayment log in the OverdraftLogs table matches the expected customerID, timestamp, depositAmt, oldOverBalance, and newOverBalance
  private static void checkOverdraftLog(Map<String,Object> overdraftLog, LocalDateTime timeWhenRequestSent, String expectedCustomerID, int expectedDepositAmtInPennies, int expectedOldOverBalanceInPennies, int expectedNewOverBalanceInPennies) {
    assertEquals(expectedCustomerID, (String)overdraftLog.get("CustomerID"));
    assertEquals(expectedDepositAmtInPennies, (int)overdraftLog.get("DepositAmt"));
    assertEquals(expectedOldOverBalanceInPennies, (int)overdraftLog.get("OldOverBalance"));
    assertEquals(expectedNewOverBalanceInPennies, (int)overdraftLog.get("NewOverBalance"));
    // verify that the timestamp for the overdraft repayement is within a reasonable range from when the Deposit request was first sent
    LocalDateTime overdraftLogTimestamp = (LocalDateTime)overdraftLog.get("Timestamp");
    LocalDateTime overdraftLogTimestampAllowedUpperBound = timeWhenRequestSent.plusSeconds(REASONABLE_TIMESTAMP_EPSILON_IN_SECONDS);
    assertTrue(overdraftLogTimestamp.compareTo(timeWhenRequestSent) >= 0 && overdraftLogTimestamp.compareTo(overdraftLogTimestampAllowedUpperBound) <= 0);
    System.out.println("Timestamp stored in OverdraftLogs table for the Repayment: " + overdraftLogTimestamp);
  }

  // Converts dollar amounts in frontend to penny representation in backend MySQL DB
  private static int convertDollarsToPennies(double dollarAmount) {
    return (int) (dollarAmount * 100);
  }

  // Fetches current local time with no milliseconds because the MySQL DB has granularity only up to seconds (does not use milliseconds)
  private static LocalDateTime fetchCurrentTimeAsLocalDateTimeNoMilliseconds() {
    LocalDateTime currentTimeAsLocalDateTime = convertDateToLocalDateTime(new java.util.Date());
    currentTimeAsLocalDateTime = currentTimeAsLocalDateTime.truncatedTo(ChronoUnit.SECONDS);
    return currentTimeAsLocalDateTime;
  }

  // Converts the java.util.Date object into the LocalDateTime returned by the MySQL DB
  private static LocalDateTime convertDateToLocalDateTime(Date dateToConvert) { 
    return dateToConvert.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
  }

  //// INTEGRATION TESTS ////

  /**
   * Verifies the simplest deposit case.
   * The customer's Balance in the Customers table should be increased,
   * and the Deposit should be logged in the TransactionHistory table.
   * 
   * Assumes that the customer's account is in the simplest state
   * (not in overdraft, account is not frozen due to too many transaction disputes, etc.)
   * 
   * @throws SQLException
   * @throws ScriptException
   */
  @Test
  public void testSimpleDeposit() throws SQLException, ScriptException {
    // initialize customer1 with a balance of $150. represented as pennies in the DB.
    double CUSTOMER1_BALANCE = 150;
    int CUSTOMER1_BALANCE_IN_PENNIES = convertDollarsToPennies(CUSTOMER1_BALANCE);
    addCustomerToDB(CUSTOMER1_ID, CUSTOMER1_PASSWORD, CUSTOMER1_FIRST_NAME, CUSTOMER1_LAST_NAME, CUSTOMER1_BALANCE_IN_PENNIES);

    // Prepare Deposit Form to Deposit $50 to customer 1's account.
    double CUSTOMER1_AMOUNT_TO_DEPOSIT = 50; // user input is in dollar amount, not pennies.
    User customer1DepositFormInputs = new User();
    customer1DepositFormInputs.setUsername(CUSTOMER1_ID);
    customer1DepositFormInputs.setPassword(CUSTOMER1_PASSWORD);
    customer1DepositFormInputs.setAmountToDeposit(CUSTOMER1_AMOUNT_TO_DEPOSIT); 

    // verify that there are no logs in TransactionHistory table before Deposit
    assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TransactionHistory;", Integer.class));

    // store timestamp of when Deposit request is sent to verify timestamps in the TransactionHistory table later
    LocalDateTime timeWhenDepositRequestSent = fetchCurrentTimeAsLocalDateTimeNoMilliseconds();
    System.out.println("Timestamp when Deposit Request is sent: " + timeWhenDepositRequestSent);

    // send request to the Deposit Form's POST handler in MvcController
    controller.submitDeposit(customer1DepositFormInputs);

    // fetch updated data from the DB
    List<Map<String,Object>> customersTableData = jdbcTemplate.queryForList("SELECT * FROM Customers;");
    List<Map<String,Object>> transactionHistoryTableData = jdbcTemplate.queryForList("SELECT * FROM TransactionHistory;");
  
    // verify that customer1's data is still the only data populated in Customers table
    assertEquals(1, customersTableData.size());
    Map<String,Object> customer1Data = customersTableData.get(0);
    assertEquals(CUSTOMER1_ID, (String)customer1Data.get("CustomerID"));

    // verify customer balance was increased by $50
    double CUSTOMER1_EXPECTED_FINAL_BALANCE = 200;
    double CUSTOMER1_EXPECTED_FINAL_BALANCE_IN_PENNIES = convertDollarsToPennies(CUSTOMER1_EXPECTED_FINAL_BALANCE);
    assertEquals(CUSTOMER1_EXPECTED_FINAL_BALANCE_IN_PENNIES, (int)customer1Data.get("Balance"));

    // verify that the Deposit is the only log in TransactionHistory table
    assertEquals(1, transactionHistoryTableData.size());
    
    // verify that the Deposit's details are accurately logged in the TransactionHistory table
    Map<String,Object> customer1TransactionLog = transactionHistoryTableData.get(0);
    int CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES = convertDollarsToPennies(CUSTOMER1_AMOUNT_TO_DEPOSIT);
    checkTransactionLog(customer1TransactionLog, timeWhenDepositRequestSent, CUSTOMER1_ID, TRANSACTION_HISTORY_DEPOSIT_ACTION, CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES);
  }

  /**
   * Verifies the simplest withdraw case.
   * The customer's Balance in the Customers table should be decreased,
   * and the Withdraw should be logged in the TransactionHistory table.
   * 
   * Assumes that the customer's account is in the simplest state
   * (not already in overdraft, the withdraw does not put customer in overdraft,
   *  account is not frozen due to too many transaction disputes, etc.)
   * 
   * @throws SQLException
   * @throws ScriptException
   */
  @Test
  public void testSimpleWithdraw() throws SQLException, ScriptException {
    // initialize customer1 with a balance of $150. represented as pennies in the DB.
    double CUSTOMER1_BALANCE = 150;
    int CUSTOMER1_BALANCE_IN_PENNIES = convertDollarsToPennies(CUSTOMER1_BALANCE);
    addCustomerToDB(CUSTOMER1_ID, CUSTOMER1_PASSWORD, CUSTOMER1_FIRST_NAME, CUSTOMER1_LAST_NAME, CUSTOMER1_BALANCE_IN_PENNIES);

    // Prepare Withdraw Form to Withdraw $50 from customer 1's account.
    double CUSTOMER1_AMOUNT_TO_WITHDRAW = 50; // user input is in dollar amount, not pennies.
    User customer1WithdrawFormInputs = new User();
    customer1WithdrawFormInputs.setUsername(CUSTOMER1_ID);
    customer1WithdrawFormInputs.setPassword(CUSTOMER1_PASSWORD);
    customer1WithdrawFormInputs.setAmountToWithdraw(CUSTOMER1_AMOUNT_TO_WITHDRAW); // user input is in dollar amount, not pennies.

    // verify that there are no logs in TransactionHistory table before Withdraw
    assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TransactionHistory;", Integer.class));

    // store timestamp of when Withdraw request is sent to verify timestamps in the TransactionHistory table later
    LocalDateTime timeWhenWithdrawRequestSent = fetchCurrentTimeAsLocalDateTimeNoMilliseconds();
    System.out.println("Timestamp when Withdraw Request is sent: " + timeWhenWithdrawRequestSent);

    // send request to the Withdraw Form's POST handler in MvcController
    controller.submitWithdraw(customer1WithdrawFormInputs);

    // fetch updated data from the DB
    List<Map<String,Object>> customersTableData = jdbcTemplate.queryForList("SELECT * FROM Customers;");
    List<Map<String,Object>> transactionHistoryTableData = jdbcTemplate.queryForList("SELECT * FROM TransactionHistory;");
  
    // verify that customer1's data is still the only data populated in Customers table
    assertEquals(1, customersTableData.size());
    Map<String,Object> customer1Data = customersTableData.get(0);
    assertEquals(CUSTOMER1_ID, (String)customer1Data.get("CustomerID"));

    // verify customer balance was decreased by $50
    double CUSTOMER1_EXPECTED_FINAL_BALANCE = 100;
    double CUSTOMER1_EXPECTED_FINAL_BALANCE_IN_PENNIES = convertDollarsToPennies(CUSTOMER1_EXPECTED_FINAL_BALANCE);
    assertEquals(CUSTOMER1_EXPECTED_FINAL_BALANCE_IN_PENNIES, (int)customer1Data.get("Balance"));

    // verify that the Withdraw is the only log in TransactionHistory table
    assertEquals(1, transactionHistoryTableData.size());

    // verify that the Withdraw's details are accurately logged in the TransactionHistory table
    Map<String,Object> customer1TransactionLog = transactionHistoryTableData.get(0);
    int CUSTOMER1_AMOUNT_TO_WITHDRAW_IN_PENNIES = convertDollarsToPennies(CUSTOMER1_AMOUNT_TO_WITHDRAW);
    checkTransactionLog(customer1TransactionLog, timeWhenWithdrawRequestSent, CUSTOMER1_ID, TRANSACTION_HISTORY_WITHDRAW_ACTION, CUSTOMER1_AMOUNT_TO_WITHDRAW_IN_PENNIES);
  }

  /**
   * Verifies the case where a customer withdraws more than their available balance.
   * The customer's main balance should be set to $0, and their Overdraft balance
   * should be the remaining withdraw amount with interest applied.
   * 
   * This Withdraw should still be recorded in the TransactionHistory table.
   * 
   * A few Assertions are omitted to remove clutter since they are already
   * checked in detail in testSimpleWithdraw().
   * 
   * @throws SQLException
   * @throws ScriptException
   */
  @Test
  public void testWithdrawTriggersOverdraft() throws SQLException, ScriptException {
    // initialize customer1 with a balance of $100. represented as pennies in the DB.
    double CUSTOMER1_BALANCE = 100;
    int CUSTOMER1_BALANCE_IN_PENNIES = convertDollarsToPennies(CUSTOMER1_BALANCE);
    addCustomerToDB(CUSTOMER1_ID, CUSTOMER1_PASSWORD, CUSTOMER1_FIRST_NAME, CUSTOMER1_LAST_NAME, CUSTOMER1_BALANCE_IN_PENNIES);

    // Prepare Withdraw Form to Withdraw $110 from customer 1's account.
    double CUSTOMER1_AMOUNT_TO_WITHDRAW = 110; // user input is in dollar amount, not pennies.
    User customer1WithdrawFormInputs = new User();
    customer1WithdrawFormInputs.setUsername(CUSTOMER1_ID);
    customer1WithdrawFormInputs.setPassword(CUSTOMER1_PASSWORD);
    customer1WithdrawFormInputs.setAmountToWithdraw(CUSTOMER1_AMOUNT_TO_WITHDRAW); // user input is in dollar amount, not pennies.

    // store timestamp of when Withdraw request is sent to verify timestamps in the TransactionHistory table later
    LocalDateTime timeWhenWithdrawRequestSent = fetchCurrentTimeAsLocalDateTimeNoMilliseconds();
    System.out.println("Timestamp when Withdraw Request is sent: " + timeWhenWithdrawRequestSent);

    // send request to the Withdraw Form's POST handler in MvcController
    controller.submitWithdraw(customer1WithdrawFormInputs);

    // fetch updated customer1 data from the DB
    List<Map<String,Object>> customersTableData = jdbcTemplate.queryForList("SELECT * FROM Customers;");
    List<Map<String,Object>> transactionHistoryTableData = jdbcTemplate.queryForList("SELECT * FROM TransactionHistory;");
    
    // verify that customer1's main balance is now 0
    Map<String,Object> customer1Data = customersTableData.get(0);
    assertEquals(0, (int)customer1Data.get("Balance"));

    // verify that customer1's Overdraft balance is equal to the remaining withdraw amount with interest applied
    // (convert to pennies before applying interest rate to avoid floating point roundoff errors when applying the interest rate)
    int CUSTOMER1_ORIGINAL_BALANCE_IN_PENNIES = convertDollarsToPennies(CUSTOMER1_BALANCE);
    int CUSTOMER1_AMOUNT_TO_WITHDRAW_IN_PENNIES = convertDollarsToPennies(CUSTOMER1_AMOUNT_TO_WITHDRAW);
    int CUSTOMER1_EXPECTED_OVERDRAFT_BALANCE_BEFORE_INTEREST_IN_PENNIES = CUSTOMER1_AMOUNT_TO_WITHDRAW_IN_PENNIES - CUSTOMER1_ORIGINAL_BALANCE_IN_PENNIES;
    int CUSTOMER1_EXPECTED_OVERDRAFT_BALANCE_AFTER_INTEREST_IN_PENNIES = (int)(CUSTOMER1_EXPECTED_OVERDRAFT_BALANCE_BEFORE_INTEREST_IN_PENNIES * MvcController.INTEREST_RATE);
    System.out.println("Expected Overdraft Balance in pennies: " + CUSTOMER1_EXPECTED_OVERDRAFT_BALANCE_AFTER_INTEREST_IN_PENNIES);
    assertEquals(CUSTOMER1_EXPECTED_OVERDRAFT_BALANCE_AFTER_INTEREST_IN_PENNIES, (int)customer1Data.get("OverdraftBalance"));

    // verify that the Withdraw's details are accurately logged in the TransactionHistory table
    Map<String,Object> customer1TransactionLog = transactionHistoryTableData.get(0);
    checkTransactionLog(customer1TransactionLog, timeWhenWithdrawRequestSent, CUSTOMER1_ID, TRANSACTION_HISTORY_WITHDRAW_ACTION, CUSTOMER1_AMOUNT_TO_WITHDRAW_IN_PENNIES);
  }

  /**
   * Verifies the case where a customer is in overdraft and deposits an amount
   * that exceeds their overdraft balance. The customer's OverdraftBalance
   * in the Customers table should be set to $0, and their main Balance
   * should be set to the excess deposit amount.
   * 
   * This Deposit should be logged in the OverdraftLogs table since it is a repayment.
   * 
   * This Deposit should still be recorded in the TransactionHistory table.
   * 
   * A few Assertions are omitted to remove clutter since they are already
   * checked in detail in testSimpleDeposit().
   * 
   * @throws SQLException
   * @throws ScriptException
   */
  @Test
  public void testDepositOverdraftClearedWithExcess() throws SQLException, ScriptException {
    // initialize customer1 with an overdraft balance of $100. represented as pennies in the DB.
    int CUSTOMER1_MAIN_BALANCE_IN_PENNIES = 0;
    double CUSTOMER1_OVERDRAFT_BALANCE = 100;
    int CUSTOMER1_OVERDRAFT_BALANCE_IN_PENNIES = convertDollarsToPennies(CUSTOMER1_OVERDRAFT_BALANCE);
    int CUSTOMER1_NUM_FRAUD_REVERSALS = 0;
    addCustomerToDB(CUSTOMER1_ID, CUSTOMER1_PASSWORD, CUSTOMER1_FIRST_NAME, CUSTOMER1_LAST_NAME, CUSTOMER1_MAIN_BALANCE_IN_PENNIES, CUSTOMER1_OVERDRAFT_BALANCE_IN_PENNIES, CUSTOMER1_NUM_FRAUD_REVERSALS);

    // Prepare Deposit Form to Deposit $150 to customer 1's account.
    double CUSTOMER1_AMOUNT_TO_DEPOSIT = 150; // user input is in dollar amount, not pennies.
    User customer1DepositFormInputs = new User();
    customer1DepositFormInputs.setUsername(CUSTOMER1_ID);
    customer1DepositFormInputs.setPassword(CUSTOMER1_PASSWORD);
    customer1DepositFormInputs.setAmountToDeposit(CUSTOMER1_AMOUNT_TO_DEPOSIT); 

    // store timestamp of when Deposit request is sent to verify timestamps in the TransactionHistory and OverdraftLogs tables later
    LocalDateTime timeWhenDepositRequestSent = fetchCurrentTimeAsLocalDateTimeNoMilliseconds();
    System.out.println("Timestamp when Deposit Request is sent: " + timeWhenDepositRequestSent);

    // send request to the Deposit Form's POST handler in MvcController
    controller.submitDeposit(customer1DepositFormInputs);

    // fetch updated data from the DB
    List<Map<String,Object>> customersTableData = jdbcTemplate.queryForList("SELECT * FROM Customers;");
    List<Map<String,Object>> transactionHistoryTableData = jdbcTemplate.queryForList("SELECT * FROM TransactionHistory;");
    List<Map<String,Object>> overdraftLogsTableData = jdbcTemplate.queryForList("SELECT * FROM OverdraftLogs;");

    // verify that customer's overdraft balance is now $0
    Map<String,Object> customer1Data = customersTableData.get(0);
    assertEquals(0, (int)customer1Data.get("OverdraftBalance"));

    // verify that the customer's main balance is now $50 due to the excess deposit amount
    int CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES = convertDollarsToPennies(CUSTOMER1_AMOUNT_TO_DEPOSIT);
    int CUSTOMER1_ORIGINAL_OVERDRAFT_BALANCE_IN_PENNIES = CUSTOMER1_OVERDRAFT_BALANCE_IN_PENNIES;
    int CUSTOMER1_EXPECTED_MAIN_BALANCE_IN_PENNIES = CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES - CUSTOMER1_ORIGINAL_OVERDRAFT_BALANCE_IN_PENNIES;
    assertEquals(CUSTOMER1_EXPECTED_MAIN_BALANCE_IN_PENNIES, (int)customer1Data.get("Balance"));

    // verify that the deposit is logged properly in the OverdraftLogs table
    Map<String,Object> customer1OverdraftLog = overdraftLogsTableData.get(0);
    checkOverdraftLog(customer1OverdraftLog, timeWhenDepositRequestSent, CUSTOMER1_ID, CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES, CUSTOMER1_ORIGINAL_OVERDRAFT_BALANCE_IN_PENNIES, 0);

    // verify that the Deposit's details are accurately logged in the TransactionHistory table
    Map<String,Object> customer1TransactionLog = transactionHistoryTableData.get(0);
    checkTransactionLog(customer1TransactionLog, timeWhenDepositRequestSent, CUSTOMER1_ID, TRANSACTION_HISTORY_DEPOSIT_ACTION, CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES);
  }

  /**
   * Verifies the case where a customer is in overdraft and deposits an amount
   * that still leaves some leftover Overdraft balance. The customer's OverdraftBalance
   * in the Customers table should be set to $0, and their main Balance
   * should still be $0 in the MySQL DB.
   * 
   * This Deposit should be logged in the OverdraftLogs table since it is a repayment.
   * 
   * This Deposit should still be recorded in the TransactionHistory table.
   * 
   * A few Assertions are omitted to remove clutter since they are already
   * checked in detail in testSimpleDeposit().
   * 
   * @throws SQLException
   * @throws ScriptException
   */
  @Test
  public void testDepositOverdraftNotCleared() throws SQLException, ScriptException {
    // initialize customer1 with an overdraft balance of $150. represented as pennies in the DB.
    int CUSTOMER1_MAIN_BALANCE_IN_PENNIES = 0;
    double CUSTOMER1_OVERDRAFT_BALANCE = 150;
    int CUSTOMER1_OVERDRAFT_BALANCE_IN_PENNIES = convertDollarsToPennies(CUSTOMER1_OVERDRAFT_BALANCE);
    int CUSTOMER1_NUM_FRAUD_REVERSALS = 0;
    addCustomerToDB(CUSTOMER1_ID, CUSTOMER1_PASSWORD, CUSTOMER1_FIRST_NAME, CUSTOMER1_LAST_NAME, CUSTOMER1_MAIN_BALANCE_IN_PENNIES, CUSTOMER1_OVERDRAFT_BALANCE_IN_PENNIES, CUSTOMER1_NUM_FRAUD_REVERSALS);

    // Prepare Deposit Form to Deposit $50 to customer 1's account.
    double CUSTOMER1_AMOUNT_TO_DEPOSIT = 50; // user input is in dollar amount, not pennies.
    User customer1DepositFormInputs = new User();
    customer1DepositFormInputs.setUsername(CUSTOMER1_ID);
    customer1DepositFormInputs.setPassword(CUSTOMER1_PASSWORD);
    customer1DepositFormInputs.setAmountToDeposit(CUSTOMER1_AMOUNT_TO_DEPOSIT); 

    // store timestamp of when Deposit request is sent to verify timestamps in the TransactionHistory and OverdraftLogs tables later
    LocalDateTime timeWhenDepositRequestSent = fetchCurrentTimeAsLocalDateTimeNoMilliseconds();
    System.out.println("Timestamp when Deposit Request is sent: " + timeWhenDepositRequestSent);

    // send request to the Deposit Form's POST handler in MvcController
    controller.submitDeposit(customer1DepositFormInputs);

    // fetch updated data from the DB
    List<Map<String,Object>> customersTableData = jdbcTemplate.queryForList("SELECT * FROM Customers;");
    List<Map<String,Object>> transactionHistoryTableData = jdbcTemplate.queryForList("SELECT * FROM TransactionHistory;");
    List<Map<String,Object>> overdraftLogsTableData = jdbcTemplate.queryForList("SELECT * FROM OverdraftLogs;");

    // verify that customer's overdraft balance is now $100
    Map<String,Object> customer1Data = customersTableData.get(0);
    int CUSTOMER1_ORIGINAL_OVERDRAFT_BALANCE_IN_PENNIES = CUSTOMER1_OVERDRAFT_BALANCE_IN_PENNIES;
    int CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES = convertDollarsToPennies(CUSTOMER1_AMOUNT_TO_DEPOSIT);
    int CUSTOMER1_EXPECTED_FINAL_OVERDRAFT_BALANCE_IN_PENNIES = CUSTOMER1_ORIGINAL_OVERDRAFT_BALANCE_IN_PENNIES - CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES;
    assertEquals(CUSTOMER1_EXPECTED_FINAL_OVERDRAFT_BALANCE_IN_PENNIES, (int)customer1Data.get("OverdraftBalance"));

    // verify that the customer's main balance is still $0
    int CUSTOMER1_EXPECTED_MAIN_BALANCE_IN_PENNIES = 0;
    assertEquals(CUSTOMER1_EXPECTED_MAIN_BALANCE_IN_PENNIES, (int)customer1Data.get("Balance"));

    // verify that the deposit is logged properly in the OverdraftLogs table
    Map<String,Object> customer1OverdraftLog = overdraftLogsTableData.get(0);
    checkOverdraftLog(customer1OverdraftLog, timeWhenDepositRequestSent, CUSTOMER1_ID, CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES, CUSTOMER1_ORIGINAL_OVERDRAFT_BALANCE_IN_PENNIES, CUSTOMER1_EXPECTED_FINAL_OVERDRAFT_BALANCE_IN_PENNIES);

    // verify that the Withdraw's details are accurately logged in the TransactionHistory table
    Map<String,Object> customer1TransactionLog = transactionHistoryTableData.get(0);
    checkTransactionLog(customer1TransactionLog, timeWhenDepositRequestSent, CUSTOMER1_ID, TRANSACTION_HISTORY_DEPOSIT_ACTION, CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES);
  }

  /**
   * Verifies the transaction dispute feature on a simple deposit transaction.
   * The customer's main balance should go back to the original value after the
   * reversal of the deposit. The customer's numFraudReversals counter should
   * also be incremented by 1.
   * 
   * The initial Deposit should be recorded in the TransactionHistory table.
   * 
   * The reversed Deposit should be recorded in the TransactionHistory table
   * as a Withdraw.
   * 
   * Some verifications are not done on the initial deposit since it is already
   * checked in detail in testSimpleDeposit().
   * 
   * @throws SQLException
   * @throws ScriptException
   * @throws InterruptedException
   */
  @Test
  public void testReversalOfSimpleDeposit() throws SQLException, ScriptException, InterruptedException {
    // initialize customer1 with a balance of $150. represented as pennies in the DB.
    // No overdraft or numFraudReversals.
    int CUSTOMER1_BALANCE_IN_PENNIES = convertDollarsToPennies(150);
    addCustomerToDB(CUSTOMER1_ID, CUSTOMER1_PASSWORD, CUSTOMER1_FIRST_NAME, CUSTOMER1_LAST_NAME, CUSTOMER1_BALANCE_IN_PENNIES);

    // Prepare Deposit Form to Deposit $50 to customer 1's account.
    double CUSTOMER1_AMOUNT_TO_DEPOSIT = 50; // user input is in dollar amount, not pennies.
    User customer1DepositFormInputs = new User();
    customer1DepositFormInputs.setUsername(CUSTOMER1_ID);
    customer1DepositFormInputs.setPassword(CUSTOMER1_PASSWORD);
    customer1DepositFormInputs.setAmountToDeposit(CUSTOMER1_AMOUNT_TO_DEPOSIT);

    // store timestamp of when Deposit request is sent to verify timestamps in the TransactionHistory table later
    LocalDateTime timeWhenDepositRequestSent = fetchCurrentTimeAsLocalDateTimeNoMilliseconds();
    System.out.println("Timestamp when Deposit Request is sent: " + timeWhenDepositRequestSent);

    // send request to the Deposit Form's POST handler in MvcController
    controller.submitDeposit(customer1DepositFormInputs);

    // verify that customer1's balance is now $200 after the deposit
    List<Map<String,Object>> customersTableData = jdbcTemplate.queryForList("SELECT * FROM Customers;");
    Map<String,Object> customer1Data = customersTableData.get(0);
    int CUSTOMER1_EXPECTED_BALANCE_AFTER_DEPOSIT_IN_PENNIES = convertDollarsToPennies(200);
    assertEquals(CUSTOMER1_EXPECTED_BALANCE_AFTER_DEPOSIT_IN_PENNIES, (int)customer1Data.get("Balance"));

    // sleep for 1 second to ensure the timestamps of Deposit and Reversal are different (and sortable) in TransactionHistory table
    Thread.sleep(1000);

    // Prepare Reversal Form to reverse the Deposit
    User customer1ReversalFormInputs = customer1DepositFormInputs;
    customer1ReversalFormInputs.setNumTransactionsAgo(1); // reverse the most recent transaction

    // store timestamp of when Reversal request is sent to verify timestamps in the TransactionHistory table later
    LocalDateTime timeWhenReversalRequestSent = fetchCurrentTimeAsLocalDateTimeNoMilliseconds();
    System.out.println("Timestamp when Reversal Request is sent: " + timeWhenReversalRequestSent);

    // send request to the Deposit Form's POST handler in MvcController
    controller.submitDispute(customer1ReversalFormInputs);

    // re-fetch updated customer data from the DB
    customersTableData = jdbcTemplate.queryForList("SELECT * FROM Customers;");
    customer1Data = customersTableData.get(0);

    // verify that customer1's balance is back to the original value of $150
    int CUSTOMER1_EXPECTED_BALANCE_AFTER_REVERSAL_IN_PENNIES = convertDollarsToPennies(150);
    assertEquals(CUSTOMER1_EXPECTED_BALANCE_AFTER_REVERSAL_IN_PENNIES, (int)customer1Data.get("Balance"));

    // verify that customer1's numFraudReversals counter is now 1
    assertEquals(1, (int) customer1Data.get("NumFraudReversals"));

    // fetch transaction data from the DB in chronological order
    // the more recent transaction should be the Reversal, and the older transaction should be the Deposit
    List<Map<String,Object>> transactionHistoryTableData = jdbcTemplate.queryForList("SELECT * FROM TransactionHistory ORDER BY Timestamp ASC;");
    Map<String,Object> customer1DepositTransactionLog = transactionHistoryTableData.get(0);
    Map<String,Object> customer1ReversalTransactionLog = transactionHistoryTableData.get(1);
    

    // verify that the Deposit's details are accurately logged in the TransactionHistory table
    int CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES = convertDollarsToPennies(CUSTOMER1_AMOUNT_TO_DEPOSIT);
    checkTransactionLog(customer1DepositTransactionLog, timeWhenDepositRequestSent, CUSTOMER1_ID, TRANSACTION_HISTORY_DEPOSIT_ACTION, CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES);

    // verify that the Reversal is accurately logged in the TransactionHistory table as a Withdraw
    int CUSTOMER1_AMOUNT_TO_WITHDRAW_IN_PENNIES = CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES;
    checkTransactionLog(customer1ReversalTransactionLog, timeWhenReversalRequestSent, CUSTOMER1_ID, TRANSACTION_HISTORY_WITHDRAW_ACTION, CUSTOMER1_AMOUNT_TO_WITHDRAW_IN_PENNIES);
  }

}
