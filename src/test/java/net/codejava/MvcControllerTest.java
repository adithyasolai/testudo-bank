package net.codejava;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.ui.Model;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class MvcControllerTest {
	@Mock
	private static JdbcTemplate jdbcTemplate;

  @Mock
  Model mockModel;

	private MvcController controller;

  private static String CUSTOMER1_USERNAME;
  private static List<Map<String, Object>> CUSTOMER1_DATA;

  @BeforeAll
  public static void init() {
    CUSTOMER1_USERNAME = "123456789";

    CUSTOMER1_DATA = new ArrayList<>();
    CUSTOMER1_DATA.add(new HashMap<>());
    CUSTOMER1_DATA.get(0).put("FirstName", "John");
    CUSTOMER1_DATA.get(0).put("LastName", "Doe");
    CUSTOMER1_DATA.get(0).put("Balance", 100);
  }

  @BeforeEach
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(jdbcTemplate.queryForList(anyString())).thenReturn(CUSTOMER1_DATA); // handles updateAccountInfo() helper method
    controller = new MvcController(jdbcTemplate);
  }

	@Test
	public void testControllerIsCreated() {
		assertThat(controller, is(notNullValue()));
	}

	@Test
	public void testShowWelcomeSuccess() {
		assertEquals("welcome", controller.showWelcome(null));
	}

	@Test
	public void testShowForm() {
		assertEquals("login_form", controller.showForm(mockModel));
	}

	@Test
	public void testSubmitFormSuccessWithCorrectPassword() {
		User customer1 = new User();
		customer1.setUsername(CUSTOMER1_USERNAME);
		customer1.setPassword("password");

		when(jdbcTemplate.queryForObject(anyString(), eq(String.class))).thenReturn("password");
		
		assertEquals("account_info", controller.submitForm(customer1));
	}

	@Test
	public void testSubmitFormFailureWithIncorrectPassword() {
		User customer1 = new User();
		customer1.setUsername(CUSTOMER1_USERNAME);
		customer1.setPassword("not password");

		when(jdbcTemplate.queryForObject(anyString(), eq(String.class))).thenReturn("password");
		
		assertEquals("welcome", controller.submitForm(customer1));
	}

	@Test
	public void testShowDepositFormSuccess() {
		assertEquals("login_form", controller.showForm(mockModel));
	}

	@Test
	public void testDepositSuccesswithCorrectPassword() {
		User customer1 = new User();
		customer1.setUsername(CUSTOMER1_USERNAME);
		customer1.setPassword("password");
		customer1.setAmountToDeposit(100);

		when(jdbcTemplate.queryForObject(anyString(), eq(String.class))).thenReturn("password");
		when(jdbcTemplate.update(anyString())).thenReturn(1);
		assertEquals("account_info", controller.submitDeposit(customer1));
		assertEquals(100, customer1.getAmountToDeposit());
	}

	@Test
	public void testDepositSuccesswithIncorrectPassword() {
		User customer1 = new User();
		customer1.setUsername(CUSTOMER1_USERNAME);
		customer1.setPassword("not password");
		customer1.setAmountToDeposit(100);

		when(jdbcTemplate.queryForObject(anyString(), eq(String.class))).thenReturn("password");
		when(jdbcTemplate.update(anyString())).thenReturn(1);
		assertEquals("welcome", controller.submitDeposit(customer1));
		assertEquals(100, customer1.getAmountToDeposit());
	}

	@Test
	public void testShowWithdrawFormSuccess() {
		assertEquals("withdraw_form", controller.showWithdrawForm(mockModel));
	}

}
