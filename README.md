# relaunch-back

This project was created using the [Ktor Project Generator](https://start.ktor.io).

Here are some useful links to get you started:

- [Ktor Documentation](https://ktor.io/docs/home.html)
- [Ktor GitHub page](https://github.com/ktorio/ktor)
- The [Ktor Slack chat](https://app.slack.com/client/T09229ZC6/C0A974TJ9). You'll need to [request an invite](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up) to join.

## Features

Here's a list of features included in this project:

| Name                                               | Description                                                 |
| ----------------------------------------------------|------------------------------------------------------------- |
| [Routing](https://start.ktor.io/p/routing-default) | Allows to define structured routes and associated handlers. |

## Building & Running

To build or run the project, use one of the following tasks:

| Task                                    | Description                                                          |
| -----------------------------------------|---------------------------------------------------------------------- |
| `./gradlew test`                        | Run the tests                                                        |
| `./gradlew build`                       | Build everything                                                     |
| `./gradlew buildFatJar`                 | Build an executable JAR of the server with all dependencies included |
| `./gradlew buildImage`                  | Build the docker image to use with the fat JAR                       |
| `./gradlew publishImageToLocalRegistry` | Publish the docker image locally                                     |
| `./gradlew run`                         | Run the server                                                       |
| `./gradlew runDocker`                   | Run using the local docker image                                     |

If the server starts successfully, you'll see the following output:

```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```

## Tests

Integration tests run against the real Supabase instance. Each test class creates its fixtures via `@Before`, cleans them up via `@After`, and is fully isolated from other test classes.

| Suite | Coverage |
|-------|----------|
| `UserRoutesTest` | CRUD + list, get by id, 400/404 handling |
| `ConversationRoutesTest` | CRUD + filter by `user_id` |
| `MessageRoutesTest` | Create/read/delete + filter by `conversation_id` |
| `ObjectiveRoutesTest` | CRUD + filter by `user_id` |

```bash
# Run all tests
./gradlew test

# Run a single suite
./gradlew test --tests "com.codingfactory.UserRoutesTest"
./gradlew test --tests "com.codingfactory.ConversationRoutesTest"
./gradlew test --tests "com.codingfactory.MessageRoutesTest"
./gradlew test --tests "com.codingfactory.ObjectiveRoutesTest"
```

Credentials are loaded from `local.properties` (gitignored). See `.env.example` for the required keys.

---

## API Reference

Base URL: `http://0.0.0.0:31337`

### Users

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/users` | List all users |
| GET | `/users/{id}` | Get a user by ID |
| POST | `/users` | Create a user |
| PUT | `/users/{id}` | Update a user |
| DELETE | `/users/{id}` | Delete a user |

**User object:**
```json
{
  "name": "john_doe",
  "mail": "john@example.com",
  "age": 25
}
```

---

### Conversations

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/conversations` | List all conversations |
| GET | `/conversations?user_id={id}` | List conversations for a user |
| GET | `/conversations/{id}` | Get a conversation by ID |
| POST | `/conversations` | Create a conversation |
| PUT | `/conversations/{id}` | Update a conversation |
| DELETE | `/conversations/{id}` | Delete a conversation |

**Conversation object:**
```json
{
  "user_id": 1,
  "objective_set": false
}
```

---

### Messages

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/messages` | List all messages |
| GET | `/messages?conversation_id={id}` | List messages for a conversation |
| GET | `/messages/{id}` | Get a message by ID |
| POST | `/messages` | Create a message |
| DELETE | `/messages/{id}` | Delete a message |

**Message object:**
```json
{
  "conversation_id": 1,
  "is_user": true,
  "content": "Hello!"
}
```

---

### Objectives

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/objectives` | List all objectives |
| GET | `/objectives?user_id={id}` | List objectives for a user |
| GET | `/objectives/{id}` | Get an objective by ID |
| POST | `/objectives` | Create an objective |
| PUT | `/objectives/{id}` | Update an objective |
| DELETE | `/objectives/{id}` | Delete an objective |

**Objective object:**
```json
{
  "user_id": 1,
  "end_at": "2026-12-31T00:00:00Z",
  "frequency": 7,
  "title": "Exercise daily",
  "description": "30 minutes of cardio"
}
```

