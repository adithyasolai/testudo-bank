package net.codejava;

import org.junit.jupiter.api.Test;
import org.python.core.PyException;
import org.python.util.PythonInterpreter;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class JythonTest {
  
  @Test
  public void testJython() throws PyException {
    PythonInterpreter pythonInterpreter = new PythonInterpreter();

    String connection_config = String.format("import sys\n"+"sys.argv = ['%s', '%s', '%s', '%s']", "A", "B", "C", "D");

    pythonInterpreter.exec(connection_config);

    pythonInterpreter.execfile(".\\src\\test\\resources\\addCustomers.py");

    pythonInterpreter.close();
  }
}
