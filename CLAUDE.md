    aude# CLAUDE.md

Diretrizes arquiteturais, regras de negócio e stack para o Claude Code (ou qualquer IA) trabalhar neste repositório. Baseado em `contexto.md` (visão de produto) e ajustado ao estado real do código.

## 🏢 O Projeto

**Sistema Revisional Bancário Web** — Plataforma SaaS B2B para advogados e auditores jurídicos.

Automatiza auditoria de contratos de financiamento bancário (foco veículos PF): extração de dados via OCR, métodos numéricos para descobrir taxas de juros ocultas (engenharia reversa), comparação com o Banco Central (BCB) e geração de laudos técnicos/jurídicos em PDF.

## 🧱 Stack (estado real do código)

- **Java 17 (LTS)**, **Spring Boot 4.1.0**, empacotamento **WAR** (Tomcat externo).
- Spring Boot starters: `webmvc`, `data-jpa`, `actuator`, `devtools`.
- **Lombok** (getters/setters/builders).
- **PostgreSQL** (driver `postgresql`).
- Build: **Maven** (wrapper `mvnw` na raiz do back-end).

> ⚠️ Divergência com `contexto.md`: o documento de visão cita Spring Boot 3.3.x, Flyway, Spring Security/JWT, PDFBox/Tess4J/OpenPDF, Testcontainers, RestClient. **Ainda não estão no `pom.xml`.** Tratar como roadmap, não como dependências instaladas. Confirmar antes de assumir que existem.

## 📁 Estrutura real do repositório

```
revisional.dev/
├── revisonalweb_back-end/        # API Spring Boot (note grafia "revisonal")
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd
│   └── src/main/java/br/com/mpgsistemas/revisionalweb/api/
│       ├── RevisonalwebBackEndApplication.java
│       ├── ServletInitializer.java
│       ├── dto/      → ReferenciaMercado, ResultadoCalculo
│       └── model/    → Usuario, CasoRevisional, DadosContrato, UploadDocumento,
│                       EventoAuditoria, ParametrosSistema, CampoExtraido
└── revisional_front-end/         # React SPA (Create React App)
    ├── package.json
    └── src/ (App.js, index.js, ...)
```

- **Pacote base real:** `br.com.mpgsistemas.revisionalweb.api` (o `contexto.md` cita `br.com.revisional` — usar o real).
- Camadas `controller/`, `service/`, `repository/`, `config/`, `security/` ainda **não existem** — criar conforme a Clean Architecture abaixo quando necessário.
- Diretórios `docker/` e GitHub Actions (`deploy.yml`) ainda não existem.

## 🚀 Comandos

### Back-end (em `revisonalweb_back-end/`)
```bash
./mvnw clean package          # Compila (gera WAR)
./mvnw spring-boot:run        # Dev, porta 8080
./mvnw test                   # Suíte de testes
```

### Front-end (em `revisional_front-end/`)
```bash
npm install
npm start                     # Dev server, proxy /api → localhost:8080
```

## ☁️ Git Flow e Branches

- **`develop`** — branch de trabalho e integração (commitar aqui).
- **`main`** — produção (release). **Protegida**: merge só via Pull Request (1 approval, force-push e delete bloqueados, `enforce_admins` ativo).

### Commits
- **Não adicionar a Anthropic/Claude como co-autor** nos commits. Nada de `Co-Authored-By: Claude` nem rodapé "🤖 Generated with Claude Code". Mesmo padrão do projeto `sigapsi.dev`.

## 🔄 CI/CD e Deploy (modelo herdado do projeto sigapsi.dev)

Pipeline de deploy contínuo via **GitHub Actions + SSH** numa VPS Linux rodando Docker Compose. Padrão validado no projeto irmão `sigapsi.dev` e adaptado aqui.

### Gatilho
- Push/merge em **`main`** dispara `.github/workflows/deploy.yml`.
- Job usa `appleboy/ssh-action`: conecta na VPS, `git pull origin main`, sobe os containers e limpa imagens órfãs.

```yaml
# .github/workflows/deploy.yml (modelo)
name: Deploy Revisional
on:
  push:
    branches: [ "main" ]
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: SSH Deploy
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.HOST }}
          username: ${{ secrets.USERNAME }}
          key: ${{ secrets.SSH_KEY }}
          script: |
            cd /home/ubuntu/revisional
            git pull origin main
            cd docker
            docker compose -f docker-compose.prod.yml up -d --build
            docker image prune -f
```

### Secrets GitHub necessários
`HOST` (IP/host da VPS), `USERNAME` (usuário SSH), `SSH_KEY` (chave privada). Definir em *Settings → Secrets and variables → Actions*.

### Topologia Docker em produção (`docker/docker-compose.prod.yml`)
- **postgres:16** — volume persistente `../data/postgres`, credenciais via `.env`.
- **backend** — build de `revisonalweb_back-end/` (multi-stage: `maven:3.9-eclipse-temurin-17` → empacota WAR → `tomcat:11.0-jdk17` como `ROOT.war`, porta 8080).
- **frontend** — build de `revisional_front-end/` (multi-stage: `node:20-alpine` → `npm run build` → `nginx:stable-alpine` servindo `build/`, porta 80).
- **apache-proxy** (`httpd:alpine`) — reverse proxy SSL nas portas 80/443: `/api/`→`backend:8080`, `/`→`frontend:80`. Habilita módulos proxy/ssl/rewrite via `sed` no boot.
- **certbot** — Let's Encrypt, renovação de certificado via webroot challenge (`/.well-known/acme-challenge/`).
- Rede bridge dedicada; todos os serviços `restart: always`.

> Estes arquivos (`docker/`, Dockerfiles, `deploy.yml`, vhost Apache) **ainda não existem** neste repo — são o alvo a criar, espelhando `sigapsi.dev`. O backend é WAR (igual ao sigapsi), então o Dockerfile do backend reaproveita o mesmo padrão Tomcat.

## 🏗️ Arquitetura Back-end (alvo: Clean Architecture)

- `controller/` → Endpoints REST.
- `service/` → Lógica isolada (Cálculos, OCR, Integração BCB, PDFs). **Não conhece banco nem HTTP.**
- `model/` (entity) → Entidades JPA (`Usuario`, `CasoRevisional`).
- `dto/` → Value Objects / composições salvas como **JSONB** (`DadosContrato`, `ResultadoCalculo`).
- `repository/` → Spring Data JPA.
- `config/` → Configs globais + `ParametrosSistema` (sem magic numbers).
- `security/` → Spring Security, filtros JWT, hash de senha (BCrypt).

### Motores matemáticos (core do negócio)
`CalculadoraFinanceiraService` NÃO usa libs financeiras de terceiros — precisão pericial e memória de cálculo.

1. **Conversão de taxa:** `CET_anual = (1 + taxa_mensal)^12 - 1`
2. **PRICE (PMT):** `PMT = PV * i / (1 - (1+i)^-n)`
3. **Engenharia reversa (Bisseção):** se o contrato omite a taxa, bissecção entre `0.0` e `1.0` (limite de iterações em `ParametrosSistema`) até bater com a parcela.
4. **CET (XIRR/VPL):** fluxo de caixa em dias corridos zerando o VPL → CET anual.
5. **Score de risco (Spread):** `R = taxa_contrato / taxa_mercado`. `R ≥ 2.0` indício forte; `R ≥ 1.50` indício moderado.

### Modelagem de dados
- **Relacional:** `Usuario` (1:N) `CasoRevisional`; `CasoRevisional` (1:N) `UploadDocumento` e `EventoAuditoria`.
- **JSONB:** DTOs complexos (`DadosContrato`, `ResultadoCalculo`) serializados com `@JdbcTypeCode(SqlTypes.JSON)` na tabela `cases` — evita explosão de tabelas.

## 💻 Arquitetura Front-end

- **Core:** React 19, React Router DOM v7 (CRA).
- **UI:** **Material UI (MUI) v7** — Cards, Grids, Tabelas obrigatoriamente via MUI. ⚠️ **Não subir para MUI v8/v9**: quebram o build do CRA/react-scripts (resolução ESM "fully specified" do `react-transition-group`). Fixado em `^7`.
- **Feedback:** **SweetAlert2** para confirmações/erros/sucesso/exclusão. Nunca `alert()` nativo. Wrappers em `src/services/alerts.js`.
- **HTTP:** Axios (`src/services/api.js`, `baseURL: /api`, dev via `proxy` no `package.json` → `:8080`). Interceptors anexam o JWT e tratam 401 (logout + redirect).
- **Arquivos:** PDFs via `Blob` + `<iframe src={blobUrl}>`.

## 🔑 Autenticação (implementada — padrão herdado do sigapsi.dev)

JWT stateless com Spring Security. Espelha o sigapsi e melhora o controle de sessão.

### Back-end (`security/`, `service/`, `controller/`)
- **Lib JWT:** `com.auth0:java-jwt` (HMAC256, issuer `revisional-api`, exp 2h). `TokenService`.
- **`Usuario implements UserDetails`** — login por **email OU cpf OU oab** (`UsuarioRepository.findByEmailOrCpfOrOab`). `getUsername()` = cpf (principal canônico).
- **Roles** (`UsuarioRole`, `@Enumerated(STRING)`): `ROLE_ADMIN`, `ROLE_AUDITOR`, `ROLE_VISUALIZADOR`. VISUALIZADOR só faz GET.
- **Sessão única:** coluna `token_ativo` — só o último token emitido vale (checado no `SecurityFilter`). Login com sessão ativa → `409`; reenviar com `?force=true`.
- **Lockout:** 5 tentativas falhas → `conta_bloqueada_ate` = +30min (`403`). Registra `data_ultimo_login` e `ultimo_ip`.
- **Endpoints** `POST/GET /api/auth`: `login` (`?force`), `register`, `logout`, `validate`.
- `SecurityConfigurations` stateless + CORS (`localhost:3000`) + `SecurityFilter` (OncePerRequest). `AuthorizationService` (UserDetailsService). `AdminUserSeeder` cria admin inicial (`app.admin.*`).
- **Segredo:** `JWT_SECRET` (prop `api.security.token.secret`). Migração `V2__auth_usuario.sql`.

### Front-end (`src/services/auth.js`, `api.js`; `src/componentes/login/Login.jsx`)
- Token em **cookie** `revisional_token` — **melhorado vs. sigapsi**: `SameSite=Strict` + `Secure` (em https) e `Max-Age` derivado do `exp` do JWT.
- **Validação client-side de expiração** (`isTokenValid`): logout proativo sem esperar o `401` do servidor. `isAuthenticated()` limpa token expirado.
- Helpers de papel a partir do payload: `isAdmin`, `isAuditor`, `isVisualizador`, `getNomeCompleto`.
- Tela de login trata `409` (diálogo SweetAlert → reenvia com `force`). Rotas protegidas em `App.js` (`RotaProtegida`/`RotaPublica`/`RotaAdmin`); `MainLayout` (AppBar + drawer + logout); `Dashboard` é a home.

## 📖 Glossário (usar em PT no código)

- **DadosContrato** — dados financeiros brutos extraídos do contrato.
- **ReferenciaMercado** — dados da API SGS 25471 do Banco Central.
- **ResultadoCalculo** — laudo processado (diferenças + conclusões).
- **LinhaPrice** — DTO de uma parcela (prestação, juros, amortização, saldo).
- **Bisseção** — algoritmo p/ achar taxa/CET ocultos.
- **Spread** — diferença percentual taxa banco vs. mercado.

## 🤖 Regras de código (obrigatórias)

1. **Null safety:** dados vêm de OCR → campos podem ser vazios. Usar wrappers (`Double`, `Integer`) nos DTOs. Validar `null` antes de qualquer operação matemática no Service.
2. **Sem números mágicos:** limites/multiplicadores/configs vêm de `ParametrosSistema` (`@Component`). Nunca chumbar `1.50` ou `180` no método.
3. **Isolamento:** Service recebe DTO, aplica fórmulas, retorna DTO. Não conhece banco nem HTTP.
4. **LGPD nos logs:** nunca logar `cliente_cpf` nem valores financeiros reais em exceções.
5. **Frontend estético:** componentes só com MUI v7; respostas de API (sucesso/falha) via SweetAlert.

## 🔐 Variáveis de Ambiente (`.env`)

| Variável | Propósito |
| --- | --- |
| `JWT_SECRET` | Assinatura dos tokens de sessão. |
| `SPRING_DATASOURCE_URL` | URL JDBC do PostgreSQL. |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco. |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco. |
| `BCB_API_TIMEOUT` | Timeout da consulta à API do Bacen. |
