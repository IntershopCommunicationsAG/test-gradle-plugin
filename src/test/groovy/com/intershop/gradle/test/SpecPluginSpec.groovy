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

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class SpecPluginSpec extends AbstractIntegrationSpec {

    def 'test plugin functionality with java project'() {
        given:
        writeJavaTestClass('com.intershop.test')
        writeJavaTestClassTest('com.intershop.test.test')

        File settingsFile = file('settings.gradle')
        settingsFile << """
            rootProject.name= 'test'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'java'
                id 'com.intershop.gradle.test'
                id 'ivy-publish'
            }

            group = 'com.test'
            version = '1.0.0.0'

            sourceCompatibility = 1.7
            targetCompatibility = 1.7

            publishing {
                repositories {
                    ivy {
                        url "\${rootProject.buildDir}/repo"
                    }
                }
                publications {
                    ivy(IvyPublication) {
                        from components.java
                    }
                }
            }

            dependencies {
                testCompile 'junit:junit:4.12'
            }
            repositories {
                jcenter()
            }

        """.stripIndent()

        when:
        def result = preparedGradleRunner
                .withArguments('test', '--stacktrace', '-i')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':createClasspathManifest').outcome == SUCCESS
        result.task(':test').outcome == SUCCESS

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'test plugin functionality with groovy project'() {
        given:
        writeGroovyTestClass('com.intershop.test')
        writeGroovyTestClassSpec('com.intershop.test.test')
        writeIntershopSpec('com.intershop.test.test')

        File settingsFile = file('settings.gradle')
        settingsFile << """
            rootProject.name= 'groovytest'
        """.stripIndent()

        buildFile << """
            plugins {
                id 'java-gradle-plugin'
                id 'groovy'
                id 'com.intershop.gradle.test'
                id 'maven-publish'
            }

            group = 'com.test'
            version = '1.0.0.0'

            sourceCompatibility = 1.7
            targetCompatibility = 1.7

            publishing {
                repositories {
                    maven {
                        url "\${rootProject.buildDir}/repo"
                    }
                }
                publications {
                    mvn(MavenPublication) {
                        from components.java
                    }
                }
            }

            dependencies {
                testCompile 'junit:junit:4.12'
                testCompile('org.spockframework:spock-core:1.0-groovy-2.4'){
                        exclude group: 'org.codehaus.groovy'
                    }
            }
            repositories {
                jcenter()
            }

        """.stripIndent()

        when:
        def result = preparedGradleRunner
                .withArguments('test', '--stacktrace', '-i')
                .withGradleVersion(gradleVersion)
                .build()

        File testWorkingDir = new File(testProjectDir, 'build/test-working/IntershopSpec')

        then:
        result.task(':createClasspathManifest').outcome == SUCCESS
        result.task(':test').outcome == SUCCESS

        testWorkingDir.exists()

        where:
        gradleVersion << supportedGradleVersions
    }

    private File writeIntershopSpec(String packageDotted, boolean failTest = false, File baseDir = testProjectDir) {
        def path = 'src/test/groovy/' + packageDotted.replace('.', '/') + '/IntershopSpec.groovy'
        def groovyFile = file(path, baseDir)
        groovyFile << """package ${packageDotted}

            import com.intershop.gradle.test.AbstractIntegrationSpec

            public class IntershopSpec extends AbstractIntegrationSpec {

                def 'hello world test'() {
                    given:
                    writeGroovyTestClass('com.test')
                    when:
                    println 'Hello World'
                    then:
                    ! ${failTest}
                }
            }
        """.stripIndent()

        return groovyFile
    }
}
