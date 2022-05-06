package net.testudobank;

import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString(onlyExplicitlyIncluded = true)
public class User {
  //// General Customer Fields ////

  @Setter @Getter @ToString.Include
	private String username;

  @Setter @Getter @ToString.Include
	private String password;

  @Setter @Getter
  private String firstName;

  @Setter @Getter
  private String lastName;

  @Setter  @Getter @PositiveOrZero @ToString.Include
	private double balance;

  @Setter @Getter @PositiveOrZero
	private double overDraftBalance;

  @Setter @Getter
	private String logs;

  @Setter @Getter
  private String transactionHist;

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
  private boolean isTransfer;

  @Setter @Getter
  private String transferHist;

  //// Crypto Fields ////

  @Setter @Getter
  private boolean isCryptoTransaction;

  @Setter @Getter
  private String cryptoHist;

  @Setter @Getter @Positive
  private double amountToBuyCrypto;

  @Setter @Getter @Positive
  private double amountToSellCrypto;

  @Setter @Getter
  private String whichCryptoToBuy;

  @Setter @Getter
  private double ethPrice;

  @Setter @Getter
  private double solPrice;

  @Setter  @Getter @PositiveOrZero @ToString.Include
  private double ethBalance;

  @Setter  @Getter @PositiveOrZero @ToString.Include
  private double solBalance;

  @Setter  @Getter @PositiveOrZero @ToString.Include
  private double cryptoBalanceUSD;

  //// Index Fund Fields ////

  @Setter @Getter
  private boolean isIndexTransaction;

  @Setter @Getter
  private String indexHist;

  @Setter @Getter @Positive
  private double amountToBuyIndex;

  @Setter @Getter @Positive
  private double amountToSellIndex;

  @Setter @Getter
  private String whichIndexToBuy;

  @Setter @Getter
  private double vooPrice;

  @Setter @Getter
  private double vtvPrice;

  @Setter  @Getter @PositiveOrZero @ToString.Include
  private double vooBalance;

  @Setter  @Getter @PositiveOrZero @ToString.Include
  private double vtvBalance;

  @Setter  @Getter @PositiveOrZero @ToString.Include
  private double indexBalanceUSD;

}
