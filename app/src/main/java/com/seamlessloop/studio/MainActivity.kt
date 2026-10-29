package com.seamlessloop.studio

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private val navy = Color.rgb(12, 18, 35)
    private val cardColor = Color.rgb(27, 38, 61)
    private val blue = Color.rgb(45, 150, 245)
    private val white = Color.WHITE
    private val muted = Color.rgb(170, 185, 210)

    private lateinit var status: TextView
    private lateinit var videoInfo: TextView
    private lateinit var estimate: TextView
    private lateinit var progressText: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var durationInput: EditText
    private lateinit var resolutionSpinner: Spinner
    private lateinit var qualitySpinner: Spinner
    private lateinit var audioSpinner: Spinner

    private lateinit var chooseButton: Button
    private lateinit var estimateButton: Button
    private lateinit var processButton: Button

    private var sourceFile: File? = null
    private var isProcessing = false

    private val executor = Executors.newSingleThreadExecutor()

    companion object {
        private const val PICK_VIDEO = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createInterface()
    }

    private fun createInterface() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(navy)
            isFillViewport = true
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(30))
        }

        scroll.addView(
            root,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val logo = TextView(this).apply {
            text = "∞"
            textSize = 42f
            gravity = Gravity.CENTER
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
            background = rounded(blue, 18)
        }

        header.addView(logo, LinearLayout.LayoutParams(dp(64), dp(64)))

        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, 0, 0)
        }

        heading.addView(label("SeamlessLoop Studio", 22f, white, true))
        heading.addView(label("VIDEO LOOP • LONG VIDEO MAKER", 11f, muted))

        header.addView(
            heading,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        root.addView(header)
        root.addView(space(20))

        val intro = card()
        intro.addView(label("Buat video panjang", 22f, white, true))
        intro.addView(space(8))
        intro.addView(
            label(
                "Ubah video pendek menjadi video panjang. " +
                    "Atur durasi, resolusi, kualitas, dan audio.",
                14f,
                muted
            )
        )

        root.addView(intro)
        root.addView(space(16))

        val videoCard = card()
        videoCard.addView(label("01 • VIDEO SUMBER", 13f, blue, true))
        videoCard.addView(space(12))

        chooseButton = button("PILIH VIDEO", blue)
        chooseButton.setOnClickListener { openVideoPicker() }
        videoCard.addView(chooseButton, matchWrap())

        videoCard.addView(space(10))
        videoInfo = label("Belum ada video yang dipilih", 13f, muted)
        videoCard.addView(videoInfo)

        root.addView(videoCard)
        root.addView(space(14))

        val settings = card()
        settings.addView(label("02 • PENGATURAN VIDEO", 13f, blue, true))
        settings.addView(space(14))

        settings.addView(label("Durasi hasil (menit)", 14f, white, true))
        settings.addView(space(6))

        durationInput = EditText(this).apply {
            setText("1")
            hint = "Masukkan 1–60 menit"
            textSize = 16f
            setTextColor(white)
            setHintTextColor(muted)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.rgb(18, 28, 47), 12)
        }

        settings.addView(durationInput, matchWrap())
        settings.addView(space(14))

        settings.addView(label("Resolusi", 14f, white, true))
        settings.addView(space(6))

        resolutionSpinner = spinner(
            listOf("Resolusi asli", "720p", "1080p")
        )
        settings.addView(resolutionSpinner, matchWrap())
        settings.addView(space(14))

        settings.addView(label("Kualitas video", 14f, white, true))
        settings.addView(space(6))

        qualitySpinner = spinner(listOf("Hemat", "Standar", "Tinggi"))
        qualitySpinner.setSelection(1)
        settings.addView(qualitySpinner, matchWrap())
        settings.addView(space(14))

        settings.addView(label("Audio", 14f, white, true))
        settings.addView(space(6))

        audioSpinner = spinner(listOf("Pertahankan audio", "Tanpa suara"))
        settings.addView(audioSpinner, matchWrap())

        root.addView(settings)
        root.addView(space(14))

        val estimateCard = card()
        estimateCard.addView(label("03 • ESTIMASI", 13f, blue, true))
        estimateCard.addView(space(10))

        estimate = label(
            "Pilih video untuk menghitung perkiraan ukuran hasil.",
            14f,
            muted
        )
        estimateCard.addView(estimate)
        estimateCard.addView(space(12))

        estimateButton = button("HITUNG ESTIMASI", cardColor)
        estimateButton.setOnClickListener { updateEstimate() }
        estimateCard.addView(estimateButton, matchWrap())

        root.addView(estimateCard)
        root.addView(space(14))

        val processCard = card()
        processCard.addView(label("04 • PROSES VIDEO", 13f, blue, true))
        processCard.addView(space(12))

        // Urutan parameter label: teks, ukuran, warna, bold.
        progressText = label("Progres: 0%", 14f, white, true)
        processCard.addView(progressText)
        processCard.addView(space(8))

        progressBar = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 100
            progress = 0
        }

        processCard.addView(
            progressBar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(8)
            )
        )

        processCard.addView(space(14))

        processButton = button("BUAT VIDEO PANJANG", blue)
        processButton.isEnabled = false
        processButton.alpha = 0.5f
        processButton.setOnClickListener { startProcessing() }

        processCard.addView(processButton, matchWrap())
        processCard.addView(space(12))

        status = label(
            "Hasil disimpan ke Movies/SeamlessLoop Studio.",
            12f,
            muted
        )
        processCard.addView(status)

        root.addView(processCard)
        root.addView(space(20))

        val footer = label(
            "SEAMLESSLOOP STUDIO • CREATE YOUR LOOP",
            11f,
            muted,
            true
        )
        footer.gravity = Gravity.CENTER
        root.addView(footer)

        setContentView(scroll)
    }

    private fun openVideoPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
        }
        startActivityForResult(intent, PICK_VIDEO)
    }

    @Deprecated("Kept for compatibility")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != PICK_VIDEO ||
            resultCode != RESULT_OK ||
            data?.data == null
        ) return

        val uri = data.data!!

        chooseButton.isEnabled = false
        status.text = "Menyiapkan video..."

        executor.execute {
            try {
                val file = copyToCache(uri)
                val name = getDisplayName(uri)

                runOnUiThread {
                    sourceFile = file
                    videoInfo.text = "Video: $name"
                    status.text = "Video siap diproses."
                    processButton.isEnabled = true
                    processButton.alpha = 1f
                    chooseButton.isEnabled = true
                    updateEstimate()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Gagal membuka video: ${e.message}"
                    chooseButton.isEnabled = true
                }
            }
        }
    }

    private fun copyToCache(uri: Uri): File {
        val output = File(
            cacheDir,
            "seamless_source_${System.currentTimeMillis()}.mp4"
        )

        contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Video tidak dapat dibaca." }
            FileOutputStream(output).use { out -> input.copyTo(out) }
        }

        if (!output.exists() || output.length() == 0L) {
            throw IllegalStateException("File video kosong.")
        }

        return output
    }

    private fun getDisplayName(uri: Uri): String {
        var name = "Video terpilih"

        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    name = cursor.getString(index) ?: name
                }
            }
        }

        return name
    }

    private fun updateEstimate() {
        val file = sourceFile
        val minutes = durationInput.text.toString().toIntOrNull()

        if (file == null) {
            estimate.text = "Pilih video terlebih dahulu."
            return
        }

        if (minutes == null || minutes !in 1..60) {
            estimate.text = "Durasi harus 1–60 menit."
            return
        }

        val estimatedBytes =
            file.length() * (minutes * 60.0 / 60.0) * qualityFactor()

        estimate.text =
            "Durasi target: $minutes menit\n" +
            "Ukuran sumber: ${formatBytes(file.length())}\n" +
            "Perkiraan ukuran hasil: ${formatBytes(estimatedBytes.toLong())}\n\n" +
            "Ukuran aktual dapat berbeda."
    }

    private fun qualityFactor(): Double {
        return when (qualitySpinner.selectedItemPosition) {
            0 -> 0.45
            2 -> 1.8
            else -> 1.0
        }
    }

    private fun startProcessing() {
        if (isProcessing) return

        val input = sourceFile
        if (input == null || !input.exists()) {
            status.text = "Pilih video terlebih dahulu."
            return
        }

        val minutes = durationInput.text.toString().toIntOrNull()
        if (minutes == null || minutes !in 1..60) {
            status.text = "Durasi harus 1–60 menit."
            return
        }

        val durationSeconds = minutes * 60

        val resolution = when (resolutionSpinner.selectedItemPosition) {
            1 -> "scale=-2:720"
            2 -> "scale=-2:1080"
            else -> "scale=trunc(iw/2)*2:trunc(ih/2)*2"
        }

        val quality = when (qualitySpinner.selectedItemPosition) {
            0 -> "7"
            2 -> "2"
            else -> "4"
        }

        val noAudio = audioSpinner.selectedItemPosition == 1

        val output = File(
            cacheDir,
            "seamless_result_${System.currentTimeMillis()}.mp4"
        )

        val audioArgs = if (noAudio) {
            "-an"
        } else {
            "-map 0:a? -c:a aac -b:a 128k"
        }

        val command =
            "-y -stream_loop -1 " +
            "-i ${quotePath(input.absolutePath)} " +
            "-t $durationSeconds " +
            "-vf \"$resolution\" " +
            "-c:v mpeg4 -q:v $quality " +
            "-pix_fmt yuv420p " +
            "$audioArgs -movflags +faststart " +
            quotePath(output.absolutePath)

        isProcessing = true
        processButton.isEnabled = false
        chooseButton.isEnabled = false
        estimateButton.isEnabled = false
        progressBar.progress = 0
        progressText.text = "Progres: 0%"
        status.text = "Sedang memproses video..."

        // Callback FFmpegKit: selesai, log, statistik.
        FFmpegKit.executeAsync(
            command,
            { session ->
                val success = ReturnCode.isSuccess(session.returnCode)

                if (success && output.exists() && output.length() > 0L) {
                    try {
                        saveToGallery(output)

                        runOnUiThread {
                            progressBar.progress = 100
                            progressText.text = "Progres: 100%"
                            status.text =
                                "Berhasil! Video tersimpan di Movies/SeamlessLoop Studio."
                            videoInfo.text =
                                "Selesai: ${formatBytes(output.length())}"
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            status.text =
                                "Video dibuat, tetapi gagal disimpan: ${e.message}"
                        }
                    }
                } else {
                    android.util.Log.e(
                        "SeamlessLoop",
                        session.allLogsAsString()
                    )
                    runOnUiThread {
                        status.text =
                            "Proses gagal. Periksa format video dan ruang penyimpanan."
                        progressText.text = "Progres: gagal"
                    }
                }

                output.delete()

                runOnUiThread {
                    isProcessing = false
                    processButton.isEnabled = true
                    chooseButton.isEnabled = true
                    estimateButton.isEnabled = true
                }
            },
            null,
            { statistics ->
                val elapsedMs = statistics.time
                if (elapsedMs > 0) {
                    val percent =
                        ((elapsedMs.toDouble() /
                            (durationSeconds * 1000.0)) * 100.0)
                            .toInt()
                            .coerceIn(0, 99)

                    runOnUiThread {
                        progressBar.progress = percent
                        progressText.text = "Progres: $percent%"
                    }
                }
            }
        )
    }

    private fun saveToGallery(file: File): Uri {
        val values = ContentValues().apply {
            put(
                MediaStore.Video.Media.DISPLAY_NAME,
                "SeamlessLoop_${System.currentTimeMillis()}.mp4"
            )
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(
                MediaStore.Video.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_MOVIES}/SeamlessLoop Studio"
            )
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val collection = MediaStore.Video.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL_PRIMARY
        )

        val uri = contentResolver.insert(collection, values)
            ?: throw IllegalStateException("Tidak dapat membuat file galeri.")

        try {
            val outputStream = contentResolver.openOutputStream(uri, "w")
    ?: throw IllegalStateException("Tidak dapat membuka file tujuan.")

outputStream.use { output ->
    file.inputStream().use { input ->
        input.copyTo(output)
    }
}

            val done = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            contentResolver.update(uri, done, null, null)

            return uri
        } catch (e: Exception) {
            contentResolver.delete(uri, null, null)
            throw e
        }
    }

    private fun quotePath(path: String): String {
        return "'" + path.replace("'", "'\\''") + "'"
    }

    private fun formatBytes(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)

        return if (mb >= 1024.0) {
            String.format(Locale.US, "%.2f GB", mb / 1024.0)
        } else {
            String.format(Locale.US, "%.2f MB", mb)
        }
    }

    private fun label(
        text: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }
    }

    private fun card(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = rounded(cardColor, 18)
        }
    }

    private fun button(text: String, color: Int): Button {
        return Button(this).apply {
            this.text = text
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(white)
            isAllCaps = false
            background = rounded(color, 12)
            minimumHeight = dp(48)
        }
    }

    private fun spinner(items: List<String>): Spinner {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            items
        )
        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        return Spinner(this).apply {
            this.adapter = adapter
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = rounded(Color.rgb(18, 28, 47), 12)
        }
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun space(height: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, dp(height))
        }
    }

    private fun matchWrap(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
