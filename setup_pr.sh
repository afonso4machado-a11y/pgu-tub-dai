#!/usr/bin/env bash
set -e

REPO='/mnt/c/Users/Asus/Desktop/pgu-tub-dai-main'
REMOTE='https://github.com/afonso4machado-a11y/pgu-tub-dai.git'
BRANCH='feat/dashboard-analytics-chart'

cd "$REPO"

echo "[1/5] Inicializando git..."
git init
git config user.email "pgu-tub@uminho.pt"
git config user.name "PGU Dev"

echo "[2/5] Configurando remote..."
git remote add origin "$REMOTE" 2>/dev/null || git remote set-url origin "$REMOTE"
echo "  Remote: $(git remote get-url origin)"

echo "[3/5] Fetching main..."
git fetch origin main --depth=1 2>&1 || echo "  Aviso: fetch falhou (repo pode estar vazio)"

echo "[4/5] Criando branch $BRANCH..."
git checkout -b "$BRANCH" origin/main 2>/dev/null || git checkout -b "$BRANCH"

echo "[5/5] Staging ficheiros alterados..."
git add frontend_vue/src/views/DashboardView.vue
git add frontend_vue/src/services/api.js
git add backend_java/src/main/java/pt/uminho/dai/pgu/repositories/RepositorioLeituras.java
git add backend_java/src/main/java/pt/uminho/dai/pgu/services/Sistema.java
git add backend_java/src/main/java/pt/uminho/dai/pgu/api/services/SistemaService.java
git add backend_java/src/main/java/pt/uminho/dai/pgu/core/RepositorioClientesAlertas.java

echo ""
echo "=== Ficheiros em staging ==="
git diff --cached --name-status

echo ""
echo "Commit + Push a seguir..."
git commit -m "feat(dashboard): add hourly passenger volume line chart

- RepositorioLeituras: new obterVolumePorHoraHoje() — GROUP BY HOUR SQL query
- services/Sistema: exposes obterVolumePorHoraHoje() delegation method
- api/services/SistemaService: demo vs real mode logic via PGU_DEMO_MODE env var
- frontend api.js: adds volumePorHora to DEMO_DASHBOARD
- DashboardView.vue: integrates vue-chartjs Line chart (no hardcoded data)
- core/RepositorioClientesAlertas: adds missing guardarEmLote() method (bug fix)"

echo ""
echo "=== Pushing branch $BRANCH ==="
git push -u origin "$BRANCH"

echo ""
echo "DONE — Abre o PR em:"
echo "  https://github.com/afonso4machado-a11y/pgu-tub-dai/compare/main...$BRANCH"
