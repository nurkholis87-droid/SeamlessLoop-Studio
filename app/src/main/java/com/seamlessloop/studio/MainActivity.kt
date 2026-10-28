package com.seamlessloop.studio

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode

class MainActivity : Activity() {

    private var videoUri: Uri? = null

    private lateinit var status: TextView
    private lateinit var processButton: Button
    private lateinit var loopInput: EditText

    private val pickVideoCode = 100
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = 0xFF0B1020.toInt()
        window.navigationBarColor = 0xFF0B1020.toInt()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 30, 24, 24)
            setBackgroundColor(0xFF0B1020.toInt())
        }

        val title = TextView(this).apply {
            text = "SeamlessLoop Studio"
            textSize = 27f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "\nBuat video looping langsung dari HP kamu"
            textSize = 15f
            setTextColor(0xFFB5BBD0.toInt())
            gravity = Gravity.CENTER
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

        loopInput = EditText(this).apply {
            hint = "Jumlah putaran, contoh: 3"
            setText("3")
            inputType = 2
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFFB5BBD0.toInt())
        }

        processButton = Button(this).apply {
            text = "🔁 PROSES LOOPING"
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
        layout.addView(loopInput, matchWrap())
        layout.addView(processButton, matchWrap())
        layout.addView(status, matchWrap())

        setContentView(
            ScrollView(this).apply {
                addView(layout)
            }
        )
    }

    private fun startLooping() {
        val uri = videoUri

        if (uri == null) {
            status.text = "Pilih video terlebih dahulu."
            return
        }

        val count = loopInput.text.toString().toIntOrNull() ?: 0

        if (count < 2 || count > 100) {
            status.text =
                "Jumlah putaran harus antara 2 sampai 100."
            return
        }

        processButton.isEnabled = false
        status.text = "Menyiapkan video..."

        executor.execute {
            var inputFile: File? = null

            try {
                inputFile = File(cacheDir, "input_loop.mp4")

                contentResolver.openInputStream(uri).use { input ->
                    if (input == null) {
                        throw Exception(
                            "Video tidak dapat dibaca."
                        )
                    }

                    FileOutputStream(inputFile).use { output ->
                        input.copyTo(output)
                    }
                }

                if (!inputFile.exists() || inputFile.length() == 0L) {
                    throw Exception(
                        "File video kosong atau gagal disalin."
                    )
                }

                val outputDir =
                    getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                        ?: throw Exception(
                            "Folder penyimpanan video tidak tersedia."
                        )

                if (!outputDir.exists() && !outputDir.mkdirs()) {
                    throw Exception(
                        "Folder output gagal dibuat."
                    )
                }

                val outputFile = File(
                    outputDir,
                    "SeamlessLoop_${System.currentTimeMillis()}.mp4"
                )

                val inputPath = inputFile.absolutePath
                val outputPath = outputFile.absolutePath

                // Loop video menggunakan encoder MPEG-4.
                // Opsi -preset sengaja tidak digunakan.
                val command =
                    "-stream_loop ${count - 1} " +
                    "-i \"$inputPath\" " +
                    "-c:v mpeg4 -q:v 4 " +
                    "-c:a aac -b:a 128k " +
                    "-movflags +faststart " +
                    "-y \"$outputPath\""

                handler.post {
                    status.text =
                        "Memproses video...\nMohon tunggu."
                }

                FFmpegKit.executeAsync(command) { session ->
                    val returnCode = session.returnCode

                    handler.post {
                        processButton.isEnabled = true

                        if (ReturnCode.isSuccess(returnCode) &&
                            outputFile.exists() &&
                            outputFile.length() > 0L
                        ) {
                            status.text =
                                "BERHASIL! 🎉\n\n" +
                                "Jumlah putaran: $count\n" +
                                "Ukuran video: " +
                                formatFileSize(outputFile.length()) +
                                "\n\nLokasi file:\n" +
                                outputFile.absolutePath +
                                "\n\nVideo tersimpan di folder Movies milik aplikasi."
                        } else {
                            val logs =
                                session.allLogsAsString ?: ""

                            val details =
                                logs.takeLast(2500).ifBlank {
                                    "FFmpeg tidak memberikan log error."
                                }

                            status.text =
                                "GAGAL MEMPROSES VIDEO\n\n" +
                                "Kode hasil: $returnCode\n\n" +
                                details

                            android.util.Log.e(
                                "SeamlessLoop",
                                "FFmpeg gagal: $details"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                handler.post {
                    processButton.isEnabled = true

                    status.text =
                        "Terjadi kesalahan:\n" +
                        (e.message ?: "Kesalahan tidak diketahui")
                }
            }
        }
    }

    private fun formatFileSize(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return String.format("%.2f MB", mb)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == pickVideoCode &&
            resultCode == RESULT_OK
        ) {
            videoUri = data?.data

            if (videoUri != null) {
                processButton.isEnabled = true
                status.text =
                    "Video berhasil dipilih!\nSiap diproses."
            } else {
                processButton.isEnabled = false
                status.text = "Video belum dipilih."
            }
        }
    }

    private fun matchWrap(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 12
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
