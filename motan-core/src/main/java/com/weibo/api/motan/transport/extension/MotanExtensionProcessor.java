/*
 * Copyright 2009-2016 Weibo, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.weibo.api.motan.transport.extension;

import com.squareup.javapoet.*;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.rpc.ResponseFuture;
import reactor.core.publisher.Mono;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author zhanglei
 * @Description create a {interfaceName}Async class with all method add a 'Async' suffix. TODO
 * support methods inherited from superinterfaces
 * @date Feb 24, 2017
 */
public class MotanExtensionProcessor extends AbstractProcessor {
    protected static String EXTENSION = MotanConstants.EXTENSION_SUFFIX;
    protected static String ASYNC = MotanConstants.ASYNC_SUFFIX;
    protected static String REACTOR = MotanConstants.REACTOR_SUFFIX;
    protected static String GENERATE_PATH_KEY = "motanGeneratePath";
    protected static String TARGET_DIR;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        String path = processingEnv.getOptions().get(GENERATE_PATH_KEY);// use javac complie options -AmotanGeneratePath=xxx
        if (path != null) {
            TARGET_DIR = path;
        } else { //use jvm option -DmotanGeneratePath=xxx
            TARGET_DIR = System.getProperty(GENERATE_PATH_KEY, "target/generated-sources/annotations/");
        }

    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        HashSet<String> types = new HashSet<>();
        types.add(MotanExtension.class.getName());
        return types;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return true;
        }
        for (Element elem : roundEnv.getElementsAnnotatedWith(MotanExtension.class)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "MotanExtensionProcessor will process " + elem.toString() + ", generate class path:" + TARGET_DIR);
            try {
                if (elem.getKind().isInterface()) {
                    writeExtensionClass(elem);
                } else {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                            "MotanExtensionProcessor not process, because " + elem.toString() + " not a interface.");
                }
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "MotanExtensionProcessor done for " + elem.toString());
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                        "MotanExtensionProcessor process " + elem.toString() + " fail. exception:" + e.getMessage());
            }
        }
        return true;
    }

    private void writeExtensionClass(Element elem) throws Exception {
        TypeElement interfaceClazz = (TypeElement) elem;
        String className = interfaceClazz.getSimpleName().toString();
        TypeSpec.Builder classBuilder = TypeSpec.interfaceBuilder(className + EXTENSION)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(TypeName.get(elem.asType()));

        // add class generic type
        classBuilder.addTypeVariables(getTypeNames(interfaceClazz.getTypeParameters()));

        // add direct method
        addMethods(interfaceClazz, classBuilder);

        // add method form superinterface
        addSuperInterfaceMethods(interfaceClazz.getInterfaces(), classBuilder);

        // write class
        String packageName = processingEnv.getElementUtils().getPackageOf(interfaceClazz).getQualifiedName().toString();
        JavaFile javaFile = JavaFile.builder(packageName, classBuilder.build()).build();

        javaFile.writeTo(new File(System.getProperty("basedir"), TARGET_DIR));
    }

    private void addMethods(TypeElement interfaceClazz, TypeSpec.Builder classBuilder) {
        List<? extends Element> elements = interfaceClazz.getEnclosedElements();
        for (Element e : elements) {
            if (ElementKind.METHOD.equals(e.getKind())) {
                ExecutableElement method = (ExecutableElement) e;
                classBuilder.addMethod(addAsyncMethod(method));
                classBuilder.addMethod(addReactorMethod(method));
            }
        }
    }

    private MethodSpec addAsyncMethod(ExecutableElement method) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(method.getSimpleName().toString() + ASYNC)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT).returns(ResponseFuture.class)
                .addTypeVariables(getTypeNames(method.getTypeParameters()));
        // add method params
        List<? extends VariableElement> vars = method.getParameters();
        for (VariableElement var : vars) {
            methodBuilder.addParameter(ParameterSpec.builder(TypeName.get(var.asType()), var.getSimpleName().toString())
                    .build());
        }
        return methodBuilder.build();
    }

    private MethodSpec addReactorMethod(ExecutableElement method) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(method.getSimpleName().toString() + REACTOR)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), TypeName.get(method.getReturnType()).box()))
                .addTypeVariables(getTypeNames(method.getTypeParameters()));
        // add method params
        List<? extends VariableElement> vars = method.getParameters();
        for (VariableElement var : vars) {
            methodBuilder.addParameter(ParameterSpec.builder(TypeName.get(var.asType()), var.getSimpleName().toString())
                    .build());
        }
        return methodBuilder.build();
    }

    private List<TypeVariableName> getTypeNames(List<? extends TypeParameterElement> types) {
        List<TypeVariableName> result = new ArrayList<>();
        if (types != null && !types.isEmpty()) {
            for (TypeParameterElement type : types) {
                result.add(TypeVariableName.get(type));
            }
        }
        return result;
    }

    private void addSuperInterfaceMethods(List<? extends TypeMirror> superInterfaces, TypeSpec.Builder classBuilder) {
        if (superInterfaces != null && !superInterfaces.isEmpty()) {
            for (TypeMirror tm : superInterfaces) {
                try {
                    if (tm.getKind().equals(TypeKind.DECLARED)) {
                        TypeElement de = (TypeElement) ((DeclaredType) tm).asElement();
                        addMethods(de, classBuilder);
                        addSuperInterfaceMethods(de.getInterfaces(), classBuilder);
                    }
                } catch (Exception e) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                            "MotanExtensionProcessor process superinterface " + tm.toString() + " fail. exception:" + e.getMessage());
                }
            }
        }
    }

}
