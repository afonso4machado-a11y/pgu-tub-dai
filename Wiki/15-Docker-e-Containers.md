# Docker e Configuração de Containers

A plataforma PGU/TUB utiliza uma arquitetura conteinerizada para garantir paridade de ambientes entre desenvolvimento e produção. O sistema é orquestrado via Docker Compose, definindo uma stack multi-camada composta por uma base de dados MySQL, um backend Java em Spring Boot e um frontend Vue.js servido por Nginx. Todos os serviços comunicam por uma rede bridge interna chamada `pgu-network`.

## Orquestração: docker-compose.yml

O ficheiro `docker-compose.yml` funciona como o manifesto de toda a infraestrutura local. Define três serviços principais e gere as suas dependências e o networking.

### Definições dos Serviços

| Serviço | Nome do Container | Imagem / Build Context | Função |
| :--- | :--- | :--- | :--- |
| `mysql` | `tub_mysql` | `mysql:8.0` | Armazenamento relacional para frota, bilhética e dados de sensores [docker-compose.yml:4-6](). |
| `backend` | `tub_backend` | `./backend_java` | API REST Spring Boot e lógica de negócio [docker-compose.yml:20-23](). |
| `frontend` | `tub_frontend` | `./frontend_vue` | SPA Vue.js servida via Nginx [docker-compose.yml:35-38](). |

### Rede e Persistência de Dados
- **pgu-network**: Uma rede com driver bridge que permite aos containers resolverem-se entre si pelo nome do serviço (por exemplo, o backend liga-se a `jdbc:mysql://mysql:3306/tub`) [docker-compose.yml:47-48]().
- **tub_data**: Um volume nomeado mapeado para `/var/lib/mysql` para garantir a persistência da base de dados entre reinícios de container [docker-compose.yml:16,51]().
- **Inicialização**: O serviço `mysql` monta `./init.sql` em `/docker-entrypoint-initdb.d/`, garantindo que o esquema é criado no primeiro arranque [docker-compose.yml:15]().

### Topologia do Deployment
O diagrama seguinte ilustra a relação entre os serviços e o fluxo interno da rede.

**Container Interaction and Networking**
```mermaid
graph TD
    subgraph "External Network"
        "User Browser"
    end

    subgraph "pgu-network (Bridge)"
        direction TB
        ["frontend (tub_frontend)"] -- "proxy_pass /api/" --> ["backend (tub_backend)"]
        ["backend (tub_backend)"] -- "JDBC" --> ["mysql (tub_mysql)"]
    end

    "User Browser" -- "Port 80" --> ["frontend (tub_frontend)"]
    "User Browser" -- "Port 8080 (Direct API)" --> ["backend (tub_backend)"]
    
    subgraph "Volumes & Init"
        ["init.sql"] -.-> ["mysql (tub_mysql)"]
        ["tub_data (Volume)"] <==> ["mysql (tub_mysql)"]
    end
```
Fontes: [docker-compose.yml:1-51]()

---

## Configuração do Backend (Java)

O container do backend é construído usando um `Dockerfile` multi-stage para minimizar o tamanho da imagem final, separando o ambiente de build do ambiente de runtime.

### Processo de Build
1.  **Build Stage**: Usa `maven:3.9.4-eclipse-temurin-17` para compilar a aplicação [backend_java/Dockerfile:2](). Faz cache das dependências usando `mvn dependency:go-offline` antes de copiar o código fonte para otimizar o tempo de rebuild [backend_java/Dockerfile:5-6]().
2.  **Run Stage**: Usa uma imagem leve `eclipse-temurin:17-jre-alpine` [backend_java/Dockerfile:10](). O `app.jar` compilado é copiado a partir do estágio de build [backend_java/Dockerfile:12]().

### Variáveis de Ambiente
O serviço backend em `docker-compose.yml` injeta variáveis específicas do Spring para sobrepor as propriedades por omissão:
- `SPRING_DATASOURCE_URL`: Configurado para usar o nome de serviço `mysql` [docker-compose.yml:27]().
- `SPRING_DATASOURCE_USERNAME`: Definido como `tub_user` [docker-compose.yml:28]().
- `SPRING_DATASOURCE_PASSWORD`: Definido como `tub_password` [docker-compose.yml:29]().

Fontes: [backend_java/Dockerfile:1-15](), [docker-compose.yml:26-31]()

---

## Configuração do Frontend (Vue.js e Nginx)

O frontend usa um build multi-stage e um reverse proxy Nginx para tratar tanto da entrega de assets estáticos como do encaminhamento de pedidos da API.

### Reverse Proxy Nginx
O ficheiro `nginx.conf` é crítico para tratar o routing das Single Page Applications (SPA) e contornar problemas de Cross-Origin Resource Sharing (CORS) durante a execução local.

- **Routing Estático**: A diretiva `try_files` garante que URLs com deep-link são redirecionados para `index.html`, permitindo que o Vue Router trate da navegação no cliente [frontend_vue/nginx.conf:8]().
- **Proxy de API**: Os pedidos enviados para `/api/` são reencaminhados para `http://backend:8080/api/` [frontend_vue/nginx.conf:12-13](). Isto permite que o frontend use caminhos relativos para chamadas de API.

### Dockerfile do Frontend
1.  **Build Stage**: Usa `node:18-alpine` para executar `npm run build`, gerando a pasta `/dist` pronta para produção [frontend_vue/Dockerfile:2-7]().
2.  **Production Stage**: Usa `nginx:alpine`. Copia os assets estáticos para `/usr/share/nginx/html` e substitui a configuração Nginx por omissão pela `nginx.conf` do projeto [frontend_vue/Dockerfile:10-12]().

**Frontend Build and Serve Pipeline**
```mermaid
graph LR
    subgraph "Build Stage (Node.js)"
        "src/*" --> "npm run build"
        "npm run build" --> "dist/"
    end

    subgraph "Production Stage (Nginx)"
        "dist/" -- "COPY" --> "html_root [/usr/share/nginx/html]"
        "nginx.conf" -- "COPY" --> "conf_dir [/etc/nginx/conf.d/]"
    end

    "Client Request" -- "GET /dashboard" --> "nginx.conf"
    "nginx.conf" -- "try_files" --> "index.html"
    "Client Request" -- "POST /api/login" --> "nginx.conf"
    "nginx.conf" -- "proxy_pass" --> "backend:8080"
```
Fontes: [frontend_vue/Dockerfile:1-14](), [frontend_vue/nginx.conf:1-20]()

---

## Configuração da Base de Dados

O serviço `mysql` fornece a camada de persistência de dados. Está configurado com variáveis de ambiente específicas para credenciais e nome da base de dados.

### Mapeamento de Variáveis de Ambiente
O serviço usa interpolação do Docker Compose para permitir overrides via ficheiro `.env`, fornecendo valores por omissão:
- `MYSQL_ROOT_PASSWORD`: Por omissão `root` [docker-compose.yml:8]().
- `MYSQL_DATABASE`: Por omissão `tub` [docker-compose.yml:9]().
- `MYSQL_USER`: Por omissão `tub_user` [docker-compose.yml:10]().
- `MYSQL_PASSWORD`: Por omissão `tub_password` [docker-compose.yml:11]().

O serviço `backend` está explicitamente configurado para esperar pelo serviço `mysql` através da diretiva `depends_on` [docker-compose.yml:30-31]().

Fontes: [docker-compose.yml:4-18]()
