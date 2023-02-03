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
class EncapsulateAnnotationProcessor : AbstractProcessor() {
    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(Encapsulate::class.java.name)
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "Processing annotations: $annotations")

        // get parameters of the annotation
        val options = processingEnv.options
        processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "Processing options: ${options.entries}")


        roundEnv.getElementsAnnotatedWith(Encapsulate::class.java)
            .forEach {

                val annotationParam = it.getAnnotation(Encapsulate::class.java).value
                processingEnv.messager.printMessage(Diagnostic.Kind.NOTE,
                    "Annotation parameter: $annotationParam")

                processingEnv.messager.printMessage(Diagnostic.Kind.NOTE,
                    "Processing kind, simpleName: " +
                            "${it.kind} ${it.simpleName}")

                processingEnv.messager.printMessage(Diagnostic.Kind.NOTE,
                    "Processing modifiers: ${it.modifiers}")

                if (it.kind != ElementKind.CLASS) {
                    processingEnv.messager
                        .printMessage(Diagnostic.Kind.ERROR, "Only classes can be annotated")
                    return true
                }
                processAnnotation(it, annotationParam)
            }

        return false
    }

    @OptIn(DelicateKotlinPoetApi::class)
    private fun processAnnotation(
        element: Element,
        specialParam: String
    ) {
        val className = element.simpleName.toString()
        val packageName = processingEnv.elementUtils.getPackageOf(element).toString()

        val fileName = "Encapsulated$className"
        val fileBuilder= FileSpec.builder(packageName, fileName)
        val classBuilder = TypeSpec.classBuilder(fileName)

        val dollarBracket = "\${"

        processingEnv.messager.printMessage(Diagnostic.Kind.NOTE,
            "Processing element: $element")
        processingEnv.messager.printMessage(Diagnostic.Kind.NOTE,
            "Processing element.enclosedElements: ${element.enclosedElements}")

        for (enclosed in element.enclosedElements) {
            if (enclosed.kind == ElementKind.FIELD) {
                classBuilder.addProperty(
                    PropertySpec
//                        .varBuilder(
                        .builder(
                            enclosed.simpleName.toString(),
                            enclosed.asType().asTypeName().copy(nullable = true),
                            KModifier.PRIVATE)
                        .mutable()
                        .initializer("null")
                        .build()
                )
                classBuilder.addFunction(
                    FunSpec.builder("get${enclosed.simpleName}")
                        .returns(
                            enclosed
                            .asType().asTypeName().copy(nullable = true))
                        .addStatement("return ${enclosed.simpleName}")
                        .build()
                )
                classBuilder.addFunction(
                    FunSpec.builder("set${enclosed.simpleName}")
                        .addParameter(
                            ParameterSpec.builder(
                                "${enclosed.simpleName}",
                                enclosed.asType().asTypeName().copy(nullable = true)
                            ).build())
                        .addStatement("this.${enclosed.simpleName} = ${enclosed.simpleName}")
                        .addCode(CodeBlock.builder().addStatement(
                            """
                                println("${enclosed.simpleName}: $dollarBracket${enclosed.simpleName}}")
                            """
                            .trimIndent()).build())
                        .build()
                )
            }
        }

        val extensionBuilder = FunSpec.builder("to${className}Validated")
            .receiver(ClassName(packageName, className))
            .returns(ClassName(packageName, className))
            .addCode(CodeBlock.builder().addStatement(
                """
                    println("${specialParam}")
                """
                .trimIndent()).build())
            .addStatement("return $className(0, \"${specialParam}\")")

        val file = fileBuilder
            .addType(classBuilder.build())
            .addFunction(extensionBuilder.build())
            .build()

        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
        file.writeTo(File(kaptKotlinGeneratedDir!!))
    }
}