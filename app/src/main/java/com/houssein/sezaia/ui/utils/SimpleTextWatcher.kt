package com.houssein.sezaia.ui.utils

import android.text.Editable

open class SimpleTextWatcher : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(s: Editable?) {}
}
