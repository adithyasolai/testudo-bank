package net.codejava;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.SQLException;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.delegate.DatabaseDelegate;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
public class MvcControllerIntegTest {

  @Container
  public static MySQLContainer db = new MySQLContainer<>("mysql:5.5")
    .withUsername("root")
    .withPassword("Prathu123$")
    .withDatabaseName("testudo_bank");


  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", db::getJdbcUrl);
    registry.add("spring.datasource.username", db::getUsername);
    registry.add("spring.datasource.password", db::getPassword);
  }


  private static DataSource dataSource() {
    MysqlDataSource dataSource = new MysqlDataSource();
    dataSource.setUrl(db.getJdbcUrl());
    dataSource.setUser(db.getUsername());
    dataSource.setPassword(db.getPassword());
    return dataSource;
  }

  private static MvcController controller;
  private static JdbcTemplate jdbcTemplate;
  private static DatabaseDelegate dbDelegate = new JdbcDatabaseDelegate(db, "");

  private static String CUSTOMER1_ID = "123456789";
  private static String CUSTOMER1_PASSWORD = "password";
  private static String CUSTOMER1_FIRST_NAME = "Foo";
  private static String CUSTOMER1_LAST_NAME = "Bar";

  @BeforeAll
  public static void init() throws SQLException {
    ScriptUtils.runInitScript(dbDelegate, "createDB.sql");
    jdbcTemplate = new JdbcTemplate(dataSource());
    jdbcTemplate.getDataSource().getConnection().setCatalog(db.getDatabaseName());
    controller = new MvcController(jdbcTemplate);
  }

  @AfterEach
  public void clearDB() throws ScriptException {
    // runInitScript() pulls all the String text from the SQL file and just calls executeDatabaseScript(),
    // so it is OK to use runInitScript() again even though we aren't initializing the DB for the first time here.
    // runInitScript() is a poorly-named function.
    ScriptUtils.runInitScript(dbDelegate, "clearDB.sql");
  }

  private void addCustomerToDB(String ID, String password, String firstName, String lastName, int balance) throws ScriptException {
    String insertCustomerSql = String.format("INSERT INTO Customers VALUES ('%s', '%s', '%s', %d, 0, 0)", ID, firstName, lastName, balance);
    ScriptUtils.executeDatabaseScript(dbDelegate, null, insertCustomerSql);

    String insertCustomerPasswordSql = String.format("INSERT INTO Passwords VALUES ('%s', '%s')", ID, password);
    ScriptUtils.executeDatabaseScript(dbDelegate, null, insertCustomerPasswordSql);
  }

  private int convertDollarsToPennies(double dollarAmount) {
    return (int) dollarAmount * 100;
  }

  @Test
  public void testSimpleDeposit() throws SQLException, ScriptException {
    // initialize customer 1 with a balance of $100. represented as pennies in the DB.
    double CUSTOMER1_BALANCE = 100;
    int CUSTOMER1_BALANCE_IN_PENNIES = convertDollarsToPennies(CUSTOMER1_BALANCE);
    addCustomerToDB(CUSTOMER1_ID, CUSTOMER1_PASSWORD, CUSTOMER1_FIRST_NAME, CUSTOMER1_LAST_NAME, CUSTOMER1_BALANCE_IN_PENNIES);

    // Prepare Deposit Form to Deposit $50 to customer 1's account.
    User customer1DepositFormInputs = new User();
    customer1DepositFormInputs.setUsername(CUSTOMER1_ID);
    customer1DepositFormInputs.setPassword(CUSTOMER1_PASSWORD);
    customer1DepositFormInputs.setAmountToDeposit(50); // user input is in dollar amount, not pennies.

    // send request to POST handler for Deposit Form in MvcController
    controller.submitDeposit(customer1DepositFormInputs);

    // fetch customer1's updated data from the DB
    List<Map<String,Object>> queryResults = jdbcTemplate.queryForList(String.format("SELECT * FROM Customers WHERE CustomerID='%s';", CUSTOMER1_ID));
  
    // verify that customer1's data still exists in Customers table
    assertEquals(1, queryResults.size());

    // verify customer balance was increased by $50
    double CUSTOMER1_EXPECTED_FINAL_BALANCE = 150;
    double CUSTOMER1_EXPECTED_FINAL_BALANCE_IN_PENNIES = convertDollarsToPennies(CUSTOMER1_EXPECTED_FINAL_BALANCE);
    Map<String,Object> customer1Data = queryResults.get(0);
    assertEquals(CUSTOMER1_EXPECTED_FINAL_BALANCE_IN_PENNIES, (int)customer1Data.get("Balance"));
  }


}
