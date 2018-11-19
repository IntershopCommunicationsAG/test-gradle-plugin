/*
 * Copyright 2015 Intershop Communications AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intershop.gradle.test

import com.intershop.gradle.test.util.TestDir
import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

/**
 * This abstract spec implements a special
 * method and a setup, needed for the Gradle test kit.
 * Furthermore it contains some helper methods for
 * Gradle plugin testing.
 */
@Slf4j
abstract class AbstractIntegrationSpec extends Specification {

    public final static String DEBUGSUPPORT = 'IDE_TEST_DEBUG_SUPPORT'
    /**
     * Project directory for tests
     */
    @TestDir
    File testProjectDir

    /**
     * Build file for root test project
     */
    // build file
    File buildFile

    /**
     * Returns a list of Gradle versions from the test
     * system properties 'intershop.gradle.versions'
     */
    List<String> getSupportedGradleVersions() {
        String gradleVersionsProps = System.properties['intershop.gradle.versions']?: ''
        String[] versionList = gradleVersionsProps.split(',')
        return versionList*.trim()
    }

    /**
     * Creates an GradleRunner with the project directory, the prepared classpath.
     * Output will be forwarded to system output
     *
     * @return prepared GradleRunner
     */
    protected GradleRunner getPreparedGradleRunner() {
        String debugSupportStr = System.getenv(DEBUGSUPPORT)
        boolean debugSupport = (debugSupportStr != null && debugSupportStr.toLowerCase().trim() == 'true')

        return GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withPluginClasspath()
                .withDebug(debugSupport)
                .forwardOutput()
    }

    /**
     * Test helper method creates a Java test file 'HelloWorld.java'
     *
     * @param packageDotted     package (with dots) for the test Java class
     * @param baseDir           project directory. Default value is testProjectDir.
     */
    protected File writeJavaTestClass(String packageDotted, File baseDir = testProjectDir) {
        String path = 'src/main/java/' + packageDotted.replace('.', '/') + '/HelloWorld.java'
        File javaFile = file(path, baseDir)
        javaFile << """package ${packageDotted};
            public class HelloWorld {
                public static void main(String[] args) {
                    System.out.println("Hello Integration Test");
                }
            }
        """.stripIndent()

        return javaFile
    }

    /**
     * Test helper method creates a Groovy test file 'HelloWorld.groovy'
     *
     * @param packageDotted     package (with dots) for the test Groovy class
     * @param baseDir           project directory. Default value is testProjectDir.
     */
    protected File writeGroovyTestClass(String packageDotted, File baseDir = testProjectDir) {
        String path = 'src/main/groovy/' + packageDotted.replace('.', '/') + '/HelloWorld.groovy'
        File groovyFile = file(path, baseDir)
        groovyFile << """package ${packageDotted}

            public class HelloWorld {
                public static void main(String[] args) {
                    println "Hello Integration Test"
                }
            }
        """.stripIndent()

        return groovyFile
    }

    /**
     * Test helper method creates a creates a Junit test Java file 'HelloWorldTest.java'
     *
     * @param packageDotted     package (with dots) for the Junit test Java class
     * @param failTest          the test result of the test method. Default value is false.
     * @param baseDir           project directory. Default value is testProjectDir.
     */
    protected File writeJavaTestClassTest(String packageDotted, boolean failTest = false, File baseDir = testProjectDir) {
        def path = 'src/test/java/' + packageDotted.replace('.', '/') + '/HelloWorldTest.java'
        def javaFile = file(path, baseDir)
        javaFile << """package ${packageDotted};

            import org.junit.Test;
            import static org.junit.Assert.assertFalse;
            public class HelloWorldTest {
                @Test public void doesSomething() {
                    assertFalse( $failTest );
                }
            }
        """.stripIndent()

        return javaFile
    }

    /**
     * Test helper method creates a Groovy spec file 'HelloWorldSpec.java'
     *
     * @param packageDotted     package (with dots) for the Groovy spec file
     * @param failTest          the test result of the test method. Default value is false.
     * @param baseDir           project directory. Default value is testProjectDir.
     */
    protected File writeGroovyTestClassSpec(String packageDotted, boolean failTest = false, File baseDir = testProjectDir) {
        def path = 'src/test/groovy/' + packageDotted.replace('.', '/') + '/HelloWorldSpec.groovy'
        def groovyFile = file(path, baseDir)
        groovyFile << """package ${packageDotted}

            import spock.lang.Specification

            public class HelloWorldSpec extends Specification {

                def 'hello world test'() {
                    when:
                    println 'Hello World'
                    then:
                    ! ${failTest}
                }
            }
        """.stripIndent()

        return groovyFile
    }

    /**
     * Test helper method creates a file for path
     *
     * @param path      path of the new file
     * @param baseDir   project directory. Default value is testProjectDir.
     * @return          the file object of the specified path
     */
    protected File file(String path, File baseDir = testProjectDir) {
        def splitted = path.split('/')
        def directory = splitted.size() > 1 ? directory(splitted[0..-2].join('/'), baseDir) : baseDir
        def file = new File(directory, splitted[-1])
        file.createNewFile()
        return file
    }

    /**
     * Test helper method creates a directory for the specified path
     *
     * @param path of the new directory
     * @param project directory. Default value is testProjectDir.
     * @return the file object of the specified path
     */
    protected File directory(String path, File baseDir = testProjectDir) {
        new File(baseDir, path).with {
            mkdirs()
            return it
        }
    }

    /**
     * Test helper method copies resources from test resources to destination
     *
     * @param srcDir    path fo the resources
     * @param target    target path, default value is an empty string
     * @param baseDir   base directory for copy target
     */
    protected void copyResources(String srcDir, String target = '', File baseDir = testProjectDir) {
        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = classLoader.getResource(srcDir);
        if (resource == null) {
            throw new RuntimeException("Could not find classpath resource: $srcDir")
        }

        File resourceFile = new File(resource.toURI())
        if (resourceFile.file) {
            if(target) {
                FileUtils.copyFile(resourceFile, new File(baseDir, target))
            } else {
                FileUtils.copyFile(resourceFile, baseDir)
            }
        } else {
            if(target) {
                FileUtils.copyDirectory(resourceFile, new File(baseDir, target))
            } else {
                FileUtils.copyDirectory(resourceFile, baseDir)
            }
        }
    }
}
