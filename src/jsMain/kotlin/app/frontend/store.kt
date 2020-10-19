package app.frontend

import dev.fritz2.binding.RootStore
import dev.fritz2.binding.handledBy
import dev.fritz2.binding.watch
import dev.fritz2.repositories.Resource
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

open class SocketStore<T>(
    initialData: T,
    override val id: String = "", socket: Socket): RootStore<T>(initialData, id) {
    val session = socket.connect()
}

fun <T, I> SocketStore<List<T>>.syncWith(socket: Socket, resource: Resource<T, I>) {
    val session = socket.connect()
    session.messages.body.map {
        resource.serializer.readList(it)
    } handledBy update
}