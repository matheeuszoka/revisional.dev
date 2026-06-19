    # CLAUDE.md

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

Deploy futuro: GitHub Actions ao merge em `main`, VPS via Docker Compose + reverse proxy (Nginx/Apache) para SSL e roteamento (`/api`→backend, `/`→frontend).

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
- **UI:** **Material UI (MUI) v7** — Cards, Grids, Tabelas obrigatoriamente via MUI.
- **Feedback:** **SweetAlert2** para confirmações/erros/sucesso/exclusão. Nunca `alert()` nativo.
- **HTTP:** Axios com JWT do Local/SessionStorage.
- **Arquivos:** PDFs via `Blob` + `<iframe src={blobUrl}>`.

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
