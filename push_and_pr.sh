#!/usr/bin/env bash
# Uso: bash push_and_pr.sh <github_username> <github_token>
set -e

USERNAME="$1"
TOKEN="$2"
REPO_PATH='/mnt/c/Users/Asus/Desktop/pgu-tub-dai-main'
REPO_OWNER='afonso4machado-a11y'
REPO_NAME='pgu-tub-dai'
BRANCH='feat/dashboard-analytics-chart'

if [ -z "$USERNAME" ] || [ -z "$TOKEN" ]; then
  echo "Uso: bash push_and_pr.sh <username> <token>"
  exit 1
fi

cd "$REPO_PATH"

# Configura remote com token embutido (limpo depois)
git remote set-url origin "https://${USERNAME}:${TOKEN}@github.com/${REPO_OWNER}/${REPO_NAME}.git"

echo "=== Pushing branch $BRANCH ==="
git push -u origin "$BRANCH"

echo ""
echo "=== Criando Pull Request via GitHub API ==="
PR_RESPONSE=$(curl -s -X POST \
  -H "Authorization: token $TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  "https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/pulls" \
  -d '{
    "title": "feat(dashboard): Gráfico de evolução temporal de passageiros por hora",
    "body": "## Resumo\nImplementa o **Dashboard Analítico** com gráfico de linhas (`vue-chartjs`) para visualização da evolução horária do volume de passageiros.\n\n## Alterações\n\n### Backend\n- `RepositorioLeituras`: novo método `obterVolumePorHoraHoje()` — query `SELECT HOUR(timestamp), SUM(entradas) ... GROUP BY HOUR` para o dia atual\n- `services/Sistema`: método de delegação `obterVolumePorHoraHoje()`\n- `SistemaService.obterDadosDashboard()`: lógica **Simulação vs Real** via env var `PGU_DEMO_MODE` — picos fictícios realistas em demo, dados reais da BD em produção\n- `core/RepositorioClientesAlertas`: corrige método `guardarEmLote()` em falta (bug fix)\n\n### Frontend\n- `api.js`: adiciona `volumePorHora` ao `DEMO_DASHBOARD`\n- `DashboardView.vue`: integra `<Line>` do `vue-chartjs` — sem dados hardcoded no template, tudo vem do endpoint `/api/dashboard`\n\n## Como testar\n- **Demo mode**: ligar o toggle Demo no frontend — gráfico mostra picos matinais e fim de tarde simulados\n- **Real mode**: sem `PGU_DEMO_MODE` no backend — usa dados reais da BD (`GROUP BY HOUR` de hoje)",
    "head": "feat/dashboard-analytics-chart",
    "base": "main"
  }')

# Limpar o token do remote URL por segurança
git remote set-url origin "https://github.com/${REPO_OWNER}/${REPO_NAME}.git"

# Extrair URL do PR
PR_URL=$(echo "$PR_RESPONSE" | grep -o '"html_url": *"[^"]*"' | head -1 | sed 's/"html_url": *"\(.*\)"/\1/')

if [ -n "$PR_URL" ] && [[ "$PR_URL" == *"pull"* ]]; then
  echo ""
  echo "✅ Pull Request criado com sucesso!"
  echo "   $PR_URL"
else
  echo ""
  echo "Push feito! PR pode já existir ou houve erro na API."
  echo "Resposta da API: $PR_RESPONSE" | head -5
  echo ""
  echo "Abre o PR manualmente em:"
  echo "  https://github.com/${REPO_OWNER}/${REPO_NAME}/compare/main...${BRANCH}"
fi
