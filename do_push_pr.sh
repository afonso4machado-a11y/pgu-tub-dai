#!/usr/bin/env bash
set -e
TOKEN="${GITHUB_TOKEN}"  # Set via environment variable
REPO="afonso4machado-a11y/pgu-tub-dai"
BRANCH="feat/dashboard-analytics-chart"

cd /mnt/c/Users/Asus/Desktop/pgu-tub-dai-main

echo "=== Push ==="
git push "https://afonso4machado-a11y:${TOKEN}@github.com/${REPO}.git" "${BRANCH}" --force
echo "Push OK"

echo ""
echo "=== Criar PR via API ==="
BODY='{"title":"feat(dashboard): Grafico de evolucao temporal de passageiros por hora","body":"Implementa grafico de linhas (vue-chartjs) no Dashboard Analitico. Dados servidos pelo /api/dashboard sem hardcode no frontend. Backend com logica Simulacao vs Real via PGU_DEMO_MODE e query GROUP BY HOUR para dados reais da BD.","head":"feat/dashboard-analytics-chart","base":"main"}'

RESULT=$(curl -s -X POST \
  -H "Authorization: token ${TOKEN}" \
  -H "Accept: application/vnd.github.v3+json" \
  "https://api.github.com/repos/${REPO}/pulls" \
  -d "${BODY}")

echo "$RESULT" | python3 -c "
import sys, json
r = json.load(sys.stdin)
if 'html_url' in r:
    print('PR criado com sucesso!')
    print('URL: ' + r['html_url'])
    print('Numero: #' + str(r['number']))
elif 'errors' in r:
    for e in r['errors']:
        msg = e.get('message','')
        if 'already exists' in msg:
            print('PR ja existe para esta branch.')
        else:
            print('Erro: ' + msg)
else:
    print('Resposta: ' + r.get('message', 'desconhecida'))
"
