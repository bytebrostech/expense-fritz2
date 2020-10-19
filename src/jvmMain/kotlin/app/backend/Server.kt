package app.backend

import app.auth.googleOauthProvider
import app.commands.*
import app.database.*
import app.model.*
import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.routing.header
import io.ktor.server.netty.*
import io.ktor.sessions.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.mapNotNull
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.update
import java.io.File
import java.util.*
import kotlin.collections.LinkedHashSet
import kotlin.text.toCharArray

val validator = NewTransactionValidator()

private fun ApplicationCall.redirectUrl(path: String): String {
    val defaultPort = if (request.origin.scheme == "http") 80 else 443
    val hostPort = request.host()!! + request.port().let { port -> if (port == defaultPort) "" else ":$port" }
    val protocol = request.origin.scheme
    return "$protocol://$hostPort$path"
}

class GameSession(val userId: String, val username: String)

fun Application.main() {
    val currentDir = File(".").absoluteFile
    environment.log.info("Current directory: $currentDir")

    install(ContentNegotiation) {
        gson()
    }

    install(Authentication) {
        oauth("google-oauth") {
            client = HttpClient(Apache)
            providerLookup = { googleOauthProvider }
            urlProvider = { redirectUrl("/login") }
        }
    }

    install(Sessions) {
        cookie<GameSession>("hangmanLiveSessionId", storage = SessionStorageMemory()) {
            val secretSignKey = hex("000102030405060708090a0b0c0d0e0f") // @TODO: Remember to change this!
            transform(SessionTransportTransformerMessageAuthentication(secretSignKey))
        }
    }

    install(WebSockets)

    Database.connect("jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1;", "org.h2.Driver")

    database {
        SchemaUtils.drop(TransactionsTable, UsersTable)
        SchemaUtils.create(UsersTable, TransactionsTable)
    }

    routing {

        route("/api") {
            route("/transactions") {
                get {
                    environment.log.info("getting all Transactions")
                    call.respond(TransactionsDB.all())
                }

                post {
                    val newTransaction = call.receive<NewTransaction>()
                    if (validator.isValid(newTransaction, "add")) {
                        environment.log.info("save new transaction: $newTransaction")
                        call.respond(HttpStatusCode.Created, TransactionsDB.add(newTransaction))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "data is not valid"))
                    }
                }
            }
        }

        route("/me") {
            get {
                val session = call.sessions.get<GameSession>()
                if (session == null) {
                    call.respond(User("", ""))
                } else {
                    val user = database {
                        UserEntity.findById(UUID.fromString(session.userId))
                    }
                    if (user == null) {
                        call.respond(User("", ""))
                    } else {
                        call.respond(user.toUser())
                    }
                }
            }
        }

        route("/logout") {
            get {
                call.sessions.clear<GameSession>()
                call.respondRedirect("/")
            }
        }

        authenticate("google-oauth") {
            route("/login") {
                handle {
                    val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
                        ?: error("No principal")

                    val json = HttpClient(Apache).get<String>("https://www.googleapis.com/userinfo/v2/me") {
                        header("Authorization", "Bearer ${principal.accessToken}")
                    }

                    val data = Gson().fromJson(json, Map::class.java)
                    val id = data["id"] as String? ?: ""
                    println(id)
                    val userEntity = UserEntity.getOrCreate(id)

                    call.sessions.set(GameSession(userEntity.id.value.toString(), userEntity.username))
                    call.respondRedirect("/index.html")
                }
            }
        }

        static("/") {
            resources("/static/")
            defaultResource("/static/index.html")
        }



//        get("/users") {
//            val users = database {
//                UserEntity.all().map { it.toVerboseUser() }
//            }
//            call.respond(users)
//        }
    }
}

fun main(args: Array<String>): Unit = EngineMain.main(args)