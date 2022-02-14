package net.testudobank.tests;

import net.testudobank.CryptoPriceClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CryptoPriceClientTest {
    CryptoPriceClient cryptoPriceClient = new CryptoPriceClient();

    /**
     *  Ensure the client is able to get a valid price from the website
     */
    @Test
    public void testBasic() {
        double val = cryptoPriceClient.getCurrentEthValue();
        assertTrue(val > 0);
    }
}
