CREATE TABLE Customers (
  CustomerID varchar(255),
  FirstName varchar(255),
  LastName varchar(255),
  Balance int,
  OverdraftBalance int,
  NumFraudReversals int
);

CREATE TABLE Passwords (
  CustomerID varchar(255),
  Password varchar(255)
);

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
  Action varchar(255) CHECK (Action IN ('Deposit', 'Withdraw', 'TransferSend', 'TransferRecieve')),
  Amount int
);

CREATE TABLE TransferHistory (
  TransferFrom varchar(255),
  TransferTo varchar(255),
  Timestamp DATETIME,
  Amount int
);
