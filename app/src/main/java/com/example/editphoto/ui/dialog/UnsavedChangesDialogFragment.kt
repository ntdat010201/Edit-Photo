package com.example.editphoto.ui.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.example.editphoto.R

class UnsavedChangesDialogFragment : DialogFragment() {

    private var onSave: (() -> Unit)? = null
    private var onDiscard: (() -> Unit)? = null
    private var onCancel: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_unsaved_changes, container, false)

        val title = requireArguments().getString(KEY_TITLE) ?: DEFAULT_TITLE
        val message = requireArguments().getString(KEY_MESSAGE) ?: DEFAULT_MESSAGE
        val positiveText = requireArguments().getString(KEY_POSITIVE) ?: DEFAULT_POSITIVE
        val negativeText = requireArguments().getString(KEY_NEGATIVE) ?: DEFAULT_NEGATIVE
        val neutralText = requireArguments().getString(KEY_NEUTRAL) ?: DEFAULT_NEUTRAL

        view.findViewById<TextView>(R.id.tvTitle).text = title
        view.findViewById<TextView>(R.id.tvMessage).text = message

        view.findViewById<Button>(R.id.btnPositive).apply {
            text = positiveText
            setOnClickListener {
                onSave?.invoke()
                dismissAllowingStateLoss()
            }
        }
        view.findViewById<Button>(R.id.btnNegative).apply {
            text = negativeText
            setOnClickListener {
                onDiscard?.invoke()
                dismissAllowingStateLoss()
            }
        }
        view.findViewById<Button>(R.id.btnNeutral).apply {
            text = neutralText
            setOnClickListener {
                onCancel?.invoke()
                dismissAllowingStateLoss()
            }
        }


        view.findViewById<View>(R.id.overlay).apply {
            isClickable = true
            setOnClickListener { /* swallow clicks */ }
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }

    companion object {
        private const val KEY_TITLE = "title"
        private const val KEY_MESSAGE = "message"
        private const val KEY_POSITIVE = "positiveText"
        private const val KEY_NEGATIVE = "negativeText"
        private const val KEY_NEUTRAL = "neutralText"

        private const val DEFAULT_TITLE = "Lưu chỉnh sửa?"
        private const val DEFAULT_MESSAGE = "Bạn có muốn lưu các chỉnh sửa đang thực hiện không?"
        private const val DEFAULT_POSITIVE = "Lưu"
        private const val DEFAULT_NEGATIVE = "Không"
        private const val DEFAULT_NEUTRAL = "Hủy"

        fun show(
            fm: FragmentManager,
            onSave: () -> Unit,
            onDiscard: () -> Unit,
            onCancel: (() -> Unit)? = null,
            title: String = DEFAULT_TITLE,
            message: String = DEFAULT_MESSAGE,
            positiveText: String = DEFAULT_POSITIVE,
            negativeText: String = DEFAULT_NEGATIVE,
            neutralText: String = DEFAULT_NEUTRAL,
        ): UnsavedChangesDialogFragment {
            val frag = UnsavedChangesDialogFragment()
            frag.onSave = onSave
            frag.onDiscard = onDiscard
            frag.onCancel = onCancel
            frag.arguments = bundleOf(
                KEY_TITLE to title,
                KEY_MESSAGE to message,
                KEY_POSITIVE to positiveText,
                KEY_NEGATIVE to negativeText,
                KEY_NEUTRAL to neutralText,
            )
            frag.show(fm, "UnsavedChangesDialog")
            return frag
        }
    }
}