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


@SpringBootTest(classes = net.codejava.TestudoBankApplication.class)
public class MvcControllerTest {

	/**
	 * Spring interprets the @Autowired annotation, and the controller is 
	 * injected before the test methods are run.
	 **/
	@Autowired
	private MvcController controller;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Mock
    User mockUser;

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

        when(mockUser.getUsername()).thenReturn("mockUsername");
         
		// assertEquals("login_form", controller.showForm(new User()));
	}

}