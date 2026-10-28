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
        val navy = Color.rgb(12, 18, 35)
        val card = Color.rgb(27, 38, 61)
        val blue = Color.rgb(45, 150, 245)
        val white = Color.WHITE
        val muted = Color.rgb(185, 198, 218)

        val scroll = ScrollView(this).apply { setBackgroundColor(navy) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(30))
        }
        scroll.addView(root)

        root.addView(TextView(this).apply {
            text = "SeamlessLoop Studio 1.7"
            textSize = 27f
            setTextColor(white)
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "VIDEO LOOP • LONG VIDEO MAKER"
            textSize = 12f
            setTextColor(blue)
            gravity = Gravity.CENTER
        })
        space(root, 24)

        root.addView(TextView(this).apply {
            text = "Buat video panjang dari video pendek.\nPilih loop biasa atau transisi crossfade."
            textSize = 15f
            setTextColor(white)
            setPadding(dp(16), dp(18), dp(16), dp(18))
            setBackgroundColor(card)
        }, matchWidth())
        space(root, 20)

        chooseButton = button("1. PILIH VIDEO", blue)
        root.addView(chooseButton, matchWidth())
        videoInfo = label("Belum ada video yang dipilih.", muted)
        root.addView(videoInfo, matchWidth())

        space(root, 12)
        root.addView(label("2. DURASI HASIL (MENIT)", white))
        durationInput = EditText(this).apply {
            setText("1")
            textSize = 17f
            setTextColor(white)
            setHintTextColor(muted)
            hint = "Masukkan 1–60 menit"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(card)
        }
        root.addView(durationInput, matchWidth())

        space(root, 14)
        root.addView(label("3. MODE LOOP", white))
        modeSpinner = spinner(listOf("Loop biasa", "Seamless Crossfade (1 detik)"), card, white)
        root.addView(modeSpinner, matchWidth())

        space(root, 14)
        root.addView(label("4. RESOLUSI VIDEO", white))
        resolutionSpinner = spinner(listOf("Resolusi asli", "720p", "1080p"), card, white)
        root.addView(resolutionSpinner, matchWidth())

        space(root, 12)
        root.addView(label("5. KUALITAS VIDEO", white))
        qualitySpinner = spinner(listOf("Hemat", "Standar", "Tinggi"), card, white)
        qualitySpinner.setSelection(1)
        root.addView(qualitySpinner, matchWidth())

        space(root, 12)
        root.addView(label("6. AUDIO", white))
        audioSpinner = spinner(listOf("Pertahankan audio", "Tanpa suara"), card, white)
        root.addView(audioSpinner, matchWidth())

        space(root, 18)
        estimateButton = button("HITUNG ESTIMASI", Color.rgb(70, 91, 130))
        root.addView(estimateButton, matchWidth())
        estimate = label("Pilih video untuk menghitung estimasi.", muted)
        root.addView(estimate, matchWidth())

        space(root, 12)
        progressText = label("Progres: 0%", white)
        root.addView(progressText, matchWidth())
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
        }
        root.addView(progressBar, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(12)
        ))

        space(root, 18)
        processButton = button("BUAT VIDEO PANJANG", Color.rgb(30, 145, 100))
        processButton.isEnabled = false
        root.addView(processButton, matchWidth())

        space(root, 18)
        status = label("Status: Siap digunakan.", white)
        status.setPadding(dp(14), dp(16), dp(14), dp(16))
        status.setBackgroundColor(card)
        root.addView(status, matchWidth())

        space(root, 18)
        root.addView(label(
            "Hasil disimpan ke Movies/SeamlessLoop Studio.\nPastikan ruang penyimpanan mencukupi.",
            muted
        ), matchWidth())

        setContentView(scroll)
        chooseButton.setOnClickListener { openVideoPicker() }
        estimateButton.setOnClickListener { updateEstimate() }
        processButton.setOnClickListener { startProcessing() }
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
                FFmpegKit.executeAsync("-i \"${file.absolutePath}\"") { session ->
                    val logs = session.allLogsAsString ?: ""
                    val match = Regex("""Duration:\s*(\d+):(\d+):(\d+(?:\.\d+)?)""").find(logs)
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
                            videoInfo.text = "Video siap digunakan\nDurasi: ${formatDuration(duration)}\nUkuran: ${formatBytes(sourceSize)}"
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
        val noAudio = audioSpinner.selectedItemPosition == 1
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
        val audioOptions = if (noAudio) "-an" else "-c:a aac -b:a 128k"

        val command = if (crossfade) {
            buildCrossfadeCommand(input, output, targetSeconds, sourceDuration, scaleFilter, qualityValue, noAudio)
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
        progressText.text = "Progres: 0%"
        status.text = if (crossfade) "Status: Memproses crossfade. Ini bisa lebih lama..." else "Status: Mengekspor video..."

        FFmpegKit.executeAsync(
            command,
            { session ->
                val success = ReturnCode.isSuccess(session.returnCode) && output.exists() && output.length() > 0L
                if (success) {
                    try {
                        saveToGallery(output)
                        handler.post {
                            progressBar.progress = 100
                            progressText.text = "Progres: 100%"
                            status.text = "BERHASIL!\n\n" +
                                "Durasi target: $minutes menit\n" +
                                "Ukuran hasil: ${formatBytes(output.length())}\n" +
                                "Codec: MPEG-4\n" +
                                "Mode: ${if (crossfade) "Seamless Crossfade" else "Loop biasa"}\n" +
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
                    progressText.text = "Progres: $percent%"
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
        noAudio: Boolean
    ): String {
        val overlap = CROSSFADE_SECONDS
        val repetitions = ceil(targetSeconds / (sourceDuration - overlap)).toInt().coerceAtLeast(2)
        val inputs = StringBuilder()
        val filters = StringBuilder()

        repeat(repetitions) { inputs.append("-i \"${input.absolutePath}\" ") }

        for (i in 0 until repetitions) {
            filters.append("[$i:v]setpts=PTS-STARTPTS,$scaleFilter,format=yuv420p[v$i];")
            if (!noAudio) {
                filters.append("[$i:a]aresample=async=1:first_pts=0,asetpts=PTS-STARTPTS[a$i];")
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
            if (!noAudio) {
                filters.append("[$currentAudio][a$i]acrossfade=d=$overlap:c1=tri:c2=tri[ax$i];")
                currentAudio = "ax$i"
            }
        }

        filters.append("[$currentVideo]trim=duration=$targetSeconds,setpts=PTS-STARTPTS[vout]")
        if (!noAudio) filters.append(";[$currentAudio]atrim=duration=$targetSeconds,asetpts=PTS-STARTPTS[aout]")

        return buildString {
            append(inputs)
            append("-filter_complex \"${filters}\" ")
            append("-map \"[vout]\" ")
            if (!noAudio) append("-map \"[aout]\" ")
            append("-c:v mpeg4 -q:v $qualityValue -pix_fmt yuv420p ")
            if (noAudio) append("-an ") else append("-c:a aac -b:a 128k ")
            append("-t $targetSeconds -movflags +faststart ")
            append("-y \"${output.absolutePath}\"")
        }
    }

    private fun saveToGallery(file: File): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/SeamlessLoop Studio")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = contentResolver.insert(collection, values) ?: throw Exception("Galeri tidak dapat membuat file.")
        try {
            val destination = contentResolver.openOutputStream(uri) ?: throw Exception("Galeri tidak dapat dibuka.")
            destination.use { out -> file.inputStream().use { it.copyTo(out) } }
            val finished = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
            contentResolver.update(uri, finished, null, null)
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
