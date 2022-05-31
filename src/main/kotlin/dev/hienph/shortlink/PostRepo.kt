package dev.hienph.shortlink

import io.vertx.kotlin.coroutines.await
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.Tuple
import java.util.UUID
import java.util.stream.StreamSupport
import kotlin.streams.toList

class PostRepo(private val client: PgPool) {
    suspend fun findAll() = client.query("SELECT * FROM posts ORDER BY id ASC")
        .execute()
        .map { rs ->
            StreamSupport.stream(rs.spliterator(), false)
                .map { mapFun(it!!) }
                .toList()
        }.await()

    suspend fun findById(id: UUID): Post? = client.preparedQuery("SELECT * FROM posts WHERE id=$1")
        .execute(Tuple.of(id))
        .map { it.iterator() }
        .map { if (it.hasNext()) mapFun(it.next()) else null }
        .await()

    suspend fun save(data: Post) =
        client.preparedQuery("INSERT INTO posts(title, content) VALUES ($1, $2) RETURNING (id)")
            .execute(Tuple.of(data.title, data.content))
            .map { it.iterator().next().getUUID("id") }
            .await()
    suspend fun saveAll(data: List<Post>): Int? {
        val tuples = data.map { Tuple.of(it.title, it.content) }
        return client.preparedQuery("INSERT INTO posts (title, content) VALUES ($1, $2)")
            .executeBatch(tuples)
            .map { it.rowCount() }
            .await()
    }
    suspend fun update(data: Post) = client.preparedQuery("UPDATE posts SET title=$1, content=$2 WHERE id=$3")
        .execute(Tuple.of(data.title, data.content, data.id))
        .map { it.rowCount() }
        .await()
    suspend fun deleteAll() = client.query("DELETE FROM posts").execute()
        .map { it.rowCount() }
        .await()
    suspend fun deleteById(id: UUID) = client.preparedQuery("DELETE FROM posts WHERE id=$1").execute(Tuple.of(id))
        .map { it.rowCount() }
        .await()
    companion object {
        val mapFun: (Row) -> Post = { row: Row ->
            Post(
                row.getUUID("id"),
                row.getString("title"),
                row.getString("content"),
                row.getLocalDateTime("created_at")
            )
        }
    }
}
