package com.infoproject1

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    fun createPdf(context: Context, lines: List<String>): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = 16f

        val margin = 40f
        val contentWidth = pageInfo.pageWidth - margin * 2

        val header = lines.firstOrNull() ?: ""
        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        val headerWidth = paint.measureText(header)
        canvas.drawText(header, pageInfo.pageWidth - margin - headerWidth, margin + 10f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        var y = margin + 60f

        val body = lines.drop(1).joinToString("\n")
        val paragraphs = body.split("\n")

        val lineHeight = 22f
        for (paragraph in paragraphs) {
            val wrapped = wrapText(paragraph, paint, contentWidth)
            for (ln in wrapped) {
                if (y > pageInfo.pageHeight - margin) break
                canvas.drawText(ln, margin, y, paint)
                y += lineHeight
            }
            y += lineHeight / 2
        }

        pdfDocument.finishPage(page)

        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "form.pdf")
        FileOutputStream(file).use { out -> pdfDocument.writeTo(out) }
        pdfDocument.close()
        return file
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        var line = ""
        for (word in text.split(" ")) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) <= maxWidth) {
                line = candidate
            } else {
                if (line.isNotEmpty()) result.add(line)
                line = word
            }
        }
        if (line.isNotEmpty()) result.add(line)
        return result
    }
}
