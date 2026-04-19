package com.infoproject1

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.infoproject1.app.R
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class ReportData(
    val headerTitle: String,
    val inspectorName: String,
    val inspectorCode: String,
    val driverName: String,
    val driverCode: String,
    val line: String,
    val startLoc: String,
    val startTime: String,
    val endLoc: String,
    val endTime: String,
    val dateStr: String,
    val positives: List<String>,
    val negatives: List<String>,
    val notes: String
)

object PdfLayout {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    private const val MARGIN_L = 32f
    private const val MARGIN_R = 32f
    private const val MARGIN_T = 32f
    private const val MARGIN_B = 32f
    private const val COL_GAP = 18f

    private val black = Color.BLACK
    private val grey = Color.rgb(90, 90, 90)

    fun render(context: Context, data: ReportData): File {
        val pdf = PdfDocument()
        var pageNumber = 1
        var page = startPage(pdf, pageNumber)
        var canvas = page.canvas

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 16f
            color = black
        }

        val h2Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 12.5f
            color = black
        }

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 11.2f
            color = black
        }

        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 8.8f
            color = grey
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = grey
            strokeWidth = 0.8f
        }

        val xLeft = MARGIN_L
        val xRight = PAGE_WIDTH - MARGIN_R
        val contentWidth = xRight - xLeft

        val logoBitmap = loadHeaderLogo(context)

        var y = MARGIN_T

        y = drawHeader(
            canvas = canvas,
            data = data,
            titlePaint = titlePaint,
            smallPaint = smallPaint,
            linePaint = linePaint,
            xLeft = xLeft,
            xRight = xRight,
            yTop = y,
            logo = logoBitmap
        )

        y += 6f

        val kvGap = 5f
        val kv = listOf(
            "Auditor" to "${data.inspectorName} (${data.inspectorCode})",
            "Ellenőrző személy" to "${data.driverName} (${data.driverCode})",
            "Szervezeti egység" to data.line,
            "Ellenőrzés helye" to "${data.startTime} ${data.startLoc} – ${data.endTime} ${data.endLoc}"
        )

        for ((k, v) in kv) {
            val h = drawKeyValue(
                canvas = canvas,
                key = k,
                value = v,
                keyPaint = h2Paint,
                valPaint = bodyPaint,
                xLeft = xLeft,
                yTop = y,
                maxWidth = contentWidth
            )
            y += h + kvGap

            if (y > PAGE_HEIGHT - MARGIN_B - 140f) {
                drawFooter(canvas, pageNumber, smallPaint, xLeft, xRight)
                pdf.finishPage(page)
                pageNumber++
                page = startPage(pdf, pageNumber)
                canvas = page.canvas
                y = MARGIN_T
            }
        }

        canvas.drawLine(xLeft, y + 2f, xRight, y + 2f, linePaint)
        y += 14f

        val colWidth = (contentWidth - COL_GAP) / 2f
        val colLeftX = xLeft
        val colRightX = xLeft + colWidth + COL_GAP

        y = ensureSpace(pdf, page, canvas, y, 24f, smallPaint, xLeft, xRight) { newPage, pn ->
            page = newPage
            canvas = page.canvas
            pageNumber = pn
        }

        drawSingleLineText(canvas, "Pozitív észrevételek", colLeftX, y, h2Paint)
        drawSingleLineText(canvas, "Negatív észrevételek", colRightX, y, h2Paint)
        y += lineHeight(h2Paint)

        val leftEndY = drawBulletedListInColumn(
            pdf = pdf,
            page = page,
            canvas = canvas,
            items = data.positives,
            bodyPaint = bodyPaint,
            smallPaint = smallPaint,
            xLeft = colLeftX,
            xRight = colLeftX + colWidth,
            startY = y,
            globalLeft = xLeft,
            globalRight = xRight
        ) { newPage, pn ->
            page = newPage
            canvas = newPage.canvas
            pageNumber = pn
        }

        val rightEndY = drawBulletedListInColumn(
            pdf = pdf,
            page = page,
            canvas = canvas,
            items = data.negatives,
            bodyPaint = bodyPaint,
            smallPaint = smallPaint,
            xLeft = colRightX,
            xRight = colRightX + colWidth,
            startY = y,
            globalLeft = xLeft,
            globalRight = xRight
        ) { newPage, pn ->
            page = newPage
            canvas = newPage.canvas
            pageNumber = pn
        }

        y = maxOf(leftEndY, rightEndY) + 8f

        if (data.notes.isNotBlank()) {
            y = ensureSpace(pdf, page, canvas, y, 24f, smallPaint, xLeft, xRight) { newPage, pn ->
                page = newPage
                canvas = newPage.canvas
                pageNumber = pn
            }

            drawSingleLineText(canvas, "Megjegyzés", xLeft, y, h2Paint)
            y += lineHeight(h2Paint)

            val wrapped = wrapText(data.notes, bodyPaint, contentWidth)
            val noteLineHeight = lineHeight(bodyPaint)

            for (line in wrapped) {
                y = ensureSpace(pdf, page, canvas, y, noteLineHeight, smallPaint, xLeft, xRight) { newPage, pn ->
                    page = newPage
                    canvas = newPage.canvas
                    pageNumber = pn
                }
                drawSingleLineText(canvas, line, xLeft, y, bodyPaint)
                y += noteLineHeight
            }
        }

        drawFooter(canvas, pageNumber, smallPaint, xLeft, xRight)
        pdf.finishPage(page)

        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Ellenőrzések")
        if (!dir.exists()) dir.mkdirs()

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(dir, "${stamp}_${data.inspectorCode}.pdf")

        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        return file
    }

    private fun startPage(pdf: PdfDocument, pageNumber: Int): PdfDocument.Page {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        return pdf.startPage(pageInfo)
    }

    private fun loadHeaderLogo(context: Context): Bitmap? {
        return runCatching {
            BitmapFactory.decodeResource(context.resources, R.drawable.fejlecpdf)
        }.getOrNull()
    }

    private fun drawHeader(
        canvas: Canvas,
        data: ReportData,
        titlePaint: Paint,
        smallPaint: Paint,
        linePaint: Paint,
        xLeft: Float,
        xRight: Float,
        yTop: Float,
        logo: Bitmap?
    ): Float {
        var y = yTop

        val logoBoxWidth = 92f
        val logoBoxHeight = 42f
        val logoPadding = 12f

        val reservedRightWidth = if (logo != null) logoBoxWidth + logoPadding else 0f
        val titleMaxWidth = (xRight - xLeft - reservedRightWidth).coerceAtLeast(100f)

        val titleLines = wrapText(data.headerTitle, titlePaint, titleMaxWidth)
        val titleLineHeight = lineHeight(titlePaint)

        var titleBottom = y
        for (line in titleLines) {
            drawSingleLineText(canvas, line, xLeft, titleBottom, titlePaint)
            titleBottom += titleLineHeight
        }

        if (logo != null) {
            val dest = fitRectTopRight(
                bitmapWidth = logo.width.toFloat(),
                bitmapHeight = logo.height.toFloat(),
                boxLeft = xRight - logoBoxWidth,
                boxTop = y,
                boxWidth = logoBoxWidth,
                boxHeight = logoBoxHeight
            )

            val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(logo, null, dest, bitmapPaint)
        }

        y = titleBottom

        drawSingleLineText(canvas, "Dátum: ${data.dateStr}", xLeft, y, smallPaint)
        y += lineHeight(smallPaint) + 2f

        canvas.drawLine(xLeft, y, xRight, y, linePaint)
        return y + 6f
    }

    private fun drawKeyValue(
        canvas: Canvas,
        key: String,
        value: String,
        keyPaint: Paint,
        valPaint: Paint,
        xLeft: Float,
        yTop: Float,
        maxWidth: Float
    ): Float {
        val keyPrefix = "$key: "
        val keyWidth = keyPaint.measureText(keyPrefix)
        val firstLineWidth = (maxWidth - keyWidth).coerceAtLeast(60f)

        val lines = wrapText(value, valPaint, firstLineWidth)
        val valueLineHeight = lineHeight(valPaint)

        drawSingleLineText(canvas, keyPrefix, xLeft, yTop, keyPaint)

        var currentY = yTop
        if (lines.isNotEmpty()) {
            drawSingleLineText(canvas, lines[0], xLeft + keyWidth, currentY, valPaint)
            for (i in 1 until lines.size) {
                currentY += valueLineHeight
                drawSingleLineText(canvas, lines[i], xLeft, currentY, valPaint)
            }
        }

        return (lines.size.coerceAtLeast(1) * valueLineHeight)
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

        val bulletIndent = bodyPaint.measureText("• ") + 2f
        val itemLineHeight = lineHeight(bodyPaint)

        for (item in items) {
            val wrapped = wrapText(item, bodyPaint, (xRight - xLeft - bulletIndent).coerceAtLeast(40f))
            val neededHeight = wrapped.size.coerceAtLeast(1) * itemLineHeight + 2f

            y = ensureSpace(pdf, currentPage, canvas, y, neededHeight, smallPaint, globalLeft, globalRight) { newPage, pn ->
                currentPage = newPage
                onNewPage(newPage, pn)
            }

            var first = true
            for (line in wrapped) {
                if (first) {
                    drawSingleLineText(canvas, "•", xLeft, y, bodyPaint)
                    drawSingleLineText(canvas, line, xLeft + bulletIndent, y, bodyPaint)
                    first = false
                } else {
                    drawSingleLineText(canvas, line, xLeft + bulletIndent, y, bodyPaint)
                }
                y += itemLineHeight
            }

            y += 2f
        }

        return y
    }

    private fun drawFooter(
        canvas: Canvas,
        pageNumber: Int,
        smallPaint: Paint,
        xLeft: Float,
        xRight: Float
    ) {
        val ts = SimpleDateFormat("yyyy.MM.dd. HH:mm:ss", Locale.getDefault()).format(Date())
        val left = "Generálva: $ts"
        val right = "Oldal $pageNumber"

        val baselineY = PAGE_HEIGHT - 16f
        canvas.drawText(left, xLeft, baselineY, smallPaint)
        val w = smallPaint.measureText(right)
        canvas.drawText(right, xRight - w, baselineY, smallPaint)
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

        val pn = page.info.pageNumber
        drawFooter(canvas, pn, smallPaint, xLeft, xRight)
        pdf.finishPage(page)

        val newPn = pn + 1
        val newPage = startPage(pdf, newPn)
        onNewPage(newPage, newPn)

        return MARGIN_T
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")

        val words = text.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var line = ""

        for (word in words) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) <= maxWidth) {
                line = candidate
            } else {
                if (line.isNotEmpty()) lines.add(line)
                line = word
            }
        }

        if (line.isNotEmpty()) lines.add(line)
        return lines
    }

    private fun lineHeight(paint: Paint): Float {
        val fm = paint.fontMetrics
        return (fm.descent - fm.ascent + fm.leading) + 1f
    }

    private fun drawSingleLineText(
        canvas: Canvas,
        text: String,
        x: Float,
        topY: Float,
        paint: Paint
    ) {
        val baseline = topY - paint.fontMetrics.ascent
        canvas.drawText(text, x, baseline, paint)
    }

    private fun fitRectTopRight(
        bitmapWidth: Float,
        bitmapHeight: Float,
        boxLeft: Float,
        boxTop: Float,
        boxWidth: Float,
        boxHeight: Float
    ): RectF {
        val scale = minOf(boxWidth / bitmapWidth, boxHeight / bitmapHeight)
        val drawWidth = bitmapWidth * scale
        val drawHeight = bitmapHeight * scale

        val left = boxLeft + (boxWidth - drawWidth)
        val top = boxTop + (boxHeight - drawHeight) / 2f

        return RectF(
            left,
            top,
            left + drawWidth,
            top + drawHeight
        )
    }
}