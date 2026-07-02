package com.polli.android.notes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polli.android.theme.LabColors

@Composable
fun NoteMarkdownPreview(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = LabColors.White85,
    mutedColor: Color = LabColors.White33,
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }
    Column(modifier = modifier.fillMaxWidth()) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> {
                    val size = when (block.level) {
                        1 -> 22.sp
                        2 -> 19.sp
                        else -> 17.sp
                    }
                    Text(
                        text = inlineMarkdown(block.text, textColor, mutedColor),
                        color = textColor,
                        fontSize = size,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                    )
                }
                is MdBlock.Paragraph -> {
                    Text(
                        text = inlineMarkdown(block.text, textColor, mutedColor),
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
                is MdBlock.Bullet -> {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = mutedColor)) { append("• ") }
                            append(inlineMarkdown(block.text, textColor, mutedColor))
                        },
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
                    )
                }
                is MdBlock.CodeBlock -> {
                    Text(
                        text = block.text,
                        color = textColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                    )
                }
            }
        }
    }
}

private sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class Bullet(val text: String) : MdBlock
    data class CodeBlock(val text: String) : MdBlock
}

private fun parseMarkdownBlocks(source: String): List<MdBlock> {
    if (source.isBlank()) return emptyList()
    val blocks = mutableListOf<MdBlock>()
    val lines = source.replace("\r\n", "\n").split('\n')
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        when {
            line.trimStart().startsWith("```") -> {
                val buffer = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                    if (buffer.isNotEmpty()) buffer.append('\n')
                    buffer.append(lines[i])
                    i++
                }
                blocks += MdBlock.CodeBlock(buffer.toString())
                if (i < lines.size) i++
            }
            line.startsWith("### ") -> {
                blocks += MdBlock.Heading(3, line.removePrefix("### ").trim())
                i++
            }
            line.startsWith("## ") -> {
                blocks += MdBlock.Heading(2, line.removePrefix("## ").trim())
                i++
            }
            line.startsWith("# ") -> {
                blocks += MdBlock.Heading(1, line.removePrefix("# ").trim())
                i++
            }
            line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> {
                blocks += MdBlock.Bullet(line.trimStart().drop(2).trim())
                i++
            }
            line.isBlank() -> i++
            else -> {
                val buffer = StringBuilder(line.trim())
                i++
                while (i < lines.size && lines[i].isNotBlank() && !isBlockStarter(lines[i])) {
                    buffer.append(' ').append(lines[i].trim())
                    i++
                }
                blocks += MdBlock.Paragraph(buffer.toString())
            }
        }
    }
    return blocks
}

private fun isBlockStarter(line: String): Boolean {
    val t = line.trimStart()
    return t.startsWith("# ") || t.startsWith("## ") || t.startsWith("### ") ||
        t.startsWith("- ") || t.startsWith("* ") || t.startsWith("```")
}

private fun inlineMarkdown(text: String, textColor: Color, mutedColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end > i) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textColor)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("*", i) && !text.startsWith("**", i) -> {
                    val end = text.indexOf('*', i + 1)
                    if (end > i) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = textColor)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("`", i) -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > i) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                color = textColor,
                                background = mutedColor.copy(alpha = 0.25f),
                            ),
                        ) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("[", i) -> {
                    val close = text.indexOf(']', i)
                    val openParen = if (close > i) text.indexOf('(', close) else -1
                    val closeParen = if (openParen > close) text.indexOf(')', openParen) else -1
                    if (close > i && openParen == close + 1 && closeParen > openParen) {
                        val label = text.substring(i + 1, close)
                        withStyle(SpanStyle(color = textColor, textDecoration = TextDecoration.Underline)) {
                            append(label)
                        }
                        i = closeParen + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}
