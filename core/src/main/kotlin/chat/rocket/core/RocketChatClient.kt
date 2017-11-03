package chat.rocket.core

import chat.rocket.common.internal.RestResult
import chat.rocket.common.util.Logger
import chat.rocket.common.util.PlatformLogger
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

class RocketChatClient private constructor(var httpClient: OkHttpClient,
                                           var restUrl: HttpUrl,
                                           var websocketUrl: String,
                                           var tokenProvider: TokenProvider,
                                           var logger: Logger) {

    var moshi: Moshi = Moshi.Builder()
                        .add(RestResult.JsonAdapterFactory())
                        .add(KotlinJsonAdapterFactory())
                        .build()

    private constructor(builder: Builder) : this(builder.httpClient, builder.restUrl,
            builder.websocketUrl, builder.tokenProvider, Logger(builder.platformLogger))

    companion object {
        fun create(init: Builder.() -> Unit) = Builder(init).build()
    }

    class Builder private constructor() {
        constructor(init: Builder.() -> Unit) : this() {
            init()
        }

        lateinit var httpClient: OkHttpClient
        lateinit var restUrl: HttpUrl
        lateinit var websocketUrl: String
        lateinit var tokenProvider : TokenProvider
        lateinit var platformLogger: PlatformLogger

        fun httpClient(init: Builder.() -> OkHttpClient) = apply { httpClient = init() }

        fun restUrl(init: Builder.() -> HttpUrl) = apply { restUrl = init() }

        fun websocketUrl(init: Builder.() -> String) = apply { websocketUrl = init() }

        fun tokenProvider(init: Builder.() -> TokenProvider) = apply { tokenProvider = init() }

        fun platformLogger(init: Builder.() -> PlatformLogger) = apply { platformLogger = init() }

        fun build() = RocketChatClient(this)
    }
}