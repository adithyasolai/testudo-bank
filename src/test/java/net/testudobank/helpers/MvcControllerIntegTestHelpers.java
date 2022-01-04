package net.testudobank.helpers;

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

import net.testudobank.MvcController;
import net.testudobank.User;
import net.testudobank.tests.MvcControllerIntegTest;

public class MvcControllerIntegTestHelpers {
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