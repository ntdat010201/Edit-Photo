package com.example.editphoto.utils.extent

import com.example.editphoto.R
import com.example.editphoto.enums.FeatureType
import com.example.editphoto.enums.SubType
import com.example.editphoto.model.AdjustModel
import com.example.editphoto.model.StickerModel
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
    SubModel("Lông mày", SubType.EYEBROW),
    SubModel("Mờ", SubType.BLUR),
)

val listAdjustSub = listOf(
    SubModel("Tỷ Lệ", SubType.CUT),
    SubModel("Lật", SubType.FLIP),
    SubModel("Xoay", SubType.TURN),
)

val listStickerSub = listOf(
    SubModel("Icon", SubType.ICON),
    SubModel("Sticker", SubType.STICKER),
    SubModel("Text", SubType.TEXT),
)

val listSticker = listOf(
    StickerModel(R.drawable.ic_sticker_1),
    StickerModel(R.drawable.ic_sticker_2),
    StickerModel(R.drawable.ic_sticker_3),
    StickerModel(R.drawable.ic_sticker_4),
    StickerModel(R.drawable.ic_sticker_5),
    StickerModel(R.drawable.ic_sticker_6),
    StickerModel(R.drawable.ic_sticker_7),
    StickerModel(R.drawable.ic_sticker_8),
    StickerModel(R.drawable.ic_sticker_9),
    StickerModel(R.drawable.ic_sticker_10),
    StickerModel(R.drawable.ic_sticker_11),
    StickerModel(R.drawable.ic_sticker_1),
    StickerModel(R.drawable.ic_sticker_2),
    StickerModel(R.drawable.ic_sticker_3),
    StickerModel(R.drawable.ic_sticker_4),
)
