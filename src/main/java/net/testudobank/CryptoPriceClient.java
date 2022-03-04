package net.testudobank;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
            // fetch the document over HTTP
            // TODO: this should probably be adapted to a Spring way by using a WebClient
            Document doc = Jsoup.connect("https://ethereumprice.org").userAgent("Mozilla").get();

            Element value = doc.getElementById("coin-price");
            if (value == null) {
                return -1;
            }
            String valueStr = value.text();

            // Replacing the '$'' and ',' characters from the string
            valueStr = valueStr.replaceAll("\\$", "").replaceAll("\\,", "");

            return Double.parseDouble(valueStr);
        } catch (IOException e) {
            // Print stack trace for debugging
            e.printStackTrace();

            // Return -1 if there was an error during web scraping
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
