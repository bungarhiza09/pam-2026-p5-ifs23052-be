package org.delcom.data

import kotlinx.serialization.Serializable
import org.delcom.entities.Todo

@Serializable
data class TodoListResponse(
    val todos: List<Todo>,
    val meta: Meta
)

@Serializable
data class Meta(
    val page: Int,
    val perPage: Int,
    val total: Long,
    val totalDone: Long,
    val totalPending: Long
)