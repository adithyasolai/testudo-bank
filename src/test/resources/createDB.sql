CREATE TABLE Customers (
  CustomerID varchar(255),
  FirstName varchar(255),
  LastName varchar(255),
  Balance int,
  OverdraftBalance int,
  NumFraudReversals int
);

INSERT INTO Customers
VALUES ('123456789', 'John', 'Adams', 100, 0, 0);
