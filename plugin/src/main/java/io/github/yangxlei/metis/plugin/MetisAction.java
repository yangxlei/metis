package io.github.yangxlei.metis.plugin;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import org.gradle.api.file.FileCollection;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.lang.model.element.Modifier;

import io.github.yangxlei.metis.annotations.Metis;
import io.github.yangxlei.metis.loader.MetisLoader;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.Loader;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;
import javassist.bytecode.annotation.MemberValue;

/**
 * Created by yanglei on 2017/6/30.
 */

public class MetisAction {

    private FileCollection mClasspath;
    private File mSourceDir;

    private ClassPool mClassPool;

    public MetisAction(FileCollection classpath, File sourceDir) {
        this.mClasspath = classpath;
        this.mSourceDir = sourceDir;

        mClassPool = new ClassPool(true) {
            @Override
            public ClassLoader getClassLoader() {
                return new Loader();
            }
        };
    }

    public boolean execute() throws NotFoundException, IOException {
        List<CtClass> classes = loadClasses();
        if (classes == null || classes.isEmpty()) {
            System.out.println("No Class found");
            return false;
        }

        for (CtClass ctClass : classes) {
            processClass(ctClass);
        }

        generateSourceCode();
        return true;
    }

    private List<CtClass> loadClasses() throws NotFoundException, IOException {
        for (File file : mClasspath) {
            mClassPool.appendClassPath(file.getAbsolutePath());
        }

        List<CtClass> classes = new ArrayList<>();

        for (File file : mClasspath) {
            loadClasses(mClassPool, classes, file);
        }

        for (CtClass ctClass : classes) {
            processClass(ctClass);
        }

        return classes;
    }

    private void loadClasses(ClassPool pool, List<CtClass> classes, File file) throws IOException {
        Stack<File> stack = new Stack<>();
        stack.push(file);

        while (!stack.isEmpty()) {
            File f = stack.pop();

            if (f.isDirectory()) {
                File[] files = f.listFiles();
                for (File child : files) {
                    stack.push(child);
                }
            } else if (f.getName().endsWith(".class")) {
                FileInputStream inputStream = null;
                try {
                    inputStream = new FileInputStream(f);
                    classes.add(pool.makeClass(inputStream));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    if (null != inputStream) {
                        inputStream.close();
                    }
                }
            } else if (f.getName().endsWith(".jar")) {
                loadJar(pool, classes, new JarFile(f));
            }
        }
    }

    private void loadJar(ClassPool pool, List<CtClass> classes, JarFile jarFile) throws IOException {
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().endsWith(".class")) {
                InputStream stream = jarFile.getInputStream(entry);
                if (stream != null) {
                    classes.add(pool.makeClass(stream));
                    stream.close();
                }
            }
        }
    }

    private Map<String, List<CtClass>> mProviders = new HashMap<>();

    private void processClass(CtClass ctClass) {
        if (!ctClass.hasAnnotation(Metis.class)) {
            return;
        }

        ClassFile classFile = ctClass.getClassFile();
        AnnotationsAttribute attr = (AnnotationsAttribute) classFile.getAttribute(AnnotationsAttribute.invisibleTag);
        if (attr == null) return;

        Annotation annotation = attr.getAnnotation(Metis.class.getName());
        if (annotation == null) return;

        ArrayMemberValue value = (ArrayMemberValue) annotation.getMemberValue("value");

        for (MemberValue member : value.getValue()) {
            ClassMemberValue classMember = (ClassMemberValue) member;

            List<CtClass> sets = mProviders.get(classMember.getValue());
            if (sets == null) {
                sets = new ArrayList<>();
                mProviders.put(classMember.getValue(), sets);
            }
            sets.add(ctClass);
        }
    }


    private void generateSourceCode() throws IOException {
        ClassName javaUtilMap = ClassName.get("java.util", "Map");
        ClassName javaUtilSet = ClassName.get("java.util", "Set");
        ClassName javaUtilCollections = ClassName.get("java.util", "Collections");

        TypeName javaLangClass = ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class));
        TypeName hashSetOfClass = ParameterizedTypeName.get(ClassName.get("java.util", "HashSet"), javaLangClass);
        TypeName linkedHashMap = ParameterizedTypeName.get(ClassName.get("java.util", "LinkedHashMap"), javaLangClass, hashSetOfClass);
        TypeName setOfClass = ParameterizedTypeName.get(javaUtilSet, javaLangClass);
        TypeName mapOfClassAndSetOfClass = ParameterizedTypeName.get(javaUtilMap, javaLangClass, hashSetOfClass);

        FieldSpec sServicesSpec = FieldSpec.builder(mapOfClassAndSetOfClass, "sServices", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T()", linkedHashMap)
                .build();

        MethodSpec getMethodSpec = MethodSpec.methodBuilder("get")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                .returns(setOfClass)
                .addParameter(javaLangClass, "key")
                .addCode(CodeBlock.builder()
                        .addStatement("$T result = sServices.get(key)", setOfClass)
                        .addStatement("return null == result ? $T.<Class<?>>emptySet() : $T.unmodifiableSet(result)", javaUtilCollections, javaUtilCollections)
                        .build())
                .build();

        MethodSpec registerMethodSpec = MethodSpec.methodBuilder("register")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC)
                .returns(TypeName.VOID)
                .addParameter(Class.class, "key")
                .addParameter(javaLangClass, "value")
                .addCode(CodeBlock.builder()
                        .addStatement("$T result = sServices.get(key)", hashSetOfClass)
                        .beginControlFlow("if (result == null)")
                        .addStatement("result = new $T()", hashSetOfClass)
                        .addStatement("sServices.put(key, result)")
                        .endControlFlow()
                        .addStatement("result.add(value)")
                        .build())
                .build();

        TypeSpec metisRegistrySpec = TypeSpec.classBuilder("MetisRegistry")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addField(sServicesSpec)
                .addMethod(getMethodSpec)
                .addMethod(registerMethodSpec)
                .addStaticBlock(generateStaticBlock())
                .build();

        JavaFile javaFile = JavaFile.builder(MetisLoader.class.getPackage().getName(), metisRegistrySpec)
                .build();

        javaFile.writeTo(mSourceDir);
    }

    private CodeBlock generateStaticBlock() {
        CodeBlock.Builder builder = CodeBlock.builder();
        Iterator<String> iterator = mProviders.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            List<CtClass> elements = mProviders.get(key);
            for (CtClass element : elements) {
                builder.addStatement("register($L.class, $L.class)", key, element.getName());
            }
        }
        return builder.build();

    }
}
