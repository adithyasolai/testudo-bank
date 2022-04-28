package net.testudobank;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.util.List;

@ToString(onlyExplicitlyIncluded = true)
public class User {
  //// General Customer Fields ////

  @Setter @Getter @ToString.Include
  private String username;

  @Setter @Getter
  private String firstName;

  @Setter @Getter
  private String lastName;

  @Setter  @Getter @ToString.Include
  private String balance;

  @Setter @Getter @PositiveOrZero
  private String overDraftBalance;

  @Setter @Getter
  private List<OverdraftHistoryEntry> overdraftHist;

  @Setter @Getter
  private List<TransactionHistoryEntry> transactionHist;

  //// Dispute Fields ////

  @Setter @Getter
  private int numTransactionsAgo;

  //// Deposit Fields ////

  @Setter @Getter @Positive
  private double amountToDeposit;

  //// Withdraw Fields ////

  @Setter @Getter @Positive
  private double amountToWithdraw;

  //// Transfer Fields ////

  @Setter @Getter
  private double amountToTransfer;

  @Setter @Getter
  private String transferRecipientID;

  @Setter @Getter
  private List<TransferHistoryEntry> transferHist;
  //// Crypto Fields ////

  @Setter @Getter
  private List<CryptoTransactionHistoryEntry> cryptoHist;

  @Setter @Getter @Positive
  private double amountToBuyCrypto;

  @Setter @Getter @Positive
  private double amountToSellCrypto;

  @Setter @Getter
  private String whichCryptoToBuy;

  @Setter @Getter
  private String ethPrice;

  @Setter @Getter
  private String solPrice;

  @Setter  @Getter @PositiveOrZero @ToString.Include
  private double ethBalance;

  @Setter  @Getter @PositiveOrZero @ToString.Include
  private double solBalance;

  @Setter  @Getter @PositiveOrZero @ToString.Include
  private String cryptoBalanceUSD;

}
