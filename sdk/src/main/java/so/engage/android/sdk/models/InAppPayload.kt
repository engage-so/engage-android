package so.engage.android.sdk.models

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import so.engage.android.sdk.utils.fromHex

enum class ContentType {
    Text, Image, Button, Row
}

data class InAppPayload(
    val position: Position,
    val background: String,
    val closeBtn: Boolean = false,
    val txtColor: String,
    val btnColor: String,
    val btnTxtColor: String,
    val borderRadius: Int? = null, // Inapplicable to carousel
    val vAlign: Align? = null, // Carousel only
    val contents: List<List<Content>>
) {
    val isCarousel: Boolean
        get() = position == Position.Carousel

    val backgroundColor: Color
        get() = Color.fromHex(background)

    val radius: Dp
        get() = (borderRadius ?: 16).dp

    val textColor: Color
        get() = Color.fromHex(txtColor)

    val buttonColor: Color
        get() = Color.fromHex(btnColor)

    val buttonTextColor: Color
        get() = Color.fromHex(btnTxtColor)

    val alignment: Alignment
        get() {
            return when (position) {
                Position.Top -> Alignment.TopCenter
                Position.Bottom -> Alignment.BottomCenter
                else -> Alignment.Center
            }
        }
}

// Content class with optional properties
data class Content(
    val type: ContentType,

    // Properties for Txt type
    val content: String? = null,

    // Properties for Image type
    val url: String? = null,
    val width: Int? = null, // Percentage; 100 default

    // Properties for Button type
    val borderRadius: Int? = null,
    val buttonWidth: String? = null, // "auto" or percentage as string
    val action: Action? = null,
    val actionObj: String? = null,

    // Properties for Row type
    val align: Align? = null,
    val contents: List<Content>? = null
) {
    val buttonRadius: Dp
        get() = (borderRadius ?: 8).dp

    val buttonFillWidth: Boolean
        get() = buttonWidth != "auto"

    val buttonMaxWidth: Float
        get() {
            return if (buttonWidth != null && buttonWidth.toIntOrNull() != null) {
                buttonWidth.toInt() / 100f
            } else {
                1f
            }
        }

    val imageMaxWidth: Float
        get() {
            return if (width != null) {
                width / 100f
            } else {
                1f
            }
        }
}

enum class Position {
    Top, Bottom, Center, Carousel
}

enum class Align {
    Between, End
}

enum class Action {
    Dismiss, Permission, Intent, Url, Go
}
