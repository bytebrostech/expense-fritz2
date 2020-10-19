package app.model

import dev.fritz2.lenses.Lenses

val ATTEMPTS = 5

@Lenses
data class User(
    val id: String = "",
    val username: String = ""
)

@Lenses
enum class GameStatus {
    PLAYING,
    LOST,
    WON
}

@Lenses
data class Guess(
    val letter: String
)

@Lenses
data class ProgressLetter(
    val letter: Char
)

@Lenses
data class Game(
    val id: String = "",
    val player: User = User(),
    val challenger: User = User(),
    val word: String = "",
    val guesses: String = "",
    val status: GameStatus = GameStatus.PLAYING)
    {

    fun guessedLetters(): Set<String> = guesses.toCharArray().map { it.toString() }.toSet()

    fun progress(): List<ProgressLetter> {
        return word.toCharArray().map { if (guessedLetters().contains(it.toString())) it else '_' }.map { ProgressLetter(it) }
    }

    fun errors(): List<String> = (guessedLetters() - word.toCharArray().map {it.toString()}.toSet()).toList()

    fun lost(): Boolean = errors().size > ATTEMPTS

    fun won(): Boolean = (word.toSet() - guessedLetters()).isEmpty()

    fun summary(): String = when(status) {
        GameStatus.WON -> "Won!"
        GameStatus.LOST -> "Lost."
        GameStatus.PLAYING -> {
            when(guessedLetters().size) {
                0 -> "New Game"
                else -> "${ATTEMPTS - errors().size} attempts left"
            }
        }
    }

    fun wrongLetters(): List<String> = errors().map { it }
    fun rightLetters(): List<String> = (guessedLetters() intersect  word.map { it.toString() }.toSet()).toList()
}

@Lenses
data class NewGame(
    val playerId: String = "",
    val challengerId: String = "",
    val word: String = "",
)