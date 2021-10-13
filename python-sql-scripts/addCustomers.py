import pymysql
import names
import random
import string
from credentials import mysql_endpoint, username, password, database_name

# SQL Config Values
num_customers_to_add = 100

# Connect to testudo_bank db in local MySQL Server
connection = pymysql.connect(host=mysql_endpoint, user=username, passwd = password, db=database_name)
cursor = connection.cursor()

# Make empty Customers table
create_customer_table_sql = '''
  CREATE TABLE Customers (
    CustomerID varchar(255),
    FirstName varchar(255),
    LastName varchar(255),
    Balance int,
    OverdraftBalance int
  );
  '''
cursor.execute(create_customer_table_sql)

# Make empty Passwords table
create_password_table_sql = '''
CREATE TABLE Passwords (
  CustomerID varchar(255),
  Password varchar(255)
);
'''
cursor.execute(create_password_table_sql)

# Make empty OverdraftLogs table
create_overdraftlogs_table_sql = '''
CREATE TABLE OverdraftLogs (
  CustomerID varchar(255),
  Timestamp DATETIME,
  DepositAmt int,
  OldOverBalance int,
  NewOverBalance int
);
'''
cursor.execute(create_overdraftlogs_table_sql)

# The two sets created below are used to ensure that this
# automated, randomized process does not accidentally 
# generate and use a customer ID that already is in use

# Add all existing customer IDs in the DB to a set
get_all_ids_sql = '''SELECT CustomerID FROM Customers;'''
cursor.execute(get_all_ids_sql)
ids_in_db = set()
for id in cursor.fetchall():
  ids_in_db.add(id[0])

# a set to store all IDs that are added in this Lambda 
# (so that we don't need to run a SELECT SQL query again)
ids_just_added = set()

# add random customers
for i in range(num_customers_to_add):
  # generate random 9-digit customer ID
  customer_id = ''.join(random.choices(string.digits, k = 9))

  # don't add row if someone already has this ID (really unlikely)
  if (customer_id not in ids_in_db and customer_id not in ids_just_added):

    # generate random name, balance, and password
    customer_first_name = names.get_first_name()
    customer_last_name = names.get_last_name()
    customer_balance = random.randint(100, 10000)
    customer_password = ''.join(random.choices(string.ascii_lowercase + string.ascii_uppercase + string.digits, k = 9))
    
    # add random customer ID, name, and balance to Customers table.
    # all customers start with Overdraft balance of 0
    insert_customer_sql = '''
    INSERT INTO Customers
    VALUES  ({0},{1},{2},{3},{4});
    '''.format("'" + customer_id + "'",
                "'" + customer_first_name + "'",
                "'" + customer_last_name + "'",
                customer_balance,
                0)
    cursor.execute(insert_customer_sql)
    
    # add customer ID and password to Passwords table
    insert_password_sql = '''
    INSERT INTO Passwords
    VALUES  ({0},{1});
    '''.format("'" + customer_id + "'",
                "'" + customer_password + "'")
    cursor.execute(insert_password_sql)
    
    # add this customer's randomly-generated ID to the set
    # to ensure this ID is not re-used by accident.
    ids_just_added.add(customer_id)

connection.commit()
cursor.close()