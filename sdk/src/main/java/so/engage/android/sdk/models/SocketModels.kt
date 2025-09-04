package so.engage.android.sdk.models

import com.google.gson.annotations.SerializedName

data class MessageModel(
    @SerializedName(value = "message_id")
    val messageId: String,
    val from: UserModel,
    val body: String,
    val uid: String,
    val user: String,
    @SerializedName(value = "parent_id")
    val parentId: String,
    val date: String,
    @SerializedName(value = "last_updated")
    val lastUpdated: String,
    val id: String,
    val outbound: Boolean,
    val read: Boolean,
    val cid: String,
    val status: String? = null,
)

data class ThreadModel(
    val id: String,
    val from: UserModel,
    val uid: String,
    val excerpt: String,
    val inbound: Boolean,
    val status: String,
    val read: List<String>,
    val date: String,
    @SerializedName(value = "last_updated")
    val lastUpdated: String
)

data class UserModel(
    var id: String,
    val name: String? = null,
    val avatar: String? = null,
    val email: String? = null,
    var identified: Boolean = false,
)

data class AccountModel(
    var id: String? = null,
    val features: FeaturesModel? = null
)

data class FeaturesModel(
    val help: HelpModel,
    val chat: ChatModel,
    val widget: WidgetModel
)

data class HelpModel(
    val site: String,
    @SerializedName(value = "default_locale")
    val defaultLocale: String,
    val sk: String
)

data class ChatModel(
    val title: String,
    val subtitle: String,
    val welcome: String?,
    @SerializedName(value = "ignore_anonymous")
    val ignoreAnonymous: Boolean,
    val availability: List<Any>,
    val position: String
)

data class WidgetModel(
    @SerializedName(value = "bg_color")
    val bgColor: String,
    @SerializedName(value = "text_color")
    val textColor: String,
    @SerializedName(value = "text_color_soft")
    val textColorSoft: String
)