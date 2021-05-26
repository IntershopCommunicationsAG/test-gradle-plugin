package com.intershop.gradle.test

import com.intershop.gradle.test.builder.TestIvyRepoBuilder
import com.intershop.gradle.test.builder.TestMavenRepoBuilder
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@Unroll
class AbstractIntegrationKotlinSpecSpec extends AbstractIntegrationKotlinSpec {

    def 'create subproject'() {
        when:
        initSettingsFile()
        File pDir = createSubProject('test1:test2',
            """
            plugins {
                java
            }
        """.stripIndent())

        File bFile = new File(pDir, 'build.gradle.kts')

        then:
        settingsFile.exists()
        pDir.exists()

        pDir.parentFile.name == 'test1'
        bFile.exists()

        bFile.text.contains("java")
        settingsFile.text.contains('include("test1:test2")')
    }


    @Unroll
    def 'test IVY repo builder publishing #gradleVersion'(gradleVersion) {
        given:
        file('settings.gradle.kts') << """
            rootProject.name = "component"
        """.stripIndent()

        writeJavaTestClass('com.intershop.test')

        File repoDir = new File(testProjectDir, 'build/ivyrepo')

        buildFile << """
            plugins {
                java
                `ivy-publish`
            }

            group = "com.intershop"
            version = "1.0.0"

            publishing {
                ${TestIvyRepoBuilder.declareRepositoryKotlin(repoDir)}
                publications {
                    create("intershopIvy", IvyPublication::class.java) {
                        from(components["java"])
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
            rootProject.name = "component"
        """.stripIndent()

        writeJavaTestClass('com.intershop.test')

        File repoDir = new File(testProjectDir, 'build/mvnrepo')

        buildFile << """
            plugins {
                java
                `maven-publish`
            }

            group = "com.intershop"
            version = "1.0.0"

            publishing {
                ${TestMavenRepoBuilder.declareRepositoryKotlin(repoDir)}
                publications {
                    create("intershopMvn", MavenPublication::class.java) {
                        from(components["java"])
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
