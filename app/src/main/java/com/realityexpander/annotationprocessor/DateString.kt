package com.realityexpander.annotationprocessor

import com.realityexpander.aprocessor.Validate

@Validate("""^\d{4}-\d{2}-\d{2}$""", false)
data class DateString(
    val value: String
)

// Generates:
//
//@JvmInline
//public value class ValidatedDateString private constructor(
//    public val `value`: String,
//) {
//    public constructor(input: String, regexPattern: String? = null) : this(
//        /* Validates the input before allowing instantiation. */
//        input.toValidatedDateString(regexPattern)?.value
//            ?: throw IllegalArgumentException("\"$input\" does not match " +
//                    "regexPattern: ${regexPattern ?: regexPatternForDateString}")
//    )
//
//    public companion object {
//        public const val regexPatternForDateString: String = "^\\d{4}-\\d{2}-\\d{2}${'$'}"
//
//        public fun String.toValidatedDateString(regexPattern: String? = regexPatternForDateString):
//                ValidatedDateString? {
//            /* Returns null for failure to validate string instead of throwing exception. */
//            return if (
//                (regexPattern ?: regexPatternForDateString).toRegex().matches(this)
//            )
//                ValidatedDateString(this)
//            else
//                null
//        }
//    }
//}