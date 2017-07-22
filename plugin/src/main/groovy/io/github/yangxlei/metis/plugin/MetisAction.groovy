package io.github.yangxlei.metis.plugin

import com.android.build.api.transform.Status
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.squareup.javapoet.*
import io.github.yangxlei.metis.annotations.Metis
import io.github.yangxlei.metis.loader.MetisLoader
import javassist.ClassPool
import javassist.CtClass
import javassist.Loader
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.ClassFile
import javassist.bytecode.annotation.Annotation
import javassist.bytecode.annotation.ArrayMemberValue
import javassist.bytecode.annotation.ClassMemberValue
import javassist.bytecode.annotation.MemberValue
import org.gradle.api.Project

import javax.lang.model.element.Modifier
import java.util.jar.JarEntry
import java.util.jar.JarFile
/**
 * Created by yanglei on 2017/7/22.
 */

public class MetisAction {

    private Project mProject;
    ClassPool mPool;
    private File mRecordFile;
    private List<MetisElement> mMetisProvider;

    public MetisAction(Project project) {
        mProject = project;
        mMetisProvider = new ArrayList<>()

        mPool = new ClassPool(true) {
            @Override
            ClassLoader getClassLoader() {
                return new Loader()
            }
        }
    }

    public void setTempDir(File dir) {
        mRecordFile = new File(dir, "MetisAnnotationClasses.txt")
        if (!mRecordFile.exists()) return
        List<MetisElement> elements = new Gson().fromJson(new FileReader(mRecordFile), new TypeToken<List<MetisElement>>() {
        }.getType());

        if (elements != null) {
            mMetisProvider.addAll(elements)
        }
    }

    protected void loadJar(JarFile jarFile, Status status) {
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().endsWith(".class")) {
                InputStream stream = jarFile.getInputStream(entry)
                if (stream != null) {
                    CtClass ctClass = mPool.makeClass(stream)
                    removeOldValue(ctClass.name)
                    anaylizeClass(ctClass)
                }
            }
        }
    }

    private void removeOldValue(String oldValue) {
        for (MetisElement element : mMetisProvider) {
            Iterator<String> iterator = element.values.iterator();
            while (iterator.hasNext()) {
                if (oldValue.equals(iterator.next())) {
                    iterator.remove()
                }
            }

        }
    }

    public void loadDirectory(File dir) {
        Stack<File> stack = new Stack<>();
        stack.push(dir)

        while (!stack.isEmpty()) {
            File f = stack.pop()
            if (f.isDirectory()) {
                f.listFiles().each { child ->
                    stack.push(child)
                }
            } else if (f.getName().endsWith(".class")) {
                FileInputStream inputStream = new FileInputStream(f)
                anaylizeClass(mPool.makeClass(inputStream))
            } else if (f.getName().endsWith(".jar")) {
                loadJar(new JarFile(f))
            }
        }
    }

    public void loadChangedFiles(Map<File, Status> changedFile) {
        changedFile.keySet().each { file ->
            mProject.logger.info("scan file:\t ${file}")
            FileInputStream inputStream = new FileInputStream(file)
            anaylizeClass(mPool.makeClass(inputStream))
        }
    }

    public def generateSourceFile() {
        mRecordFile.write(new Gson().toJson(mMetisProvider))

        ClassName javaUtilMap = ClassName.get("java.util", "Map");
        ClassName javaUtilSet = ClassName.get("java.util", "Set");
        ClassName javaUtilCollections = ClassName.get("java.util", "Collections");

        TypeName javaLangClass = ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class));
        TypeName hashSetOfClass = ParameterizedTypeName.get(ClassName.get("java.util", "HashSet"), javaLangClass);
        TypeName linkedHashMap = ParameterizedTypeName.get(ClassName.get("java.util", "LinkedHashMap"), javaLangClass, hashSetOfClass);
        TypeName setOfClass = ParameterizedTypeName.get(javaUtilSet, javaLangClass);
        TypeName mapOfClassAndSetOfClass = ParameterizedTypeName.get(javaUtilMap, javaLangClass, hashSetOfClass);

        FieldSpec sServicesSpec = FieldSpec.builder(mapOfClassAndSetOfClass, "sServices", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("new \$T()", linkedHashMap)
                .build();

        MethodSpec getMethodSpec = MethodSpec.methodBuilder("get")
                .addModifiers(Modifier.FINAL, Modifier.STATIC)
                .returns(setOfClass)
                .addParameter(javaLangClass, "key")
                .addCode(CodeBlock.builder()
                .addStatement("\$T result = sServices.get(key)", setOfClass)
                .addStatement("return null == result ? \$T.<Class<?>>emptySet() : \$T.unmodifiableSet(result)", javaUtilCollections, javaUtilCollections)
                .build())
                .build();

        MethodSpec registerMethodSpec = MethodSpec.methodBuilder("register")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC)
                .returns(TypeName.VOID)
                .addParameter(Class.class, "key")
                .addParameter(javaLangClass, "value")
                .addCode(CodeBlock.builder()
                .addStatement("\$T result = sServices.get(key)", hashSetOfClass)
                .beginControlFlow("if (result == null)")
                .addStatement("result = new \$T()", hashSetOfClass)
                .addStatement("sServices.put(key, result)")
                .endControlFlow()
                .addStatement("result.add(value)")
                .build())
                .build();

        TypeSpec metisRegistrySpec = TypeSpec.classBuilder("MetisRegistry")
                .addModifiers(Modifier.FINAL)
                .addField(sServicesSpec)
                .addMethod(getMethodSpec)
                .addMethod(registerMethodSpec)
                .addStaticBlock(generateStaticBlock())
                .build();

        String packageName = MetisLoader.class.package.name;
        JavaFile javaFile = JavaFile.builder(packageName, metisRegistrySpec)
                .build();

        javaFile.writeTo(mRecordFile.parentFile);

        String fileName = "${packageName.replaceAll("\\.", "/")}/MetisRegistry.java"

        return new File(mRecordFile.parentFile, fileName)
    }

    void anaylizeClass(CtClass ctClass) {
        if (!ctClass.hasAnnotation(Metis.class))
            return
        ClassFile classFile = ctClass.getClassFile();
        AnnotationsAttribute attr = (AnnotationsAttribute) classFile.getAttribute(AnnotationsAttribute.invisibleTag);
        if (attr == null) return;

        Annotation annotation = attr.getAnnotation(Metis.class.getName());
        if (annotation == null) return;

        ArrayMemberValue value = (ArrayMemberValue) annotation.getMemberValue("value");

        for (MemberValue member : value.getValue()) {
            ClassMemberValue classMember = (ClassMemberValue) member
            MetisElement element = getElement(classMember.getValue())
            if (element == null) {
                element = new MetisElement()
                element.key = classMember.getValue()
                element.values = new HashSet<>()
                mMetisProvider.add(element)
            }

            element.values.add(ctClass.getName())
        }
    }

    private MetisElement getElement(String key) {
        for (MetisElement element : mMetisProvider) {
            if (key.equals(element.key)) {
                return element;
            }
        }
        return null;
    }

    private CodeBlock generateStaticBlock() {
        CodeBlock.Builder builder = CodeBlock.builder();
        mMetisProvider.each { element ->
            element.values.each { value ->
                builder.addStatement("register(\$L.class, \$L.class)", element.key, value);
            }
        }
        return builder.build();

    }
}
