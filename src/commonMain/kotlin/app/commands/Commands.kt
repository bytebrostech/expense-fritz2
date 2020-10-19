package app.commands

import app.model.Game
import app.model.Guess
import app.model.NewGame
import dev.fritz2.serialization.Serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.internal.StringDescriptor

@Serializable(with = CommandTypeSerializer::class)
enum class CommandType {
    EMPTY,
    LOAD_GAMES,
    GUESS,
    SET_GAME,
    SET_GAMES,
    SET_CHALLENGE,
    SET_CHALLENGES,
    NEW_GAME,
    SET_USER,
}

@kotlinx.serialization.Serializer(forClass = CommandType::class)
object CommandTypeSerializer {
    override val descriptor: SerialDescriptor
        get() = StringDescriptor   // 2
    override fun deserialize(decoder: Decoder): CommandType {  // 3
        return CommandType.valueOf(decoder.decodeString().toUpperCase())
    }
    override fun serialize(encoder: Encoder, obj: CommandType) {  // 4
        encoder.encodeString(obj.name.toLowerCase())
    }
}

data class GameGuess(val gameId: String, val guess: String)

data class GameCommand(val type: String, val payload: Any?)

data class GuessCommand(val payload: GameGuess) { val type: String = CommandType.GUESS.toString() }
data class NewGameCommand(val payload: NewGame) { val type: String = CommandType.NEW_GAME.toString() }
data class SetGamesCommand(val payload: List<Game>) { val type: String = CommandType.SET_GAMES.toString() }
data class SetGameCommand(val payload: Game) { val type: String = CommandType.SET_GAME.toString() }
data class LoadGamesCommand(val payload: String) { val type: String = CommandType.LOAD_GAMES.toString() }
data class SetChallengeCommand(val payload: Game) { val type: String = CommandType.SET_CHALLENGE.toString() }
data class SetChallengesCommand(val payload: List<Game>) { val type: String = CommandType.SET_CHALLENGES.toString() }
data class SetUserCommand(val payload: String) { val type: String = CommandType.SET_USER.toString() }
class EmptyCommand { val payload: Any? = null; val type: String = CommandType.EMPTY.toString() }

open class GameCommands(val type: String, val payload: Any? = null) {
    companion object {
        fun SetGames(games: List<Game>) = SetGamesCommand(games)
        fun SetGame(game: Game) = SetGameCommand(game)
        fun SetChallenges(games: List<Game>) = SetChallengesCommand(games)
        fun SetChallenge(game: Game) = SetChallengeCommand(game)
        fun SetUser(userId: String) = SetUserCommand(userId)

        fun NewGame(newGame: NewGame) = NewGameCommand(newGame)
        fun LoadGames(userId: String) = LoadGamesCommand(userId)
        fun Guess(gameGuess: GameGuess) = GuessCommand(gameGuess)
        fun Empty() = EmptyCommand()
    }
}
