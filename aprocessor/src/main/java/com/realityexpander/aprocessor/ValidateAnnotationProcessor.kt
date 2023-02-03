package com.realityexpander.aprocessor

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

@AutoService(Processor::class)
class ValidateAnnotationProcessor : AbstractProcessor() {
    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(Validate::class.java.name)
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        processingEnv.messager.printMessage(Diagnostic.Kind.NOTE,
            "Processing annotations: $annotations")

        roundEnv.getElementsAnnotatedWith(Validate::class.java)
            .forEach {
                if (it.kind != ElementKind.CLASS) {
                    processingEnv.messager
                        .printMessage(Diagnostic.Kind.ERROR, "Only classes can be annotated")

                    return true
                }

                val regexParam = it.getAnnotation(Validate::class.java).matchRegex
                val useExceptions = it.getAnnotation(Validate::class.java).throwExceptions
                processAnnotation(it, regexParam, useExceptions)
            }

        return false
    }


    private fun processAnnotation(
        element: Element,
        regexParam: String,
        useExceptions: Boolean = false
    ) {
        val className = element.simpleName.toString()
        val packageName = processingEnv.elementUtils.getPackageOf(element).toString()

        val fileName = "Validated${className}"
        val modifiedClassName = fileName
        val fileBuilder= FileSpec.builder(packageName, fileName)

        val companion = TypeSpec.companionObjectBuilder()
            .addProperty(
                PropertySpec.builder("regexPatternFor$className", String::class)
                    .initializer("%S", regexParam)
                    .addModifiers(KModifier.CONST)
                    .build()
            )
            .addFunction(
                FunSpec.builder("toValidated$className")
                    .receiver(String::class)
                    .addParameter(ParameterSpec.builder(
                            "regexPattern",
                            String::class.asTypeName().copy(nullable = true),
                        ).defaultValue("regexPatternFor$className")
                        .build())
                    .addCode(
                        """ 
                        |   /* Returns null for failure to validate string instead of throwing exception. */
                        |   return if (
                        |      (regexPattern ?: regexPatternForDateString).toRegex().matches(this)
                        |   )
                        |      ${modifiedClassName}(this)
                        |   else
                        |       null   
                        """.trimMargin()
                    )
                    .returns(ClassName(packageName, modifiedClassName).copy(nullable = true))
                    .build()
            )
            .build()

        val publicConstructor = FunSpec.constructorBuilder()
            .addParameter("valueString", String::class)
            .addParameter(
                ParameterSpec.builder(
                    "regexPattern",
                    String::class.asTypeName().copy(nullable = true),
                )
                .defaultValue("null")
                .build()
            )
            .callThisConstructor(
                """
                |
                |/* Validates the valueString before allowing instantiation. */
                |valueString.to$modifiedClassName(regexPattern)?._data
                |    ?: throw IllegalArgumentException("value: ${'$'}valueString does not match " + 
                |    "regexPattern: ${'$'}regexPattern")
                |
                """.trimMargin()
            )
            .build()

        val inlineValueClass = TypeSpec.valueClassBuilder(fileName)
            .addAnnotation(JvmInline::class)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("_data", String::class)
                    .addModifiers(KModifier.PRIVATE)
                    .build(),
            )
            .addProperty(
                PropertySpec.builder("_data", String::class)
                    .initializer("_data")
                    .addModifiers(KModifier.PRIVATE)
                    .build(),
            )
            .addFunction(publicConstructor)
            .addType(companion)




        val file = fileBuilder
//            .addType(classBuilder.build())
//            .addFunction(extensionBuilder.build())
            .addType(inlineValueClass.build())
            .build()

        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
        file.writeTo(File(kaptKotlinGeneratedDir!!))
    }


}