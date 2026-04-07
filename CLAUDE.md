# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build
mvn clean package

# Run application (port 8080)
mvn spring-boot:run

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=AuctionServiceTest

# Build without tests
mvn clean package -DskipTests

# Run the JAR directly
java -jar target/ortb-api-1.0.0.jar
```

## Architecture

This is a Java 21 + Spring Boot 3.2.4 implementation of an **OpenRTB 2.6 real-time bidding auction engine**.

### Request Flow

```
POST /openrtb/bid (BidController)
    → AuctionService.process(BidRequest)
        ├── DspClientService.fetchExternalCandidates()   # parallel HTTP calls to DSPs
        ├── AdInventoryService.getAllAds()                # in-memory local inventory
        ├── TargetingService.isEligible()                 # per-impression eligibility filter
        └── returns BidResponse with winning bid(s)
```

### Key Services

- **`AuctionService`** — Orchestrates the auction. Merges local ads and external DSP bids, applies first-price (at=1) or second-price+$0.01 (at=2) pricing, returns one winner per impression.
- **`TargetingService`** — 10-stage eligibility pipeline: format, size, floor price, blocked categories/advertisers, country, region, device type, content categories, keywords.
- **`DspClientService`** — Parallel HTTP calls to external DSPs using Java 21 virtual threads (`Executors.newVirtualThreadPerTaskExecutor()`). Per-DSP timeout: 80ms. DSP failures are gracefully skipped.
- **`AdInventoryService`** — Thread-safe `ConcurrentHashMap` in-memory store. Sample ads loaded at startup by `AdInventoryConfig`.

### Models

- `model/openrtb/` — Standard IAB OpenRTB 2.6 domain objects (`BidRequest`, `BidResponse`, `Bid`, `Imp`, `Banner`, `Video`, `Device`, `Geo`, `Site`, `App`, `User`)
- `model/ad/` — Internal inventory models (`Ad`, `AdFormat`, `Targeting`)
- `model/auction/AuctionCandidate` — Normalized entry used internally to compare local ads vs. DSP bids

### Configuration

`src/main/resources/application.yml` controls:
- Server port (8080)
- Virtual threads (`spring.threads.virtual.enabled: true`)
- Auction timeout: 100ms
- DSP endpoints: three sample DSPs on `localhost:9901–9903`

DSP endpoint properties are bound via `DspProperties` and used in `DspClientConfig`.

## REST Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/openrtb/bid` | Main auction endpoint (OpenRTB BidRequest → BidResponse) |
| GET | `/openrtb/ads` | List all ads in inventory |
| GET | `/openrtb/ads/{id}` | Fetch a specific ad by ID |

## Testing

Tests live in `src/test/java/com/ortb/`:
- `controller/BidControllerIntegrationTest` — Full HTTP integration tests using `@SpringBootTest` + `@AutoConfigureMockMvc`
- `service/AuctionServiceTest` — Unit tests for auction logic (pricing, eligibility, DSP merge)
- `service/TargetingServiceTest` — Unit tests for the targeting/eligibility pipeline
- `service/DspClientServiceTest` — Unit tests for DSP call handling and fallback behavior

DSP HTTP calls are mocked in tests to avoid external dependencies.

## POM Version

Follow the version format: `<MAJOR>-SNAPSHOT-<BRANCH>` (e.g., `1.0.0.0-SNAPSHOT-ADS-0001`). Current version: `1.0.0`.
