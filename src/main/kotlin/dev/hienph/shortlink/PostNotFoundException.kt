package dev.hienph.shortlink

import java.util.UUID

class PostNotFoundException(id: UUID) : RuntimeException("Post id: $id was not found. ")
