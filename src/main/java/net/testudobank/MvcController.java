package net.testudobank;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

@Controller
public class MvcController {
  
  // A simplified JDBC client that is injected with the login credentials
  // specified in /src/main/resources/application.properties
  private JdbcTemplate jdbcTemplate;

  // Formatter for converting Java Dates to SQL-compatible DATETIME Strings
  private static java.text.SimpleDateFormat SQL_DATETIME_FORMATTER = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  //// CONSTANT LITERALS ////
  public final static double INTEREST_RATE = 1.02;
  private final static int MAX_OVERDRAFT_IN_PENNIES = 100000;
  private final static int MAX_DISPUTES = 2;
  private final static int MAX_NUM_TRANSACTIONS_DISPLAYED = 3;
  private final static int MAX_REVERSABLE_TRANSACTIONS_AGO = 3;
  private final static String HTML_LINE_BREAK = "<br/>";
  public static String TRANSACTION_HISTORY_DEPOSIT_ACTION = "Deposit";
  public static String TRANSACTION_HISTORY_WITHDRAW_ACTION = "Withdraw";

  public MvcController(@Autowired JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  //// HTML GET HANDLERS ////

  /**
   * HTML GET request handler that serves the "welcome" page to the user.
   * 
   * @param model
   * @return "welcome" page
   */
	@GetMapping("/")
	public String showWelcome(Model model) {
		return "welcome";
	}

  /**
   * HTML GET request handler that serves the "login_form" page to the user.
   * An empty `User` object is also added to the Model as an Attribute to store
   * the user's login form input.
   * 
   * @param model
   * @return "login_form" page
   */
  @GetMapping("/login")
	public String showLoginForm(Model model) {
		User user = new User();
		model.addAttribute("user", user);
		
		return "login_form";
	}

  /**
   * HTML GET request handler that serves the "deposit_form" page to the user.
   * An empty `User` object is also added to the Model as an Attribute to store
   * the user's deposit form input.
   * 
   * @param model
   * @return "deposit_form" page
   */
  @GetMapping("/deposit")
	public String showDepositForm(Model model) {
    User user = new User();
		model.addAttribute("user", user);
		return "deposit_form";
	}

  /**
   * HTML GET request handler that serves the "withdraw_form" page to the user.
   * An empty `User` object is also added to the Model as an Attribute to store
   * the user's withdraw form input.
   * 
   * @param model
   * @return "withdraw_form" page
   */
  @GetMapping("/withdraw")
	public String showWithdrawForm(Model model) {
    User user = new User();
		model.addAttribute("user", user);
		return "withdraw_form";
	}

  /**
   * HTML GET request handler that serves the "dispute_form" page to the user.
   * An empty `User` object is also added to the Model as an Attribute to store
   * the user's dispute form input.
   * 
   * @param model
   * @return "dispute_form" page
   */
  @GetMapping("/dispute")
	public String showDisputeForm(Model model) {
    User user = new User();
		model.addAttribute("user", user);
		return "dispute_form";
	}

  //// HELPER METHODS ////

  /**
   * Helper method that queries the MySQL DB for the customer account info (First Name, Last Name, and Balance)
   * and adds these values to the `user` Model Attribute so that they can be displayed in the "account_info" page.
   * 
   * @param user
   */
  private void updateAccountInfo(User user) {
    String getUserNameAndBalanceAndOverDraftBalanceSql = String.format("SELECT FirstName, LastName, Balance, OverdraftBalance FROM Customers WHERE CustomerID='%s';", user.getUsername());
    List<Map<String,Object>> queryResults = jdbcTemplate.queryForList(getUserNameAndBalanceAndOverDraftBalanceSql);
    String getOverDraftLogsSql = String.format("SELECT * FROM OverdraftLogs WHERE CustomerID='%s';", user.getUsername());
    // SQL Query that only fetches the three most recent transaction logs for this customer.
    String getTransactionHistorySql = String.format("Select * from TransactionHistory WHERE CustomerId='%s' ORDER BY Timestamp DESC LIMIT %d;", user.getUsername(), MAX_NUM_TRANSACTIONS_DISPLAYED);
    
    List<Map<String,Object>> queryLogs = jdbcTemplate.queryForList(getOverDraftLogsSql);
    String logs = HTML_LINE_BREAK;
    for(Map<String, Object> overdraftLog : queryLogs){
      logs += overdraftLog + HTML_LINE_BREAK;
    }
    List<Map<String,Object>> transactionLogs = jdbcTemplate.queryForList(getTransactionHistorySql);
    String transactionHistoryOutput = HTML_LINE_BREAK;
    for(Map<String, Object> transactionLog : transactionLogs){
      transactionHistoryOutput += transactionLog + HTML_LINE_BREAK;
    }

    Map<String,Object> userData = queryResults.get(0);

    user.setFirstName((String)userData.get("FirstName"));
    user.setLastName((String)userData.get("LastName"));
    user.setBalance((int)userData.get("Balance")/100.0);
    double overDraftBalance = (int)userData.get("OverdraftBalance");
    user.setOverDraftBalance(overDraftBalance/100);
    user.setLogs(logs);
    user.setTransactionHist(transactionHistoryOutput);
  }

  // Converts dollar amounts in frontend to penny representation in backend MySQL DB
  private static int convertDollarsToPennies(double dollarAmount) {
    return (int) (dollarAmount * 100);
  }

  //// HTML POST HANDLERS ////

  /**
   * HTML POST request handler that uses user input from Login Form page to determine 
   * login success or failure.
   * 
   * Queries 'passwords' table in MySQL DB for the correct password associated with the
   * username ID given by the user. Compares the user's password attempt with the correct
   * password.
   * 
   * If the password attempt is correct, the "account_info" page is served to the customer
   * with all account details retrieved from the MySQL DB.
   * 
   * If the password attempt is incorrect, the user is redirected to the "welcome" page.
   * 
   * @param user
   * @return "account_info" page if login successful. Otherwise, redirect to "welcome" page.
   */
  @PostMapping("/login")
	public String submitLoginForm(@ModelAttribute("user") User user) {
    // Print user's existing fields for debugging
		System.out.println(user);

    String userID = user.getUsername();
    String userPasswordAttempt = user.getPassword();

    // Retrieve correct password for this customer.
    String userPassword = TestudoBankRepository.getCustomerPassword(jdbcTemplate, userID);

    if (userPasswordAttempt.equals(userPassword)) {
      updateAccountInfo(user);

      return "account_info";
    } else {
      return "welcome";
    }
	}

  /**
   * HTML POST request handler for the Deposit Form page.
   * 
   * The same username+password handling from the login page is used.
   * 
   * If the password attempt is correct, the balance is incremented by the amount specified
   * in the Deposit Form. The user is then served the "account_info" with an updated balance.
   * 
   * If the password attempt is incorrect, the user is redirected to the "welcome" page.
   * 
   * @param user
   * @return "account_info" page if login successful. Otherwise, redirect to "welcome" page.
   */
  @PostMapping("/deposit")
  public String submitDeposit(@ModelAttribute("user") User user) {
    String userID = user.getUsername();
    String userPasswordAttempt = user.getPassword();

    String userPassword = TestudoBankRepository.getCustomerPassword(jdbcTemplate, userID);

    // unsuccessful login
    if (userPasswordAttempt.equals(userPassword) == false) {
      return "welcome";
    }

    double userDepositAmt = user.getAmountToDeposit();
    int userDepositAmtInPennies = convertDollarsToPennies(userDepositAmt);

    int numOfReversals = TestudoBankRepository.getCustomerNumberOfReversals(jdbcTemplate, userID);

    //If too many reversals dont do deposit
    if (userDepositAmt < 0 || numOfReversals >= MAX_DISPUTES){
      return "welcome";
    }
    
    String currentTime = SQL_DATETIME_FORMATTER.format(new java.util.Date());

    //Adds deposit to transaction history
    String transactionHistorySql = String.format("INSERT INTO TransactionHistory VALUES ('%s', '%s', %s, %d);",
                                                  userID,
                                                  currentTime,
                                                  String.format("'%s'", TRANSACTION_HISTORY_DEPOSIT_ACTION),
                                                  userDepositAmtInPennies);
    jdbcTemplate.update(transactionHistorySql);

    int userOverdraftBalanceInPennies = TestudoBankRepository.getCustomerOverdraftBalanceInPennies(jdbcTemplate, userID);

    // if the overdraft balance is positive, subtract the deposit with interest
    if (userOverdraftBalanceInPennies > 0) {
      int newOverdraftBalanceInPennies = Math.max(userOverdraftBalanceInPennies - userDepositAmtInPennies, 0);
      String overdraftLogsInsertSql = String.format("INSERT INTO OverdraftLogs VALUES ('%s', '%s', %d, %d, %d);", 
                                                    userID,
                                                    currentTime,
                                                    userDepositAmtInPennies,
                                                    userOverdraftBalanceInPennies,
                                                    newOverdraftBalanceInPennies);
      jdbcTemplate.update(overdraftLogsInsertSql);

      // updating customers table
      String overdraftBalanceUpdateSql = String.format("UPDATE Customers SET OverdraftBalance = %d WHERE CustomerID='%s';", newOverdraftBalanceInPennies, userID);
      jdbcTemplate.update(overdraftBalanceUpdateSql);
      updateAccountInfo(user);
    }

    // if in the overdraft case and there is excess deposit, deposit the excess amount.
    // otherwise, this is a non-overdraft case, so just use the userDepositAmt.
    int balanceIncreaseAmtInPennies = 0;
    if (userOverdraftBalanceInPennies > 0 && userDepositAmtInPennies > userOverdraftBalanceInPennies) {
      balanceIncreaseAmtInPennies = userDepositAmtInPennies - userOverdraftBalanceInPennies;
    } else if (userOverdraftBalanceInPennies > 0 && userDepositAmtInPennies <= userOverdraftBalanceInPennies) {
      balanceIncreaseAmtInPennies = 0; // overdraft case, but no excess deposit. don't increase balance column.
    } else {
      balanceIncreaseAmtInPennies = userDepositAmtInPennies;
    }

    String balanceIncreaseSql = String.format("UPDATE Customers SET Balance = Balance + %d WHERE CustomerID='%s';", balanceIncreaseAmtInPennies, userID);
    System.out.println(balanceIncreaseSql); // Print executed SQL update for debugging
    jdbcTemplate.update(balanceIncreaseSql);
    updateAccountInfo(user);
    return "account_info";
  }
	
  /**
   * HTML POST request handler for the Withdraw Form page.
   * 
   * The same username+password handling from the login page is used.
   * 
   * If the password attempt is correct, the balance is decremented by the amount specified
   * in the Withdraw Form. The user is then served the "account_info" with an updated balance.
   * 
   * If the password attempt is incorrect, the user is redirected to the "welcome" page.
   * 
   * @param user
   * @return "account_info" page if login successful. Otherwise, redirect to "welcome" page.
   */
  @PostMapping("/withdraw")
  public String submitWithdraw(@ModelAttribute("user") User user) {
    String userID = user.getUsername();
    String userPasswordAttempt = user.getPassword();
    
    String userPassword = TestudoBankRepository.getCustomerPassword(jdbcTemplate, userID);

    // unsuccessful login
    if (userPasswordAttempt.equals(userPassword) == false) {
      return "welcome";
    }

    double userWithdrawAmt = user.getAmountToWithdraw();
    int userWithdrawAmtInPennies = convertDollarsToPennies(userWithdrawAmt);

    int numOfReversals = TestudoBankRepository.getCustomerNumberOfReversals(jdbcTemplate, userID);

    //If too many reversals dont do withdraw
    if (userWithdrawAmt < 0 || numOfReversals >= MAX_DISPUTES){
      return "welcome";
    }

    int userBalanceInPennies = TestudoBankRepository.getCustomerBalanceInPennies(jdbcTemplate, userID);
    
    // if the balance is not positive, withdraw with interest fee
    if (userBalanceInPennies - userWithdrawAmtInPennies < 0) {
      // subtracts the remaining balance from withdrawal amount 
      int newOverdraftAmtInPennies = userWithdrawAmtInPennies - userBalanceInPennies;

      if (newOverdraftAmtInPennies > MAX_OVERDRAFT_IN_PENNIES) {
        return "welcome";
      }

      // factor in the existing overdraft balance before executing another overdraft
      int userOverdraftBalanceInPennies = TestudoBankRepository.getCustomerOverdraftBalanceInPennies(jdbcTemplate, userID);
      if (newOverdraftAmtInPennies + userOverdraftBalanceInPennies > MAX_OVERDRAFT_IN_PENNIES) {
        return "welcome";
      }

      String currentTime = SQL_DATETIME_FORMATTER.format(new java.util.Date());

      //Adds withdraw to transaction history
      String transactionHistorySql = String.format("INSERT INTO TransactionHistory VALUES ('%s', '%s', %s, %d);",
                                                    userID,
                                                    currentTime,
                                                    String.format("'%s'", TRANSACTION_HISTORY_WITHDRAW_ACTION),
                                                    userWithdrawAmtInPennies);
      jdbcTemplate.update(transactionHistorySql);

      // this is a valid overdraft, so we can set Balance column to 0
      String updateBalanceSql = String.format("UPDATE Customers SET Balance = %d WHERE CustomerID='%s';", 0, userID);
      jdbcTemplate.update(updateBalanceSql);

      int newOverdraftAmtAfterInterestInPennies = (int)(newOverdraftAmtInPennies * INTEREST_RATE);
      int cumulativeOverdraftInPennies = userOverdraftBalanceInPennies + newOverdraftAmtAfterInterestInPennies;

      String overDraftBalanceUpdateSql = String.format("UPDATE Customers SET OverdraftBalance = %d WHERE CustomerID='%s';", cumulativeOverdraftInPennies, userID);
      jdbcTemplate.update(overDraftBalanceUpdateSql);
      System.out.println(overDraftBalanceUpdateSql);

      updateAccountInfo(user);
      return "account_info";

    }

    // non-overdraft case
    String balanceDecreaseSql = String.format("UPDATE Customers SET Balance = Balance - %d WHERE CustomerID='%s';", userWithdrawAmtInPennies, userID);
    System.out.println(balanceDecreaseSql);
    jdbcTemplate.update(balanceDecreaseSql);
    

    String currentTime = SQL_DATETIME_FORMATTER.format(new java.util.Date());

    //Adds withdraw to transaction history
    String transactionHistorySql = String.format("INSERT INTO TransactionHistory VALUES ('%s', '%s', %s, %d);",
                                                    userID,
                                                    currentTime,
                                                    String.format("'%s'", TRANSACTION_HISTORY_WITHDRAW_ACTION),
                                                    userWithdrawAmtInPennies);
    jdbcTemplate.update(transactionHistorySql);

    updateAccountInfo(user);

    return "account_info";

  }

  /**
   * HTML POST request handler for the Dispute Form page.
   * 
   * The same username+password handling from the login page is used.
   * 
   * If the password attempt is correct, the transaction is reversed and the proper
   * balances are updated
   * 
   * If the password attempt is incorrect, the user is redirected to the "welcome" page.
   * 
   * @param user
   * @return "account_info" page if login successful. Otherwise, redirect to "welcome" page.
   */

  @PostMapping("/dispute")
  public String submitDispute(@ModelAttribute("user") User user) {
    // Ensure that requested transaction to reverse is within acceptable range
    if (user.getNumTransactionsAgo() <= 0 || user.getNumTransactionsAgo() > MAX_REVERSABLE_TRANSACTIONS_AGO) {
      return "welcome";
    }

    String userID = user.getUsername();
    String userPasswordAttempt = user.getPassword();
    
    String userPassword = TestudoBankRepository.getCustomerPassword(jdbcTemplate, userID);

    // unsuccessful login
    if (userPasswordAttempt.equals(userPassword) == false) {
      return "welcome";
    }

    // check if customer account is frozen
    int numOfReversals = TestudoBankRepository.getCustomerNumberOfReversals(jdbcTemplate, userID);
    if (numOfReversals >= MAX_DISPUTES) {
      return "welcome";
    }
    
    // Fetch 3 most recent transactions for this customer
    String getTransactionHistorySql = String.format("Select * from TransactionHistory WHERE CustomerId='%s' ORDER BY Timestamp DESC LIMIT %d;", user.getUsername(), MAX_NUM_TRANSACTIONS_DISPLAYED);
    List<Map<String,Object>> transactionLogs = jdbcTemplate.queryForList(getTransactionHistorySql);
    
    // Ensure customer has enough transactions to complete the reversal
    if (user.getNumTransactionsAgo() > transactionLogs.size()) {
      return "welcome";
    }

    // Retrieve correct log based on what transaction user wants to reverse
    Map<String, Object> logToReverse = transactionLogs.get(user.getNumTransactionsAgo() - 1);

    // Get balance and overdraft balance
    int userBalanceInPennies = TestudoBankRepository.getCustomerBalanceInPennies(jdbcTemplate, userID);
    int userOverdraftBalanceInPennies = TestudoBankRepository.getCustomerOverdraftBalanceInPennies(jdbcTemplate, userID);

    int reversalAmount = (int) logToReverse.get("Amount");

    // If transaction to reverse is a deposit, then withdraw the money out
    if (((String) logToReverse.get("Action")).toLowerCase().equals("deposit")) {
      // if withdraw would exceed max overdraft possible, return welcome
      if (userOverdraftBalanceInPennies + (reversalAmount - userBalanceInPennies) > MAX_OVERDRAFT_IN_PENNIES) {
        return "welcome";
      }

      // if balance is large enough to have reversalAmount taken from it, subtract reversalAmount from balance
      if (userBalanceInPennies - reversalAmount > 0){
        String balanceDecreaseSql = String.format("UPDATE Customers SET Balance = Balance - %d WHERE CustomerID='%s';", reversalAmount, userID);
        jdbcTemplate.update(balanceDecreaseSql);
      } else { // Case when reversing deposit causes overdraft or go deeper into overdraft
        // Set main balance to 0 since we are either going into overdraft or already in overdraft
        String balanceZeroSql = String.format("UPDATE Customers SET Balance = 0 WHERE CustomerID='%s';", userID);
        jdbcTemplate.update(balanceZeroSql);

        int difference = reversalAmount - userBalanceInPennies;

        //check if deposit helped pay off overdraft balance
        String getOverDraftLogsSql = String.format("SELECT * FROM OverdraftLogs WHERE CustomerID='%s' AND Timestamp='%s';", userID, logToReverse.get("Timestamp"));
        List<Map<String,Object>> queryLogs = jdbcTemplate.queryForList(getOverDraftLogsSql);
        if (queryLogs.size() == 0) { // if deposit did not help pay of overdraft balance, then apply interest rate
          String overdraftBalanceUpdateSql = String.format("UPDATE Customers SET OverdraftBalance = OverdraftBalance + %d WHERE CustomerID='%s';", ((int) (difference * INTEREST_RATE)), userID);
          jdbcTemplate.update(overdraftBalanceUpdateSql);
        } else { // otherwise don't apply interest and remove from overdraft logs
          String overdraftBalanceUpdateSql = String.format("UPDATE Customers SET OverdraftBalance = OverdraftBalance + %d WHERE CustomerID='%s';", difference, userID);
          jdbcTemplate.update(overdraftBalanceUpdateSql);
          String removeFromOverdraftLogsSql = String.format("DELETE from OverdraftLogs where Timestamp='%s';", logToReverse.get("Timestamp"));
          jdbcTemplate.update(removeFromOverdraftLogsSql);
        }
      }
      
      String currentTime = SQL_DATETIME_FORMATTER.format(new java.util.Date());

      // add transaction to transaction history
      String transactionHistorySql = String.format("INSERT INTO TransactionHistory VALUES ('%s', '%s', %s, %d);",
                                                  userID,
                                                  currentTime,
                                                  String.format("'%s'", TRANSACTION_HISTORY_WITHDRAW_ACTION),
                                                  reversalAmount);
      jdbcTemplate.update(transactionHistorySql);
    } else { // Case when reversing a withdraw, deposit the money instead
      if (userOverdraftBalanceInPennies == 0) {
        String balanceIncreaseSql = String.format("UPDATE Customers SET Balance = Balance + %d WHERE CustomerID='%s';", reversalAmount, userID);
        jdbcTemplate.update(balanceIncreaseSql);
        
        String currentTime = SQL_DATETIME_FORMATTER.format(new java.util.Date());

        //adds transaction to transaction hisotry
        String transactionHistorySql = String.format("INSERT INTO TransactionHistory VALUES ('%s', '%s', %s, %d);",
                                                  userID,
                                                  currentTime,
                                                  String.format("'%s'", TRANSACTION_HISTORY_DEPOSIT_ACTION),
                                                  reversalAmount);
        jdbcTemplate.update(transactionHistorySql);
      } else { // case when user is in overdraft
        // if amount is greater than overdraft balance, add difference to balance
        int difference = userOverdraftBalanceInPennies - reversalAmount;
        if (difference < 0) {
          String balanceDecreaseSql = String.format("UPDATE Customers SET Balance = Balance + %d WHERE CustomerID='%s';", (difference * -1), userID);
          System.out.println(balanceDecreaseSql);
          jdbcTemplate.update(balanceDecreaseSql);
        }
        
        //sets new overdraft balance
        int newOverdraftBalanceInPennies = Math.max(difference, 0);
        String overdraftBalanceUpdateSql = String.format("UPDATE Customers SET OverdraftBalance = %d WHERE CustomerID='%s';", newOverdraftBalanceInPennies, userID);
        jdbcTemplate.update(overdraftBalanceUpdateSql);
        
        String currentTime = SQL_DATETIME_FORMATTER.format(new java.util.Date());

        //adds change into overdraft logs
        String overdraftLogsInsertSql = String.format("INSERT INTO OverdraftLogs VALUES ('%s', '%s', %d, %d, %d);", 
                                                    userID,
                                                    currentTime,
                                                    reversalAmount,
                                                    userOverdraftBalanceInPennies,
                                                    newOverdraftBalanceInPennies);
        jdbcTemplate.update(overdraftLogsInsertSql);

        //adds transaction to transaction logs
        String transactionHistorySql = String.format("INSERT INTO TransactionHistory VALUES ('%s', '%s', %s, %d);",
                                                    userID,
                                                    currentTime,
                                                    String.format("'%s'", TRANSACTION_HISTORY_DEPOSIT_ACTION),
                                                    reversalAmount);
        jdbcTemplate.update(transactionHistorySql);
      }
    }

    // Adds to number of reversals only after a successful reversal 
    numOfReversals++;
    String numOfReversalsUpdateSql = String.format("UPDATE Customers SET NumFraudReversals = %d WHERE CustomerID='%s';", numOfReversals, userID);
    jdbcTemplate.update(numOfReversalsUpdateSql);

    updateAccountInfo(user);

    return "account_info";
  }

}