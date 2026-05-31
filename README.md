# PGU — Plataforma de Gestão Unificada (TUB)

Prova de conceito para gestão inteligente da frota de autocarros dos Transportes Urbanos de Braga (TUB), desenvolvida no âmbito da Unidade Curricular de Desenvolvimento de Aplicações Informáticas (DAI) — Licenciatura em Engenharia Informática, Universidade do Minho, 2025/2026.

---

## Arquitetura do Sistema

O sistema segue uma arquitetura em camadas (N-Tier) rigorosamente mapeada de acordo com os resultados do método 4SRS:

| Camada | Tecnologia | Descrição |
|--------|-----------|-----------|
| **P5 — UI** | Vue.js 3 + Vite (PWA) | Cliente frontend para Backoffice (Dashboard, Frota, Correlação, Alertas) + App Passageiro |
| **API REST Layer** | Java 17 + Spring Boot | Interface de servidor (Controllers, DTOs e Configuração) |
| **P2 — Business Logic** | Java 17 (Classes de Negócio) | Lógica e regras de domínio estruturadas por pilares funcionais (p2_businesslogic) |
| **P7 — Data** | JDBC + MySQL (Azure Flexible) | Persistência relacional com o padrão Repository (p7_data) |

```
┌────────────────────────────────────────────────────────┐
│ P5 UI (Frontend Vue.js SPA/PWA) │
│ ├── Backoffice (Gestor Operacional de Frota) │
│ └── App Passageiro (Interface Mobile-First) │
├────────────────────────────────────────────────────────┤
│ REST API (Spring Boot HTTP Entrypoint) │
│ └── ApiController / PaymentController │
├────────────────────────────────────────────────────────┤
│ P2 Business Logic (Camada de Domínio / Regras) │
│ └── pt.uminho.dai.pgu.p2_businesslogic │
├────────────────────────────────────────────────────────┤
│ P7 Data Layer (Persistência / Conexão JDBC) │
│ └── pt.uminho.dai.pgu.p7_data │
├────────────────────────────────────────────────────────┤
│ MySQL Database (Azure Poland Central) │
│ └── 10 tabelas relacionais com índices B-Tree │
└────────────────────────────────────────────────────────┘
```

---

## Execução Local

### Pré-requisitos
- Java 17 ou superior
- Node.js 18 ou superior
- MySQL 8 ou superior (ou ambiente Docker operacional)

### 1. Base de Dados
Instale a base de dados utilizando uma das opções abaixo:

```bash
# Opção A: Utilizando Docker Compose (Recomendado)
docker-compose up -d

# Opção B: Importando diretamente para o MySQL local
mysql -u root < init.sql
```

### 2. Servidor Backend (Spring Boot)
Configure as credenciais e inicie a API:

```bash
cd backend_java
cp .env.example .env # Edite o ficheiro com as credenciais reais do seu ambiente
mvn clean install
mvn spring-boot:run
# O servidor iniciará em: http://localhost:8080
```

### 3. Cliente Frontend (Vue.js)
Instale as dependências e inicie o servidor de desenvolvimento:

```bash
cd frontend_vue
npm install
npm run dev
# O cliente iniciará em: http://localhost:5173
```

---

## Deploy na Cloud (Microsoft Azure)

- **Backoffice e PWA:** `https://pgu-tub-frontend-2026.azurewebsites.net/`
- **Servidor API REST:** `https://backend-pgu-tub-2026.azurewebsites.net/api`
- **Health Check da API:** `https://backend-pgu-tub-2026.azurewebsites.net/api/health`

---

## Estrutura do Repositório

O repositório encontra-se organizado de forma modular, permitindo a perfeita correspondência entre os Casos de Uso (UCs) e a sua implementação física:

```
pgu-tub-dai/
├── backend_java/ # API REST e Lógica Servidor (Spring Boot)
│ ├── src/main/java/pt/uminho/dai/pgu/
│ │ ├── api/ # REST API Layer (Controllers, DTOs e Configurações)
│ │ │ ├── acessos_configuracao/ # Endpoints de login e pagamentos via Stripe
│ │ │ ├── operacao_tempo_real/ # Endpoints de telemetria e leituras IoT
│ │ │ └── analitica_historico/ # Endpoints de dashboards e correlacionador
│ │ ├── p2_businesslogic/ # Business Logic Layer (Regras de Domínio - Pilar P2)
│ │ │ ├── acessos_configuracao/ # Entidades Cliente e Bilhete
│ │ │ ├── operacao_tempo_real/ # Entidades Autocarro, Leitura, Alerta, Paragem e Linha
│ │ │ └── analitica_historico/ # Motor central de processamento (Sistema.java)
│ │ └── p7_data/ # Data Persistence Layer (Repositórios JDBC - Pilar P7)
│ │ ├── acessos_configuracao/ # Repositórios de Clientes e Bilhetes
│ │ ├── operacao_tempo_real/ # Repositórios de Leituras, Veículos e Alertas
│ │ └── analitica_historico/ # Repositório de Correlação de dados GTFS
│ └── src/test/java/ # Testes Unitários e de Integração (JUnit)
├── frontend_vue/ # Frontend SPA/PWA (Vue.js 3 + Vite)
│ ├── src/views/ # Ecrãs do utilizador (Backoffice e Mobile)
│ ├── src/components/ # Componentes reutilizáveis
│ └── src/services/ # Serviços de comunicação HTTP com a API
├── Wiki/ # Documentação técnica completa da plataforma
├── historico/ # Ficheiros de suporte académico e dados reais
├── tools/ # Simulador de sensores IoT e ferramentas de testes
├── docker-compose.yml # Configuração de orquestração local
├── init.sql # Ficheiro de criação de base de dados e índices
└── pom.xml # Configuração Maven Multi-Module
```

---

## Endpoints Principais da API

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `GET` | `/api/health` | Estado de funcionamento do sistema |
| `GET` | `/api/dashboard` | Sumário estatístico para o painel operacional |
| `GET` | `/api/autocarros` | Listagem da frota TUB |
| `GET` | `/api/autocarros/:id` | Detalhe de lotação e estado de um veículo |
| `POST` | `/api/leituras` | Submissão de leituras de sensores IoT das portas |
| `GET` | `/api/correlacao` | Cruzamento procura (contagem) vs. oferta (GTFS/Bilhética) |
| `GET` | `/api/alertas` | Histórico recente de anomalias operacionais |
| `GET` | `/api/historico` | Histórico analítico por dia |
| `GET` | `/api/linhas` | Listagem das linhas TUB mapeadas |
| `GET` | `/api/paragens` | Listagem das paragens de autocarros |
| `POST` | `/api/auth/login` | Autenticação de passageiros |
| `POST` | `/api/auth/signup` | Registo de novos passageiros |
| `POST` | `/api/auth/admin/login` | Autenticação de Administradores |
| `GET` | `/api/auth/profile/:id` | Consulta de perfil do utilizador |
| `PUT` | `/api/auth/profile/:id` | Atualização de perfil |
| `POST` | `/api/payments/create-intent` | Inicialização de pagamento Stripe (PaymentIntent) |
| `POST` | `/api/payments/webhook` | Receção e validação do pagamento (Emissão de Bilhete) |

---

## Controlo de Acesso e Autenticação

- **Administrador:** Acesso restrito a utilizadores com domínio `@uminho.pt` ou `@um.pt`. Permite monitorizar KPIs operacionais, frota e correlações sensíveis.
- **Passageiro:** Registo comum com email e palavra-passe. O utilizador pode consultar o estado da rede e adquirir bilhetes virtuais com geração de Código QR dinâmico anti-fraude.
- **Modo de Demonstração (Fallback):** Caso a ligação ao backend falhe, a aplicação Vue.js ativa um modo demo automático em cache para garantir a continuidade da navegação.

---

## Equipa de Desenvolvimento

Projeto curricular desenvolvido por alunos da Universidade do Minho — Licenciatura em Engenharia Informática.

*Nota de Segurança: O ficheiro de variáveis de ambiente `.env` encontra-se devidamente excluído do controlo de versões (conforme as definições do `.gitignore`). Consulte o ficheiro `.env.example` para conhecer os parâmetros obrigatórios em ambiente de produção.*
