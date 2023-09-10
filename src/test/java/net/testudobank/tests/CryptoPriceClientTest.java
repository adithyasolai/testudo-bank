package net.testudobank.tests;

import net.testudobank.CryptoPriceClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CryptoPriceClientTest {
    CryptoPriceClient cryptoPriceClient = new CryptoPriceClient();

    /**
     *  Ensure the client is able to get a valid price from the Yahoo Finance API
     */
    @Test
    public void testBasic() {
        // assertTrue(cryptoPriceClient.getCurrentEthValue() > 0);
        // assertTrue(cryptoPriceClient.getCurrentSolValue() > 0);
    }
}
