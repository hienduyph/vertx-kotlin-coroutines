package dev.hienph.shortlink

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.core.json.Json
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.awaitEvent
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import kotlinx.coroutines.launch

class MainVert : CoroutineVerticle() {

    val contentTypeJson = "application/json"

    companion object {
        init {
            val objectMapper = DatabindCodec.mapper()
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            objectMapper.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
            objectMapper.disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
            objectMapper.registerModule(JavaTimeModule())
            objectMapper.registerKotlinModule()
        }
    }

    override suspend fun start() {
        val pgPool = createPGPool(vertx)
        val postRepo = PostRepo(pgPool)
        val postHandle = PostsHandler(postRepo)

        val serverFuture = vertx.createHttpServer()
            .requestHandler(routes(postHandle))
            .listen(8888)
        val server = serverFuture.await()
        println("HTTP server port: ${server.actualPort()}")
    }

    override suspend fun stop() {
        super.stop()
    }

    private fun routes(postHandlers: PostsHandler): Router {
        val router = Router.router(vertx)

        val health = mapOf("name" to "Link")
        router.get("/").handler { it.response().end(Json.encode(health)) }

        router.get("/health").handler { r ->
            launch {
                awaitEvent<Long> { h -> vertx.setTimer(2000, h) }
                r.response().end(Json.encode(health)).await()
            }
        }
        router.get("/posts")
            .produces(contentTypeJson)
            .handler { r -> launch { postHandlers.all(r) } }
        router.post("/posts")
            .consumes(contentTypeJson)
            .produces(contentTypeJson)
            .handler(BodyHandler.create())
            .handler { r -> launch { postHandlers.save(r) } }
        router.get("/posts/:id")
            .produces(contentTypeJson)
            .handler { r -> launch { postHandlers.getById(r) } }

        router.post("/posts/:id")
            .consumes(contentTypeJson)
            .produces(contentTypeJson)
            .handler(BodyHandler.create())
            .handler { r -> launch { postHandlers.update(r) } }

        router.route().failureHandler { r ->
            if (r.failure() is PostNotFoundException) {
                r.response().setStatusCode(404)
                    .end(json {
                        obj("message" to "${r.failure().message}", "code" to "not found")
                    }.toString())
            }
        }

        return router
    }

    private fun createPGPool(vert: Vertx): PgPool {
        val opt = PgConnectOptions()
        opt.host = "127.0.0.1"
        opt.user = "postgres"
        opt.password = "root"
        opt.database = "blog"
        val poolOpt = PoolOptions()
        poolOpt.maxSize = 10
        return PgPool.pool(vert, opt, poolOpt)
    }
}