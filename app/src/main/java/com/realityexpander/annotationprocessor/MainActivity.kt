package com.realityexpander.annotationprocessor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.realityexpander.annotationprocessor.ValidatedDateString.Companion.toValidatedDateString

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val eModel = EncapsulatedModel()
        println("eModel = ${eModel.toString()}")

        val dateString = DateString("2019-12-31")
        println("dateString = ${"2019-12-31".toValidatedDateString()}")

        try {
            val validatedDateString = ValidatedDateString("2019-12-31")
            println("dateStringValidated = $validatedDateString")
        } catch (e: Exception) {
            println("dateStringValidated = ${e.message}")
        }
    }
}
