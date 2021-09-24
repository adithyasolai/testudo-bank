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
	// random change
  /**
   * A simplified JDBC client that is injected with the login credentials
   * specified in /src/main/resources/application.properties
   */
  @Autowired
  private JdbcTemplate jdbcTemplate;

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
	public String showForm(Model model) {
		User user = new User();
		model.addAttribute("user", user);
		
		return "login_form";
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
	public String submitForm(@ModelAttribute("user") User user) {
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
      String balanceIncreaseSql = String.format("UPDATE Customers SET Balance = Balance + %d WHERE CustomerID='%s';", user.getAmountToDeposit(), userID);
      System.out.println(balanceIncreaseSql); // Print executed SQL update for debugging
      jdbcTemplate.update(balanceIncreaseSql);

      updateAccountInfo(user);

      return "account_info";
    } else {
      return "welcome";
    }
  }

  /**
   * Helper method that queries the MySQL DB for the customer account info (First Name, Last Name, and Balance)
   * and adds these values to the `user` Model Attribute so that they can be displayed in the "account_info" page.
   * 
   * @param user
   */
  private void updateAccountInfo(User user) {
    String getUserNameAndBalanceSql = String.format("SELECT FirstName, LastName, Balance FROM customers WHERE CustomerID='%s';", user.getUsername());
    List<Map<String,Object>> queryResults = jdbcTemplate.queryForList(getUserNameAndBalanceSql);
    Map<String,Object> userData = queryResults.get(0);

    user.setFirstName((String)userData.get("FirstName"));
    user.setLastName((String)userData.get("LastName"));
    user.setBalance((int)userData.get("Balance"));
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
      String balanceIncreaseSql = String.format("UPDATE Customers SET Balance = Balance - %d WHERE CustomerID='%s';", user.getAmountToWithdraw(), userID);
      System.out.println(balanceIncreaseSql);
      jdbcTemplate.update(balanceIncreaseSql);

      // query user's first name, last name, and balance.
      // add values to User's corresponding fields
      // (so that these values are displayed properly in the account_info page)
      String getUserNameAndBalanceSql = String.format("SELECT FirstName, LastName, Balance FROM customers WHERE CustomerID='%s';", userID);

      List<Map<String,Object>> queryResults = jdbcTemplate.queryForList(getUserNameAndBalanceSql);
      Map<String,Object> userData = queryResults.get(0);

      user.setFirstName((String)userData.get("FirstName"));
      user.setLastName((String)userData.get("LastName"));
      user.setBalance((int)userData.get("Balance"));

      return "account_info";
    } else {
      return "welcome";
    }
  }
}