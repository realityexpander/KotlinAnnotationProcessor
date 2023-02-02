package com.realityexpander.aprocessor

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Encapsulate(val value: String = "")