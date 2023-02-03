package com.realityexpander.aprocessor

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Validate(
    val matchRegex: String = "",
)