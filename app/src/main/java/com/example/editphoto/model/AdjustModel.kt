package com.example.editphoto.model

import com.example.editphoto.enums.FeatureType

data class AdjustModel(
    var icon: Int,
    var text: String,
    val type: FeatureType
)
