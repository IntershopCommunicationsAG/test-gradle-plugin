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

import com.intershop.gradle.test.builder.TestIvyRepoBuilder
import com.intershop.gradle.test.builder.TestMavenRepoBuilder
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@Unroll
class AbstractIntegrationGroovySpecSpec extends AbstractIntegrationGroovySpec {

    def 'create subproject'() {
        when:
        initSettingsFile()
        File pDir = createSubProject('test1:test2',
        """
            apply plugin: 'java'
        """)

        File bFile = new File(pDir, 'build.gradle')

        then:
        settingsFile.exists()
        pDir.exists()

        pDir.parentFile.name == 'test1'
        bFile.exists()

        bFile.text.contains("apply plugin: 'java'")
        settingsFile.text.contains("include 'test1:test2'")
    }


    @Unroll
    def 'test IVY repo builder publishing #gradleVersion'(gradleVersion) {
        given:
        file('settings.gradle') << """
            rootProject.name = 'component'
        """.stripIndent()

        writeJavaTestClass('com.intershop.test')

        File repoDir = new File(testProjectDir, 'build/ivyrepo')

        buildFile << """
            plugins {
                id 'java'
                id 'ivy-publish'
            }

            group = 'com.intershop'
            version = '1.0.0'

            publishing {
                ${TestIvyRepoBuilder.declareRepository(repoDir)}
                publications {
                    ivy(IvyPublication) {
                        from components.java
                    }
                }
            }
        """.stripIndent()

        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .forwardOutput()
            .withArguments('publish', '--stacktrace', '-i')
            .withGradleVersion(gradleVersion)
            .build()

        then:
        result.task(':publish').outcome == SUCCESS
        repoDir.exists()
        new File(repoDir, 'com.intershop/component/1.0.0/ivy-1.0.0.xml').exists()
        new File(repoDir, 'com.intershop/component/1.0.0/component-1.0.0.jar').exists()

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'test MVN repo builder publishing #gradleVersion'(gradleVersion) {
        given:
        file('settings.gradle') << """
            rootProject.name = 'component'
        """.stripIndent()

        writeJavaTestClass('com.intershop.test')

        File repoDir = new File(testProjectDir, 'build/mvnrepo')

        buildFile << """
            plugins {
                id 'java'
                id 'maven-publish'
            }

            group = 'com.intershop'
            version = '1.0.0'

            publishing {
                ${TestMavenRepoBuilder.declareRepository(repoDir)}
                publications {
                    mvn(MavenPublication) {
                        from components.java
                    }
                }
            }
        """.stripIndent()

        when:
        def result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .forwardOutput()
            .withArguments('publish', '--stacktrace', '-i')
            .withGradleVersion(gradleVersion)
            .build()

        then:
        result.task(':publish').outcome == SUCCESS
        repoDir.exists()
        new File(repoDir, 'com/intershop/component/1.0.0/component-1.0.0.pom').exists()
        new File(repoDir, 'com/intershop/component/1.0.0/component-1.0.0.jar').exists()

        where:
        gradleVersion << supportedGradleVersions
    }

}
