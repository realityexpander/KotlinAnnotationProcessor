package com.realityexpander.annotationprocessor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

//import android.support.v7.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val eModel = EncapsulatedModel()
        println("eModel = ${eModel.toString()}")
    }
}
