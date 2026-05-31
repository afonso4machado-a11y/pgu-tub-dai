#!/usr/bin/env bash
set -e

TOKEN="${GITHUB_TOKEN}"  # Set via environment variable
REMOTE="https://afonso4machado-a11y:${TOKEN}@github.com/afonso4machado-a11y/pgu-tub-dai.git"
BRANCH="feat/dashboard-analytics-chart"
WORK="/mnt/c/Users/Asus/Desktop/pgu-tub-dai-main"
CLONE_DIR="/tmp/pgu_clone_$$"

cd "$WORK"

# 1. Remover .git problemático
echo "[1/6] Limpando repo local antigo..."
rm -rf .git

# 2. Clonar o main numa pasta temporária
echo "[2/6] Clonando historico do remote (main)..."
git clone --depth=1 --single-branch -b main "$REMOTE" "$CLONE_DIR"

# 3. Trazer apenas o .git para o projecto local
echo "[3/6] Importando historico para o projecto..."
cp -r "$CLONE_DIR/.git" .
rm -rf "$CLONE_DIR"

# Ajustar o remote para remover o token depois
git remote set-url origin "https://github.com/afonso4machado-a11y/pgu-tub-dai.git"

# 4. Criar branch de feature
echo "[4/6] Criando branch ${BRANCH}..."
git checkout -b "$BRANCH"

# 5. Stage dos 6 ficheiros modificados
echo "[5/6] Staging dos ficheiros alterados..."
git add "frontend_vue/src/views/DashboardView.vue"
git add "frontend_vue/src/services/api.js"
git add "backend_java/src/main/java/pt/uminho/dai/pgu/repositories/RepositorioLeituras.java"
git add "backend_java/src/main/java/pt/uminho/dai/pgu/services/Sistema.java"
git add "backend_java/src/main/java/pt/uminho/dai/pgu/api/services/SistemaService.java"
git add "backend_java/src/main/java/pt/uminho/dai/pgu/core/RepositorioClientesAlertas.java"

echo ""
echo "=== Ficheiros a commitar ==="
git diff --cached --stat

# 6. Commit + Push
echo ""
echo "[6/6] Commit..."
git commit -m "feat(dashboard): add hourly passenger volume line chart

- RepositorioLeituras: new obterVolumePorHoraHoje() with GROUP BY HOUR SQL query
- services/Sistema: exposes obterVolumePorHoraHoje() delegation method
- api/services/SistemaService: demo vs real mode logic via PGU_DEMO_MODE env var
- frontend api.js: adds volumePorHora to DEMO_DASHBOARD
- DashboardView.vue: integrates vue-chartjs Line chart (no hardcoded data)
- core/RepositorioClientesAlertas: adds missing guardarEmLote() method (bug fix)"

echo ""
echo "=== Pushing ==="
git push "https://afonso4machado-a11y:${TOKEN}@github.com/afonso4machado-a11y/pgu-tub-dai.git" "${BRANCH}" --force

# Criar PR via API
echo ""
echo "=== Criando Pull Request ==="
PR=$(curl -s -X POST \
  -H "Authorization: token ${TOKEN}" \
  -H "Accept: application/vnd.github.v3+json" \
  "https://api.github.com/repos/afonso4machado-a11y/pgu-tub-dai/pulls" \
  -d "{\"title\":\"feat(dashboard): Grafico de evolucao temporal de passageiros por hora\",\"body\":\"Implementa grafico de linhas (vue-chartjs) no Dashboard Analitico. Dados servidos pelo /api/dashboard sem hardcode no frontend. Backend com logica Simulacao vs Real via PGU_DEMO_MODE.\",\"head\":\"feat/dashboard-analytics-chart\",\"base\":\"main\"}")

PR_URL=$(echo "$PR" | python3 -c "import sys,json; r=json.load(sys.stdin); print(r.get('html_url','ERRO: '+r.get('message','desconhecido')))")
echo ""
echo "Resultado: $PR_URL"
