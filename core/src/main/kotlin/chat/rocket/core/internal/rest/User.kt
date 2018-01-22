package chat.rocket.core.internal.rest

import chat.rocket.common.RocketChatException
import chat.rocket.common.model.BaseResult
import chat.rocket.common.model.User
import chat.rocket.common.util.CalendarISO8601Converter
import chat.rocket.core.RocketChatClient
import chat.rocket.core.internal.RestMultiResult
import chat.rocket.core.internal.RestResult
import chat.rocket.core.internal.model.Subscription
import chat.rocket.core.internal.model.UserPayload
import chat.rocket.core.model.ChatRoom
import chat.rocket.core.model.Myself
import chat.rocket.core.model.Room
import com.squareup.moshi.Types
import kotlinx.coroutines.experimental.async
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

/**
 * Returns the current logged user information, useful to check if the Token from TokenProvider
 * is still valid.
 *
 * @see Myself
 * @see RocketChatException
 */
suspend fun RocketChatClient.me(): Myself {
    val httpUrl = requestUrl(restUrl, "me").build()
    val request = requestBuilder(httpUrl).get().build()

    return handleRestCall(request, Myself::class.java)
}

/**
 * Updates the profile for the user.
 *
 * @param userId The ID of the user to update.
 * @param email The email address for the user.
 * @param name The display name of the user.
 * @param password The password for the user.
 * @param username The username for the user.
 * @return An [User] with an updated profile.
 */
suspend fun RocketChatClient.updateProfile(userId: String,
                                           email: String? = null,
                                           name: String? = null,
                                           password: String? = null,
                                           username: String? = null): User {
    val payload = UserPayload(userId, email, name, password, username,null)
    val adapter = moshi.adapter(UserPayload::class.java)

    val payloadBody = adapter.toJson(payload)
    val body = RequestBody.create(MEDIA_TYPE_JSON, payloadBody)

    val httpUrl = requestUrl(restUrl, "users.update").build()
    val request = requestBuilder(httpUrl).post(body).build()

    val type = Types.newParameterizedType(RestResult::class.java, User::class.java)
    return handleRestCall<RestResult<User>>(request, type).result()
}

/**
 * Resets the user's avatar.
 *
 * @param userId The ID of the user to reset the avatar.
 *
 * @return True if the avatar was reset, false otherwise.
 */
suspend fun RocketChatClient.resetAvatar(userId: String): Boolean {
    val payload = UserPayload(userId, null, null, null, null, null)
    val adapter = moshi.adapter(UserPayload::class.java)

    val payloadBody = adapter.toJson(payload)
    val body = RequestBody.create(MEDIA_TYPE_JSON, payloadBody)

    val httpUrl = requestUrl(restUrl, "users.resetAvatar").build()
    val request = requestBuilder(httpUrl).post(body).build()

    return handleRestCall<BaseResult>(request, BaseResult::class.java).success
}

/**
 * Sets the user's avatar.
 *
 * @param file The file to set the avatar.
 * @param mimeType The MIME type of the file. Allowed MIME types are: *image/gif*, *image/png*, *image/jpeg*, *image/bmp* and *image/webp*.
 *
 * @return True if the avatar was setted up, false otherwise.
 */
suspend fun RocketChatClient.setAvatar(file: File, mimeType: String): Boolean {
    if (mimeType != "image/gif" && mimeType != "image/png" && mimeType != "image/jpeg" && mimeType != "image/bmp" && mimeType != "image/webp") {
        throw RocketChatException("Invalid image type")
    }

    val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image=", file.name,
                    RequestBody.create(MediaType.parse(mimeType), file))
            .build()

    val httpUrl = requestUrl(restUrl, "users.setAvatar").build()
    val request = requestBuilder(httpUrl).post(body).build()

    return handleRestCall<BaseResult>(request, BaseResult::class.java).success
}

/**
 * Sets the user's avatar.
 *
 * @param avatarUrl Url of the avatar for the user
 *
 * @return True if the avatar was setted up, false otherwise.
 */
suspend fun RocketChatClient.setAvatar(avatarUrl: String): Boolean {
    val payload = UserPayload(null, null, null, null, null, avatarUrl)
    val adapter = moshi.adapter(UserPayload::class.java)

    val payloadBody = adapter.toJson(payload)
    val body = RequestBody.create(MEDIA_TYPE_JSON, payloadBody)

    val httpUrl = requestUrl(restUrl, "users.setAvatar").build()
    val request = requestBuilder(httpUrl).post(body).build()

    return handleRestCall<BaseResult>(request, BaseResult::class.java).success
}

suspend fun RocketChatClient.chatRooms(timestamp: Long = 0): RestMultiResult<List<ChatRoom>> {
    val rooms = async { listRooms(timestamp) }
    val subscriptions = async { listSubscriptions(timestamp) }

    return combine(rooms.await(), subscriptions.await())
}

internal fun RocketChatClient.combine(rooms: RestMultiResult<List<Room>>, subscriptions: RestMultiResult<List<Subscription>>): RestMultiResult<List<ChatRoom>> {
    val update = combine(rooms.update, subscriptions.update)
    val remove = combine(rooms.remove, subscriptions.remove)

    return RestMultiResult.create(update, remove)
}

internal fun RocketChatClient.combine(rooms: List<Room>, subscriptions: List<Subscription>): List<ChatRoom> {
    val map = HashMap<String, Room>()
    rooms.forEach {
        map[it.id] = it
    }

    val chatRooms = ArrayList<ChatRoom>(subscriptions.size)

    subscriptions.forEach {
        val room = map[it.roomId]
        val subscription = it
        // In case of any inconsistency we just ignore the room/subscription...
        // This should be a very, very rare situation, like the user leaving/joining a channel
        // between the 2 calls.
        room?.let {
            chatRooms.add(ChatRoom.create(room, subscription, this))
        }
    }

    return chatRooms
}

internal suspend fun RocketChatClient.listSubscriptions(timestamp: Long = 0): RestMultiResult<List<Subscription>> {
    val urlBuilder = requestUrl(restUrl, "subscriptions.get")
    val date = CalendarISO8601Converter().fromTimestamp(timestamp)
    urlBuilder.addQueryParameter("updatedAt", date)

    val request = requestBuilder(urlBuilder.build()).get().build()

    val type = Types.newParameterizedType(RestMultiResult::class.java,
            Types.newParameterizedType(List::class.java, Subscription::class.java))
    val result = handleRestCall<RestMultiResult<List<Subscription>>>(request, type)
    return result
}

internal suspend fun RocketChatClient.listRooms(timestamp: Long = 0): RestMultiResult<List<Room>> {
    val urlBuilder = requestUrl(restUrl, "rooms.get")
    val date = CalendarISO8601Converter().fromTimestamp(timestamp)
    urlBuilder.addQueryParameter("updatedAt", date)

    val request = requestBuilder(urlBuilder.build()).get().build()

    val type = Types.newParameterizedType(RestMultiResult::class.java,
            Types.newParameterizedType(List::class.java, Room::class.java))
    val result = handleRestCall<RestMultiResult<List<Room>>>(request, type)
    return result
}