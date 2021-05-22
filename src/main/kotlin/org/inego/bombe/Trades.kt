package org.inego.bombe

import androidx.compose.desktop.AppManager
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.window.Notifier
import com.bcs.bm.tradesstore.api.grpc.Trade
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileOutputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.*
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.io.path.absolute
import kotlin.io.path.nameWithoutExtension


object TradeDecoder : BaseDecoder() {
    override val tableName: String = "trades"
    override fun parseFrom(bytes: ByteArray): Trade = Trade.parseFrom(bytes)
}


fun sqlQueryForLastWeek(): String {

    val startOfLastWeek = LocalDate.now().with(DayOfWeek.MONDAY).minusWeeks(1)
    val endOfLastWeek = startOfLastWeek.with(DayOfWeek.SUNDAY)

    return """
        select
            trade_date, class_code, trade_num, operation, client_code, order_num,
            encode(content, 'base64')
        from
            trades
        where trade_date between '$startOfLastWeek' and '$endOfLastWeek'
            and position('LKW_'::bytea in content) > 0
    """.trimIndent()
}



@Composable
fun TradeContextButtons(modifier: Modifier = Modifier) {

    val notifier = Notifier()

    val clipboardManager = LocalClipboardManager.current

    Surface(modifier, color = Color.Yellow) {
        Row {
            TextButton({
                val sqlQueryForLastWeek = sqlQueryForLastWeek()
                val annotatedString = buildAnnotatedString {
                    append(sqlQueryForLastWeek)
                }
                clipboardManager.setText(annotatedString)
                notifier.notify("Copied to clipboard", sqlQueryForLastWeek)
            }) {
                Text("Copy query")
            }

            val convertEnabled = remember { mutableStateOf(true) }
            val addStr = remember { mutableStateOf("") }

            TextButton({
                convertCsvToExcel(convertEnabled, addStr)
            }, enabled = convertEnabled.value) {
                Text("CSV → xlsx ${addStr.value}")
            }
        }
    }
}


fun workingDir() = Paths.get("").absolute()

private val chooser = JFileChooser().apply {
    isAcceptAllFileFilterUsed = false
    currentDirectory = workingDir().toFile()
    addChoosableFileFilter(FileNameExtensionFilter("CSV files", "csv"))
}

val focusedWindow = AppManager.focusedWindow?.window


private fun convertCsvToExcel(convertEnabled: MutableState<Boolean>, addStr: MutableState<String>) {

    convertEnabled.value = false

    try {
        val saveIntResult = chooser.showOpenDialog(focusedWindow)

        if (saveIntResult == JFileChooser.APPROVE_OPTION) {
            val path = chooser.selectedFile.toPath()

            GlobalScope.launch {
                convertForStats(path, addStr)
                convertEnabled.value = true
            }
        }
    }
    catch (e: Exception) {
        JOptionPane.showMessageDialog(null, e.localizedMessage, "Error", JOptionPane.ERROR_MESSAGE)
        convertEnabled.value = true
    }
}


private fun convertForStats(inputCsv: Path, addStr: MutableState<String>) {

    val workbook = XSSFWorkbook()
    val sheet = workbook.createSheet("trades")

    val helper = workbook.creationHelper
    val dateStyle = workbook.createCellStyle().apply {
        dataFormat = helper.createDataFormat().getFormat("yyyy-mm-dd")
    }

    val decoder = Base64.getMimeDecoder()

    fun autosize() {
        for (i in 0..8) {
            addStr.value = "autosizing $i..."
            sheet.autoSizeColumn(i)
        }
    }

    var counter = 0

    csvReader().open(inputCsv.toFile()) {

        readAllWithHeaderAsSequence().forEachIndexed { idx, map ->

            counter = idx

            if (idx % 1_000 == 0) {
                if (idx == 1_000) {
                    autosize()
                } else {
                    addStr.value = "${(idx / 1000)}k"
                }
            }

            val base64String = map["encode"]
            val bytes = decoder.decode(base64String)

            val trade = Trade.parseFrom(bytes)

            val body = trade.body

            val brokerRef = body.brokerRef

            if(!brokerRef.contains("/LKW_")) {
                throw AssertionError(brokerRef)
            }

            val row = sheet.createRow(idx)

            row.createCell(0).apply {
                setCellValue(LocalDate.parse(map["trade_date"]))
                cellStyle = dateStyle
            }
            row.createCell(1).setCellValue(map["class_code"])
            row.createCell(2).setCellValue(body.secCode)
            row.createCell(3).setCellValue(map["trade_num"])
            row.createCell(4).setCellValue(map["operation"])
            row.createCell(5).setCellValue(map["client_code"])
            row.createCell(6).setCellValue(map["order_num"])
            row.createCell(7).setCellValue(body.settleCurrency)
            row.createCell(8).setCellValue(body.value)
            row.createCell(9).setCellValue(brokerRef)
        }
    }

    if (counter < 1_000) {
        autosize()
    }

    val xlsxPath = inputCsv.resolveSibling(inputCsv.nameWithoutExtension + ".xlsx")

    addStr.value = "saving..."

    workbook.use {
        FileOutputStream(xlsxPath.toFile()).use {
            workbook.write(it)
        }
    }

    addStr.value = ""
}
