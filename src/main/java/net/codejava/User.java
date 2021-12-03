package net.codejava;

import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import lombok.Getter;
import lombok.Setter;

public class User {
  @Setter @Getter
	private String username;

  @Setter @Getter
	private String password;

  @Setter @Getter
  private String firstName;

  @Setter @Getter
  private String lastName;

  @Setter  @Getter @PositiveOrZero
	private double balance;

  @Setter  @Getter @PositiveOrZero
	private double ethbalance;

  @Setter  @Getter @PositiveOrZero
	private double totalCryptoInvestment;

  @Setter @Getter @PositiveOrZero
	private double overDraftBalance;

  @Setter @Getter 
	private double profits;

  @Setter @Getter
	private String logs;
  @Setter @Getter
  private String transactionHist;

  @Setter @Getter @Positive
  private double amountToDeposit;

  @Setter @Getter @Positive
  private double amountToWithdraw;

  @Setter @Getter @Positive
  private double amountToBuyCrypto;

  @Setter @Getter @Positive
  private double amountToSellCrypto;

  @Setter @Getter
  private int numTransactionsAgo;

	@Override
	public String toString() {
		return "User [username=" + username + ", password=" + password + ", balance=" + balance + "]";
	}

}
