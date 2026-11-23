package com.soulbrowser.app
import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
class QrCodeScannerContract : ActivityResultContract<Unit, String?>() {
    override fun createIntent(context: Context, input: Unit): Intent = Intent("com.google.zxing.client.android.SCAN").apply { putExtra("SCAN_MODE", "QR_CODE_MODE") }
    override fun parseResult(resultCode: Int, intent: Intent?): String? {
        if (resultCode != Activity.RESULT_OK) return null
        return intent?.getStringExtra("SCAN_RESULT")?.takeIf { it.isNotBlank() }
    }
}
