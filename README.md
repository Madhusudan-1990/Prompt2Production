# Prompt2Production — AI-first API Test Suite

> **Prompts in. 101 green tests out.** This repo tells one complete story: 15 LLM prompts -> a
> headless-verified Postman collection (100 requests / 238 assertions) -> a production-style
> **Java + RestAssured + TestNG** regression suite of **101 tests, green in ~60 seconds** —
> with every real API defect it uncovered pinned as a passing test instead of being hidden.

![tests](https://img.shields.io/badge/tests-101-brightgreen)
![classes](https://img.shields.io/badge/classes-15-blueviolet)
![assertions](https://img.shields.io/badge/assertions-238-blue)
![build](https://github.com/Madhusudan-1990/Prompt2Production/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-25-orange)
![RestAssured](https://img.shields.io/badge/RestAssured-5.5.1-blue)
![TestNG](https://img.shields.io/badge/TestNG-7.7-purple)
![Allure](https://img.shields.io/badge/Allure-reporting-lightgrey)

**The pipeline:**

```
 15 prompts         Postman + Newman           Java suite
 (test plan)  -->  238 assertions green   -->  15 classes / 101 tests
                                                   |
              +----------------+-------------------+------------------+
              v                v                                       v
     Jenkinsfile (4 envs)  GitHub Actions (ci.yml)            Allure report
     dev/qa/stage/prod      green check on every push
```

**Live demo in one command** (watch 101 tests go green):

```bash
mvn clean test -Dsurefire.suiteXmlFiles=src/test/resources/testrunners/testng_ecommerce_regression.xml
```

System under test: **https://ecommerce-api.fastapicloud.dev** (FastAPI, no auth, no delete
endpoints, shared public demo — every created test data is tagged `tc-`).

---


## 1. Prerequisites

| Tool | Version |
|---|---|
| JDK | 11+ (verified on 25) |
| Maven | 3.6+ (`mvn -version`) |
| Network | outbound HTTPS to `ecommerce-api.fastapicloud.dev` |

## 2. How to run the tests

```bash
# full suite (default env = prod)
mvn clean test -Dsurefire.suiteXmlFiles=src/test/resources/testrunners/testng_ecommerce_regression.xml

# choose environment: dev | qa | stage | uat | prod
mvn clean test -Dsurefire.suiteXmlFiles=src/test/resources/testrunners/testng_ecommerce_regression.xml -Denv=qa

# single test class
mvn test -Dtest=OrderManagementTest -Dsurefire.suiteXmlFiles=src/test/resources/testrunners/testng_ecommerce_regression.xml
```

Output goes to the console, `target/surefire-reports/` (TestNG XML/TXT) and `allure-results/`.

> CI: `Jenkinsfile` runs the same command in 4 stages (dev / qa / stage / prod), and .github/workflows/ci.yml runs it on every push with a green-check badge.

## 3. How the suite is organised

```
src/
├── main/java/com/qa/api/
│   ├── client/RestClient.java          # HTTP layer
│   ├── base/ (see test)                # BaseTest lives under src/test
│   ├── constants/                      # AuthType, StatusCode, AppConstants
│   ├── manager/ConfigManager.java      # loads config_<env>.properties
│   ├── mocking/                        # WireMock setup (infrastructure, no tests use it now)
│   ├── pojo/                           # Lombok POJOs
│   └── utils/                          # JsonPath/Excel/Schema helpers
└── test/
    ├── java/com/qa/api/
    │   ├── base/BaseTest.java
    │   └── ecommerce/
    │       ├── base/ECommerceBaseTest.java
    │       └── tests/*.java            # 15 test classes = 15 folders of the test plan
    └── resources/
        ├── config_{dev,qa,stage,uat,prod}.properties
        └── testrunners/testng_ecommerce_regression.xml
```

Tests never build their own HTTP calls: they extend `ECommerceBaseTest`, which provides
`get/post/put/patch/delete/call(...)` plus FastAPI contract helpers
(`assertDetailArray`, `assertMethodNotAllowed`, `assertNotFound`, `detailPaths`) and
**lazy fixtures** `ensureProductId() / ensureUserId() / ensureOrderId()` that create
`tc-` tagged data once per run and flow it forward between tests.

## 4. File-by-file description

### Test classes — `src/test/java/com/qa/api/ecommerce/tests/`

| File | What it verifies |
|---|---|
| `AuthenticationAccessTest` | No auth anywhere: endpoints reachable without credentials, bearer token ignored, `openapi.json` has no securitySchemes, `/internal/health`, `/admin/stats`, `/debug/db` are public |
| `ProductEndpointsTest` | `GET/POST /products`, `GET/PUT /products/{id}`, `PATCH .../select`; `PUT` ignores `selected` (FINDING); `PATCH/DELETE` on the item route → 405; 422 validation |
| `UserEndpointsTest` | `GET/POST /users` only; `GET /users/{id}` → 404, `PUT/PATCH/DELETE` → 405; invalid email accepted (FINDING) |
| `OrderManagementTest` | `GET/POST /orders`, response shapes, created order persists; `total_cost` uses list index (off-by-one FINDING); single-order routes → 404/405 |
| `PaymentGapTest` | `/payments` not implemented: GET → 404, POST/PATCH → 405 |
| `ShoppingCartGapTest` | `/cart` not implemented: GET → 404, POST/PUT/DELETE → 405 |
| `InventoryGapTest` | `/inventory` not implemented; stock lives on the product as an integer `stock` field |
| `SearchGapTest` | `/products/search` and `/products/filter` are swallowed by `/products/{id}` → 422 `int_parsing`; search params silently ignored |
| `ErrorHandlingTest` | Error contract: 404/405/422 shapes, malformed JSON → 422 `json_invalid` (not 400), `/purchases` query validation, no stack traces leak |
| `CorsOptionsTest` | Preflight rules (200 only with `Origin` + `Access-Control-Request-Method`, else 405), ACAO/ACAM/ACAC/max-age/ACAH behaviour, CORS answers unknown paths too |
| `PaginationGapTest` | `limit/page/skip/take` ignored — full list always returned, no envelope |
| `ValidationTest` | Type-level validation only; empty name / negative price / negative stock accepted (FINDING); out-of-range product id in an order → **500** (BUG); extra body fields ignored |
| `SortingGapTest` | `sort/order_by/direction` ignored — order identical to unsorted list |
| `StatusCategoryFilterTest` | `status`/`category` filters ignored; `/payments?status=` → 404 |
| `RateLimitPerformanceTest` | Response times < 1s, 12-call burst never returns 429, no rate-limit headers, `HEAD` → 404 despite being advertised in CORS |

### Support classes

| File | Purpose |
|---|---|
| `src/test/java/com/qa/api/ecommerce/base/ECommerceBaseTest.java` | Base class for all e-commerce tests: request helpers, FastAPI assertion helpers, `tc-` name generators, lazy `ensure*` fixtures, transient-503 retry |
| `src/test/java/com/qa/api/base/BaseTest.java` | Reads `baseurl.ecommerce` from config, creates the `RestClient`, registers the Allure filter |
| `src/main/java/com/qa/api/client/RestClient.java` | HTTP client. `execute(...)` is the **spec-free** method used by this suite (the older `get/post/put/...` methods hard-code allowed status codes like 200/201/204 and would fail on 405/422/500 tests) |
| `src/main/java/com/qa/api/manager/ConfigManager.java` | Static loader for `config_<env>.properties`; env chosen by `-Denv=` (default `prod`) |
| `src/main/java/com/qa/api/constants/` | `AuthType` (tests use `NO_AUTH`), `StatusCode`, `AppConstants` |
| `src/main/java/com/qa/api/utils/`, `pojo/`, `mocking/` | Framework utilities (JSON/Excel/schema helpers, Lombok POJOs, WireMock setup) — infrastructure kept for future suites |

### Resources & config

| File | Purpose |
|---|---|
| `src/test/resources/testrunners/testng_ecommerce_regression.xml` | The suite: 15 `<test>` blocks, one per folder — **point surefire/Jenkins here** |
| `src/test/resources/config_<env>.properties` | Per-environment config. Currently only `baseurl.ecommerce` |
| `pom.xml` | RestAssured 5.5.1, TestNG 7.7, Allure, Lombok, WireMock, POI. Also pins `maven-compiler-plugin` 3.13 with `proc=full` — **required on JDK 23+**, otherwise Lombok getters are not generated and compilation fails |
| `Jenkinsfile` | 4 stages (dev/qa/stage/prod) running the e-commerce suite |
| `SKILL.md` | Agent-facing skill: run commands, conventions, findings, gotchas |

## 5. Conventions worth knowing

1. **`tc-` prefix on all created data** — the API has no auth and no DELETE endpoints, so
   test data can never be cleaned up; the prefix keeps it identifiable on the shared demo.
2. **Findings are asserted as-is.** Known API defects are encoded as passing tests named/marked
   `FINDING:` or `BUG:`. They document real behaviour — don't "fix" them by weakening asserts.
3. **The test owns the expected status code** → always use `call(...)`/`get(...)`/`post(...)`
   from `ECommerceBaseTest`, never the spec-bound `restClient.get(...)`.
4. **Path params:** embed the id *or* pass the `{id}` template + `path("id", id)` — never both.
5. **Fixture order:** where state matters (PUT before PATCH select) tests use `dependsOnMethods`.

## 6. Adding a new test (recipe)

```java
public class MyNewTest extends ECommerceBaseTest {
    @Test
    public void someBehaviour() {
        Response response = call("GET", ECOMMERCE_PRODUCTS_ENDPOINT, null, null, null,
                                 query("limit", "1"));            // request
        Assert.assertEquals(response.statusCode(), 200);          // assert status yourself
        Assert.assertNotNull(response.jsonPath().getList("$"));   // assert body
    }
}
```
Then add the class to `testrunners/testng_ecommerce_regression.xml` and re-run the suite.

## 7. Real API defects found & pinned

These are not test bugs — they are **API bugs this suite discovered and documented**. Each one
is a passing test that asserts the *current* behaviour (tagged `FINDING:` / `BUG:`), so a future
API fix flips the test on purpose instead of silently changing coverage.

| # | Defect | What the test does |
|---|---|---|
| 1 | `POST /orders` uses the product **list index** as id -> off-by-one `total_cost` | `OrderManagementTest` asserts the exact (wrong) cost |
| 2 | Out-of-range product id in an order -> unhandled **500 plain-text** | `ValidationTest` asserts 500 + `Content-Type: text/plain` |
| 3 | `PUT /products/{id}` **ignores** the `selected` field | `ProductEndpointsTest` asserts `selected` stays unchanged |
| 4 | **No email validation** — `not-an-email` is accepted and persists | `UserEndpointsTest` asserts 200 + persistence |
| 5 | Empty `name`, negative `price`, negative `stock` all accepted | `ValidationTest` asserts 200 on each |
| 6 | Pagination / sorting / filters — **every query param ignored**, no envelope | `PaginationGapTest`, `SortingGapTest`, `StatusCategoryFilterTest` |
| 7 | `/products/search` + `/products/filter` swallowed by `/{id}` route -> **422 `int_parsing`** | `SearchGapTest` asserts the 422 contract |
| 8 | `/admin/stats` and `/debug/db` are **public** (no auth anywhere) | `AuthenticationAccessTest` asserts 200 without credentials |
| 9 | `HEAD` advertised via CORS but returns **404**; no rate-limit headers, **429 never returned** | `RateLimitPerformanceTest` |

## 8. Design decisions & highlights

The choices that shape this suite, and why they were made:

1. **A measurable story** — 15 prompts -> 238 Newman assertions -> 101 Java tests, green in
   ~60s, verified on two environments. The Postman collection and this suite are two artefacts of
   the same plan; they can be diffed folder-for-folder.
2. **Findings-first testing** — 9 real API defects were found and pinned as passing tests
   (`FINDING:` / `BUG:`) instead of weakening assertions. A red test on a bug you do not own is
   noise; a green test documenting the bug is a contract.
3. **Contract-driven design** — the legacy `RestClient` methods hard-code allowed status codes;
   this suite uses a spec-free `execute()` so 405/422/500 are assertable, with helpers that check
   FastAPI's exact 404/405/422 body shapes.
4. **Shared-environment hygiene** — the demo API has no auth, no DELETE, and shared state, so all
   data is `tc-` tagged, fixtures are created once per run and flow forward via
   `dependsOnMethods`, and transient 503s (the host restarts) get a 3-attempt retry.
5. **CI/CD** — a 4-stage Jenkins pipeline (Windows agent, env selection, UNSTABLE-vs-fail
   policy) plus GitHub Actions running the same command on every push (`.github/workflows/ci.yml`).
6. **Build sanity on modern JDKs** — the build pins `maven-compiler-plugin 3.13` with
   `<proc>full</proc>` because JDK 23+ disables annotation processing by default and Lombok getters
   silently vanish; the error then points nowhere near Lombok.
7. **AI-first and transparent about it** — the whole workflow (prompts used, how it was built)
   is documented in `SKILL.md` §31, including 9 reusable prompts for maintainers.

**Expected output** of the command in §2:

```
Tests run: 101, Failures: 0, Errors: 0, Skipped: 0
```

Every `FINDING:` test stays green by design — it documents current API behaviour and fails the
day that behaviour changes.

## 9. AI-assisted development & prompts

This repository was created with AI assistance, end to end:

1. **Test plan** — 15 numbered **OpenAI (ChatGPT) prompts** produced the test areas and cases
   (evidence: request descriptions in the original Postman collection say *"Prompt 1 assumed
   PATCH support…"*, *"Prompt 3 assumed GET /users/{id}…"*).
2. **Postman collection** — generated from the plan (100 requests / 238 assertions) and verified
   headlessly with **Newman** against the live API until green.
3. **This Java suite** — ported to RestAssured + TestNG by an **AI coding agent (opencode CLI)**,
   iterated with Maven until 101/101 passed on `-Denv=prod` and `-Denv=qa`.
4. **Cleanup & docs** — removal of the unrelated sample suites and the writing of `README.md`
   and `SKILL.md` were also agent-assisted; a human reviewer owned the decisions and the repo.

**Full details, the exact session prompts, and 9 reusable prompts for working on this repo**
(port a suite, add endpoint tests, triage a red run, pin a new API defect, refresh docs, …)
live in **`SKILL.md` §31**.
