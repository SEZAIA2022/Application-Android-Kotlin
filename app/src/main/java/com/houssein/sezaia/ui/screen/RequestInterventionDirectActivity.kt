package com.houssein.sezaia.ui.screen

import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.houssein.sezaia.R
import com.houssein.sezaia.ui.utils.UIUtils

class RequestInterventionDirectActivity : AppCompatActivity() {

    private lateinit var scroll: NestedScrollView
    private lateinit var idQr: TextInputEditText
    private lateinit var idQrLayout: TextInputLayout
    private lateinit var comment: TextInputEditText
    private lateinit var commentLayout: TextInputLayout
    private lateinit var confirmButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Le clavier redimensionne la zone utile
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        setContentView(R.layout.activity_request_intervention_direct)
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        scroll = findViewById(R.id.scroll)
        applyImeAndSystemBarsPadding(scroll)
        idQr = findViewById(R.id.idQr)
        idQrLayout = findViewById(R.id.idQrLayout)
        comment = findViewById(R.id.comment)
        commentLayout = findViewById(R.id.commentLayout)
        confirmButton = findViewById(R.id.confirmButton)

        // Auto-scroll vers le champ en focus
        attachFocusAutoScroll(
            scroll,
            listOf(idQr, comment)
        )
    }

    /** Ajoute un padding bas = max(system bars, clavier) pour garder le bouton visible. */
    private fun applyImeAndSystemBarsPadding(target: View) {
        val start = target.paddingLeft
        val top = target.paddingTop
        val end = target.paddingRight
        val baseBottom = target.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(target) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.updatePadding(left = start, top = top, right = end, bottom = baseBottom + maxOf(sys.bottom, ime.bottom))
            WindowInsetsCompat.CONSUMED
        }
    }

    /** Fait défiler seulement si l’input n’est pas entièrement visible. */
    private fun attachFocusAutoScroll(nsv: NestedScrollView, views: List<View>) {
        val margin = dp(12f)
        views.forEach { v ->
            v.setOnFocusChangeListener { fv, hasFocus ->
                if (hasFocus) fv.post { ensureVisible(nsv, fv, margin) }
            }
        }
    }

    private fun ensureVisible(nsv: NestedScrollView, child: View, margin: Int) {
        val r = Rect()
        child.getDrawingRect(r)
        nsv.offsetDescendantRectToMyCoords(child, r)

        val top = nsv.scrollY + nsv.paddingTop
        val bottom = nsv.scrollY + nsv.height - nsv.paddingBottom

        when {
            r.top - margin < top -> nsv.smoothScrollTo(0, r.top - nsv.paddingTop - margin)
            r.bottom + margin > bottom -> {
                val y = r.bottom - (nsv.height - nsv.paddingBottom) + margin
                nsv.smoothScrollTo(0, y)
            }
        }
    }

    private fun dp(v: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()
}
