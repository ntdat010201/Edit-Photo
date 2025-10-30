package com.example.editphoto.utils.extent

import com.example.editphoto.R
import com.example.editphoto.enums.FeatureType
import com.example.editphoto.enums.SubType
import com.example.editphoto.model.AdjustModel
import com.example.editphoto.model.SubModel

val listAdjust = listOf(

    AdjustModel(R.drawable.ic_layout, "Điều chỉnh", FeatureType.ADJUST),
    AdjustModel(R.drawable.ic_make_up, "Khuôn mặt", FeatureType.FACE),
    AdjustModel(R.drawable.ic_sticker, "Sticker", FeatureType.STICKER),
)

val listFaceSub = listOf(
    SubModel("Môi", SubType.LIPS),
    SubModel("Mắt", SubType.EYES),
    SubModel("Má", SubType.CHEEKS),
    SubModel("Tẩy", SubType.WHITE),
    SubModel("Mờ", SubType.BLUR),
)

val listAdjustSub = listOf(
    SubModel("Tỷ Lệ", SubType.CUT),
    SubModel("Lật", SubType.FLIP),
    SubModel("Xoay", SubType.TURN),
)