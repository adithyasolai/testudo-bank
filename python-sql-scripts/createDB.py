import pymysql
import names
import random
import string
# from credentials import mysql_endpoint, username, password, database_name

mysql_endpoint='localhost'
username='root'
passwd="tatakae"
database_name = 'testudo_bank'

connection = pymysql.connect(host=mysql_endpoint, user=username, password = passwd)
cursor = connection.cursor()

create_testudo_bank_db_sql = '''
CREATE DATABASE {};
'''.format(database_name)

cursor.execute(create_testudo_bank_db_sql)

del_testudo_bank_db_sql = '''
DROP DATABASE {};
'''.format(database_name)
# cursor.execute(del_testudo_bank_db_sql)

connection.commit()
cursor.close()




# sudo mysql -u root -p
# Allows me to use it