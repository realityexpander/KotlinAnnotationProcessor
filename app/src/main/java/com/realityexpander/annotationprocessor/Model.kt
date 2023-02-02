package com.realityexpander.annotationprocessor

import com.realityexpander.aprocessor.Encapsulate


@Encapsulate("hello world")
data class Model(
    val counter: Int,
    val post : kotlin.String
)

@Validate("""^\d{4}-\d{2}\d{2}$""")
data class DateString(
    val value: String
)

fun String.toDataStringValidated(regexPattern: String): DateString? {
    return if (regexPattern.toRegex().matches(this)) DateString(this) else null
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Validate(val regexPattern: String)
