package net.testudobank;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CryptoPriceClient {

    /**
     * Method which is used to return the current value of Ethereum
     * in USD. This method uses JSoup to scrape the website "https://ethereumprice.org"
     * and retrieve the current USD value of 1 ETH.
     * <p>
     * NOTE: If the web scraper fails, a value of -1 is returned
     *
     * @return the current value of 1 ETH in USD
     */
    public double getCurrentEthValue() {
        try {
            // fetch the document over HTTP
            Document doc = Jsoup.connect("https://ethereumprice.org").userAgent("Mozilla").get();

            Element value = doc.getElementById("coin-price");
            String valueStr = value.text();

            // Replacing the '$'' and ',' characters from the string
            valueStr = valueStr.replaceAll("\\$", "").replaceAll("\\,", "");
            double ethValue = Double.parseDouble(valueStr);

            return ethValue;
        } catch (IOException e) {
            // Print stack trace for debugging
            e.printStackTrace();

            // Return -1 if there was an error during web scraping
            return -1;
        }
    }
}
