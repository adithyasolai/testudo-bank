package net.testudobank.helpers;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

import javax.script.ScriptException;
import javax.sql.DataSource;

import com.mysql.cj.jdbc.MysqlDataSource;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.delegate.DatabaseDelegate;
import org.testcontainers.ext.ScriptUtils;

import net.testudobank.MvcController;
import net.testudobank.tests.MvcControllerIntegTest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.IOException;


public class MvcControllerIntegTestHelpers {

  public static double getCurrentEthValue() {
    try {
      // fetch the document over HTTP
      Document doc = Jsoup.connect("https://ethereumprice.org").userAgent("Mozilla").get();

      Element value = doc.getElementById("coin-price");
      String valueStr = value.text();

      // Replacing the '$'' and ',' characters from the string
      valueStr = valueStr.replaceAll("\\$", "").replaceAll("\\,", "");
      double ethValue = Double.parseDouble(valueStr);

      return ethValue;
    } catch (IOException e) {
      // Print stack trace for debugging
      e.printStackTrace();

      // Return -1 if there was an error during web scraping
      return -1;
    }
  }

  // Fetches DB credentials to initialize jdbcTemplate client
  public static DataSource dataSource(MySQLContainer db) {
    MysqlDataSource dataSource = new MysqlDataSource();
    dataSource.setUrl(db.getJdbcUrl());
    dataSource.setUser(db.getUsername());
    dataSource.setPassword(db.getPassword());
    return dataSource;
  }

  // Uses given customer details to initialize the customer in the Customers and Passwords table in the MySQL DB.
  public static void addCustomerToDB(DatabaseDelegate dbDelegate, String ID, String password, String firstName, String lastName, int balance, int overdraftBalance, int numFraudReversals) throws ScriptException {
    String insertCustomerSql = String.format("INSERT INTO Customers VALUES ('%s', '%s', '%s', %d, %d, %d)", ID, firstName, lastName, balance, overdraftBalance, numFraudReversals);
    ScriptUtils.executeDatabaseScript(dbDelegate, null, insertCustomerSql);

    String insertCustomerPasswordSql = String.format("INSERT INTO Passwords VALUES ('%s', '%s')", ID, password);
    ScriptUtils.executeDatabaseScript(dbDelegate, null, insertCustomerPasswordSql);
  }

  // Adds a customer to the MySQL DB with no overdraft balance or fraud disputes
  public static void addCustomerToDB(DatabaseDelegate dbDelegate, String ID, String password, String firstName, String lastName, int balance) throws ScriptException {
    addCustomerToDB(dbDelegate, ID, password, firstName, lastName, balance, 0, 0);
  }

  // Verifies that a single transaction log in the TransactionHistory table matches the expected customerID, timestamp, action, and amount
  public static void checkTransactionLog(Map<String,Object> transactionLog, LocalDateTime timeWhenRequestSent, String expectedCustomerID, String expectedAction, int expectedAmountInPennies) {
    assertEquals(expectedCustomerID, (String)transactionLog.get("CustomerID"));
    assertEquals(expectedAction, (String)transactionLog.get("Action"));
    assertEquals(expectedAmountInPennies, (int)transactionLog.get("Amount"));
    // verify that the timestamp for the Deposit is within a reasonable range from when the request was first sent
    LocalDateTime transactionLogTimestamp = (LocalDateTime)transactionLog.get("Timestamp");
    LocalDateTime transactionLogTimestampAllowedUpperBound = timeWhenRequestSent.plusSeconds(MvcControllerIntegTest.REASONABLE_TIMESTAMP_EPSILON_IN_SECONDS);
    assertTrue(transactionLogTimestamp.compareTo(timeWhenRequestSent) >= 0 && transactionLogTimestamp.compareTo(transactionLogTimestampAllowedUpperBound) <= 0);
    System.out.println("Timestamp stored in TransactionHistory table for the request: " + transactionLogTimestamp);
  }

  // public static void checkCryptoTransactionLog(Map<String,Object> transactionLog, LocalDateTime timeWhenRequestSent, String expectedCustomerID, String expectedAction, String expectedCryptoName, int expectedAmountInPennies, int epsilon) {
  public static void checkCryptoTransactionLog(Map<String,Object> transactionLog, LocalDateTime timeWhenRequestSent, String expectedCustomerID, String expectedAction, String expectedCryptoName, int expectedAmountInPennies, int epsilon) {
    System.out.println("null?");
    double currentEthPrice = MvcControllerIntegTestHelpers.getCurrentEthValue();
    assertEquals(expectedCustomerID, (String)transactionLog.get("CustomerID"));
    System.out.println("null? 2");

    assertEquals(expectedAction, (String)transactionLog.get("Action"));
    for(String str: transactionLog.keySet()) {
      System.out.println(str);
      System.out.println(transactionLog.get(str));
    }
    System.out.println("null? 3");
    try {
      if(transactionLog.get("CryptoAmount") instanceof Float) {
        System.out.println("expectedAmountInPennies" + expectedAmountInPennies);
        System.out.println("Well: " + convertDollarsToPennies((float)transactionLog.get("CryptoAmount") * currentEthPrice ));
        assertTrue(expectedAmountInPennies < convertDollarsToPennies((((float)transactionLog.get("CryptoAmount")*currentEthPrice) + convertDollarsToPennies(epsilon))));
        assertTrue(expectedAmountInPennies > convertDollarsToPennies((((float)transactionLog.get("CryptoAmount")*currentEthPrice) - convertDollarsToPennies(epsilon))));
      }
      // System.out.println("Well: " + ( transactionLog.get("CryptoAmount")) );

    } catch(Exception e) {
      e.printStackTrace();
    }
    // assertTrue(expectedAmountInPennies < (((float)transactionLog.get("CryptoAmount")*currentEthPrice) + epsilon));
    System.out.println("null 4");

    // assertTrue(expectedAmountInPennies > (((float)transactionLog.get("CryptoAmount")*currentEthPrice) - epsilon));
    System.out.println("null 5");

    assertEquals(expectedCryptoName, (String)transactionLog.get("CryptoName"));
    System.out.println("null 6");

    // verify that the timestamp for the Deposit is within a reasonable range from when the request was first sent
    LocalDateTime transactionLogTimestamp = (LocalDateTime)transactionLog.get("Timestamp");
    System.out.println("null 7");

    LocalDateTime transactionLogTimestampAllowedUpperBound = timeWhenRequestSent.plusSeconds(MvcControllerIntegTest.REASONABLE_TIMESTAMP_EPSILON_IN_SECONDS);
    System.out.println("null 8");

    assertTrue(transactionLogTimestamp.compareTo(timeWhenRequestSent) >= 0 && transactionLogTimestamp.compareTo(transactionLogTimestampAllowedUpperBound) <= 0);
    System.out.println("null 9");

    System.out.println("Timestamp stored in TransactionHistory table for the request: " + transactionLogTimestamp);
  }

  // Verifies that a single overdraft repayment log in the OverdraftLogs table matches the expected customerID, timestamp, depositAmt, oldOverBalance, and newOverBalance
  public static void checkOverdraftLog(Map<String,Object> overdraftLog, LocalDateTime timeWhenRequestSent, String expectedCustomerID, int expectedDepositAmtInPennies, int expectedOldOverBalanceInPennies, int expectedNewOverBalanceInPennies) {
    assertEquals(expectedCustomerID, (String)overdraftLog.get("CustomerID"));
    assertEquals(expectedDepositAmtInPennies, (int)overdraftLog.get("DepositAmt"));
    assertEquals(expectedOldOverBalanceInPennies, (int)overdraftLog.get("OldOverBalance"));
    assertEquals(expectedNewOverBalanceInPennies, (int)overdraftLog.get("NewOverBalance"));
    // verify that the timestamp for the overdraft repayement is within a reasonable range from when the Deposit request was first sent
    LocalDateTime overdraftLogTimestamp = (LocalDateTime)overdraftLog.get("Timestamp");
    LocalDateTime overdraftLogTimestampAllowedUpperBound = timeWhenRequestSent.plusSeconds(MvcControllerIntegTest.REASONABLE_TIMESTAMP_EPSILON_IN_SECONDS);
    assertTrue(overdraftLogTimestamp.compareTo(timeWhenRequestSent) >= 0 && overdraftLogTimestamp.compareTo(overdraftLogTimestampAllowedUpperBound) <= 0);
    System.out.println("Timestamp stored in OverdraftLogs table for the Repayment: " + overdraftLogTimestamp);
  }

  // Converts dollar amounts in frontend to penny representation in backend MySQL DB
  public static int convertDollarsToPennies(double dollarAmount) {
    return (int) (dollarAmount * 100);
  }

  // Applies overdraft interest rate to a dollar amount in pennies, and returns an int penny result
  public static int applyOverdraftInterest(int dollarAmountInPennies) {
    return (int) (dollarAmountInPennies * MvcController.INTEREST_RATE);
  }

  // Fetches current local time with no milliseconds because the MySQL DB has granularity only up to seconds (does not use milliseconds)
  public static LocalDateTime fetchCurrentTimeAsLocalDateTimeNoMilliseconds() {
    LocalDateTime currentTimeAsLocalDateTime = convertDateToLocalDateTime(new java.util.Date());
    currentTimeAsLocalDateTime = currentTimeAsLocalDateTime.truncatedTo(ChronoUnit.SECONDS);
    return currentTimeAsLocalDateTime;
  }

  // Converts the java.util.Date object into the LocalDateTime returned by the MySQL DB
  public static LocalDateTime convertDateToLocalDateTime(Date dateToConvert) { 
    return dateToConvert.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
  }
}