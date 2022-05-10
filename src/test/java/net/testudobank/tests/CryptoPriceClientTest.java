package net.testudobank.tests;

import net.testudobank.CryptoPriceClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.annotation.EnableCaching;

import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest
@EnableCaching
public class CryptoPriceClientTest {
    @Autowired
    @SpyBean
    CryptoPriceClient cryptoPriceClient;

    /**
     * Ensure the client is able to get a valid price from the Yahoo Finance API
     */
    @Test
    public void testBasic() {
        assertTrue(cryptoPriceClient.getCacheableCurrentCryptoValue("ETH") > 0);
        assertTrue(cryptoPriceClient.getCacheableCurrentCryptoValue("SOL") > 0);
    }

    /**
     * Test that caching is working
     */
    @Test
    public void testCache() {
        cryptoPriceClient.clearPriceCache();
        assertTrue(cryptoPriceClient.getCacheableCurrentCryptoValue("ETH") > 0);
        assertTrue(cryptoPriceClient.getCacheableCurrentCryptoValue("ETH") > 0);
        assertTrue(cryptoPriceClient.getCacheableCurrentCryptoValue("SOL") > 0);
        assertTrue(cryptoPriceClient.getCacheableCurrentCryptoValue("SOL") > 0);
        Mockito.verify(cryptoPriceClient, Mockito.times(1)).getCurrentEthValue();
        Mockito.verify(cryptoPriceClient, Mockito.times(1)).getCurrentSolValue();
        cryptoPriceClient.clearPriceCache();
        assertTrue(cryptoPriceClient.getCacheableCurrentCryptoValue("ETH") > 0);
        assertTrue(cryptoPriceClient.getCacheableCurrentCryptoValue("SOL") > 0);
        Mockito.verify(cryptoPriceClient, Mockito.times(2)).getCurrentEthValue();
        Mockito.verify(cryptoPriceClient, Mockito.times(2)).getCurrentSolValue();
    }
}
