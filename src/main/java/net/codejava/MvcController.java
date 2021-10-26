package net.codejava;

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
  /**
   * A simplified JDBC client that is injected with the login credentials
   * specified in /src/main/resources/application.properties
   */
  private JdbcTemplate jdbcTemplate;
  private static java.util.Date dt = new java.util.Date();
  private static java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  private final static String SQL_DATETIME_FORMAT = sdf.format(dt);
  private final static float INTEREST = 1.02f;
  private final static int MAX_OVERDRAFT_IN_PENNIES = 100000;
  private static final String HTML_LINE_BREAK = "<br/>";

  public MvcController(@Autowired JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

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
   * Helper method that queries the MySQL DB for the customer account info (First Name, Last Name, and Balance)
   * and adds these values to the `user` Model Attribute so that they can be displayed in the "account_info" page.
   * 
   * @param user
   */
  private void updateAccountInfo(User user) {
    String getUserNameAndBalanceAndOverDraftBalanceSql = String.format("SELECT FirstName, LastName, Balance, OverdraftBalance FROM customers WHERE CustomerID='%s';", user.getUsername());
    List<Map<String,Object>> queryResults = jdbcTemplate.queryForList(getUserNameAndBalanceAndOverDraftBalanceSql);
    String getOverDraftLogsSql = String.format("SELECT * FROM OverdraftLogs WHERE CustomerID='%s';", user.getUsername());
    
    List<Map<String,Object>> queryLogs = jdbcTemplate.queryForList(getOverDraftLogsSql);
    String logs = HTML_LINE_BREAK;
    for(Map<String, Object> x : queryLogs){
      logs += x + HTML_LINE_BREAK;
    }
    Map<String,Object> userData = queryResults.get(0);

    user.setFirstName((String)userData.get("FirstName"));
    user.setLastName((String)userData.get("LastName"));
    user.setBalance((int)userData.get("Balance"));
    double overDraftBalance = (int)userData.get("OverdraftBalance");
    user.setOverDraftBalance(overDraftBalance/100);
    user.setLogs(logs);
  }

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
    String getUserPasswordSql = String.format("SELECT Password FROM passwords WHERE CustomerID='%s';", userID);
    String userPassword = jdbcTemplate.queryForObject(getUserPasswordSql, String.class);

    if (userPasswordAttempt.equals(userPassword)) {
      updateAccountInfo(user);

      return "account_info";
    } else {
      return "welcome";
    }
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
		System.out.println(user); // Print user's existing fields for debugging

    String userID = user.getUsername();
    String userPasswordAttempt = user.getPassword();

    // Retrieve correct password for this customer.
    String getUserPasswordSql = String.format("SELECT Password FROM passwords WHERE CustomerID='%s';", userID);
    String userPassword = jdbcTemplate.queryForObject(getUserPasswordSql, String.class);

    if (userPasswordAttempt.equals(userPassword)) {
      
      // Execute SQL Update command that increments user's Balance by given amount from the deposit form.

      if(user.getAmountToDeposit() < 0){
        return "welcome";
      }

      String getUserOverdraftBalanceSql = String.format("SELECT OverdraftBalance FROM customers WHERE CustomerID='%s';", userID);
      String userOverdraftBalance = jdbcTemplate.queryForObject(getUserOverdraftBalanceSql, String.class);


      int userOverDraftBalanceFlag = 0; // to confirm the passed value is integer
      if(Character.isDigit(userOverdraftBalance.charAt(0))){
        userOverDraftBalanceFlag = Integer.parseInt(userOverdraftBalance);
      }
      // if the overdraft balance is positive, subtract the deposit with interest
      if(userOverDraftBalanceFlag > 0){
        int leftOver = user.getAmountToDeposit() - Integer.parseInt(userOverdraftBalance); // remaining value after clearing overdraft
        float overDraftMoney = ((float)user.getAmountToDeposit());
        int newOverdraftBalance = Integer.parseInt(userOverdraftBalance) - (int)overDraftMoney;
        // avoiding overdraft balance < 0
        if(leftOver > 0){
          newOverdraftBalance = 0;
        }
        
        
        // inserting into the database information about payment log
        String overDraftInsertSql = String.format("INSERT INTO OverdraftLogs VALUES ('%s' , '%s', %d, %d, %d);", userID, SQL_DATETIME_FORMAT,
        user.getAmountToDeposit(), Integer.parseInt(userOverdraftBalance), newOverdraftBalance);
        jdbcTemplate.update(overDraftInsertSql);

        // updating cusomters table
        String overDraftBalanceUpdateSql = String.format("UPDATE Customers SET OverdraftBalance = %d WHERE CustomerID='%s';", 
        newOverdraftBalance, userID);
        jdbcTemplate.update(overDraftBalanceUpdateSql);
        updateAccountInfo(user);
        if(leftOver > 0){
          String balanceIncreaseSql = String.format("UPDATE Customers SET Balance = Balance + %d WHERE CustomerID='%s';", leftOver/100, userID);
          System.out.println(balanceIncreaseSql); // Print executed SQL update for debugging
          jdbcTemplate.update(balanceIncreaseSql);

          updateAccountInfo(user);

          return "account_info";
        } else {
          return "account_info";
        }
      }

      String balanceIncreaseSql = String.format("UPDATE Customers SET Balance = Balance + %d WHERE CustomerID='%s';", user.getAmountToDeposit()/100, userID);
      System.out.println(balanceIncreaseSql); // Print executed SQL update for debugging
      jdbcTemplate.update(balanceIncreaseSql);

      updateAccountInfo(user);

      return "account_info";
    } else {
      return "welcome";
    }
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
    
    String getUserPasswordSql = String.format("SELECT Password FROM passwords WHERE CustomerID='%s';", userID);

    String userPassword = jdbcTemplate.queryForObject(getUserPasswordSql, String.class);

    if (userPasswordAttempt.equals(userPassword)) {
      // Execute SQL Update command that decrements Balance value for
      // user's row in Customers table using user.getAmountToWithdraw()

      if(user.getAmountToWithdraw() < 0){
        return "welcome";
      }
      String getUserBalanceSql =  String.format("SELECT Balance FROM customers WHERE CustomerID='%s';", userID);
      String userBalance = jdbcTemplate.queryForObject(getUserBalanceSql, String.class);
      
      int theUserBalance = MAX_OVERDRAFT_IN_PENNIES; // to confirm the passed value is integer
      if(Character.isDigit(userBalance.charAt(0))){
        theUserBalance = Integer.parseInt(userBalance);
      }
      // if the balance is not positive, withdraw with interest fee
      if(theUserBalance*100 - user.getAmountToWithdraw() < 0){
        int effectiveWithdrawal = user.getAmountToWithdraw();
        // subtracts the remaining balance from withdrawal amount if balance > 0
        if(theUserBalance > 0){
          String updateBalanceSql = String.format("UPDATE Customers SET Balance = %d WHERE CustomerID='%s';", 0, userID);
          effectiveWithdrawal = effectiveWithdrawal - theUserBalance*100;
          // user cannot afford this withdrawal, terminate
          if(effectiveWithdrawal > MAX_OVERDRAFT_IN_PENNIES) {
            return "welcome";
          }
          jdbcTemplate.update(updateBalanceSql);
        }
        String getUserOverdraftBalanceSql = String.format("SELECT OverdraftBalance FROM customers WHERE CustomerID='%s';", userID);
        String userOverdraftBalance = jdbcTemplate.queryForObject(getUserOverdraftBalanceSql, String.class);

        if(effectiveWithdrawal + Integer.parseInt(userOverdraftBalance) <= MAX_OVERDRAFT_IN_PENNIES){
          int newOverdraftBalance = Integer.parseInt(userOverdraftBalance);
          float afterInterest = (float)effectiveWithdrawal*INTEREST;
          String overDraftBalanceUpdateSql = String.format("UPDATE Customers SET OverdraftBalance = %d WHERE CustomerID='%s';", 
          newOverdraftBalance + (int)afterInterest, userID);
          jdbcTemplate.update(overDraftBalanceUpdateSql);
          System.out.println(overDraftBalanceUpdateSql);
          updateAccountInfo(user);
          return "account_info";
        } else {
          return "welcome";
        }

      }

      String balanceDecreaseSql = String.format("UPDATE Customers SET Balance = Balance - %d WHERE CustomerID='%s';", user.getAmountToWithdraw()/100, userID);
      System.out.println(balanceDecreaseSql);
      jdbcTemplate.update(balanceDecreaseSql);

      updateAccountInfo(user);

      return "account_info";
    } else {
      return "welcome";
    }
  }
}