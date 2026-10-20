package com.example.smsvote.ui.results

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.smsvote.data.model.Poll
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 50f
    private val CONTENT_W = PAGE_W - 2 * MARGIN

    fun shareAsPdf(
        context: Context,
        poll: Poll,
        results: List<OptionResult>,
        totalVotes: Int,
        aiAnalysis: String?
    ) {
        val uri = buildPdf(context, poll, results, totalVotes, aiAnalysis) ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "投票結果：${poll.question}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "匯出 PDF"))
    }

    private fun buildPdf(
        context: Context,
        poll: Poll,
        results: List<OptionResult>,
        totalVotes: Int,
        aiAnalysis: String?
    ): android.net.Uri? {
        val pdfDoc = PdfDocument()
        val pages = mutableListOf<PdfDocument.Page>()

        // ── Paints ──
        val titlePaint = TextPaint().apply {
            textSize = 18f; isFakeBoldText = true; color = Color.BLACK
        }
        val sectionPaint = TextPaint().apply {
            textSize = 13f; isFakeBoldText = true; color = Color.BLACK
        }
        val bodyPaint = TextPaint().apply {
            textSize = 11f; color = Color.BLACK
        }
        val mutedPaint = TextPaint().apply {
            textSize = 9f; color = Color.GRAY
        }
        val barFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(37, 99, 235)
        }
        val barBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY; strokeWidth = 0.5f
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(37, 99, 235)
        }

        fun newPage(): Pair<PdfDocument.Page, Canvas> {
            val info = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pages.size + 1).create()
            val page = pdfDoc.startPage(info)
            pages.add(page)
            return page to page.canvas
        }

        fun drawFooter(canvas: Canvas) {
            val footerPaint = TextPaint().apply { textSize = 8f; color = Color.LTGRAY }
            canvas.drawText(
                "由 SMSVote 產生  ·  ${SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())}",
                MARGIN, PAGE_H - 20f, footerPaint
            )
        }

        var (page, canvas) = newPage()
        var y = MARGIN

        // ── Header bar ──
        val headerPaint = Paint().apply { color = Color.rgb(37, 99, 235) }
        canvas.drawRect(0f, 0f, PAGE_W.toFloat(), 8f, headerPaint)
        y = 24f

        // ── Report title ──
        canvas.drawText("SMSVote 投票結果報告", MARGIN, y + 18f, titlePaint)
        y += 30f
        canvas.drawText(
            "產生時間：${SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())}",
            MARGIN, y + 9f, mutedPaint
        )
        y += 18f
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, linePaint)
        y += 16f

        // ── Poll question ──
        canvas.drawText("投票主題", MARGIN, y + 9f, mutedPaint)
        y += 14f
        val qLayout = makeLayout(poll.question, sectionPaint, CONTENT_W.toInt())
        canvas.save(); canvas.translate(MARGIN, y); qLayout.draw(canvas); canvas.restore()
        y += qLayout.height + 6f

        // Meta (endTime / maxChoices)
        val meta = buildString {
            if (poll.maxChoices > 1) append("複選，最多 ${poll.maxChoices} 個  ")
            if (poll.endTime != null)
                append("截止：${SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(poll.endTime))}")
        }.trim()
        if (meta.isNotEmpty()) {
            canvas.drawText(meta, MARGIN, y + 9f, mutedPaint)
            y += 14f
        }

        // ── Total votes circle summary ──
        y += 6f
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, linePaint)
        y += 14f

        val totalStr = "共 $totalVotes 票"
        canvas.drawText(totalStr, MARGIN, y + 11f, sectionPaint)
        y += 20f
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, linePaint)
        y += 14f

        // ── Option bars ──
        val maxCount = results.maxOfOrNull { it.count } ?: 0
        results.forEach { result ->
            if (y > PAGE_H - 100f) {
                drawFooter(canvas)
                pdfDoc.finishPage(page)
                val next = newPage()
                page = next.first; canvas = next.second; y = MARGIN
            }

            val isLeader = result.count == maxCount && result.count > 0

            // Label pill
            val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isLeader) Color.rgb(37, 99, 235) else Color.rgb(226, 232, 240)
            }
            val labelTextPaint = TextPaint().apply {
                textSize = 10f; isFakeBoldText = true
                color = if (isLeader) Color.WHITE else Color.GRAY
            }
            canvas.drawRoundRect(RectF(MARGIN, y, MARGIN + 20f, y + 14f), 4f, 4f, pillPaint)
            canvas.drawText(result.label, MARGIN + 5f, y + 10f, labelTextPaint)

            // Option text
            canvas.drawText(result.option, MARGIN + 26f, y + 10f, bodyPaint)

            // Count right-aligned
            val countStr = "${result.count} 票  ${"%.0f".format(result.percent * 100)}%"
            val countW = bodyPaint.measureText(countStr)
            canvas.drawText(countStr, PAGE_W - MARGIN - countW, y + 10f, bodyPaint)
            y += 18f

            // Bar
            val barH = 8f
            canvas.drawRoundRect(RectF(MARGIN, y, PAGE_W - MARGIN, y + barH), 4f, 4f, barBgPaint)
            if (result.percent > 0f) {
                canvas.drawRoundRect(
                    RectF(MARGIN, y, MARGIN + CONTENT_W * result.percent, y + barH),
                    4f, 4f, if (isLeader) barFillPaint else Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(99, 102, 241)
                    }
                )
            }
            y += barH + 14f
        }

        // ── AI Analysis ──
        if (!aiAnalysis.isNullOrBlank()) {
            y += 6f
            if (y > PAGE_H - 120f) {
                drawFooter(canvas)
                pdfDoc.finishPage(page)
                val next = newPage()
                page = next.first; canvas = next.second; y = MARGIN
            }

            canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, linePaint)
            y += 14f

            // AI badge
            val aiBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(124, 58, 237) }
            canvas.drawRoundRect(RectF(MARGIN, y, MARGIN + 60f, y + 14f), 7f, 7f, aiBadgePaint)
            val badgeTextPaint = TextPaint().apply {
                textSize = 8f; color = Color.WHITE; isFakeBoldText = true
            }
            canvas.drawText("✦ AI 分析", MARGIN + 6f, y + 10f, badgeTextPaint)
            y += 20f

            val analysisLayout = makeLayout(aiAnalysis, bodyPaint, CONTENT_W.toInt())
            // Check if it fits on current page; if not, new page
            if (y + analysisLayout.height > PAGE_H - 40f) {
                drawFooter(canvas)
                pdfDoc.finishPage(page)
                val next = newPage()
                page = next.first; canvas = next.second; y = MARGIN
                // Re-draw AI badge header on new page
                canvas.drawRoundRect(RectF(MARGIN, y, MARGIN + 60f, y + 14f), 7f, 7f, aiBadgePaint)
                canvas.drawText("✦ AI 分析（續）", MARGIN + 6f, y + 10f, badgeTextPaint)
                y += 20f
            }
            canvas.save(); canvas.translate(MARGIN, y); analysisLayout.draw(canvas); canvas.restore()
            y += analysisLayout.height + 16f
        }

        drawFooter(canvas)
        pdfDoc.finishPage(page)

        // ── Save & return URI ──
        return runCatching {
            val fileName = "smsvote_${poll.id.take(6)}_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, fileName)
            pdfDoc.writeTo(file.outputStream())
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.also { pdfDoc.close() }.getOrNull()
    }

    private fun makeLayout(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setLineSpacing(3f, 1f)
            .build()
}
