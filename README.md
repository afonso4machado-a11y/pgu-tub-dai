# PGU - Plataforma Integrada de Gestão (TUB)

Este repositório contém a infraestrutura e código da plataforma PGU (Plataforma Global Unificada) desenvolvida como prova de conceito para a disciplina de DAI. O sistema divide-se num backend robusto Java Spring Boot para processamento central e IoT, e uma PWA frontend em Vue.js focada em alta disponibilidade para passageiros e visualização tática para gestores.

---

## 🛠️ Stack Tecnológica
- **Backend:** Java 21, Spring Boot, JDBC, Padrão MVC e Service Layer.
- **Frontend:** Vue.js 3 + Vite, Componentes Responsivos (Glassmorphism), Chart.js
- **Database:** MySQL Flexible Server Integrado no Azure Cloud.
- **Arquitetura Visual:** Mapeada via Standard C4.

## 🚀 Como Iniciar

A solução pode ser visualizada via cloud pública, dispensando instalação local massiva para fins de demonstração:
- **Painel Administrativo:** `https://pgu-tub-frontend-2026.azurewebsites.net/`
- **Serviço de API REST Base:** `https://backend-pgu-tub-2026.azurewebsites.net/api`

### Execução Local (Modo Desenvolvedor)

Se pretender inspecionar a base local de código e compilar.

**1. Base de Dados**
O ficheiro `init.sql` já contempla índices B-Tree de alta performance e as tabelas core (`autocarros`, `leituras`, `alertas`, `linhas`, etc). 
Apenas inicie o contêiner de Docker anexo:
```bash
docker-compose up -d
```

**2. Backend Java**
```bash
cd backend_java
mvn clean install
mvn spring-boot:run
```

**3. Frontend Vue**
```bash
cd frontend_vue
npm install
npm run dev
```

---

## 📚 Mapa de Navegação da Arquitetura (Wiki)
O repositório foi limpo e organizado seguindo standard governamentais empresariais:
- `/backend_java`: Alberga os `Repositorios`, as sub-camadas de Domain Entities e a camda de `services/SistemaService` que encapsula o *Motor de Correlação* de Gestão de Transportes (Lógica IoT).
- `/frontend_vue`: Contém as interfaces adaptativas. As diretrizes `@ui-ux-pro-max` impulsionaram uma conversão estética premium do layout original.
- `/diagramas`: Modelos plantUML representativos de Casos de Uso.
- `/05_Relatorios_Finais_e_Arquitetura`: Ficheiros centrais do C4 Model, Avaliação de Custos na Cloud Azure e Relatórios 4SRS formatados profissionalmente.

> **Nota para Avaliação Júri:** A componente Backend foi reavaliada em Abril de 2026 para respeitar na íntegra a injeção via padrões `Service` separando controlos HTTP da lógica pesada. Todas as dependências (PK/FK) na database MySQL são agora cobertas por Cascatas e B-Tree Indexes prevenindo asfixia transacional na Cloud.
