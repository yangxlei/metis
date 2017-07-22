package io.github.yangxlei.metis.plugin

import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

public class MetisPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.dependencies {
            compile 'io.github.yangxlei:metis-loader:1.0.0'
        }

        if (!project.plugins.hasPlugin(AppPlugin)) {
            return
        }

        project.android.registerTransform(new MetisTransform(project))
    }
}