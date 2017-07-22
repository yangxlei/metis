package io.github.yangxlei.metis.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.process.ExecResult

import java.util.jar.JarFile
/**
 * Created by yanglei on 2017/7/22.
 */

public class MetisTransform extends Transform {

    private Project mProject
    private MetisAction mAction;

    public MetisTransform(Project project) {
        mProject = project
        mAction = new MetisAction(project);
    }

    @Override
    public String getName() {
        return "metis";
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {

        mProject.logger.info("==================================> Metis start working <=======================================")
        mAction.tempDir = transformInvocation.context.temporaryDir

        def destDir
        List<String> classpaths = new ArrayList<>()
        transformInvocation.inputs.each { input ->

            input.jarInputs.each { jarInput ->

                def jarName = jarInput.name
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }

                def dest = transformInvocation.outputProvider.getContentLocation(jarName, jarInput.contentTypes, jarInput.scopes, Format.JAR)

                classpaths.add(dest)
                mAction.loadJar(new JarFile(jarInput.file), jarInput.status)
                FileUtils.copyFile(jarInput.file, dest)

                mProject.logger.info("scan file:\t ${jarInput.file} status:${jarInput.status}")
            }

            input.directoryInputs.each { dirInput ->

                // 测试发现: 如果目录下的文件没有任何改变，不会进入到这个 transform
                Map<File, Status> changedFiles = dirInput.changedFiles
                if (changedFiles == null || changedFiles.isEmpty()) {
                    // clean 后进入， changed 为空
                    mAction.loadDirectory(dirInput.file)
                    mProject.logger.info("scan dir:\t ${dirInput.file}")
                } else {
                    mAction.loadChangedFiles(changedFiles)
                }

                destDir = transformInvocation.outputProvider.getContentLocation(dirInput.name, dirInput.contentTypes, dirInput.scopes, Format.DIRECTORY)
                classpaths.add(destDir)
                FileUtils.copyDirectory(dirInput.file, destDir)
            }
        }

        def sourceFile = mAction.generateSourceFile()
        mProject.logger.info("generateSourceFile:\t ${sourceFile}")

        compileSourceFile(transformInvocation, sourceFile, destDir, classpaths)
        mProject.logger.info("compileSourceFile targetDir:\t ${destDir}")

        mProject.logger.info("==================================> Metis work finish <=======================================")
    }

    def compileSourceFile(transformInvocation, sourceFile, destDir, classpaths) {
        def sourceCompatibility
        def targetCompatibility
        def bootClasspath
        mProject.tasks.each { task ->
            if (AbstractCompile.isAssignableFrom(task.class)) {
                sourceCompatibility = task.sourceCompatibility
                targetCompatibility = task.targetCompatibility
            }

            if (JavaCompile.isAssignableFrom(task.class)) {
                bootClasspath = task.options.bootClasspath
            }
        }

        def javac = getJavac()

        def shell = generateCompileShell(
                transformInvocation.context.temporaryDir,
                javac,
                sourceCompatibility,
                targetCompatibility,
                sourceFile,
                destDir,
                bootClasspath,
                classpaths)



        ExecResult result = mProject.exec {
            executable 'sh'
            args shell.absolutePath
        }
    }

    def generateCompileShell(tempDir, javac, sourceCompatibility, targetCompatibility, sourceFile, destDir, bootClasspath, classpaths) {
        def shellFile = new File(tempDir, "compileMetisShell.sh")
        if (shellFile.exists()) shellFile.delete()

        shellFile.append("#!/bin/sh")

        shellFile.append("\n")

        shellFile.append("${javac} -source ${sourceCompatibility} -target ${targetCompatibility} ${sourceFile} -d ${destDir}")

        shellFile.append(" -bootclasspath ${bootClasspath}")

        shellFile.append(" -classpath ")

        classpaths.each { classpath ->
            shellFile.append("${classpath}:")
        }

        return shellFile
    }

    def getJavac() {
        def stdOut = new ByteArrayOutputStream()
        mProject.exec {
            commandLine 'which'
            args 'javac'
            standardOutput = stdOut
        }

        return stdOut.toString().trim()
    }
}
