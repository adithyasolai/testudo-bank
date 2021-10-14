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
    String getUserNameAndBalanceSql = String.format("SELECT FirstName, LastName, Balance, OverdraftBalance FROM customers WHERE CustomerID='%s';", user.getUsername());
    List<Map<String,Object>> queryResults = jdbcTemplate.queryForList(getUserNameAndBalanceSql);
    Map<String,Object> userData = queryResults.get(0);

    user.setFirstName((String)userData.get("FirstName"));
    user.setLastName((String)userData.get("LastName"));
    user.setBalance((int)userData.get("Balance"));
    user.setOverdraftBalance((int)userData.get("OverdraftBalance"));

    String getOverdraftLogs = String.format("SELECT Timestamp, DepositAmt, OldOverBalance, NewOverBalance FROM overdraftLogs WHERE CustomerID='%s';", user.getUsername());
    queryResults = jdbcTemplate.queryForList(getOverdraftLogs);
    String logs = "      Timestamp     DepositAmt  OldOverBalance  NewOverBalance\n";
    
    for (int i = 0; i < queryResults.size(); i++) {
      userData = queryResults.get(i);
      logs += userData.get("Timestamp") + "            " + userData.get("DepositAmt") + "     " + userData.get("OldOverBalance") 
        + "      " + userData.get("NewOverBalance") + "\n";
    }

    user.setOverdraftLogs(logs);
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

    // Get current overdraft balance
    String getOverdraftBalanceSql = String.format("SELECT OverdraftBalance FROM customers WHERE CustomerID='%s';", userID);
    int curOverBal = jdbcTemplate.queryForObject(getOverdraftBalanceSql, Integer.class);
    curOverBal *= 100;
    int amtDepositAdj = user.getAmountToDeposit() * 100;
    int interest = amtDepositAdj / 102;
    amtDepositAdj -= interest;

    // Retrieve correct password for this customer.
    String getUserPasswordSql = String.format("SELECT Password FROM passwords WHERE CustomerID='%s';", userID);
    String userPassword = jdbcTemplate.queryForObject(getUserPasswordSql, String.class);

    if (userPasswordAttempt.equals(userPassword)) {
      if (curOverBal > 0) { // Checking if user has an overdrafted balance
        if (amtDepositAdj >= curOverBal) { // If deposit is greater than current overdraft
          int depAmt = (amtDepositAdj - curOverBal) / 100;
          String balanceIncreaseSql = String.format("UPDATE Customers SET Balance = %d WHERE CustomerID='%s';", depAmt, userID);
          System.out.println(balanceIncreaseSql); // Print executed SQL update for debugging
          jdbcTemplate.update(balanceIncreaseSql);
          updateAccountInfo(user);

          String overdraftBalanceDecreaseSql = String.format("UPDATE Customers SET OverdraftBalance = %d WHERE CustomerID='%s';", 0, userID);
          System.out.println(overdraftBalanceDecreaseSql); // Print executed SQL update for debugging
          jdbcTemplate.update(overdraftBalanceDecreaseSql);

          updateAccountInfo(user);

          java.util.Date dt = new java.util.Date();
          java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
          String currentTime = sdf.format(dt);

          String addOverdraftLog = String.format("INSERT INTO OverdraftLogs VALUES ('%s', '%s', %d, %d, %d)", userID, currentTime, curOverBal, curOverBal, 0);
          System.out.println(addOverdraftLog); // Print executed SQL update for debugging
          jdbcTemplate.update(addOverdraftLog);

          updateAccountInfo(user);
        } else { // If deposit is less than current overdraft
          int newOverBal = curOverBal - amtDepositAdj;
          String overdraftBalanceDecreaseSql = String.format("UPDATE Customers SET OverdraftBalance = %d WHERE CustomerID='%s';", newOverBal/100, userID);
          System.out.println(overdraftBalanceDecreaseSql); // Print executed SQL update for debugging
          jdbcTemplate.update(overdraftBalanceDecreaseSql);

          updateAccountInfo(user);

          java.util.Date dt = new java.util.Date();
          java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
          String currentTime = sdf.format(dt);

          String addOverdraftLog = String.format("INSERT INTO OverdraftLogs VALUES ('%s', '%s', %d, %d, %d)", userID, currentTime, user.getAmountToDeposit()*100, curOverBal, newOverBal);
          System.out.println(addOverdraftLog); // Print executed SQL update for debugging
          jdbcTemplate.update(addOverdraftLog);

          updateAccountInfo(user);
        }
      } else {
        // Execute SQL Update command that increments user's Balance by given amount from the deposit form.
        String balanceIncreaseSql = String.format("UPDATE Customers SET Balance = Balance + %d WHERE CustomerID='%s';", user.getAmountToDeposit(), userID);
        System.out.println(balanceIncreaseSql); // Print executed SQL update for debugging
        jdbcTemplate.update(balanceIncreaseSql);

        updateAccountInfo(user);
      }

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

    // Get current balance of user
    String getUserBalanceSql = String.format("SELECT Balance FROM customers WHERE CustomerID='%s';", userID);
    int userBal = jdbcTemplate.queryForObject(getUserBalanceSql, Integer.class);
    
    // Get current overdraft balance
    String getOverdraftBalanceSql = String.format("SELECT OverdraftBalance FROM customers WHERE CustomerID='%s';", userID);
    int curOverBal = jdbcTemplate.queryForObject(getOverdraftBalanceSql, Integer.class);

    String getUserPasswordSql = String.format("SELECT Password FROM passwords WHERE CustomerID='%s';", userID);
    String userPassword = jdbcTemplate.queryForObject(getUserPasswordSql, String.class);

    if (userPasswordAttempt.equals(userPassword)) {
      int overBal = user.getAmountToWithdraw() - userBal;
      // Execute SQL Update command that decrements Balance value for
      // user's row in Customers table using user.getAmountToWithdraw()

      if (user.getAmountToWithdraw() <= userBal) { // Normal withdrawal
        String balanceDecreaseSql = String.format("UPDATE Customers SET Balance = Balance - %d WHERE CustomerID='%s';", user.getAmountToWithdraw(), userID);
        System.out.println(balanceDecreaseSql);
        jdbcTemplate.update(balanceDecreaseSql);

        updateAccountInfo(user);
      } else if (overBal <= 1000 && overBal + curOverBal <= 1000) { // Withdrawal within range of overdraft limit

        String balanceDecreaseSql = String.format("UPDATE Customers SET Balance = %d WHERE CustomerID='%s';", 0, userID);
        System.out.println(balanceDecreaseSql);
        jdbcTemplate.update(balanceDecreaseSql);

        updateAccountInfo(user);

        String overdraftBalanceIncreaseSql = String.format("UPDATE Customers SET OverdraftBalance = OverdraftBalance + %d WHERE CustomerID='%s';", overBal, userID);
        System.out.println(overdraftBalanceIncreaseSql);
        jdbcTemplate.update(overdraftBalanceIncreaseSql);

        updateAccountInfo(user);
      } else { // Withdrawal too large
        System.out.println("Withdrawal too large");
        return "welcome"; 
      }

      return "account_info";
    } else {
      System.out.println("Password wrong?");
      return "welcome";
    }
  }
}