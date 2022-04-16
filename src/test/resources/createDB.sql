CREATE TABLE Customers (
  CustomerID varchar(255),
  FirstName varchar(255),
  LastName varchar(255),
  Balance int,
  OverdraftBalance int,
  NumFraudReversals int
);

CREATE TABLE Users (
  CustomerID varchar(255),
  Password varchar(255),
  Enabled bool
);

CREATE TABLE Authorities (
    CustomerID varchar(255),
    Authority  varchar(255),
    FOREIGN KEY (CustomerID) REFERENCES Users (CustomerID)
);

CREATE UNIQUE INDEX ix_auth_username
    on Authorities (CustomerID, Authority);

CREATE TABLE OverdraftLogs (
  CustomerID varchar(255),
  Timestamp DATETIME,
  DepositAmt int,
  OldOverBalance int,
  NewOverBalance int
);

CREATE TABLE TransactionHistory (
  CustomerID varchar(255),
  Timestamp DATETIME,
  Action varchar(255) CHECK (Action IN ('Deposit', 'Withdraw', 'TransferSend', 'TransferRecieve', 'CryptoBuy', 'CryptoSell')),
  Amount int
);

CREATE TABLE TransferHistory (
  TransferFrom varchar(255),
  TransferTo varchar(255),
  Timestamp DATETIME,
  Amount int
);

CREATE TABLE CryptoHoldings (
  CustomerID varchar(255),
  CryptoName varchar(255),
  CryptoAmount decimal(30,18)
);

CREATE TABLE CryptoHistory (
  CustomerID varchar(255),
  Timestamp DATETIME,
  Action varchar(255) CHECK (Action IN ('Buy', 'Sell')),
  CryptoName varchar(255),
  CryptoAmount decimal(30,18)
);