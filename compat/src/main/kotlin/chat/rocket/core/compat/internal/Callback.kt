package chat.rocket.core.compat.internal

import chat.rocket.common.RocketChatException
import chat.rocket.core.compat.Call
import chat.rocket.core.compat.Callback
import kotlinx.coroutines.experimental.AbstractCoroutine
import kotlinx.coroutines.experimental.CompletedExceptionally
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.newCoroutineContext
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.startCoroutine

@JvmOverloads
public fun <T> callback(context: CoroutineContext = DefaultDispatcher,
                        callback: Callback<T>,
                        block: suspend CoroutineScope.() -> T
): Call {
    /*val newContext = newCoroutineContext(context)
    val job = Job(newContext[Job])
    val coroutine = CallbackCoroutine(newContext + job, callback)
    block.startCoroutine(coroutine, coroutine)
    return Call(job)*/
    return Call(Job())
}

/*private class CallbackCoroutine<T>(
        parentContext: CoroutineContext,
        private val callback: Callback<T>
) : AbstractCoroutine<T>(parentContext, true) {
    @Suppress("UNCHECKED_CAST")
    override fun afterCompletion(state: Any?, mode: Int) {
        if (isCancelled) return
        if (state is CompletedExceptionally) {
            if (state.exception is RocketChatException) {
                callback.onError(state.exception as RocketChatException)
            } else {
                callback.onError(RocketChatException(state.exception.message ?: "Unknown Error",
                        state.exception))
            }
        } else
            callback.onSuccess(state as T)
    }

}*/
