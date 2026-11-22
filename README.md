# Animal Picture App

A Spring Boot service that fetches random pictures of cats, dogs and bears, stores them in a database
and returns the last stored picture for each animal. It also has a simple web page to try it out.

Tech: Java 21, Spring Boot 3.5, Maven, H2 (embedded) or PostgreSQL.

## How to run

### With Docker

```bash
docker compose up --build
```

This starts the app together with a PostgreSQL database. Open http://localhost:8080.

### Without Docker

Requires JDK 21. Maven is downloaded automatically by the wrapper.

```bash
./mvnw spring-boot:run
```

On Windows use `mvnw.cmd spring-boot:run`. The app uses an embedded H2 database stored in `./data`,
so nothing else needs to be installed.

To build a jar instead:

```bash
./mvnw clean package
java -jar target/animal-picture-app.jar
```

### Tests

```bash
./mvnw verify
```

## API

`{animal}` can be `cat`, `dog` or `bear`.

- `POST /api/animals/{animal}/pictures?count=3` fetches and stores pictures (count 1 to 10, default 1)
- `GET /api/animals/{animal}/pictures/latest` returns details of the last stored picture
- `GET /api/animals/{animal}/pictures/latest/image` returns the last stored picture as an image
- `GET /api/pictures/{id}/image` returns a stored picture by id

Example:

```bash
curl -X POST "http://localhost:8080/api/animals/dog/pictures?count=3"
curl http://localhost:8080/api/animals/dog/pictures/latest
```

Swagger UI is available at http://localhost:8080/swagger-ui.html.

## Notes

- placekitten.com is offline, so cat pictures come from cataas.com instead. Dogs come from place.dog and
  bears from placebear.com.
- Each request uses a random image size, otherwise the services return the same picture every time.
- The image itself is stored in the database, not only its URL.
- If one picture in a request fails to download, nothing from that request is saved.

## What I would add with more time

- Database migrations with Flyway
- Retries for the external APIs
- An integration test against PostgreSQL with Testcontainers
- An endpoint to list all stored pictures
- Running the fetch step as a job worker in a Camunda BPMN process, so retries and failures are
  visible in Operate
