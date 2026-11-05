package com.mymoises.mystickerapp

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.TextView

class StickerKeyboardService : InputMethodService() {

    override fun onCreateInputView(): View {
        val textView = TextView(this)

        textView.text = "My Sticker"
        textView.textSize = 20f
        textView.setPadding(30, 30, 30, 30)

        return textView
    }
}
