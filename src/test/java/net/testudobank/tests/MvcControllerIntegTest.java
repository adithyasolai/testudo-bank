package net.testudobank.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.script.ScriptException;

import lombok.AllArgsConstructor;
import lombok.Builder;
import net.testudobank.CryptoPriceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.delegate.DatabaseDelegate;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import net.testudobank.MvcController;
import net.testudobank.User;
import net.testudobank.helpers.MvcControllerIntegTestHelpers;

@Testcontainers
@SpringBootTest
public class MvcControllerIntegTest {

  private static java.text.SimpleDateFormat SQL_DATETIME_FORMATTER = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  //// LITERAL CONSTANTS ////
  private static String CUSTOMER1_ID = "123456789";
  private static String CUSTOMER1_PASSWORD = "password";
  private static String CUSTOMER1_FIRST_NAME = "Foo";
  private static String CUSTOMER1_LAST_NAME = "Bar";
  public static long REASONABLE_TIMESTAMP_EPSILON_IN_SECONDS = 1L;

  private static String CUSTOMER2_ID = "987654321";
  private static String CUSTOMER2_PASSWORD = "password";
  private static String CUSTOMER2_FIRST_NAME = "Foo1";
  private static String CUSTOMER2_LAST_NAME = "Bar1";
  
  // Spins up small MySQL DB in local Docker container
  @Container
  public static MySQLContainer db = new MySQLContainer<>("mysql:5.7.37")
    .withUsername("root")
    .withPassword("db_password")
    .withDatabaseName("testudo_bank");


  private static MvcController controller;
  private static JdbcTemplate jdbcTemplate;
  private static DatabaseDelegate dbDelegate;
  private static CryptoPriceClient cryptoPriceClient = Mockito.mock(CryptoPriceClient.class);

  @BeforeAll
  public static void init() throws SQLException {
    dbDelegate = new JdbcDatabaseDelegate(db, "");
    ScriptUtils.runInitScript(dbDelegate, "createDB.sql");
    jdbcTemplate = new JdbcTemplate(MvcControllerIntegTestHelpers.dataSource(db));
    jdbcTemplate.getDataSource().getConnection().setCatalog(db.getDatabaseName());
    controller = new MvcController(jdbcTemplate, cryptoPriceClient);
  }

  @AfterEach
  public void clearDB() throws ScriptException {
    // runInitScript() pulls all the String text from the SQL file and just calls executeDatabaseScript(),
    // so it is OK to use runInitScript() again even though we aren't initializing the DB for the first time here.
    // runInitScript() is a poorly-named function.
    ScriptUtils.runInitScript(dbDelegate, "clearDB.sql");
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
    // initialize customer1 with a balance of $123.45 (to make sure this works for non-whole dollar amounts). represented as pennies in the DB.
    double CUSTOMER1_BALANCE = 123.45;
    int CUSTOMER1_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_BALANCE);
    MvcControllerIntegTestHelpers.addCustomerToDB(dbDelegate, CUSTOMER1_ID, CUSTOMER1_PASSWORD, CUSTOMER1_FIRST_NAME, CUSTOMER1_LAST_NAME, CUSTOMER1_BALANCE_IN_PENNIES);

    // Prepare Deposit Form to Deposit $12.34 to customer 1's account.
    double CUSTOMER1_AMOUNT_TO_DEPOSIT = 12.34; // user input is in dollar amount, not pennies.
    User customer1DepositFormInputs = new User();
    customer1DepositFormInputs.setUsername(CUSTOMER1_ID);
    customer1DepositFormInputs.setPassword(CUSTOMER1_PASSWORD);
    customer1DepositFormInputs.setAmountToDeposit(CUSTOMER1_AMOUNT_TO_DEPOSIT); 

    // verify that there are no logs in TransactionHistory table before Deposit
    assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TransactionHistory;", Integer.class));

    // store timestamp of when Deposit request is sent to verify timestamps in the TransactionHistory table later
    LocalDateTime timeWhenDepositRequestSent = MvcControllerIntegTestHelpers.fetchCurrentTimeAsLocalDateTimeNoMilliseconds();
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

    // verify customer balance was increased by $12.34
    double CUSTOMER1_EXPECTED_FINAL_BALANCE = CUSTOMER1_BALANCE + CUSTOMER1_AMOUNT_TO_DEPOSIT;
    double CUSTOMER1_EXPECTED_FINAL_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_EXPECTED_FINAL_BALANCE);
    assertEquals(CUSTOMER1_EXPECTED_FINAL_BALANCE_IN_PENNIES, (int)customer1Data.get("Balance"));

    // verify that the Deposit is the only log in TransactionHistory table
    assertEquals(1, transactionHistoryTableData.size());
    
    // verify that the Deposit's details are accurately logged in the TransactionHistory table
    Map<String,Object> customer1TransactionLog = transactionHistoryTableData.get(0);
    int CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_AMOUNT_TO_DEPOSIT);
    MvcControllerIntegTestHelpers.checkTransactionLog(customer1TransactionLog, timeWhenDepositRequestSent, CUSTOMER1_ID, MvcController.TRANSACTION_HISTORY_DEPOSIT_ACTION, CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES);
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
    // initialize customer1 with a balance of $123.45 (to make sure this works for non-whole dollar amounts). represented as pennies in the DB.
    double CUSTOMER1_BALANCE = 123.45;
    int CUSTOMER1_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_BALANCE);
    MvcControllerIntegTestHelpers.addCustomerToDB(dbDelegate, CUSTOMER1_ID, CUSTOMER1_PASSWORD, CUSTOMER1_FIRST_NAME, CUSTOMER1_LAST_NAME, CUSTOMER1_BALANCE_IN_PENNIES);

    // Prepare Withdraw Form to Withdraw $12.34 from customer 1's account.
    double CUSTOMER1_AMOUNT_TO_WITHDRAW = 12.34; // user input is in dollar amount, not pennies.
    User customer1WithdrawFormInputs = new User();
    customer1WithdrawFormInputs.setUsername(CUSTOMER1_ID);
    customer1WithdrawFormInputs.setPassword(CUSTOMER1_PASSWORD);
    customer1WithdrawFormInputs.setAmountToWithdraw(CUSTOMER1_AMOUNT_TO_WITHDRAW); // user input is in dollar amount, not pennies.

    // verify that there are no logs in TransactionHistory table before Withdraw
    assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TransactionHistory;", Integer.class));

    // store timestamp of when Withdraw request is sent to verify timestamps in the TransactionHistory table later
    LocalDateTime timeWhenWithdrawRequestSent = MvcControllerIntegTestHelpers.fetchCurrentTimeAsLocalDateTimeNoMilliseconds();
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

    // verify customer balance was decreased by $12.34
    double CUSTOMER1_EXPECTED_FINAL_BALANCE = CUSTOMER1_BALANCE - CUSTOMER1_AMOUNT_TO_WITHDRAW;
    double CUSTOMER1_EXPECTED_FINAL_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_EXPECTED_FINAL_BALANCE);
    assertEquals(CUSTOMER1_EXPECTED_FINAL_BALANCE_IN_PENNIES, (int)customer1Data.get("Balance"));

    // verify that the Withdraw is the only log in TransactionHistory table
    assertEquals(1, transactionHistoryTableData.size());

    // verify that the Withdraw's details are accurately logged in the TransactionHistory table
    Map<String,Object> customer1TransactionLog = transactionHistoryTableData.get(0);
    int CUSTOMER1_AMOUNT_TO_WITHDRAW_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_AMOUNT_TO_WITHDRAW);
    MvcControllerIntegTestHelpers.checkTransactionLog(customer1TransactionLog, timeWhenWithdrawRequestSent, CUSTOMER1_ID, MvcController.TRANSACTION_HISTORY_WITHDRAW_ACTION, CUSTOMER1_AMOUNT_TO_WITHDRAW_IN_PENNIES);
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
    // initialize customer1 with a balance of $123.45 (to make sure this works for non-whole dollar amounts). represented as pennies in the DB.
    double CUSTOMER1_BALANCE = 123.45;
    int CUSTOMER1_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_BALANCE);
    MvcControllerIntegTestHelpers.addCustomerToDB(dbDelegate, CUSTOMER1_ID, CUSTOMER1_PASSWORD, CUSTOMER1_FIRST_NAME, CUSTOMER1_LAST_NAME, CUSTOMER1_BALANCE_IN_PENNIES);

    // Prepare Withdraw Form to Withdraw $150 from customer 1's account.
    double CUSTOMER1_AMOUNT_TO_WITHDRAW = 150; // user input is in dollar amount, not pennies.
    User customer1WithdrawFormInputs = new User();
    customer1WithdrawFormInputs.setUsername(CUSTOMER1_ID);
    customer1WithdrawFormInputs.setPassword(CUSTOMER1_PASSWORD);
    customer1WithdrawFormInputs.setAmountToWithdraw(CUSTOMER1_AMOUNT_TO_WITHDRAW); // user input is in dollar amount, not pennies.

    // store timestamp of when Withdraw request is sent to verify timestamps in the TransactionHistory table later
    LocalDateTime timeWhenWithdrawRequestSent = MvcControllerIntegTestHelpers.fetchCurrentTimeAsLocalDateTimeNoMilliseconds();
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
    int CUSTOMER1_ORIGINAL_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_BALANCE);
    int CUSTOMER1_AMOUNT_TO_WITHDRAW_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_AMOUNT_TO_WITHDRAW);
    int CUSTOMER1_EXPECTED_OVERDRAFT_BALANCE_BEFORE_INTEREST_IN_PENNIES = CUSTOMER1_AMOUNT_TO_WITHDRAW_IN_PENNIES - CUSTOMER1_ORIGINAL_BALANCE_IN_PENNIES;
    int CUSTOMER1_EXPECTED_OVERDRAFT_BALANCE_AFTER_INTEREST_IN_PENNIES = MvcControllerIntegTestHelpers.applyOverdraftInterest(CUSTOMER1_EXPECTED_OVERDRAFT_BALANCE_BEFORE_INTEREST_IN_PENNIES);
    System.out.println("Expected Overdraft Balance in pennies: " + CUSTOMER1_EXPECTED_OVERDRAFT_BALANCE_AFTER_INTEREST_IN_PENNIES);
    assertEquals(CUSTOMER1_EXPECTED_OVERDRAFT_BALANCE_AFTER_INTEREST_IN_PENNIES, (int)customer1Data.get("OverdraftBalance"));

    // verify that the Withdraw's details are accurately logged in the TransactionHistory table
    Map<String,Object> customer1TransactionLog = transactionHistoryTableData.get(0);
    MvcControllerIntegTestHelpers.checkTransactionLog(customer1TransactionLog, timeWhenWithdrawRequestSent, CUSTOMER1_ID, MvcController.TRANSACTION_HISTORY_WITHDRAW_ACTION, CUSTOMER1_AMOUNT_TO_WITHDRAW_IN_PENNIES);
  }

  /**
   * The customer will be given an initial balance of $100 and will withdraw $1099.
   * This will test the scenario where the withdraw excess amount, which is $100 - $1099 = -$999,
   * results in a valid overdraft balance, but once the 2% interest rate is applied, will cross the limit
   * of $1000. 
   * 
   * This test checks to make sure that the customer's balance stays the same as before due to a failed 
   * withdraw request, and checks that the TransactionHistory table is empty.
   * 
   * @throws SQLException
   * @throws ScriptException
   */
  @Test
  public void testWithdrawOverdraftLimitExceeded() throws SQLException, ScriptException { 
    //initialize customer1 with a balance of $100. this will be represented as pennies in DB.
    double CUSTOMER1_BALANCE = 100;
    int CUSTOMER1_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_BALANCE);
    MvcControllerIntegTestHelpers.addCustomerToDB(dbDelegate, CUSTOMER1_ID, CUSTOMER1_PASSWORD, CUSTOMER1_FIRST_NAME, CUSTOMER1_LAST_NAME, CUSTOMER1_BALANCE_IN_PENNIES);

    //Prepare Withdraw Form to withdraw $1099 from this customer's account.
    double CUSTOMER1_AMOUNT_TO_WITHDRAW = 1099; 
    User customer1WithdrawFormInputs = new User();
    customer1WithdrawFormInputs.setUsername(CUSTOMER1_ID);
    customer1WithdrawFormInputs.setPassword(CUSTOMER1_PASSWORD);
    customer1WithdrawFormInputs.setAmountToWithdraw(CUSTOMER1_AMOUNT_TO_WITHDRAW);

    //Store the timestamp of the withdraw request to verify it in the TransactionHistory table later
    LocalDateTime timeWhenWithdrawRequestSent = MvcControllerIntegTestHelpers.fetchCurrentTimeAsLocalDateTimeNoMilliseconds();
    System.out.println("Timestamp when withdraw request sent: " + timeWhenWithdrawRequestSent);

    //Check the response when the withdraw request is submitted. This should return the user back to the home screen due to an invalid request
    String responsePage = controller.submitWithdraw(customer1WithdrawFormInputs);
    assertEquals("welcome", responsePage);

    //Fetch customer1's data from DB
    List<Map<String, Object>> customersTableData = jdbcTemplate.queryForList("SELECT * FROM Customers;");

    //Since the request did not go through, the balance is supposed to stay the same.
    Map<String, Object> customer1Data = customersTableData.get(0);
    assertEquals(CUSTOMER1_BALANCE_IN_PENNIES, (int)customer1Data.get("Balance"));

    //Checks to make sure that the overdraft balance was not increased
    assertEquals(0, (int)customer1Data.get("OverdraftBalance"));

    //check that TransactionHistory table is empty
    List<Map<String, Object>> transactionHistoryTableData = jdbcTemplate.queryForList("SELECT * FROM TransactionHistory;");
    assertTrue(transactionHistoryTableData.isEmpty());

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
    // initialize customer1 with an overdraft balance of $123.45 (to make sure this works for non-whole dollar amounts). represented as pennies in the DB.
    int CUSTOMER1_MAIN_BALANCE_IN_PENNIES = 0;
    double CUSTOMER1_OVERDRAFT_BALANCE = 123.45;
    int CUSTOMER1_OVERDRAFT_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_OVERDRAFT_BALANCE);
    int CUSTOMER1_NUM_FRAUD_REVERSALS = 0;
    MvcControllerIntegTestHelpers.addCustomerToDB(dbDelegate, CUSTOMER1_ID, CUSTOMER1_PASSWORD, CUSTOMER1_FIRST_NAME, CUSTOMER1_LAST_NAME, CUSTOMER1_MAIN_BALANCE_IN_PENNIES, CUSTOMER1_OVERDRAFT_BALANCE_IN_PENNIES, CUSTOMER1_NUM_FRAUD_REVERSALS);

    // Prepare Deposit Form to Deposit $150 to customer 1's account.
    double CUSTOMER1_AMOUNT_TO_DEPOSIT = 150; // user input is in dollar amount, not pennies.
    User customer1DepositFormInputs = new User();
    customer1DepositFormInputs.setUsername(CUSTOMER1_ID);
    customer1DepositFormInputs.setPassword(CUSTOMER1_PASSWORD);
    customer1DepositFormInputs.setAmountToDeposit(CUSTOMER1_AMOUNT_TO_DEPOSIT); 

    // store timestamp of when Deposit request is sent to verify timestamps in the TransactionHistory and OverdraftLogs tables later
    LocalDateTime timeWhenDepositRequestSent = MvcControllerIntegTestHelpers.fetchCurrentTimeAsLocalDateTimeNoMilliseconds();
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
    int CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_AMOUNT_TO_DEPOSIT);
    int CUSTOMER1_ORIGINAL_OVERDRAFT_BALANCE_IN_PENNIES = CUSTOMER1_OVERDRAFT_BALANCE_IN_PENNIES;
    int CUSTOMER1_EXPECTED_MAIN_BALANCE_IN_PENNIES = CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES - CUSTOMER1_ORIGINAL_OVERDRAFT_BALANCE_IN_PENNIES;
    assertEquals(CUSTOMER1_EXPECTED_MAIN_BALANCE_IN_PENNIES, (int)customer1Data.get("Balance"));

    // verify that the deposit is logged properly in the OverdraftLogs table
    Map<String,Object> customer1OverdraftLog = overdraftLogsTableData.get(0);
    MvcControllerIntegTestHelpers.checkOverdraftLog(customer1OverdraftLog, timeWhenDepositRequestSent, CUSTOMER1_ID, CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES, CUSTOMER1_ORIGINAL_OVERDRAFT_BALANCE_IN_PENNIES, 0);

    // verify that the Deposit's details are accurately logged in the TransactionHistory table
    Map<String,Object> customer1TransactionLog = transactionHistoryTableData.get(0);
    MvcControllerIntegTestHelpers.checkTransactionLog(customer1TransactionLog, timeWhenDepositRequestSent, CUSTOMER1_ID, MvcController.TRANSACTION_HISTORY_DEPOSIT_ACTION, CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES);
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
    // initialize customer1 with an overdraft balance of $123.45 (to make sure this works for non-whole dollar amounts). represented as pennies in the DB.
    int CUSTOMER1_MAIN_BALANCE_IN_PENNIES = 0;
    double CUSTOMER1_OVERDRAFT_BALANCE = 123.45;
    int CUSTOMER1_OVERDRAFT_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_OVERDRAFT_BALANCE);
    int CUSTOMER1_NUM_FRAUD_REVERSALS = 0;
    MvcControllerIntegTestHelpers.addCustomerToDB(dbDelegate, CUSTOMER1_ID, CUSTOMER1_PASSWORD, CUSTOMER1_FIRST_NAME, CUSTOMER1_LAST_NAME, CUSTOMER1_MAIN_BALANCE_IN_PENNIES, CUSTOMER1_OVERDRAFT_BALANCE_IN_PENNIES, CUSTOMER1_NUM_FRAUD_REVERSALS);

    // Prepare Deposit Form to Deposit $50 to customer 1's account.
    double CUSTOMER1_AMOUNT_TO_DEPOSIT = 50; // user input is in dollar amount, not pennies.
    User customer1DepositFormInputs = new User();
    customer1DepositFormInputs.setUsername(CUSTOMER1_ID);
    customer1DepositFormInputs.setPassword(CUSTOMER1_PASSWORD);
    customer1DepositFormInputs.setAmountToDeposit(CUSTOMER1_AMOUNT_TO_DEPOSIT); 

    // store timestamp of when Deposit request is sent to verify timestamps in the TransactionHistory and OverdraftLogs tables later
    LocalDateTime timeWhenDepositRequestSent = MvcControllerIntegTestHelpers.fetchCurrentTimeAsLocalDateTimeNoMilliseconds();
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
    int CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_AMOUNT_TO_DEPOSIT);
    int CUSTOMER1_EXPECTED_FINAL_OVERDRAFT_BALANCE_IN_PENNIES = CUSTOMER1_ORIGINAL_OVERDRAFT_BALANCE_IN_PENNIES - CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES;
    assertEquals(CUSTOMER1_EXPECTED_FINAL_OVERDRAFT_BALANCE_IN_PENNIES, (int)customer1Data.get("OverdraftBalance"));

    // verify that the customer's main balance is still $0
    int CUSTOMER1_EXPECTED_MAIN_BALANCE_IN_PENNIES = 0;
    assertEquals(CUSTOMER1_EXPECTED_MAIN_BALANCE_IN_PENNIES, (int)customer1Data.get("Balance"));

    // verify that the deposit is logged properly in the OverdraftLogs table
    Map<String,Object> customer1OverdraftLog = overdraftLogsTableData.get(0);
    MvcControllerIntegTestHelpers.checkOverdraftLog(customer1OverdraftLog, timeWhenDepositRequestSent, CUSTOMER1_ID, CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES, CUSTOMER1_ORIGINAL_OVERDRAFT_BALANCE_IN_PENNIES, CUSTOMER1_EXPECTED_FINAL_OVERDRAFT_BALANCE_IN_PENNIES);

    // verify that the Withdraw's details are accurately logged in the TransactionHistory table
    Map<String,Object> customer1TransactionLog = transactionHistoryTableData.get(0);
    MvcControllerIntegTestHelpers.checkTransactionLog(customer1TransactionLog, timeWhenDepositRequestSent, CUSTOMER1_ID, MvcController.TRANSACTION_HISTORY_DEPOSIT_ACTION, CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES);
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
   * Some verifications are not done on the initial Deposit since it is already
   * checked in detail in testSimpleDeposit().
   * 
   * @throws SQLException
   * @throws ScriptException
   * @throws InterruptedException
   */
  @Test
  public void testReversalOfSimpleDeposit() throws SQLException, ScriptException, InterruptedException {
    // initialize customer1 with a balance of $123.45 (to make sure this works for non-whole dollar amounts). represented as pennies in the DB.
    // No overdraft or numFraudReversals.
    double CUSTOMER1_BALANCE = 123.45;
    int CUSTOMER1_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_BALANCE);
    MvcControllerIntegTestHelpers.addCustomerToDB(dbDelegate, CUSTOMER1_ID, CUSTOMER1_PASSWORD, CUSTOMER1_FIRST_NAME, CUSTOMER1_LAST_NAME, CUSTOMER1_BALANCE_IN_PENNIES);

    // Prepare Deposit Form to Deposit $12.34 (to make sure this works for non-whole dollar amounts) to customer 1's account.
    double CUSTOMER1_AMOUNT_TO_DEPOSIT = 12.34; // user input is in dollar amount, not pennies.
    User customer1DepositFormInputs = new User();
    customer1DepositFormInputs.setUsername(CUSTOMER1_ID);
    customer1DepositFormInputs.setPassword(CUSTOMER1_PASSWORD);
    customer1DepositFormInputs.setAmountToDeposit(CUSTOMER1_AMOUNT_TO_DEPOSIT);

    // store timestamp of when Deposit request is sent to verify timestamps in the TransactionHistory table later
    LocalDateTime timeWhenDepositRequestSent = MvcControllerIntegTestHelpers.fetchCurrentTimeAsLocalDateTimeNoMilliseconds();
    System.out.println("Timestamp when Deposit Request is sent: " + timeWhenDepositRequestSent);

    // send request to the Deposit Form's POST handler in MvcController
    controller.submitDeposit(customer1DepositFormInputs);

    // verify customer1's balance after the deposit
    List<Map<String,Object>> customersTableData = jdbcTemplate.queryForList("SELECT * FROM Customers;");
    Map<String,Object> customer1Data = customersTableData.get(0);
    double CUSTOMER1_EXPECTED_BALANCE_AFTER_DEPOSIT = CUSTOMER1_BALANCE + CUSTOMER1_AMOUNT_TO_DEPOSIT; 
    int CUSTOMER1_EXPECTED_BALANCE_AFTER_DEPOSIT_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_EXPECTED_BALANCE_AFTER_DEPOSIT);
    assertEquals(CUSTOMER1_EXPECTED_BALANCE_AFTER_DEPOSIT_IN_PENNIES, (int)customer1Data.get("Balance"));

    // sleep for 1 second to ensure the timestamps of Deposit and Reversal are different (and sortable) in TransactionHistory table
    Thread.sleep(1000);

    // Prepare Reversal Form to reverse the Deposit
    User customer1ReversalFormInputs = customer1DepositFormInputs;
    customer1ReversalFormInputs.setNumTransactionsAgo(1); // reverse the most recent transaction

    // store timestamp of when Reversal request is sent to verify timestamps in the TransactionHistory table later
    LocalDateTime timeWhenReversalRequestSent = MvcControllerIntegTestHelpers.fetchCurrentTimeAsLocalDateTimeNoMilliseconds();
    System.out.println("Timestamp when Reversal Request is sent: " + timeWhenReversalRequestSent);

    // send Dispute request
    controller.submitDispute(customer1ReversalFormInputs);

    // re-fetch updated customer data from the DB
    customersTableData = jdbcTemplate.queryForList("SELECT * FROM Customers;");
    customer1Data = customersTableData.get(0);

    // verify that customer1's balance is back to the original value
    int CUSTOMER1_EXPECTED_BALANCE_AFTER_REVERSAL_IN_PENNIES = CUSTOMER1_BALANCE_IN_PENNIES;
    assertEquals(CUSTOMER1_EXPECTED_BALANCE_AFTER_REVERSAL_IN_PENNIES, (int)customer1Data.get("Balance"));

    // verify that customer1's numFraudReversals counter is now 1
    assertEquals(1, (int) customer1Data.get("NumFraudReversals"));

    // fetch transaction data from the DB in chronological order
    // the more recent transaction should be the Reversal, and the older transaction should be the Deposit
    List<Map<String,Object>> transactionHistoryTableData = jdbcTemplate.queryForList("SELECT * FROM TransactionHistory ORDER BY Timestamp ASC;");
    Map<String,Object> customer1DepositTransactionLog = transactionHistoryTableData.get(0);
    Map<String,Object> customer1ReversalTransactionLog = transactionHistoryTableData.get(1);

    // verify that the Deposit's details are accurately logged in the TransactionHistory table
    int CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_AMOUNT_TO_DEPOSIT);
    MvcControllerIntegTestHelpers.checkTransactionLog(customer1DepositTransactionLog, timeWhenDepositRequestSent, CUSTOMER1_ID, MvcController.TRANSACTION_HISTORY_DEPOSIT_ACTION, CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES);

    // verify that the Reversal is accurately logged in the TransactionHistory table as a Withdraw
    int CUSTOMER1_AMOUNT_TO_WITHDRAW_IN_PENNIES = CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES;
    MvcControllerIntegTestHelpers.checkTransactionLog(customer1ReversalTransactionLog, timeWhenReversalRequestSent, CUSTOMER1_ID, MvcController.TRANSACTION_HISTORY_WITHDRAW_ACTION, CUSTOMER1_AMOUNT_TO_WITHDRAW_IN_PENNIES);
  }

  /**
   * Verifies the transaction dispute feature on a simple withdraw transaction.
   * The customer's main balance should go back to the original value after the
   * reversal of the withdraw. The customer's numFraudReversals counter should
   * also be incremented by 1.
   * 
   * The initial Withdraw should be recorded in the TransactionHistory table.
   * 
   * The reversed Withdraw should be recorded in the TransactionHistory table
   * as a Deposit.
   * 
   * Some verifications are not done on the initial Withdraw since it is already
   * checked in detail in testSimpleWithdraw().
   * 
   * @throws SQLException
   * @throws ScriptException
   * @throws InterruptedException
   */
  @Test
  public void testReversalOfSimpleWithdraw() throws SQLException, ScriptException, InterruptedException {
    // initialize customer1 with a balance of $123.45 (to make sure this works for non-whole dollar amounts). represented as pennies in the DB.
    // No overdraft or numFraudReversals.
    double CUSTOMER1_BALANCE = 123.45;
    int CUSTOMER1_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_BALANCE);
    MvcControllerIntegTestHelpers.addCustomerToDB(dbDelegate, CUSTOMER1_ID, CUSTOMER1_PASSWORD, CUSTOMER1_FIRST_NAME, CUSTOMER1_LAST_NAME, CUSTOMER1_BALANCE_IN_PENNIES);

    // Prepare Withdraw Form to Withdraw $12.34 to customer 1's account.
    double CUSTOMER1_AMOUNT_TO_WITHDRAW = 12.34; // user input is in dollar amount, not pennies.
    User customer1WithdrawFormInputs = new User();
    customer1WithdrawFormInputs.setUsername(CUSTOMER1_ID);
    customer1WithdrawFormInputs.setPassword(CUSTOMER1_PASSWORD);
    customer1WithdrawFormInputs.setAmountToWithdraw(CUSTOMER1_AMOUNT_TO_WITHDRAW);

    // store timestamp of when Withdraw request is sent to verify timestamps in the TransactionHistory table later
    LocalDateTime timeWhenWithdrawRequestSent = MvcControllerIntegTestHelpers.fetchCurrentTimeAsLocalDateTimeNoMilliseconds();
    System.out.println("Timestamp when Withdraw Request is sent: " + timeWhenWithdrawRequestSent);

    // send request to the Withdraw Form's POST handler in MvcController
    controller.submitWithdraw(customer1WithdrawFormInputs);

    // verify customer1's balance after the withdraw
    List<Map<String,Object>> customersTableData = jdbcTemplate.queryForList("SELECT * FROM Customers;");
    Map<String,Object> customer1Data = customersTableData.get(0);
    double CUSTOMER1_EXPECTED_BALANCE_AFTER_WITHDRAW = CUSTOMER1_BALANCE - CUSTOMER1_AMOUNT_TO_WITHDRAW;
    int CUSTOMER1_EXPECTED_BALANCE_AFTER_WITHDRAW_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_EXPECTED_BALANCE_AFTER_WITHDRAW);
    assertEquals(CUSTOMER1_EXPECTED_BALANCE_AFTER_WITHDRAW_IN_PENNIES, (int)customer1Data.get("Balance"));

    // sleep for 1 second to ensure the timestamps of Withdraw and Reversal are different (and sortable) in TransactionHistory table
    Thread.sleep(1000);

    // Prepare Reversal Form to reverse the Withdraw
    User customer1ReversalFormInputs = customer1WithdrawFormInputs;
    customer1ReversalFormInputs.setNumTransactionsAgo(1); // reverse the most recent transaction

    // store timestamp of when Reversal request is sent to verify timestamps in the TransactionHistory table later
    LocalDateTime timeWhenReversalRequestSent = MvcControllerIntegTestHelpers.fetchCurrentTimeAsLocalDateTimeNoMilliseconds();
    System.out.println("Timestamp when Reversal Request is sent: " + timeWhenReversalRequestSent);

    // send Dispute request
    controller.submitDispute(customer1ReversalFormInputs);

    // re-fetch updated customer data from the DB
    customersTableData = jdbcTemplate.queryForList("SELECT * FROM Customers;");
    customer1Data = customersTableData.get(0);

    // verify that customer1's balance is back to the original value
    int CUSTOMER1_EXPECTED_BALANCE_AFTER_REVERSAL_IN_PENNIES = CUSTOMER1_BALANCE_IN_PENNIES;
    assertEquals(CUSTOMER1_EXPECTED_BALANCE_AFTER_REVERSAL_IN_PENNIES, (int)customer1Data.get("Balance"));

    // verify that customer1's numFraudReversals counter is now 1
    assertEquals(1, (int) customer1Data.get("NumFraudReversals"));

    // fetch transaction data from the DB in chronological order
    // the more recent transaction should be the Reversal, and the older transaction should be the Withdraw
    List<Map<String,Object>> transactionHistoryTableData = jdbcTemplate.queryForList("SELECT * FROM TransactionHistory ORDER BY Timestamp ASC;");
    Map<String,Object> customer1WithdrawTransactionLog = transactionHistoryTableData.get(0);
    Map<String,Object> customer1ReversalTransactionLog = transactionHistoryTableData.get(1);

    // verify that the Withdraw's details are accurately logged in the TransactionHistory table
    int CUSTOMER1_AMOUNT_TO_WITHDRAW_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_AMOUNT_TO_WITHDRAW);
    MvcControllerIntegTestHelpers.checkTransactionLog(customer1WithdrawTransactionLog, timeWhenWithdrawRequestSent, CUSTOMER1_ID, MvcController.TRANSACTION_HISTORY_WITHDRAW_ACTION, CUSTOMER1_AMOUNT_TO_WITHDRAW_IN_PENNIES);

    // verify that the Reversal is accurately logged in the TransactionHistory table as a Deposit
    int CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES = CUSTOMER1_AMOUNT_TO_WITHDRAW_IN_PENNIES;
    MvcControllerIntegTestHelpers.checkTransactionLog(customer1ReversalTransactionLog, timeWhenReversalRequestSent, CUSTOMER1_ID, MvcController.TRANSACTION_HISTORY_DEPOSIT_ACTION, CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES);
  }

  /**
   * Verifies that a customer's account is "frozen" if they
   * have reached the maximum allowed disputes/reversals.
   * 
   * "Frozen" means that any further deposit, withdraw,
   * and dispute requests are ignored and the customer is 
   * simply redirected to the "welcome" page.
   * 
   * The customer should still be able to view their account
   * via the login form.
   * 
   * @throws SQLException
   * @throws ScriptException
   * @throws InterruptedException
   */
  @Test
  public void testFrozenAccount() throws SQLException, ScriptException, InterruptedException {
    // initialize a customer in the DB that can only do 1 more dispute/reversal.
    int CUSTOMER1_NUM_FRAUD_REVERSALS = MvcController.MAX_DISPUTES - 1;
    // initialize with $100 main balance and $0 overdraft balance for simplicity
    double CUSTOMER1_MAIN_BALANCE = 100;
    int CUSTOMER1_MAIN_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_MAIN_BALANCE);
    int CUSTOMER1_OVERDRAFT_BALANCE_IN_PENNIES = 0;
    MvcControllerIntegTestHelpers.addCustomerToDB(dbDelegate, 
                                                  CUSTOMER1_ID, 
                                                  CUSTOMER1_PASSWORD, 
                                                  CUSTOMER1_FIRST_NAME, 
                                                  CUSTOMER1_LAST_NAME, 
                                                  CUSTOMER1_MAIN_BALANCE_IN_PENNIES, 
                                                  CUSTOMER1_OVERDRAFT_BALANCE_IN_PENNIES,
                                                  CUSTOMER1_NUM_FRAUD_REVERSALS);

    // Deposit $50, and then immediately dispute/reverse that deposit.
    // this will bring the customer to the MAX_DISPUTES limit, and also have a few
    // transactions in the TransactionHistory table that we can attempt to dispute/reverse
    // later (which we will expect to fail due to MAX_DISPUTES limit, and not due to a lack
    // of transactions to reverse for the customer).
    // The asserts for this deposit & dispute are not very rigorous as they are covered
    // in the testReversalOfSimpleDeposit() test case.
    double CUSTOMER1_AMOUNT_TO_DEPOSIT = 50;
    User customer1DepositFormInputs = new User();
    customer1DepositFormInputs.setUsername(CUSTOMER1_ID);
    customer1DepositFormInputs.setPassword(CUSTOMER1_PASSWORD);
    customer1DepositFormInputs.setAmountToDeposit(CUSTOMER1_AMOUNT_TO_DEPOSIT);

    // send Deposit request to the Deposit Form's POST handler in MvcController
    controller.submitDeposit(customer1DepositFormInputs);

    // verify customer1's balance after the deposit
    List<Map<String,Object>> customersTableData = jdbcTemplate.queryForList("SELECT * FROM Customers;");
    Map<String,Object> customer1Data = customersTableData.get(0);
    double CUSTOMER1_EXPECTED_BALANCE_AFTER_DEPOSIT = CUSTOMER1_MAIN_BALANCE + CUSTOMER1_AMOUNT_TO_DEPOSIT; 
    int CUSTOMER1_EXPECTED_BALANCE_AFTER_DEPOSIT_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_EXPECTED_BALANCE_AFTER_DEPOSIT);
    assertEquals(CUSTOMER1_EXPECTED_BALANCE_AFTER_DEPOSIT_IN_PENNIES, (int)customer1Data.get("Balance"));

    // Prepare Reversal Form to reverse the Deposit
    User customer1ReversalFormInputs = customer1DepositFormInputs;
    customer1ReversalFormInputs.setNumTransactionsAgo(1); // reverse the most recent transaction

    // send Dispute request
    controller.submitDispute(customer1ReversalFormInputs);

    // re-fetch updated customer data from the DB
    customersTableData = jdbcTemplate.queryForList("SELECT * FROM Customers;");
    customer1Data = customersTableData.get(0);

    // verify that customer1's balance is back to the original value
    int CUSTOMER1_EXPECTED_BALANCE_AFTER_REVERSAL_IN_PENNIES = CUSTOMER1_MAIN_BALANCE_IN_PENNIES;
    assertEquals(CUSTOMER1_EXPECTED_BALANCE_AFTER_REVERSAL_IN_PENNIES, (int)customer1Data.get("Balance"));

    // verify that customer1's numFraudReversals counter is now MAX_DISPUTES
    assertEquals(MvcController.MAX_DISPUTES, (int)customer1Data.get("NumFraudReversals"));

    // verify that there are two transactions in the TransactionHistory table
    // (one for the deposit, one for the reversal of that deposit)
    List<Map<String,Object>> transactionHistoryTableData = jdbcTemplate.queryForList("SELECT * FROM TransactionHistory;");
    assertEquals(2, transactionHistoryTableData.size());

    //// Begin Frozen Account Checks ////
    User customer1FrozenFormInputs = new User();

    // customer should still be able to view account info with the Login Form
    customer1FrozenFormInputs.setUsername(CUSTOMER1_ID);
    customer1FrozenFormInputs.setPassword(CUSTOMER1_PASSWORD);
    String responsePage = controller.submitLoginForm(customer1FrozenFormInputs);
    assertEquals("account_info", responsePage);

    // customer should not be able to Deposit
    customer1FrozenFormInputs.setAmountToDeposit(MvcControllerIntegTestHelpers.convertDollarsToPennies(50));
    responsePage = controller.submitDeposit(customer1FrozenFormInputs);
    assertEquals("welcome", responsePage);

    // customer should not be able to Withdraw
    customer1FrozenFormInputs.setAmountToWithdraw(MvcControllerIntegTestHelpers.convertDollarsToPennies(50));
    responsePage = controller.submitWithdraw(customer1FrozenFormInputs);
    assertEquals("welcome", responsePage);

    // customer should not be able to Dispute/Reverse a Transaction
    customer1FrozenFormInputs.setNumTransactionsAgo(1);
    responsePage = controller.submitDispute(customer1FrozenFormInputs);
    assertEquals("welcome", responsePage);

    // verify customer's data and # of transactions is unchanged
    // re-fetch updated customer data from the DB
    customersTableData = jdbcTemplate.queryForList("SELECT * FROM Customers;");
    customer1Data = customersTableData.get(0);
    assertEquals(CUSTOMER1_EXPECTED_BALANCE_AFTER_REVERSAL_IN_PENNIES, (int)customer1Data.get("Balance"));
    assertEquals(MvcController.MAX_DISPUTES, (int)customer1Data.get("NumFraudReversals"));

    transactionHistoryTableData = jdbcTemplate.queryForList("SELECT * FROM TransactionHistory;");
    assertEquals(2, transactionHistoryTableData.size());
  }

  /**
   * Verifies the transaction dispute feature on a reversal of a deposit that 
   * causes a customer to exceed the overdraft limit.
   * 
   * The initial Deposit and Withdraw should be recorded in the TransactionHistory table.
   * 
   * Trying to reverse a deposit that causes the customer to go over the overdraft limit
   * should result in the customer being directed to the welcome screen and not process 
   * the reversal.
   * 
   * @throws SQLException
   * @throws ScriptException
   * @throws InterruptedException
   */
  @Test
  public void testReverseDepositExceedsOverdraftLimit() throws SQLException, ScriptException, InterruptedException {
    // initialize customer1 with a balance of $0 represented as pennies in the DB.
    // No overdraft or numFraudReversals.
    double CUSTOMER1_BALANCE = 0;
    int CUSTOMER1_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_BALANCE);
    MvcControllerIntegTestHelpers.addCustomerToDB(dbDelegate, CUSTOMER1_ID, CUSTOMER1_PASSWORD, CUSTOMER1_FIRST_NAME, CUSTOMER1_LAST_NAME, CUSTOMER1_BALANCE_IN_PENNIES);

    // Prepare Deposit Form to Deposit $100 to customer 1's account.
    double CUSTOMER1_AMOUNT_TO_DEPOSIT = 100; // user input is in dollar amount, not pennies.
    User customer1DepositFormInputs = new User();
    customer1DepositFormInputs.setUsername(CUSTOMER1_ID);
    customer1DepositFormInputs.setPassword(CUSTOMER1_PASSWORD);
    customer1DepositFormInputs.setAmountToDeposit(CUSTOMER1_AMOUNT_TO_DEPOSIT);

    // store timestamp of when Deposit request is sent to verify timestamps in the TransactionHistory table later
    LocalDateTime timeWhenDepositRequestSent = MvcControllerIntegTestHelpers.fetchCurrentTimeAsLocalDateTimeNoMilliseconds();
    System.out.println("Timestamp when Deposit Request is sent: " + timeWhenDepositRequestSent);

    // send request to the Deposit Form's POST handler in MvcController
    controller.submitDeposit(customer1DepositFormInputs);
 
     // sleep for 1 second to ensure the timestamps of Deposit, Withdraw, and Reversal are different (and sortable) in TransactionHistory table
     Thread.sleep(1000);

    // Prepare Withdraw Form to Withdraw $1050 from customer 1's account.
    double CUSTOMER1_AMOUNT_TO_WITHDRAW = 1050; // user input is in dollar amount, not pennies.
    User customer1WithdrawFormInputs = new User();
    customer1WithdrawFormInputs.setUsername(CUSTOMER1_ID);
    customer1WithdrawFormInputs.setPassword(CUSTOMER1_PASSWORD);
    customer1WithdrawFormInputs.setAmountToWithdraw(CUSTOMER1_AMOUNT_TO_WITHDRAW);

    // store timestamp of when Withdraw request is sent to verify timestamps in the TransactionHistory table later
    LocalDateTime timeWhenWithdrawRequestSent = MvcControllerIntegTestHelpers.fetchCurrentTimeAsLocalDateTimeNoMilliseconds();
    System.out.println("Timestamp when Withdraw Request is sent: " + timeWhenWithdrawRequestSent);

    // send request to the Withdraw Form's POST handler in MvcController
    controller.submitWithdraw(customer1WithdrawFormInputs);

    // sleep for 1 second to ensure the timestamps of Deposit, Withdraw, and Reversal are different (and sortable) in TransactionHistory table
    Thread.sleep(1000);

    // fetch transaction data from the DB in chronological order
    List<Map<String,Object>> transactionHistoryTableData = jdbcTemplate.queryForList("SELECT * FROM TransactionHistory ORDER BY Timestamp ASC;");

    // verify that the Deposit & Withdraw are the only logs in TransactionHistory table
    assertEquals(2, transactionHistoryTableData.size());

    // Prepare Reversal Form to reverse the Deposit
    User customer1ReversalFormInputs = customer1DepositFormInputs;
    customer1ReversalFormInputs.setNumTransactionsAgo(2); // reverse the first transaction

    // send Dispute request
    String responsePage = controller.submitDispute(customer1ReversalFormInputs);
    assertEquals("welcome", responsePage);

    // re-fetch transaction data from the DB in chronological order
    // the more recent transaction should be the Withdraw, and the older transaction should be the Deposit
    transactionHistoryTableData = jdbcTemplate.queryForList("SELECT * FROM TransactionHistory ORDER BY Timestamp ASC;");

    // verify that the original Deposit & Withdraw are still the only logs in TransactionHistory table
    assertEquals(2, transactionHistoryTableData.size());

    Map<String,Object> customer1DepositTransactionLog = transactionHistoryTableData.get(0);
    Map<String,Object> customer1WithdrawTransactionLog = transactionHistoryTableData.get(1);

    // verify that the Deposit's details are accurately logged in the TransactionHistory table
    int CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_AMOUNT_TO_DEPOSIT);
    MvcControllerIntegTestHelpers.checkTransactionLog(customer1DepositTransactionLog, timeWhenDepositRequestSent, CUSTOMER1_ID, MvcController.TRANSACTION_HISTORY_DEPOSIT_ACTION, CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES);

    // verify that the Withdraw's details are accurately logged in the TransactionHistory table
    int CUSTOMER1_AMOUNT_TO_WITHDRAW_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_AMOUNT_TO_WITHDRAW);
    MvcControllerIntegTestHelpers.checkTransactionLog(customer1WithdrawTransactionLog, timeWhenWithdrawRequestSent, CUSTOMER1_ID, MvcController.TRANSACTION_HISTORY_WITHDRAW_ACTION, CUSTOMER1_AMOUNT_TO_WITHDRAW_IN_PENNIES);
  }

  /**
   * Verifies the transaction dispute feature on a reversal of a deposit that 
   * causes a customer to fall into overdraft.
   * 
   * 
   * Reversing a deposit that causes a customer to fall into overdraft should 
   * make the customer go into overdraft and apply the 2% interest fee on 
   * the overdraft balance.
   * 
   * @throws SQLException
   * @throws ScriptException
   * @throws InterruptedException
   */
  @Test
  public void testReverseDepositCausesOverdraft() throws SQLException, ScriptException, InterruptedException {
    // initialize customer1 with a balance of $0 represented as pennies in the DB.
    // No overdraft or numFraudReversals.
    double CUSTOMER1_BALANCE = 0;
    int CUSTOMER1_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_BALANCE);
    MvcControllerIntegTestHelpers.addCustomerToDB(dbDelegate, CUSTOMER1_ID, CUSTOMER1_PASSWORD, CUSTOMER1_FIRST_NAME, CUSTOMER1_LAST_NAME, CUSTOMER1_BALANCE_IN_PENNIES);

    // Prepare Deposit Form to Deposit $100 to customer 1's account.
    double CUSTOMER1_AMOUNT_TO_DEPOSIT = 100; // user input is in dollar amount, not pennies.
    User customer1DepositFormInputs = new User();
    customer1DepositFormInputs.setUsername(CUSTOMER1_ID);
    customer1DepositFormInputs.setPassword(CUSTOMER1_PASSWORD);
    customer1DepositFormInputs.setAmountToDeposit(CUSTOMER1_AMOUNT_TO_DEPOSIT);

    // send request to the Deposit Form's POST handler in MvcController
    controller.submitDeposit(customer1DepositFormInputs);

    // sleep for 1 second to ensure the timestamps of Withdraw and Reversal are different (and sortable) in TransactionHistory table
    Thread.sleep(1000);

    // Prepare Withdraw Form to Withdraw $50 from customer 1's account.
    double CUSTOMER1_AMOUNT_TO_WITHDRAW = 50; // user input is in dollar amount, not pennies.
    User customer1WithdrawFormInputs = new User();
    customer1WithdrawFormInputs.setUsername(CUSTOMER1_ID);
    customer1WithdrawFormInputs.setPassword(CUSTOMER1_PASSWORD);
    customer1WithdrawFormInputs.setAmountToWithdraw(CUSTOMER1_AMOUNT_TO_WITHDRAW);

    // send request to the Withdraw Form's POST handler in MvcController
    controller.submitWithdraw(customer1WithdrawFormInputs);

    // sleep for 1 second to ensure the timestamps of Withdraw and Reversal are different (and sortable) in TransactionHistory table
    Thread.sleep(1000);

    // fetch updated customer1 data from the DB
    List<Map<String,Object>> customersTableData = jdbcTemplate.queryForList("SELECT * FROM Customers;");

    // Prepare Reversal Form to reverse the Deposit
    User customer1ReversalFormInputs = customer1DepositFormInputs;
    customer1ReversalFormInputs.setNumTransactionsAgo(2); // reverse the first transaction

    // send Dispute request
    controller.submitDispute(customer1ReversalFormInputs);

    // fetch updated customer1 data from the DB
    customersTableData = jdbcTemplate.queryForList("SELECT * FROM Customers;");
     
     // verify that customer1's main balance is now 0
    Map<String,Object> customer1Data = customersTableData.get(0);
    assertEquals(0, (int)customer1Data.get("Balance"));
    
    // verify that customer1's Overdraft balance is equal to the remaining withdraw amount with interest applied
    // (convert to pennies before applying interest rate to avoid floating point roundoff errors when applying the interest rate)
    int CUSTOMER1_ORIGINAL_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_BALANCE);
    int CUSTOMER1_AMOUNT_TO_WITHDRAW_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_AMOUNT_TO_WITHDRAW);
    int CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_AMOUNT_TO_DEPOSIT);
    int CUSTOMER1_AMOUNT_TO_REVERSE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_AMOUNT_TO_DEPOSIT);
    int CUSTOMER1_EXPECTED_OVERDRAFT_BALANCE_BEFORE_INTEREST_IN_PENNIES = CUSTOMER1_ORIGINAL_BALANCE_IN_PENNIES + CUSTOMER1_AMOUNT_TO_DEPOSIT_IN_PENNIES + CUSTOMER1_AMOUNT_TO_WITHDRAW_IN_PENNIES - CUSTOMER1_AMOUNT_TO_REVERSE_IN_PENNIES;
    int CUSTOMER1_EXPECTED_OVERDRAFT_BALANCE_AFTER_INTEREST_IN_PENNIES = MvcControllerIntegTestHelpers.applyOverdraftInterest(CUSTOMER1_EXPECTED_OVERDRAFT_BALANCE_BEFORE_INTEREST_IN_PENNIES);
    System.out.println("Expected Overdraft Balance in pennies: " + CUSTOMER1_EXPECTED_OVERDRAFT_BALANCE_AFTER_INTEREST_IN_PENNIES);
    assertEquals(CUSTOMER1_EXPECTED_OVERDRAFT_BALANCE_AFTER_INTEREST_IN_PENNIES, (int)customer1Data.get("OverdraftBalance"));
  }

  /**
   * Verifies the transaction dispute feature on a reversal of a deposit that 
   * causes a customer to fall back into overdraft
   * 
   * 
   * Reversing a deposit that causes a customer to fall back into an overdraft balance 
   * greater than 0 should put the customer back into the original overdraft balance. 
   * The 2% interest rate should not be re-applied.
   * 
   * @throws SQLException
   * @throws ScriptException
   * @throws InterruptedException
   */
  @Test
  public void testReverseDepositThatRepaysOverdraft() throws SQLException, ScriptException, InterruptedException {
    // initialize customer1 with a balance of $50 represented as pennies in the DB.
    // No overdraft or numFraudReversals.
    double CUSTOMER1_BALANCE = 0;
    int CUSTOMER1_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_BALANCE);
    int CUSTOMER1_OVERDRAFT_BALANCE_IN_PENNIES = 50;
    int CUSTOMER1_NUM_FRAUD_REVERSALS = 0;
    MvcControllerIntegTestHelpers.addCustomerToDB(dbDelegate, 
                                                  CUSTOMER1_ID, 
                                                  CUSTOMER1_PASSWORD, 
                                                  CUSTOMER1_FIRST_NAME, 
                                                  CUSTOMER1_LAST_NAME, 
                                                  CUSTOMER1_BALANCE_IN_PENNIES, 
                                                  CUSTOMER1_OVERDRAFT_BALANCE_IN_PENNIES,
                                                  CUSTOMER1_NUM_FRAUD_REVERSALS
                                                  );
    
    // Prepare Deposit Form to Deposit $100 to customer 1's account.
    double CUSTOMER1_AMOUNT_TO_DEPOSIT = 100; // user input is in dollar amount, not pennies.
    User customer1DepositFormInputs = new User();
    customer1DepositFormInputs.setUsername(CUSTOMER1_ID);
    customer1DepositFormInputs.setPassword(CUSTOMER1_PASSWORD);
    customer1DepositFormInputs.setAmountToDeposit(CUSTOMER1_AMOUNT_TO_DEPOSIT);

    // send request to the Deposit Form's POST handler in MvcController
    controller.submitDeposit(customer1DepositFormInputs);
 
    // sleep for 1 second to ensure the timestamps of Deposit, Withdraw, and Reversal are different (and sortable) in TransactionHistory table
    Thread.sleep(1000);

    // Prepare Reversal Form to reverse the Deposit
    User customer1ReversalFormInputs = customer1DepositFormInputs;
    customer1ReversalFormInputs.setNumTransactionsAgo(1); // reverse the first transaction

    // send Dispute request
    controller.submitDispute(customer1ReversalFormInputs);

    // fetch updated customer1 data from the DB
     List<Map<String,Object>> customersTableData = jdbcTemplate.queryForList("SELECT * FROM Customers;");
     customersTableData = jdbcTemplate.queryForList("SELECT * FROM Customers;");
     Map<String,Object> customer1Data = customersTableData.get(0);

    // verfiy that overdraft balance does not apply extra 2% interest after dispute
    assertEquals(CUSTOMER1_OVERDRAFT_BALANCE_IN_PENNIES, (int)customer1Data.get("OverdraftBalance"));
  }

 /**
   * This test verifies that a simple transfer of $100 from Customer1 to Customer2 will take place. Customer1's balance will be
   * initialized to $1000, and Customer2's balance will be $500. 
   * 
   * After a successful transfer, Customer1's balance should reflect a $900 balance, and Customer2's balance should be $600. 
   * 
   * @throws SQLException
   */
  @Test
  public void testTransfer() throws SQLException, ScriptException { 

    //Initialize customer1 with a balance of $1000. Balance will be represented as pennies in DB.
    double CUSTOMER1_BALANCE = 1000;
    int CUSTOMER1_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_BALANCE);
    MvcControllerIntegTestHelpers.addCustomerToDB(dbDelegate, CUSTOMER1_ID, CUSTOMER1_PASSWORD, CUSTOMER1_FIRST_NAME, CUSTOMER1_LAST_NAME, CUSTOMER1_BALANCE_IN_PENNIES);

    //Initialize customer2 with a balance of $500. Balance will be represented as pennies in DB. 
    double CUSTOMER2_BALANCE = 500;
    int CUSTOMER2_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER2_BALANCE);
    MvcControllerIntegTestHelpers.addCustomerToDB(dbDelegate, CUSTOMER2_ID, CUSTOMER2_PASSWORD, CUSTOMER2_FIRST_NAME, CUSTOMER2_LAST_NAME, CUSTOMER2_BALANCE_IN_PENNIES);

    //Amount to transfer
    double TRANSFER_AMOUNT = 100;
    int TRANSFER_AMOUNT_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(TRANSFER_AMOUNT);

    //Initializing users for the transfer
    User CUSTOMER1 = new User();
    CUSTOMER1.setUsername(CUSTOMER1_ID);
    CUSTOMER1.setPassword(CUSTOMER1_PASSWORD);
    CUSTOMER1.setTransferRecipientID(CUSTOMER2_ID);
    CUSTOMER1.setAmountToTransfer(TRANSFER_AMOUNT);
    
    //Send the transfer request.
    String returnedPage = controller.submitTransfer(CUSTOMER1);
    
    //Fetch customer1 & customer2's data from DB
    List<Map<String, Object>> customer1SqlResult = jdbcTemplate.queryForList(String.format("SELECT * FROM Customers WHERE CustomerID='%s';", CUSTOMER1_ID));
    Map<String, Object> customer1Data = customer1SqlResult.get(0);

    List<Map<String, Object>> customer2SqlResult = jdbcTemplate.queryForList(String.format("SELECT * FROM Customers WHERE CustomerID='%s';", CUSTOMER2_ID));
    Map<String, Object> customer2Data = customer2SqlResult.get(0);
   
    //Verify that customer1's balance decreased by $100. 
    assertEquals((CUSTOMER1_BALANCE_IN_PENNIES - TRANSFER_AMOUNT_IN_PENNIES), (int)customer1Data.get("Balance"));

    //Verify that customer2's balance increased by $100.
    assertEquals((CUSTOMER2_BALANCE_IN_PENNIES + TRANSFER_AMOUNT_IN_PENNIES), (int)customer2Data.get("Balance"));

    //Check that transfer request goes through.
    assertEquals("account_info", returnedPage);
  }

  /**
   * This test is written to check the scenario where a transfer between the sender and the recipient occurs when
   * the recipient is in overdraft. The sender will have $1000 in the bank account and will send $100. The recipient
   * will have an overdraft balance of $101. Receiving $100 should make the sender have an updated overdraft balance of
   * $1. 
   * 
   * The sender's balance should decrease by the transfer amount.
   */
  @Test
  public void testTransferPaysOffOverdraftBalance() throws SQLException, ScriptException { 
    
    //Initialize customer1 with a balance of $1000. Balance will be represented as pennies in DB.
    double CUSTOMER1_BALANCE = 1000;
    int CUSTOMER1_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_BALANCE);
    MvcControllerIntegTestHelpers.addCustomerToDB(dbDelegate, CUSTOMER1_ID, CUSTOMER1_PASSWORD, CUSTOMER1_FIRST_NAME, CUSTOMER1_LAST_NAME, CUSTOMER1_BALANCE_IN_PENNIES);

    //Initialize customer2 with a balance of $0 and Overdraft balance of $101. Balance will be represented as pennies in DB. 
    double CUSTOMER2_BALANCE = 0;
    int CUSTOMER2_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER2_BALANCE);
    double CUSTOMER2_OVERDRAFT_BALANCE = 101.0;
    int CUSTOMER2_OVERDRAFT_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER2_OVERDRAFT_BALANCE);
    int CUSTOMER2_NUM_FRAUD_REVERSALS = 0;
    MvcControllerIntegTestHelpers.addCustomerToDB(dbDelegate, CUSTOMER2_ID, CUSTOMER2_PASSWORD, CUSTOMER2_FIRST_NAME, CUSTOMER2_LAST_NAME, CUSTOMER2_BALANCE_IN_PENNIES, CUSTOMER2_OVERDRAFT_BALANCE_IN_PENNIES, CUSTOMER2_NUM_FRAUD_REVERSALS);

    //Amount to transfer
    double TRANSFER_AMOUNT = 100;
    int TRANSFER_AMOUNT_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(TRANSFER_AMOUNT);

    //Initializing users for the transfer
    User CUSTOMER1 = new User();
    CUSTOMER1.setUsername(CUSTOMER1_ID);
    CUSTOMER1.setPassword(CUSTOMER1_PASSWORD);

    CUSTOMER1.setTransferRecipientID(CUSTOMER2_ID);
    CUSTOMER1.setAmountToTransfer(TRANSFER_AMOUNT);

    //Send the transfer request.
    String returnedPage = controller.submitTransfer(CUSTOMER1);

    //fetch customer1 & customer2's data from DB
    List<Map<String,Object>> customer1SqlResult = jdbcTemplate.queryForList(String.format("SELECT * FROM Customers WHERE CustomerID='%s';", CUSTOMER1_ID));
    Map<String, Object> customer1Data = customer1SqlResult.get(0);

    List<Map<String,Object>> customer2SqlResult = jdbcTemplate.queryForList(String.format("SELECT * FROM Customers WHERE CustomerID='%s';", CUSTOMER2_ID));
    Map<String, Object> customer2Data = customer2SqlResult.get(0);

    //Verify that customer1's balance decreased by $100. 
    assertEquals((CUSTOMER1_BALANCE_IN_PENNIES - TRANSFER_AMOUNT_IN_PENNIES), (int)customer1Data.get("Balance"));

    //Verify that customer2's overdraft balance decreased by $100.
    assertEquals((CUSTOMER2_OVERDRAFT_BALANCE_IN_PENNIES - TRANSFER_AMOUNT_IN_PENNIES), (int)customer2Data.get("OverdraftBalance"));

    //Check that transfer request goes through.
    assertEquals("account_info", returnedPage);
}

/**
 * This test will test a scenario where the sender sends the recipient (who currently has an overdraft balance) an amount
 * that clears the recipient's overdraft balance and deposits the remainder. 
 * 
 * The sender will be initialized with $1000, and the recipient will have an overdraft balance of $100. Due to applied interest,
 * the recipient will have an overdraft balance of $102. The sender will send $150, so the recipient's balance should reflect $48
 * after the transfer.
 * 
 * @throws SQLException
 * @throws ScriptException
 */
@Test
public void testTransferPaysOverdraftAndDepositsRemainder() throws SQLException, ScriptException { 

    //Initialize customer1 with a balance of $1000. Balance will be represented as pennies in DB.
    double CUSTOMER1_BALANCE = 1000;
    int CUSTOMER1_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_BALANCE);
    MvcControllerIntegTestHelpers.addCustomerToDB(dbDelegate, CUSTOMER1_ID, CUSTOMER1_PASSWORD, CUSTOMER1_FIRST_NAME, CUSTOMER1_LAST_NAME, CUSTOMER1_BALANCE_IN_PENNIES);

    double CUSTOMER2_BALANCE = 0;
    int CUSTOMER2_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER2_BALANCE);
    double CUSTOMER2_OVERDRAFT_BALANCE = 100.0;
    int CUSTOMER2_OVERDRAFT_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER2_OVERDRAFT_BALANCE);
    int CUSTOMER2_NUM_FRAUD_REVERSALS = 0;
    MvcControllerIntegTestHelpers.addCustomerToDB(dbDelegate, CUSTOMER2_ID, CUSTOMER2_PASSWORD, CUSTOMER2_FIRST_NAME, CUSTOMER2_LAST_NAME, CUSTOMER2_BALANCE_IN_PENNIES, CUSTOMER2_OVERDRAFT_BALANCE_IN_PENNIES, CUSTOMER2_NUM_FRAUD_REVERSALS);

    //Transfer $150 from sender's account to recipient's account.
    double TRANSFER_AMOUNT = 150;
    int TRANSFER_AMOUNT_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(TRANSFER_AMOUNT);

    //Initializing users for the transfer
    User CUSTOMER1 = new User();
    CUSTOMER1.setUsername(CUSTOMER1_ID);
    CUSTOMER1.setPassword(CUSTOMER1_PASSWORD);

    CUSTOMER1.setTransferRecipientID(CUSTOMER2_ID);
    CUSTOMER1.setAmountToTransfer(TRANSFER_AMOUNT);

    //Send the transfer request.
    String returnedPage = controller.submitTransfer(CUSTOMER1);

    //fetch customer1 & customer2's data from DB
    List<Map<String,Object>> customer1SqlResult = jdbcTemplate.queryForList(String.format("SELECT * FROM Customers WHERE CustomerID='%s';", CUSTOMER1_ID));
    Map<String, Object> customer1Data = customer1SqlResult.get(0);

    List<Map<String,Object>> customer2SqlResult = jdbcTemplate.queryForList(String.format("SELECT * FROM Customers WHERE CustomerID='%s';", CUSTOMER2_ID));
    Map<String, Object> customer2Data = customer2SqlResult.get(0);

    //Verify that customer1's balance decreased by $100. 
    assertEquals((CUSTOMER1_BALANCE_IN_PENNIES - TRANSFER_AMOUNT_IN_PENNIES), (int)customer1Data.get("Balance"));

    //Verify that customer2's overdraft balance is now $0.
    assertEquals(0, (int)customer2Data.get("OverdraftBalance"));

    //Verify that customer2's balance reflects a positive amount due to a remainder being leftover after the transfer amount - overdraft balance.
    int CUSTOMER2_EXPECTED_BALANCE_IN_PENNIES = TRANSFER_AMOUNT_IN_PENNIES - CUSTOMER2_OVERDRAFT_BALANCE_IN_PENNIES;
    assertEquals(CUSTOMER2_EXPECTED_BALANCE_IN_PENNIES, (int)customer2Data.get("Balance"));

    //Check that transfer request goes through.
    assertEquals("account_info", returnedPage);
  }

  /**
   * Enum for {@link CryptoTransactionTester}
   */
  @AllArgsConstructor
  enum CryptoTransactionTestType {
    BUY("Buy", "CryptoBuy"),
    SELL("Sell", "CryptoSell");
    final String cryptoHistoryActionName;
    final String transactionHistoryActionName;
  }

  /**
   * Class to represent transaction properties for {@link CryptoTransactionTester}
   */
  @Builder
  static class CryptoTransaction {
    /**
     * The name of the cryptocurrency for the transaction
     */
    final String cryptoName;

    /**
     * The price of the cryptocurrency in dollars at the time of the transaction
     */
    final double cryptoPrice;

    /**
     * The expected (cash) balance of the user after the transaction
     */
    final double expectedEndingBalanceInDollars;

    /**
     * The (cash) overdraft balance of the user before the transaction takes place
     */
    @Builder.Default
    final double initialOverdraftBalanceInDollars = 0.0;

    /**
     * The expected ending overdraft balance of the user
     */
    @Builder.Default
    final double expectedEndingOverdraftBalanceInDollars = 0.0;

    /**
     * The expected ending crypto balance of the user
     */
    @Builder.Default
    double expectedEndingCryptoBalance = 0.0;

    /**
     * The amount of cryptocurrency to buy (in units of the cryptocurrency)
     */
    final double cryptoAmountToTransact;

    /**
     * Whether the transaction is made with the correct password
     */
    @Builder.Default
    final boolean validPassword = true;

    /**
     * Whether the transaction is expected to succeed with the supplied parameters
     */
    final boolean shouldSucceed;

    /**
     * Whether the transaction should add to the overdraft logs
     */
    @Builder.Default
    final boolean overdraftTransaction = false;

    /**
     * The type of the transaction (buy or sell)
     */
    final CryptoTransactionTestType cryptoTransactionTestType;
  }

  /**
   * Helper class to test crypto buying and selling in with various parameters.
   * This does several checks to see if the transaction took place correctly.
   */
  @Builder
  static class CryptoTransactionTester {

    /**
     * The initial (cash) balance of the user
     */
    final double initialBalanceInDollars;

    /**
     * The (cash) overdraft balance of the user
     */
    @Builder.Default
    final double initialOverdraftBalanceInDollars = 0.0;

    /**
     * The initial cryptocurrency balance of the user in units of cryptocurrency
     * Map of cryptocurrency name to initial balance
     */
    @Builder.Default
    final Map<String, Double> initialCryptoBalance = Collections.emptyMap();

    void initialize() throws ScriptException {
      int balanceInPennies = MvcControllerIntegTestHelpers.convertDollarsToPennies(initialBalanceInDollars);
      MvcControllerIntegTestHelpers.addCustomerToDB(dbDelegate, CUSTOMER1_ID, CUSTOMER1_PASSWORD, CUSTOMER1_FIRST_NAME,
              CUSTOMER1_LAST_NAME, balanceInPennies, MvcControllerIntegTestHelpers.convertDollarsToPennies(initialOverdraftBalanceInDollars), 0);
      for (Map.Entry<String, Double> initialBalance : initialCryptoBalance.entrySet()) {
        MvcControllerIntegTestHelpers.setCryptoBalance(dbDelegate, CUSTOMER1_ID, initialBalance.getKey(), initialBalance.getValue());
      }
    }

    // Counter for number of transactions completed by this tester
    private int numTransactions = 0;

    // Counter for the number of overdraft transaction completed by this tester
    private int numOverdraftTransactions = 0;

    /**
     * Attempts a transaction
     */
    void test(CryptoTransaction transaction) {
      User user = new User();
      user.setUsername(CUSTOMER1_ID);
      if (transaction.validPassword) {
        user.setPassword(CUSTOMER1_PASSWORD);
      } else {
        user.setPassword("wrong_password");
      }
      user.setWhichCryptoToBuy(transaction.cryptoName);


      // Mock the price of the cryptocurrency
      Mockito.when(cryptoPriceClient.getCurrentCryptoValue(transaction.cryptoName)).thenReturn(transaction.cryptoPrice);

      // attempt transaction
      LocalDateTime cryptoTransactionTime = MvcControllerIntegTestHelpers.fetchCurrentTimeAsLocalDateTimeNoMilliseconds();
      String returnedPage;
      if (transaction.cryptoTransactionTestType == CryptoTransactionTestType.BUY) {
        user.setAmountToBuyCrypto(transaction.cryptoAmountToTransact);
        returnedPage = controller.buyCrypto(user);
      } else {
        user.setAmountToSellCrypto(transaction.cryptoAmountToTransact);
        returnedPage = controller.sellCrypto(user);
      }

      // check the crypto balance
      try {
        double endingCryptoBalance = jdbcTemplate.queryForObject("SELECT CryptoAmount FROM CryptoHoldings WHERE CustomerID=? AND CryptoName=?", BigDecimal.class, CUSTOMER1_ID, transaction.cryptoName).doubleValue();
        assertEquals(transaction.expectedEndingCryptoBalance, endingCryptoBalance);
      } catch (EmptyResultDataAccessException e) {
        assertEquals(transaction.expectedEndingCryptoBalance, 0);
      }

      // check the cash balance
      assertEquals(MvcControllerIntegTestHelpers.convertDollarsToPennies(transaction.expectedEndingBalanceInDollars),
              jdbcTemplate.queryForObject("SELECT Balance FROM Customers WHERE CustomerID=?", Integer.class, CUSTOMER1_ID));

      // check the overdraft balance
      assertEquals(MvcControllerIntegTestHelpers.convertDollarsToPennies(transaction.expectedEndingOverdraftBalanceInDollars),
              jdbcTemplate.queryForObject("SELECT OverdraftBalance FROM Customers WHERE CustomerID=?", Integer.class, CUSTOMER1_ID));

      if (!transaction.shouldSucceed) {
        // verify no transaction took place
        assertEquals("welcome", returnedPage);
        assertEquals(numTransactions, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TransactionHistory;", Integer.class));
        assertEquals(numTransactions, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM CryptoHistory;", Integer.class));
        assertEquals(numOverdraftTransactions, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM OverdraftLogs;", Integer.class));
      } else {
        assertEquals("account_info", returnedPage);

        // check transaction logs
        assertEquals(numTransactions + 1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TransactionHistory;", Integer.class));
        List<Map<String, Object>> transactionHistoryTableData = jdbcTemplate.queryForList("SELECT * FROM TransactionHistory ORDER BY Timestamp DESC;");
        Map<String, Object> customer1TransactionLog = transactionHistoryTableData.get(0);
        int expectedCryptoValueInPennies = MvcControllerIntegTestHelpers.convertDollarsToPennies(transaction.cryptoPrice * transaction.cryptoAmountToTransact);
        MvcControllerIntegTestHelpers.checkTransactionLog(customer1TransactionLog, cryptoTransactionTime, CUSTOMER1_ID, transaction.cryptoTransactionTestType.transactionHistoryActionName, expectedCryptoValueInPennies);

        // check crypto logs
        assertEquals(numTransactions + 1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM CryptoHistory;", Integer.class));
        List<Map<String, Object>> cryptoHistoryTableData = jdbcTemplate.queryForList("SELECT * FROM CryptoHistory ORDER BY Timestamp DESC;");
        Map<String, Object> customer1CryptoLog = cryptoHistoryTableData.get(0);
        MvcControllerIntegTestHelpers.checkCryptoLog(customer1CryptoLog, cryptoTransactionTime, CUSTOMER1_ID, transaction.cryptoTransactionTestType.cryptoHistoryActionName,
                transaction.cryptoName, transaction.cryptoAmountToTransact);

        // check overdraft logs (if applicable)
        if (transaction.overdraftTransaction) {
          assertEquals(numOverdraftTransactions + 1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM OverdraftLogs;", Integer.class));
          List<Map<String, Object>> overdraftLogTableData = jdbcTemplate.queryForList("SELECT * FROM OverdraftLogs ORDER BY Timestamp DESC;");
          Map<String, Object> customer1OverdraftLog = overdraftLogTableData.get(0);
          MvcControllerIntegTestHelpers.checkOverdraftLog(customer1OverdraftLog, cryptoTransactionTime, CUSTOMER1_ID, expectedCryptoValueInPennies,
                  MvcControllerIntegTestHelpers.convertDollarsToPennies(transaction.initialOverdraftBalanceInDollars),
                  MvcControllerIntegTestHelpers.convertDollarsToPennies(transaction.expectedEndingOverdraftBalanceInDollars));
          numOverdraftTransactions++;
        } else {
          assertEquals(numOverdraftTransactions, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM OverdraftLogs;", Integer.class));
        }

        numTransactions++;

      }
    }
  }

  /**
   * Test that no crypto buy transaction occurs when the user password is incorrect
   */
  @Test
  public void testCryptoBuyInvalidPassword() throws ScriptException {
    CryptoTransactionTester cryptoTransactionTester = CryptoTransactionTester.builder()
            .initialBalanceInDollars(1000)
            .build();

    cryptoTransactionTester.initialize();

    CryptoTransaction cryptoTransaction = CryptoTransaction.builder()
            .expectedEndingBalanceInDollars(1000)
            .cryptoPrice(1000)
            .cryptoAmountToTransact(0.1)
            .cryptoName("ETH")
            .validPassword(false)
            .cryptoTransactionTestType(CryptoTransactionTestType.BUY)
            .shouldSucceed(false)
            .build();
    cryptoTransactionTester.test(cryptoTransaction);
  }

  /**
   * Test that no crypto sell transaction occurs when the user password is incorrect
   */
  @Test
  public void testCryptoSellInvalidPassword() throws ScriptException {
    CryptoTransactionTester cryptoTransactionTester = CryptoTransactionTester.builder()
            .initialBalanceInDollars(1000)
            .build();

    cryptoTransactionTester.initialize();

    CryptoTransaction cryptoTransaction = CryptoTransaction.builder()
            .expectedEndingBalanceInDollars(1000)
            .cryptoPrice(1000)
            .cryptoAmountToTransact(0.1)
            .validPassword(false)
            .cryptoName("ETH")
            .cryptoTransactionTestType(CryptoTransactionTestType.SELL)
            .shouldSucceed(false)
            .build();
    cryptoTransactionTester.test(cryptoTransaction);
  }

  /**
   * Test simple buying of cryptocurrency
   */
  @Test
  public void testCryptoBuySimple() throws ScriptException {
    CryptoTransactionTester cryptoTransactionTester = CryptoTransactionTester.builder()
            .initialBalanceInDollars(1000)
            .initialCryptoBalance(Collections.singletonMap("ETH", 0.0))
            .build();

    cryptoTransactionTester.initialize();

    CryptoTransaction cryptoTransaction = CryptoTransaction.builder()
            .expectedEndingBalanceInDollars(900)
            .expectedEndingCryptoBalance(0.1)
            .cryptoPrice(1000)
            .cryptoAmountToTransact(0.1)
            .cryptoName("ETH")
            .cryptoTransactionTestType(CryptoTransactionTestType.BUY)
            .shouldSucceed(true)
            .build();
    cryptoTransactionTester.test(cryptoTransaction);
  }

  /**
   * Test simple selling of cryptocurrency
   */
  @Test
  public void testCryptoSellSimple() throws ScriptException {
    CryptoTransactionTester cryptoTransactionTester = CryptoTransactionTester.builder()
            .initialBalanceInDollars(1000)
            .initialCryptoBalance(Collections.singletonMap("ETH", 0.1))
            .build();

    cryptoTransactionTester.initialize();

    CryptoTransaction cryptoTransaction = CryptoTransaction.builder()
            .expectedEndingBalanceInDollars(1100)
            .expectedEndingCryptoBalance(0)
            .cryptoPrice(1000)
            .cryptoAmountToTransact(0.1)
            .cryptoName("ETH")
            .cryptoTransactionTestType(CryptoTransactionTestType.SELL)
            .shouldSucceed(true)
            .build();
    cryptoTransactionTester.test(cryptoTransaction);
  }

  /**
   * Test buying of cryptocurrency with an insufficient balance does not invoke a transaction
   */
  @Test
  public void testCryptoBuyInsufficientBalance() throws ScriptException {
    CryptoTransactionTester cryptoTransactionTester = CryptoTransactionTester.builder()
            .initialBalanceInDollars(1000)
            .build();

    cryptoTransactionTester.initialize();

    CryptoTransaction cryptoTransaction = CryptoTransaction.builder()
            .expectedEndingBalanceInDollars(1000)
            .cryptoPrice(1000)
            .cryptoAmountToTransact(10)
            .cryptoName("ETH")
            .cryptoTransactionTestType(CryptoTransactionTestType.BUY)
            .shouldSucceed(false)
            .build();
    cryptoTransactionTester.test(cryptoTransaction);
  }

  /**
   * Test that buying a negative amount of cryptocurrency does not invoke a transaction
   */
  @Test
  public void testCryptoBuyNegativeAmount() throws ScriptException {
    CryptoTransactionTester cryptoTransactionTester = CryptoTransactionTester.builder()
            .initialBalanceInDollars(1000)
            .build();

    cryptoTransactionTester.initialize();

    CryptoTransaction cryptoTransaction = CryptoTransaction.builder()
            .expectedEndingBalanceInDollars(1000)
            .cryptoPrice(1000)
            .cryptoAmountToTransact(-0.1)
            .cryptoName("ETH")
            .cryptoTransactionTestType(CryptoTransactionTestType.BUY)
            .shouldSucceed(false)
            .build();
    cryptoTransactionTester.test(cryptoTransaction);
  }

  /**
   * Test that selling a negative amount of cryptocurrency does not invoke a transaction
   */
  @Test
  public void testCryptoSellNegativeAmount() throws ScriptException {
    CryptoTransactionTester cryptoTransactionTester = CryptoTransactionTester.builder()
            .initialBalanceInDollars(1000)
            .initialCryptoBalance(Collections.singletonMap("ETH", 0.1))
            .build();

    cryptoTransactionTester.initialize();

    CryptoTransaction cryptoTransaction = CryptoTransaction.builder()
            .expectedEndingBalanceInDollars(1000)
            .expectedEndingCryptoBalance(0.1)
            .cryptoPrice(1000)
            .cryptoAmountToTransact(-0.1)
            .cryptoName("ETH")
            .cryptoTransactionTestType(CryptoTransactionTestType.SELL)
            .shouldSucceed(false)
            .build();
    cryptoTransactionTester.test(cryptoTransaction);
  }

  /**
   * Test that no buying should take place when user is under overdraft
   */
  @Test
  public void testCryptoBuyOverdraft() throws ScriptException {
    CryptoTransactionTester cryptoTransactionTester = CryptoTransactionTester.builder()
            .initialBalanceInDollars(1000)
            .initialOverdraftBalanceInDollars(100)
            .build();

    cryptoTransactionTester.initialize();

    CryptoTransaction cryptoTransaction = CryptoTransaction.builder()
            .expectedEndingBalanceInDollars(1000)
            .expectedEndingOverdraftBalanceInDollars(100)
            .cryptoPrice(1000)
            .cryptoAmountToTransact(0.1)
            .cryptoName("ETH")
            .cryptoTransactionTestType(CryptoTransactionTestType.BUY)
            .overdraftTransaction(true)
            .shouldSucceed(false)
            .build();
    cryptoTransactionTester.test(cryptoTransaction);
  }

  /**
   * Test that selling cryptocurrency first pays off overdraft balance
   */
  @Test
  public void testCryptoSellOverdraft() throws ScriptException {
    CryptoTransactionTester cryptoTransactionTester = CryptoTransactionTester.builder()
            .initialBalanceInDollars(1000)
            .initialOverdraftBalanceInDollars(50)
            .initialCryptoBalance(Collections.singletonMap("ETH", 0.15))
            .build();

    cryptoTransactionTester.initialize();

    CryptoTransaction cryptoTransaction = CryptoTransaction.builder()
            .initialOverdraftBalanceInDollars(50)
            .expectedEndingBalanceInDollars(1050)
            .expectedEndingCryptoBalance(0.05)
            .expectedEndingOverdraftBalanceInDollars(0)
            .cryptoPrice(1000)
            .cryptoAmountToTransact(0.1)
            .cryptoName("ETH")
            .cryptoTransactionTestType(CryptoTransactionTestType.SELL)
            .overdraftTransaction(true)
            .shouldSucceed(true)
            .build();
    cryptoTransactionTester.test(cryptoTransaction);
  }

  /**
   * Test that no buy transaction occurs when the cryptocurrency price cannot be obtained
   */
  @Test
  public void testCryptoBuyInvalidPrice() throws ScriptException {
    CryptoTransactionTester cryptoTransactionTester = CryptoTransactionTester.builder()
            .initialBalanceInDollars(1000)
            .initialCryptoBalance(Collections.singletonMap("ETH", 0.0))
            .build();

    cryptoTransactionTester.initialize();

    CryptoTransaction cryptoTransaction = CryptoTransaction.builder()
            .expectedEndingBalanceInDollars(1000)
            .expectedEndingCryptoBalance(0)
            .cryptoPrice(-1)
            .cryptoAmountToTransact(0.1)
            .cryptoName("ETH")
            .cryptoTransactionTestType(CryptoTransactionTestType.BUY)
            .shouldSucceed(false)
            .build();
    cryptoTransactionTester.test(cryptoTransaction);
  }

  /**
   * Test that no sell transaction occurs when the cryptocurrency price cannot be obtained
   */
  @Test
  public void testCryptoSellInvalidPrice() throws ScriptException {
    CryptoTransactionTester cryptoTransactionTester = CryptoTransactionTester.builder()
            .initialBalanceInDollars(1000)
            .initialCryptoBalance(Collections.singletonMap("ETH", 0.1))
            .build();

    cryptoTransactionTester.initialize();

    CryptoTransaction cryptoTransaction = CryptoTransaction.builder()
            .expectedEndingBalanceInDollars(1000)
            .expectedEndingCryptoBalance(0.1)
            .cryptoPrice(-1)
            .cryptoAmountToTransact(0.1)
            .cryptoName("ETH")
            .cryptoTransactionTestType(CryptoTransactionTestType.SELL)
            .shouldSucceed(false)
            .build();
    cryptoTransactionTester.test(cryptoTransaction);
  }


  /**
   * Verifies the simplest crypto deposit case.
   * The customer's Balance in the Customers table should be increased,
   * and the Deposit should be logged in the TransactionHistory table.
   * 
   * Assumes that the customer's account is in the simplest state
   * (not in overdraft, account is not frozen due to too many transaction disputes, etc.)
   * 
   * @throws SQLException
   * @throws ScriptException
   */
  // @Test
  // public void testSimpleCryptoDeposit() throws SQLException, ScriptException {
  //   System.out.println("testSimpleCryptoDeposit");
  //   // initialize customer1 with a balance of $123.45 (to make sure this works for non-whole dollar amounts). represented as pennies in the DB.
  //   double CUSTOMER1_BALANCE = 123.45;
  //   int CUSTOMER1_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_BALANCE);
  //   MvcControllerIntegTestHelpers.addCustomerToDB(dbDelegate, CUSTOMER1_ID, CUSTOMER1_PASSWORD, CUSTOMER1_FIRST_NAME, CUSTOMER1_LAST_NAME, CUSTOMER1_BALANCE_IN_PENNIES);

  //   // Prepare Deposit Form to buy $12.34 worth of ETH
  //   double CUSTOMER1_AMOUNT_TO_Buy_Crypto = 12.34; // user input is in dollar amount, not pennies.
  //   User customer1CryptoBuyFormInputs = new User();
  //   customer1CryptoBuyFormInputs.setUsername(CUSTOMER1_ID);
  //   customer1CryptoBuyFormInputs.setPassword(CUSTOMER1_PASSWORD);
  //   customer1CryptoBuyFormInputs.setBalance(CUSTOMER1_BALANCE);
  //   customer1CryptoBuyFormInputs.setAmountToBuyCrypto(CUSTOMER1_AMOUNT_TO_Buy_Crypto);

  //   // verify that there are no logs in TransactionHistory table before Deposit
  //   assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TransactionHistory;", Integer.class));

  //   // store timestamp of when crypto buy request is sent to verify timestamps in the Crypto History table later
  //   LocalDateTime timeWhenCryptoBuyRequestSent = MvcControllerIntegTestHelpers.fetchCurrentTimeAsLocalDateTimeNoMilliseconds();
  //   System.out.println("Timestamp when Buy Request is sent: " + timeWhenCryptoBuyRequestSent);

  //   // send request to the crypto buy Form's POST handler in MvcController
  //   controller.buyCrypto(customer1CryptoBuyFormInputs);

  //   // fetch updated data from the DB
  //   List<Map<String,Object>> customersTableData = jdbcTemplate.queryForList("SELECT * FROM Customers;");
  //   List<Map<String,Object>> cryptoHistoryTableData = jdbcTemplate.queryForList("SELECT * FROM CryptoHistory;");
  
  //   for(Map<String,Object> m: cryptoHistoryTableData) {
  //     for(Object obj: m.keySet()) {
  //       System.out.println("Inside: " + m.get(obj));
  //     }
  //     // System.out.println(m.keySet());
  //   }
  //   // verify that customer1's data is still the only data populated in Customers table
  //   assertEquals(1, customersTableData.size());
  //   Map<String,Object> customer1Data = customersTableData.get(0);
  //   assertEquals(CUSTOMER1_ID, (String)customer1Data.get("CustomerID"));

  //   /*
  //   123.45 - 12.34 = 111.11
  //   111.11 + 1.24 = 112.35
  //   111.11 - 1.24 = 109.87
  //   */
  //   // verify customer balance was decreased by $12.34 +- epsilon
  //   double epsilon = 1.24;
  //   int EPSILON_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(epsilon);
  //   double CUSTOMER1_EXPECTED_FINAL_BALANCE = CUSTOMER1_BALANCE - CUSTOMER1_AMOUNT_TO_Buy_Crypto;
  //   int CUSTOMER1_EXPECTED_FINAL_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_EXPECTED_FINAL_BALANCE);
  //   System.out.println("CUSTOMER1_EXPECTED_FINAL_BALANCE_IN_PENNIES: " + CUSTOMER1_EXPECTED_FINAL_BALANCE_IN_PENNIES);
  //   System.out.println("((int)customer1Data.get(Balance): " + (int)customer1Data.get("Balance"));
  //   System.out.println("((int)customer1Data.get(Balance) + EPSILON_PENNIES) : " + ((int)customer1Data.get("Balance") + EPSILON_PENNIES));
  //   System.out.println("((int)customer1Data.get(Balance) - EPSILON_PENNIES) : " + ((int)customer1Data.get("Balance") - EPSILON_PENNIES));
  //   System.out.println("EPSILON_PENNIES : " + EPSILON_PENNIES);

  //   assertTrue(CUSTOMER1_EXPECTED_FINAL_BALANCE_IN_PENNIES <= ((int)customer1Data.get("Balance") + EPSILON_PENNIES));
  //   assertTrue(CUSTOMER1_EXPECTED_FINAL_BALANCE_IN_PENNIES >= ((int)customer1Data.get("Balance") - EPSILON_PENNIES));
  //   // assertEquals(CUSTOMER1_EXPECTED_FINAL_BALANCE_IN_PENNIES, (int)customer1Data.get("Balance"));

  //   // verify that the Deposit is the only log in TransactionHistory table
  //   assertEquals(1, cryptoHistoryTableData.size());
    
  //   // verify that the Deposit's details are accurately logged in the Crypto History table
  //   Map<String,Object> customer1TransactionLog = cryptoHistoryTableData.get(0);
  //   for(String str: customer1TransactionLog.keySet()) {
  //     System.out.println("column: " +str);
  //     System.out.println("Log: " + customer1TransactionLog.get(str));
  //   }
  //     // System.out.println(m.keySet());
    
  //   int CUSTOMER1_AMOUNT_TO_Buy_Crypto_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_AMOUNT_TO_Buy_Crypto);
    
  //   // double currentEthPrice = MvcControllerIntegTestHelpers.getCurrentEthValue();
  //   // double EXPECTED_ETH_BALANCE = CUSTOMER1_EXPECTED_FINAL_BALANCE/ currentEthPrice;
  //   // double EXPECTED_ETH_EPSILON = epsilon / currentEthPrice;

  //   MvcControllerIntegTestHelpers.checkCryptoTransactionLog(customer1TransactionLog, timeWhenCryptoBuyRequestSent, CUSTOMER1_ID, "Buy", "ETH", CUSTOMER1_AMOUNT_TO_Buy_Crypto_IN_PENNIES, EPSILON_PENNIES);

  // }

  /**
   * Verifies the crypto deposit case that stops users from going into overdraft.
   * The customer's Balance should not change at all
   * 
   * Assumes that the customer's account is in the simplest state
   * (not in overdraft, account is not frozen due to too many transaction disputes, etc.)
   * 
   * @throws SQLException
   * @throws ScriptException
   */
  @Test
  public void testCryptoDepositImpossibleOverdraft() throws SQLException, ScriptException {
    // initialize customer1 with a balance of $123.45 (to make sure this works for non-whole dollar amounts). represented as pennies in the DB.
    double CUSTOMER1_BALANCE = 123.45;
    int CUSTOMER1_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_BALANCE);
    MvcControllerIntegTestHelpers.addCustomerToDB(dbDelegate, CUSTOMER1_ID, CUSTOMER1_PASSWORD, CUSTOMER1_FIRST_NAME, CUSTOMER1_LAST_NAME, CUSTOMER1_BALANCE_IN_PENNIES);

    // Prepare Deposit Form to buy $12.34 worth of ETH
    double CUSTOMER1_AMOUNT_TO_Buy_Crypto = 300; // user input is in dollar amount, not pennies.
    User customer1CryptoBuyFormInputs = new User();
    customer1CryptoBuyFormInputs.setUsername(CUSTOMER1_ID);
    customer1CryptoBuyFormInputs.setPassword(CUSTOMER1_PASSWORD);
    customer1CryptoBuyFormInputs.setBalance(CUSTOMER1_BALANCE);
    customer1CryptoBuyFormInputs.setAmountToBuyCrypto(CUSTOMER1_AMOUNT_TO_Buy_Crypto);

    // verify that there are no logs in TransactionHistory table before Deposit
    assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TransactionHistory;", Integer.class));

    // store timestamp of when crypto buy request is sent to verify timestamps in the Crypto History table later
    LocalDateTime timeWhenCryptoBuyRequestSent = MvcControllerIntegTestHelpers.fetchCurrentTimeAsLocalDateTimeNoMilliseconds();
    System.out.println("Timestamp when Buy Request is sent: " + timeWhenCryptoBuyRequestSent);

    // send request to the crypto buy Form's POST handler in MvcController
    controller.buyCrypto(customer1CryptoBuyFormInputs);

    // fetch updated data from the DB
    List<Map<String,Object>> customersTableData = jdbcTemplate.queryForList("SELECT * FROM Customers;");
    List<Map<String,Object>> cryptoHistoryTableData = jdbcTemplate.queryForList("SELECT * FROM CryptoHistory;");
    // verify that customer1's data is still the only data populated in Customers table
    assertEquals(1, customersTableData.size());
    Map<String,Object> customer1Data = customersTableData.get(0);
    assertEquals(CUSTOMER1_ID, (String)customer1Data.get("CustomerID"));
    // verify that the no crypto transaction passed
    assertEquals(0, cryptoHistoryTableData.size());
  }


  /*
  Verifies the possibility of a user having two types of cryptocurrencies.
  The customer's balance should decrease as a result of the cryptocurrency purchases
  Following, the customer's balance should increase following the result of selling SOL
  */
  @Test void testBuyEthBuySolSellSol() throws ScriptException{
    CryptoTransactionTester cryptoTransactionTester = CryptoTransactionTester.builder()
              .initialBalanceInDollars(1000)
              .initialCryptoBalance(Collections.singletonMap("ETH", 0.0))
              .build();
    
    cryptoTransactionTester.initialize();

    CryptoTransaction transactionBuyEth = CryptoTransaction.builder()
              .expectedEndingBalanceInDollars(900)
              .expectedEndingCryptoBalance(0.1)
              .cryptoPrice(1000)
              .cryptoAmountToTransact(0.1)
              .cryptoName("ETH")
              .cryptoTransactionTestType(CryptoTransactionTestType.BUY)
              .shouldSucceed(true)
              .build();

    cryptoTransactionTester.test(transactionBuyEth);
    // cryptoTransactionTester.test(transactionEth);

    CryptoTransaction transactionBuySol = CryptoTransaction.builder()
    .expectedEndingBalanceInDollars(800)
    .expectedEndingCryptoBalance(0.1)
    .cryptoPrice(1000)
    .cryptoAmountToTransact(0.1)
    .cryptoName("SOL")
    .cryptoTransactionTestType(CryptoTransactionTestType.BUY)
    .shouldSucceed(true)
    .build();
    cryptoTransactionTester.test(transactionBuySol);

    CryptoTransaction transactionSellSol = CryptoTransaction.builder()
    .expectedEndingBalanceInDollars(900)
    .expectedEndingCryptoBalance(0.0)
    .cryptoPrice(1000)
    .cryptoAmountToTransact(0.1)
    .cryptoName("SOL")
    .cryptoTransactionTestType(CryptoTransactionTestType.SELL)
    .shouldSucceed(true)
    .build();
    cryptoTransactionTester.test(transactionSellSol);
  }

  /*
    Verifies that a type of cryptocurrency that does not exist fails to be bought.
    Here BTC is used. 
    The user balance should not change
    The "welcome" page should be returned as a result.
  */
  @Test void testBuyInvalidCoinBTC() throws ScriptException {
    CryptoTransactionTester cryptoTransactionTester = CryptoTransactionTester.builder()
              .initialBalanceInDollars(1000)
              .initialCryptoBalance(Collections.singletonMap("ETH", 0.0))
              .build();
    
    cryptoTransactionTester.initialize();

    CryptoTransaction transactionBuyBtc = CryptoTransaction.builder()
              .expectedEndingBalanceInDollars(1000)
              .expectedEndingCryptoBalance(0.0)
              .cryptoPrice(1000)
              .cryptoAmountToTransact(0.1)
              .cryptoName("BTC")
              .cryptoTransactionTestType(CryptoTransactionTestType.BUY)
              .shouldSucceed(false)
              .build();

    cryptoTransactionTester.test(transactionBuyBtc);
  
  }

  /*
    Verifies that a type of cryptocurrency that does not exist fails to be sold.
    Here BTC is used. 
    The user balance should not change
    The "welcome" page should be returned as a result.
  */
  @Test void testSellInvalidCoinBTC() throws ScriptException {
    CryptoTransactionTester cryptoTransactionTester = CryptoTransactionTester.builder()
              .initialBalanceInDollars(1000)
              .initialCryptoBalance(Collections.singletonMap("ETH", 0.0))
              .build();
    
    cryptoTransactionTester.initialize();

    CryptoTransaction transactionSellBtc = CryptoTransaction.builder()
              .expectedEndingBalanceInDollars(1000)
              .expectedEndingCryptoBalance(0.0)
              .cryptoPrice(1000)
              .cryptoAmountToTransact(0.1)
              .cryptoName("BTC")
              .cryptoTransactionTestType(CryptoTransactionTestType.SELL)
              .shouldSucceed(false)
              .build();

    cryptoTransactionTester.test(transactionSellBtc);
  }

  /**
   * Verifies the simplest crypto sell case.
   * The customer's Balance in the Customers table should be increased,
   * and the sell should be logged in the CryptoHistory table.
   * 
   * Assumes that the customer's account is in the simplest state
   * (not already in overdraft, the withdraw does not put customer in overdraft,
   *  account is not frozen due to too many transaction disputes, etc.)
   * 
   * @throws SQLException
   * @throws ScriptException
   */
  // @Test
  // public void testSimpleCryptoSellNoOverdraft() throws SQLException, ScriptException {
  //   // initialize customer1 with a balance of $123.45 (to make sure this works for non-whole dollar amounts). represented as pennies in the DB.
  //   double CUSTOMER1_BALANCE = 123.45;
  //   int CUSTOMER1_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_BALANCE);
  //   MvcControllerIntegTestHelpers.addCustomerToDB(dbDelegate, CUSTOMER1_ID, CUSTOMER1_PASSWORD, CUSTOMER1_FIRST_NAME, CUSTOMER1_LAST_NAME, CUSTOMER1_BALANCE_IN_PENNIES);

  //   // Design: User submits how much in dollars they want to sell ETH
  //   // Then we convert that into pennies; Then convert from dollers to ETH
  //   double CUSTOMER1_BALANCE_CRYPTO = 123.45;
  //   int CUSTOMER1_BALANCE_CRYPTO_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_BALANCE_CRYPTO);
  //   double CUSTOMER1_ETH_BALANCE_AMT = CUSTOMER1_BALANCE_CRYPTO / MvcControllerIntegTestHelpers.getCurrentEthValue();
  //   /*Next: Add to the crypto table. Set up the cryptoSell . Check that account balance increased by val +- epsilon */
  //   String currentTime = SQL_DATETIME_FORMATTER.format(new java.util.Date()); // use same timestamp for all logs created by this transfer
  //   net.testudobank.TestudoBankRepository.insertRowToCryptoLogsTable(jdbcTemplate, CUSTOMER1_ID, currentTime, "Buy" , "ETH",  CUSTOMER1_ETH_BALANCE_AMT);
  //   net.testudobank.TestudoBankRepository.insertRowToCryptoHoldingsTable(jdbcTemplate, CUSTOMER1_ID, "ETH", CUSTOMER1_ETH_BALANCE_AMT);

  //   // Prepare Sell Form to sell $12.34 from customer 1's crypto account.
  //   double CUSTOMER1_AMOUNT_TO_SELL_CRYPTO = 12.34; // user input is in dollar amount, not pennies.
  //   User customer1CryptoSellFormInputs = new User();
  //   customer1CryptoSellFormInputs.setUsername(CUSTOMER1_ID);
  //   customer1CryptoSellFormInputs.setPassword(CUSTOMER1_PASSWORD);
  //   customer1CryptoSellFormInputs.setAmountToSellCrypto(CUSTOMER1_AMOUNT_TO_SELL_CRYPTO);
  //   // customer1CryptoWithdrawFormInputs.setAmountToWithdraw(CUSTOMER1_AMOUNT_TO_WITHDRAW_Crypto); // user input is in dollar amount, not pennies.

  //   // verify that there are no logs in TransactionHistory table before Withdraw
  //   assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TransactionHistory;", Integer.class));

  //   // store timestamp of when Withdraw request is sent to verify timestamps in the TransactionHistory table later
  //   LocalDateTime timeWhenCryptoSellRequestSent = MvcControllerIntegTestHelpers.fetchCurrentTimeAsLocalDateTimeNoMilliseconds();
  //   System.out.println("Timestamp when Sell Request is sent: " + timeWhenCryptoSellRequestSent);

  //   // send request to the deposit Form's POST handler in MvcController
  //   controller.sellCrypto(customer1CryptoSellFormInputs);

  //   // fetch updated data from the DB
  //   List<Map<String,Object>> customersTableData = jdbcTemplate.queryForList("SELECT * FROM Customers;");
  //   List<Map<String,Object>> transactionHistoryTableData = jdbcTemplate.queryForList("SELECT * FROM TransactionHistory;");
  //   List<Map<String,Object>> cryptoHistoryTableData = jdbcTemplate.queryForList("SELECT * FROM CryptoHistory;");

  //   // for(Map<String,Object> m: cryptoHistoryTableData) {
  //   //   for(Object obj: m.keySet()) {
  //   //     System.out.println("Inside" + obj);
  //   //   }
  //   // }
  //   // verify that customer1's data is still the only data populated in Customers table
  //   assertEquals(1, customersTableData.size());
  //   Map<String,Object> customer1Data = customersTableData.get(0);
  //   assertEquals(CUSTOMER1_ID, (String)customer1Data.get("CustomerID"));

  //   // verify customer balance was increased by $12.34 +- epsilon
  //   double epsilon = 1.24;
  //   int EPSILON_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(epsilon);
  //   double CUSTOMER1_EXPECTED_FINAL_BALANCE = CUSTOMER1_BALANCE + CUSTOMER1_AMOUNT_TO_SELL_CRYPTO;
  //   int CUSTOMER1_EXPECTED_FINAL_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_EXPECTED_FINAL_BALANCE);
  //   // assertTrue(CUSTOMER1_EXPECTED_FINAL_BALANCE_IN_PENNIES < ((int)customer1Data.get("Balance") + EPSILON_PENNIES)
  //   // && CUSTOMER1_EXPECTED_FINAL_BALANCE_IN_PENNIES > ((int)customer1Data.get("Balance") - EPSILON_PENNIES));
  //   assertTrue(CUSTOMER1_EXPECTED_FINAL_BALANCE_IN_PENNIES < ((int)customer1Data.get("Balance") + EPSILON_PENNIES));
  //   assertTrue(CUSTOMER1_EXPECTED_FINAL_BALANCE_IN_PENNIES > ((int)customer1Data.get("Balance") - EPSILON_PENNIES));
    
  //   // verify that the sell is the only log in TransactionHistory table
  //   assertEquals(1, transactionHistoryTableData.size());
  //   // verify that the sell is the only log in CryptoHistory table
  //   assertEquals(2, cryptoHistoryTableData.size()); // setup deposit && test sell

  //   // verify that the Deposit's details are accurately logged in the Crypto History table
  //   // Map<String,Object> customer1TransactionLog = cryptoHistoryTableData.get(0);
  //   // int CUSTOMER1_AMOUNT_TO_Sell_Crypto_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_AMOUNT_TO_SELL_CRYPTO);
  //   // MvcControllerIntegTestHelpers.checkCryptoTransactionLog(customer1TransactionLog, timeWhenCryptoSellRequestSent, CUSTOMER1_ID, "SELL", "ETH", CUSTOMER1_AMOUNT_TO_Sell_Crypto_IN_PENNIES);
  // }


   /**
   * Verifies the crypto sell case with an overdraft.
   * The sell should go pay off the debt
   * 
   * Assumes that the customer's account is in the simplest state
   * (not already in overdraft, the withdraw does not put customer in overdraft,
   *  account is not frozen due to too many transaction disputes, etc.)
   * 
   * @throws SQLException
   * @throws ScriptException
   */
  // @Test
  // public void testSimpleCryptoSellWithOverdraft() throws SQLException, ScriptException {
  //   // initialize customer1 with a balance of $123.45 (to make sure this works for non-whole dollar amounts). represented as pennies in the DB.
  //   double CUSTOMER1_BALANCE = 123.45;
  //   int CUSTOMER1_BALANCE_IN_PENNIES = MvcControllerIntegTestHelpers.convertDollarsToPennies(CUSTOMER1_BALANCE);
  //   MvcControllerIntegTestHelpers.addCustomerToDB(dbDelegate, CUSTOMER1_ID, CUSTOMER1_PASSWORD, CUSTOMER1_FIRST_NAME, CUSTOMER1_LAST_NAME, CUSTOMER1_BALANCE_IN_PENNIES);
    
  //   // Prepare Withdraw Form to Withdraw $150 from customer 1's account.
  //   double CUSTOMER1_AMOUNT_TO_WITHDRAW = 150; // user input is in dollar amount, not pennies.
  //   User customer1WithdrawFormInputs = new User();
  //   customer1WithdrawFormInputs.setUsername(CUSTOMER1_ID);
  //   customer1WithdrawFormInputs.setPassword(CUSTOMER1_PASSWORD);
  //   customer1WithdrawFormInputs.setAmountToWithdraw(CUSTOMER1_AMOUNT_TO_WITHDRAW); // user input is in dollar amount, not pennies.

  //   // store timestamp of when Withdraw request is sent to verify timestamps in the TransactionHistory table later
  //   LocalDateTime timeWhenWithdrawRequestSent = MvcControllerIntegTestHelpers.fetchCurrentTimeAsLocalDateTimeNoMilliseconds();
  //   System.out.println("Timestamp when Withdraw Request is sent: " + timeWhenWithdrawRequestSent);
  //   // send request to the Withdraw Form's POST handler in MvcController
  //   controller.submitWithdraw(customer1WithdrawFormInputs);
    
    
  //   // Design: User submits how much in dollars they want to sell ETH
  //   // Then we convert that into pennies; Then convert from dollers to ETH
  //   double CUSTOMER1_BALANCE_CRYPTO = 400;
  //   double CUSTOMER1_ETH_BALANCE_AMT = CUSTOMER1_BALANCE_CRYPTO / MvcControllerIntegTestHelpers.getCurrentEthValue();
  //   /*Next: Add to the crypto table. Set up the cryptoSell . Check that account balance increased by val +- epsilon */
  //   String currentTime = SQL_DATETIME_FORMATTER.format(new java.util.Date()); // use same timestamp for all logs created by this transfer
  //   net.testudobank.TestudoBankRepository.insertRowToCryptoLogsTable(jdbcTemplate, CUSTOMER1_ID, currentTime, "Buy" , "ETH",  CUSTOMER1_ETH_BALANCE_AMT);
  //   net.testudobank.TestudoBankRepository.insertRowToCryptoHoldingsTable(jdbcTemplate, CUSTOMER1_ID, "ETH", CUSTOMER1_ETH_BALANCE_AMT);

  //   // fetch updated customer1 data from the DB
  //   List<Map<String,Object>> customersTableData = jdbcTemplate.queryForList("SELECT * FROM Customers;");    
  //   // verify that customer1's main balance is now 0
  //   Map<String,Object> customer1Data = customersTableData.get(0);
  //   assertEquals(0, (int)customer1Data.get("Balance"));

  //   // Prepare Sell Form to sell $12.34 from customer 1's crypto account.
  //   double CUSTOMER1_AMOUNT_TO_SELL_CRYPTO = 300; // user input is in dollar amount, not pennies.
  //   User customer1CryptoSellFormInputs = new User();
  //   customer1CryptoSellFormInputs.setUsername(CUSTOMER1_ID);
  //   customer1CryptoSellFormInputs.setPassword(CUSTOMER1_PASSWORD);
  //   customer1CryptoSellFormInputs.setAmountToSellCrypto(CUSTOMER1_AMOUNT_TO_SELL_CRYPTO);
  //   // customer1CryptoWithdrawFormInputs.setAmountToWithdraw(CUSTOMER1_AMOUNT_TO_WITHDRAW_Crypto); // user input is in dollar amount, not pennies.

  //   // store timestamp of when Withdraw request is sent to verify timestamps in the TransactionHistory table later
  //   LocalDateTime timeWhenCryptoSellRequestSent = MvcControllerIntegTestHelpers.fetchCurrentTimeAsLocalDateTimeNoMilliseconds();
  //   System.out.println("Timestamp when Sell Request is sent: " + timeWhenCryptoSellRequestSent);

  //   // send request to the deposit Form's POST handler in MvcController
  //   controller.sellCrypto(customer1CryptoSellFormInputs);

  //   // fetch updated data from the DB
  //   customersTableData = jdbcTemplate.queryForList("SELECT * FROM Customers;");
  //   // verify sell replenished account balance
  //   customer1Data = customersTableData.get(0);
  //   assertEquals(CUSTOMER1_ID, (String)customer1Data.get("CustomerID"));

  //   customer1Data = customersTableData.get(0);
  //   assertTrue((int)customer1Data.get("Balance") != 0);
  // }


  
  /*Sell with an overdraft 
  Not go into overdraft*/

}