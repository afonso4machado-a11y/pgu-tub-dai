# Instruções de Implementação para o Backend (Stripe & Compra de Bilhetes)

Para dar suporte completo ao fluxo de "Comprar Bilhete" implementado no frontend móvel (`BuyTicketView.vue`), é necessário realizar as seguintes alterações no backend Java:

## 1. Dependências do Maven (`pom.xml`)
Adicionar a dependência oficial da Stripe para processamento de pagamentos:

```xml
<dependency>
 <groupId>com.stripe</groupId>
 <artifactId>stripe-java</artifactId>
 <version>24.22.0</version> <!-- Usar a versão mais recente -->
</dependency>
```

## 2. Base de Dados
É necessário criar uma nova tabela para armazenar o histórico de bilhetes comprados. Além disso, as credenciais da Stripe devem ser protegidas no ambiente (ex: usando propriedades em `application.properties` com `System.getenv`).

**Sugestão de SQL para a tabela de Bilhetes:**
```sql
CREATE TABLE IF NOT EXISTS bilhetes (
 id VARCHAR(50) PRIMARY KEY,
 cliente_id VARCHAR(50) NOT NULL,
 tipo VARCHAR(50) NOT NULL, -- Ex: 'Bilhete Simples', 'Passe Mensal'
 data_compra DATETIME NOT NULL,
 data_validade DATETIME NOT NULL,
 estado VARCHAR(20) NOT NULL DEFAULT 'Ativo', -- 'Ativo', 'Utilizado', 'Expirado'
 preco DECIMAL(10, 2) NOT NULL,
 payment_intent_id VARCHAR(100) NULL, -- ID do Stripe
 FOREIGN KEY (cliente_id) REFERENCES clientes(id)
);
```

## 3. Alterações no `pt.uminho.dai.pgu.api.ApiController` e DTOs

### 3.1. Novo Endpoint: Inicializar Pagamento (Create Payment Intent)
Criar um endpoint `POST /api/payments/create-intent` que aceita um DTO com o tipo de bilhete (e preço), chama a API do Stripe para criar um `PaymentIntent` e devolve o `clientSecret`.
- Este `clientSecret` é o que o frontend irá usar para carregar o widget do *Stripe Elements*.

### 3.2. Novo Endpoint: Confirmar Pagamento e Gerar Bilhete
Criar um endpoint `POST /api/payments/confirm` ou utilizar *Stripe Webhooks* para validar que o pagamento foi bem-sucedido.
- Uma vez validado, deve criar uma entrada na base de dados (`tabela bilhetes`).
- Deve gerar os dados do bilhete e atualizá-lo para ser retornado ao perfil do utilizador.

## 4. Alterações em `pt.uminho.dai.pgu.services.Sistema` e Repositórios
- Adicionar um novo serviço/classe (ex: `PaymentService` ou integrar em `Sistema.java`) que encapsula as invocações à biblioteca da Stripe.
- Criar o correspondente `RepositorioBilhetes.java` para salvar as transações geradas após o pagamento ser verificado.
- A configuração da chave da Stripe (`Stripe.apiKey = System.getenv("STRIPE_SECRET_KEY")`) deve ser feita no arranque (ex: no construtor do `Sistema` ou `SpringConfig`). NUNCA hardcode a chave.

Com estes elementos implementados, o fluxo entre a aplicação Vue (que recolhe os dados bancários de forma encriptada) e o backend Java (que interage com a API e emite o bilhete validado) fica totalmente funcional de acordo com os padrões de segurança do Stripe.