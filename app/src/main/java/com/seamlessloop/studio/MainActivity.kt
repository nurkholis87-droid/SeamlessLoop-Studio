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
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
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
    private lateinit var durationInput: EditText
    private lateinit var estimate: TextView
    private lateinit var chooseButton: Button
    private lateinit var estimateButton: Button
    private lateinit var processButton: Button

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    private var videoUri: Uri? = null
    private var sourceFile: File? = null
    private var sourceDurationSeconds = 0.0
    private var sourceFileSize = 0L
    private var isProcessing = false

    companion object {
        private const val PICK_VIDEO = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createInterface()
    }

    private fun createInterface() {
        val background = Color.rgb(12, 18, 35)
        val cardColor = Color.rgb(25, 35, 57)
        val accent = Color.rgb(50, 170, 255)
        val white = Color.WHITE
        val muted = Color.rgb(185, 197, 215)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(background)
        }

        val scroll = ScrollView(this)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(24), dp(22), dp(28))
        }

        val title = TextView(this).apply {
            text = "SeamlessLoop Studio"
            textSize = 26f
            setTextColor(white)
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val subtitle = TextView(this).apply {
            text = "VIDEO LOOP & LONG VIDEO MAKER"
            textSize = 12f
            setTextColor(accent)
            gravity = Gravity.CENTER
        }

        content.addView(title)
        content.addView(subtitle)

        addSpace(content, 24)

        val infoCard = TextView(this).apply {
            text = "Buat video panjang dari video pendek. " +
                "Pilih video, tentukan durasi, lalu simpan hasilnya."
            textSize = 15f
            setTextColor(white)
            setPadding(dp(16), dp(18), dp(16), dp(18))
            setBackgroundColor(cardColor)
        }

        content.addView(
            infoCard,
            matchWidth()
        )

        addSpace(content, 20)

        chooseButton = makeButton(
            "1. PILIH VIDEO",
            accent
        )

        content.addView(chooseButton, matchWidth())

        videoInfo = TextView(this).apply {
            text = "Belum ada video yang dipilih."
            textSize = 14f
            setTextColor(muted)
            setPadding(dp(4), dp(12), dp(4), dp(8))
        }

        content.addView(videoInfo)

        addSpace(content, 12)

        val durationLabel = TextView(this).apply {
            text = "2. DURASI VIDEO HASIL (MENIT)"
            textSize = 14f
            setTextColor(white)
        }

        content.addView(durationLabel)

        durationInput = EditText(this).apply {
            setText("10")
            textSize = 18f
            setTextColor(white)
            setHintTextColor(muted)
            hint = "Masukkan durasi 1–60 menit"
            inputType =
                android.text.InputType.TYPE_CLASS_NUMBER
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(cardColor)
        }

        content.addView(
            durationInput,
            matchWidth()
        )

        addSpace(content, 16)

        estimateButton = makeButton(
            "3. HITUNG ESTIMASI",
            Color.rgb(72, 95, 135)
        )

        content.addView(
            estimateButton,
            matchWidth()
        )

        estimate = TextView(this).apply {
            text = "Estimasi akan muncul di sini."
            textSize = 14f
            setTextColor(muted)
            setPadding(dp(4), dp(16), dp(4), dp(16))
        }

        content.addView(estimate)

        processButton = makeButton(
            "4. BUAT VIDEO PANJANG",
            Color.rgb(35, 145, 100)
        )

        processButton.isEnabled = false

        content.addView(
            processButton,
            matchWidth()
        )

        addSpace(content, 18)

        status = TextView(this).apply {
            text = "Status: Siap digunakan."
            textSize = 14f
            setTextColor(white)
            setPadding(dp(12), dp(16), dp(12), dp(16))
            setBackgroundColor(cardColor)
        }

        content.addView(status, matchWidth())

        addSpace(content, 16)

        val note = TextView(this).apply {
            text = "Catatan: Proses video membutuhkan ruang " +
                "penyimpanan yang cukup. Waktu proses bergantung " +
                "pada durasi video, resolusi, dan kemampuan HP."
            textSize = 12f
            setTextColor(muted)
        }

        content.addView(note)

        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(root)

        chooseButton.setOnClickListener {
            openVideoPicker()
        }

        estimateButton.setOnClickListener {
            updateEstimate()
        }

        processButton.setOnClickListener {
            startLooping()
        }
    }

    private fun makeButton(
        label: String,
        color: Int
    ): Button {
        return Button(this).apply {
            text = label
            textSize = 14f
            setTextColor(Color.WHITE)
            setBackgroundColor(color)
            isAllCaps = false
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
    }

    private fun addSpace(
        parent: LinearLayout,
        height: Int
    ) {
        val space = View(this)
        parent.addView(
            space,
            LinearLayout.LayoutParams(
                1,
                dp(height)
            )
        )
    }

    private fun matchWidth(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun openVideoPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
        }

        @Suppress("DEPRECATION")
        startActivityForResult(intent, PICK_VIDEO)
    }

    @Deprecated("Deprecated Android callback retained for compatibility")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != PICK_VIDEO ||
            resultCode != RESULT_OK
        ) {
            return
        }

        val uri = data?.data ?: return

        videoUri = uri
        sourceFile = null
        sourceDurationSeconds = 0.0
        sourceFileSize = 0L

        processButton.isEnabled = false
        estimate.text = "Menyiapkan video..."
        videoInfo.text = "Video dipilih. Sedang membaca informasi..."
        status.text = "Status: Membaca video..."

        executor.execute {
            try {
                val inputFile = copyUriToCache(uri)

                handler.post {
                    videoInfo.text =
                        "Nama file: ${inputFile.name}\n" +
                        "Ukuran: ${formatBytes(inputFile.length())}"

                    status.text =
                        "Status: Membaca durasi video..."
                }

                val probeCommand =
                    "-i \"${inputFile.absolutePath}\""

                FFmpegKit.executeAsync(probeCommand) { session ->

                    val logs = session.allLogsAsString ?: ""

                    val durationRegex = Regex(
                        """Duration:\s*(\d+):(\d+):(\d+(?:\.\d+)?)"""
                    )

                    val match = durationRegex.find(logs)

                    val duration = if (match != null) {
                        val hours =
                            match.groupValues[1].toDouble()

                        val minutes =
                            match.groupValues[2].toDouble()

                        val seconds =
                            match.groupValues[3].toDouble()

                        hours * 3600 +
                            minutes * 60 +
                            seconds
                    } else {
                        0.0
                    }

                    handler.post {
                        if (duration > 0.0) {
                            sourceFile = inputFile
                            sourceDurationSeconds = duration
                            sourceFileSize = inputFile.length()

                            videoInfo.text =
                                "Video berhasil dipilih\n" +
                                "Durasi: ${formatDuration(duration)}\n" +
                                "Ukuran: ${formatBytes(sourceFileSize)}"

                            processButton.isEnabled = true

                            status.text =
                                "Status: Video siap diproses."

                            updateEstimate()
                        } else {
                            sourceFile = null
                            processButton.isEnabled = false

                            status.text =
                                "Gagal membaca durasi video. " +
                                "Coba pilih file MP4 lain."

                            estimate.text =
                                "Durasi video tidak berhasil dibaca."
                        }
                    }
                }
            } catch (e: Exception) {
                handler.post {
                    processButton.isEnabled = false
                    status.text =
                        "Gagal membaca video: ${e.message}"
                }
            }
        }
    }

    private fun copyUriToCache(uri: Uri): File {
        val input = contentResolver.openInputStream(uri)
            ?: throw Exception("Video tidak dapat dibuka.")

        val outputFile = File(
            cacheDir,
            "source_${System.currentTimeMillis()}.mp4"
        )

        input.use { source ->
            outputFile.outputStream().use { destination ->
                source.copyTo(destination)
            }
        }

        if (!outputFile.exists() || outputFile.length() == 0L) {
            throw Exception("File video kosong atau tidak terbaca.")
        }

        return outputFile
    }

    private fun updateEstimate() {
        if (sourceDurationSeconds <= 0.0 ||
            sourceFileSize <= 0L
        ) {
            estimate.text =
                "Pilih video yang valid terlebih dahulu."
            return
        }

        val minutes = durationInput.text.toString().toIntOrNull()

        if (minutes == null || minutes !in 1..60) {
            estimate.text =
                "Masukkan durasi antara 1 sampai 60 menit."
            return
        }

        val targetSeconds = minutes * 60.0

        val repetitions = ceil(
            targetSeconds / sourceDurationSeconds
        ).toInt().coerceAtLeast(1)

        val estimatedBytes =
            sourceFileSize.toDouble() *
                targetSeconds / sourceDurationSeconds

        estimate.text =
            "Durasi sumber: ${formatDuration(sourceDurationSeconds)}\n" +
            "Durasi target: $minutes menit\n" +
            "Perkiraan pengulangan: $repetitions kali\n" +
            "Perkiraan ukuran hasil: ${formatBytes(estimatedBytes)}\n\n" +
            "Catatan: Ukuran akhir bisa berbeda karena video " +
            "akan dikodekan ulang."
    }

    private fun startLooping() {
        if (isProcessing) return

        val inputFile = sourceFile

        if (inputFile == null ||
            !inputFile.exists() ||
            sourceDurationSeconds <= 0.0
        ) {
            status.text =
                "Status: Pilih video yang valid terlebih dahulu."
            return
        }

        val minutes = durationInput.text.toString().toIntOrNull()

        if (minutes == null || minutes !in 1..60) {
            status.text =
                "Status: Durasi harus 1 sampai 60 menit."
            return
        }

        val targetSeconds = minutes * 60
        val repetitions = ceil(
            targetSeconds / sourceDurationSeconds
        ).toInt().coerceAtLeast(1)

        val timestamp = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.US
        ).format(Date())

        val outputFile = File(
            cacheDir,
            "SeamlessLoop_$timestamp.mp4"
        )

        val command =
            "-stream_loop ${repetitions - 1} " +
            "-i \"${inputFile.absolutePath}\" " +
            "-t $targetSeconds " +
            "-c:v mpeg4 -q:v 4 " +
            "-c:a aac -b:a 128k " +
            "-movflags +faststart " +
            "-y \"${outputFile.absolutePath}\""

        isProcessing = true
        processButton.isEnabled = false
        chooseButton.isEnabled = false
        estimateButton.isEnabled = false

        status.text =
            "Status: Sedang membuat video $minutes menit...\n" +
            "Jangan tutup aplikasi selama proses berlangsung."

        FFmpegKit.executeAsync(command) { session ->

            val returnCode = session.returnCode

            if (ReturnCode.isSuccess(returnCode) &&
                outputFile.exists() &&
                outputFile.length() > 0L
            ) {
                try {
                    val savedUri = saveToGallery(outputFile)

                    handler.post {
                        status.text =
                            "BERHASIL!\n\n" +
                            "Video selesai dibuat.\n" +
                            "Durasi target: $minutes menit\n" +
                            "Ukuran file: ${formatBytes(outputFile.length())}\n" +
                            "Lokasi: Galeri > Movies > SeamlessLoop Studio"

                        if (savedUri != null) {
                            Toast.makeText(
                                this,
                                "Video berhasil disimpan ke Galeri!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    handler.post {
                        status.text =
                            "Video berhasil dibuat, tetapi gagal " +
                            "disimpan ke Galeri: ${e.message}"
                    }
                }
            } else {
                val details = session.failStackTrace ?: ""

                handler.post {
                    status.text =
                        "Gagal membuat video.\n" +
                        "Periksa ruang penyimpanan dan format video.\n" +
                        details.take(400)
                }
            }

            handler.post {
                isProcessing = false
                processButton.isEnabled = sourceFile != null
                chooseButton.isEnabled = true
                estimateButton.isEnabled = true
            }
        }
    }

    private fun saveToGallery(file: File): Uri? {
        val values = ContentValues().apply {
            put(
                MediaStore.Video.Media.DISPLAY_NAME,
                file.name
            )

            put(
                MediaStore.Video.Media.MIME_TYPE,
                "video/mp4"
            )

            put(
                MediaStore.Video.Media.RELATIVE_PATH,
                Environment.DIRECTORY_MOVIES +
                    "/SeamlessLoop Studio"
            )

            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val collection =
            MediaStore.Video.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )

        val uri = contentResolver.insert(
            collection,
            values
        ) ?: throw Exception(
            "Galeri tidak dapat membuat file."
        )

        try {
            val output = contentResolver.openOutputStream(uri)
                ?: throw Exception(
                    "File Galeri tidak dapat dibuka."
                )

            output.use { destination ->
                file.inputStream().use { source ->
                    source.copyTo(destination)
                }
            }

            val finishedValues = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }

            contentResolver.update(
                uri,
                finishedValues,
                null,
                null
            )

            return uri
        } catch (e: Exception) {
            contentResolver.delete(uri, null, null)
            throw e
        }
    }

    private fun formatDuration(seconds: Double): String {
        val totalSeconds = seconds.toInt()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60

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
            DecimalFormat("0.00").format(mb / 1024.0) +
                " GB"
        } else {
            DecimalFormat("0.00").format(mb) + " MB"
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}
