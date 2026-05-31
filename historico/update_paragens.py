import mysql.connector

db_config = {
    'host': 'pgu-tub-db-pl.mysql.database.azure.com',
    'user': 'tubadmin',
    'password': 'Cunha@2006',
    'database': 'pgu_tub'
}

paragens = [
  { "nome": "S. Mamede d' Este", "lat": 41.5680, "lng": -8.3920 },
  { "nome": 'Avenida da Liberdade', "lat": 41.5510, "lng": -8.4210 },
  { "nome": 'Celeirós', "lat": 41.5720, "lng": -8.4050 },
  { "nome": 'São Vítor', "lat": 41.5437, "lng": -8.4148 },
  { "nome": 'Hospital de Braga', "lat": 41.5578, "lng": -8.3843 },
  { "nome": 'Rua Egídio Guimarães', "lat": 41.5505, "lng": -8.4180 },
  { "nome": 'Avenida Central', "lat": 41.5492, "lng": -8.4260 },
  { "nome": 'Rua Mário de Almeida', "lat": 41.5470, "lng": -8.4300 },
  { "nome": 'Estação C.P.', "lat": 41.5489, "lng": -8.4341 },
  { "nome": 'Universidade do Minho', "lat": 41.5614, "lng": -8.3966 },
  { "nome": 'Ponte de Prado', "lat": 41.5612, "lng": -8.4315 },
  { "nome": 'Terminal Intermodal', "lat": 41.5503, "lng": -8.4227 },
  { "nome": 'Santuário do Bom Jesus', "lat": 41.5621, "lng": -8.4147 },
  { "nome": 'Avenida Central / Hospital', "lat": 41.5578, "lng": -8.3843 },
  { "nome": 'Praça Conde de Agrolongo', "lat": 41.5542, "lng": -8.4335 },
  { "nome": 'Ruães', "lat": 41.5476, "lng": -8.4395 },
  { "nome": 'Gualtar / Universidade', "lat": 41.5590, "lng": -8.3980 },
  { "nome": 'Lageosa', "lat": 41.5813, "lng": -8.4047 },
  { "nome": 'Pedralva', "lat": 41.5942, "lng": -8.3961 },
  { "nome": 'Areal', "lat": 41.5514, "lng": -8.4094 },
  { "nome": 'Jardim da Ponte', "lat": 41.5528, "lng": -8.4065 },
  { "nome": 'Boavista', "lat": 41.5607, "lng": -8.4160 },
  { "nome": 'Igreja de Dume', "lat": 41.5569, "lng": -8.4270 },
  { "nome": 'Dume / Ruas de Dume', "lat": 41.5580, "lng": -8.4325 },
  { "nome": 'Quinta da Capela', "lat": 41.5785, "lng": -8.4182 },
  { "nome": 'Av. Gen. Norton de Matos', "lat": 41.5600, "lng": -8.4200 },
  { "nome": 'Semelhe', "lat": 41.5828, "lng": -8.4182 },
  { "nome": 'Gondizalves', "lat": 41.5770, "lng": -8.3974 },
  { "nome": 'Rua 25 de Abril', "lat": 41.5466, "lng": -8.4312 },
  { "nome": 'Sete Fontes', "lat": 41.5397, "lng": -8.4471 },
  { "nome": 'Nogueira (Barral)', "lat": 41.5620, "lng": -8.4480 },
  { "nome": 'Priscos', "lat": 41.5655, "lng": -8.4728 },
  { "nome": 'Rua do Raio', "lat": 41.5837, "lng": -8.3962 },
  { "nome": 'Esporões', "lat": 41.5766, "lng": -8.4816 },
  { "nome": 'Pinheiro do Bicho', "lat": 41.5772, "lng": -8.4954 },
  { "nome": 'Ponte Nova', "lat": 41.5598, "lng": -8.4022 },
  { "nome": 'Escudeiros', "lat": 41.5735, "lng": -8.4052 },
  { "nome": 'Maximinos', "lat": 41.5461, "lng": -8.4378 },
  { "nome": 'Bom Jesus', "lat": 41.5547, "lng": -8.3787 },
  { "nome": 'Nogueiró', "lat": 41.5519, "lng": -8.4486 },
  { "nome": 'Gualtar', "lat": 41.5590, "lng": -8.3980 }
]

def update():
    print("A conectar à base de dados no Azure...")
    conn = mysql.connector.connect(**db_config)
    cursor = conn.cursor()
    
    # Update known stops directly by exact match
    for p in paragens:
        cursor.execute("UPDATE paragens SET latitude = %s, longitude = %s WHERE nome = %s", (p["lat"], p["lng"], p["nome"]))
    
    # Also update any stops containing the string to catch brackets like "Celeirós (Pte Covedelo)"
    for p in paragens:
        cursor.execute("UPDATE paragens SET latitude = %s, longitude = %s WHERE nome LIKE %s AND latitude IS NULL", (p["lat"], p["lng"], f"%{p['nome']}%"))
        
    conn.commit()
    cursor.close()
    conn.close()
    print("Coordenadas atualizadas com sucesso!")

if __name__ == '__main__':
    update()
