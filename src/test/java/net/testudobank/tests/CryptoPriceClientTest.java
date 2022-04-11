package net.testudobank.tests;

import net.testudobank.CryptoPriceClient;

import javax.script.ScriptException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;


public class CryptoPriceClientTest extends MvcControllerIntegTest{
    CryptoPriceClient cryptoPriceClient = new CryptoPriceClient();


    /**
     *  Ensure the client is able to get a valid price from the Yahoo Finance API
     */
    @Test
    public void testBasic() {
        assertTrue(cryptoPriceClient.getCurrentEthValue() > 0);
        assertTrue(cryptoPriceClient.getCurrentSolValue() > 0);
    }


    /**
     * Buy ETH Buy Sol sell SOL, ensure user is able to buy different crypto and sell some of the crypto
     * @throws ScriptException
     */

    @Test
    public void testCryptoBuyETHBuySOLSellSOL() throws ScriptException {
        CryptoTransactionTester cryptoTransactionTester = CryptoTransactionTester.builder()
            .initialBalanceInDollars(10000)
            .initialCryptoBalance(Collections.singletonMap("ETH", 0.0))
            .initialCryptoBalance(Collections.singletonMap("SOL", 0.0))
            .build();
        cryptoTransactionTester.initialize();

        double ethValue = cryptoPriceClient.getCurrentEthValue();
        double solValue = cryptoPriceClient.getCurrentSolValue();
        CryptoTransaction cryptoTransactionETH = CryptoTransaction.builder()
            .expectedEndingBalanceInDollars(10000-ethValue)
            .expectedEndingCryptoBalance(1)
            .cryptoPrice(ethValue)
            .cryptoAmountToTransact(1)
            .cryptoName("ETH")
            .cryptoTransactionTestType(CryptoTransactionTestType.BUY)
            .shouldSucceed(true)
            .build();
        cryptoTransactionTester.test(cryptoTransactionETH);
        

        CryptoTransaction cryptoTransactionSOL = CryptoTransaction.builder()
            .expectedEndingBalanceInDollars(10000-ethValue-solValue)
            .expectedEndingCryptoBalance(1)
            .cryptoPrice(solValue)
            .cryptoAmountToTransact(1)
            .cryptoName("SOL")
            .cryptoTransactionTestType(CryptoTransactionTestType.BUY)
            .shouldSucceed(true)
            .build();
        cryptoTransactionTester.test(cryptoTransactionSOL);

        cryptoTransactionSOL = CryptoTransaction.builder()
            .expectedEndingBalanceInDollars(10000-ethValue)
            .expectedEndingCryptoBalance(1)
            .cryptoPrice(solValue)
            .cryptoAmountToTransact(1)
            .cryptoName("SOL")
            .cryptoTransactionTestType(CryptoTransactionTestType.SELL)
            .shouldSucceed(true)
            .build();
        cryptoTransactionTester.test(cryptoTransactionSOL);

    }


    /** BUYING BTC TEST should return welcome page (shouldsucceed = false)*/
    @Test
    public void testCryptoBuyBTC() throws ScriptException {
        CryptoTransactionTester cryptoTransactionTester = CryptoTransactionTester.builder()
                .initialBalanceInDollars(1000)
                .build();

        cryptoTransactionTester.initialize();
        CryptoTransaction cryptoTransaction = CryptoTransaction.builder()
                .expectedEndingBalanceInDollars(1000)
                .expectedEndingCryptoBalance(0)
                .cryptoPrice(1)
                .cryptoAmountToTransact(0.1)
                .cryptoName("BTC")
                .cryptoTransactionTestType(CryptoTransactionTestType.BUY)
                .shouldSucceed(false)
                .build();
        cryptoTransactionTester.test(cryptoTransaction);
    }


    /** SELLING BTC TEST should return welcome page (shouldsucceed = false)*/
    @Test
    public void testCryptoSellBTC() throws ScriptException {
        CryptoTransactionTester cryptoTransactionTester = CryptoTransactionTester.builder()
                .initialBalanceInDollars(1000)
                .build();

        cryptoTransactionTester.initialize();
        CryptoTransaction cryptoTransaction = CryptoTransaction.builder()
                .expectedEndingBalanceInDollars(1000)
                .expectedEndingCryptoBalance(0)
                .cryptoPrice(1)
                .cryptoAmountToTransact(0.1)
                .cryptoName("BTC")
                .cryptoTransactionTestType(CryptoTransactionTestType.BUY)
                .shouldSucceed(false)
                .build();
        cryptoTransactionTester.test(cryptoTransaction);
    }


}




