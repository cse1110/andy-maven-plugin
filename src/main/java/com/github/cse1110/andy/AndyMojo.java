package com.github.cse1110.andy;

import com.google.common.io.Files;
import nl.tudelft.cse1110.andy.Andy;
import nl.tudelft.cse1110.andy.execution.mode.Action;
import nl.tudelft.cse1110.andy.utils.PropertyUtils;
import nl.tudelft.cse1110.andy.writer.standard.StandardResultWriter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static nl.tudelft.cse1110.andy.utils.FilesUtils.*;

@Mojo(name = "run",
    requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
    defaultPhase = LifecyclePhase.CLEAN)
public class AndyMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(property = "full")
    private boolean full;

    @Parameter(property = "coverage")
    private boolean coverage;

    @Override
    public void execute() {
        File basedir = project.getBasedir();

        File workDir = null;
        try {
            /**
             * We should first create a temporary work directory and
             * copy the student solution, the config file, and the library code
             * to it.
             */
            workDir = Files.createTempDir();

            Collection<File> javaFiles = getJavaFiles(basedir);
            for (File javaFile : javaFiles) {
                copyFile(javaFile.getAbsolutePath(), workDir.getAbsolutePath());
            }

            /**
             * Create an output directory where we generate all the reports.
             * We delete it first, because there might be one already, from a
             * previous execution.
             */
            File outputDir = new File(concatenateDirectories(basedir.getAbsolutePath(), "andy"));
            if(outputDir.exists()) {
                deleteDirectory(outputDir);
            }
            createDirIfNeeded(outputDir.getAbsolutePath());

            /* We get the list of dependencies, to help the Andy's Java compiler to find them all */
            List<String> compileClasspathElements = project.getCompileClasspathElements();

            /* Run Andy! */
            new Andy(
                action(),
                workDir.getAbsolutePath(),
                outputDir.getAbsolutePath(),
                compileClasspathElements,
                new StandardResultWriter(PropertyUtils.getVersionInformation())
            ).run();

            /* Read output file */
            String output = readFile(new File(concatenateDirectories(outputDir.getAbsolutePath(), "stdout.txt")));
            System.out.println(output);
            System.out.println("\nCheck branch and mutation coverage in the /andy folder!");

        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            System.exit(-1);
        } finally {
            // Delete the work dir as it's not needed anymore
            if(workDir!=null)
                deleteDirectory(workDir);
        }
    }

    private Action action() {
        if(full)
            return Action.FULL_WITH_HINTS;
        if(coverage)
            return Action.COVERAGE;

        return Action.FULL_WITHOUT_HINTS;
    }

    private Collection<File> getJavaFiles(File basedir) {
        Collection<File> javaFilesInSrc = getAllJavaFiles(basedir.getAbsolutePath() + "/src");
        Collection<File> javaFilesInConfig = getAllJavaFiles(basedir.getAbsolutePath() + "/config");
        Collection<File> javaFiles = new ArrayList<>();
        javaFiles.addAll(javaFilesInSrc);
        javaFiles.addAll(javaFilesInConfig);
        return javaFiles;
    }

}
