package com.infoproject1

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class ReportData(
    val headerTitle: String,              // pl. "Ellenőrzési jegyzőkönyv"
    val inspectorName: String,
    val inspectorCode: String,
    val driverName: String,
    val driverCode: String,
    val line: String,
    val startLoc: String,
    val startTime: String,
    val endLoc: String,
    val endTime: String,
    val dateStr: String,                  // "yyyy.MM.dd"
    val positives: List<String>,
    val negatives: List<String>,
    val notes: String
)

object PdfLayout {

    // A4 pontosan: 595x842 pt (Portrait)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    // Margók és grid
    private const val MARGIN_L = 40f
    private const val MARGIN_R = 40f
    private const val MARGIN_T = 48f
    private const val MARGIN_B = 48f
    private const val COL_GAP = 24f

    private val black = Color.BLACK
    private val grey = Color.rgb(90, 90, 90)

    fun render(context: Context, data: ReportData): File {
        val pdf = PdfDocument()
        var pageNumber = 1
        var page = startPage(pdf, pageNumber)
        var canvas = page.canvas

        // Festékek/stílusok
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
            color = black
        }

        val h2Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 14f
            color = black
        }

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 11.5f
            color = black
        }

        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 9.5f
            color = grey
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = grey
            strokeWidth = 0.8f
        }

        // Oldalhasznos terület
        val xLeft = MARGIN_L
        val xRight = PAGE_WIDTH - MARGIN_R
        val contentWidth = xRight - xLeft

        var y = MARGIN_T

        // ---------- FEJLÉC ----------
        y = drawHeader(canvas, data, titlePaint, smallPaint, linePaint, xLeft, xRight, y)

        // ---------- META BLOKK (kulcs-érték) ----------
        y += 12f
        val kvGap = 6f
        val kv = listOf(
            "Auditor" to "${data.inspectorName} (${data.inspectorCode})",
            "Ellenörző személy" to "${data.driverName} (${data.driverCode})",
            "Szervezeti Egység" to data.line,
            "Szakasz" to "${data.startTime} ${data.startLoc}  –  ${data.endTime} ${data.endLoc}"
        )
        for ((k, v) in kv) {
            val h = drawKeyValue(canvas, k, v, h2Paint, bodyPaint, xLeft, xRight, y, contentWidth)
            y += h + kvGap
            // Szükség esetén új oldal
            if (y > PAGE_HEIGHT - MARGIN_B - 140f) {
                drawFooter(canvas, pageNumber, smallPaint, xLeft, xRight)
                pdf.finishPage(page)
                pageNumber++
                page = startPage(pdf, pageNumber)
                canvas = page.canvas
                y = MARGIN_T
            }
        }

        // Választóvonal
        canvas.drawLine(xLeft, y + 6f, xRight, y + 6f, linePaint)
        y += 18f

        // ---------- POZITÍV / NEGATÍV OSZLOPOK ----------
        val colWidth = (contentWidth - COL_GAP) / 2f
        val colLeftX = xLeft
        val colRightX = xLeft + colWidth + COL_GAP

        // Címek
        y = ensureSpace(pdf, page, canvas, y, 28f, smallPaint, xLeft, xRight) { newPage, pn ->
            page = newPage; canvas = page.canvas; pageNumber = pn
        }
        canvas.drawText("Pozitív észrevételek", colLeftX, y, h2Paint)
        canvas.drawText("Negatív észrevételek", colRightX, y, h2Paint)
        y += 12f

        // Listák tördelése oszlopokban
        val leftEndY = drawBulletedListInColumn(
            pdf, page, canvas, data.positives, bodyPaint, smallPaint,
            colLeftX, colLeftX + colWidth, y, xLeft, xRight
        ) { newPage, pn -> page = newPage; canvas = page.canvas; pageNumber = pn }

        val rightEndY = drawBulletedListInColumn(
            pdf, page, canvas, data.negatives, bodyPaint, smallPaint,
            colRightX, colRightX + colWidth, y, xLeft, xRight
        ) { newPage, pn -> page = newPage; canvas = page.canvas; pageNumber = pn }

        y = maxOf(leftEndY, rightEndY) + 10f

        // ---------- MEGJEGYZÉS ----------
        if (data.notes.isNotBlank()) {
            y = ensureSpace(pdf, page, canvas, y, 24f, smallPaint, xLeft, xRight) { newPage, pn ->
                page = newPage; canvas = page.canvas; pageNumber = pn
            }
            canvas.drawText("Megjegyzés", xLeft, y, h2Paint)
            y += 10f
            val wrapped = wrapText(data.notes, bodyPaint, contentWidth)
            for (line in wrapped) {
                y = ensureSpace(pdf, page, canvas, y, bodyPaint.textSize + 5f, smallPaint, xLeft, xRight) { newPage, pn ->
                    page = newPage; canvas = page.canvas; pageNumber = pn
                }
                canvas.drawText(line, xLeft, y, bodyPaint)
                y += bodyPaint.textSize + 5f
            }
        }

        // ---------- LÁBLÉC ----------
        drawFooter(canvas, pageNumber, smallPaint, xLeft, xRight)
        pdf.finishPage(page)

        // Mentés
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Ellenőrzések")
        if (!dir.exists()) dir.mkdirs()
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(dir, "${stamp}_${data.inspectorCode}.pdf")

        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        return file
    }

    // ===== Segédfüggvények =====

    private fun startPage(pdf: PdfDocument, pageNumber: Int): PdfDocument.Page {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        return pdf.startPage(pageInfo)
    }

    private fun drawHeader(
        canvas: Canvas,
        data: ReportData,
        titlePaint: Paint,
        smallPaint: Paint,
        linePaint: Paint,
        xLeft: Float,
        xRight: Float,
        yTop: Float
    ): Float {
        var y = yTop
        // Főcím
        canvas.drawText(data.headerTitle, xLeft, y, titlePaint)
        // Dátum a jobb felső sarokban
        val dateText = "Dátum: ${data.dateStr}"
        val dateWidth = smallPaint.measureText(dateText)
        canvas.drawText(dateText, xRight - dateWidth, y, smallPaint)
        y += 10f

        // Vékony elválasztó
        canvas.drawLine(xLeft, y + 6f, xRight, y + 6f, linePaint)
        return y + 16f
    }

    private fun drawKeyValue(
        canvas: Canvas,
        key: String,
        value: String,
        keyPaint: Paint,
        valPaint: Paint,
        xLeft: Float,
        xRight: Float,
        yTop: Float,
        maxWidth: Float
    ): Float {
        val keyWidth = keyPaint.measureText("$key: ")
        canvas.drawText("$key:", xLeft, yTop, keyPaint)

        val lines = wrapText(value, valPaint, maxWidth - keyWidth)
        var y = yTop
        if (lines.isNotEmpty()) {
            canvas.drawText(lines[0], xLeft + keyWidth, y, valPaint)
            for (i in 1 until lines.size) {
                y += valPaint.textSize + 4f
                canvas.drawText(lines[i], xLeft, y, valPaint) // további sorokat balról kezdjük
            }
        }
        return yTop + (valPaint.textSize + 4f) * (lines.size.coerceAtLeast(1))
    }

    private fun drawBulletedListInColumn(
        pdf: PdfDocument,
        page: PdfDocument.Page,
        canvas: Canvas,
        items: List<String>,
        bodyPaint: Paint,
        smallPaint: Paint,
        xLeft: Float,
        xRight: Float,
        startY: Float,
        globalLeft: Float,
        globalRight: Float,
        onNewPage: (PdfDocument.Page, Int) -> Unit
    ): Float {
        var y = startY
        var currentPage = page
        var pageNum = page.info.pageNumber
        val bullet = "• "

        for (item in items) {
            val wrapped = wrapText(item, bodyPaint, xRight - xLeft - bodyPaint.textSize) // hagyjunk helyet a bulletnek
            val neededHeight = wrapped.size * (bodyPaint.textSize + 4f)

            y = ensureSpace(pdf, currentPage, canvas, y, neededHeight, smallPaint, globalLeft, globalRight) { newPage, pn ->
                currentPage = newPage; pageNum = pn
                onNewPage(newPage, pn)
            }

            // Első sor bullettel
            var first = true
            for (line in wrapped) {
                val drawX = xLeft + if (first) 0f else bodyPaint.textSize // behúzás a további sorokra
                if (first) {
                    canvas.drawText(bullet, xLeft, y, bodyPaint)
                    canvas.drawText(line, xLeft + bodyPaint.textSize, y, bodyPaint)
                    first = false
                } else {
                    canvas.drawText(line, drawX, y, bodyPaint)
                }
                y += bodyPaint.textSize + 4f
            }
            y += 2f
        }
        return y
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int, smallPaint: Paint, xLeft: Float, xRight: Float) {
        val ts = SimpleDateFormat("yyyy.MM.dd. HH:mm:ss", Locale.getDefault()).format(Date())
        val left = "Generálva: $ts"
        val right = "Oldal $pageNumber"
        canvas.drawText(left, xLeft, PAGE_HEIGHT - 20f, smallPaint)
        val w = smallPaint.measureText(right)
        canvas.drawText(right, xRight - w, PAGE_HEIGHT - 20f, smallPaint)
    }

    private fun ensureSpace(
        pdf: PdfDocument,
        page: PdfDocument.Page,
        canvas: Canvas,
        currentY: Float,
        neededHeight: Float,
        smallPaint: Paint,
        xLeft: Float,
        xRight: Float,
        onNewPage: (PdfDocument.Page, Int) -> Unit
    ): Float {
        val limit = PAGE_HEIGHT - MARGIN_B - 24f
        if (currentY + neededHeight <= limit) return currentY
        // lábléc az előző oldalra
        val pn = page.info.pageNumber
        drawFooter(canvas, pn, smallPaint, xLeft, xRight)
        pdf.finishPage(page)
        val newPn = pn + 1
        val newPage = startPage(pdf, newPn)
        onNewPage(newPage, newPn)
        return MARGIN_T
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var line = ""
        for (w in words) {
            val candidate = if (line.isEmpty()) w else "$line $w"
            if (paint.measureText(candidate) <= maxWidth) {
                line = candidate
            } else {
                if (line.isNotEmpty()) lines.add(line)
                line = w
            }
        }
        if (line.isNotEmpty()) lines.add(line)
        return lines
    }
}


