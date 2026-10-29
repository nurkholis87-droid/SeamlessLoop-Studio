package com.seamlessloop.studio

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.ceil

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var videoInfo: TextView
    private lateinit var estimate: TextView
    private lateinit var progressText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var durationInput: EditText
    private lateinit var resolutionSpinner: Spinner
    private lateinit var qualitySpinner: Spinner
    private lateinit var audioSpinner: Spinner
    private lateinit var modeSpinner: Spinner
    private lateinit var chooseButton: Button
    private lateinit var estimateButton: Button
    private lateinit var processButton: Button

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    private var sourceFile: File? = null
    private var sourceDuration = 0.0
    private var sourceSize = 0L
    private var sourceHasAudio = false
    private var isProcessing = false

    companion object {
        private const val PICK_VIDEO = 100
        private const val CROSSFADE_SECONDS = 1.0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createInterface()
    }

    private fun createInterface() {
        val navy = Color.rgb(5, 12, 27)
        val card = Color.rgb(12, 27, 49)
        val card2 = Color.rgb(16, 39, 69)
        val blue = Color.rgb(0, 153, 255)
        val cyan = Color.rgb(0, 215, 255)
        val white = Color.rgb(244, 249, 255)
        val muted = Color.rgb(155, 177, 204)
        val line = Color.rgb(30, 74, 115)

        window.statusBarColor = navy
        window.navigationBarColor = navy
        window.decorView.systemUiVisibility = 0

        val scroll = ScrollView(this).apply {
            setBackgroundColor(navy)
            isFillViewport = true
            clipToPadding = false
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(28))
        }
        scroll.addView(root)

        // Brand header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(2), dp(2), dp(8))
        }
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.seamlessloop_logo)
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            contentDescription = "Logo SeamlessLoop Studio"
        }
        header.addView(logo, LinearLayout.LayoutParams(dp(72), dp(72)))
        val brandText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        val brandName = TextView(this).apply {
            text = "SeamlessLoop"
            textSize = 23f
            setTextColor(white)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        val brandStudio = TextView(this).apply {
            text = "STUDIO  •  VIDEO TOOLKIT"
            textSize = 10f
            letterSpacing = 0.12f
            setTextColor(cyan)
        }
        brandText.addView(brandName)
        brandText.addView(brandStudio)
        header.addView(brandText, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(header, matchWidth())

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = roundedBackground(card2, dp(22), line, dp(1))
        }
        hero.addView(TextView(this).apply {
            text = "VIDEO LOOP  •  LONG VIDEO MAKER"
            textSize = 10f
            letterSpacing = 0.12f
            setTextColor(cyan)
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        hero.addView(TextView(this).apply {
            text = "Video pendek,\njadi video panjang."
            textSize = 26f
            setTextColor(white)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(4))
        })
        hero.addView(TextView(this).apply {
            text = "Buat loop mulus atau crossfade dengan pengaturan yang mudah."
            textSize = 13f
            setTextColor(muted)
        })
        space(hero, 16)
        chooseButton = button("＋   PILIH VIDEO", blue).apply {
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = roundedBackground(blue, dp(14))
            minHeight = dp(54)
        }
        hero.addView(chooseButton, matchWidth())
        root.addView(hero, matchWidth())

        space(root, 18)
        root.addView(sectionHeading("VIDEO SUMBER", "Pilih video yang ingin di-loop", white, muted), matchWidth())
        val sourceCard = panel(card, line, 18)
        val sourceRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val videoIcon = TextView(this).apply {
            text = "▶"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(cyan)
            background = roundedBackground(Color.rgb(8, 54, 91), dp(14))
        }
        sourceRow.addView(videoIcon, LinearLayout.LayoutParams(dp(54), dp(54)))
        videoInfo = label("Belum ada video dipilih.\nPilih video dari galeri untuk mulai.", muted).apply {
            textSize = 13f
            setPadding(dp(12), dp(4), 0, dp(4))
        }
        sourceRow.addView(videoInfo, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        sourceCard.addView(sourceRow, matchWidth())
        space(sourceCard, 12)
        val changeVideo = button("Ganti / Pilih Video", Color.rgb(14, 46, 78)).apply {
            background = roundedBackground(Color.rgb(14, 46, 78), dp(12), line, dp(1))
            setTextColor(cyan)
        }
        sourceCard.addView(changeVideo, matchWidth())
        root.addView(sourceCard, matchWidth())

        space(root, 18)
        root.addView(sectionHeading("PENGATURAN VIDEO", "Sesuaikan hasil sesuai kebutuhan", white, muted), matchWidth())
        val settings = panel(card, line, 18)
        settings.addView(label("DURASI TARGET  ·  MENIT", white).apply {
            textSize = 11f
            setTypeface(null, android.graphics.Typeface.BOLD)
            letterSpacing = 0.08f
        })
        durationInput = EditText(this).apply {
            setText("1")
            textSize = 18f
            setTextColor(white)
            setHintTextColor(muted)
            hint = "1–60 menit"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            singleLine = true
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = roundedBackground(Color.rgb(5, 18, 36), dp(12), line, dp(1))
        }
        settings.addView(durationInput, matchWidth())
        space(settings, 8)
        val quickScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val quickRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("1", "5", "10", "30", "60").forEach { minutes ->
            val chip = button("$minutes m", Color.rgb(12, 42, 73)).apply {
                textSize = 12f
                minHeight = dp(40)
                background = roundedBackground(Color.rgb(12, 42, 73), dp(12), line, dp(1))
                setTextColor(if (minutes == "1") cyan else white)
                setPadding(dp(14), 0, dp(14), 0)
                setOnClickListener { durationInput.setText(minutes) }
            }
            val chipParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(40))
            chipParams.setMargins(0, 0, dp(8), 0)
            quickRow.addView(chip, chipParams)
        }
        quickScroll.addView(quickRow)
        settings.addView(quickScroll, matchWidth())
        space(settings, 14)
        settings.addView(label("MODE LOOP", white).apply { textSize = 11f; setTypeface(null, android.graphics.Typeface.BOLD) })
        modeSpinner = spinner(listOf("Loop biasa", "Seamless Crossfade (1 detik)"), Color.rgb(5, 18, 36), white)
        modeSpinner.background = roundedBackground(Color.rgb(5, 18, 36), dp(12), line, dp(1))
        settings.addView(modeSpinner, matchWidth())
        space(settings, 12)
        settings.addView(label("RESOLUSI OUTPUT", white).apply { textSize = 11f; setTypeface(null, android.graphics.Typeface.BOLD) })
        resolutionSpinner = spinner(listOf("Resolusi asli", "720p", "1080p"), Color.rgb(5, 18, 36), white)
        resolutionSpinner.background = roundedBackground(Color.rgb(5, 18, 36), dp(12), line, dp(1))
        settings.addView(resolutionSpinner, matchWidth())
        space(settings, 12)
        settings.addView(label("KUALITAS VIDEO", white).apply { textSize = 11f; setTypeface(null, android.graphics.Typeface.BOLD) })
        qualitySpinner = spinner(listOf("Hemat", "Standar", "Tinggi"), Color.rgb(5, 18, 36), white).apply { setSelection(1) }
        qualitySpinner.background = roundedBackground(Color.rgb(5, 18, 36), dp(12), line, dp(1))
        settings.addView(qualitySpinner, matchWidth())
        space(settings, 12)
        settings.addView(label("PENGATURAN AUDIO", white).apply { textSize = 11f; setTypeface(null, android.graphics.Typeface.BOLD) })
        audioSpinner = spinner(listOf("Pertahankan audio (sambung biasa)", "Crossfade audio (mode crossfade)", "Tanpa suara"), Color.rgb(5, 18, 36), white)
        audioSpinner.background = roundedBackground(Color.rgb(5, 18, 36), dp(12), line, dp(1))
        settings.addView(audioSpinner, matchWidth())
        root.addView(settings, matchWidth())

        space(root, 18)
        root.addView(sectionHeading("ESTIMASI HASIL", "Periksa perkiraan sebelum ekspor", white, muted), matchWidth())
        val estimateCard = panel(card, line, 18)
        val estimateHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val estimateIcon = TextView(this).apply {
            text = "▤"
            textSize = 23f
            gravity = Gravity.CENTER
            setTextColor(cyan)
            background = roundedBackground(Color.rgb(8, 54, 91), dp(12))
        }
        estimateHeader.addView(estimateIcon, LinearLayout.LayoutParams(dp(46), dp(46)))
        estimateHeader.addView(TextView(this).apply {
            text = "Ringkasan ekspor"
            textSize = 15f
            setTextColor(white)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dp(12), 0, 0, 0)
        })
        estimateCard.addView(estimateHeader, matchWidth())
        estimate = label("Pilih video untuk menghitung estimasi ukuran dan durasi.", muted).apply {
            textSize = 13f
            setPadding(0, dp(10), 0, dp(4))
        }
        estimateCard.addView(estimate, matchWidth())
        estimateButton = button("▦   HITUNG ESTIMASI", Color.rgb(12, 74, 125)).apply {
            background = roundedBackground(Color.rgb(12, 74, 125), dp(12), line, dp(1))
            minHeight = dp(48)
        }
        estimateCard.addView(estimateButton, matchWidth())
        root.addView(estimateCard, matchWidth())

        space(root, 18)
        root.addView(sectionHeading("EKSPOR VIDEO", "Jangan tutup aplikasi selama proses berjalan", white, muted), matchWidth())
        val exportCard = panel(card, line, 18)
        progressText = label("PROGRES EKSPOR     0%", white).apply {
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        exportCard.addView(progressText, matchWidth())
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            progressTintList = android.content.res.ColorStateList.valueOf(cyan)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(30, 49, 74))
        }
        exportCard.addView(progressBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8)))
        space(exportCard, 12)
        processButton = button("▶   BUAT VIDEO PANJANG", blue).apply {
            isEnabled = false
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = roundedBackground(blue, dp(14))
            minHeight = dp(54)
        }
        exportCard.addView(processButton, matchWidth())
        space(exportCard, 12)
        status = label("Siap digunakan. Hasil akan disimpan ke Movies/SeamlessLoop Studio.", muted).apply {
            textSize = 12f
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = roundedBackground(Color.rgb(5, 18, 36), dp(12))
        }
        exportCard.addView(status, matchWidth())
        root.addView(exportCard, matchWidth())

        space(root, 20)
        root.addView(TextView(this).apply {
            text = "SEAMLESSLOOP STUDIO  •  VERSION 1.8.0\nDibuat untuk loop video yang lebih mudah."
            textSize = 10f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(104, 132, 165))
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }, matchWidth())

        setContentView(scroll)
        chooseButton.setOnClickListener { openVideoPicker() }
        changeVideo.setOnClickListener { openVideoPicker() }
        estimateButton.setOnClickListener { updateEstimate() }
        processButton.setOnClickListener { startProcessing() }
    }

    private fun panel(color: Int, stroke: Int, padding: Int): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(padding), dp(padding), dp(padding), dp(padding))
            background = roundedBackground(color, dp(18), stroke, dp(1))
        }

    private fun sectionHeading(title: String, subtitle: String, primary: Int, secondary: Int): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(2), 0, dp(2), dp(10))
            addView(TextView(this@MainActivity).apply {
                text = title
                textSize = 12f
                letterSpacing = 0.08f
                setTextColor(primary)
                setTypeface(null, android.graphics.Typeface.BOLD)
            })
            addView(TextView(this@MainActivity).apply {
                text = subtitle
                textSize = 11f
                setTextColor(secondary)
                setPadding(0, dp(3), 0, 0)
            })
        }

    private fun roundedBackground(color: Int, radius: Int, strokeColor: Int? = null, strokeWidth: Int = 0): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
            if (strokeColor != null && strokeWidth > 0) setStroke(strokeWidth, strokeColor)
        }

    private fun button(textValue: String, color: Int): Button =
        Button(this).apply {
            text = textValue
            textSize = 14f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(color)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }

    private fun label(textValue: String, color: Int): TextView =
        TextView(this).apply {
            text = textValue
            textSize = 14f
            setTextColor(color)
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }

    private fun spinner(items: List<String>, background: Int, textColor: Int): Spinner {
        val adapter = object : ArrayAdapter<String>(
            this, android.R.layout.simple_spinner_item, items
        ) {
            override fun getView(position: Int, convertView: android.view.View?, parent: ViewGroup): android.view.View {
                return super.getView(position, convertView, parent).apply {
                    (this as? TextView)?.setTextColor(textColor)
                }
            }

            override fun getDropDownView(position: Int, convertView: android.view.View?, parent: ViewGroup): android.view.View {
                return super.getDropDownView(position, convertView, parent).apply {
                    (this as? TextView)?.setTextColor(Color.BLACK)
                }
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        return Spinner(this).apply {
            this.adapter = adapter
            setBackgroundColor(background)
        }
    }

    private fun space(parent: LinearLayout, height: Int) {
        parent.addView(android.view.View(this), LinearLayout.LayoutParams(1, dp(height)))
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun openVideoPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
        }
        @Suppress("DEPRECATION")
        startActivityForResult(intent, PICK_VIDEO)
    }

    @Deprecated("Uses the Android activity result callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK_VIDEO || resultCode != RESULT_OK) return
        val uri = data?.data ?: return

        sourceFile = null
        sourceDuration = 0.0
        sourceSize = 0L
        sourceHasAudio = false
        processButton.isEnabled = false
        chooseButton.isEnabled = false
        estimateButton.isEnabled = false
        progressBar.progress = 0
        progressText.text = "PROGRES EKSPOR     0%"
        status.text = "Status: Membaca video..."
        videoInfo.text = "Sedang menyiapkan video..."

        executor.execute {
            try {
                val file = copyToCache(uri)
                FFmpegKit.executeAsync("-i \"${file.absolutePath}\"") { session ->
                    val logs = session.allLogsAsString ?: ""
                    val match = Regex("""Duration:\s*(\d+):(\d+):(\d+(?:\.\d+)?)""").find(logs)
                    val hasAudio = Regex("""Stream #\d+:\d+.*Audio:""", RegexOption.IGNORE_CASE).containsMatchIn(logs)
                    val duration = if (match != null) {
                        match.groupValues[1].toDouble() * 3600 +
                            match.groupValues[2].toDouble() * 60 +
                            match.groupValues[3].toDouble()
                    } else 0.0

                    handler.post {
                        chooseButton.isEnabled = true
                        estimateButton.isEnabled = true
                        if (duration > 0.0) {
                            sourceFile = file
                            sourceDuration = duration
                            sourceSize = file.length()
                            sourceHasAudio = hasAudio
                            videoInfo.text = "Video siap digunakan\nDurasi: ${formatDuration(duration)}\nUkuran: ${formatBytes(sourceSize)}\nAudio: ${if (hasAudio) "tersedia" else "tidak ditemukan"}"
                            processButton.isEnabled = true
                            status.text = "Status: Video berhasil dibaca."
                            updateEstimate()
                        } else {
                            processButton.isEnabled = false
                            status.text = "Gagal membaca durasi video. Coba video lain."
                            videoInfo.text = "Video tidak dapat dibaca."
                        }
                    }
                }
            } catch (e: Exception) {
                handler.post {
                    chooseButton.isEnabled = true
                    estimateButton.isEnabled = true
                    processButton.isEnabled = false
                    status.text = "Gagal membuka video: ${e.message}"
                }
            }
        }
    }

    private fun copyToCache(uri: Uri): File {
        val input = contentResolver.openInputStream(uri)
            ?: throw Exception("Video tidak dapat dibuka.")
        val file = File(cacheDir, "input_${System.currentTimeMillis()}.mp4")
        input.use { source ->
            file.outputStream().use { destination -> source.copyTo(destination) }
        }
        if (file.length() <= 0L) throw Exception("File video kosong.")
        return file
    }

    private fun updateEstimate() {
        if (sourceDuration <= 0.0 || sourceSize <= 0L) {
            estimate.text = "Pilih video terlebih dahulu."
            return
        }
        val minutes = durationInput.text.toString().toIntOrNull()
        if (minutes == null || minutes !in 1..60) {
            estimate.text = "Masukkan durasi antara 1 sampai 60 menit."
            return
        }
        val crossfade = modeSpinner.selectedItemPosition == 1
        val effectiveDuration = if (crossfade) sourceDuration - CROSSFADE_SECONDS else sourceDuration
        val repetitions = ceil(minutes * 60.0 / effectiveDuration.coerceAtLeast(1.0)).toInt()
        val estimatedSize = sourceSize.toDouble() * minutes * 60.0 / sourceDuration
        estimate.text = "Durasi sumber: ${formatDuration(sourceDuration)}\n" +
            "Durasi target: $minutes menit\n" +
            "Mode: ${if (crossfade) "Seamless Crossfade" else "Loop biasa"}\n" +
            "Perkiraan pengulangan: $repetitions kali\n" +
            "Ukuran sumber: ${formatBytes(sourceSize)}\n" +
            "Estimasi kasar ukuran hasil: ${formatBytes(estimatedSize)}\n\n" +
            "Ukuran akhir dapat berubah karena pengodean ulang."
    }

    private fun startProcessing() {
        if (isProcessing) return
        val input = sourceFile
        val minutes = durationInput.text.toString().toIntOrNull()
        if (input == null || !input.exists()) {
            status.text = "Status: Pilih video terlebih dahulu."
            return
        }
        if (minutes == null || minutes !in 1..60) {
            status.text = "Status: Durasi harus antara 1 dan 60 menit."
            return
        }

        val targetSeconds = minutes * 60
        val crossfade = modeSpinner.selectedItemPosition == 1
        if (crossfade && sourceDuration <= CROSSFADE_SECONDS * 2) {
            status.text = "Video terlalu pendek untuk crossfade. Pilih video lebih dari 2 detik atau gunakan Loop biasa."
            return
        }

        val resolution = resolutionSpinner.selectedItemPosition
        val quality = qualitySpinner.selectedItemPosition
        val audioMode = audioSpinner.selectedItemPosition
        val includeAudio = audioMode != 2 && sourceHasAudio
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val output = File(cacheDir, "SeamlessLoop_$timestamp.mp4")
        val scaleFilter = when (resolution) {
            1 -> "scale=-2:720"
            2 -> "scale=-2:1080"
            else -> "scale=trunc(iw/2)*2:trunc(ih/2)*2"
        }
        val qualityValue = when (quality) {
            0 -> "7"
            2 -> "2"
            else -> "4"
        }
        val audioOptions = if (!includeAudio) "-an" else "-c:a aac -b:a 128k"

        val command = if (crossfade) {
            buildCrossfadeCommand(input, output, targetSeconds, sourceDuration, scaleFilter, qualityValue, includeAudio, audioMode)
        } else {
            val repetitions = ceil(targetSeconds.toDouble() / sourceDuration).toInt().coerceAtLeast(1)
            buildString {
                append("-stream_loop ${repetitions - 1} ")
                append("-i \"${input.absolutePath}\" ")
                append("-t $targetSeconds ")
                append("-vf \"$scaleFilter\" ")
                append("-c:v mpeg4 -q:v $qualityValue -pix_fmt yuv420p ")
                append("$audioOptions -movflags +faststart ")
                append("-y \"${output.absolutePath}\"")
            }
        }

        isProcessing = true
        processButton.isEnabled = false
        chooseButton.isEnabled = false
        estimateButton.isEnabled = false
        progressBar.progress = 0
        progressText.text = "PROGRES EKSPOR     0%"
        status.text = (if (crossfade) "Status: Memproses crossfade. Ini bisa lebih lama..." else "Status: Mengekspor video...") +
            if (!includeAudio) "\nOutput tanpa audio." else if (crossfade && audioMode == 1) "\nAudio memakai crossfade." else "\nAudio dipertahankan tanpa crossfade."

        FFmpegKit.executeAsync(
            command,
            { session ->
                val success = ReturnCode.isSuccess(session.returnCode) && output.exists() && output.length() > 0L
                if (success) {
                    try {
                        saveToGallery(output)
                        handler.post {
                            progressBar.progress = 100
                            progressText.text = "PROGRES EKSPOR     100%"
                            status.text = "BERHASIL!\n\n" +
                                "Durasi target: $minutes menit\n" +
                                "Ukuran hasil: ${formatBytes(output.length())}\n" +
                                "Codec: MPEG-4\n" +
                                "Mode: ${if (crossfade) "Seamless Crossfade" else "Loop biasa"}\n" +
                                "Audio: ${if (!includeAudio) "tanpa audio" else if (crossfade && audioMode == 1) "crossfade halus" else "dipertahankan"}\n" +
                                "Lokasi: Movies/SeamlessLoop Studio"
                            Toast.makeText(this, "Video berhasil disimpan ke Galeri!", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        handler.post { status.text = "Video selesai, tetapi gagal disimpan: ${e.message}" }
                    }
                } else {
                    val logs = session.allLogsAsString ?: ""
                    handler.post { status.text = "Ekspor gagal. Periksa ruang penyimpanan dan format video.\n${logs.takeLast(600)}" }
                }
                handler.post {
                    isProcessing = false
                    processButton.isEnabled = sourceFile != null
                    chooseButton.isEnabled = true
                    estimateButton.isEnabled = true
                }
            },
            null,
            { statistics ->
                val percent = (statistics.time.toDouble() / (targetSeconds * 1000.0) * 100.0).toInt().coerceIn(0, 99)
                handler.post {
                    progressBar.progress = percent
                    progressText.text = "PROGRES EKSPOR     $percent%"
                    status.text = "Status: Mengekspor video...\nWaktu video diproses: ${formatDuration(statistics.time / 1000.0)}"
                }
            }
        )
    }

    private fun buildCrossfadeCommand(
        input: File,
        output: File,
        targetSeconds: Int,
        sourceDuration: Double,
        scaleFilter: String,
        qualityValue: String,
        includeAudio: Boolean,
        audioMode: Int
    ): String {
        val overlap = CROSSFADE_SECONDS
        val repetitions = ceil(targetSeconds / (sourceDuration - overlap)).toInt().coerceAtLeast(2)
        val inputs = StringBuilder()
        val filters = StringBuilder()

        repeat(repetitions) { inputs.append("-i \"${input.absolutePath}\" ") }

        for (i in 0 until repetitions) {
            filters.append("[$i:v]setpts=PTS-STARTPTS,$scaleFilter,format=yuv420p[v$i];")
            if (includeAudio) {
                filters.append("[$i:a]aresample=async=1:first_pts=0,atrim=duration=$sourceDuration,apad=whole_dur=$sourceDuration,asetpts=PTS-STARTPTS[a$i];")
            }
        }

        var currentVideo = "v0"
        var currentAudio = "a0"
        var currentDuration = sourceDuration
        for (i in 1 until repetitions) {
            val offset = (currentDuration - overlap).coerceAtLeast(0.0)
            filters.append("[$currentVideo][v$i]xfade=transition=fade:duration=$overlap:offset=$offset[vx$i];")
            currentVideo = "vx$i"
            currentDuration += sourceDuration - overlap

            if (includeAudio && audioMode == 1) {
                filters.append("[$currentAudio][a$i]acrossfade=d=$overlap:c1=tri:c2=tri[ax$i];")
                currentAudio = "ax$i"
            }
        }

        filters.append("[$currentVideo]trim=duration=$targetSeconds,setpts=PTS-STARTPTS[vout]")
        if (includeAudio) {
            if (audioMode == 1) {
                filters.append(";[$currentAudio]atrim=duration=$targetSeconds,asetpts=PTS-STARTPTS[aout]")
            } else {
                val audioInputs = (0 until repetitions).joinToString("") { "[a$it]" }
                filters.append(";$audioInputs concat=n=$repetitions:v=0:a=1,atrim=duration=$targetSeconds,asetpts=PTS-STARTPTS[aout]")
            }
        }

        return buildString {
            append(inputs)
            append("-filter_complex \"${filters}\" ")
            append("-map \"[vout]\" ")
            if (includeAudio) append("-map \"[aout]\" ")
            append("-c:v mpeg4 -q:v $qualityValue -pix_fmt yuv420p ")
            if (!includeAudio) append("-an ") else append("-c:a aac -b:a 128k ")
            append("-t $targetSeconds -movflags +faststart ")
            append("-y \"${output.absolutePath}\"")
        }
    }

    private fun saveToGallery(file: File): Uri {
        val values = ContentValues()
        values.put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        values.put(
            MediaStore.Video.Media.RELATIVE_PATH,
            Environment.DIRECTORY_MOVIES + "/SeamlessLoop Studio"
        )

        val collection = MediaStore.Video.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL_PRIMARY
        )
        val uri = contentResolver.insert(collection, values)
            ?: throw IllegalStateException("Galeri tidak dapat membuat file.")

        try {
            val destination = contentResolver.openOutputStream(uri, "w")
                ?: throw IllegalStateException("Galeri tidak dapat dibuka.")

            destination.use { output ->
                file.inputStream().use { input ->
                    input.copyTo(output)
                }
            }

            return uri
        } catch (e: Exception) {
            contentResolver.delete(uri, null, null)
            throw e
        }
    }

    private fun formatDuration(seconds: Double): String {
        val total = seconds.toInt().coerceAtLeast(0)
        val hours = total / 3600
        val minutes = (total % 3600) / 60
        val secs = total % 60
        return if (hours > 0) String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, secs)
        else String.format(Locale.US, "%02d:%02d", minutes, secs)
    }

    private fun formatBytes(bytes: Long): String = formatBytes(bytes.toDouble())

    private fun formatBytes(bytes: Double): String {
        if (!bytes.isFinite() || bytes <= 0.0) return "0 MB"
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024.0) DecimalFormat("0.00").format(mb / 1024.0) + " GB"
        else DecimalFormat("0.00").format(mb) + " MB"
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}
