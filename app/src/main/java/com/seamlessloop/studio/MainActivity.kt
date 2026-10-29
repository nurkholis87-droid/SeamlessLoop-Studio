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
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
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

    private val navy = Color.rgb(7, 15, 32)
    private val card = Color.rgb(17, 32, 57)
    private val field = Color.rgb(9, 22, 43)
    private val blue = Color.rgb(0, 145, 255)
    private val cyan = Color.rgb(0, 210, 255)
    private val green = Color.rgb(0, 175, 115)
    private val white = Color.WHITE
    private val muted = Color.rgb(165, 185, 215)

    companion object {
        private const val PICK_VIDEO = 100
        private const val CROSSFADE_SECONDS = 1.0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createInterface()
    }

    // =========================================================
    // PREMIUM UI
    // =========================================================

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
            )
        )

        // HEADER
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val logo = FrameLayout(this).apply {
            background = rounded(
                Color.rgb(15, 42, 83),
                22
            )
            addView(
                LoopLogoView(this@MainActivity),
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }

        header.addView(
            logo,
            LinearLayout.LayoutParams(dp(76), dp(76))
        )

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), 0, 0, 0)
        }

        brand.addView(TextView(this).apply {
            text = "SeamlessLoop"
            textSize = 23f
            setTextColor(white)
            setTypeface(null, Typeface.BOLD)
        })

        brand.addView(TextView(this).apply {
            text = "STUDIO"
            textSize = 18f
            setTextColor(cyan)
            setTypeface(null, Typeface.BOLD)
        })

        brand.addView(TextView(this).apply {
            text = "VIDEO LOOP • LONG VIDEO MAKER"
            textSize = 9f
            setTextColor(muted)
        })

        header.addView(
            brand,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        root.addView(header)

        space(root, 22)

        // INTRO CARD
        root.addView(sectionCard {
            addView(TextView(this@MainActivity).apply {
                text = "Buat Video Panjang"
                textSize = 21f
                setTextColor(white)
                setTypeface(null, Typeface.BOLD)
            })

            space(this, 7)

            addView(TextView(this@MainActivity).apply {
                text = "Ubah video pendek menjadi video panjang. Atur durasi, resolusi, kualitas, dan audio sesuai kebutuhan."
                textSize = 13f
                setTextColor(muted)
                setLineSpacing(dp(3).toFloat(), 1f)
            })
        })

        space(root, 22)

        // SECTION 1
        root.addView(sectionTitle("01", "VIDEO SUMBER"))

        chooseButton = button(
            "▶   PILIH VIDEO DARI GALERI",
            blue
        )

        root.addView(chooseButton, matchWidth())

        space(root, 8)

        videoInfo = label(
            "Belum ada video yang dipilih.",
            muted
        ).apply {
            background = rounded(card, 14)
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }

        root.addView(videoInfo, matchWidth())

        space(root, 22)

        // SECTION 2
        root.addView(sectionTitle("02", "PENGATURAN VIDEO"))

        root.addView(sectionCard {

            addView(fieldTitle("Durasi hasil (menit)"))

            durationInput = EditText(this@MainActivity).apply {
                setText("1")
                textSize = 17f
                setTextColor(white)
                setHintTextColor(muted)
                hint = "Masukkan 1–60 menit"
                inputType =
                    android.text.InputType.TYPE_CLASS_NUMBER
                setSingleLine(true)
                setPadding(
                    dp(14), dp(12), dp(14), dp(12)
                )
                background = rounded(field, 12)
            }

            addView(durationInput, matchWidth())

            space(this, 16)

            addView(fieldTitle("Mode looping"))

            modeSpinner = spinner(
                listOf(
                    "Loop biasa",
                    "Seamless Crossfade (1 detik)"
                )
            )

            addView(modeSpinner, matchWidth())

            space(this, 16)

            addView(fieldTitle("Resolusi video"))

            resolutionSpinner = spinner(
                listOf(
                    "Resolusi asli",
                    "720p HD",
                    "1080p Full HD"
                )
            )

            addView(resolutionSpinner, matchWidth())

            space(this, 16)

            addView(fieldTitle("Kualitas video"))

            qualitySpinner = spinner(
                listOf("Hemat", "Standar", "Tinggi")
            )

            qualitySpinner.setSelection(1)

            addView(qualitySpinner, matchWidth())

            space(this, 16)

            addView(fieldTitle("Pengaturan audio"))

            audioSpinner = spinner(
                listOf(
                    "Pertahankan audio",
                    "Crossfade audio",
                    "Tanpa suara"
                )
            )

            addView(audioSpinner, matchWidth())
        })

        space(root, 22)

        // SECTION 3
        root.addView(sectionTitle("03", "ESTIMASI HASIL"))

        estimateButton = button(
            "▦   HITUNG ESTIMASI",
            Color.rgb(35, 92, 160)
        )

        root.addView(estimateButton, matchWidth())

        space(root, 8)

        estimate = label(
            "Pilih video untuk menghitung estimasi.",
            muted
        ).apply {
            background = rounded(card, 14)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }

        root.addView(estimate, matchWidth())

        space(root, 22)

        // SECTION 4
        root.addView(sectionTitle("04", "PROSES EKSPOR"))

        root.addView(sectionCard {

            progressText = TextView(this@MainActivity).apply {
                text = "Progres: 0%"
                textSize = 17f
                setTextColor(white)
                setTypeface(null, Typeface.BOLD)
            }

            addView(progressText)

            space(this, 12)

            progressBar = ProgressBar(
                this@MainActivity,
                null,
                android.R.attr.progressBarStyleHorizontal
            ).apply {
                max = 100
                progress = 0
                progressTintList =
                    android.content.res.ColorStateList.valueOf(cyan)
                progressBackgroundTintList =
                    android.content.res.ColorStateList.valueOf(
                        Color.rgb(42, 58, 82)
                    )
            }

            addView(
                progressBar,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(10)
                )
            )

            space(this, 18)

            processButton = button(
                "▶   BUAT VIDEO PANJANG",
                green
            )

            processButton.isEnabled = false

            addView(processButton, matchWidth())
        })

        space(root, 22)

        // SECTION 5
        root.addView(sectionTitle("05", "STATUS APLIKASI"))

        status = label(
            "Siap digunakan. Pilih video untuk memulai.",
            white
        ).apply {
            background = rounded(card, 16)
            setPadding(dp(14), dp(16), dp(14), dp(16))
        }

        root.addView(status, matchWidth())

        space(root, 20)

        root.addView(TextView(this).apply {
            text = "SEAMLESSLOOP STUDIO • VERSION 2.0\nHasil disimpan ke Movies/SeamlessLoop Studio."
            textSize = 10f
            gravity = Gravity.CENTER
            setTextColor(muted)
            setLineSpacing(dp(4).toFloat(), 1f)
        }, matchWidth())

        setContentView(scroll)

        chooseButton.setOnClickListener {
            openVideoPicker()
        }

        estimateButton.setOnClickListener {
            updateEstimate()
        }

        processButton.setOnClickListener {
            startProcessing()
        }
    }

    // =========================================================
    // UI HELPERS
    // =========================================================

    private fun rounded(
        color: Int,
        radius: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun sectionCard(
        content: LinearLayout.() -> Unit
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(17), dp(15), dp(17))
            background = rounded(card, 18)
            content()
        }
    }

    private fun sectionTitle(
        number: String,
        title: String
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(12))

            addView(TextView(this@MainActivity).apply {
                text = number
                textSize = 13f
                setTextColor(cyan)
                gravity = Gravity.CENTER
                setTypeface(null, Typeface.BOLD)
                background = rounded(
                    Color.rgb(19, 53, 91),
                    9
                )
            }, LinearLayout.LayoutParams(dp(38), dp(32)))

            addView(TextView(this@MainActivity).apply {
                text = title
                textSize = 13f
                setTextColor(white)
                setTypeface(null, Typeface.BOLD)
                setPadding(dp(12), 0, 0, 0)
            })
        }
    }

    private fun fieldTitle(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 13f
            setTextColor(Color.rgb(200, 215, 235))
            setPadding(0, 0, 0, dp(8))
        }
    }

    private fun button(
        value: String,
        color: Int
    ): Button {
        return Button(this).apply {
            text = value
            textSize = 14f
            isAllCaps = false
            setTextColor(white)
            background = rounded(color, 14)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            minHeight = dp(54)
        }
    }

    private fun label(
        value: String,
        color: Int
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = 13f
            setTextColor(color)
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }
    }

    private fun spinner(items: List<String>): Spinner {
        val adapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            items
        ) {
            override fun getView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                return super.getView(
                    position, convertView, parent
                ).apply {
                    (this as? TextView)?.apply {
                        setTextColor(white)
                        textSize = 14f
                    }
                }
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                return super.getDropDownView(
                    position, convertView, parent
                ).apply {
                    (this as? TextView)?.apply {
                        setTextColor(Color.BLACK)
                        setPadding(
                            dp(12), dp(12), dp(12), dp(12)
                        )
                    }
                }
            }
        }

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        return Spinner(this).apply {
            this.adapter = adapter
            background = rounded(field, 12)
            setPadding(dp(8), dp(5), dp(8), dp(5))
        }
    }

    private fun space(parent: LinearLayout, height: Int) {
        parent.addView(
            View(this),
            LinearLayout.LayoutParams(1, dp(height))
        )
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    // =========================================================
    // VIDEO PICKER
    // =========================================================

    private fun openVideoPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
        }

        @Suppress("DEPRECATION")
        startActivityForResult(intent, PICK_VIDEO)
    }

    @Deprecated("Uses Android activity result callback")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != PICK_VIDEO || resultCode != RESULT_OK) {
            return
        }

        val uri = data?.data ?: return

        sourceFile = null
        sourceDuration = 0.0
        sourceSize = 0L
        sourceHasAudio = false

        processButton.isEnabled = false
        chooseButton.isEnabled = false
        estimateButton.isEnabled = false

        progressBar.progress = 0
        progressText.text = "Progres: 0%"
        status.text = "Status: Membaca video..."
        videoInfo.text = "Sedang menyiapkan video..."

        executor.execute {
            try {
                val file = copyToCache(uri)

                FFmpegKit.executeAsync(
                    "-i \"${file.absolutePath}\""
                ) { session ->

                    val logs = session.allLogsAsString ?: ""

                    val match = Regex(
                        """Duration:\s*(\d+):(\d+):(\d+(?:\.\d+)?)"""
                    ).find(logs)

                    val hasAudio = Regex(
                        """Stream #\d+:\d+.*Audio:""",
                        RegexOption.IGNORE_CASE
                    ).containsMatchIn(logs)

                    val duration = if (match != null) {
                        match.groupValues[1].toDouble() * 3600 +
                            match.groupValues[2].toDouble() * 60 +
                            match.groupValues[3].toDouble()
                    } else {
                        0.0
                    }

                    handler.post {
                        chooseButton.isEnabled = true
                        estimateButton.isEnabled = true

                        if (duration > 0.0) {
                            sourceFile = file
                            sourceDuration = duration
                            sourceSize = file.length()
                            sourceHasAudio = hasAudio

                            videoInfo.text =
                                "VIDEO SIAP\n" +
                                "Durasi: ${formatDuration(duration)}\n" +
                                "Ukuran: ${formatBytes(sourceSize)}\n" +
                                "Audio: ${if (hasAudio) "tersedia" else "tidak ditemukan"}"

                            processButton.isEnabled = true
                            status.text = "Status: Video berhasil dibaca."

                            updateEstimate()
                        } else {
                            processButton.isEnabled = false
                            status.text =
                                "Gagal membaca durasi video. Coba video lain."
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

        val file = File(
            cacheDir,
            "input_${System.currentTimeMillis()}.mp4"
        )

        input.use { source ->
            file.outputStream().use { destination ->
                source.copyTo(destination)
            }
        }

        if (file.length() <= 0L) {
            throw Exception("File video kosong.")
        }

        return file
    }

    // =========================================================
    // ESTIMATION
    // =========================================================

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

        val effectiveDuration =
            if (crossfade) {
                sourceDuration - CROSSFADE_SECONDS
            } else {
                sourceDuration
            }

        val repetitions = ceil(
            minutes * 60.0 /
                effectiveDuration.coerceAtLeast(1.0)
        ).toInt()

        val estimatedSize =
            sourceSize.toDouble() * minutes * 60.0 / sourceDuration

        estimate.text =
            "Durasi sumber: ${formatDuration(sourceDuration)}\n" +
            "Durasi target: $minutes menit\n" +
            "Mode: ${if (crossfade) "Seamless Crossfade" else "Loop biasa"}\n" +
            "Perkiraan pengulangan: $repetitions kali\n" +
            "Ukuran sumber: ${formatBytes(sourceSize)}\n" +
            "Estimasi ukuran hasil: ${formatBytes(estimatedSize)}\n\n" +
            "Estimasi bersifat perkiraan dan dapat berubah."
    }

    // =========================================================
    // VIDEO PROCESSING
    // =========================================================

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

        if (
            crossfade &&
            sourceDuration <= CROSSFADE_SECONDS * 2
        ) {
            status.text =
                "Video terlalu pendek untuk crossfade. " +
                "Pilih video lebih dari 2 detik atau gunakan Loop biasa."
            return
        }

        val resolution = resolutionSpinner.selectedItemPosition
        val quality = qualitySpinner.selectedItemPosition
        val audioMode = audioSpinner.selectedItemPosition

        val includeAudio = audioMode != 2 && sourceHasAudio

        val timestamp = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.US
        ).format(Date())

        val output = File(
            cacheDir,
            "SeamlessLoop_$timestamp.mp4"
        )

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

        val audioOptions =
            if (!includeAudio) "-an"
            else "-c:a aac -b:a 128k"

        val command = if (crossfade) {
            buildCrossfadeCommand(
                input,
                output,
                targetSeconds,
                sourceDuration,
                scaleFilter,
                qualityValue,
                includeAudio,
                audioMode
            )
        } else {
            val repetitions = ceil(
                targetSeconds.toDouble() / sourceDuration
            ).toInt().coerceAtLeast(1)

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
        progressText.text = "Progres: 0%"

        status.text =
            if (crossfade) {
                "Status: Memproses crossfade. Ini bisa lebih lama..."
            } else {
                "Status: Mengekspor video..."
            }

        FFmpegKit.executeAsync(
            command,
            { session ->

                val success =
                    ReturnCode.isSuccess(session.returnCode) &&
                    output.exists() &&
                    output.length() > 0L

                if (success) {
                    try {
                        saveToGallery(output)

                        handler.post {
                            progressBar.progress = 100
                            progressText.text = "Progres: 100%"

                            status.text =
                                "BERHASIL!\n\n" +
                                "Durasi target: $minutes menit\n" +
                                "Ukuran hasil: ${formatBytes(output.length())}\n" +
                                "Codec: MPEG-4\n" +
                                "Mode: ${if (crossfade) "Seamless Crossfade" else "Loop biasa"}\n" +
                                "Audio: ${
                                    if (!includeAudio) {
                                        "tanpa audio"
                                    } else if (crossfade && audioMode == 1) {
                                        "crossfade"
                                    } else {
                                        "dipertahankan"
                                    }
                                }\n" +
                                "Lokasi: Movies/SeamlessLoop Studio"

                            Toast.makeText(
                                this,
                                "Video berhasil disimpan ke Galeri!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        handler.post {
                            status.text =
                                "Video selesai, tetapi gagal disimpan: ${e.message}"
                        }
                    }
                } else {
                    val logs = session.allLogsAsString ?: ""

                    handler.post {
                        status.text =
                            "Ekspor gagal. Periksa ruang penyimpanan dan format video.\n" +
                            logs.takeLast(600)
                    }
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
                val percent = (
                    statistics.time.toDouble() /
                    (targetSeconds * 1000.0) * 100.0
                ).toInt().coerceIn(0, 99)

                handler.post {
                    progressBar.progress = percent
                    progressText.text = "Progres: $percent%"

                    status.text =
                        "Status: Mengekspor video...\n" +
                        "Waktu video diproses: " +
                        formatDuration(statistics.time / 1000.0)
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

        val repetitions = ceil(
            targetSeconds / (sourceDuration - overlap)
        ).toInt().coerceAtLeast(2)

        val inputs = StringBuilder()
        val filters = StringBuilder()

        repeat(repetitions) {
            inputs.append("-i \"${input.absolutePath}\" ")
        }

        for (i in 0 until repetitions) {
            filters.append(
                "[$i:v]setpts=PTS-STARTPTS," +
                "$scaleFilter,format=yuv420p[v$i];"
            )

            if (includeAudio) {
                filters.append(
                    "[$i:a]aresample=async=1:first_pts=0," +
                    "atrim=duration=$sourceDuration," +
                    "apad=whole_dur=$sourceDuration," +
                    "asetpts=PTS-STARTPTS[a$i];"
                )
            }
        }

        var currentVideo = "v0"
        var currentAudio = "a0"
        var currentDuration = sourceDuration

        for (i in 1 until repetitions) {
            val offset = (
                currentDuration - overlap
            ).coerceAtLeast(0.0)

            filters.append(
                "[$currentVideo][v$i]" +
                "xfade=transition=fade:" +
                "duration=$overlap:offset=$offset[vx$i];"
            )

            currentVideo = "vx$i"
            currentDuration += sourceDuration - overlap

            if (includeAudio && audioMode == 1) {
                filters.append(
                    "[$currentAudio][a$i]" +
                    "acrossfade=d=$overlap:c1=tri:c2=tri[ax$i];"
                )

                currentAudio = "ax$i"
            }
        }

        filters.append(
            "[$currentVideo]trim=duration=$targetSeconds," +
            "setpts=PTS-STARTPTS[vout]"
        )

        if (includeAudio) {
            if (audioMode == 1) {
                filters.append(
                    ";[$currentAudio]atrim=duration=$targetSeconds," +
                    "asetpts=PTS-STARTPTS[aout]"
                )
            } else {
                val audioInputs = (0 until repetitions)
                    .joinToString("") { "[a$it]" }

                filters.append(
                    ";$audioInputs concat=n=$repetitions:v=0:a=1," +
                    "atrim=duration=$targetSeconds," +
                    "asetpts=PTS-STARTPTS[aout]"
                )
            }
        }

        return buildString {
            append(inputs)
            append("-filter_complex \"$filters\" ")
            append("-map \"[vout]\" ")

            if (includeAudio) {
                append("-map \"[aout]\" ")
            }

            append("-c:v mpeg4 -q:v $qualityValue -pix_fmt yuv420p ")

            if (!includeAudio) {
                append("-an ")
            } else {
                append("-c:a aac -b:a 128k ")
            }

            append("-t $targetSeconds -movflags +faststart ")
            append("-y \"${output.absolutePath}\"")
        }
    }

    // =========================================================
    // SAVE VIDEO
    // =========================================================

    private fun saveToGallery(file: File): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(
                MediaStore.Video.Media.RELATIVE_PATH,
                Environment.DIRECTORY_MOVIES +
                    "/SeamlessLoop Studio"
            )
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val collection = MediaStore.Video.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL_PRIMARY
        )

        val uri = contentResolver.insert(collection, values)
            ?: throw Exception("Galeri tidak dapat membuat file.")

        try {
            val destination = contentResolver.openOutputStream(uri)
                ?: throw Exception("Galeri tidak dapat dibuka.")

            destination.use { out ->
                file.inputStream().use { it.copyTo(out) }
            }

            val finished = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }

            contentResolver.update(uri, finished, null, null)

            return uri
        } catch (e: Exception) {
            contentResolver.delete(uri, null, null)
            throw e
        }
    }

    // =========================================================
    // FORMATTING
    // =========================================================

    private fun formatDuration(seconds: Double): String {
        val total = seconds.toInt().coerceAtLeast(0)
        val hours = total / 3600
        val minutes = (total % 3600) / 60
        val secs = total % 60

        return if (hours > 0) {
            String.format(
                Locale.US,
                "%02d:%02d:%02d",
                hours,
                minutes,
                secs
            )
        } else {
            String.format(
                Locale.US,
                "%02d:%02d",
                minutes,
                secs
            )
        }
    }

    private fun formatBytes(bytes: Long): String {
        return formatBytes(bytes.toDouble())
    }

    private fun formatBytes(bytes: Double): String {
        if (!bytes.isFinite() || bytes <= 0.0) {
            return "0 MB"
        }

        val mb = bytes / (1024.0 * 1024.0)

        return if (mb >= 1024.0) {
            DecimalFormat("0.00").format(mb / 1024.0) + " GB"
        } else {
            DecimalFormat("0.00").format(mb) + " MB"
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}

// =============================================================
// LOGO PLAY + LOOP
// =============================================================

private class LoopLogoView(
    context: android.content.Context
) : View(context) {

    private val paint = android.graphics.Paint(
        android.graphics.Paint.ANTI_ALIAS_FLAG
    )

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = width.coerceAtMost(height) * 0.30f

        // Lingkaran loop
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = width * 0.065f
        paint.strokeCap = android.graphics.Paint.Cap.ROUND
        paint.color = Color.rgb(0, 190, 255)

        canvas.drawArc(
            cx - radius,
            cy - radius,
            cx + radius,
            cy + radius,
            40f,
            290f,
            false,
            paint
        )

        // Panah loop
        paint.style = android.graphics.Paint.Style.FILL

        val arrow = android.graphics.Path().apply {
            moveTo(
                cx + radius * 0.92f,
                cy - radius * 0.12f
            )
            lineTo(
                cx + radius * 1.28f,
                cy + radius * 0.02f
            )
            lineTo(
                cx + radius * 1.04f,
                cy + radius * 0.35f
            )
            close()
        }

        canvas.drawPath(arrow, paint)

        // Tombol play
        paint.color = Color.WHITE

        val play = android.graphics.Path().apply {
            moveTo(
                cx - radius * 0.28f,
                cy - radius * 0.52f
            )
            lineTo(
                cx + radius * 0.48f,
                cy
            )
            lineTo(
                cx - radius * 0.28f,
                cy + radius * 0.52f
            )
            close()
        }

        canvas.drawPath(play, paint)
    }
}
