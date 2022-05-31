package dev.hienph.shortlink

import io.vertx.core.json.Json
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.await
import java.util.*
import java.util.logging.Level

class PostsHandler(val posts: PostRepo) {
    suspend fun all(rc: RoutingContext) {
        val data = posts.findAll()
        rc.response().end(Json.encode(data)).await()
    }

    suspend fun getById(rc: RoutingContext) {
        val params = rc.pathParams()
        val id = params["id"]
        val uuid = UUID.fromString(id)
        val data = posts.findById(uuid)
        if (data != null) {
            rc.response().end(Json.encode(data)).await()
        } else {
            rc.fail(404, PostNotFoundException(uuid))
        }
    }
    suspend fun save(rc: RoutingContext) {
        val body = rc.body().asJsonObject()
        LOGGER.info("Got body ${body}")
        val (title, content) = body.mapTo(CreatePostCommand::class.java)
        LOGGER.info("Got data ${title} & ${content}")
        val savedId = posts.save(Post(title = title, content = content))
        rc.response()
            .putHeader("Location", "/posts/$savedId")
            .setStatusCode(201)
            .end()
            .await()
    }
    suspend fun update(rc: RoutingContext) { val params = rc.pathParams()
        val id = params["id"]
        val uuid = UUID.fromString(id)
        val body = rc.bodyAsJson
        LOGGER.log(Level.INFO, "\npath param id: {0}\nrequest body: {1}", arrayOf(id, body))
        var (title, content) = body.mapTo(CreatePostCommand::class.java)
        var existing: Post? = posts.findById(uuid)
        if (existing != null) {
            val data: Post = existing.apply {
                title = title
                content = content
            }
            posts.update(data)
            rc.response().setStatusCode(204).end().await()
        } else {
            rc.fail(404, PostNotFoundException(uuid))
        }
    }
    suspend fun delete(rc: RoutingContext) {
        val params = rc.pathParams()
        val id = params["id"]
        val uuid = UUID.fromString(id)
        val existing = posts.findById(uuid)
        if (existing != null) {
            rc.response().setStatusCode(204).end().await()
        } else {
            rc.fail(404, PostNotFoundException(uuid))
        }
    }

    companion object {
        private val LOGGER = java.util.logging.Logger.getLogger(PostsHandler::class.java.simpleName)
    }
}