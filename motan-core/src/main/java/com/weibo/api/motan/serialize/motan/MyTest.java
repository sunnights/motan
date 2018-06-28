/*
 *
 *   Copyright 2009-2016 Weibo, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.weibo.api.motan.serialize.motan;

import com.alibaba.fastjson.JSON;
import javassist.*;
import javassist.bytecode.SignatureAttribute;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhanglei28 on 2018/6/8.
 */
public class MyTest {
    public static void main(String[] args) {
        register(MyTestObject.class);
        register(MyTestObject.MyTestSubObject.class);

        Map<String, String> tmap = new HashMap<>(2);
        tmap.put("xxx", "YYY");
        tmap.put("zzz", "www");

        MyTestObject tmp = new MyTestObject();
        tmp.setF1(100);

        MyTestObject.MyTestSubObject testSubObject = new MyTestObject.MyTestSubObject();
        testSubObject.setF1("tso");
        testSubObject.setF2(Arrays.asList("xxx", "yyy"));

        MyTestObject testObject = new MyTestObject();
        testObject.setF1(123);
        testObject.setF2(123.3f);
        testObject.setF3("xxx");
        testObject.setF4(tmap);
        testObject.setF5(testSubObject);
        testObject.setF6(Arrays.asList("zzz", tmp));
        System.out.println(JSON.toJSONString(testObject));

        Message message = ((MessageTemplate<MyTestObject>) MotanSerialization.get(MyTestObject.class.getName())).toMessage(testObject);
        System.out.println(JSON.toJSONString(message));

        MyTestObject newObject = (MyTestObject) MotanSerialization.get(MyTestObject.class.getName()).fromMessage(message);
        System.out.println(JSON.toJSONString(newObject));
    }

    public static boolean isJavaClass(Class<?> clz) {
        return clz != null && clz.getClassLoader() == null;
    }

    public static CtMethod toMessage(CtClass ctClass, Class clazz) throws CannotCompileException {
        StringBuilder sb = new StringBuilder(512);
        sb.append("public Message toMessage(Object o) {\n");
        sb.append("Message message = new Message();\n");
        sb.append(clazz.getName() + " object = (" + clazz.getName() + ") o;\n");
        int index = 0;
        for (Field field : clazz.getDeclaredFields()) {
            sb.append("message.putField(" + (++index) + ", ");
            if (isJavaClass(field.getType())) {
                sb.append("($w)object." + field.getName() + ");\n");
            } else {
                Class<?> fieldClass = field.getType();
                sb.append("MotanSerialization.get(" + fieldClass.getName() + ".class.getName())" + ".toMessage(($w)object." + field.getName() + "));\n");
            }
        }
        sb.append("return message;\n");
        sb.append("}");

        CtMethod ctMethod = CtNewMethod.make(sb.toString(), ctClass);
        SignatureAttribute.TypeVariable clazzTVar = new SignatureAttribute.TypeVariable(clazz.getSimpleName());
        SignatureAttribute.TypeVariable messageTVar = new SignatureAttribute.TypeVariable(Message.class.getSimpleName());
        SignatureAttribute.MethodSignature ms1 = new SignatureAttribute.MethodSignature(null, new SignatureAttribute.Type[]{clazzTVar}, messageTVar, null);
        ctMethod.setGenericSignature(ms1.encode());
        return ctMethod;
    }

    public static CtMethod fromMessage(CtClass ctClass, Class clazz) throws CannotCompileException {
        StringBuilder sb = new StringBuilder(512);
        sb.append("public Object fromMessage(Message message) {\n");
        sb.append("Object o = new " + clazz.getName() + "();\n");
        sb.append(clazz.getName() + " object = (" + clazz.getName() + ") o;\n");
        int index = 0;
        for (Field field : clazz.getDeclaredFields()) {
            String prefix = "object." + field.getName() + " = ";
            String value = "message.getField(" + (++index) + ")";
            if (field.getType() == byte.class) {
                sb.append(prefix + "((Byte) " + value + ").byteValue();\n");
            } else if (field.getType() == short.class) {
                sb.append(prefix + "((Short) " + value + ").shortValue();\n");
            } else if (field.getType() == int.class) {
                sb.append(prefix + "((Integer) " + value + ").intValue();\n");
            } else if (field.getType() == long.class) {
                sb.append(prefix + "((Long) " + value + ").longValue();\n");
            } else if (field.getType() == float.class) {
                sb.append(prefix + "((Float) " + value + ").floatValue();\n");
            } else if (field.getType() == double.class) {
                sb.append(prefix + "((Double) " + value + ").doubleValue();\n");
            } else if (field.getType() == char.class) {
                sb.append(prefix + "((Char) " + value + ").charValue();\n");
            } else if (field.getType() == boolean.class) {
                sb.append(prefix + "((Boolean) " + value + ").booleanValue();\n");
            } else if (isJavaClass(field.getType())) {
                sb.append(prefix + "(" + field.getType().getName() + ") " + value + ";\n");
            } else {
                Class<?> fieldClass = field.getType();
                sb.append(prefix + "(" + fieldClass.getName() + ")MotanSerialization.get(" + fieldClass.getName() + ".class.getName())" + ".fromMessage((Message) " + value + ");\n");
            }
        }
        sb.append("return object;\n");
        sb.append("}");

        CtMethod ctMethod = CtNewMethod.make(sb.toString(), ctClass);
        SignatureAttribute.TypeVariable clazzTVar = new SignatureAttribute.TypeVariable(clazz.getSimpleName());
        SignatureAttribute.TypeVariable messageTVar = new SignatureAttribute.TypeVariable(Message.class.getSimpleName());
        SignatureAttribute.MethodSignature ms2 = new SignatureAttribute.MethodSignature(null, new SignatureAttribute.Type[]{messageTVar}, clazzTVar, null);
        ctMethod.setGenericSignature(ms2.encode());
        return ctMethod;
    }

    public static void register(Class clazz) {
        try {
            ClassPool classPool = ClassPool.getDefault();
            classPool.importPackage("com.weibo.api.motan.serialize.motan");
            CtClass ctClass = classPool.makeClass(clazz.getName() + "MessageTemplate");
            ctClass.addInterface(classPool.get(MessageTemplate.class.getName()));
            SignatureAttribute.ClassSignature classSignature = new SignatureAttribute.ClassSignature(new SignatureAttribute.TypeParameter[]{new SignatureAttribute.TypeParameter(clazz.getSimpleName())});
            ctClass.setGenericSignature(classSignature.encode());
            ctClass.addMethod(toMessage(ctClass, clazz));
            ctClass.addMethod(fromMessage(ctClass, clazz));
            MessageTemplate messageTemplate = (MessageTemplate) ctClass.toClass().newInstance();
            MotanSerialization.register(clazz.getName(), messageTemplate);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
