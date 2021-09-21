import pymysql
import names
import random
import string
from credentials import mysql_endpoint, username, password, database_name

# Connect to local MySQL Server and create a DB called 'testudo_bank'
connection = pymysql.connect(host=mysql_endpoint, user=username, passwd = password)
cursor = connection.cursor()

create_testudo_bank_db_sql = '''
CREATE DATABASE {};
'''.format(database_name)

cursor.execute(create_testudo_bank_db_sql)

connection.commit()
cursor.close()