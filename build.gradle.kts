import com.jfrog.bintray.gradle.BintrayExtension
import org.asciidoctor.gradle.jvm.AsciidoctorTask
import java.util.Date

plugins {
    // project plugins
    groovy
    // test coverage
    jacoco

    // ide plugin
    idea

    // plugin for documentation
    id("org.asciidoctor.jvm.convert") version "2.3.0"

    // publish plugin
    `maven-publish`

    // plugin for publishing to jcenter
    id("com.jfrog.bintray") version "1.8.4"
}

group = "com.intershop.gradle.test"
description = "Gradle test library - test extension for Gradle plugin builds"

version = "3.4.0"

java {
    withJavadocJar()
    withSourcesJar()
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

// set correct project status
if (project.version.toString().endsWith("-SNAPSHOT")) {
    status = "snapshot'"
}

tasks {
    withType<Test>().configureEach {
        testLogging.showStandardStreams = true

        // Gradle versions for test
        systemProperty("intershop.gradle.versions", "5.6.4,6.0")
        systemProperty("intershop.test.base.dir", (File(project.buildDir, "test-working")).absolutePath)
    }

    val copyAsciiDoc = register<Copy>("copyAsciiDoc") {
        includeEmptyDirs = false

        val outputDir = file("$buildDir/tmp/asciidoctorSrc")
        val inputFiles = fileTree(mapOf("dir" to rootDir,
            "include" to listOf("**/*.asciidoc"),
            "exclude" to listOf("build/**")))

        inputs.files.plus( inputFiles )
        outputs.dir( outputDir )

        doFirst {
            outputDir.mkdir()
        }

        from(inputFiles)
        into(outputDir)
    }

    withType<AsciidoctorTask> {
        dependsOn("copyAsciiDoc")

        setSourceDir(file("$buildDir/tmp/asciidoctorSrc"))
        sources(delegateClosureOf<PatternSet> {
            include("README.asciidoc")
        })

        outputOptions {
            setBackends(listOf("html5", "docbook"))
        }

        options = mapOf( "doctype" to "article",
            "ruby"    to "erubis")
        attributes = mapOf(
            "latestRevision"        to  project.version,
            "toc"                   to "left",
            "toclevels"             to "2",
            "source-highlighter"    to "coderay",
            "icons"                 to "font",
            "setanchors"            to "true",
            "idprefix"              to "asciidoc",
            "idseparator"           to "-",
            "docinfo1"              to "true")
    }

    withType<JacocoReport> {
        reports {
            xml.isEnabled = true
            html.isEnabled = true

            html.destination = File(project.buildDir, "jacocoHtml")
        }

        val jacocoTestReport by tasks
        jacocoTestReport.dependsOn("test")
    }

    withType<Test> {
        dependsOn("jar")
    }

    getByName("bintrayUpload")?.dependsOn("asciidoctor")
    getByName("publishToMavenLocal")?.dependsOn("asciidoctor")
}

publishing {
    publications {
        create("intershopMvn", MavenPublication::class.java) {

            from(components["java"])

            artifact(File(buildDir, "asciidoc/html5/README.html")) {
                classifier = "reference"
            }

            artifact(File(buildDir, "asciidoc/docbook/README.xml")) {
                classifier = "docbook"
            }

            pom {
                name.set(project.name)
                description.set(project.description)
                url.set("https://github.com/IntershopCommunicationsAG/${project.name}")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }

                scm {
                    connection.set("https://github.com/IntershopCommunicationsAG/${project.name}.git")
                    developerConnection.set("git@github.com:IntershopCommunicationsAG/${project.name}.git")
                    url.set("https://github.com/IntershopCommunicationsAG/${project.name}")
                }

                organization {
                    name.set("Intershop Communications AG")
                    url.set("http://intershop.com")
                }
            }
        }
    }
}

bintray {
    user = System.getenv("BINTRAY_USER")
    key = System.getenv("BINTRAY_KEY")

    setPublications("intershopMvn")
    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {

        repo = "GradlePlugins"
        name = project.name
        userOrg = "intershopcommunicationsag"

        setLicenses("Apache-2.0")
        vcsUrl = "https://github.com/IntershopCommunicationsAG/${project.name}"

        desc = project.description
        websiteUrl = "https://github.com/IntershopCommunicationsAG/${project.name}"
        issueTrackerUrl = "https://github.com/IntershopCommunicationsAG/${project.name}/issues"

        setLabels("intershop", "gradle", "plugin", "test")
        publicDownloadNumbers = true

        version(delegateClosureOf<BintrayExtension.VersionConfig> {
            name = project.version.toString()
            desc = "${project.description} ${project.version}"
            released  = Date().toString()
            vcsTag = project.version.toString()
        })
    })
}

repositories {
    jcenter()
}

dependencies {
    implementation("org.spockframework:spock-core:1.2-groovy-2.5") {
        exclude(group = "org.codehaus.groovy")
    }

    implementation("commons-io:commons-io:2.2")
    implementation("com.sun.xml.bind:jaxb-impl:2.2.3")

    implementation(gradleTestKit())
}

