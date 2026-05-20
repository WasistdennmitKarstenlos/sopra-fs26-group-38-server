# TripSync Server
Trip Sync consists of a Front- and Backend. Check out the Frontend here: [Client Repo](https://github.com/WasistdennmitKarstenlos/sopra-fs26-group-38-client)

## Introduction
Planning group trips is often a frustrating experience: decisions are scattered across chat
threads, preferences clash and reaching a consensus feels impossible. Without a dedicated
space, valuable ideas get lost and the loudest voice often wins rather than the best idea.
TripSync solves this by providing a simple, intuitive and real time collaborative platform built around focused Trip Rooms.

This repository contains the TripSync backend. It provides the server-side logic and APIs for registering and logging in, creating and joining Trip Rooms, proposing destinations, searching activities, voting, commenting, finalizing a trip, and downloading a final trip report.

## Technologies Used
- Backend: Java 17, Spring Boot 4, Spring Web MVC, Spring Data JPA
- Database: H2 for local development, MySQL for production
- External APIs: Google Places API
- Real-time Communication: Server-Sent Events
- Testing & Quality Assurance: JUnit 5, Mockito, JaCoCo, SonarCloud
- Cloud Infrastructure: Google Cloud Build, Google Cloud Run, Google Cloud SQL

## High-Level Components

| Component | Role | Main files |
| --- | --- | --- |
| Main Class File | Boots the server, exposes `/`, configures CORS, Google Maps settings, and HTTP clients. | [`Main Class File`](src/main/java/ch/uzh/ifi/hase/soprafs26/Application.java) |
| REST API layer | Receives requests from the client, validates bearer tokens where required, converts entities to DTOs, and delegates business logic to services. | [`UserController`](src/main/java/ch/uzh/ifi/hase/soprafs26/controller/UserController.java), [`TripController`](src/main/java/ch/uzh/ifi/hase/soprafs26/controller/TripController.java), [`DestinationController`](src/main/java/ch/uzh/ifi/hase/soprafs26/controller/DestinationController.java), [`ActivityController`](src/main/java/ch/uzh/ifi/hase/soprafs26/controller/ActivityController.java) |
| Domain services | Owns the trip-planning rules: registration/login, trip membership, destination proposals, activity voting, comments, finalization, and final reports. | [`UserService`](src/main/java/ch/uzh/ifi/hase/soprafs26/service/UserService.java), [`TripService`](src/main/java/ch/uzh/ifi/hase/soprafs26/service/TripService.java), [`DestinationService`](src/main/java/ch/uzh/ifi/hase/soprafs26/service/DestinationService.java), [`ActivityManagementService`](src/main/java/ch/uzh/ifi/hase/soprafs26/service/ActivityManagementService.java), [`FinalReportService`](src/main/java/ch/uzh/ifi/hase/soprafs26/service/FinalReportService.java) |
| Persistence layer | Stores users, trips, memberships, destinations, activities, comments, and votes through Spring Data repositories. | [`entity`](src/main/java/ch/uzh/ifi/hase/soprafs26/entity), [`repository`](src/main/java/ch/uzh/ifi/hase/soprafs26/repository) |
| External and real-time integrations | Searches Google Places for activities, fetches activity photos, and publishes destination/status changes to subscribed clients via SSE. | [`ActivitySearchService`](src/main/java/ch/uzh/ifi/hase/soprafs26/service/ActivitySearchService.java), [`DestinationRealtimeService`](src/main/java/ch/uzh/ifi/hase/soprafs26/service/DestinationRealtimeService.java), [`DestinationVotesUpdatedEvent`](src/main/java/ch/uzh/ifi/hase/soprafs26/event/DestinationVotesUpdatedEvent.java), [`TripStatusUpdatedEvent`](src/main/java/ch/uzh/ifi/hase/soprafs26/event/TripStatusUpdatedEvent.java) |

The usual request path is: controller -> service -> repository/entity -> DTO response. When votes, destinations, or trip status change, services publish events that [`DestinationController`](src/main/java/ch/uzh/ifi/hase/soprafs26/controller/DestinationController.java) forwards to clients through `/trips/{tripId}/destinations/stream`.

## Getting Started

These instructions get a local copy of the backend running for development and testing. Local development uses an in-memory H2 database, so no external database has to be running. Activity search and photo endpoints require a Google Maps API key.

### Prerequisites
Ensure you have the following things installed:

```bash
java -version
git --version
docker --version
```

Required versions and tools:
* Java 17
* Git
* The included Gradle wrapper, `./gradlew`
* Optional but needed for activity search: a Google Maps API key with Places API access

## Launch & Deployment
### Local Launch
Local launch requires only Java 17. The application uses [`application.properties`](src/main/resources/application.properties), starts on `PORT` or `8080`, allows the local client origins `http://localhost:3000` and `http://127.0.0.1:3000`, and stores data in H2 memory.

```bash
export GOOGLE_MAPS_API_KEY="your-google-maps-api-key"
./gradlew bootRun
```

### Local Development Mode
For automatic rebuilds while editing, use two terminals:
```bash
./gradlew build --continuous -xtest
```
```bash
./gradlew bootRun
```

### Local Database Access
The local H2 console is available while the server is running:
```text
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:testdb
User: sa
Password: empty
```

### Production Deployment
Production deployment is configured for Google Cloud Run in [`.github/workflows/cloudrun.yml`](.github/workflows/cloudrun.yml). On pushes to `main`, CI:

* installs Java 17
* runs `./gradlew test jacocoTestReport sonar`
* builds and pushes a Docker image to Artifact Registry
* deploys the image to Cloud Run with Cloud SQL attached
* smoke-tests the deployed `/` endpoint

Production database values are defined in [`app.yaml`](app.yaml) while Google Maps API Key, Sonar Token and Service Account credentials are stored as secrets in Google Cloud.

```text
GCP_SERVICE_CREDENTIALS
GOOGLE_MAPS_API_KEY
SONAR_TOKEN
```

## Running the Tests
Run all automated tests:
```bash
./gradlew test
```

### Break Down Into End To End Tests
The closest backend end-to-end tests are the Spring Boot and integration tests. They boot application context slices or the full application, use the H2 database, and verify that components work together.

Examples:

```bash
./gradlew test --tests "ch.uzh.ifi.hase.soprafs26.Soprafs26ApplicationTests"
./gradlew test --tests "ch.uzh.ifi.hase.soprafs26.ApplicationCorsTest"
./gradlew test --tests "ch.uzh.ifi.hase.soprafs26.service.ActivityPersistenceIntegrationTest"
```

### And Coding Style Tests
There is no dedicated local Checkstyle or Spotless task in this backend. Code quality is enforced through compilation, unit tests, JaCoCo, and SonarCloud in CI. To run the same local quality checks that feed SonarCloud, provide a `SONAR_TOKEN` and run:

```bash
./gradlew test jacocoTestReport sonar
```

For focused unit and controller tests, use:
```bash
./gradlew test --tests "ch.uzh.ifi.hase.soprafs26.service.TripServiceTest"
./gradlew test --tests "ch.uzh.ifi.hase.soprafs26.controller.TripControllerTest"
```

## Roadmap
* Add stronger authentication and password storage, such as hashed passwords and expiring session tokens.
* Add richer notifications for trip events, including participant joins, comments, and finalization.
* Expand final reports with persisted activity comments, export history, and richer itinerary data.

## Built With
* [Spring Boot](https://spring.io/projects/spring-boot) - Web framework and application runtime
* [Gradle](https://gradle.org/) - Build and dependency management
* [Spring Data JPA](https://spring.io/projects/spring-data-jpa) - Persistence abstraction
* [H2 Database](https://www.h2database.com/) - Local in-memory database
* [MySQL](https://www.mysql.com/) and [Google Cloud SQL](https://cloud.google.com/sql) - Production database
* [Google Places API](https://developers.google.com/maps/documentation/places/web-service) - Activity search and photos
* [Google Cloud Run](https://cloud.google.com/run) - Containerized deployment
* [JUnit 5](https://junit.org/junit5/) and [Mockito](https://site.mockito.org/) - Automated testing


## Authors
* [sajayshenoy](https://github.com/sajayshenoy)
* [itsmeroya](https://github.com/itsmeroya)
* [JeanKeim1](https://github.com/JeanKeim1)
* [WasistdennmitKarstenlos](https://github.com/WasistdennmitKarstenlos)
* [3xpr](https://github.com/3xpr)

See also the list of [repository contributors](https://github.com/WasistdennmitKarstenlos/sopra-fs26-group-38-server/contributors).

## License
This project is licensed under the Apache License 2.0. See [`LICENSE`](LICENSE) for details.

## Acknowledgments
* University of Zurich SoPra FS26 teaching team for the original Spring Boot server template.
* Spring, Gradle, and Google Cloud documentation and tooling.
* Everyone who tested the trip-planning flow and helped shape the final project.