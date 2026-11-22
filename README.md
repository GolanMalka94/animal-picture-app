# Animal Picture App

A small Spring Boot microservice that fetches random pictures of **cats, dogs and bears** from public
placeholder APIs, stores them (including the image bytes) in a database, and serves the last stored
picture of each animal. It also includes a minimal web UI and Swagger docs.

| | |
|---|---|
| Language / build | Java 21, Maven (wrapper included: `./mvnw`) |
| Framework | Spring Boot 3.5 (Web, Data JPA, Validation, Actuator) |
| Database | **H2** embedded (default, no setup needed) · **PostgreSQL** (docker compose) |
| Tests | JUnit 5, Mockito, MockMvc, `@DataJpaTest`, `MockRestServiceServer` (33 tests) |
| Extras | Web UI at `/`, Swagger UI at `/swagger-ui.html`, health at `/actuator/health`, GitHub Actions CI |

## Quick start

### Option 1: containerized (app + PostgreSQL)

Requires Docker only.

```bash
docker compose up --build
```

Open http://localhost:8080. Stop with `docker compose down` (add `-v` to also delete the database volume).

The image can also run on its own with the embedded H2 database:

```bash
docker build -t animal-picture-app .
docker run -p 8080:8080 animal-picture-app
```

### Option 2: non-containerized (embedded H2)

Requires JDK 21 only. Maven is downloaded by the wrapper.

```bash
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

or build a portable jar and run it:

```bash
./mvnw clean package
java -jar target/animal-picture-app.jar
```

Data is stored in `./data/animal-pictures.mv.db` and survives restarts. The H2 console is available at
http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./data/animal-pictures`, user `sa`, empty password).
Use `--server.port=9090` (or `PORT=9090`) if port 8080 is taken.

### Run the tests

```bash
./mvnw verify
```

The tests don't call the real picture APIs, so the build doesn't depend on third-party uptime.

## REST API

`{animal}` is `cat`, `dog` or `bear` (case-insensitive).

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/animals/{animal}/pictures?count=N` | Fetch `N` (1–10, default 1) random pictures and store them. Returns `201` with the stored pictures' metadata. |
| `GET` | `/api/animals/{animal}/pictures/latest` | Metadata (JSON) of the last stored picture of that animal. |
| `GET` | `/api/animals/{animal}/pictures/latest/image` | The last stored picture itself (`image/jpeg`, `image/png`, …). |
| `GET` | `/api/pictures/{id}/image` | A stored picture by id (used by `imageUrl`). |

Examples:

```bash
curl -X POST "http://localhost:8080/api/animals/dog/pictures?count=3"
curl http://localhost:8080/api/animals/dog/pictures/latest
curl -o latest-dog.jpg http://localhost:8080/api/animals/dog/pictures/latest/image
```

```json
{
  "id": 4,
  "animal": "dog",
  "contentType": "image/jpeg",
  "sizeBytes": 331990,
  "sourceUrl": "https://place.dog/473/413",
  "createdAt": "2026-11-22T20:04:04.324377Z",
  "imageUrl": "/api/pictures/4/image"
}
```

Errors are returned as RFC 9457 problem details (`application/problem+json`):

| Status | When |
|---|---|
| `400` | Unknown animal or `count` outside 1–10 |
| `404` | No picture stored yet for that animal / unknown id |
| `502` | The external picture API is down or didn't return an image |

## Picture sources

| Animal | Source | Note |
|---|---|---|
| Cat | `https://cataas.com/cat?width=…&height=…` | **placekitten.com is offline** (HTTP 521), so I replaced it with [Cat as a Service](https://cataas.com) from the public-apis list. |
| Dog | `https://place.dog/{width}/{height}` | as suggested |
| Bear | `https://placebear.com/{width}/{height}` | as suggested |

These placeholder services return an image for a requested size, and placebear always returns the same image
for the same size. The service therefore requests a **random size (200–500 px)** each time, so every call returns a
different picture. Sources, sizes and timeouts are configured in
[`application.yml`](src/main/resources/application.yml) under `app.picture-sources`. Each can be overridden with
environment variables, e.g. `APP_PICTURESOURCES_URLS_CAT=...`.

## Design notes

```
web (REST controller, error handling, UI)  →  service  →  client (external APIs)
                                                       →  domain (JPA entity + repository)
```

- **Storing the image, not only the URL:** the placeholder URLs aren't stable (the same URL can return another
  image later), so the actual bytes are stored. Then "the last picture" is exactly the picture that was fetched.
- **All-or-nothing:** all `N` pictures are downloaded first and then saved in one `saveAll`, so a failing source
  never leaves a partial batch.
- **Two databases, one code base:** H2 for the zero-setup run and the tests, PostgreSQL via the `postgres` Spring
  profile in docker compose. The schema is created by Hibernate (`ddl-auto: update`) and maps to `bytea` on
  PostgreSQL.
- **Robust outbound calls:** connect/read timeouts, redirects followed, content type checked, and a 10 MB size
  cap.
- The container runs as a non-root user. The Docker build is multi-stage and runs the tests as part of
  `mvn package`.

## What I would add with more time

- **Schema migrations** with Flyway instead of `ddl-auto`.
- **Object storage** (S3/MinIO) for the image bytes, keeping only metadata in the database, if volume grows.
- **Resilience:** retries with backoff and a circuit breaker (Resilience4j) per source, and parallel downloads for
  larger counts.
- **Testcontainers** integration test against a real PostgreSQL (today Postgres is verified manually via compose).
- **Pagination endpoint** to list stored pictures, plus deletion/retention policy.
- **Observability:** Micrometer metrics per source (latency, failures), structured logging.
- **Process orchestration:** in a Camunda context, "fetch → validate → store → notify" could be modeled as a BPMN
  process, with this service's fetch logic as a job worker. Retries, timeouts and incidents would then be
  handled and visible in Operate instead of in code.
