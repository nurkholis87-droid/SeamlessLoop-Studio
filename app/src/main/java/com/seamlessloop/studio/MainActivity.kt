package com.seamlessloop.studio

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {

    private var videoUri: Uri? = null
    private lateinit var status: TextView
    private lateinit var processButton: Button
    private lateinit var loopInput: EditText

    private val pickVideoCode = 100

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
            text = "\nLooping video langsung dari HP kamu"
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
        }

        processButton = Button(this).apply {
            text = "🔁 PROSES LOOPING"
            isEnabled = false
            setOnClickListener {
                status.text =
                    "Video sudah dipilih. Mesin pemrosesan akan dihubungkan pada tahap berikutnya."
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

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (
            requestCode == pickVideoCode &&
            resultCode == RESULT_OK
        ) {
            videoUri = data?.data

            if (videoUri != null) {
                processButton.isEnabled = true
                status.text = "\nVideo berhasil dipilih! Siap melanjutkan."
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
}
