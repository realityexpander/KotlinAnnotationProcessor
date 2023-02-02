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

        val fileName = "Validate${className}Ext"
        val fileName2 = "Validate${className}"
        val fileBuilder= FileSpec.builder(packageName, fileName)

        val dollarBracket = "\${"

//        val classBuilder = TypeSpec.classBuilder(fileName)
//        for (enclosed in element.enclosedElements) {
//            if (enclosed.kind == ElementKind.FIELD) {
//                classBuilder.addProperty(
//                    PropertySpec
//                        .varBuilder(
//                            enclosed.simpleName.toString(),
//                            enclosed.asType().asTypeName().asNullable(),
//                            KModifier.PRIVATE)
//                        .initializer("null")
//                        .build()
//                )
//                classBuilder.addFunction(
//                    FunSpec.builder("get${enclosed.simpleName}")
//                        .returns(
//                            enclosed
//                                .asType().asTypeName().asNullable())
//                        .addStatement("return ${enclosed.simpleName}")
//                        .build()
//                )
//                classBuilder.addFunction(
//                    FunSpec.builder("set${enclosed.simpleName}")
//                        .addParameter(
//                            ParameterSpec.builder(
//                                "${enclosed.simpleName}",
//                                enclosed.asType().asTypeName().asNullable()
//                            ).build())
//                        .addStatement("this.${enclosed.simpleName} = ${enclosed.simpleName}")
//                        .addCode(CodeBlock.builder().addStatement(
//                            """
//                                println("${enclosed.simpleName}: $dollarBracket${enclosed.simpleName}}")
//                            """
//                                .trimIndent()).build())
//                        .build()
//                )
//            }
//        }

        val extensionBuilder = FunSpec.builder("toValidated${className}")
//            .receiver(ClassName(packageName, className))
            .receiver(String::class.asTypeName())
            .addParameter(
                ParameterSpec.builder(
                    "regexPattern",
                    String::class.asTypeName()
                ).defaultValue("\"\"\"${regexParam}\"\"\"")
                .build())
            .returns(ClassName(packageName, className).asNullable())
            .addCode(CodeBlock.builder().addStatement(
                """
                return if (regexPattern.toRegex().matches(this))
                       ${className}(this)
                    else
                       ${if (useExceptions)
                           "throw IllegalArgumentException(\"value: \$this does not match regexPattern: \$regexPattern\")"
                        else
                            "null"
                       }
                """
            .trimIndent()).build())
//            .addStatement("return $className(0, \"${regexParam}\")")

//        val inlineClass = TypeSpec.classBuilder(fileName2)
//            .addModifiers(KModifier.valueOf("value"))
//            .primaryConstructor(
//                FunSpec.builder("constructor")
//                    .addParameter(
//                        ParameterSpec.builder(
//                            "value",
//                            String::class.asTypeName()
//                        ).build()
//                    ).addModifiers(KModifier.PRIVATE)
//                    .build()
//                )
////            .addCode(CodeBlock.builder().addStatement(
////                """
////                    println("hello")
////
////                """.trimIndent()
////            ))


        val file = fileBuilder
//            .addType(classBuilder.build())
            .addFunction(extensionBuilder.build())
//            .addType(inlineClass.build())
            .build()

        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
        file.writeTo(File(kaptKotlinGeneratedDir!!))
    }
}