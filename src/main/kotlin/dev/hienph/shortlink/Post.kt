package dev.hienph.shortlink

import java.time.LocalDateTime
import java.util.UUID

data class Post(
    var id: UUID = UUID.randomUUID(),
    var title: String,
    var content: String = "",
    var created_at: LocalDateTime = LocalDateTime.now(),
) {
}

data class CreatePostCommand(
    var title: String,
    var content: String
)