package app.database

import app.model.*
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction


object TransactionsTable : LongIdTable() {
    val text = varchar("word", 50)
    val amount = float("amount")
}

class TransactionEntity(id: EntityID<Long>): LongEntity(id) {
    companion object : LongEntityClass<TransactionEntity>(TransactionsTable)

    var text by TransactionsTable.text
    var amount by TransactionsTable.amount

    fun toTransaction() = database { Transaction(this@TransactionEntity.id.value, text, amount) }
}

object TransactionsDB {

    fun find(id: Long): Transaction? = database {
        TransactionEntity.findById(id)?.toTransaction()
    }

    fun all(): List<Transaction> = database {
        TransactionEntity.all().map { it.toTransaction() }
    }

    fun add(newTransaction: NewTransaction): Transaction = database {
        TransactionEntity.new {
            text = newTransaction.text
            amount = newTransaction.amount.toFloat()
        }.toTransaction()
    }

}

fun <T> database(statement: org.jetbrains.exposed.sql.Transaction.() -> T): T {
    return transaction {
        addLogger(StdOutSqlLogger)
        statement()
    }
}