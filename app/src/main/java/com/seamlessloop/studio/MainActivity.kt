package com.seamlessloop.studio

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.Executors
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode

class MainActivity : Activity() {

    private var videoUri: Uri? = null

    private lateinit var status: TextView
    private lateinit var processButton: Button
    private lateinit var durationInput: EditText
    private lateinit var estimate: TextView

    private val pickVideoCode = 100
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    private var sourceDurationSeconds = 0.0
    private var sourceFileSize = 0L
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = 0xFF0B1020.toInt()
        window.navigationBarColor = 0xFF0B1020.toInt()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(0xFF0B1020.toInt())
        }

        val title = TextView(this).apply {
            text = "SeamlessLoop Studio"
            textSize = 26f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
        }

        val subtitle = TextView(this).apply {
            text = "\nVERSI 1.4.0 • LONG VIDEO MAKER"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(0xFF9DA9C9.toInt())
        }

        val chooseButton = Button(this).apply {
            text = "📂 PILIH VIDEO"
            setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "video/*"
                }
                startActivityForResult(intent, pickVideoCode)
            }
        }

        val durationLabel = TextView(this).apply {
            text = "\nDurasi target (menit)"
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
        }

        durationInput = EditText(this).apply {
            hint = "Masukkan 1 sampai 60"
            setText("10")
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF9DA9C9.toInt())
        }

        val estimateButton = Button(this).apply {
            text = "📊 HITUNG ESTIMASI"
            setOnClickListener {
                updateEstimate()
            }
        }

        estimate = TextView(this).apply {
            text = "\nPilih video untuk melihat estimasi."
            textSize = 14f
            setTextColor(0xFFB5BBD0.toInt())
        }

        processButton = Button(this).apply {
            text = "🔁 BUAT VIDEO PANJANG"
            isEnabled = false
            setOnClickListener {
                startLooping()
            }
        }

        status = TextView(this).apply {
            text = "\nPilih video untuk memulai."
            textSize = 14f
            setTextColor(0xFFB5BBD0.toInt())
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(chooseButton, matchWrap())
        layout.addView(durationLabel, matchWrap())
        layout.addView(durationInput, matchWrap())
        layout.addView(estimateButton, matchWrap())
        layout.addView(estimate, matchWrap())
        layout.addView(processButton, matchWrap())
        layout.addView(status, matchWrap())

        setContentView(
            ScrollView(this).apply {
                addView(layout)
            }
        )
    }

    private fun updateEstimate() {
        val minutes = durationInput.text.toString().toIntOrNull()

        if (minutes == null || minutes !in 1..60) {
            estimate.text = "Masukkan durasi antara 1 sampai 60 menit."
            return
        }

        if (sourceDurationSeconds <= 0.0) {
            estimate.text = "Pilih video terlebih dahulu."
            return
        }

        val targetSeconds = minutes * 60.0
        val repetitions = kotlin.math.ceil(
            targetSeconds / sourceDurationSeconds
        ).toInt().coerceAtLeast(1)

        val estimatedBytes = if (sourceDurationSeconds > 0) {
            sourceFileSize.toDouble() *
                targetSeconds / sourceDurationSeconds
        } else {
            0.0
        }

        estimate.text =
            "Durasi sumber: ${formatDuration(sourceDurationSeconds)}\n" +
            "Durasi target: $minutes menit\n" +
            "Jumlah putaran: $repetitions kali\n" +
            "Perkiraan ukuran: ${formatSize(estimatedBytes)}\n\n" +
            "Ukuran sebenarnya dapat berbeda setelah encoding."
    }

    private fun startLooping() {
        if (isProcessing) return

        val uri = videoUri ?: run {
            status.text = "Pilih video terlebih dahulu."
            return
        }

        val minutes = durationInput.text.toString().toIntOrNull()

        if (minutes == null || minutes !in 1..60) {
            status.text = "Durasi harus antara 1 sampai 60 menit."
            return
        }

        if (sourceDurationSeconds <= 0.0) {
            status.text = "Durasi video sumber belum diketahui."
            return
        }

        val targetSeconds = minutes * 60.0
        val repetitions = kotlin.math.ceil(
            targetSeconds / sourceDurationSeconds
        ).toInt().coerceAtLeast(1)

        isProcessing = true
        processButton.isEnabled = false
        status.text = "Menyiapkan video..."

        executor.execute {
            try {
                val inputFile = File(cacheDir, "input_loop.mp4")

                contentResolver.openInputStream(uri).use { input ->
                    if (input == null) {
                        throw Exception("Video tidak dapat dibaca.")
                    }

                    FileOutputStream(inputFile).use { output ->
                        input.copyTo(output)
                    }
                }

                if (!inputFile.exists() || inputFile.length() == 0L) {
                    throw Exception("File video kosong.")
                }

                val outputDir =
                    getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                        ?: throw Exception("Folder output tidak tersedia.")

                if (!outputDir.exists() && !outputDir.mkdirs()) {
                    throw Exception("Gagal membuat folder output.")
                }

                val outputFile = File(
                    outputDir,
                    "SeamlessLoop_${System.currentTimeMillis()}.mp4"
                )

                /*
                 * -stream_loop mengulang input sebelum encoding.
                 * -t membatasi durasi akhir sesuai target.
                 * Encoder mpeg4 dipakai karena versi sebelumnya berhasil.
                 */
                val command =
                    "-stream_loop ${repetitions - 1} " +
                    "-i \"${inputFile.absolutePath}\" " +
                    "-t $targetSeconds " +
                    "-c:v mpeg4 -q:v 4 " +
                    "-c:a aac -b:a 128k " +
                    "-movflags +faststart " +
                    "-y \"${outputFile.absolutePath}\""

                handler.post {
                    status.text =
                        "Memproses video...\n\n" +
                        "Target: $minutes menit\n" +
                        "Pengulangan: $repetitions kali\n\n" +
                        "Proses dapat memerlukan waktu."
                }

                FFmpegKit.executeAsync(command) { session ->
                    if (ReturnCode.isSuccess(session.returnCode) &&
                        outputFile.exists() &&
                        outputFile.length() > 0L
                    ) {
                        try {
                            val galleryUri = saveToGallery(outputFile)

                            handler.post {
                                status.text =
                                    if (galleryUri != null) {
                                        "BERHASIL! 🎉\n\n" +
                                        "Target: $minutes menit\n" +
                                        "Pengulangan: $repetitions kali\n" +
                                        "Ukuran: ${formatSize(outputFile.length().toDouble())}\n\n" +
                                        "Video tersimpan di Galeri:\n" +
                                        "Movies/SeamlessLoop Studio"
                                    } else {
                                        "Video berhasil dibuat, tetapi gagal " +
                                        "disalin ke Galeri.\n\n" +
                                        outputFile.absolutePath
                                    }

                                isProcessing = false
                                processButton.isEnabled = true
                            }
                        } catch (e: Exception) {
                            handler.post {
                                status.text =
                                    "Video selesai, tetapi penyimpanan " +
                                    "ke Galeri gagal:\n${e.message}\n\n" +
                                    outputFile.absolutePath

                                isProcessing = false
                                processButton.isEnabled = true
                            }
                        }
                    } else {
                        val logs = session.allLogsAsString ?: ""
                        val details = logs.takeLast(2500).ifBlank {
                            "FFmpeg tidak memberikan log error."
                        }

                        handler.post {
                            status.text =
                                "GAGAL MEMPROSES VIDEO\n\n" +
                                "Kode: ${session.returnCode}\n\n" +
                                details

                            isProcessing = false
                            processButton.isEnabled = true
                        }
                    }
                }
            } catch (e: Exception) {
                handler.post {
                    status.text =
                        "Terjadi kesalahan:\n" +
                        (e.message ?: "Kesalahan tidak diketahui")

                    isProcessing = false
                    processButton.isEnabled = true
                }
            }
        }
    }

    private fun saveToGallery(file: File): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(
                MediaStore.Video.Media.RELATIVE_PATH,
                Environment.DIRECTORY_MOVIES + "/SeamlessLoop Studio"
            )
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val collection = MediaStore.Video.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL_PRIMARY
        )

        val uri = contentResolver.insert(collection, values)
            ?: throw Exception("Galeri tidak dapat membuat file.")

        try {
            val output = contentResolver.openOutputStream(uri)
                ?: throw Exception("File Galeri tidak dapat dibuka.")

            output.use { destination ->
                file.inputStream().use { source ->
                    source.copyTo(destination)
                }
            }

            val finishedValues = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }

            contentResolver.update(uri, finishedValues, null, null)
            return uri
        } catch (e: Exception) {
            contentResolver.delete(uri, null, null)
            throw e
        }
    }

    private fun formatDuration(seconds: Double): String {
        val total = seconds.toLong()
        val minutes = total / 60
        val remainingSeconds = total % 60
        return "${minutes}m ${remainingSeconds}d"
    }

    private fun formatSize(bytes: Double): String {
        val mb = bytes / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.2f MB", mb)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == pickVideoCode && resultCode == RESULT_OK) {
            val uri = data?.data

            if (uri != null) {
                videoUri = uri
                status.text = "Membaca informasi video..."

                executor.execute {
                    try {
                        val tempFile = File(cacheDir, "probe_video.mp4")

                        contentResolver.openInputStream(uri).use { input ->
                            if (input == null) {
                                throw Exception("Video tidak dapat dibaca.")
                            }

                            FileOutputStream(tempFile).use { output ->
                                input.copyTo(output)
                            }
                        }

                        val probeCommand =
                            "-i \"${tempFile.absolutePath}\""

                        FFmpegKit.execute(probeCommand) { session ->
                            val logs = session.allLogsAsString ?: ""

                            val durationRegex =
                                Regex("""Duration:\s*(\d+):(\d+):(\d+(?:\.\d+)?)""")

                            val match = durationRegex.find(logs)

                            val duration = if (match != null) {
                                val hours = match.groupValues[1].toDouble()
                                val minutes = match.groupValues[2].toDouble()
                                val seconds = match.groupValues[3].toDouble()
                                hours * 3600 + minutes * 60 + seconds
                            } else {
                                0.0
                            }

                            handler.post {
                                sourceDurationSeconds = duration
                                sourceFileSize = tempFile.length()

                                processButton.isEnabled = duration > 0.0

                                if (duration > 0.0) {
                                    status.text =
                                        "Video siap diproses!\n" +
                                        "Durasi: ${formatDuration(duration)}"

                                    updateEstimate()
                                } else {
                                    status.text =
                                        "Durasi video tidak terbaca.\n" +
                                        "Coba video MP4 lain."
                                }
                            }
                        }
                    } catch (e: Exception) {
                        handler.post {
                            processButton.isEnabled = false
                            status.text =
                                "Gagal membaca video:\n${e.message}"
                        }
                    }
                }
            }
        }
    }

    private fun matchWrap(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 10
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
