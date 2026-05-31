# ============================================================
# criar_pr.ps1 — Inicializa o git, cria branch e abre PR
# Executa na raiz do projeto: .\criar_pr.ps1 -Remote "https://github.com/OWNER/REPO.git"
# ============================================================
param(
    [Parameter(Mandatory=$true)]
    [string]$Remote  # ex: https://github.com/afonso4machado/pgu-tub-dai.git
)

$branch = "feat/dashboard-analytics-chart"
$commitMsg = "feat(dashboard): add hourly passenger volume line chart

- RepositorioLeituras: new obterVolumePorHoraHoje() — GROUP BY HOUR SQL query
- services/Sistema: exposes obterVolumePorHoraHoje() delegation method
- api/services/SistemaService: demo vs real mode logic via PGU_DEMO_MODE env var
- frontend api.js: adds volumePorHora to DEMO_DASHBOARD
- DashboardView.vue: integrates vue-chartjs Line chart (no hardcoded data)
- core/RepositorioClientesAlertas: adds missing guardarEmLote() method"

Set-Location "c:\Users\Asus\Desktop\pgu-tub-dai-main"

# 1 - Init se necessario
if (-not (Test-Path ".git")) {
    Write-Host "[1/6] Inicializando repositório git..." -ForegroundColor Cyan
    git init
    git remote add origin $Remote
} else {
    Write-Host "[1/6] Repositório git já existe." -ForegroundColor Green
    $existingRemote = git remote get-url origin 2>$null
    if (-not $existingRemote) {
        git remote add origin $Remote
    }
}

# 2 - Fetch para ter o historico do remote
Write-Host "[2/6] Fetching do remote..." -ForegroundColor Cyan
git fetch origin main --depth=1 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "  Aviso: fetch falhou (pode ser normal se o repo ainda nao tem historico remoto)" -ForegroundColor Yellow
}

# 3 - Criar branch a partir de main
Write-Host "[3/6] Criando branch $branch..." -ForegroundColor Cyan
git checkout -b $branch origin/main 2>$null
if ($LASTEXITCODE -ne 0) {
    git checkout -b $branch
}

# 4 - Stage apenas os ficheiros modificados
Write-Host "[4/6] Staging ficheiros alterados..." -ForegroundColor Cyan
$files = @(
    "frontend_vue/src/views/DashboardView.vue",
    "frontend_vue/src/services/api.js",
    "backend_java/src/main/java/pt/uminho/dai/pgu/repositories/RepositorioLeituras.java",
    "backend_java/src/main/java/pt/uminho/dai/pgu/services/Sistema.java",
    "backend_java/src/main/java/pt/uminho/dai/pgu/api/services/SistemaService.java",
    "backend_java/src/main/java/pt/uminho/dai/pgu/core/RepositorioClientesAlertas.java"
)
foreach ($f in $files) {
    git add $f
    Write-Host "  + $f" -ForegroundColor Gray
}

# 5 - Commit
Write-Host "[5/6] Commit..." -ForegroundColor Cyan
git commit -m $commitMsg

# 6 - Push e criar PR
Write-Host "[6/6] Push + PR..." -ForegroundColor Cyan
git push -u origin $branch

# Usar GitHub CLI se disponivel
$ghAvailable = Get-Command gh -ErrorAction SilentlyContinue
if ($ghAvailable) {
    gh pr create `
        --title "feat(dashboard): Gráfico de evolução temporal de passageiros" `
        --body "## Resumo
Implementa o Dashboard Analítico com gráfico de linhas (vue-chartjs) para visualização da evolução horária do volume de passageiros.

## Alterações
### Backend
- \`RepositorioLeituras\`: novo método \`obterVolumePorHoraHoje()\` — query \`GROUP BY HOUR(timestamp)\` para o dia atual
- \`services/Sistema\`: método de delegação \`obterVolumePorHoraHoje()\`
- \`SistemaService.obterDadosDashboard()\`: lógica **Simulação vs Real** via env var \`PGU_DEMO_MODE\` — injeta dados fictícios com picos realistas em demo, dados reais da BD em produção
- \`core/RepositorioClientesAlertas\`: corrige método \`guardarEmLote()\` em falta (bug fix)

### Frontend
- \`api.js\`: adiciona \`volumePorHora\` ao \`DEMO_DASHBOARD\`
- \`DashboardView.vue\`: integra \`<Line>\` do \`vue-chartjs\` — sem dados hardcoded no template, tudo vem do endpoint \`/api/dashboard\`

## Como testar
- **Demo mode**: ligar o toggle Demo no frontend — o gráfico mostra picos matinais e fim de tarde simulados
- **Real mode**: configurar \`PGU_DEMO_MODE=1\` no backend para simular via env var, ou deixar sem valor para usar dados reais da BD" `
        --base main `
        --head $branch
} else {
    Write-Host ""
    Write-Host "GitHub CLI (gh) nao encontrado. Abre o PR manualmente em:" -ForegroundColor Yellow
    Write-Host "  $Remote" -ForegroundColor White
    Write-Host "  Branch: $branch → main" -ForegroundColor White
}

Write-Host ""
Write-Host "Concluido!" -ForegroundColor Green
