package com.realityexpander.annotationprocessor

import com.realityexpander.aprocessor.Encapsulate
import com.realityexpander.aprocessor.Validate


@Encapsulate("hello world!!!")
data class Model(
    val counter: Int,
    val post : kotlin.String
)