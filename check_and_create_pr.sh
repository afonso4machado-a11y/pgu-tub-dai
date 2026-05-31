#!/usr/bin/env bash
TOKEN="${GITHUB_TOKEN}"  # Set via environment variable

# Verificar se o PR já foi criado
echo "=== PRs abertos ==="
curl -s \
  -H "Authorization: token $TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  "https://api.github.com/repos/afonso4machado-a11y/pgu-tub-dai/pulls?state=open" \
  | python3 -c "
import sys, json
prs = json.load(sys.stdin)
if not prs:
    print('Nenhum PR aberto — criando agora...')
else:
    for p in prs:
        print(f'PR #{p[\"number\"]}: {p[\"title\"]}')
        print(f'  URL: {p[\"html_url\"]}')
        print(f'  Branch: {p[\"head\"][\"ref\"]} -> {p[\"base\"][\"ref\"]}')
"

# Criar PR se ainda não existir
echo ""
echo "=== Criando PR ==="
curl -s -X POST \
  -H "Authorization: token $TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  "https://api.github.com/repos/afonso4machado-a11y/pgu-tub-dai/pulls" \
  -d '{
    "title": "feat(dashboard): Grafico de evolucao temporal de passageiros por hora",
    "body": "## Resumo\nImplementa o **Dashboard Analitico** com grafico de linhas (`vue-chartjs`) para visualizacao da evolucao horaria do volume de passageiros.\n\n## Alteracoes\n\n### Backend\n- `RepositorioLeituras`: novo metodo `obterVolumePorHoraHoje()` com `GROUP BY HOUR(timestamp)`\n- `services/Sistema`: metodo de delegacao `obterVolumePorHoraHoje()`\n- `SistemaService`: logica **Simulacao vs Real** via env var `PGU_DEMO_MODE`\n- `core/RepositorioClientesAlertas`: corrige `guardarEmLote()` em falta (bug fix)\n\n### Frontend\n- `api.js`: adiciona `volumePorHora` ao `DEMO_DASHBOARD`\n- `DashboardView.vue`: integra `<Line>` do `vue-chartjs` sem dados hardcoded",
    "head": "feat/dashboard-analytics-chart",
    "base": "main"
  }' | python3 -c "
import sys, json
r = json.load(sys.stdin)
if 'html_url' in r:
    print('PR criado com sucesso!')
    print('URL: ' + r['html_url'])
    print('Titulo: ' + r['title'])
elif 'errors' in r or 'message' in r:
    msg = r.get('message', str(r.get('errors','')))
    if 'already exists' in msg or 'pull request' in msg.lower():
        print('PR ja existe para esta branch.')
    else:
        print('Erro: ' + msg)
"
