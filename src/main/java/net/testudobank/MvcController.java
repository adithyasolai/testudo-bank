package net.testudobank;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

@Controller
public class MvcController {
  
  // A simplified JDBC client that is injected with the login credentials
  // specified in /src/main/resources/application.properties
  private JdbcTemplate jdbcTemplate;

  // Client to get crypto price
  private CryptoPriceClient cryptoPriceClient;

  // Formatter for converting Java Dates to SQL-compatible DATETIME Strings
  private static java.text.SimpleDateFormat SQL_DATETIME_FORMATTER = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  //// CONSTANT LITERALS ////
  public final static double INTEREST_RATE = 1.02;
  private final static int MAX_OVERDRAFT_IN_PENNIES = 100000;
  public final static int MAX_DISPUTES = 2;
  private final static int MAX_NUM_TRANSACTIONS_DISPLAYED = 3;
  private final static int MAX_NUM_TRANSFERS_DISPLAYED = 10;
  private final static int MAX_REVERSABLE_TRANSACTIONS_AGO = 3;
  private final static String HTML_LINE_BREAK = "<br/>";
  public static String TRANSACTION_HISTORY_DEPOSIT_ACTION = "Deposit";
  public static String TRANSACTION_HISTORY_WITHDRAW_ACTION = "Withdraw";
  public static String TRANSACTION_HISTORY_TRANSFER_SEND_ACTION = "TransferSend";
  public static String TRANSACTION_HISTORY_TRANSFER_RECEIVE_ACTION = "TransferReceive";
  public static String TRANSACTION_HISTORY_CRYPTO_SELL_ACTION = "CryptoSell";
  public static String TRANSACTION_HISTORY_CRYPTO_BUY_ACTION = "CryptoBuy";
  public static String CRYPTO_HISTORY_SELL_ACTION = "Sell";
  public static String CRYPTO_HISTORY_BUY_ACTION = "Buy";
  public static Set<String> SUPPORTED_CRYPTOCURRENCIES = new HashSet<>(Arrays.asList("ETH", "SOL"));
  private static double BALANCE_INTEREST_RATE = 1.015;

  public MvcController(@Autowired JdbcTemplate jdbcTemplate, @Autowired CryptoPriceClient cryptoPriceClient) {
    this.jdbcTemplate = jdbcTemplate;
    this.cryptoPriceClient = cryptoPriceClient;
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

  /**
   * HTML GET request handler that serves the "transfer_form" page to the user.
   * An empty `User` object is also added to the Model as an Attribute to store
   * the user's transfer form input.
   * 
   * @param model
   * @return "dispute_form" page
   */
  @GetMapping("/transfer")
	public String showTransferForm(Model model) {
    User user = new User();
		model.addAttribute("user", user);
		return "transfer_form";
	}

  /**
   * HTML GET request handler that serves the "buycrypto_form" page to the user.
   * An empty `User` object is also added to the Model as an Attribute to store
   * the user's input for buying cryptocurrency.
   * 
   * @param model
   * @return "buycrypto_form" page
   */
  @GetMapping("/buycrypto")
	public String showBuyCryptoForm(Model model) {
    User user = new User();
    user.setEthPrice(cryptoPriceClient.getCurrentEthValue());
    user.setSolPrice(cryptoPriceClient.getCurrentSolValue());
		model.addAttribute("user", user);
		return "buycrypto_form";
	}

  /**
   * HTML GET request handler that serves the "sellcrypto_form" page to the user.
   * An empty `User` object is also added to the Model as an Attribute to store
   * the user's input for selling cryptocurrency.
   * 
   * @param model
   * @return "sellcrypto_form" page
   */
  @GetMapping("/sellcrypto")
	public String showSellCryptoForm(Model model) {
    User user = new User();
    user.setEthPrice(cryptoPriceClient.getCurrentEthValue());
    user.setSolPrice(cryptoPriceClient.getCurrentSolValue());
		model.addAttribute("user", user);
		return "sellcrypto_form";
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

    List<Map<String,Object>> transferLogs = TestudoBankRepository.getTransferLogs(jdbcTemplate, user.getUsername(), MAX_NUM_TRANSFERS_DISPLAYED);
    String transferHistoryOutput = HTML_LINE_BREAK;
    for(Map<String, Object> transferLog : transferLogs){
      transferHistoryOutput += transferLog + HTML_LINE_BREAK;
    }

    List<Map<String, Object>> cryptoLogs = TestudoBankRepository.getCryptoLogs(jdbcTemplate, user.getUsername());
    StringBuilder cryptoHistoryOutput = new StringBuilder(HTML_LINE_BREAK);
    for (Map<String, Object> cryptoLog : cryptoLogs) {
      cryptoHistoryOutput.append(cryptoLog).append(HTML_LINE_BREAK);
    }

    String getUserNameAndBalanceAndOverDraftBalanceSql = String.format("SELECT FirstName, LastName, Balance, OverdraftBalance, NumDepositsForInterest FROM Customers WHERE CustomerID='%s';", user.getUsername());
    List<Map<String,Object>> queryResults = jdbcTemplate.queryForList(getUserNameAndBalanceAndOverDraftBalanceSql);
    Map<String,Object> userData = queryResults.get(0);

    // calculate total Crypto holdings balance by summing balance of each supported cryptocurrency
    double cryptoBalanceInDollars = 0;
    for (String cryptoName : MvcController.SUPPORTED_CRYPTOCURRENCIES) {
      cryptoBalanceInDollars += TestudoBankRepository.getCustomerCryptoBalance(jdbcTemplate, user.getUsername(), cryptoName).orElse(0.0) * cryptoPriceClient.getCurrentCryptoValue(cryptoName);
    }

    user.setFirstName((String)userData.get("FirstName"));
    user.setLastName((String)userData.get("LastName"));
    user.setBalance((int)userData.get("Balance")/100.0);
    double overDraftBalance = (int)userData.get("OverdraftBalance");
    user.setOverDraftBalance(overDraftBalance/100);
    user.setCryptoBalanceUSD(cryptoBalanceInDollars);
    user.setLogs(logs);
    user.setTransactionHist(transactionHistoryOutput);
    user.setTransferHist(transferHistoryOutput);
    user.setCryptoHist(cryptoHistoryOutput.toString());
    user.setEthBalance(TestudoBankRepository.getCustomerCryptoBalance(jdbcTemplate, user.getUsername(), "ETH").orElse(0.0));
    user.setSolBalance(TestudoBankRepository.getCustomerCryptoBalance(jdbcTemplate, user.getUsername(), "SOL").orElse(0.0));
    user.setEthPrice(cryptoPriceClient.getCurrentEthValue());
    user.setSolPrice(cryptoPriceClient.getCurrentSolValue());
    user.setNumDepositsForInterest(user.getNumDepositsForInterest());
  }

  // Converts dollar amounts in frontend to penny representation in backend MySQL DB
  private static int convertDollarsToPennies(double dollarAmount) {
    return (int) (dollarAmount * 100);
  }

  // Converts LocalDateTime to Date variable
  private static Date convertLocalDateTimeToDate(LocalDateTime ldt){
    Date dateTime = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    return dateTime;
  }

  // HTML POST HANDLERS ////

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
        TestudoBankRepository.increaseCustomerCashBalance(jdbcTemplate, userID, mainBalanceIncreaseAmtInPennies);
      }

    } else { // simple deposit case
      TestudoBankRepository.increaseCustomerCashBalance(jdbcTemplate, userID, userDepositAmtInPennies);
    }

    // only adds deposit to transaction history if is not transfer
    if (user.isTransfer()){
      // Adds transaction recieve to transaction history
      TestudoBankRepository.insertRowToTransactionHistoryTable(jdbcTemplate, userID, currentTime, TRANSACTION_HISTORY_TRANSFER_RECEIVE_ACTION, userDepositAmtInPennies);
    } else if (user.isCryptoTransaction()) {
      TestudoBankRepository.insertRowToTransactionHistoryTable(jdbcTemplate, userID, currentTime, TRANSACTION_HISTORY_CRYPTO_SELL_ACTION, userDepositAmtInPennies);
    } else {
      // Adds deposit to transaction history
      TestudoBankRepository.insertRowToTransactionHistoryTable(jdbcTemplate, userID, currentTime, TRANSACTION_HISTORY_DEPOSIT_ACTION, userDepositAmtInPennies);
    }

    // update Model so that View can access new main balance, overdraft balance, and logs
    applyInterest(user);
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
    int userBalanceInPennies = TestudoBankRepository.getCustomerCashBalanceInPennies(jdbcTemplate, userID);
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
      TestudoBankRepository.setCustomerCashBalance(jdbcTemplate, userID, 0);

      // increase overdraft balance by the withdraw amount after interest
      TestudoBankRepository.setCustomerOverdraftBalance(jdbcTemplate, userID, newOverdraftBalanceInPennies);

    } else { // simple, non-overdraft withdraw case
      TestudoBankRepository.decreaseCustomerCashBalance(jdbcTemplate, userID, userWithdrawAmtInPennies);
    }

    // only adds withdraw to transaction history if is not transfer
    if (user.isTransfer()){
      // Adds transfer send to transaction history
      TestudoBankRepository.insertRowToTransactionHistoryTable(jdbcTemplate, userID, currentTime, TRANSACTION_HISTORY_TRANSFER_SEND_ACTION, userWithdrawAmtInPennies);
    } else if (user.isCryptoTransaction()) {
      TestudoBankRepository.insertRowToTransactionHistoryTable(jdbcTemplate, userID, currentTime, TRANSACTION_HISTORY_CRYPTO_BUY_ACTION, userWithdrawAmtInPennies);
    } else {
      // Adds withdraw to transaction history
      TestudoBankRepository.insertRowToTransactionHistoryTable(jdbcTemplate, userID, currentTime, TRANSACTION_HISTORY_WITHDRAW_ACTION, userWithdrawAmtInPennies);
    }

  
    // update Model so that View can access new main balance, overdraft balance, and logs
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
    int userBalanceInPennies = TestudoBankRepository.getCustomerCashBalanceInPennies(jdbcTemplate, userID);
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
        // if it did, do not re-apply the interest rate after the reversal of the deposit since the customer was already in overdraft
        String datetimeOfReversedDeposit = SQL_DATETIME_FORMATTER.format(convertLocalDateTimeToDate((LocalDateTime)logToReverse.get("Timestamp")));
        List<Map<String,Object>> overdraftLogs = TestudoBankRepository.getOverdraftLogs(jdbcTemplate, userID, datetimeOfReversedDeposit);

        // fetch updated overdraft balance with extra interest rate applied
        double updatedOverdraftBalanceInPennies = TestudoBankRepository.getCustomerOverdraftBalanceInPennies(jdbcTemplate, userID);
        // reverse extra application of interest rate since customer was already in overdraft
        int newOverdraftBalanceInPennies = (int) (updatedOverdraftBalanceInPennies / 1.02);

        if (overdraftLogs.size() != 0) {
          // remove extra entry from overdraft logs
          TestudoBankRepository.deleteRowFromOverdraftLogsTable(jdbcTemplate, userID, datetimeOfReversedDeposit);
          TestudoBankRepository.setCustomerOverdraftBalance(jdbcTemplate, userID, newOverdraftBalanceInPennies);
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
   * Transfer function is implemented by re-using deposit and withdraw handlers to 
   * facilitate a transfer between 2 users.
   * 
   * @param user
   * @return "account_info" page if login successful. Otherwise, redirect to "welcome" page.
   */
  @PostMapping("/transfer")
  public String submitTransfer(@ModelAttribute("user") User sender) {

    // checks to see the customer you are transfering to exists
    if (!TestudoBankRepository.doesCustomerExist(jdbcTemplate, sender.getTransferRecipientID())){
      return "welcome";
    }

    String senderUserID = sender.getUsername();
    String senderPasswordAttempt = sender.getPassword();
    String senderPassword = TestudoBankRepository.getCustomerPassword(jdbcTemplate, senderUserID);

    // creates new user for recipient
    User recipient = new User();
    String recipientUserID = sender.getTransferRecipientID();
    String recipientPassword = TestudoBankRepository.getCustomerPassword(jdbcTemplate, recipientUserID);
    recipient.setUsername(recipientUserID);
    recipient.setPassword(recipientPassword);

    // sets isTransfer to true for sender and recipient
    sender.setTransfer(true);
    recipient.setTransfer(true);

    /// Invalid Input/State Handling ///

    // unsuccessful login
    if (senderPasswordAttempt.equals(senderPassword) == false) {
      return "welcome";
    }

    // case where customer already has too many reversals
    int numOfReversals = TestudoBankRepository.getCustomerNumberOfReversals(jdbcTemplate, senderUserID);
    if (numOfReversals >= MAX_DISPUTES) {
      return "welcome";
    }

    // case where customer tries to send money to themselves
    if (sender.getTransferRecipientID().equals(senderUserID)){
      return "welcome";
    }

    // initialize variables for transfer amount
    double transferAmount = sender.getAmountToTransfer();
    int transferAmountInPennies = convertDollarsToPennies(transferAmount);

    // negative transfer amount is not allowed
    if (transferAmount < 0) {
      return "welcome";
    } 
  
    String currentTime = SQL_DATETIME_FORMATTER.format(new java.util.Date()); // use same timestamp for all logs created by this transfer

    // withdraw transfer amount from sender and deposit into recipient's account
    sender.setAmountToWithdraw(transferAmount);
    submitWithdraw(sender);

    recipient.setAmountToDeposit(transferAmount);
    submitDeposit(recipient);

    // Inserting transfer into transfer history for both customers
    TestudoBankRepository.insertRowToTransferLogsTable(jdbcTemplate, senderUserID, recipientUserID, currentTime, transferAmountInPennies);
    updateAccountInfo(sender);

    return "account_info";
  }

  /**
   * HTML POST request handler for the Buy Crypto Form page.
   * <p>
   * The same username+password handling from the login page is used.
   * <p>
   * If the password attempt is correct, the user is not in overdraft,
   * and the purchase amount is a valid amount that does not exceed balance,
   * the cost of the cryptocurrency in cash will be subtracted from the users balance,
   * and cryptocurrency will be added to the users account
   * <p>
   * If the password attempt is incorrect or the amount to purchase is invalid,
   * the user is redirected to the "welcome" page.
   * <p>
   * Crypto purchase function is implemented by re-using withdraw handler.
   *
   * @param user
   * @return "account_info" page if buy successful. Otherwise, redirect to "welcome" page.
   */
  @PostMapping("/buycrypto")
  public String buyCrypto(@ModelAttribute("user") User user) {

    String userID = user.getUsername();
    String userPasswordAttempt = user.getPassword();
    String userPassword = TestudoBankRepository.getCustomerPassword(jdbcTemplate, userID);

    //// Invalid Input/State Handling ////

    // unsuccessful login
    if (!userPasswordAttempt.equals(userPassword)) {
      return "welcome";
    }

    // must buy a supported cryptocurrency
    String cryptoToBuy = user.getWhichCryptoToBuy();
    if (MvcController.SUPPORTED_CRYPTOCURRENCIES.contains(cryptoToBuy) == false) {
      return "welcome";
    }

    // must buy a positive amount
    double cryptoAmountToBuy = user.getAmountToBuyCrypto();
    if (cryptoAmountToBuy <= 0) {
      return "welcome";
    }

    // cannot buy crypto while in overdraft
    int userOverdraftBalanceInPennies = TestudoBankRepository.getCustomerOverdraftBalanceInPennies(jdbcTemplate, userID);
    if (userOverdraftBalanceInPennies > 0) {
      return "welcome";
    }

    // calculate how much it will cost to buy currently
    double costOfCryptoPurchaseInDollars = cryptoPriceClient.getCurrentCryptoValue(cryptoToBuy) * cryptoAmountToBuy;

    // possible for web scraper to fail and return a negative value, abort if so
    if (costOfCryptoPurchaseInDollars < 0) {
      return "welcome";
    }

    double costOfCryptoPurchaseInPennies = convertDollarsToPennies(costOfCryptoPurchaseInDollars);

    int userBalanceInPennies = TestudoBankRepository.getCustomerCashBalanceInPennies(jdbcTemplate, userID);

    // check if balance will cover purchase
    if (costOfCryptoPurchaseInPennies > userBalanceInPennies) {
      return "welcome";
    }

    String currentTime = SQL_DATETIME_FORMATTER.format(new java.util.Date());

    // buy crypto
    user.setAmountToWithdraw(costOfCryptoPurchaseInDollars);
    user.setCryptoTransaction(true);

    // TODO: I don't like how this is dependent on a string return value. Withdraw logic should probably be extracted
    String withdrawResponse = submitWithdraw(user);

    if (withdrawResponse.equals("account_info")) {

      // create an entry in CryptoHoldings table if customer is buying this Crypto for the first time.
      if (!TestudoBankRepository.getCustomerCryptoBalance(jdbcTemplate, userID, cryptoToBuy).isPresent()) {
        TestudoBankRepository.initCustomerCryptoBalance(jdbcTemplate, userID, cryptoToBuy);
      }

      TestudoBankRepository.increaseCustomerCryptoBalance(jdbcTemplate, userID, cryptoToBuy, cryptoAmountToBuy);
      TestudoBankRepository.insertRowToCryptoLogsTable(jdbcTemplate, userID, cryptoToBuy, CRYPTO_HISTORY_BUY_ACTION, currentTime, cryptoAmountToBuy);

      updateAccountInfo(user);

      return "account_info";
    } else {
      return "welcome";
    }

  }

  /**
   * HTML POST request handler for the Sell Crypto Form page.
   * <p>
   * The same username+password handling from the login page is used.
   * <p>
   * If the password attempt is correct, and the purchase amount is a valid amount
   * that does not exceed crypto balance, the cost of the cryptocurrency in cash will be
   * added to the users cash balance, and cryptocurrency will be subtracted from the users account
   * <p>
   * If the password attempt is incorrect or the amount to purchase is invalid,
   * the user is redirected to the "welcome" page.
   * <p>
   * Crypto purchase function is implemented by re-using deposit handler.
   * Logic of deposit (applying to overdraft, adding to balance, etc.) is delegated to this handler.
   *
   * @param user
   * @return "account_info" page if sell successful. Otherwise, redirect to "welcome" page.
   */
  @PostMapping("/sellcrypto")
  public String sellCrypto(@ModelAttribute("user") User user) {
    String userID = user.getUsername();
    String userPasswordAttempt = user.getPassword();
    String userPassword = TestudoBankRepository.getCustomerPassword(jdbcTemplate, userID);

    //// Invalid Input/State Handling ////

    // unsuccessful login
    if (!userPasswordAttempt.equals(userPassword)) {
      return "welcome";
    }

    // must buy a supported cryptocurrency
    String cryptoToBuy = user.getWhichCryptoToBuy();
    if (MvcController.SUPPORTED_CRYPTOCURRENCIES.contains(cryptoToBuy) == false) {
      return "welcome";
    }

    // must sell a positive amount
    double cryptoAmountToSell = user.getAmountToSellCrypto();
    if (cryptoAmountToSell <= 0) {
      return "welcome";
    }

    // possible for user to not have any crypto
    Optional<Double> cryptoBalance = TestudoBankRepository.getCustomerCryptoBalance(jdbcTemplate, userID, cryptoToBuy);
    if (!cryptoBalance.isPresent()) {
      return "welcome";
    }

    // check if user has required crypto balance
    // TODO: comparing doubles like this is probably not a good idea
    if (cryptoBalance.get() < cryptoAmountToSell) {
      return "welcome";
    }

    double cryptoValueInDollars = cryptoPriceClient.getCurrentCryptoValue(cryptoToBuy) * cryptoAmountToSell;

    String currentTime = SQL_DATETIME_FORMATTER.format(new java.util.Date());

    user.setAmountToDeposit(cryptoValueInDollars);
    user.setCryptoTransaction(true);

    // TODO: I don't like how this is dependent on a string return value. Deposit logic should probably be extracted
    String depositResponse = submitDeposit(user);

    if (depositResponse.equals("account_info")) {

      TestudoBankRepository.decreaseCustomerCryptoBalance(jdbcTemplate, userID, cryptoToBuy, cryptoAmountToSell);
      TestudoBankRepository.insertRowToCryptoLogsTable(jdbcTemplate, userID, cryptoToBuy, CRYPTO_HISTORY_SELL_ACTION, currentTime, cryptoAmountToSell);

      updateAccountInfo(user);

      return "account_info";
    } else {
      return "welcome";
    }
  }

  /**
   * 
   * 
   * @param user
   * @return "account_info" if interest applied. Otherwise, redirect to "welcome" page.
   */
  public String applyInterest(@ModelAttribute("user") User user) {

    return "welcome";

  }

}