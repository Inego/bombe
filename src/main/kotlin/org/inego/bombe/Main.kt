package org.inego.bombe

import androidx.compose.desktop.AppWindow
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import com.fasterxml.jackson.core.io.JsonStringEncoder
import javax.swing.SwingUtilities


fun main() {

    var decoder by mutableStateOf<Decoder>(OrderDecoder)
    val decoded = mutableStateOf("")
    val input = mutableStateOf(TextFieldValue())

    fun decode() {

        val text = input.value.text

        val regex = Regex("\"(.*?)\"", RegexOption.DOT_MATCHES_ALL)
        val matches = regex.findAll(text)

        val result = matches
            .map { it.groupValues[1] }
            .map {
                try {
                    decoder.toJsonString(it)
                } catch (e: Exception) {
                    "\"" + String(JsonStringEncoder.getInstance().quoteAsString("<ERROR: ${e.message}>")) + "\""
                }
            }.joinToString(",\n\n")

        decoded.value = "[$result]"
    }

    SwingUtilities.invokeLater {

        val appWindow = AppWindow(title = "Bombe")

        appWindow.show {

            Column {

                MyToggleButton(
                    listOf(OrderDecoder, StopOrderDecoder, TradeDecoder),
                    decoder,
                    { decoder = it },
                ) { element, isCurrent ->
                    Text(
                        element.tableName,
                        color = if (isCurrent) Color.Unspecified else Color.LightGray
                    )
                }

                val clipboardManager = LocalClipboardManager.current

                Row(Modifier.fillMaxWidth()) {

                    val sampleQuery = decoder.sampleQuery

                    SelectionContainer(
                        Modifier.weight(1F)
                    ) {
                        Text(
                            sampleQuery,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                        )
                    }
                    Button({
                        val annotatedString = buildAnnotatedString {
                            append(sampleQuery)
                        }
                        clipboardManager.setText(annotatedString)
                    }) {
                        Text("Copy")
                    }
                }

                Row(Modifier.fillMaxWidth()) {

                    Column(Modifier.weight(1F).fillMaxHeight()) {
                        Button({
                            val annotatedString = clipboardManager.getText()
                            if (annotatedString != null) {
                                input.value = TextFieldValue(annotatedString)
                            }
                        }) { Text("Paste") }
                        TextField(
                            input.value,
                            { input.value = it },
                            Modifier.weight(1F).fillMaxSize()
                        )
                    }

                    Button({ decode() }) {
                        Text("→")
                    }

                    Column(Modifier.weight(1F).fillMaxHeight()) {

                        Row(Modifier.weight(1F).fillMaxWidth()) {

                            val scrollState = rememberScrollState()

                            SelectionContainer(
                                Modifier.weight(1F)
                            ) {

                                Text(
                                    decoded.value,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.fillMaxSize().verticalScroll(
                                        scrollState
                                    )
                                )
                            }

                            if (decoded.value.isNotEmpty()) {
                                VerticalScrollbar(
                                    rememberScrollbarAdapter(scrollState),
                                    Modifier.fillMaxHeight()
                                )
                            }
                        }

                        Button(
                            {
                                val annotatedString = buildAnnotatedString {
                                    append(decoded.value)
                                }
                                clipboardManager.setText(annotatedString)
                            },
                            Modifier.align(Alignment.End)
                        ) {
                            Text("Copy")
                        }
                    }
                }
            }
        }
    }
}
