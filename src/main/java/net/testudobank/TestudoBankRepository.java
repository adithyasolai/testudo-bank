package net.testudobank;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;

public class TestudoBankRepository {

  public final static double ONE = 1;

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

  public static int getCustomerOverdraftBalanceInPennies(JdbcTemplate jdbcTemplate, String customerID) {
    String getUserOverdraftBalanceSql = String.format("SELECT OverdraftBalance FROM Customers WHERE CustomerID='%s';", customerID);
    int userOverdraftBalanceInPennies = jdbcTemplate.queryForObject(getUserOverdraftBalanceSql, Integer.class);
    return userOverdraftBalanceInPennies;
  }

  // This function returns 1 is the user owns crypto
  public static boolean hasCrypto(JdbcTemplate jdbcTemplate, String customerID) {
    String getCryptoHoldingSql = String.format("SELECT COUNT(*) FROM CryptoHoldings WHERE CustomerID='%s';", customerID);
    int amount = jdbcTemplate.queryForObject(getCryptoHoldingSql, Integer.class);

    if(amount == ONE){
      return true;
    }
    return false;
  }

  // Returns the amount of crypto that the user has
  public static double getAmountOfCrypto(JdbcTemplate jdbcTemplate, String customerID) {
    String getAmountSql = String.format("SELECT CryptoAmount FROM CryptoHoldings WHERE CustomerID='%s';", customerID);
    double cryptoAmount = (double)jdbcTemplate.queryForObject(getAmountSql, Float.class);
    return cryptoAmount;
  }
  public static List<Map<String,Object>> getRecentTransactions(JdbcTemplate jdbcTemplate, String customerID, int numTransactionsToFetch) {
    String getTransactionHistorySql = String.format("Select * from TransactionHistory WHERE CustomerID='%s' ORDER BY Timestamp DESC LIMIT %d;", customerID, numTransactionsToFetch);
    List<Map<String,Object>> transactionLogs = jdbcTemplate.queryForList(getTransactionHistorySql);
    return transactionLogs;
  }

  public static List<Map<String,Object>> getRecentCryptoLogs(JdbcTemplate jdbcTemplate, String customerID, int numTransactionsToFetch) {
    String getCryptoHistorySql = String.format("Select * from CryptoHistory WHERE CustomerID='%s' ORDER BY Timestamp DESC LIMIT %d;", customerID, numTransactionsToFetch);
    List<Map<String,Object>> cryptoHistoryLogs = jdbcTemplate.queryForList(getCryptoHistorySql);
    return cryptoHistoryLogs;
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

  public static void increaseCryptoHoldings(JdbcTemplate jdbcTemplate, String customerID, float amount) {
    String increaseCryptoHoldingSql = String.format("UPDATE CryptoHoldings SET CryptoAmount = CryptoAmount + %f WHERE CustomerID='%s';", amount, customerID);
    jdbcTemplate.update(increaseCryptoHoldingSql);
  }

  
  public static void decreaseCryptoHoldings(JdbcTemplate jdbcTemplate, String customerID, float amount) {
    String decreaseCryptoHoldingSql = String.format("UPDATE CryptoHoldings SET CryptoAmount = CryptoAmount - %f WHERE CustomerID='%s';", amount, customerID);
    jdbcTemplate.update(decreaseCryptoHoldingSql);
  }

  public static void setCryptoHoldings(JdbcTemplate jdbcTemplate, String customerID, float amount) {
    String decreaseCryptoHoldingSql = String.format("UPDATE CryptoHoldings SET CryptoAmount = %f WHERE CustomerID='%s';", amount, customerID);
    jdbcTemplate.update(decreaseCryptoHoldingSql);
  }

  public static void increaseCustomerOverdraftBalance(JdbcTemplate jdbcTemplate, String customerID, int increaseAmtInPennies) {
    String overdraftBalanceIncreaseSql = String.format("UPDATE Customers SET OverdraftBalance = OverdraftBalance + %d WHERE CustomerID='%s';", increaseAmtInPennies, customerID);
    jdbcTemplate.update(overdraftBalanceIncreaseSql);
  }

  public static void setCustomerBalance(JdbcTemplate jdbcTemplate, String customerID, int newBalanceInPennies) {
    String updateBalanceSql = String.format("UPDATE Customers SET Balance = %d WHERE CustomerID='%s';", newBalanceInPennies, customerID);
    jdbcTemplate.update(updateBalanceSql);
  }

  public static void increaseCustomerBalance(JdbcTemplate jdbcTemplate, String customerID, int increaseAmtInPennies) {
    String balanceIncreaseSql = String.format("UPDATE Customers SET Balance = Balance + %d WHERE CustomerID='%s';", increaseAmtInPennies, customerID);
    jdbcTemplate.update(balanceIncreaseSql);
  }
  
  public static void decreaseCustomerBalance(JdbcTemplate jdbcTemplate, String customerID, int decreaseAmtInPennies) {
    String balanceDecreaseSql = String.format("UPDATE Customers SET Balance = Balance - %d WHERE CustomerID='%s';", decreaseAmtInPennies, customerID);
    jdbcTemplate.update(balanceDecreaseSql);
  }

  public static void deleteRowFromOverdraftLogsTable(JdbcTemplate jdbcTemplate, String customerID, String timestamp) {
    String deleteRowFromOverdraftLogsSql = String.format("DELETE from OverdraftLogs where CustomerID='%s' AND Timestamp='%s';", customerID, timestamp);
    jdbcTemplate.update(deleteRowFromOverdraftLogsSql);
  }

  public static void insertRowToTransferLogsTable(JdbcTemplate jdbcTemplate, String customerID, String recipientID, String timestamp, int transferAmount) { 
    String transferHistoryToSql = String.format("INSERT INTO TransferHistory VALUES ('%s', '%s', '%s', %d);",
                                                    customerID,
                                                    recipientID,
                                                    timestamp,
                                                    transferAmount);
    jdbcTemplate.update(transferHistoryToSql);
  }

  public static void insertRowToCryptoHoldingsTable(JdbcTemplate jdbcTemplate, String customerID, String CryptoName, float CryptoAmount) {
    String insertRowToCryptoHoldingsSql = String.format("INSERT INTO CryptoHoldings VALUES ('%s', '%s', %f);",
                                                              customerID,
                                                              CryptoName,
                                                              CryptoAmount);
    jdbcTemplate.update(insertRowToCryptoHoldingsSql);
  }

  // // I will hold on to this for now
  // public static void deleteRowToCryptoHoldingsTable(JdbcTemplate jdbcTemplate, String customerID) {
  //   String deleteRowToCryptoHoldingsSql = String.format("DELETE from CryptoHoldings where CustomerID='%s';",
  //                                                             customerID);
  //   jdbcTemplate.update(deleteRowToCryptoHoldingsSql);
  // }
  
  public static void insertRowToCryptoHistoryTable(JdbcTemplate jdbcTemplate, String customerID, String Timestamp, String action, String CryptoName, float CryptoAmount) {
    String insertRowToCryptoHistorySql = String.format("INSERT INTO CryptoHistory VALUES ('%s', '%s', '%s', '%s', %f);",
                                                              customerID,
                                                              Timestamp,
                                                              action,
                                                              CryptoName,
                                                              CryptoAmount);
    jdbcTemplate.update(insertRowToCryptoHistorySql);
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
