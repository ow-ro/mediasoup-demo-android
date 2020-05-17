package org.mediasoup.droid.demo.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.core.content.getSystemService

object ClipboardCopy {
    fun clipboardCopy(context: Context, content: String?, tipsResId: Int) {
        val clipboard = context.getSystemService<ClipboardManager>() ?: return
        val clip = ClipData.newPlainText("label", content)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, tipsResId, Toast.LENGTH_SHORT).show()
    }
}