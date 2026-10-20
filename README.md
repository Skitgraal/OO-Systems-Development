# Portfolio Desk

A small trading app built with microservices: a **REST** web service, a **gRPC** server and two databases.
Users create portfolios, buy and sell Paris-listed stocks at (simulated) live prices, and follow their P&L.

```
Vue.js  --REST-->  portfolioService  --gRPC-->  quoteServer
                         |                          |
                    PostgreSQL                     H2
```

Full architecture documentation: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

## Project structure

| Folder | Content |
|---|---|
| `quoteApi/` | gRPC contract (`quote_service.proto`) shared by client and server |
| `quoteServer/` | gRPC server: market data simulation (port 9090) |
| `portfolioService/` | REST API: portfolios, trading, valuation (port 8080) |
| `portfolio-frontend/` | Vue.js 3 web client |
| `docs/` | Architecture documentation |

## Requirements

- JDK 17 or newer
- Node.js 18+ (frontend only)
- Docker (optional, for PostgreSQL or the full stack)

## Run on Windows in one click

Double-click **`start.bat`**. It builds the services, starts the gRPC server, the REST service
(in-memory H2 database) and the frontend, then opens the browser. If a default port
(9090, 8080, 5173) is already used, the next free one is taken. Press a key or close the
window to stop everything (`stop.bat` also stops a run left behind).
Logs go to `%TEMP%\portfolio-desk-logs`.

When the project sits in a OneDrive folder, Gradle builds into
`%LOCALAPPDATA%\finance-portfolio-build` because OneDrive locks files under `build/`.

## Run with Docker (everything)

```bash
docker compose up --build
```

Open http://localhost:3000

## Run locally (development)

```bash
# 1. gRPC quote server
./gradlew :quoteServer:bootRun

# 2. REST service, with an in-memory H2 database (no PostgreSQL needed)
./gradlew :portfolioService:bootRun --args='--spring.profiles.active=dev'
#    ...or with PostgreSQL:
#    docker compose up -d postgres
#    ./gradlew :portfolioService:bootRun

# 3. Frontend
cd portfolio-frontend
npm install
npm run dev        # http://localhost:5173
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Try the API with curl

```bash
# Market data (REST -> gRPC)
curl http://localhost:8080/api/instruments
curl http://localhost:8080/api/quotes/BNP.PA

# Create a portfolio with 10,000 EUR
curl -X POST http://localhost:8080/api/portfolios \
  -H "Content-Type: application/json" \
  -d '{"name":"Main","owner":"Sacha","initialCash":10000}'

# Buy then sell
curl -X POST http://localhost:8080/api/portfolios/1/buy \
  -H "Content-Type: application/json" -d '{"symbol":"BNP.PA","quantity":20}'
curl -X POST http://localhost:8080/api/portfolios/1/sell \
  -H "Content-Type: application/json" -d '{"symbol":"BNP.PA","quantity":5}'

# Valuation and history
curl http://localhost:8080/api/portfolios/1
curl http://localhost:8080/api/portfolios/1/transactions

# Error examples
curl http://localhost:8080/api/portfolios/999          # 404
curl http://localhost:8080/api/quotes/UNKNOWN          # 404
curl -X POST http://localhost:8080/api/portfolios/1/buy \
  -H "Content-Type: application/json" -d '{"symbol":"MC.PA","quantity":1000}'   # 422
```

Call the gRPC server directly (needs [grpcurl](https://github.com/fullstorydev/grpcurl)):

```bash
grpcurl -plaintext localhost:9090 list
grpcurl -plaintext -d '{"symbol":"AIR.PA"}' localhost:9090 quote.QuoteService/GetQuote
```

## Tests

```bash
./gradlew test
```

## Tech stack

Java 17+, Spring Boot 3.4, Spring Data JPA, gRPC 1.68 / Protobuf 3, PostgreSQL 16, H2, Vue 3, Vite, Docker Compose.
