package net.testudobank;

import java.util.List;
import java.util.Map;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public class TestudoBankRepository {
  public static String getCustomerPassword(JdbcTemplate jdbcTemplate, String customerID) {
    String getCustomerPasswordSql = String.format("SELECT Password FROM Passwords WHERE CustomerID='%s';", customerID);
    String customerPassword = jdbcTemplate.queryForObject(getCustomerPasswordSql, String.class);
    return customerPassword;
  }

  public static int getCustomerNumberOfReversals(JdbcTemplate jdbcTemplate, String customerID) {
    String getNumberOfReversalsSql = String.format("SELECT NumFraudReversals FROM Customers WHERE CustomerID='%s';", customerID);
    int numOfReversals = jdbcTemplate.queryForObject(getNumberOfReversalsSql, Integer.class);
    return numOfReversals;
  }

  public static int getCustomerBalanceInPennies(JdbcTemplate jdbcTemplate, String customerID) {
    String getUserBalanceSql =  String.format("SELECT Balance FROM Customers WHERE CustomerID='%s';", customerID);
    int userBalanceInPennies = jdbcTemplate.queryForObject(getUserBalanceSql, Integer.class);
    return userBalanceInPennies;
  }

  public static double getCryptoAmt(JdbcTemplate jdbcTemplate, String customerID, String cryptoName) {
    String getCryptoAmtSql =  String.format("SELECT CryptoAmount FROM CryptoHoldings WHERE CustomerID='%s' AND CryptoName='%s';", customerID, cryptoName);
    try {
      double userCryptoAmt = jdbcTemplate.queryForObject(getCryptoAmtSql, Double.class);
      return userCryptoAmt;
    } catch(EmptyResultDataAccessException exception){ return 0.0; }
  }

  public static int getCustomerOverdraftBalanceInPennies(JdbcTemplate jdbcTemplate, String customerID) {
    String getUserOverdraftBalanceSql = String.format("SELECT OverdraftBalance FROM Customers WHERE CustomerID='%s';", customerID);
    int userOverdraftBalanceInPennies = jdbcTemplate.queryForObject(getUserOverdraftBalanceSql, Integer.class);
    return userOverdraftBalanceInPennies;
  }

  public static List<Map<String,Object>> getRecentTransactions(JdbcTemplate jdbcTemplate, String customerID, int numTransactionsToFetch) {
    String getTransactionHistorySql = String.format("Select * from TransactionHistory WHERE CustomerId='%s' ORDER BY Timestamp DESC LIMIT %d;", customerID, numTransactionsToFetch);
    List<Map<String,Object>> transactionLogs = jdbcTemplate.queryForList(getTransactionHistorySql);
    return transactionLogs;
  }

  public static List<Map<String,Object>> getRecentCryptoTransactions(JdbcTemplate jdbcTemplate, String customerID, int numCryptoTransactionsToFetch) {
    String getCryptoTransactionHistorySql = String.format("Select * from CryptoHistory WHERE CustomerId='%s' ORDER BY Timestamp DESC LIMIT %d;", customerID, numCryptoTransactionsToFetch);
    List<Map<String,Object>> cryptotransactionLogs = jdbcTemplate.queryForList(getCryptoTransactionHistorySql);
    return cryptotransactionLogs;
  }

  public static List<Map<String,Object>> getTransferLogs(JdbcTemplate jdbcTemplate, String customerID, int numTransfersToFetch) {
    String getTransferHistorySql = String.format("Select * from TransferHistory WHERE TransferFrom='%s' OR TransferTo='%s' ORDER BY Timestamp DESC LIMIT %d;", customerID, customerID, numTransfersToFetch);
    List<Map<String,Object>> transferLogs = jdbcTemplate.queryForList(getTransferHistorySql);
    return transferLogs;
  }

  public static List<Map<String,Object>> getOverdraftLogs(JdbcTemplate jdbcTemplate, String customerID){
    String getOverDraftLogsSql = String.format("SELECT * FROM OverdraftLogs WHERE CustomerID='%s';", customerID);
    List<Map<String,Object>> overdraftLogs = jdbcTemplate.queryForList(getOverDraftLogsSql);
    return overdraftLogs;
  }

  public static List<Map<String,Object>> getOverdraftLogs(JdbcTemplate jdbcTemplate, String customerID, String timestamp){
    String getOverDraftLogsSql = String.format("SELECT * FROM OverdraftLogs WHERE CustomerID='%s' AND Timestamp='%s';", customerID, timestamp);
    List<Map<String,Object>> overdraftLogs = jdbcTemplate.queryForList(getOverDraftLogsSql);
    return overdraftLogs;
  }

  public static void insertRowToTransactionHistoryTable(JdbcTemplate jdbcTemplate, String customerID, String timestamp, String action, int amtInPennies) {
    String insertRowToTransactionHistorySql = String.format("INSERT INTO TransactionHistory VALUES ('%s', '%s', '%s', %d);",
                                                              customerID,
                                                              timestamp,
                                                              action,
                                                              amtInPennies);
    jdbcTemplate.update(insertRowToTransactionHistorySql);
  }

  public static void insertRowToCryptoHistoryTable(JdbcTemplate jdbcTemplate, String customerID, String timestamp, String action, String cryptoName, double amtInCrypto) {
    String insertRowToCryptoHistorySql = String.format("INSERT INTO CryptoHistory VALUES ('%s', '%s', '%s', '%s', %f);",
                                                              customerID,
                                                              timestamp,
                                                              action,
                                                              cryptoName,
                                                              amtInCrypto);
    jdbcTemplate.update(insertRowToCryptoHistorySql);
  }

  public static void insertRowToCryptoHoldingsTable(JdbcTemplate jdbcTemplate, String customerID, String cryptoName, double amtInCrypto) {
    String insertRowToCryptoHoldingsSql = String.format("INSERT INTO CryptoHoldings VALUES ('%s', '%s', %f);",
                                                              customerID,
                                                              cryptoName,
                                                              amtInCrypto);
    jdbcTemplate.update(insertRowToCryptoHoldingsSql);
  }

  public static void insertRowToOverdraftLogsTable(JdbcTemplate jdbcTemplate, String customerID, String timestamp, int depositAmtIntPennies, int oldOverdraftBalanceInPennies, int newOverdraftBalanceInPennies) {
    String insertRowToOverdraftLogsSql = String.format("INSERT INTO OverdraftLogs VALUES ('%s', '%s', %d, %d, %d);", 
                                                        customerID,
                                                        timestamp,
                                                        depositAmtIntPennies,
                                                        oldOverdraftBalanceInPennies,
                                                        newOverdraftBalanceInPennies);
    jdbcTemplate.update(insertRowToOverdraftLogsSql);
  }

  public static void setCustomerNumFraudReversals(JdbcTemplate jdbcTemplate, String customerID, int newNumFraudReversals) {
    String numOfReversalsUpdateSql = String.format("UPDATE Customers SET NumFraudReversals = %d WHERE CustomerID='%s';", newNumFraudReversals, customerID);
    jdbcTemplate.update(numOfReversalsUpdateSql);
  }

  public static void setCustomerOverdraftBalance(JdbcTemplate jdbcTemplate, String customerID, int newOverdraftBalanceInPennies) {
    String overdraftBalanceUpdateSql = String.format("UPDATE Customers SET OverdraftBalance = %d WHERE CustomerID='%s';", newOverdraftBalanceInPennies, customerID);
    jdbcTemplate.update(overdraftBalanceUpdateSql);
  }

  public static void increaseCustomerOverdraftBalance(JdbcTemplate jdbcTemplate, String customerID, int increaseAmtInPennies) {
    String overdraftBalanceIncreaseSql = String.format("UPDATE Customers SET OverdraftBalance = OverdraftBalance + %d WHERE CustomerID='%s';", increaseAmtInPennies, customerID);
    jdbcTemplate.update(overdraftBalanceIncreaseSql);
  }

  public static void setCustomerBalance(JdbcTemplate jdbcTemplate, String customerID, int newBalanceInPennies) {
    String updateBalanceSql = String.format("UPDATE Customers SET Balance = %d WHERE CustomerID='%s';", newBalanceInPennies, customerID);
    jdbcTemplate.update(updateBalanceSql);
  }

  public static void setCustomerCryptoBalance(JdbcTemplate jdbcTemplate, String customerID, String cryptoName, double newBalanceInCrypto) {
    String updateCryptoBalanceSql = String.format("UPDATE CryptoHoldings SET CryptoAmount = %f WHERE CustomerID='%s' AND CryptoName='%s';", newBalanceInCrypto, customerID, cryptoName);
    jdbcTemplate.update(updateCryptoBalanceSql);
  }

  public static void increaseCustomerBalance(JdbcTemplate jdbcTemplate, String customerID, int increaseAmtInPennies) {
    String balanceIncreaseSql = String.format("UPDATE Customers SET Balance = Balance + %d WHERE CustomerID='%s';", increaseAmtInPennies, customerID);
    jdbcTemplate.update(balanceIncreaseSql);
  }

  public static void increaseCustomerCryptoBalance(JdbcTemplate jdbcTemplate, String customerID, String cryptoName, double increaseAmtInCrypto) {
    String cryptobalanceIncreaseSql = String.format("UPDATE CryptoHoldings SET CryptoAmount=CryptoAmount+%f WHERE CustomerID='%s' AND CryptoName='%s';", increaseAmtInCrypto, customerID, cryptoName);
    jdbcTemplate.update(cryptobalanceIncreaseSql);
  }
  
  public static void decreaseCustomerBalance(JdbcTemplate jdbcTemplate, String customerID, int decreaseAmtInPennies) {
    String balanceDecreaseSql = String.format("UPDATE Customers SET Balance = Balance - %d WHERE CustomerID='%s';", decreaseAmtInPennies, customerID);
    jdbcTemplate.update(balanceDecreaseSql);
  }

  public static void decreaseCustomerCryptoBalance(JdbcTemplate jdbcTemplate, String customerID, String cryptoName, double decreaseAmtInCrypto) {
    String cryptobalanceDecreaseSql = String.format("UPDATE CryptoHoldings SET CryptoAmount = CryptoAmount - %f WHERE CustomerID='%s' AND CryptoName='%s';", decreaseAmtInCrypto, customerID, cryptoName);
    jdbcTemplate.update(cryptobalanceDecreaseSql);
  }

  public static void deleteRowFromOverdraftLogsTable(JdbcTemplate jdbcTemplate, String customerID, String timestamp) {
    String deleteRowFromOverdraftLogsSql = String.format("DELETE from OverdraftLogs where CustomerID='%s' AND Timestamp='%s';", customerID, timestamp);
    jdbcTemplate.update(deleteRowFromOverdraftLogsSql);
  }

  public static void deleteRowFromCryptoHoldingsTable(JdbcTemplate jdbcTemplate, String customerID, String cryptoName) {
    String deleteRowFromCryptoHoldingsSql = String.format("DELETE from CryptoHoldings where CustomerID='%s' AND CryptoName='%s';", customerID, cryptoName);
    jdbcTemplate.update(deleteRowFromCryptoHoldingsSql);
  }

  public static void insertRowToTransferLogsTable(JdbcTemplate jdbcTemplate, String customerID, String recipientID, String timestamp, int transferAmount) { 
    String transferHistoryToSql = String.format("INSERT INTO TransferHistory VALUES ('%s', '%s', '%s', %d);",
                                                    customerID,
                                                    recipientID,
                                                    timestamp,
                                                    transferAmount);
    jdbcTemplate.update(transferHistoryToSql);
  }
  
  public static boolean doesCustomerExist(JdbcTemplate jdbcTemplate, String customerID) { 
    String getCustomerIDSql =  String.format("SELECT CustomerID FROM Customers WHERE CustomerID='%s';", customerID);
    if (jdbcTemplate.queryForObject(getCustomerIDSql, String.class) != null) {
     return true;
    } else {
      return false;
    }
  }
}
