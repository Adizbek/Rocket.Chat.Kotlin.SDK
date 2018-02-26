package chat.rocket.core.internal.model

import com.squareup.moshi.Json
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
class ChatRoomPayload(@Json(name = "rid") val roomId: String)