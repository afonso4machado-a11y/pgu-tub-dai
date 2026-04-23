# PGU — Plataforma de Gestão Unificada (TUB)

> Prova de conceito para gestão inteligente da frota de autocarros dos Transportes Urbanos de Braga (TUB), desenvolvida no âmbito da UC de Desenvolvimento de Aplicações Informáticas (DAI) — Universidade do Minho, 2026.

---

## 🏗️ Arquitetura

O sistema segue uma **arquitetura em camadas** mapeada pelo método 4SRS:

| Camada | Tecnologia | Descrição |
|--------|-----------|-----------|
| **P5 — UI** | Vue.js 3 + Vite (PWA) | Backoffice (Dashboard, Frota, Correlação, Alertas) + App Passageiro |
| **P2 — Business Logic** | Java 21 + Spring Boot | Motor de Ingestão IoT, Correlação, Alertas, Autenticação |
| **P7 — Data** | MySQL (Azure Flexible Server) | Persistência via JDBC com padrão Repository |

```
┌─────────────────────────────────────────────────┐
│  Frontend Vue.js (SPA/PWA)                      │
│  ├── Backoffice (Gestor Operacional)            │
│  └── App Passageiro (Mobile-First)              │
├─────────────────────────────────────────────────┤
│  API REST (Spring Boot)                         │
│  ├── ApiController → SistemaService → Sistema   │
│  ├── Health Check (/api/health)                 │
│  └── Auth (Admin @uminho.pt + Passageiro)       │
├─────────────────────────────────────────────────┤
│  MySQL — Azure App Service                      │
│  └── 10 tabelas com índices B-Tree otimizados   │
└─────────────────────────────────────────────────┘
```

## 🚀 Execução Local

### Pré-requisitos
- Java 21+
- Node.js 18+
- MySQL 8+ (ou Docker)

### 1. Base de Dados
```bash
# Opção A: Docker
docker-compose up -d

# Opção B: MySQL local
mysql -u root < init.sql
```

### 2. Backend
```bash
cd backend_java
cp .env.example .env  # Editar com credenciais reais
mvn clean install
mvn spring-boot:run
# → http://localhost:8080/api/health
```

### 3. Frontend
```bash
cd frontend_vue
npm install
npm run dev
# → http://localhost:5173
```

## 🌐 Deploy (Azure)
- **Backoffice:** `https://pgu-tub-frontend-2026.azurewebsites.net/`
- **API REST:** `https://backend-pgu-tub-2026.azurewebsites.net/api`
- **Health Check:** `https://backend-pgu-tub-2026.azurewebsites.net/api/health`

## 📁 Estrutura do Repositório

```
TrabalhoDAI-master/
├── backend_java/               # API REST (Spring Boot + JDBC)
│   ├── src/main/java/.../api/  # Controller, Service, Config
│   ├── src/main/java/.../core/ # Domínio: Sistema, Repositórios, Entidades
│   ├── .env.example            # Template de configuração
│   └── pom.xml                 # Dependências Maven
├── frontend_vue/               # SPA/PWA (Vue.js 3 + Vite)
│   ├── src/views/              # Views do Backoffice
│   ├── src/views/passenger/    # Views da App Passageiro
│   ├── src/views/admin/        # Login Administrador
│   ├── src/services/           # API + Auth services
│   └── src/components/         # Sidebar, TopHeader
├── diagramas/                  # Diagramas arquiteturais (PNG)
├── init.sql                    # Schema MySQL com índices B-Tree
├── docker-compose.yml          # MySQL local para desenvolvimento
└── simulador-pgu.html          # Simulador de sensores IoT
```

## 🔑 API Endpoints

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `GET` | `/api/health` | Health check do sistema |
| `GET` | `/api/dashboard` | Dados do painel operacional |
| `GET` | `/api/autocarros` | Listar frota |
| `GET` | `/api/autocarros/:id` | Detalhes de um autocarro |
| `POST` | `/api/leituras` | Registar leitura de sensor IoT |
| `GET` | `/api/correlacao` | Motor de correlação procura/oferta |
| `GET` | `/api/alertas` | Alertas recentes do sistema |
| `GET` | `/api/historico` | Histórico de ocupação por dia |
| `GET` | `/api/linhas` | Listar linhas TUB |
| `GET` | `/api/paragens` | Listar paragens |
| `POST` | `/api/auth/login` | Login passageiro |
| `POST` | `/api/auth/signup` | Registo passageiro |
| `POST` | `/api/auth/admin/login` | Login administrador |
| `GET` | `/api/auth/profile/:id` | Perfil do utilizador |
| `PUT` | `/api/auth/profile/:id` | Atualizar perfil |

## 🔒 Autenticação

- **Administrador:** Email institucional `@uminho.pt` + password mestre
- **Passageiro:** Registo com email/password (persistido em MySQL)
- **Modo Demo:** Funciona sem backend ativo (fallback automático com dados simulados)

## 👥 Equipa

Projeto desenvolvido por alunos da Universidade do Minho — Licenciatura em Engenharia Informática, 2025/2026.

---

> **Nota Técnica:** O ficheiro `.env` com credenciais da BD está excluído do Git (ver `.gitignore`). Consultar `.env.example` para a estrutura necessária.
