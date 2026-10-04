# skill.md — Prompt2Production

> **Basis of this document:** a static read of every source file in the repository, plus **runtime verification** (the full suite was executed twice against the live API: `Tests run: 101, Failures: 0, Errors: 0, Skipped: 0`, ~60s per run, `-Denv=prod` and `-Denv=qa`). Statements about the API are evidenced by those runs; statements about unused code are evidenced by grep. Where something could not be verified it is marked **"Not found in the repository."**

---

# 1. Project Identity

| Item | Finding |
|---|---|
| Repository name | `Prompt2Production` (GitHub: `Madhusudan-1990`, was `RestAssuredAPIAutomationFramework`) |
| Maven coordinates | `groupId`/`artifactId`: `RestAssuredAPITestingFramework`, version `0.0.1-SNAPSHOT` (differs from repo name) |
| Purpose | REST API test automation for the public **E-Commerce API** demo at `https://ecommerce-api.fastapicloud.dev` (FastAPI, OpenAPI spec at `/openapi.json`, Swagger UI at `/docs`) |
| Framework type | RestAssured + TestNG + Maven, config-driven (`config_<env>.properties`), base-class inheritance, Allure reporting. Originally a multi-service learning framework (GoRest, contacts, Spotify, WireMock…); **all unrelated suites were removed** and only the e-commerce regression suite remains |
| Application areas automated | Products, Users, Orders, Payments/Cart/Inventory (gap documentation), Error handling, CORS/OPTIONS, Pagination/Sorting/Filtering (gap documentation), Validation, Rate-limiting/Performance |
| Primary goals (evidenced) | Regression suite run via Maven/Jenkins, multi-environment config, evidence-of-absence testing (404/405/ignored params), documented API findings encoded as passing tests |
| Maturity | Working and green (101/101 verified), but the underlying API itself is a demo with real defects that the suite deliberately pins (see §2). Framework hygiene issues listed in §26 |

---

# 2. Application Under Test (API contract + known findings)

No auth anywhere (OpenAPI has **no** `securitySchemes`, no bearer/JWT; auth headers are ignored).

**Routes that exist:** `GET/POST /products`, `GET/PUT /products/{id}`, `PATCH /products/{id}/select`, `GET/POST /users`, `GET/POST /orders`, `GET /openapi.json`, `/docs`, `GET /internal/health`, `GET /admin/stats`, `GET /debug/db`, `GET /purchases?user_id=` (required query param), CORS middleware answers `OPTIONS` on any path.

**Routes that do not:** `GET /users/{id}` → 404; `PUT/PATCH/DELETE /users/{id}` → 405; `GET /orders/{id}` → 404; `PUT/PATCH/DELETE /orders/{id}` → 405; `PATCH/DELETE /products/{id}` → 405 (but **PUT is implemented though undocumented** in the spec); `/payments`, `/cart`, `/cart/items`, `/inventory` → GET 404, writes 405; `HEAD /products` → 404 (even though CORS advertises HEAD).

**Error contract (FastAPI/Pydantic):**

| Status | Body |
|---|---|
| 404 | `{"detail": "<string>"}` (products: `"Product not found"`) |
| 405 | `{"detail": "Method Not Allowed"}` |
| 422 | `{"detail": [ {loc, msg, type, input} ]}` — `loc` is dotted as `body.name`, `path.product_id`, `query.user_id` |
| Malformed JSON | 422 `json_invalid` (**not** 400) |
| Creates | **200**, never 201 |

**Known API defects pinned as tests (do not "fix" these assertions):**

1. `POST /orders` indexes the product list with the raw product id → wrong `total_cost` (off-by-one), and an id ≥ product count → **500 Internal Server Error** (plain text body).
2. `PUT /products/{id}` silently ignores `selected`; only `PATCH /products/{id}/select` changes it.
3. No format/range/length validation: `name=""`, `price=-999.99`, `stock=-5`, `email="not-an-email"` are all accepted (200).
4. **Every list query param is ignored** — pagination (`limit/page/skip/take`), sorting (`sort/order_by/direction`), filtering (`status/category/inStock`), search (`q/minPrice/maxPrice`); unknown params are never validated or typed.
5. `/products/search` and `/products/filter` are captured by `/products/{id}` → 422 `int_parsing` (`input: "search"`).
6. No rate limiting: 12-call burst → no 429, no `X-RateLimit-*`/`Retry-After` headers.
7. CORS: preflight 200 **only** when both `Origin` + `Access-Control-Request-Method` present (else 405, even on unknown paths); ACAO reflects origin, ACAM lists GET/POST/PUT/PATCH/DELETE/HEAD/OPTIONS, ACAC `true`, max-age `600`, ACAH echoes requested headers.
8. No auth: `/admin/stats` and `/debug/db` are publicly reachable (access-control finding).
9. **No DELETE endpoints anywhere** — test data can never be cleaned up; server is a shared public demo (17+ products, 16+ users as of writing, mostly `tc-` rows from prior runs).

---

# 3. Technology Stack

Only items found in `pom.xml` / source / `Jenkinsfile`.

| Area | Technology | Version / evidence |
|---|---|---|
| Language | Java | `maven.compiler.release = 11`; **verified to build and run on JDK 25** (Temurin) |
| Build | Maven | **Not pinned** — no `mvnw`/`.mvn` wrapper in repo. Verified with Apache Maven 3.9.9 |
| Compiler plugin | maven-compiler-plugin | **3.13.0** with `<proc>full</proc>` + Lombok on `annotationProcessorPaths` — required on JDK 23+ (annotation processing is otherwise disabled and Lombok accessors silently disappear) |
| Test runner plugin | maven-surefire-plugin | **3.2.5**; suite selected at runtime via `-Dsurefire.suiteXmlFiles=…` (no default `suiteXmlFiles` in pom) |
| HTTP client | rest-assured | **5.5.1** |
| Test framework | TestNG | **7.7.0** (`scope=test`) |
| JSON | jackson-databind | 2.21.1; `jackson-dataformat-xml` 2.19.0 |
| JSON path | jayway json-path | 3.0.0 |
| JSON schema | rest-assured `json-schema-validator` | 5.5.6 (utility `SchemaValidator` exists; **not used by the current suite**) |
| Boilerplate | Lombok | 1.18.42 (`@Data/@Builder` on POJOs; needs the compiler-plugin APT config above) |
| Reporting | Allure | `allure-testng` 2.29.1, `allure-rest-assured` 2.19.0 (filter registered in `BaseTest`) |
| Reporting | chaintest-testng | 1.0.12 — declared; listener only referenced in the deleted regression suite and a commented line in `BaseTest` |
| Excel | Apache POI | `poi`/`poi-ooxml`/`poi-scratchpad` 3.9 + `ooxml4j` — **leftover from the removed Excel-driven tests** (`ExcelUtil` unused) |
| Mocking | WireMock | 3.13.0 (`WireMockSetup`/`APIMocks` exist; **no tests start it anymore** — `BaseTest` no longer boots port 8089) |
| CI/CD | Jenkins + GitHub Actions | `Jenkinsfile` (declarative, uses `bat` → **Windows agent**, 4 env stages), `.github/workflows/ci.yml` (runs the suite on push, JDK 25, green badge) |
| Not present | Maven wrapper, Docker, DB/JDBC, Cucumber/BDD, REST-assured mock (WireMock unused) | **Not found in the repository.** |

---

# 4. Repository Structure

```
Prompt2Production/
├── README.md                      # newcomer guide: how to run + file-by-file map
├── SKILL.md                       # this document
├── Jenkinsfile                    # 4 real stages (dev/qa/stage/prod), all → e-commerce suite
├── .github/workflows/ci.yml       # GitHub Actions: 101-test suite on every push (JDK 25)
├── pom.xml                        # deps + compiler/surefire plugin config
├── .gitignore                     # /target/, allure-results/, test-output/
└── src/
    ├── main/java/com/qa/api/
    │   ├── client/RestClient.java          # HTTP layer (spec-bound + spec-free execute())
    │   ├── constants/{AuthType,StatusCode,AppConstants}.java
    │   ├── errors/APIError.java
    │   ├── exceptions/APIException.java    # RuntimeException for invalid auth type
    │   ├── manager/ConfigManager.java      # static config loader (-Denv=)
    │   ├── mocking/{WireMockSetup,APIMocks}.java   # WireMock infra (unused by tests)
    │   ├── pojo/{Product,User,ContactsCredentials}.java
    │   └── utils/{JsonUtils,JsonPathUtil,JsonPathValidatorUtil,JsonUtils,SchemaValidator,
    │              ExcelUtil,CSVReader,NumberUtils,StringUtils,XmlPathUtil,ObjectMapperUtil}.java
    └── test/
        ├── java/com/qa/api/
        │   ├── base/BaseTest.java                 # RestClient + base URL + Allure filter
        │   └── ecommerce/
        │       ├── base/ECommerceBaseTest.java    # request helpers, contract asserts, fixtures
        │       └── tests/  (15 classes, 101 @Test methods)
        └── resources/
            ├── config_{dev,qa,stage,uat,prod}.properties   # each: baseurl.ecommerce=...
            ├── testrunners/testng_ecommerce_regression.xml # THE suite
            ├── __files/mockuser.json               # WireMock body (leftover)
            └── chaintest.properties                # chaintest listener config (leftover)
```

Test classes (all in `com.qa.api.ecommerce.tests`, @Test counts in brackets):
`AuthenticationAccessTest` [6], `ProductEndpointsTest` [10], `UserEndpointsTest` [8],
`OrderManagementTest` [10], `PaymentGapTest` [4], `ShoppingCartGapTest` [5],
`InventoryGapTest` [5], `SearchGapTest` [4], `ErrorHandlingTest` [10], `CorsOptionsTest` [10],
`PaginationGapTest` [6], `ValidationTest` [7], `SortingGapTest` [4],
`StatusCategoryFilterTest` [5], `RateLimitPerformanceTest` [7] → **101**.

---

# 5. Framework Architecture

Layered, inheritance-based (no DI, no interface abstraction):

```
config_<env>.properties ──▶ ConfigManager (static block, -Denv=)
                                    │
                                    ▼
BaseTest ── @BeforeTest: RestAssured.filters(AllureRestAssured),
           BASE_URL_ECOMMERCE = ConfigManager.get("baseurl.ecommerce"),
           restClient = new RestClient()
                                    │
                                    ▼
RestClient ── setupRequest(baseUri, AuthType, ContentType) + applyParams()
              ├─ spec-bound: get/post/put/patch/delete (fixed status codes 200/201/204/404…)
              └─ execute(method, …, headers, …)  ← NO spec; used by the whole suite
                                    │
                                    ▼
ECommerceBaseTest ── get/post/put/patch/delete/call() wrap execute() with AuthType.NO_AUTH,
                     503 retry, tc- generators, ensure* fixtures,
                     assertDetailArray/assertMethodNotAllowed/assertNotFound/detailPaths
                                    │
                                    ▼
15 test classes ── org.testng.Assert + @Epic/@Story/@Severity/@Description
                                    │
                                    ▼
testng_ecommerce_regression.xml ── 15 <test> blocks, verbose=2, no listeners, no parallel
```

Design rule of the suite: **the test owns the expected status code**, because the API legitimately returns 405/422/500 and `RestClient`'s spec-bound methods would throw before any assertion runs.

---

# 6. Execution Flow (real)

```
mvn clean test -Dsurefire.suiteXmlFiles=src/test/resources/testrunners/testng_ecommerce_regression.xml -Denv=prod
  └─ surefire 3.2.5 picks the suite
      └─ TestNG runs 15 <test> blocks sequentially
          ├─ per <test>: @BeforeTest → initSetup() (Allure filter + base URL), setup() (RestClient)
          ├─ test methods run in declaration order within a class;
          │  stateful flows chained with dependsOnMethods (create → GET → PUT → PATCH select → DELETE-405)
          ├─ first test needing shared data calls ensureProductId()/ensureUserId()/ensureOrderId()
          │  → lazily POSTs a tc- resource ONCE (synchronized, static fields) and reuses the id
          ├─ every request goes through ECommerceBaseTest.call() → RestClient.execute()
          │  → 503 responses retried up to 3× with 1s/2s backoff (shared demo restarts)
          └─ @AfterTest → stopMockServer() (no-op; WireMock is no longer started)
  Result: Tests run: 101, Failures: 0, Errors: 0, Skipped: 0  (~60 s, ~55–60 HTTP calls + retries)
```

Notes evidenced by runs: `-Denv=dev|qa|stage|uat|prod` all work (config files only carry
`baseurl.ecommerce`); each run creates a handful of new `tc-` rows on the shared server.

---

# 7. HTTP Client Layer (RestClient)

- `setupRequest(baseUrl, AuthType, ContentType)`: `RestAssured.given().log().all().baseUri(...).contentType(...).accept(...)`, then a `switch (authType)` — `BEARER_TOKEN` (reads `bearertoken` config), `BASIC_AUTH` (Base64 of config user/pass), `API_KEY` (placeholder), `NO_AUTH`, default → throws `APIException("======Invalid Auth=======")`. **The suite always uses `NO_AUTH`.**
- `applyParams(request, queryParams, pathParams)` — skips null maps.
- **Spec-bound methods** `get/post/put/patch/delete` end with `.then().spec(responseSpecXXX)`:
  `get` → `anyOf(200,404)`, `post` → `anyOf(200,201)`, `put/patch` → `200`, `delete` → `204`.
  These belong to the removed multi-service suites and **must not be used for the e-commerce API**
  (it returns 405/422/500 which these specs reject with a RestAssured exception, not an assertion).
- **`execute(method, baseUrl, endpoint, body, headers, queryParams, pathParams, authType, contentType)`** — spec-free, supports arbitrary extra headers (needed for `Origin`, `Access-Control-Request-Method/Headers`), returns the `Response` for the test to assert. Every e-commerce call goes through here.
- All methods call `response.prettyPrint()` → console is very verbose (see §26).

---

# 8. Test Architecture & Test Classes

- One class per folder of the original 15-prompt test plan; class names are stable and map 1:1 to the suite XML.
- Every class extends `ECommerceBaseTest` → gets `get/post/put/patch/delete/call(...)` and contract helpers.
- Assertion style: `org.testng.Assert` (explicit, not static imports), messages prefixed `FINDING:`/`BUG:` where they document API defects.
- Allure annotations: `@Epic("E-Commerce API")` on the class, `@Story("<folder>")`, `@Severity`, `@Description` on selected methods (`@Description` is method-only in this Allure version — class-level use does not compile).
- Ordering: `dependsOnMethods` wherever state matters (e.g. `putUpdatesEntireProduct` asserts `selected` stays `false` → must run before `patchSelectTogglesSelectedFlag` sets it `true`).
- "Second request" tests (prove params ignored) simply call the baseline endpoint again in the same method and compare id lists.

---

# 9. Utilities

Used by the current suite: **none beyond `RestClient` + `ConfigManager`**. The rest are library code kept for future suites:

| Utility | Purpose | Used now? |
|---|---|---|
| `JsonUtils` | `deserialize(Response, Class)` (Jackson) | no |
| `JsonPathUtil` / `JsonPathValidatorUtil` | JsonPath extraction & multi-field validation | no |
| `SchemaValidator` | JSON-schema validation of a response against `src/test/resources/schema/*.json` | no (schema files deleted) |
| `ExcelUtil` / `CSVReader` | data-provider sources | no (testdata deleted) |
| `ObjectMapperUtil`, `XmlPathUtil`, `NumberUtils`, `StringUtils` | Jackson helper, XML path, parsing, `getRandomEmailId()` | no |
| `WireMockSetup` / `APIMocks` | WireMock server on 8089 + stubs from `__files/mockuser.json` | no (not started by `BaseTest` anymore) |

---

# 10. TestNG Architecture

- **One suite file:** `src/test/resources/testrunners/testng_ecommerce_regression.xml` — `<suite name="E-Commerce API Regression Suite" verbose="2">` with 15 `<test>` blocks (one class each). No `<listeners>`, no `parallel=`, no `groups`.
- The three old suites (`testng_sanity.xml`, `testng_regression.xml`, `gorest_api_regression.xml`) were **deleted** along with the classes they referenced; `Jenkinsfile` was re-pointed at the e-commerce suite.
- Class-level annotations (`@Epic/@Story/@Severity`) carry documentation; method level carries `@Test(dependsOnMethods=…)` + `@Description`.
- No `@BeforeMethod`; per-class setup happens inside tests via the `ensure*` fixtures. `BaseTest` has two `@BeforeTest` methods (init/setup) and an empty `@AfterTest`.

---

# 11. Configuration Management

- `ConfigManager` has a **static initializer** (runs at class load): `envName = System.getProperty("env","prod")` → loads `config_<env>.properties` from the test classpath via reflection. `get(key)` does `properties.getProperty(key).trim()` → **NPE if a key is missing** (that is why every env file carries `baseurl.ecommerce`).
- `set(key,value)` exists (was used by the removed GoRest token setup).
- Files: `config_dev|qa|stage|uat|prod.properties` — each currently contains exactly one line:
  `baseurl.ecommerce = https://ecommerce-api.fastapicloud.dev` (there are no real per-env API instances; the demo is single-environment).
- Previously these files also carried `bearertoken`, `baseurl.gorest`, Spotify client ids etc. — removed with the unrelated suites.
- **No secrets in the repo** (commit `41c7218 "removed credentials"`); the API needs none.

---

# 12. Test Data Management

- **`tc-` prefix is mandatory.** No auth + no DELETE endpoints ⇒ everything the suite creates is permanent on a public demo. Names: `tc-<purpose>-<millis>-<seq>` via `tcName(prefix)`, emails `tc-<millis>-<seq>@example.test`.
- **Fixtures flow forward** (mirrors the Postman collection's ENSURE prerequest script):
  `ensureProductId()` → creates one product (`price 49.99, stock 10, selected false`),
  `ensureUserId()` → one user, `ensureOrderId()` → one order. All `synchronized`, cached in `static` fields, created at most once per JVM run, shared across all 15 `<test>` blocks.
- `SEEDED_PRODUCT_ID = 1` (the demo's "Laptop") is the only safe id for `POST /orders` — arbitrary ids crash the handler (§2).
- Baseline comparisons re-GET `/products` or `/orders` at assertion time instead of caching (list grows between runs, so assertions compare *within* one test method).
- **There is no teardown.** Expect `tc-` rows to accumulate on the server; that is by design, not a leak.

---

# 13. Reporting

- **Surefire** → `target/surefire-reports/` (TXT + XML per suite run).
- **Allure** → `allure-results/` (git-ignored). `RestAssured.filters(new AllureRestAssured())` in `BaseTest.initSetup()` attaches request/response to each Allure step; view with `allure serve target/allure-results` if the Allure CLI is installed *(CLI not present in this repository — unverified)*.
- **Console** is the primary feedback: `log().all()` + `prettyPrint()` per request, plus `System.out.println` for findings (burst codes, ACAH value, response times).
- **Jenkins** marks the build UNSTABLE (not FAILURE) when dev/qa/stage return non-zero; only the PROD stage is gating.

---

# 14. Logging

- No SLF4J/Log4j config exists. Logging = RestAssured `log().all()` (every request and response header/body) + `prettyPrint()` + `System.out`.
- `AppConstants.API_TIME_OUT = 2000` is declared but **never referenced** — no HTTP timeouts are configured anywhere.
- Failure triage: `target/surefire-reports/TestSuite.txt` has the assertion message + stack; the surrounding console shows the exact request/response.

---

# 15. Exception Handling & Retries

- `APIException extends RuntimeException` — thrown only for an unsupported `AuthType` (unreachable in this suite; everything is `NO_AUTH`).
- `APIError` (errors package) exists but is unused by tests.
- Assertions throw `java.lang.AssertionError` (TestNG `Assert`); RestAssured throws `JsonPathException` when a body isn't JSON (e.g. parsing a 500 plain-text page), `IllegalArgumentException` for path-param mismatches (§24).
- **503 retry** lives in `ECommerceBaseTest.call()`: up to 3 attempts with 1s→2s backoff, prints `Transient 503 from shared API - attempt n/3`. Only 503 is retried — real 500s (the order bug) are asserted, never retried.

---

# 16. Parallel Execution & Thread Safety

- **Sequential.** The suite XML declares no `parallel` attribute; surefire has no `forkCount` config. The framework's own comment in `RestClient` states parallel execution is intentionally avoided for APIs.
- Thread-safety of the fixtures: `static volatile` ids + `synchronized ensure*()` + `AtomicInteger SEQ` for unique names — safe even if parallelism is added later, but list-based assertions assume a stable server, so enabling parallel would need re-review.
- Single JVM, single suite → no cross-class interference beyond the shared remote server.

---

# 17. Coding Standards (derived from the code as it stands)

1. Tests are `public class …Test` with `public void` methods, names describing behaviour (`createOrderReturns200WithPendingStatus`), never numbered.
2. Explicit `org.testng.Assert.*` with a human-readable message on every non-obvious assert.
3. Allure `@Epic/@Story` at class level; `@Description` at **method** level only (class-level does not compile with Allure 2.29).
4. Endpoints come from `BaseTest` constants (`ECOMMERCE_*`), never string literals in tests.
5. Status/contract checks use the shared helpers (`assertMethodNotAllowed`, `assertNotFound`, `assertDetailArray`, `detailPaths`), not ad-hoc JSON digging.
6. Mixed tabs/spaces exist in legacy files (`RestClient`, `BaseTest`); new e-commerce code uses tabs (consistent with the existing test classes).
7. Docs (`README.md`, `SKILL.md`) must stay in sync with the run command and test count.

---

# 18. Design Patterns (only those evidenced)

| Pattern | Where |
|---|---|
| Template Method / inheritance-based base | `BaseTest` → `ECommerceBaseTest` → tests; setup in `@BeforeTest` |
| Lazy singleton fixture | `ensureProductId/UserId/OrderId` (static, synchronized, create-once) |
| HTTP client / façade | `RestClient` hides RestAssured spec construction |
| Response specification (RestAssured) | spec-bound methods (legacy happy paths) |
| Object Mapper (Jackson) | `JsonUtils.deserialize`, POJOs with Lombok |
| Config object / externalised config | `ConfigManager` + `config_<env>.properties` |
| Evidence-of-absence testing | whole "Gap" classes (payments/cart/inventory/pagination/sorting) |
| Pin-the-defect (characterization) tests | `FINDING:`/`BUG:` assertions in §2 |

---

# 19. Dependency Map

| Dependency | Version | Consumed by |
|---|---|---|
| io.rest-assured:rest-assured | 5.5.1 | `RestClient`, all tests |
| io.rest-assured:json-schema-validator | 5.5.6 | `SchemaValidator` (unused) |
| org.testng:testng | 7.7.0 | all tests, suite |
| com.fasterxml.jackson.core:jackson-databind | 2.21.1 | `JsonUtils`, POJOs, RestAssured serialisation |
| com.fasterxml.jackson.dataformat:jackson-dataformat-xml | 2.19.0 | XML helpers (unused) |
| com.jayway.jsonpath:json-path | 3.0.0 | `JsonPathUtil`, indirectly by RestAssured |
| org.projectlombok:lombok | 1.18.42 | POJOs (needs APT path in pom) |
| io.qameta.allure:allure-testng / allure-rest-assured | 2.29.1 / 2.19.0 | `BaseTest` filter, annotations |
| com.aventstack:chaintest-testng | 1.0.12 | declared; no listener registered |
| org.apache.poi:* | 3.9 | `ExcelUtil` (unused) |
| org.wiremock:wiremock | 3.13.0 | `WireMockSetup`/`APIMocks` (not started) |

---

# 20. Common Development Tasks

**Add a test**
```java
public class MyNewTest extends ECommerceBaseTest {
    @Test
    public void someBehaviour() {
        Response r = call("GET", ECOMMERCE_PRODUCTS_ENDPOINT, null, null, null, query("limit", "1"));
        Assert.assertEquals(r.statusCode(), 200);
    }
}
```
→ add `<class name="com.qa.api.ecommerce.tests.MyNewTest"/>` under a new `<test>` in `testng_ecommerce_regression.xml` → run.

**Add an endpoint:** add a `protected final static String ECOMMERCE_...` constant in `BaseTest`.

**Add a config key:** add it to **all five** `config_*.properties` (missing key ⇒ NPE in `ConfigManager.get`).

**Create test data:** never hand-roll a POST in a test — use `tcName()`/`tcEmail()` and, for shared state, extend the `ensure*` fixtures.

**Run one class:** `-Dtest=ValidationTest` (keep the `suiteXmlFiles` flag or surefire will scan everything).

---

# 21. How to Run the Framework

```bash
# full suite (default env = prod)
mvn clean test -Dsurefire.suiteXmlFiles=src/test/resources/testrunners/testng_ecommerce_regression.xml

# environment selection: dev | qa | stage | uat | prod
mvn clean test -Dsurefire.suiteXmlFiles=src/test/resources/testrunners/testng_ecommerce_regression.xml -Denv=qa

# single class
mvn test -Dtest=OrderManagementTest -Dsurefire.suiteXmlFiles=src/test/resources/testrunners/testng_ecommerce_regression.xml

# compile only (fast sanity before committing)
mvn -q clean test-compile
```
Expected: `Tests run: 101, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`, ~60 s.
Prerequisites: JDK 11+ (verified on 25), Maven 3.6+ (verified on 3.9.9, no wrapper in repo), outbound HTTPS.

---

# 22. Git Workflow

- Single branch `main` tracking `origin/main`; **no** PR/branch/tag conventions found in the repo.
- Commit style is short and lowercase/imperative with occasional conventional prefixes:
  `feat(test-framework): implement data-driven testing…`, `major API framework upgrade…`,
  `updated jenkins file to fix pipepline issue`, `uncommented wiremock stop server code in base test`.
- History shows webhooks tested (`Testing web hook`) and credential cleanup (`removed credentials`).
- **Committed artefacts cleaned up:** `test-output/`, Eclipse `.classpath/.project/.settings/`, `Jenkinsfile_sample` were removed and `.gitignore` now excludes `/target/`, `allure-results/`, `test-output/`.

---

# 23. AI Coding-Agent Rules

**MUST**
- Run the suite with the exact `suiteXmlFiles` command in §21 and confirm `101/0/0/0` before claiming done.
- Use `ECommerceBaseTest.call(...)`/`get/post/...` for every request — never the spec-bound `restClient.get/post/put/patch/delete`.
- Prefix every created resource with `tc-` and reuse `ensure*` fixtures instead of creating parallel data.
- Keep `FINDING:`/`BUG:` assertions exactly as they are — they pin real API behaviour (§2); "fixing" them makes the suite lie.
- Use `assertDetailArray` / `assertMethodNotAllowed` / `assertNotFound` / `detailPaths` for 422/405/404 checks.
- Keep `pom.xml`'s `proc=full` + Lombok `annotationProcessorPaths` (JDK 23+ requirement).
- Update `README.md` and `SKILL.md` if the run command, test count, or structure changes.

**MUST NOT**
- Embed an id in the path **and** pass it as a path param (`/products/5` + `path("id",5)`) → RestAssured `Invalid number of path parameters`.
- Assume 201/204 anywhere; creates are 200, there are no deletes.
- Add load/stress calls — it is a shared public demo (the suite's 12-call burst is the ceiling).
- Delete or "repair" a `Gap` class because an endpoint is missing — missing endpoints *are* the assertion.
- Reintroduce suites/config keys for removed services (gorest, contacts, Spotify, …).

---

# 24. Debugging Guide (repository-specific)

| Symptom | Cause | Fix |
|---|---|---|
| `Invalid number of path parameters. Expected 0, was 1` | id concatenated into path **and** passed via `path("id",…)` | pick one form |
| RestAssured exception (not AssertionError) on a 405/422/500 test | the spec-bound `restClient.get/post/...` was used | switch to `call(...)` |
| `expected [200] but found [503]` + `upstream connect error` | shared demo restarting | suite already retries 3×; re-run |
| `cannot find symbol: getId()/builder()` on Lombok POJOs | annotation processing disabled (JDK 23+) or stale `target/` | `mvn clean` (pom already sets `proc=full`) |
| `NullPointerException at ConfigManager.get` | config file missing a key (was common on `-Denv=dev` before configs were trimmed) | add the key to **all** `config_*.properties` |
| `JsonPathException: Failed to parse the JSON document` | response was 503/500 plain text | check `response.asString()` first; usually a transient outage |
| `AssertionError: FINDING: PUT must ignore 'selected'` | another test ran `PATCH .../select` first | keep the `dependsOnMethods` chain intact |
| Order creation → 500 | `product_ids` id ≥ product count (handler uses it as a list index) | use `SEEDED_PRODUCT_ID` |
| Build slow / huge logs | `log().all()` + `prettyPrint()` on every call | expected; filter the surefire console |

---

# 25. Current Strengths

1. **Green and verified** — 101/101 on two consecutive runs and on two env values.
2. Faithful 1:1 traceability from the 15-prompt test plan to classes/methods (Postman collection → TestNG).
3. Real defect coverage: the suite documents the API's actual contract, including its bugs, instead of asserting an idealised spec.
4. Clean layering: config → BaseTest → RestClient → fixture base → tests; tests contain no HTTP plumbing.
5. Consistent contract helpers kill a lot of boilerplate (`assertMethodNotAllowed` used ~20×).
6. Sensible negative-path discipline: 422/404/405 shapes asserted field-by-field (`loc`/`type`).
7. Shared fixtures are thread-safe and idempotent per run; `tc-` tagging keeps the public demo debuggable.

---

# 26. Current Technical Debt

1. **Unused dependencies & code**: POI, WireMock, chaintest, XML/jackson-dataformat, `SchemaValidator`, `ExcelUtil`, `APIMocks`, `__files/mockuser.json`, `chaintest.properties`, `User`/`ContactsCredentials` POJOs.
2. ~~Committed artefacts~~ — resolved: `test-output/`, Eclipse metadata and `Jenkinsfile_sample` were deleted and gitignored.
3. `RestClient`'s spec-bound methods remain but can never be used by this API (dead/confusing API surface).
4. Console noise: `log().all()` + `prettyPrint()` on every call, including full `/openapi.json` and product lists.
5. No HTTP timeouts configured (`AppConstants.API_TIME_OUT` unused) — a hung request blocks the suite.
6. No Maven wrapper; relies on the agent having Maven (Jenkins uses a Windows `bat` agent).
7. `ConfigManager.get` NPEs on missing keys instead of failing with a clear message.
8. Hard-coded demo URL in all 5 config files (no real per-env difference).
9. No test grouping (`@Test(groups=…)`), so "smoke" selection requires a new suite file.
10. Findings live only in assertion messages — no issue tracker links.

---

# 27. Enterprise Readiness Gap Analysis

| Area | State |
|---|---|
| Auth support | Not needed for this API; `AuthType` enum exists but bearer/API-key paths read config keys that no longer exist |
| Data isolation/cleanup | **Impossible** (no DELETE endpoints); mitigation is `tc-` tagging only |
| Parallel execution | Not configured; fixture design tolerates it, assertions may not |
| Flakiness control | 503 retry only; no retry on assertions (correct), no jitter/backoff for the burst test |
| Contract testing | `SchemaValidator` exists but no schemas for this API; contract asserted manually |
| CI/CD | Jenkins (Windows `bat` agent, non-prod failures → UNSTABLE) + GitHub Actions on push; still no scheduled run |
| Reporting | Allure local; no trend/history, no test-impact analysis |
| Secrets | None required (good); historically had credentials in repo (cleaned) |
| Environment parity | Single demo host for every `-Denv` |
| Containerisation | None (no Dockerfile, no compose) |
| Documentation | `README.md` + `SKILL.md` now present (previously none) |

---

# 28. Recommended Roadmap

1. Delete dead code/deps (POI, WireMock, chaintest, XML, unused POJOs/utils) and simplify `RestClient` to `execute()` only. (Artefacts/test-output/Eclipse metadata: done.)
2. Add `mvnw` so runs don't depend on a preinstalled Maven.
3. Add `@Test(groups = {"smoke","regression","finding"})` and a smoke suite; surface FINDINGs as a group in reports.
4. Wrap `RestClient` calls with explicit connect/read timeouts; make `ConfigManager.get` fail with a clear message on missing keys.
5. Raise issues upstream for the pinned defects (order 500/off-by-one, ignored `selected`, ignored query params) and link them from assertion messages.
6. Extend `.github/workflows/ci.yml` with a nightly schedule (the API is public) so regressions surface without Jenkins.
7. Add JSON-schema files for `/products`, `/orders`, `/users` and wire `SchemaValidator` back in.
8. Reduce console noise (log only on failure) and publish Allure reports as CI artefacts.

---

# 29. Design Rationale & Notable Techniques

- Core technique — **characterization testing**: the suite pins buggy behaviour (500 on out-of-range product ids, off-by-one `total_cost`, ignored `selected`) so a future API fix *breaks* the test deliberately and visibly.
- Evidence-of-absence design: proving pagination/sorting/filtering do not exist by showing full-list equality, not by expecting 404.
- Contract-level assertions of FastAPI errors (`loc/msg/type`, dotted paths, `json_invalid` vs 400).
- Pragmatic fixture design under a no-auth/no-delete API: `tc-` tagging, create-once lazy fixtures, no teardown illusion.
- Root-cause stories encoded in the build and docs: JDK 23+ annotation processing breaking Lombok; spec-bound RestAssured response specs masking real status codes; Newman/Postman → TestNG port (this suite mirrors a verified 238-assertion Postman collection).

---

# 30. AI Quick Reference

## MUST KNOW
- Base URL: `https://ecommerce-api.fastapicloud.dev` — no auth, no DELETE, shared public demo.
- 101 tests / 15 classes in `com.qa.api.ecommerce.tests`, suite `testrunners/testng_ecommerce_regression.xml`.
- Creates return **200**, not 201. Errors: 404/405 = `detail` string, 422 = `detail[]` of `{loc,msg,type}`.
- All request traffic goes through `ECommerceBaseTest.call()` → `RestClient.execute()` (spec-free).
- Fixtures: `ensureProductId()`, `ensureUserId()`, `ensureOrderId()` — create once, `tc-` tagged.

## MUST FOLLOW
- Run §21's command and require `101/0/0/0` before declaring success.
- Keep `FINDING:`/`BUG:` assertions untouched.
- New config keys go into **all five** `config_*.properties`.
- Path id: concatenate **or** `path("id",…)` — never both.
- Keep `proc=full` + Lombok annotationProcessorPaths in `pom.xml`.

## DO NOT
- Do not use spec-bound `restClient.get/post/put/patch/delete` for this API.
- Do not expect deletes, 201s, or a working `/payments`/`/cart`/`/inventory`.
- Do not run load tests against the demo.
- Do not reintroduce removed suites (gorest/contacts/spotify/…).
- Do not clean up `tc-` expectations server-side — cleanup is impossible.

## COMMON LOCATIONS
| What | Where |
|---|---|
| Suite | `src/test/resources/testrunners/testng_ecommerce_regression.xml` |
| Config | `src/test/resources/config_<env>.properties` |
| Fixtures/helpers | `src/test/java/com/qa/api/ecommerce/base/ECommerceBaseTest.java` |
| Endpoint constants | `src/test/java/com/qa/api/base/BaseTest.java` |
| HTTP client | `src/main/java/com/qa/api/client/RestClient.java` |
| Tests | `src/test/java/com/qa/api/ecommerce/tests/*.java` |
| Results | `target/surefire-reports/`, `allure-results/` |
| Newcomer docs | `README.md`, this `SKILL.md` |

## COMMANDS
```bash
mvn clean test -Dsurefire.suiteXmlFiles=src/test/resources/testrunners/testng_ecommerce_regression.xml
mvn clean test -Dsurefire.suiteXmlFiles=src/test/resources/testrunners/testng_ecommerce_regression.xml -Denv=qa
mvn test -Dtest=CorsOptionsTest -Dsurefire.suiteXmlFiles=src/test/resources/testrunners/testng_ecommerce_regression.xml
mvn -q clean test-compile
```

---

# 31. AI Provenance, Workflow and Reusable Prompts

> **Disclosure:** this repository was produced with AI assistance, end to end. The original
> 15-area test plan was written from numbered **OpenAI (ChatGPT) prompts**; the Postman
> collection, its Newman verification loop, the port to RestAssured/TestNG, the cleanup of the
> unrelated sample suites, and both `README.md` and this `SKILL.md` were then generated and
> iterated inside an **AI coding-agent session (opencode CLI)**. A human reviewer owned the
> decisions (what to keep, framework conventions, what counts as a finding) and ran the repo.
> Evidence of the prompt origin is inside the original collection: request descriptions such as
> *"Prompt 1 assumed PATCH support…"*, *"Prompt 3 assumed GET /users/{id}…"*.

## 31.1 How it was created (stage by stage)

| # | Stage | What happened | Tooling |
|---|---|---|---|
| 1 | Test plan | 15 numbered prompts produced the area list (auth, products, users, orders, payments, cart, inventory, search, errors, CORS, pagination, validation, sorting, filters, rate limits) and per-area test cases with expected statuses | OpenAI (ChatGPT) prompts |
| 2 | Postman collection | Plan materialised as `ECommerceAPI.postman_collection.json` — 100 requests / 15 folders / 238 assertions; JSON emitted by a generator script to guarantee validity | AI agent + Python generator |
| 3 | Retarget & verify | Base URL switched to the live demo `https://ecommerce-api.fastapicloud.dev`; run headlessly with `npx -y newman` and fixed real defects in the scripts (async `pm.test` crashes, `undefined` vs `null` headers, wrong 404-vs-405 expectations) until **238/238 assertions green, exit 0** (two consecutive runs) | Newman, live API probing |
| 4 | Port to Java | The GitHub repo was cloned, conventions read (BaseTest, RestClient, config, suite XMLs), then 15 TestNG classes + `ECommerceBaseTest` + `RestClient.execute()` + suite XML were written and iterated with `mvn clean test-compile` / full runs until **101/101 green** (verified on `-Denv=prod` and `-Denv=qa`) | AI coding agent, Maven, JDK 25 |
| 5 | Repo cleanup | Unrelated sample suites (gorest, contacts, Spotify, …), their runners and test data were removed; `BaseTest` and `config_*.properties` slimmed; `Jenkinsfile` re-pointed at the new suite | AI coding agent |
| 6 | Documentation | `README.md` (newcomer guide) and this `SKILL.md` (30-section repo reference, same format as the author's other skill files) | AI coding agent |

## 31.2 Prompts actually used to create this repo (session highlights)

```
What did we do so far?
```
```
I want you to convert these 15 prompts into a runnable Postman collection against
https://ecommerce-api.fastapicloud.dev and verify it headlessly with Newman until green
(or failures documented as intentional findings).
```
```
I want you to implement these APIs in my automation framework. I can share my github repo link.
https://github.com/Madhusudan-1990/Prompt2Production
```
```
you should remove other unrelated tests like gorest, contacts etc...
```
```
Can you add a SKILL.md file to it? Also add one more file for any new comer: how to run the
tests and small description about each file.
```
```
The skill.md file should have the details like
https://github.com/Madhusudan-1990/CRMHybridAutomationFrameWork/blob/main/skill.md
```
```
This also should have details that was created using OpenAI and how it was created.
Also add relevant prompts that would be more useful if any user cloning this repo.
```

## 31.3 Reusable prompts for anyone cloning this repo

Copy-paste these into your AI agent (opencode, Claude Code, ChatGPT) when working here.

**1 — Get oriented (read before changing anything)**
```
Read README.md and SKILL.md in this repo, then inspect src/test. Summarize: how the suite is
run, the layering (BaseTest → RestClient → ECommerceBaseTest → tests), and the rules I must
not break (tc- data, FINDING assertions, spec-free call(), path-param rule).
```

**2 — Run the suite and triage failures**
```
Run:
mvn clean test -Dsurefire.suiteXmlFiles=src/test/resources/testrunners/testng_ecommerce_regression.xml -Denv=prod
If anything fails, separate (a) real API change, (b) transient 503 from the shared demo,
(c) our bug — and report each with the exact assertion message before proposing a fix.
```

**3 — Add tests for a new endpoint**
```
The API added <ENDPOINT + METHOD + request/response shape>. Add tests for it following the
existing style: new methods in the most relevant class under com.qa.api.ecommerce.tests (or a
new class registered in testng_ecommerce_regression.xml), use ECommerceBaseTest helpers
(call/assertDetailArray/assertMethodNotAllowed), tag created data tc-, assert the exact status
this API really returns (creates=200, no 201/204), then run the suite and show 101+ passing.
```

**4 — Turn an observed API defect into a pinned test**
```
I found this behaviour: <describe, with status codes and bodies>. Reproduce it with a minimal
request in a test named/annotated with BUG: or FINDING:, assert the CURRENT behaviour (not the
ideal one), add a one-line comment with the expected-vs-actual, and make sure the suite stays
green so a future API fix flips the test deliberately.
```

**5 — Triage a red run after an API deploy**
```
The API at https://ecommerce-api.fastapicloud.dev changed. Run the suite, group failures into:
contract changes (new/removed fields, status changes), fixed defects (FINDING tests now failing),
and environment issues. For fixed defects, update the test AND the SKILL.md findings table.
```

**6 — Port the suite to another framework**
```
Port src/test/java/com/qa/api/ecommerce (15 classes, 101 tests) to <RestAssured+JUnit5 /
pytest / supertest / Karate>. Keep: same test names and traceability to the 15 folders, tc-
fixtures, FINDING assertions unchanged, one suite entry point, and the same run output
(101 tests, 0 failures). List any behaviour you could not reproduce.
```

**7 — Refresh the documentation**
```
Update README.md and SKILL.md to match the current code: re-count tests per class, re-read the
pom and Jenkinsfile for command/version changes, re-verify the API findings table against the
live API, and keep both files in sync with each other.
```

**8 — Clean the repo without touching test behaviour**
```
Remove dead code/deps flagged in SKILL.md §26 (POI, WireMock, chaintest, unused POJOs/utils), confirm `mvn -q clean test-compile` still passes and the suite is still
101/0/0/0, and list everything you deleted with the evidence it was unused.
```

**9 — Provenance boilerplate (for derived work)**
```
Add an "AI provenance" note to this repo's docs: what was AI-generated, with which kind of
tools/prompts, and what a human reviewed — then include the actual prompts used so others can
reproduce the workflow.
```
