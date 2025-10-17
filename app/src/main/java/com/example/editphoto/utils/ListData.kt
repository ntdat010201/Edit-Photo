package com.example.editphoto.utils

import com.example.editphoto.R
import com.example.editphoto.enums.FeatureType
import com.example.editphoto.enums.SubType
import com.example.editphoto.model.AdjustModel
import com.example.editphoto.model.SubModel

val listAdjust = listOf(
    /*AdjustModel(R.drawable.ic_crop, "Cắt", FeatureType.CROP),
    AdjustModel(R.drawable.ic_cut, "Tẩy", FeatureType.CUT),*/
    AdjustModel(R.drawable.ic_ratio, "Tỷ lệ", FeatureType.RADIO),
 /*   AdjustModel(R.drawable.ic_dessy, "Khuôn mặt", FeatureType.DESSY),*/
    AdjustModel(R.drawable.ic_face, "Khuôn mặt", FeatureType.FACE),
    AdjustModel(R.drawable.ic_sticker, "Sticker", FeatureType.STICKER),
  /*  AdjustModel(R.drawable.ic_text, "Văn bản", FeatureType.TEXT),*/
)

val listFace = listOf(
    SubModel(R.drawable.ic_filter, "Lips", SubType.LIPS),
    SubModel(R.drawable.ic_filter, "Eyes", SubType.EYES),
    SubModel(R.drawable.ic_filter, "Cheeks", SubType.CHEEKS),
    SubModel(R.drawable.ic_filter, "White", SubType.WHITE),
    SubModel(R.drawable.ic_filter, "Blur", SubType.BLUR),
)