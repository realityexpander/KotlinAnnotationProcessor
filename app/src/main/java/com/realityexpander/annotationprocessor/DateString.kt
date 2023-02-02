package com.realityexpander.annotationprocessor

import com.realityexpander.aprocessor.Validate

@Validate("""^\d{4}-\d{2}-\d{2}$""", false)
data class DateString(
    val value: String
)

// Generates:  (returns a validated DateString, not a ValidatedDateString!)
fun String.toValidatedDateString1(regexPattern: String = """^\d{4}-\d{2}-\d{2}$"""): DateString? =
    if (regexPattern.toRegex().matches(this)) DateString(this) else null


// A generated class that performs validation before accepting a value
data class ValidatedDateString1 private constructor(
    private val value: String
) {
    constructor(value: String, regexPattern: String = """^\d{4}-\d{2}-\d{2}$""") : this(
        if (regexPattern.toRegex().matches(value))
            value
        else
            // null // if throwExceptions == false
            throw IllegalArgumentException("value: $value does not match regexPattern: $regexPattern")
    )
}


// V2
@JvmInline
value class ValidatedDateString3 private constructor(val value: String) {  // <---

    // Enforces validation upon instantiation
    constructor(value: String, regexPattern: String? = null) : this(
        if ((regexPattern ?: regexPatternForDateString).toRegex().matches(value))
            value
        else
            // null // if throwExceptions == false
            throw IllegalArgumentException("value: $value does not match regexPattern: $regexPatternForDateString")
    )

    companion object {
        const val regexPatternForDateString = """^\d{4}-\d{2}-\d{2}$"""

        fun String.toValidatedDateString(regexPattern: String?): ValidatedDateString3? {
            return if ((regexPattern ?: regexPatternForDateString).toRegex().matches(this))
                ValidatedDateString3(this)
            else
                null
        }
    }
}