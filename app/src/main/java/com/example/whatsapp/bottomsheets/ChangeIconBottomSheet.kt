package com.example.whatsapp.bottomsheets

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.example.whatsapp.R
import com.example.whatsapp.databinding.ChangeGroupIconBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ChangeIconBottomSheet: BottomSheetDialogFragment() {
    private lateinit var bottomSheetBinding: ChangeGroupIconBottomSheetBinding
    private lateinit var listener: BottomSheetListener

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bottomSheetBinding =
            DataBindingUtil.inflate(inflater,
                R.layout.change_group_icon_bottom_sheet, container, false)


        bottomSheetBinding.removeIconFab.setOnClickListener{
            listener.onFabClicked("Remove icon")
        }

        bottomSheetBinding.galleryFab.setOnClickListener {
            listener.onFabClicked("Gallery")
        }

        bottomSheetBinding.cameraFab.setOnClickListener {
            listener.onFabClicked("Camera")
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