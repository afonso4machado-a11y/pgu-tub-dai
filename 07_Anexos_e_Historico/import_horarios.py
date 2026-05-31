import mysql.connector
import os
import sys

# Forcar output UTF-8 no terminal Windows
sys.stdout.reconfigure(encoding='utf-8', errors='replace')

# ────────────────────────────────────────────────
# Configurações da Base de Dados
# ────────────────────────────────────────────────
db_config = {
    'host': 'localhost',
    'user': 'tub_user',
    'password': 'tub_pass',
    'database': 'tub'
}

def get_connection():
    return mysql.connector.connect(**db_config)


def limpar_tabelas(conn):
    """Remove dados anteriores das tabelas de horários para re-importação limpa."""
    cursor = conn.cursor(buffered=True)
    print("A limpar tabelas de horários antigas...")
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    cursor.execute("TRUNCATE TABLE horarios")
    cursor.execute("TRUNCATE TABLE viagens")
    cursor.execute("TRUNCATE TABLE linha_paragens")
    cursor.execute("TRUNCATE TABLE paragens")
    cursor.execute("TRUNCATE TABLE linhas")
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    conn.commit()
    cursor.close()


def get_or_create_paragem(cursor, nome):
    """Obtém o ID de uma paragem existente ou cria uma nova."""
    nome = nome.strip()
    cursor.execute("SELECT id FROM paragens WHERE nome = %s", (nome,))
    res = cursor.fetchone()
    if res:
        return res[0]
    cursor.execute("INSERT INTO paragens (nome) VALUES (%s)", (nome,))
    return cursor.lastrowid


def criar_linha_e_paragens(cursor, linha_id, nomes_paragens):
    """Cria a linha e associa as paragens pela ordem correcta."""
    cursor.execute("INSERT IGNORE INTO linhas (id, nome) VALUES (%s, %s)",
                   (linha_id, f"Linha {linha_id}"))
    ids = []
    for i, nome in enumerate(nomes_paragens):
        nome = nome.strip()
        if nome:
            p_id = get_or_create_paragem(cursor, nome)
            cursor.execute(
                "INSERT IGNORE INTO linha_paragens (linha_id, paragem_id, ordem) VALUES (%s, %s, %s)",
                (linha_id, p_id, i + 1)
            )
            ids.append(p_id)
        else:
            ids.append(None)
    return ids


def inserir_viagem_e_horarios(cursor, linha_id, tipo_dia, periodo_escolar, paragens_ids, horas):
    """Cria uma viagem e insere os horários para as paragens fornecidas."""
    # Só cria viagem se houver pelo menos uma hora válida
    if not any(":" in h for h in horas if h):
        return

    cursor.execute(
        "INSERT INTO viagens (linha_id, tipo_dia, periodo_escolar) VALUES (%s, %s, %s)",
        (linha_id, tipo_dia, periodo_escolar)
    )
    v_id = cursor.lastrowid

    for i, hora in enumerate(horas):
        if i >= len(paragens_ids):
            break
        p_id = paragens_ids[i]
        if p_id and hora and ":" in hora:
            h_limpa = hora.strip().split(' ')[0]  # Remove notas como "a)", "b)"
            if len(h_limpa.split(':')) == 2:
                cursor.execute(
                    "INSERT INTO horarios (viagem_id, paragem_id, hora) VALUES (%s, %s, %s)",
                    (v_id, p_id, f"{h_limpa}:00")
                )


def is_header_line(line):
    """Verifica se uma linha é um cabeçalho de paragens (não contém horas)."""
    # Linha de cabeçalho se tem ; OU se não tem nenhum ":" de hora
    if ";" in line:
        return True
    parts = [p.strip() for p in line.split(',')]
    has_hour = any(
        len(p.split(':')) == 2 and p.split(':')[0].strip().isdigit()
        for p in parts if p
    )
    return not has_hour


# ──────────────────────────────────────────────────────────────
# IMPORTAR LINHAS CIRCULARES (07H e 40H)
# Formato:
#   Linha 1: Paragens separadas por ";"
#   Depois secções "Dias úteis" e "Sábados Domingos e Feriados"
#   Cada linha de dados = 1 viagem
# ──────────────────────────────────────────────────────────────
def importar_circular(conn, filename, linha_id):
    print(f"\n{'='*50}")
    print(f"  Processando {filename} (Linha {linha_id} - Circular)")
    print(f"{'='*50}")

    cursor = conn.cursor(buffered=True)

    with open(filename, 'r', encoding='utf-8') as f:
        linhas = f.readlines()

    # -- Ler cabecalho de paragens (pode ocupar 1 ou 2 linhas com ";")
    header_raw = ""
    data_start = 0
    for i, linha in enumerate(linhas):
        stripped = linha.strip()
        if ";" in stripped:
            # Concatena sem espacos duplicados
            header_raw = (header_raw + " " + stripped).strip()
            data_start = i + 1
        elif header_raw:
            # Primeira linha sem ";" depois do cabecalho: comeca os dados
            data_start = i
            break

    nomes_paragens = [p.strip() for p in header_raw.split(';') if p.strip()]
    paragens_ids = criar_linha_e_paragens(cursor, linha_id, nomes_paragens)
    print(f"  Paragens ({len(nomes_paragens)}): {nomes_paragens}")

    # ── Processar secções de dias
    tipo_dia = 'UTIL'
    periodo_escolar = True
    num_viagens = 0

    for linha in linhas[data_start:]:
        content = linha.strip()
        if not content:
            continue

        content_lower = content.lower()

        # Detectar mudança de secção
        if any(k in content_lower for k in ['úteis', 'uteis']):
            tipo_dia = 'UTIL'
            print("  -> Seccao: Dias Uteis")
            continue
        if any(k in content_lower for k in ['sábado', 'sabado', 'domingo', 'feriado']):
            tipo_dia = 'SABADO'
            print("  -> Seccao: Fim de Semana / Feriados")
            continue

        # Ignorar linhas de cabeçalho dentro das secções
        if is_header_line(content):
            continue

        # Linha de dados: cada campo separado por vírgula
        partes = [p.strip() for p in content.split(',')]
        inserir_viagem_e_horarios(cursor, linha_id, tipo_dia, periodo_escolar, paragens_ids, partes)
        num_viagens += 1

    conn.commit()
    cursor.close()
    print(f"  OK {num_viagens} viagens importadas.")


# ──────────────────────────────────────────────────────────────
# IMPORTAR LINHA 43H (Bidirecional com Períodos Escolares)
# Formato:
#   Linha 1: "Estação C.P.; U.Minho; U.Minho; Estação C.P."
#   Linha 2: "Partida; Chegada; Partida; Chegada"
#   Secção "Dias Úteis (Período Escolar)"
#   Secção "Dias Úteis (Período Não Escolar)" + nova linha de cabeçalho a ignorar
#   Cada linha de dados cria 2 viagens:
#     Viagem A: colunas 0 e 1  (Estação → U.Minho)
#     Viagem B: colunas 2 e 3  (U.Minho → Estação)
# ──────────────────────────────────────────────────────────────
def importar_43h(conn, filename, linha_id):
    print(f"\n{'='*50}")
    print(f"  Processando {filename} (Linha {linha_id} - Bidirecional)")
    print(f"{'='*50}")

    cursor = conn.cursor(buffered=True)

    with open(filename, 'r', encoding='utf-8') as f:
        linhas = f.readlines()

    # Linha 1 tem as paragens separadas por ";"
    nomes_paragens = [p.strip() for p in linhas[0].split(';') if p.strip()]
    paragens_ids = criar_linha_e_paragens(cursor, linha_id, nomes_paragens)
    print(f"  Paragens: {nomes_paragens}")
    # nomes_paragens[0] = "Estação C.P."  → paragem de partida Viagem A
    # nomes_paragens[1] = "U.Minho"       → paragem de chegada  Viagem A
    # nomes_paragens[2] = "U.Minho"       → paragem de partida Viagem B
    # nomes_paragens[3] = "Estação C.P."  → paragem de chegada  Viagem B

    p_ids_a = [paragens_ids[0], paragens_ids[1]]  # Estação→UMinho
    p_ids_b = [paragens_ids[2], paragens_ids[3]]  # UMinho→Estação

    tipo_dia = 'UTIL'
    periodo_escolar = True
    num_viagens = 0

    for linha in linhas[2:]:  # Começa na linha 3 (ignora paragens e "Partida;Chegada")
        content = linha.strip()
        if not content:
            continue

        content_lower = content.lower()

        # Detectar secção
        if 'escolar' in content_lower and 'não' not in content_lower and 'nao' not in content_lower:
            periodo_escolar = True
            tipo_dia = 'UTIL'
            print("  -> Seccao: Dias Uteis - Periodo Escolar")
            continue
        if 'não escolar' in content_lower or 'nao escolar' in content_lower:
            periodo_escolar = False
            tipo_dia = 'UTIL'
            print("  -> Seccao: Dias Uteis - Periodo Nao Escolar")
            continue
        if any(k in content_lower for k in ['sábado', 'sabado', 'domingo', 'feriado']):
            tipo_dia = 'SABADO'
            periodo_escolar = True
            print("  -> Seccao: Fim de Semana / Feriados")
            continue

        # Ignorar linhas de cabeçalho (ex: "P. ESTAÇÃO,C. U.MINHO,...")
        if is_header_line(content):
            continue

        partes = [p.strip() for p in content.split(',')]
        if len(partes) < 2:
            continue

        # Viagem A: Estação → U.Minho (cols 0 e 1)
        horas_a = [partes[0] if len(partes) > 0 else '', partes[1] if len(partes) > 1 else '']
        inserir_viagem_e_horarios(cursor, linha_id, tipo_dia, periodo_escolar, p_ids_a, horas_a)
        if any(":" in h for h in horas_a if h):
            num_viagens += 1

        # Viagem B: U.Minho → Estação (cols 2 e 3)
        horas_b = [partes[2] if len(partes) > 2 else '', partes[3] if len(partes) > 3 else '']
        inserir_viagem_e_horarios(cursor, linha_id, tipo_dia, periodo_escolar, p_ids_b, horas_b)
        if any(":" in h for h in horas_b if h):
            num_viagens += 1

    conn.commit()
    cursor.close()
    print(f"  OK {num_viagens} viagens importadas.")


# ──────────────────────────────────────────────────────────────
# MAIN
# ──────────────────────────────────────────────────────────────
def main():
    print("=" * 50)
    print("  Importação de Horários TUB")
    print("=" * 50)

    try:
        conn = get_connection()
        limpar_tabelas(conn)

        importar_circular(conn, '07h.csv', '07H')
        importar_circular(conn, '40h.csv', '40H')
        importar_43h(conn,      '43h.csv', '43H')

        # Verificação final
        cursor = conn.cursor(buffered=True)
        cursor.execute("SELECT linha_id, COUNT(*) as total FROM viagens GROUP BY linha_id")
        resultados = cursor.fetchall()
        cursor.execute("SELECT COUNT(*) FROM horarios")
        total_horarios = cursor.fetchone()[0]
        cursor.close()
        conn.close()

        print(f"\n{'='*50}")
        print("  Resumo Final")
        print(f"{'='*50}")
        for linha_id, total in resultados:
            print(f"  Linha {linha_id}: {total} viagens")
        print(f"  Total de horários inseridos: {total_horarios}")
        print("=" * 50)
        print("  Processo concluído com sucesso! OK")
        print("=" * 50)

    except mysql.connector.Error as err:
        print(f"\n  ERRO Erro de Base de Dados: {err}")
    except Exception as e:
        import traceback
        print(f"\n  ERRO Erro inesperado: {e}")
        traceback.print_exc()


if __name__ == "__main__":
    main()
