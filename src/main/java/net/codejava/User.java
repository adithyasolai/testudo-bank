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

  @Setter @Getter @PositiveOrZero
	private double overDraftBalance;

  @Setter @Getter
	private String logs;
  @Setter @Getter
  private String transactionHist;

  @Setter @Getter @Positive
  private double amountToDeposit;

  @Setter @Getter @Positive
  private double amountToWithdraw;

  @Setter @Getter
  private int numTransactionsAgo;

  @Setter @Getter
  private int creditScore;

  @Setter @Getter
  private double networth;

  @Setter @Getter
  private double largeLoanInstallments;

  @Setter @Getter
  private double largeLoanBalance;

  @Setter @Getter
  private double largeLoanInstallmentPayment;

  @Setter @Getter
  private String largeLoanLogs;

	@Override
	public String toString() {
		return "User [username=" + username + ", password=" + password + ", balance=" + balance + "]";
	}

}
