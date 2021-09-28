import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import net.codejava.MvcController;
import net.codejava.User;
import org.springframework.ui.Model;
import org.mockito.Mock;
import static org.mockito.Mockito.*;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.util.ArrayList;

@SpringBootTest(classes = net.codejava.TestudoBankApplication.class)
public class MvcControllerTest {

	/**
	 * Spring interprets the @Autowired annotation, and the controller is 
	 * injected before the test methods are run.
	 **/
	@Autowired
	private MvcController controller;

	@Mock
	private JdbcTemplate jdbcTemplate;

	@Mock
    Model mockModel;

	/**
	 * The @SpringBootTest annotation tells Spring Boot to look for a main 
	 * configuration class (one with @SpringBootApplication, for instance) 
	 * and use that to start a Spring application context. This test ensures
	 * that the TestudoBankApplication context is creating our controller
	 */
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
		User user = new User();
		user.setUsername("643613220");
		user.setPassword("5D3AVwLJB");

		when(jdbcTemplate.queryForObject(anyString(), eq(String.class), anyObject())).thenReturn("5D3AVwLJB");
		
		assertEquals("account_info", controller.submitForm(user));
	}

	@Test
	public void testSubmitFormFailureWithIncorrectPassword() {
		User user = new User();
		user.setUsername("643613220");
		user.setPassword("password");

		when(jdbcTemplate.queryForObject(anyString(), eq(String.class),anyObject())).thenReturn("5D3AVwLJB");
		
		assertEquals("welcome", controller.submitForm(user));
	}

	@Test
	public void testShowDepositFormSuccess() {
		assertEquals("login_form", controller.showForm(mockModel));
	}

	@Test
	public void testDepositSuccesswithCorrectPassword() {
		User user = new User();
		user.setUsername("643613220");
		user.setPassword("5D3AVwLJB");
		user.setAmountToDeposit(100);

		when(jdbcTemplate.queryForObject(anyString(), eq(String.class), anyObject())).thenReturn("5D3AVwLJB");
		when(jdbcTemplate.update(anyString())).thenReturn(1);
		assertEquals("account_info", controller.submitDeposit(user));
		assertEquals(100, user.getAmountToDeposit());
	}

	@Test
	public void testDepositSuccesswithIncorrectPassword() {
		User user = new User();
		user.setUsername("643613220");
		user.setPassword("password");
		user.setAmountToDeposit(100);

		when(jdbcTemplate.queryForObject(anyString(), eq(String.class), anyObject())).thenReturn("5D3AVwLJB");
		when(jdbcTemplate.update(anyString())).thenReturn(1);
		assertEquals("welcome", controller.submitDeposit(user));
		assertEquals(100, user.getAmountToDeposit());
	}

	@Test
	public void testShowWithdrawFormSuccess() {
		assertEquals("withdraw_form", controller.showWithdrawForm(mockModel));
	}

}
