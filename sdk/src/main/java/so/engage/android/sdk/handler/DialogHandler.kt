package so.engage.android.sdk.handler

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.google.gson.Gson
import so.engage.android.sdk.models.InAppPayload
import so.engage.android.sdk.views.CarouselDialog
import so.engage.android.sdk.views.SimpleDialog

class DialogHandler private constructor() : DialogHandlerInterface {
    companion object {
        private var _instance: DialogHandler? = null

        val instance: DialogHandler
            get() {
                return _instance ?: DialogHandler().also { _instance = it }
            }
    }


    override fun showDialog(context: Context, isCarousel: Boolean) {
        val inAppPayload =
            Gson().fromJson(if (isCarousel) carouselMap else dialogMap, InAppPayload::class.java)

        val composeView = ComposeView(context).apply {
            setContent {
                if (inAppPayload.isCarousel) {
                    CarouselDialog(
                        inAppPayload = inAppPayload,
                        onDismissRequest = {
                            (parent as ViewGroup).removeView(this)
                            println("Closing Carousel")
                        }
                    )
                } else {
                    SimpleDialog(
                        inAppPayload = inAppPayload,
                        onDismissRequest = {
                            (parent as ViewGroup).removeView(this)
                            println("Closing Dialog")
                        }
                    )
                }
            }
        }

        (context as Activity).addContentView(
            composeView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }
}

const val dialogMap = """
    {
            "position": "Center",
            "background": "#FFFFFF",
            "closeBtn": false,
            "txtColor": "#000000",
            "btnColor": "#007BFF",
            "btnTxtColor": "#FFFFFF",
            "borderRadius": 12,
            "contents": [
                [
                    {
                        "type": "Text",
                        "content": "Dialog Title"
                    },
                    {
                        "type": "Image",
                        "url": "https://img.freepik.com/premium-photo/woman-holding-camera-with-hat-her-head-scarf-around-her-neck_1313501-26402.jpg",
                        "width": 100
                    },
                    {
                        "type": "Text",
                        "content": "This is the dialog description. It provides more details to the user."
                    },
                    {
                        "type": "Button",
                        "content": "Continue",
                        "borderRadius": 8,
                        "buttonWidth": "100",
                        "action": "Dismiss"
                    }
                ]
            ]
        }
    """

const val carouselMap = """
    {
            "position": "Carousel",
            "background": "#FFFFFF",
            "closeBtn": false,
            "txtColor": "#000000",
            "btnColor": "#007BFF",
            "btnTxtColor": "#FFFFFF",
            "borderRadius": 12,
            "contents": [
                [
                    {
                        "type": "Text",
                        "content": "Dialog One"
                    },
                    {
                        "type": "Image",
                        "url": "https://img.freepik.com/premium-photo/woman-holding-camera-with-hat-her-head-scarf-around-her-neck_1313501-26402.jpg",
                        "width": 100
                    },
                    {
                        "type": "Text",
                        "content": "This is the dialog description. It provides more details to the user."
                    },
                    {
                        "type": "Button",
                        "content": "Continue",
                        "borderRadius": 8,
                        "buttonWidth": "100",
                        "action": "Dismiss"
                    }
                ],
                [
                    {
                        "type": "Text",
                        "content": "Dialog Two"
                    },
                    {
                        "type": "Image",
                        "url": "https://img.freepik.com/premium-photo/woman-holding-camera-with-hat-her-head-scarf-around-her-neck_1313501-26402.jpg",
                        "width": 100
                    },
                    {
                        "type": "Text",
                        "content": "This is the dialog description. It provides more details to the user."
                    },
                    {
                        "type": "Button",
                        "content": "Continue",
                        "borderRadius": 8,
                        "buttonWidth": "60",
                        "action": "Dismiss"
                    }
                ],
                [
                    {
                        "type": "Text",
                        "content": "Dialog Three"
                    },
                    {
                        "type": "Image",
                        "url": "https://img.freepik.com/premium-photo/woman-holding-camera-with-hat-her-head-scarf-around-her-neck_1313501-26402.jpg",
                        "width": 100
                    },
                    {
                        "type": "Text",
                        "content": "This is the dialog description. It provides more details to the user."
                    },
                    {
                        "type": "Button",
                        "content": "Done",
                        "borderRadius": 8,
                        "buttonWidth": "100",
                        "action": "Dismiss"
                    }
                ]
            ]
        }
    """