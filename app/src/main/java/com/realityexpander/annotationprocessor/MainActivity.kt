package com.realityexpander.annotationprocessor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.realityexpander.annotationprocessor.ValidatedDateString.Companion.toValidatedDateString

// Notes
// https://cafonsomota.medium.com/debug-annotation-processor-in-kotlin-6eb462e965f8

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val eModel = EncapsulatedModel()
        println("eModel = ${eModel.toString()}")

        val dateString = DateString("2019-12-31")
        println("dateString = ${"2019-12-31".toValidatedDateString()}")

        val x = "2012-09-22".toValidatedDateString()?.value

        // Example: Override built-in regex
        val y = "20".toValidatedDateString("\\d{2}")?.value

        try {
            val validatedDateString = ValidatedDateString("2019-12-310")
            println("dateStringValidated = $validatedDateString")
        } catch (e: Exception) {
            println("EXCEPTION dateStringValidated = ${e.message}")
        }
    }
}
