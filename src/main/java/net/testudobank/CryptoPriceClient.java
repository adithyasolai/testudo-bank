package net.testudobank;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import yahoofinance.YahooFinance;

import java.io.IOException;

@Component
public class CryptoPriceClient {

    /**
     * Method which is used to return the current value of Ethereum
     * in USD. This method uses JSoup to scrape the website "https://ethereumprice.org"
     * and retrieve the current USD value of 1 ETH.
     * <p>
     * To avoid frequent calls to the external service, the value is cached.
     * See {@link #clearEthPriceCache()}
     * <p>
     * NOTE: If the web scraper fails, a value of -1 is returned
     *
     * @return the current value of 1 ETH in USD
     */
    @Cacheable("eth-value")
    public double getCurrentEthValue() {
        try {
            return YahooFinance.get("ETH-USD").getQuote().getPrice().doubleValue();
        } catch (IOException e1) {
            // Print Stack Trace for Debugging
            e1.printStackTrace();
            return -1;
        }
    }

    /**
     * Clear the cached price of ethereum.
     * <p>
     * This method is scheduled to run every 30 seconds.
     */
    @Scheduled(fixedRate = 30000)
    @CacheEvict("eth-value")
    public void clearEthPriceCache() {
    }

}
