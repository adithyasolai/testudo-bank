package net.codejava;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.mysql.cj.jdbc.MysqlDataSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.python.util.PythonInterpreter;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
public class MvcControllerIntegTest {

  @Container
  public static MySQLContainer db = new MySQLContainer("mysql:5.5")
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

  @BeforeAll
  public static void init() {
    ScriptUtils.runInitScript(new JdbcDatabaseDelegate(db, ""), "createDB.sql");

    //ScriptUtils.executeDatabaseScript(databaseDelegate, scriptPath, script);
    //ScriptUtils.runInitScript(new JdbcDatabaseDelegate(db, ""), "");
  }

  @Test
  public void testSimpleDeposit() throws SQLException {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource());
    jdbcTemplate.getDataSource().getConnection().setCatalog(db.getDatabaseName());

    List<Map<String,Object>> queryResults = jdbcTemplate.queryForList("SELECT * FROM Customers;");
    Map<String,Object> userData = queryResults.get(0);
    System.out.println(Arrays.toString(userData.entrySet().toArray()));

    System.out.println("Context Loads!");
  }

}
