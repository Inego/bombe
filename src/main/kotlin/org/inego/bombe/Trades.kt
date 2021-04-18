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

            TextButton({
                convertCsvToExcel(convertEnabled)
            }, enabled = convertEnabled.value) {
                Text("CSV → xlsx")
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


private fun convertCsvToExcel(convertEnabled: MutableState<Boolean>) {

    convertEnabled.value = false

    try {
        val saveIntResult = chooser.showOpenDialog(focusedWindow)

        if (saveIntResult == JFileChooser.APPROVE_OPTION) {
            val path = chooser.selectedFile.toPath()
            convertForStats(path)
        }
    }
    catch (e: Exception) {
        JOptionPane.showMessageDialog(null, e.localizedMessage, "Error", JOptionPane.ERROR_MESSAGE)
    }
    finally {
        convertEnabled.value = true
    }
}


private fun convertForStats(inputCsv: Path) {

    val workbook = XSSFWorkbook()
    val sheet = workbook.createSheet("trades")

    val helper = workbook.creationHelper
    val dateStyle = workbook.createCellStyle().apply {
        dataFormat = helper.createDataFormat().getFormat("yyyy-mm-dd")
    }

    val decoder = Base64.getMimeDecoder()

    csvReader().open(inputCsv.toFile()) {

        readAllWithHeaderAsSequence().forEachIndexed { idx, map ->

            val base64String = map["encode"]
            val bytes = decoder.decode(base64String)

            val trade = Trade.parseFrom(bytes)

            if(!trade.body.brokerRef.contains("/LKW_")) {
                throw AssertionError(trade.body.brokerRef)
            }

            val row = sheet.createRow(idx)

            row.createCell(0).apply {
                setCellValue(LocalDate.parse(map["trade_date"]))
                cellStyle = dateStyle
            }
            row.createCell(1).setCellValue(map["class_code"])
            row.createCell(2).setCellValue(map["trade_num"])
            row.createCell(3).setCellValue(map["operation"])
            row.createCell(4).setCellValue(map["client_code"])
            row.createCell(5).setCellValue(map["order_num"])
            row.createCell(6).setCellValue(trade.body.settleCurrency)
            row.createCell(7).setCellValue(trade.body.value)
        }
    }

    for (i in 0..7) {
        sheet.autoSizeColumn(i)
    }

    val xlsxPath = inputCsv.resolveSibling(inputCsv.nameWithoutExtension + ".xlsx")

    workbook.use {
        FileOutputStream(xlsxPath.toFile()).use {
            workbook.write(it)
        }
    }
}
