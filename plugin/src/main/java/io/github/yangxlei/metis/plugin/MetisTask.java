package io.github.yangxlei.metis.plugin;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;

import javassist.NotFoundException;

public class MetisTask extends DefaultTask {

    private FileCollection mClassPath;
    private File mSourceDir;

    public MetisTask() {
        this.mClassPath = getProject().files();
    }

    @InputFiles
    public void setClassPath(FileCollection classPath) {
        mClassPath = classPath;
    }

    public FileCollection getClassPath() {
        return mClassPath;
    }

    public void setSourceDir(File sourceDir) {
        mSourceDir = sourceDir;
    }

    @OutputDirectory
    public File getSourceDir() {
        return mSourceDir;
    }

    @TaskAction
    public void doTask() throws NotFoundException, IOException {
        setDidWork(new MetisAction(mClassPath, mSourceDir).execute());
    }
}
