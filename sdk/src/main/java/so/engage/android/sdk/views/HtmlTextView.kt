package so.engage.android.sdk.views

import android.text.Html
import android.text.Spanned
import android.text.style.URLSpan
import android.text.style.StyleSpan
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.material3.Text
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle

@Composable
fun HtmlTextView(
    html: String,
    modifier: Modifier = Modifier,
    isHtml: Boolean = html.contains("<[a-zA-Z][^>]*>".toRegex()) // Detect HTML tags
) {
    val uriHandler = LocalUriHandler.current

    val annotatedString = if (isHtml) {
        // Parse HTML for formatted text
        val spanned: Spanned = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)

        buildAnnotatedString {
            append(spanned.trim().toString())

            // Apply bold/italic styles
            spanned.getSpans(0, spanned.length, StyleSpan::class.java).forEach { span ->
                val start = spanned.getSpanStart(span)
                val end = spanned.getSpanEnd(span)
                when (span.style) {
                    android.graphics.Typeface.BOLD -> addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold),
                        start,
                        end
                    )
                    android.graphics.Typeface.ITALIC -> addStyle(
                        SpanStyle(fontStyle = FontStyle.Italic),
                        start,
                        end
                    )
                    android.graphics.Typeface.BOLD_ITALIC -> addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                        start,
                        end
                    )
                }
            }

            // Apply clickable links using LinkAnnotation
            spanned.getSpans(0, spanned.length, URLSpan::class.java).forEach { span ->
                val start = spanned.getSpanStart(span)
                val end = spanned.getSpanEnd(span)
                addStyle(
                    SpanStyle(
                        color = Color.Blue, // Customize link color
                        textDecoration = TextDecoration.Underline
                    ),
                    start,
                    end
                )
                addLink(
                    LinkAnnotation.Url(
                        url = span.url,
                        styles = TextLinkStyles(
                            SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline)
                        ),
                        linkInteractionListener = { uriHandler.openUri(span.url) }
                    ),
                    start,
                    end
                )
            }
        }
    } else {
        // Plain text, no HTML
        AnnotatedString(html)
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        style = TextStyle.Default // Customize text style if needed
    )
}