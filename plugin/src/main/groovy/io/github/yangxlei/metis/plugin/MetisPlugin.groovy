package io.github.yangxlei.metis.plugin

import com.android.build.gradle.AppPlugin;
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

public class MetisPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.dependencies {
            compile 'io.github.yangxlei:metis-loader:1.0.0'
        }

        project.afterEvaluate {
            if (!project.plugins.hasPlugin(AppPlugin)) {
                // library
                return;
            }

            project.android.applicationVariants.all { variant ->
                def metisSourceDir = project.file("${project.buildDir}/intermediates/metis/${variant.dirName}/src")
                def metisClasspath = project.files(project.android.bootClasspath,  variant.javaCompile.classpath, variant.javaCompile.destinationDir)

                def generateMetisTask = project.task("generateMetis${variant.name.capitalize()}", type: MetisTask) {
                    description = "Generate MetisRegistry for ${variant.name.capitalize()}"
                    sourceDir = metisSourceDir
                    classPath += metisClasspath
                    outputs.upToDateWhen { false }
                }

                def compileMetisTask = project.task("compileGenerateMetis${variant.name.capitalize()}", type: JavaCompile) {
                    description = "Compile MetisRegistry for ${variant.name.capitalize()}"
                    source = metisSourceDir
                    include '**/*.java'
                    classpath = metisClasspath
                    destinationDir = variant.javaCompile.destinationDir
                    sourceCompatibility = '1.5'
                    targetCompatibility = '1.5'
                }

                generateMetisTask.mustRunAfter(variant.javaCompile)
                compileMetisTask.mustRunAfter(generateMetisTask)
                variant.assemble.dependsOn(generateMetisTask, compileMetisTask)
            }
        }
    }
}