package org.inego.bombe

import com.bcs.bm.ordersstore.service.grpc.Order
import com.bcs.bm.ordersstore.service.grpc.StopOrder
import com.google.protobuf.GeneratedMessageV3
import com.google.protobuf.util.JsonFormat
import java.time.LocalDate
import java.util.*


interface Decoder {
    val tableName: String
    val sampleQuery: String
    fun toJsonString(base64: String): String
}

private val decoder = Base64.getMimeDecoder()
private val printer = JsonFormat.printer()


abstract class BaseDecoder : Decoder {

    private val today: String
        get() = LocalDate.now().toString()

    override val sampleQuery: String
        get() = "SELECT encode(content, 'base64') FROM $tableName WHERE trade_date = '$today' LIMIT 10;"

    override fun toJsonString(base64: String): String {
        val bytes = decoder.decode(base64)

        val messageV3 = parseFrom(bytes)

        return printer.print(messageV3)
    }

    abstract fun parseFrom(bytes: ByteArray): GeneratedMessageV3
}


object OrderDecoder : BaseDecoder() {
    override val tableName: String = "orders"
    override fun parseFrom(bytes: ByteArray): Order = Order.parseFrom(bytes)
}

object StopOrderDecoder : BaseDecoder() {
    override val tableName: String = "stop_orders"
    override fun parseFrom(bytes: ByteArray): StopOrder = StopOrder.parseFrom(bytes)
}
