package com.example.ghostcart.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.ghostcart.app.R
import java.io.File
import java.io.FileOutputStream

object GhostCardImageExporter {
    private const val WIDTH = 3000
    private const val HEIGHT = 1890

    fun export(context: Context, config: WalletConfig, cardholderName: String): Result<Uri> = runCatching {
        val bitmap = render(context, config, cardholderName)
        try {
            saveToPictures(context, bitmap, "ghost-card-${System.currentTimeMillis()}.png")
        } finally {
            bitmap.recycle()
        }
    }

    private fun render(context: Context, config: WalletConfig, cardholderName: String): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val (background, foreground, accent) = when (config.cardTheme) {
            "Light" -> Triple(Color.rgb(241, 241, 237), Color.rgb(5, 5, 5), Color.rgb(100, 214, 74))
            "Ghost Green" -> Triple(Color.rgb(100, 214, 74), Color.rgb(5, 5, 5), Color.WHITE)
            else -> Triple(Color.rgb(5, 5, 5), Color.WHITE, Color.rgb(100, 214, 74))
        }
        val cardBounds = RectF(30f, 30f, WIDTH - 30f, HEIGHT - 30f)
        canvas.drawRoundRect(cardBounds, 150f, 150f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = background })

        val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = foreground
            alpha = 22
            style = Paint.Style.STROKE
            strokeWidth = 10f
        }
        repeat(4) { index ->
            val y = 310f + index * 250f
            val path = Path().apply {
                moveTo(1050f, y)
                cubicTo(1650f, y - 180f, 2300f, y + 180f, 3100f, y - 30f)
            }
            canvas.drawPath(path, wavePaint)
        }

        val watermarkPaint = textPaint(foreground, 220f, Typeface.BOLD).apply { alpha = 18 }
        canvas.save()
        canvas.rotate(-20f, WIDTH / 2f, HEIGHT / 2f)
        canvas.drawText("SIMULATION ONLY", 520f, 1120f, watermarkPaint)
        canvas.restore()

        ContextCompat.getDrawable(context, R.drawable.ghost_cart_icon)?.let { logo ->
            logo.setBounds(190, 170, 510, 490)
            logo.draw(canvas)
        }

        canvas.drawText(config.cardName, 600f, 300f, textPaint(foreground, 104f, Typeface.BOLD))
        drawPill(canvas, "SIMULATION ONLY", WIDTH - 940f, 180f, 710f, 180f, accent, foreground)

        canvas.drawText("CARDHOLDER", 200f, 790f, textPaint(foreground, 46f, Typeface.BOLD).apply { alpha = 150 })
        canvas.drawText(cardholderName, 200f, 925f, textPaint(foreground, 92f, Typeface.BOLD))

        canvas.drawText("GHOST ID", 200f, 1170f, textPaint(foreground, 46f, Typeface.BOLD).apply { alpha = 150 })
        canvas.drawText(config.ghostId, 200f, 1310f, textPaint(foreground, 88f, Typeface.BOLD))

        canvas.drawText("MEMBER SINCE", 2150f, 1170f, textPaint(foreground, 46f, Typeface.BOLD).apply { alpha = 150 })
        canvas.drawText(config.memberSince, 2150f, 1310f, textPaint(foreground, 76f, Typeface.BOLD))

        drawPill(canvas, "NOT A PAYMENT CARD", 200f, 1510f, 1020f, 190f, accent, foreground)
        canvas.drawText(
            "Ghost Membership is an internal simulation feature. It cannot be used for purchases.",
            200f,
            1770f,
            textPaint(foreground, 42f, Typeface.NORMAL).apply { alpha = 170 }
        )
        return bitmap
    }

    private fun drawPill(
        canvas: Canvas,
        text: String,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        background: Int,
        foreground: Int
    ) {
        val bounds = RectF(left, top, left + width, top + height)
        canvas.drawRoundRect(bounds, height / 2f, height / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = background })
        val paint = textPaint(foreground, 50f, Typeface.BOLD)
        val textX = left + (width - paint.measureText(text)) / 2f
        val textY = top + height / 2f - (paint.ascent() + paint.descent()) / 2f
        canvas.drawText(text, textX, textY, paint)
    }

    private fun textPaint(colorValue: Int, size: Float, style: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorValue
        textSize = size
        typeface = Typeface.create(Typeface.SANS_SERIF, style)
    }

    private fun saveToPictures(context: Context, bitmap: Bitmap, fileName: String): Uri {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Ghost Cart")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Unable to create the Ghost Card image")
            try {
                resolver.openOutputStream(uri)?.use { output ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                } ?: error("Unable to open the Ghost Card image")
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                return uri
            } catch (error: Throwable) {
                resolver.delete(uri, null, null)
                throw error
            }
        }

        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "Ghost Cart"
        ).apply { mkdirs() }
        val file = File(directory, fileName)
        FileOutputStream(file).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
        return Uri.fromFile(file)
    }
}
