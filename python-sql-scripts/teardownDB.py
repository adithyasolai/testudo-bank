import pymysql
import names
import random
import string
from credentials import mysql_endpoint, username, password, database_name

# Connect to local MySQL Server and delete a DB called 'testudo_bank'
connection = pymysql.connect(host=mysql_endpoint, user=username, passwd = password)
cursor = connection.cursor()

delete_testudo_bank_db_sql = '''
DROP DATABASE {};
'''.format(database_name)

cursor.execute(delete_testudo_bank_db_sql)

connection.commit()
cursor.close()