# Trade Service — FinTech Platform

## Overview
The **Trade Service** provides a peer-to-peer (P2P) cryptocurrency/fiat exchange mechanism with an escrow system. Users can create trade offers (sell orders), and other users can accept them.

## Features
- Create, view, update, and cancel trade offers
- Accept trades (executes escrow transfers)
- Oracle integration to fetch live exchange rates and prevent significant deviations
- Commission processing to a platform wallet
- Uses Resilience4j circuit breakers for Ledger and Wallet service interactions

## Tech Stack
- Java 21 / Spring Boot 3.4.1
- PostgreSQL (JPA/Hibernate)
- MapStruct, Lombok
- OpenAPI (SpringDoc)
- Eureka Client

## Port
`8084`

## API Endpoints
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/trades` | Create a trade offer |
| GET | `/api/trades` | Browse open trades |
| GET | `/api/trades/my` | My trades |
| GET | `/api/trades/{id}` | Get trade details |
| POST | `/api/trades/{id}/accept` | Accept trade |
| PUT | `/api/trades/{id}` | Update an open trade |
| DELETE | `/api/trades/{id}` | Delete an open trade |
| GET | `/api/trades/oracle/rates` | Get live market rates from oracle API |

## Configuration
Copy `.env.example` to `.env` and fill in values before running.

```bash
cp .env.example .env
mvn spring-boot:run
```
