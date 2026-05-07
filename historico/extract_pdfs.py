import os
import tabula

# Lê o PDF e converte diretamente para CSV
# 'pages="all"' lê o ficheiro inteiro
tabula.convert_into("07h.pdf", "07h.csv", output_format="csv", pages='all')
tabula.convert_into("40h.pdf", "40h.csv", output_format="csv", pages='all')
tabula.convert_into("43h.pdf", "43h.csv", output_format="csv", pages='all')

print("Feito! Verifica o ficheiro horarios.csv")

