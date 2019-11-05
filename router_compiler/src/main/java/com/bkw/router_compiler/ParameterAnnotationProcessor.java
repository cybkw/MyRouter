package com.bkw.router_compiler;

import com.bkw.router_annotation.core.Parameter;
import com.bkw.router_compiler.factory.ParameterFactory;
import com.bkw.router_compiler.utils.Cons;
import com.bkw.router_compiler.utils.EmptyUtils;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes(Cons.PARAMETER_ANNOTATIONS_TYPES)
public class ParameterAnnotationProcessor extends AbstractProcessor {

    private Elements elementUtils;
    private Filer filer;
    private Messager messager;
    private Types typeUtils;

    // 临时map存储，用来存放被@Parameter注解的属性集合，生成类文件时遍历
    // key:类节点, value:被@Parameter注解的属性集合
    private Map<TypeElement, List<Element>> tempParameterMap = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
        typeUtils = processingEnv.getTypeUtils();


    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        //是否有类节点使用了@Parameter注解
        if (!EmptyUtils.isEmpty(annotations)) {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Parameter.class);

            if (!EmptyUtils.isEmpty(elements)) {
                //解析元素并赋值临时Map存储
                parserElement(elements);
                messager.printMessage(Diagnostic.Kind.NOTE, "@Parameter parserElement");
                //生成类文件
                createParameterFile();
                return true;
            }
            return true;
        }
        return false;
    }

    private void createParameterFile() {
        if (EmptyUtils.isEmpty(tempParameterMap)) {
            return;
        }
        messager.printMessage(Diagnostic.Kind.NOTE, "@Parameter parserElement");

        //通过Element工具类，获取Parameter类型
        TypeElement activityType = elementUtils.getTypeElement(Cons.ACTIVITY);
        TypeElement parameterType = elementUtils.getTypeElement(Cons.PARAMETER_INTERFACE);

        //参数体配置
        ParameterSpec parameterSpec = ParameterSpec.builder(TypeName.OBJECT,
                Cons.PARAMETER_NAME).build();

        for (Map.Entry<TypeElement, List<Element>> entry : tempParameterMap.entrySet()) {
            // Map集合中的key是类名，如：MainActivity
            TypeElement typeElement = entry.getKey();
            // 如果类名的类型和Activity类型不匹配
            if (!typeUtils.isSubtype(typeElement.asType(), activityType.asType())) {
                throw new RuntimeException("@Parameter注解目前仅限用于Activity类之上");
            }

            // 获取类名
            ClassName className = ClassName.get(typeElement);

            //方法体内容构建
            ParameterFactory factory = new ParameterFactory.Builder(parameterSpec)
                    .setMessager(messager)
                    .setElementUtils(elementUtils)
                    .setTypeUtils(typeUtils)
                    .setClassName(className)
                    .build();

            //添加方法体内容第一行
            factory.addFirstStatement();

            //遍历类里面所有属性
            for (Element element : entry.getValue()) {
                factory.buildStatement(element);
            }

            //最终生成的类文件名
            String finalClassName = typeElement.getSimpleName() + Cons.PARAMETER_FILE_NAME;
            messager.printMessage(Diagnostic.Kind.NOTE, "APT生成参数类文件：" + className.packageName() + "." + finalClassName);

            try {
                JavaFile.builder(className.packageName(),
                        TypeSpec.classBuilder(finalClassName)
                                .addSuperinterface(ClassName.get(parameterType))
                                .addModifiers(Modifier.PUBLIC)
                                .addMethod(factory.build())
                                .build())
                        .build()
                        .writeTo(filer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 赋值临时map存储，用来存放被@Parameter注解的属性集合，生成类文件时遍历
     *
     * @param elements 被 @Parameter 注解的 元素集合
     */
    private void parserElement(Set<? extends Element> elements) {
        for (Element element : elements) {
            //注解在属性之上，属性节点的父节点是类节点
            TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
            //Map集合中key:类节点存在，直接添加属性。
            if (tempParameterMap.containsKey(enclosingElement)) {
                tempParameterMap.get(enclosingElement).add(element);
            } else {
                List<Element> fields = new ArrayList<>();
                fields.add(element);
                tempParameterMap.put(enclosingElement, fields);
            }
        }
    }
}
