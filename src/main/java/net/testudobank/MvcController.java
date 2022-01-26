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
  public final static int MAX_DISPUTES = 2;
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
    List<Map<String,Object>> overdraftLogs = TestudoBankRepository.getOverdraftLogs(jdbcTemplate, user.getUsername());
    String logs = HTML_LINE_BREAK;
    for(Map<String, Object> overdraftLog : overdraftLogs){
      logs += overdraftLog + HTML_LINE_BREAK;
    }

    List<Map<String,Object>> transactionLogs = TestudoBankRepository.getRecentTransactions(jdbcTemplate, user.getUsername(), MAX_NUM_TRANSACTIONS_DISPLAYED);
    String transactionHistoryOutput = HTML_LINE_BREAK;
    for(Map<String, Object> transactionLog : transactionLogs){
      transactionHistoryOutput += transactionLog + HTML_LINE_BREAK;
    }

    String getTransferLogsToSql = String.format("SELECT * FROM TransferHistory WHERE TransferFrom='%s';", user.getUsername());
    List<Map<String,Object>> transferLogsTo = jdbcTemplate.queryForList(getTransferLogsToSql);
    String getTransferLogsFromSql = String.format("SELECT * FROM TransferHistory WHERE TransferTo='%s';", user.getUsername());
    List<Map<String,Object>> transferLogsFrom = jdbcTemplate.queryForList(getTransferLogsFromSql);
    String transferLogsToOutput = HTML_LINE_BREAK;
    for(Map<String, Object> transferLogTo : transferLogsTo){
      transferLogsToOutput += transferLogTo + HTML_LINE_BREAK;
    }
    String transferLogsFromOutput = HTML_LINE_BREAK;
    for(Map<String, Object> transferLogFrom : transferLogsFrom){
      transferLogsFromOutput += transferLogFrom + HTML_LINE_BREAK;
    }

    String getUserNameAndBalanceAndOverDraftBalanceSql = String.format("SELECT FirstName, LastName, Balance, OverdraftBalance FROM Customers WHERE CustomerID='%s';", user.getUsername());
    List<Map<String,Object>> queryResults = jdbcTemplate.queryForList(getUserNameAndBalanceAndOverDraftBalanceSql);
    Map<String,Object> userData = queryResults.get(0);

    user.setFirstName((String)userData.get("FirstName"));
    user.setLastName((String)userData.get("LastName"));
    user.setBalance((int)userData.get("Balance")/100.0);
    double overDraftBalance = (int)userData.get("OverdraftBalance");
    user.setOverDraftBalance(overDraftBalance/100);
    user.setLogs(logs);
    user.setTransactionHist(transactionHistoryOutput);
    user.setTransferLogs(transferLogsToOutput + transferLogsFromOutput);
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
   * If the user is currently not in overdraft, the deposit amount is simply
   * added to the user's main balance.
   * 
   * If the user is in overdraft, the deposit amount first pays off the overdraft balance,
   * and any excess deposit amount is added to the main balance.
   * 
   * @param user
   * @return "account_info" page if valid deposit request. Otherwise, redirect to "welcome" page.
   */
  @PostMapping("/deposit")
  public String submitDeposit(@ModelAttribute("user") User user) {
    String userID = user.getUsername();
    String userPasswordAttempt = user.getPassword();
    String userPassword = TestudoBankRepository.getCustomerPassword(jdbcTemplate, userID);

    //// Invalid Input/State Handling ////

    // unsuccessful login
    if (userPasswordAttempt.equals(userPassword) == false) {
      return "welcome";
    }

    // If customer already has too many reversals, their account is frozen. Don't complete deposit.
    int numOfReversals = TestudoBankRepository.getCustomerNumberOfReversals(jdbcTemplate, userID);
    if (numOfReversals >= MAX_DISPUTES){
      return "welcome";
    }

    // Negative deposit amount is not allowed
    double userDepositAmt = user.getAmountToDeposit();
    if (userDepositAmt < 0) {
      return "welcome";
    }
    
    //// Complete Deposit Transaction ////
    int userDepositAmtInPennies = convertDollarsToPennies(userDepositAmt); // dollar amounts stored as pennies to avoid floating point errors
    String currentTime = SQL_DATETIME_FORMATTER.format(new java.util.Date()); // use same timestamp for all logs created by this deposit
    int userOverdraftBalanceInPennies = TestudoBankRepository.getCustomerOverdraftBalanceInPennies(jdbcTemplate, userID);
    if (userOverdraftBalanceInPennies > 0) { // deposit will pay off overdraft first
      // update overdraft balance in Customers table, and log the repayment in OverdraftLogs table.
      int newOverdraftBalanceInPennies = Math.max(userOverdraftBalanceInPennies - userDepositAmtInPennies, 0);
      TestudoBankRepository.setCustomerOverdraftBalance(jdbcTemplate, userID, newOverdraftBalanceInPennies);
      TestudoBankRepository.insertRowToOverdraftLogsTable(jdbcTemplate, userID, currentTime, userDepositAmtInPennies, userOverdraftBalanceInPennies, newOverdraftBalanceInPennies);
      
      // add any excess deposit amount to main balance in Customers table
      if (userDepositAmtInPennies > userOverdraftBalanceInPennies) {
        int mainBalanceIncreaseAmtInPennies = userDepositAmtInPennies - userOverdraftBalanceInPennies;
        TestudoBankRepository.increaseCustomerBalance(jdbcTemplate, userID, mainBalanceIncreaseAmtInPennies);
      }

    } else { // simple deposit case
      TestudoBankRepository.increaseCustomerBalance(jdbcTemplate, userID, userDepositAmtInPennies);
    }

    // Adds deposit to transaction history logs
    TestudoBankRepository.insertRowToTransactionHistoryTable(jdbcTemplate, userID, currentTime, TRANSACTION_HISTORY_DEPOSIT_ACTION, userDepositAmtInPennies);

    // update Model so that View can access new main balance, overdraft balance, and logs
    updateAccountInfo(user);
    return "account_info";
  }
	
  /**
   * HTML POST request handler for the Withdraw Form page.
   * 
   * If the user is not currently in overdraft and the withdraw amount does not exceed the user's
   * current main balance, the main balance is decremented by the amount specified
   * 
   * If the withdraw amount exceeds the user's current main balance, the user's main balance is set to
   * 0 and the user's overdraft balance becomes the excess withdraw amount with interest applied.
   * 
   * If the user was already in overdraft, the entire withdraw amount with interest applied is added
   * to the existing overdraft balance.
   * 
   * @param user
   * @return "account_info" page if withdraw request is valid. Otherwise, redirect to "welcome" page.
   */
  @PostMapping("/withdraw")
  public String submitWithdraw(@ModelAttribute("user") User user) {
    String userID = user.getUsername();
    String userPasswordAttempt = user.getPassword();
    String userPassword = TestudoBankRepository.getCustomerPassword(jdbcTemplate, userID);

    //// Invalid Input/State Handling ////

    // unsuccessful login
    if (userPasswordAttempt.equals(userPassword) == false) {
      return "welcome";
    }

    // If customer already has too many reversals, their account is frozen. Don't complete deposit.
    int numOfReversals = TestudoBankRepository.getCustomerNumberOfReversals(jdbcTemplate, userID);
    if (numOfReversals >= MAX_DISPUTES){
      return "welcome";
    }

    // Negative deposit amount is not allowed
    double userWithdrawAmt = user.getAmountToWithdraw();
    if (userWithdrawAmt < 0) {
      return "welcome";
    }

    //// Complete Withdraw Transaction ////
    int userWithdrawAmtInPennies = convertDollarsToPennies(userWithdrawAmt); // dollar amounts stored as pennies to avoid floating point errors
    String currentTime = SQL_DATETIME_FORMATTER.format(new java.util.Date()); // use same timestamp for all logs created by this deposit
    int userBalanceInPennies = TestudoBankRepository.getCustomerBalanceInPennies(jdbcTemplate, userID);
    int userOverdraftBalanceInPennies = TestudoBankRepository.getCustomerOverdraftBalanceInPennies(jdbcTemplate, userID);
    if (userWithdrawAmtInPennies > userBalanceInPennies) { // if withdraw amount exceeds main balance, withdraw into overdraft with interest fee
      int excessWithdrawAmtInPennies = userWithdrawAmtInPennies - userBalanceInPennies;
      int newOverdraftIncreaseAmtAfterInterestInPennies = (int)(excessWithdrawAmtInPennies * INTEREST_RATE);
      int newOverdraftBalanceInPennies = userOverdraftBalanceInPennies + newOverdraftIncreaseAmtAfterInterestInPennies;

      // abort withdraw transaction if new overdraft balance exceeds max overdraft limit
      // IMPORTANT: Compare new overdraft balance to max overdraft limit AFTER applying the interest rate!
      if (newOverdraftBalanceInPennies > MAX_OVERDRAFT_IN_PENNIES) {
        return "welcome";
      }

      // this is a valid withdraw into overdraft, so we can set Balance column to 0.
      // OK to do this even if we were already in overdraft since main balance was already 0 anyways
      TestudoBankRepository.setCustomerBalance(jdbcTemplate, userID, 0);

      // increase overdraft balance by the withdraw amount after interest
      TestudoBankRepository.setCustomerOverdraftBalance(jdbcTemplate, userID, newOverdraftBalanceInPennies);

    } else { // simple, non-overdraft withdraw case
      TestudoBankRepository.decreaseCustomerBalance(jdbcTemplate, userID, userWithdrawAmtInPennies);
    }

    // Adds withdraw to transaction history
    TestudoBankRepository.insertRowToTransactionHistoryTable(jdbcTemplate, userID, currentTime, TRANSACTION_HISTORY_WITHDRAW_ACTION, userWithdrawAmtInPennies);

    // update Model so that View can access new main balance, overdraft balance, and logs
    updateAccountInfo(user);
    return "account_info";

  }

/**
   * HTML GET request handler that serves the "transfer_form" page to the user.
   * An empty `User` object is also added to the Model as an Attribute to store
   * the user's transfer form input.
   * 
   * @param model
   * @return "transfer_form" page
   */
  @GetMapping("/transfer")
	public String showTransferForm(Model model) {
    User user = new User();
		model.addAttribute("user", user);
		return "transfer_form";
	}

  /**
   * HTML POST request handler for the Transfer Form page.
   * 
   * The same username+password handling from the login page is used.
   * 
   * If the password attempt is correct, the users transfer successfully goes through
   * if it is a valid transfer. Both customers balances are properly updated.
   * 
   * If the password attempt is incorrect, the user is redirected to the "welcome" page.
   * 
   * @param user
   * @return "account_info" page if the transfer takes place successfully.
   */

  @PostMapping("/transfer")
  public String submitTransfer(@ModelAttribute("user") User user) {
    String userID = user.getUsername();
    String userPasswordAttempt = user.getPassword();
    String userPassword = TestudoBankRepository.getCustomerPassword(jdbcTemplate, userID);
    
    /// Invalid Input/State Handling ///

    // unsuccessful login
    if (userPasswordAttempt.equals(userPassword) == false) {
      return "welcome";
    }

    //If a customer already has too many reversals, or they try to send money to themselves, transfer fails.
    int numOfReversals = TestudoBankRepository.getCustomerNumberOfReversals(jdbcTemplate, userID);
    if (numOfReversals >= MAX_DISPUTES || user.getWhoToTransfer().equals(userID)) {
      return "welcome";
    }

    // Negative transfer amount is not allowed
    double transferAmt = user.getAmountToTransfer();
    if (transferAmt < 0) {
      return "welcome";
    }

    //checks to see the customer you are transfering to exists
    String getCustomerIDSql =  String.format("SELECT CustomerID FROM Customers WHERE CustomerID='%s';", user.getWhoToTransfer());
    try {
      jdbcTemplate.queryForObject(getCustomerIDSql, String.class);
    }
    catch(Exception e) {
      return "welcome";
    }    
    double senderWithdrawAmt = user.getAmountToTransfer();
    int senderWithdrawAmtInPennies = convertDollarsToPennies(senderWithdrawAmt);

    //Get the sender's balance in pennies.
    int userBalanceInPennies = TestudoBankRepository.getCustomerBalanceInPennies(jdbcTemplate, userID);
    
    // if the balance is not positive, withdraw with interest fee
    if (senderWithdrawAmtInPennies > userBalanceInPennies) {
      int userOverdraftBalanceInPennies = TestudoBankRepository.getCustomerOverdraftBalanceInPennies(jdbcTemplate, userID);

      // subtracts the remaining balance from withdrawal amount 
      int excessWithdrawAmtInPennies = senderWithdrawAmtInPennies - userBalanceInPennies;
      int newOverdraftIncreaseAmtAfterInterestInPennies = (int) (excessWithdrawAmtInPennies * INTEREST_RATE);
      if (newOverdraftIncreaseAmtAfterInterestInPennies > MAX_OVERDRAFT_IN_PENNIES) {
        return "welcome";
      }

      // factor in the existing overdraft balance before executing another overdraft
      if (newOverdraftIncreaseAmtAfterInterestInPennies + userOverdraftBalanceInPennies > MAX_OVERDRAFT_IN_PENNIES) {
        return "welcome";
      }

      // this is a valid overdraft, so we can set Balance column to 0
      TestudoBankRepository.setCustomerBalance(jdbcTemplate, userID, 0);

      //Set the overdraft balance to the previous overdraft + new overdraft
      int cumulativeOverdraftInPennies = userOverdraftBalanceInPennies + newOverdraftIncreaseAmtAfterInterestInPennies;

      // increase overdraft balance by the withdraw amount after interest
      TestudoBankRepository.setCustomerOverdraftBalance(jdbcTemplate, userID, cumulativeOverdraftInPennies);


    }
    else { // simple, non-overdraft withdraw case
      TestudoBankRepository.decreaseCustomerBalance(jdbcTemplate, userID, senderWithdrawAmtInPennies);
    }

    int transferAmtInPennies = convertDollarsToPennies(transferAmt);

    int userOverdraftBalanceInPennies = TestudoBankRepository.getCustomerOverdraftBalanceInPennies(jdbcTemplate, user.getWhoToTransfer());

    // if the overdraft balance is positive, subtract the deposit with interest
    if (userOverdraftBalanceInPennies > 0) {
      int newOverdraftBalanceInPennies = Math.max(userOverdraftBalanceInPennies - transferAmtInPennies, 0);
      String currentTime = SQL_DATETIME_FORMATTER.format(new java.util.Date());
      // Adds withdraw to transaction history
      TestudoBankRepository.insertRowToOverdraftLogsTable(jdbcTemplate, user.getWhoToTransfer(), currentTime, transferAmtInPennies, userOverdraftBalanceInPennies, newOverdraftBalanceInPennies);

      // updating customers table
      TestudoBankRepository.setCustomerOverdraftBalance(jdbcTemplate, user.getWhoToTransfer(), newOverdraftBalanceInPennies);
      
      // update Model so that View can access new main balance, overdraft balance, and logs
      updateAccountInfo(user);
    }

    // if in the overdraft case and there is excess deposit, deposit the excess amount.
    // otherwise, this is a non-overdraft case, so just use the userDepositAmt.
    int balanceIncreaseAmtInPennies = 0;
    if (userOverdraftBalanceInPennies > 0 && transferAmtInPennies > userOverdraftBalanceInPennies) {
      balanceIncreaseAmtInPennies = transferAmtInPennies - userOverdraftBalanceInPennies;
    } else if (userOverdraftBalanceInPennies > 0 && transferAmtInPennies <= userOverdraftBalanceInPennies) {
      balanceIncreaseAmtInPennies = 0; // overdraft case, but no excess deposit. don't increase balance column.
    } else {
      balanceIncreaseAmtInPennies = transferAmtInPennies;
    }

    //Increase the recipient's balance.
    TestudoBankRepository.increaseCustomerBalance(jdbcTemplate, user.getWhoToTransfer(), balanceIncreaseAmtInPennies);
    
    // Inserting transfer into transfer history for both customers
    TestudoBankRepository.insertRowToTransferLogsTable(jdbcTemplate, userID, user.getWhoToTransfer(), user.getAmountToTransfer());
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
    List<Map<String,Object>> transactionLogs = TestudoBankRepository.getRecentTransactions(jdbcTemplate, userID, MAX_NUM_TRANSACTIONS_DISPLAYED);
    
    // Ensure customer has enough transactions to complete the reversal
    if (user.getNumTransactionsAgo() > transactionLogs.size()) {
      return "welcome";
    }

    // Retrieve correct log based on what transaction user wants to reverse
    Map<String, Object> logToReverse = transactionLogs.get(user.getNumTransactionsAgo() - 1);

    // Get balance and overdraft balance
    int userBalanceInPennies = TestudoBankRepository.getCustomerBalanceInPennies(jdbcTemplate, userID);
    int userOverdraftBalanceInPennies = TestudoBankRepository.getCustomerOverdraftBalanceInPennies(jdbcTemplate, userID);

    int reversalAmountInPennies = (int) logToReverse.get("Amount");
    double reversalAmount = reversalAmountInPennies / 100.0;

    // If transaction to reverse is a deposit, then withdraw the money out
    if (((String) logToReverse.get("Action")).toLowerCase().equals("deposit")) {
      // if withdraw would exceed max overdraft possible, return welcome
      if (userOverdraftBalanceInPennies + (reversalAmountInPennies - userBalanceInPennies) > MAX_OVERDRAFT_IN_PENNIES) {
        return "welcome";
      }
      user.setAmountToWithdraw(reversalAmount);
      submitWithdraw(user);

      // If reversing a deposit puts customer back in overdraft
      if (reversalAmountInPennies > userBalanceInPennies){
        // check if the reversed deposit helped pay off overdraft balance
        List<Map<String,Object>> overdraftLogs = TestudoBankRepository.getOverdraftLogs(jdbcTemplate, userID, (String)logToReverse.get("Timestamp"));
        if (overdraftLogs.size() != 0) {
          // remove extra entry from overdraft logs
          TestudoBankRepository.deleteRowFromOverdraftLogsTable(jdbcTemplate, userID, (String)logToReverse.get("Timestamp"));
        }
      } 
    } else { // Case when reversing a withdraw, deposit the money instead
      user.setAmountToDeposit(reversalAmount);
      submitDeposit(user);
    }

    // Adds to number of reversals only after a successful reversal 
    numOfReversals++;
    TestudoBankRepository.setCustomerNumFraudReversals(jdbcTemplate, userID, numOfReversals);

    updateAccountInfo(user);

    return "account_info";
  }

}