import mysql.connector

db_config = {
    'host': 'pgu-tub-db-pl.mysql.database.azure.com',
    'user': 'tubadmin',
    'password': 'Cunha@2006',
    'database': 'pgu_tub'
}

def check():
    conn = mysql.connector.connect(**db_config)
    cursor = conn.cursor()
    cursor.execute("SELECT id, nome FROM paragens")
    rows = cursor.fetchall()
    print("Todas as paragens na base de dados:")
    for r in rows:
        print(f"- {r[1]}")
    cursor.close()
    conn.close()

if __name__ == '__main__':
    check()
