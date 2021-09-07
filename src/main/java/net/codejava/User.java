package net.codejava;

import java.sql.Date;

public class User {
	private String username;
	private String password;
	private int balance;

	public String getUsername() {
		return username;
	}

	public void setUsername(String name) {
		this.username = name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getBalance() {
		return balance;
	}

	public void setBalance(int balance) {
		this.balance = balance;
	}

	public void deposit(int quantity) {
		this.balance += quantity;
	}

	public void withdraw(int quantity) {
		this.balance -= quantity;
	}

	@Override
	public String toString() {
		return "User [username=" + username + ", password=" + password + "]";
	}

}
