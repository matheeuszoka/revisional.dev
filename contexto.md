Este arquivo fornece as diretrizes arquiteturais, regras de negócio e stack tecnológica para o Claude Code (ou qualquer IA) trabalhar neste repositório.

## 🏢 O Projeto

**Sistema Revisional Bancário Web** — Plataforma SaaS B2B voltada para advogados e auditores jurídicos.
O sistema automatiza a auditoria de contratos de financiamento bancário (foco em veículos PF). Ele realiza extração de dados via OCR, usa métodos numéricos para descobrir taxas de juros ocultas (engenharia reversa), compara com o Banco Central (BCB) e gera laudos técnicos/jurídicos em PDF.

**Stack:** Spring Boot API (Java 17+) + React SPA + PostgreSQL.

---

## 🚀 Comandos Rápidos

### Infraestrutura (Rodar primeiro)

```bash
cd docker
docker compose up -d

```

Inicia o banco de dados **PostgreSQL**, **pgAdmin** e serviços auxiliares (como **RabbitMQ/Redis** para processamento assíncrono de OCR, se configurado).

### Back-end (API Spring Boot)

```bash
cd backend
./mvnw clean package          # Compila o projeto
./mvnw spring-boot:run        # Roda em ambiente de desenvolvimento (porta 8080)
./mvnw test                   # Roda a suíte de testes matemáticos

```

### Front-end (React SPA)

```bash
cd frontend
npm install
npm start                     # Roda o dev server proxyando /api para localhost:8080

```

---

## ☁️ Implantação (Deployment) e Branches

O fluxo de trabalho segue o modelo de Pull Requests com branches protegidas:

* **`develop`**: Branch de trabalho e integração.
* **`main`**: Branch de produção (release).

O deploy em produção é automatizado via GitHub Actions (`deploy.yml`) ao realizar o merge para a branch `main`. A aplicação roda em uma VPS via Docker Compose, utilizando um *Reverse Proxy* (Nginx/Apache) para gerenciar certificados SSL e roteamento (`/api` para o backend, `/` para o frontend).

---

## 🏗️ Arquitetura Back-end (`backend/`)

Baseado no Spring Boot 3+, utilizando **Clean Architecture** e separação rigorosa em camadas.
Pacote base: `br.com.revisional`.

### Layout de Pacotes

* `controller/` → Endpoints REST.
* `service/` → Motores de lógica isolada (Cálculos, Extração OCR, Integração BCB, Geração de PDFs).
* `model/entity/` → Entidades JPA (ex: `Usuario`, `CasoRevisional`). Mapeiam o banco relacional.
* `model/dto/` → Objetos de Valor e Composições salvas como **JSONB** no banco (ex: `DadosContrato`, `ResultadoCalculo`).
* `repository/` → Spring Data JPA.
* `config/` → Configurações globais e a classe Singleton `ParametrosSistema` (que evita *magic numbers*).
* `security/` → Configurações do Spring Security, filtros JWT e senhas criptografadas via PBKDF2/BCrypt.

### Motores e Fórmulas Matemáticas (O Core do Negócio)

A classe `CalculadoraFinanceiraService` não utiliza bibliotecas prontas de terceiros para garantir a precisão pericial e emissão de memória de cálculo.

1. **Conversão de Taxas:** $CET_{anual} = (1 + Taxa_{mensal})^{12} - 1$
2. **Fórmula PRICE (PMT):** $PMT = PV \times \frac{i}{1 - (1+i)^{-n}}$
3. **Engenharia Reversa (Bisseção):** Caso o contrato omita a taxa, o sistema usa o Método de Bisseção (com limites de iteração definidos em `ParametrosSistema`) iterando entre `0.0` e `1.0` até encontrar a taxa exata que bate com o valor da parcela.
4. **Cálculo de CET (XIRR/VPL):** Utiliza fluxo de caixa em dias corridos para zerar o Valor Presente Líquido (VPL) e achar o Custo Efetivo Total anual.
5. **Score de Risco (Spread):** Razão $R = \frac{Taxa_{Contrato}}{Taxa_{Mercado}}$. Se $\ge 2.0$ (Indício Forte); se $\ge 1.50$ (Indício Moderado).

### Modelagem de Dados

* **Relacional (`@Entity`):** `Usuario` (1:N) `CasoRevisional`. `CasoRevisional` (1:N) `UploadDocumento` e `EventoAuditoria`.
* **JSONB:** Estruturas complexas financeiras (DTOs como `DadosContrato` e `ResultadoCalculo`) são serializadas com `@JdbcTypeCode(SqlTypes.JSON)` dentro da tabela `cases` para evitar explosão de tabelas relacionais.

---

## 💻 Arquitetura Front-end (`frontend/`)

* **Core:** React 19, React Router DOM v7.
* **UI & Estilização:** **Material UI (MUI) v7**. A construção de layouts (Cards, Grids, Tabelas) deve obrigatoriamente seguir os componentes e o sistema de design do Material UI.
* **Feedback & Alertas:** **SweetAlert2** (`sweetalert2` / `sweetalert2-react-content`) para modais de confirmação bonitos, alertas de erro, sucesso e exclusão de casos. Evitar o `alert()` nativo do navegador.
* **Comunicação:** Axios (para chamadas HTTP consumindo JWT do LocalStorage/SessionStorage).
* **Tratamento de Arquivos:** Renderização de PDFs através de `Blob` + `<iframe src={blobUrl}>` nativo ou componentes visuais para os artefatos legais.

---

## 📖 Glossário de Domínio (Ubiquitous Language)

Sempre utilize estes termos em português no código:

* **DadosContrato:** Dados financeiros brutos extraídos do contrato.
* **ReferenciaMercado:** Os dados capturados da API SGS 25471 do Banco Central.
* **ResultadoCalculo:** O laudo processado com diferenças matemáticas e conclusões.
* **LinhaPrice:** Um `Record` ou DTO representando uma única parcela (prestação, juros, amortização, saldo).
* **Bisseção:** Algoritmo matemático para achar taxa de juros ou CET ocultos.
* **Spread:** Diferença percentual entre a taxa do banco e a taxa do mercado.

---

## 🤖 Regras Direcionais para a IA (Diretrizes de Código)

1. **Segurança de Nulos (Null Safety):** Os dados de origem vêm de arquivos via OCR. Campos *sempre* podem vir vazios. **Obrigatoriamente** utilize classes Wrapper (`Double`, `Integer`) nos modelos DTO. Valide `null` antes de qualquer operação matemática no `Service`.
2. **Sem Números Mágicos (Hardcoding):** Qualquer limite matemático, multiplicador de risco ou configuração de domínio deve ser puxado da classe injetável `ParametrosSistema` (`@Component`). Nunca chumbe `1.50` ou `180` iterações soltas nos métodos.
3. **Isolamento Total:** A camada `Service` recebe os DTOs, aplica as fórmulas matemáticas rigorosas e devolve o DTO de Resultado. Ela não deve conhecer banco de dados ou requisições HTTP do *Controller*.
4. **LGPD e Logs:** Nunca expor `cliente_cpf` ou valores financeiros reais nos logs do sistema em caso de exceções (`log.error`).
5. **Frontend Estético:** Sempre que for criar ou refatorar componentes visuais em React, empacote botões, inputs e tabelas utilizando exclusivamente os componentes do **MUI v7** e trate as respostas da API (sucesso/falha) disparando pop-ups do **SweetAlert**.

## 🔐 Variáveis de Ambiente Essenciais ( `.env` )

| Variável | Propósito |
| --- | --- |
| `JWT_SECRET` | Chave de assinatura para os tokens de sessão. |
| `SPRING_DATASOURCE_URL` | URL de conexão JDBC com o PostgreSQL. |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco de dados. |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco de dados. |
| `BCB_API_TIMEOUT` | Tempo máximo de espera para consulta na API do Bacen. |


stacks: Tecnologia
Java 17 (LTS)
Spring Boot 3.3.x
Spring MVC + react com Material UI v7, SweetAlert2
Spring Data JPA + PostgreSQL
Flyway
Spring Security (form login, CSRF nativo, BCrypt)
Apache PDFBox / OpenPDF
Apache PDFBox + Tess4J
Spring RestClient
Bean Validation (jakarta.validation)
JUnit 5 + Mockito + Testcontainers
Maven
Docker (multi-stage) + docker-compose
