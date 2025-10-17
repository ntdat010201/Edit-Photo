package com.example.editphoto.base

import androidx.fragment.app.activityViewModels
import com.example.editphoto.viewmodel.EditImageViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


abstract class BaseFeatureBottomSheet : BottomSheetDialogFragment() {

    protected val viewModel: EditImageViewModel by activityViewModels()
}
