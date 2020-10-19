package app.frontend

import app.model.*
import dev.fritz2.binding.*
import dev.fritz2.dom.*
import dev.fritz2.dom.html.*
import dev.fritz2.remote.getBody
import dev.fritz2.remote.remote
import dev.fritz2.repositories.Resource
import dev.fritz2.repositories.localstorage.localStorageEntity
import dev.fritz2.repositories.localstorage.localStorageQuery
import dev.fritz2.repositories.rest.restQuery
import kotlinx.browser.window
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlin.math.abs
import kotlin.math.exp
import kotlin.random.Random
import kotlin.time.ExperimentalTime

var COUNTER = 1L
fun randomId() = ++COUNTER

val transactionResource = Resource(
    Transaction::id,
    TransactionSerializer,
    Transaction()
)
val transactionPrefix = "txns"

@ExperimentalStdlibApi
@ExperimentalCoroutinesApi
object NewTransactionStore : RootStore<NewTransaction>(NewTransaction()) {
    private val localStorage =
        localStorageEntity(transactionResource, transactionPrefix)

    private val transactions = remote("/api/transactions")

    private val validator = NewTransactionValidator()
    private val serializer = NewTransactionSerializer

    val updateAmount = handle<String> { txn, amount ->
        if (amount.toFloatOrNull() == null || amount.tooManyDecimals()) {
            txn
        } else txn.copy(amount=amount)
    }

    val save = handleAndOffer<NewTransaction, Unit> { oldTxn, newTxn ->
        if (validator.isValid(newTxn, "add")) {
            transactions.body(serializer.write(newTxn))
                .contentType("application/json; charset=utf-8")
                .acceptJson().post()
            localStorage.saveOrUpdate(Transaction(randomId(), newTxn.text, newTxn.amount.toFloat())).let {
                offer(Unit)
                NewTransaction()
            }
        } else oldTxn
    }
}

@ExperimentalStdlibApi
@ExperimentalCoroutinesApi
object TransactionsStore : RootStore<List<Transaction>>(emptyList()) {
    private val localStorage =
        localStorageQuery<Transaction, Long, Unit>(transactionResource, transactionPrefix)

    private val query = restQuery<Transaction, Long, Unit>(transactionResource, "/api/transactions")

    val load = handle(execute=query::query)
    val remove = handleAndOffer<Long, Unit> { list, id: Long ->
        query.delete(list, id)
        localStorage.delete(list, id).also {
            offer(Unit)
        }
    }

    init {
        action() handledBy load
    }
}

enum class TransactionFilter {
    ALL,
    EXPENSE,
    INCOME
}

fun List<Transaction>.transactionFilter(f: TransactionFilter): List<Transaction> = when (f) {
        TransactionFilter.ALL -> { it: Transaction -> true }
        TransactionFilter.EXPENSE -> { it: Transaction -> it.isNegative() }
        TransactionFilter.INCOME -> { it: Transaction -> it.isPositive() }
    }.let {
        this.filter { txn -> it(txn) }
    }

fun List<Transaction>.asMoney() = map { it.amount }.reduce { acc, fl -> acc + fl }.asMoney()

fun List<Transaction>.expense(): String = transactionFilter(TransactionFilter.EXPENSE).asMoney()

fun List<Transaction>.income(): String = transactionFilter(TransactionFilter.INCOME).asMoney()


@ExperimentalStdlibApi
@ExperimentalTime
@ExperimentalCoroutinesApi
@FlowPreview
fun main() {

    val transactions = TransactionsStore.data.map { txns -> txns.sortedByDescending { it.id } }
    val income = transactions.map { it.income() }
    val expense = transactions.map { it.expense() }
    val total = transactions.map { it.asMoney() }

    val header = render {
        h2 {
            +"Expense Tracker"
        }
    }

    fun HtmlElements.categorySummary(categoryName: String, className: String, transactions: Flow<String>) {
        div {
            h4 { +categoryName }
            p("money $className") {
                transactions.bind()
            }
        }
    }

    fun HtmlElements.incomeExpense() = div("inc-exp-container") {
        categorySummary("Income", "plus", income)
        categorySummary("Expense", "minus", expense)
    }

    fun HtmlElements.transaction(txn: Transaction): Li {
        return li(if (txn.isNegative()) "minus" else "plus") {
            +txn.text
            span {
                +txn.displayAmount()
            }
            button("delete-btn") {
                +"x"
                attr("role", "button")
                clicks.map { txn.id } handledBy TransactionsStore.remove
            }
        }
    }

    fun HtmlElements.transactionList() = ul("list") {
        transactions.each(Transaction::id).render {
            transaction(it)
        }.bind()
    }

    fun HtmlElements.formControl(fieldName: String, content: Input.() -> Unit) = div("form-control") {
        label {
            `for`=const(fieldName.toLowerCase())
            +fieldName
        }
        input(id="input-$fieldName") {
            content()
        }
    }

    fun HtmlElements.newTransactionForm() = form {
        val textStore = NewTransactionStore.sub(L.NewTransaction.text)
        val amountStore = NewTransactionStore.sub(L.NewTransaction.amount)
        action = const("javascript:void(0);")

        formControl("Text") {
            value=textStore.data
            changes.values() handledBy textStore.update
            type=const("text")
            placeholder= const("Groceries")
            name=const("text")
        }
        formControl("Amount") {
            value=amountStore.data
            changes.values().map { it } handledBy NewTransactionStore.updateAmount
            name=const("amount")
            placeholder= const("38.26")
            type=const("number")
        }
        NewTransactionStore.data.render { newTransaction ->
            button("btn", id="button") {
                +"Add transaction"
                type=const("submit")
                attr("role", "button")
                clicks.map { newTransaction } handledBy NewTransactionStore.save
            }
        }.bind()
    }

    val container = render {
        div("container") {
            h4 { +"Your Balance" }
            h1 { total.bind() }

            h3 { +"History" }
            transactionList()

            incomeExpense()

            h3 { +"Add a new transaction" }
            newTransactionForm()
        }
    }
    append("target", header, container)

    val trigger = merge(NewTransactionStore.save, TransactionsStore.remove)
    trigger handledBy TransactionsStore.load
}
