package app.model

import dev.fritz2.identification.inspect
import dev.fritz2.lenses.Lenses
import dev.fritz2.serialization.Serializer
import dev.fritz2.validation.ValidationMessage
import dev.fritz2.validation.Validator
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.math.abs

@Lenses
@Serializable
data class Transaction(val id: Long = -1L, val text: String = "", val amount: Float = 0.00f) {
    fun isNegative(): Boolean = amount < 0f
    fun isPositive(): Boolean = !isNegative()
    fun sign(): String = if (isNegative()) "-" else "+"
    fun displayAmount(): String = sign() + "\$" + abs(amount)
}

@Lenses
@Serializable
data class NewTransaction(val text: String = "", val amount: String = "")

object TransactionSerializer : Serializer<Transaction, String> {
    override fun read(msg: String): Transaction = Json.decodeFromString(Transaction.serializer(), msg)

    override fun readList(msg: String): List<Transaction> = Json.decodeFromString(ListSerializer(Transaction.serializer()), msg)

    override fun write(item: Transaction): String = Json.encodeToString(Transaction.serializer(), item)

    override fun writeList(items: List<Transaction>): String = Json.encodeToString(ListSerializer(Transaction.serializer()), items)
}

object NewTransactionSerializer : Serializer<NewTransaction, String> {
    override fun read(msg: String): NewTransaction = Json.decodeFromString(NewTransaction.serializer(), msg)

    override fun readList(msg: String): List<NewTransaction> = Json.decodeFromString(ListSerializer(NewTransaction.serializer()), msg)

    override fun write(item: NewTransaction): String = Json.encodeToString(NewTransaction.serializer(), item)

    override fun writeList(items: List<NewTransaction>): String = Json.encodeToString(ListSerializer(NewTransaction.serializer()), items)
}

data class Message(val id: String, val text: String): ValidationMessage {
    override fun isError() = true
}

class NewTransactionValidator: Validator<NewTransaction, Message, String>() {
    override fun validate(data: NewTransaction, metadata: String): List<Message> {
        val msgs = mutableListOf<Message>()
        val inspector = inspect(data)

        val text = inspector.sub(L.NewTransaction.text)
        if(text.data.trim().isBlank()) {
            msgs.add(Message(text.id, "Text cannot be blank"))
        }

        val amount = inspector.sub(L.NewTransaction.amount)
        if(amount.data.toFloatOrNull() == null) {
            msgs.add(Message(amount.id, "Please enter a number"))
        } else if(amount.data.tooManyDecimals()) {
            msgs.add(Message(amount.id, "Please enter valid cents"))
        }

        return msgs
    }

}

fun String.tooManyDecimals(): Boolean {
    val lastCharAt = (length - 1 - 2)
    return if (contains('.')) {
        indexOf('.') < lastCharAt
    } else false
}

@Lenses
data class User(
    val id: String = "",
    val username: String = ""
)

