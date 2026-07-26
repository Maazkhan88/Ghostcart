package com.example.ghostcart.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Fires automatically right after a simulated checkout completes - the
// invoice is emailed by default, not only when the user taps "Share /
// Email again" (that button just re-sends/re-generates the same PDF).
// Sign-in only; a guest's order never had a real email to send to.
object InvoiceEmailRepository {

    suspend fun sendInvoiceEmail(context: Context, invoice: InvoiceData): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = AuthRepository.getToken(context) ?: return@runCatching Unit
            val items = JSONArray()
            invoice.items.forEach { item ->
                items.put(
                    JSONObject()
                        .put("name", item.name)
                        .put("quantity", item.quantity)
                        .put("lineTotalCents", item.lineTotal.toLong() * 100)
                )
            }
            val payload = JSONObject()
                .put("orderId", invoice.orderId)
                .put("placedAtMillis", invoice.placedAtMillis)
                .put("deliveryAddress", invoice.deliveryAddress)
                .put("items", items)
                .put("subtotalCents", invoice.subtotal.toLong() * 100)
                .put("promoDiscountCents", invoice.promoDiscount.toLong() * 100)
                .put("serviceFeeCents", invoice.serviceFee.toLong() * 100)
                .put("vatCents", invoice.vat.toLong() * 100)
                .put("totalCents", invoice.total.toLong() * 100)
            request("/api/me/simulated-orders/invoice-email", "POST", token, payload)
            Unit
        }
    }

    private fun request(path: String, method: String, token: String?, body: JSONObject?): JSONObject {
        val connection = (URL("${ApiConfig.BASE_URL}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Accept", "application/json")
            token?.let { setRequestProperty("Authorization", "Bearer $it") }
            connectTimeout = ApiConfig.CONNECT_TIMEOUT_MS
            readTimeout = ApiConfig.READ_TIMEOUT_MS
            if (body != null) {
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }
        }
        return try {
            body?.let { value -> connection.outputStream.use { it.write(value.toString().toByteArray()) } }
            val status = connection.responseCode
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = if (text.isBlank()) JSONObject() else JSONObject(text)
            if (status !in 200..299) throw Exception(json.optString("error", "Request failed ($status)"))
            json
        } finally {
            connection.disconnect()
        }
    }
}
