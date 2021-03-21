package com.example.whatsapp.dialogs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.example.whatsapp.R
import com.example.whatsapp.databinding.BottomSheetLayoutBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class BottomSheetDialog : BottomSheetDialogFragment() {

    private lateinit var bottomSheetBinding: BottomSheetLayoutBinding
    private lateinit var listener: BottomSheetListener

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bottomSheetBinding =
            DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_layout, container, false)

        bottomSheetBinding.pdfFab.setOnClickListener {
            listener.onFabClicked("PDF")
            dismiss()
        }

        bottomSheetBinding.msWordFab.setOnClickListener {
            listener.onFabClicked("Ms Word")
            dismiss()
        }

        bottomSheetBinding.imageFab.setOnClickListener {
            listener.onFabClicked("Image")
            dismiss()
        }

        bottomSheetBinding.audioFab.setOnClickListener {
            listener.onFabClicked("Audio")
            dismiss()
        }

        bottomSheetBinding.videoFab.setOnClickListener {
            listener.onFabClicked("Video")
            dismiss()
        }

        bottomSheetBinding.contactFab.setOnClickListener {
            listener.onFabClicked("Contact")
            dismiss()
        }

        return bottomSheetBinding.root
    }

    interface BottomSheetListener {
        fun onFabClicked(textUnderFab: String?)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as BottomSheetListener
        } catch (e: ClassCastException) {
            throw ClassCastException(
                "$context must implement BottomSheetListener"
            )
        }
    }
}