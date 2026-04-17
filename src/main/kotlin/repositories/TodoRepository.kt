package org.delcom.repositories

import org.delcom.dao.TodoDAO
import org.delcom.entities.Todo
import org.delcom.helpers.suspendTransaction
import org.delcom.helpers.todoDAOToModel
import org.delcom.tables.TodoTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.lowerCase
import java.util.*

class TodoRepository(private val baseUrl: String) : ITodoRepository {

    // Bangun kondisi query agar tidak duplikasi kode
    private fun buildCondition(
        userId: String,
        search: String,
        isDone: Boolean?
    ): org.jetbrains.exposed.sql.Op<Boolean> {
        var condition = (TodoTable.userId eq UUID.fromString(userId))

        if (search.isNotBlank()) {
            val keyword = "%${search.lowercase()}%"
            condition = condition and (TodoTable.title.lowerCase() like keyword)
        }

        if (isDone != null) {
            condition = condition and (TodoTable.isDone eq isDone)
        }

        return condition
    }

    override suspend fun getAll(
        userId: String,
        search: String,
        isDone: Boolean?,
        page: Int,
        perPage: Int
    ): List<Todo> = suspendTransaction {
        val offset = ((page - 1) * perPage).toLong()

        TodoDAO
            .find { buildCondition(userId, search, isDone) }
            .orderBy(TodoTable.createdAt to SortOrder.DESC)
            .limit(perPage)   // ✅ tidak deprecated
            .offset(offset)   // ✅ tidak deprecated
            .map { todoDAOToModel(it, baseUrl) }
    }

    override suspend fun count(
        userId: String,
        search: String,
        isDone: Boolean?
    ): Long = suspendTransaction {
        TodoDAO
            .find { buildCondition(userId, search, isDone) }
            .count()
    }

    override suspend fun getById(todoId: String): Todo? = suspendTransaction {
        TodoDAO
            .find { TodoTable.id eq UUID.fromString(todoId) }
            .limit(1)
            .map { todoDAOToModel(it, baseUrl) }
            .firstOrNull()
    }

    override suspend fun create(todo: Todo): String = suspendTransaction {
        val todoDAO = TodoDAO.new {
            userId = UUID.fromString(todo.userId)
            title = todo.title
            description = todo.description
            cover = todo.cover
            isDone = todo.isDone
            createdAt = todo.createdAt
            updatedAt = todo.updatedAt
        }
        todoDAO.id.value.toString()
    }

    override suspend fun update(userId: String, todoId: String, newTodo: Todo): Boolean = suspendTransaction {
        val todoDAO = TodoDAO
            .find {
                (TodoTable.id eq UUID.fromString(todoId)) and
                        (TodoTable.userId eq UUID.fromString(userId))
            }
            .limit(1)
            .firstOrNull()

        if (todoDAO != null) {
            todoDAO.title = newTodo.title
            todoDAO.description = newTodo.description
            todoDAO.cover = newTodo.cover
            todoDAO.isDone = newTodo.isDone
            todoDAO.updatedAt = newTodo.updatedAt
            true
        } else {
            false
        }
    }

    override suspend fun delete(userId: String, todoId: String): Boolean = suspendTransaction {
        val rowsDeleted = TodoTable.deleteWhere {
            (TodoTable.id eq UUID.fromString(todoId)) and
                    (TodoTable.userId eq UUID.fromString(userId))
        }
        rowsDeleted >= 1
    }
}