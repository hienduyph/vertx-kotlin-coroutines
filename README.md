# Vertx Kotlin Coroutine Example

## Get started
**Run database first**

```sql
podman run --name postgres -d -p 5432:5432 -e POSTGRES_PASSWORD=root -e POSTGRES_DB=blog docker.io/postgres:14
```
**Init the database**

```sql
CREATE EXTENSION "pgcrypto";

CREATE TABLE blog.posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid() ,
    title VARCHAR(255),
    content text,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```
**Run the server**
```bash
./gradlew run
```

```bash
curl -X POST -H "content-type:application/json" -d '{"title": "This is next post", "content": "Hello world from sample post"}' 'http://localhost:8888/posts'
curl 'http://localhost:8888/posts'
```