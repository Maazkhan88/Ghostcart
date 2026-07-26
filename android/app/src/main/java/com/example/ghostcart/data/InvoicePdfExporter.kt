package com.example.ghostcart.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class InvoiceLineItem(val name: String, val quantity: Int, val lineTotal: Int)

data class InvoiceData(
    val orderId: String,
    val placedAtMillis: Long,
    val customerName: String,
    val customerEmail: String,
    val deliveryAddress: String,
    val items: List<InvoiceLineItem>,
    val subtotal: Int,
    val promoDiscount: Int,
    val serviceFee: Int,
    val vat: Int,
    val total: Int
)

// A single-page PDF mirroring the in-app "Order placed" invoice card - same
// data, same disclosure, so the downloaded/emailed copy never claims more
// than the app itself does (no real payment, no real delivery).
object InvoicePdfExporter {
    private const val PAGE_WIDTH = 595 // A4 at 72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48f

    fun generate(context: Context, invoice: InvoiceData): Result<Uri> = runCatching {
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        draw(page.canvas, invoice)
        document.finishPage(page)
        try {
            saveToDownloads(context, document, "ghost-cart-invoice-${invoice.orderId}.pdf")
        } finally {
            document.close()
        }
    }

    private fun draw(canvas: Canvas, invoice: InvoiceData) {
        var y = MARGIN + 24f
        val ink = Color.rgb(5, 5, 5)
        val muted = Color.rgb(120, 120, 120)
        val green = Color.rgb(100, 214, 74)

        fun text(value: String, size: Float, style: Int = Typeface.NORMAL, color: Int = ink, x: Float = MARGIN) {
            canvas.drawText(value, x, y, paint(size, style, color))
            y += size + 10f
        }

        text("Ghost Cart Invoice", 22f, Typeface.BOLD)
        text("Simulation only - no real payment or delivery occurred.", 10f, color = muted)
        y += 10f

        text("Order number", 9f, color = muted)
        text(invoice.orderId, 13f, Typeface.BOLD)
        y += 4f

        val dateText = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(invoice.placedAtMillis))
        text("Date & time", 9f, color = muted)
        text(dateText, 12f)
        y += 4f

        if (invoice.customerName.isNotBlank() || invoice.customerEmail.isNotBlank()) {
            text("Customer", 9f, color = muted)
            text(listOf(invoice.customerName, invoice.customerEmail).filter { it.isNotBlank() }.joinToString(" · "), 12f)
            y += 4f
        }

        if (invoice.deliveryAddress.isNotBlank()) {
            text("Delivery address", 9f, color = muted)
            invoice.deliveryAddress.lines().forEach { line -> text(line, 11f) }
            y += 4f
        }

        y += 8f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint(1f, Typeface.NORMAL, Color.LTGRAY))
        y += 22f

        text("Order summary", 12f, Typeface.BOLD)
        y += 4f
        invoice.items.forEach { item ->
            val label = "${item.name} (x${item.quantity})"
            canvas.drawText(label, MARGIN, y, paint(11f, Typeface.NORMAL, ink))
            val amount = "${Marketplace.currency} ${item.lineTotal}"
            canvas.drawText(amount, PAGE_WIDTH - MARGIN - paint(11f).measureText(amount), y, paint(11f, Typeface.NORMAL, ink))
            y += 20f
        }

        y += 4f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint(1f, Typeface.NORMAL, Color.LTGRAY))
        y += 22f

        fun summaryLine(label: String, amount: Int, color: Int = ink, bold: Boolean = false) {
            val amountText = "${Marketplace.currency} $amount"
            val size = if (bold) 13f else 11f
            val style = if (bold) Typeface.BOLD else Typeface.NORMAL
            canvas.drawText(label, MARGIN, y, paint(size, style, if (bold) ink else muted))
            canvas.drawText(amountText, PAGE_WIDTH - MARGIN - paint(size, style).measureText(amountText), y, paint(size, style, color))
            y += size + 12f
        }

        summaryLine("Subtotal", invoice.subtotal)
        if (invoice.promoDiscount > 0) summaryLine("Promo Discount (GHOST10)", -invoice.promoDiscount, color = green)
        if (invoice.serviceFee > 0) summaryLine("Service Fee", invoice.serviceFee)
        if (invoice.vat > 0) summaryLine("VAT (5%)", invoice.vat)
        y += 6f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint(1f, Typeface.NORMAL, Color.LTGRAY))
        y += 22f
        summaryLine("Total", invoice.total, bold = true)

        y += 20f
        text("Payment method: Simulation only", 10f, color = muted)
        y += 20f
        text(
            "This is a simulation-only invoice. Ghost Cart does not process real payments, place real orders, or deliver products.",
            9f,
            color = muted
        )
    }

    private fun paint(size: Float, style: Int = Typeface.NORMAL, color: Int = Color.BLACK) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            typeface = Typeface.create(Typeface.SANS_SERIF, style)
            this.color = color
        }

    private fun saveToDownloads(context: Context, document: PdfDocument, fileName: String): Uri {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Ghost Cart")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Unable to create the invoice PDF")
            try {
                resolver.openOutputStream(uri)?.use { output -> document.writeTo(output) }
                    ?: error("Unable to open the invoice PDF")
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                return uri
            } catch (error: Throwable) {
                resolver.delete(uri, null, null)
                throw error
            }
        }

        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Ghost Cart")
            .apply { mkdirs() }
        val file = File(directory, fileName)
        FileOutputStream(file).use { output -> document.writeTo(output) }
        return Uri.fromFile(file)
    }
}
