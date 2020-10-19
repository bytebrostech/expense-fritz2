package app.database

import app.model.*
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction

object UsersTable: UUIDTable() {
    val username = varchar("username", 50).uniqueIndex()
    val oid = varchar("oid", 300)
}

class UserEntity(id: EntityID<UUID>): UUIDEntity(id) {
    companion object: UUIDEntityClass<UserEntity>(UsersTable) {
        fun getOrCreate(id: String): UserEntity = transaction {
            val userId = UserEntity.count()
            find { UsersTable.oid eq id }.firstOrNull() ?: UserEntity.new {
                username = "User-${userId}"
                oid = id
            }
        }
    }

    var username by UsersTable.username
    var oid by UsersTable.oid

    fun toUser() = database { User(this@UserEntity.id.value.toString(), username) }
    fun toVerboseUser() = database { VerboseUser(this@UserEntity.id.value.toString(), username, oid) }

}

data class VerboseUser(val id: String, val username: String, val oid: String)