package so.engage.android.sdk.handler

import android.content.Context

interface DialogHandlerInterface {
    fun openChat(context: Context, uid: String)
    fun showDialog(context: Context, isCarousel: Boolean)
}