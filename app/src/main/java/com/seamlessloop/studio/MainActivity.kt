package com.seamlessloop.studio

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.graphics.drawable.GradientDrawable
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
    private var sourceName = ""
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
            textSize = 38f
            gravity = Gravity.CENTER
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
            background = rounded(blue, 18)
        }

        header.addView(
            logo,
            LinearLayout.LayoutParams(dp(64), dp(64))
        )

        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, 0, 0)
        }

        heading.addView(label("SeamlessLoop", 24f, white, true))
        heading.addView(label("STUDIO • VIDEO LOOP MAKER", 11f, muted, true))

        header.addView(
            heading,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        root.addView(header)
        root.addView(space(20))

        val intro = card()

        intro.addView(label("Buat video panjang", 22f, white, true))
        intro.addView(space(6))
        intro.addView(
            label(
                "Ubah video pendek menjadi video berulang dengan durasi, resolusi, dan kualitas pilihanmu.",
                14f,
                muted
            )
        )

        root.addView(intro)
        root.addView(space(18))

        val videoCard = card()
        videoCard.addView(label("01  •  VIDEO SUMBER", 13f, blue, true))
        videoCard.addView(space(12))

        chooseButton = button("PILIH VIDEO", blue)
        chooseButton.setOnClickListener {
            openVideoPicker()
        }

        videoCard.addView(
            chooseButton,
            matchWrap()
        )

        videoCard.addView(space(10))

        videoInfo = label("Belum ada video yang dipilih", 13f, muted)
        videoCard.addView(videoInfo)

        root.addView(videoCard)
        root.addView(space(14))

        val settingsCard = card()

        settingsCard.addView(label("02  •  PENGATURAN VIDEO", 13f, blue, true))
        settingsCard.addView(space(14))

        settingsCard.addView(label("Durasi hasil (menit)", 14f, white, true))
        settingsCard.addView(space(6))

        durationInput = EditText(this).apply {
            setText("1")
            hint = "1 sampai 60 menit"
            textSize = 16f
            setTextColor(white)
            setHintTextColor(muted)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.rgb(18, 28, 47), 12)
        }

        settingsCard.addView(durationInput, matchWrap())
        settingsCard.addView(space(14))

        settingsCard.addView(label("Resolusi", 14f, white, true))
        settingsCard.addView(space(6))

        resolutionSpinner = spinner(
            listOf("Resolusi asli", "720p", "1080p")
        )

        settingsCard.addView(resolutionSpinner, matchWrap())
        settingsCard.addView(space(14))

        settingsCard.addView(label("Kualitas video", 14f, white, true))
        settingsCard.addView(space(6))

        qualitySpinner = spinner(
            listOf("Hemat", "Standar", "Tinggi")
        )
        qualitySpinner.setSelection(1)

        settingsCard.addView(qualitySpinner, matchWrap())
        settingsCard.addView(space(14))

        settingsCard.addView(label("Audio", 14f, white, true))
        settingsCard.addView(space(6))

        audioSpinner = spinner(
            listOf("Pertahankan audio", "Tanpa suara")
        )

        settingsCard.addView(audioSpinner, matchWrap())

        root.addView(settingsCard)
        root.addView(space(14))

        val estimateCard = card()

        estimateCard.addView(label("03  •  ESTIMASI", 13f, blue, true))
        estimateCard.addView(space(10))

        estimate = label(
            "Pilih video dan hitung estimasi sebelum memproses.",
            14f,
            muted
        )

        estimateCard.addView(estimate)
        estimateCard.addView(space(12))

        estimateButton = button("HITUNG ESTIMASI", Color.rgb(54, 76, 108))
        estimateButton.setOnClickListener {
            updateEstimate()
        }

        estimateCard.addView(estimateButton, matchWrap())

        root.addView(estimateCard)
        root.addView(space(14))

        val processCard = card()

        processCard.addView(label("04  •  PROSES VIDEO", 13f, blue, true))
        processCard.addView(space(12))

        progressText = label("Progres: 0%", white, true)
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

        processButton.setOnClickListener {
            startProcessing()
        }

        processCard.addView(processButton, matchWrap())
        processCard.addView(space(12))

        status = label(
            "Hasil video akan disimpan ke Movies/SeamlessLoop Studio.",
            12f,
            muted
        )

        processCard.addView(status)

        root.addView(processCard)
        root.addView(space(18))

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

    @Deprecated("Deprecated by Android, retained for compatibility")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != PICK_VIDEO ||
            resultCode != RESULT_OK ||
            data?.data == null
        ) {
            return
        }

        val uri = data.data!!

        status.text = "Menyiapkan video..."
        chooseButton.isEnabled = false

        executor.execute {
            try {
                val copiedFile = copyToCache(uri)
                val name = getDisplayName(uri)

                runOnUiThread {
                    sourceFile = copiedFile
                    sourceName = name

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
        val output = File(cacheDir, "seamless_source_${System.currentTimeMillis()}.mp4")

        contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Video tidak dapat dibaca." }

            FileOutputStream(output).use { out ->
                input.copyTo(out)
            }
        }

        if (!output.exists() || output.length() == 0L) {
            throw IllegalStateException("File video kosong.")
        }

        return output
    }

    private fun getDisplayName(uri: Uri): String {
        var name = "Video terpilih"

        val cursor = contentResolver.query(
            uri,
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(
                    android.provider.OpenableColumns.DISPLAY_NAME
                )

                if (index >= 0) {
                    name = it.getString(index) ?: name
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
            estimate.text = "Masukkan durasi antara 1 sampai 60 menit."
            return
        }

        val targetSeconds = minutes * 60L
        val estimatedBytes = file.length() *
            (targetSeconds.toDouble() / 60.0) *
            qualityFactor()

        estimate.text =
            "Durasi target: $minutes menit\n" +
            "Ukuran sumber: ${formatBytes(file.length())}\n" +
            "Perkiraan ukuran hasil: ${formatBytes(estimatedBytes.toLong())}\n\n" +
            "Estimasi dapat berubah sesuai isi video dan audio."
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
            status.text = "Durasi harus antara 1 sampai 60 menit."
            return
        }

        val durationSeconds = minutes * 60

        val resolutionFilter = when (resolutionSpinner.selectedItemPosition) {
            1 -> "scale=-2:720"
            2 -> "scale=-2:1080"
            else -> "scale=trunc(iw/2)*2:trunc(ih/2)*2"
        }

        val quality = when (qualitySpinner.selectedItemPosition) {
            0 -> "7"
            2 -> "2"
            else -> "4"
        }

        val audioOption = audioSpinner.selectedItemPosition
        val tempOutput = File(
            cacheDir,
            "seamless_result_${System.currentTimeMillis()}.mp4"
        )

        val audioCommand = if (audioOption == 1) {
            "-an"
        } else {
            "-map 0:a? -c:a aac -b:a 128k"
        }

        val command =
            "-y -stream_loop -1 " +
            "-i ${quotePath(input.absolutePath)} " +
            "-t $durationSeconds " +
            "-vf $resolutionFilter " +
            "-c:v mpeg4 -q:v $quality " +
            "-pix_fmt yuv420p " +
            "$audioCommand " +
            "-movflags +faststart " +
            quotePath(tempOutput.absolutePath)

        isProcessing = true
        processButton.isEnabled = false
        chooseButton.isEnabled = false
        estimateButton.isEnabled = false

        progressBar.progress = 0
        progressText.text = "Progres: memulai..."
        status.text = "Sedang membuat video. Jangan tutup aplikasi."

        FFmpegKit.executeAsync(
            command,
            { session ->
                val success = ReturnCode.isSuccess(session.returnCode)

                if (success && tempOutput.exists() && tempOutput.length() > 0L) {
                    try {
                        val savedUri = saveToGallery(tempOutput)

                        runOnUiThread {
                            progressBar.progress = 100
                            progressText.text = "Progres: 100%"
                            status.text =
                                "Berhasil!\nVideo tersimpan di Movies/SeamlessLoop Studio."
                            videoInfo.text =
                                "Selesai: ${formatBytes(tempOutput.length())}"
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            status.text =
                                "Video berhasil dibuat, tetapi gagal menyimpan ke galeri: ${e.message}"
                        }
                    }
                } else {
                    val logs = session.allLogsAsString()
                    runOnUiThread {
                        status.text =
                            "Proses gagal. Periksa format video atau ruang penyimpanan."
                        progressText.text = "Progres: gagal"
                    }
                    android.util.Log.e("SeamlessLoop", logs)
                }

                tempOutput.delete()

                runOnUiThread {
                    isProcessing = false
                    processButton.isEnabled = true
                    chooseButton.isEnabled = true
                    estimateButton.isEnabled = true
                }
            },
            { log ->
                val message = log.message ?: ""
                val match = Regex("time=([0-9:.]+)").find(message)

                if (match != null) {
                    val seconds = parseTime(match.groupValues[1])
                    val percent = ((seconds / durationSeconds) * 100)
                        .toInt()
                        .coerceIn(0, 99)

                    runOnUiThread {
                        progressBar.progress = percent
                        progressText.text = "Progres: $percent%"
                    }
                }
            },
            { }
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
            contentResolver.openOutputStream(uri).use { output ->
                requireNotNull(output) { "Tidak dapat membuka file tujuan." }

                file.inputStream().use { input ->
                    input.copyTo(output)
                }
            }

            val updateValues = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }

            contentResolver.update(uri, updateValues, null, null)
            return uri
        } catch (e: Exception) {
            contentResolver.delete(uri, null, null)
            throw e
        }
    }

    private fun quotePath(path: String): String {
        return "'" + path.replace("'", "'\\''") + "'"
    }

    private fun parseTime(value: String): Double {
        return try {
            val parts = value.split(":")
            if (parts.size != 3) {
                0.0
            } else {
                parts[0].toDouble() * 3600.0 +
                    parts[1].toDouble() * 60.0 +
                    parts[2].toDouble()
            }
        } catch (_: Exception) {
            0.0
        }
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
            layoutParams = LinearLayout.LayoutParams(
                1,
                dp(height)
            )
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
