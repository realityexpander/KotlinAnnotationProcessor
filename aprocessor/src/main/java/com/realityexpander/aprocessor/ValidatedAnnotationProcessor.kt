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

// Use-case: Create single-property value class that restricts instantiation to a preset regex.
// For any Class, generates a "Validated Value" Class that only allows
// input strings pass a preset Regex Match pattern.

@AutoService(Processor::class)
class ValidatedAnnotationProcessor : AbstractProcessor() {
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
                processAnnotation(it, regexParam)
            }

        return false
    }

    private fun processAnnotation(
        element: Element,
        regexParam: String,
    ) {
        val className = element.simpleName.toString()
        val packageName = processingEnv.elementUtils.getPackageOf(element).toString()

        val fileName = "Validated${className}"
        val fileBuilder= FileSpec.builder(packageName, fileName)

        val companionObject = TypeSpec.companionObjectBuilder()
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
                        |      (regexPattern ?: regexPatternFor$className).toRegex().matches(this)
                        |   )
                        |      $fileName(this)
                        |   else
                        |       null   
                        """.trimMargin()
                    )
                    .returns(ClassName(packageName, fileName).copy(nullable = true))
                    .build()
            )
            .build()

        val publicConstructor = FunSpec.constructorBuilder()
            .addParameter("input", String::class)
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
                |/* Validates the input before allowing instantiation. */
                |input.to$fileName(regexPattern)?.value
                |    ?: throw IllegalArgumentException("\"${'$'}input\" does not match " + 
                |       "regexPattern: ${'$'}{regexPattern ?: regexPatternFor$className}")
                |
                """.trimMargin()
            )
            .build()

        val inlineValueClass = TypeSpec.valueClassBuilder(fileName)
            .addAnnotation(JvmInline::class)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("value", String::class)
                    .addModifiers(KModifier.PRIVATE)
                    .build(),
            )
            .addProperty(
                PropertySpec.builder("value", String::class)
                    .initializer("value")
//                    .addModifiers(KModifier.PRIVATE)
                    .build(),
            )
            .addFunction(publicConstructor)
            .addType(companionObject)

        val file = fileBuilder
            .addType(inlineValueClass.build())
            .build()

        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
        file.writeTo(File(kaptKotlinGeneratedDir!!))
    }
}