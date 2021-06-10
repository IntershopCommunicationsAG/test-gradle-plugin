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
package com.intershop.gradle.test.builder

import groovy.xml.MarkupBuilder

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import static jakarta.xml.bind.DatatypeConverter.printHexBinary

/**
 * A builder for creating local maven repositories.
 *
 * For information about its usage, consider the following example:
 * <pre>
 * new TestMavenRepoBuilder().repository {*     project(artifactId:'foo') {*         dependency(artifactId:'dep')
 *}*     project(artifactId:'bar', packaging:'pkg', classifier:'cls') {*         module('sub1')
 *         module('sub2')
 *         parent(artifactId:'par', relativePath:'relPath')
 *         dependency(artifactId:'dep1', classifier:'cls', scope:'scope', type:'typ', optional:true)
 *         dependency(artifactId:'dep2', optional:false)
 *
 *         artifact('content')
 *         artifact {*             file(path:'foo/bar', 'bazzzz')
 *}*         artifact(classifier:'javadoc') {*             dir('foo/baz')
 *}*}*}.writeTo(testDir)
 * </pre>
 */
class TestMavenRepoBuilder extends BuilderSupport {
    static class Repository {
        String name
        List<Project> projects = []

        def writeTo(File directory) {
            projects*.writeTo(directory)
        }

        String declareRepository(File repoDir) {
            def _name = ''
            if (name) {
                _name = "name '$name'"
            }

            String returnValue = """
            repositories {
            maven {
                ${_name}
                url "${getURL(repoDir)}"
                }
            }
            """.stripIndent()

            return returnValue
        }

        String declareRepositoryForRepositoryHandler(File repoDir) {
            def _name = ''
            if (name) {
                _name = "name '$name'"
            }

            String returnValue = """
            maven {
                ${_name}
                url "${getURL(repoDir)}"
            }
            """.stripIndent()

            return returnValue
        }
    }

    static class Artifact {
        String classifier
        String ext = 'jar'
        def content
        List<ArchiveEntry> entries

        def writeTo(File projectDirectory, String prefix) {
            if (entries == null && content == null) {
                content = ''
            }

            def artifactFileName = classifier ?
                    "$prefix-$classifier.$ext" :
                    "$prefix.$ext"
            def artifactFile = new File(projectDirectory, artifactFileName)

            if (content != null) {
                if (content instanceof File) {
                    def File contentFile = content;
                    Files.copy(contentFile.toPath(), artifactFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                } else {
                    artifactFile << content
                }
            } else {
                zipTo(artifactFile)
            }

            def checksumFile = new File("${artifactFile.absolutePath}.sha1")
            def sha1 = MessageDigest.getInstance('SHA-1')
            def chksum = sha1.digest(artifactFile.bytes)
            checksumFile.text = printHexBinary(chksum)
        }

        def zipTo(File zipFile) {
            def zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile))

            try {
                entries*.writeTo(zipOutputStream)
            }
            finally {
                zipOutputStream.close()
            }
        }
    }

    static class Dependency {
        String groupId = 'com.example'
        String artifactId
        String version = '1.0.0'
        String type = 'jar'
        String classifier
        String scope
        boolean optional = false
        // TODO Exclusions
    }

    static class Parent {
        String groupId = 'com.example'
        String artifactId
        String version = '1.0.0'
        String relativePath
    }

    static class Module {
        String name
    }

    static class DependencyManagement {
        List<Dependency> dependencies = []
    }

    static class MvnProperties {
        Map<String, String> pairs = [:]
    }

    static class Project {
        String groupId = 'com.example'
        String artifactId
        String version = '1.0.0'
        String packaging = 'jar'
        String classifier
        Parent parent
        DependencyManagement dependencyManagement
        List<Module> modules = []
        List<Artifact> artifacts = []
        List<Dependency> dependencies = []
        MvnProperties mvnProperties

        def writeTo(File repoDir) {
            def projectDir = new File(repoDir, "${groupId.replace '.', '/'}/$artifactId/$version")
            projectDir.mkdirs()

            def pom = new StringWriter()
            def xml = new MarkupBuilder(pom)
            xml.'project'('xmlns': 'http://maven.apache.org/POM/4.0.0',
                    'xmlns:xsi': 'http://www.w3.org/2001/XMLSchema-instance',
                    'xsi:schemaLocation': 'http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd') {
                modelVersion('4.0.0')

                groupId(groupId)
                artifactId(artifactId)
                version(version)
                if (packaging != 'jar')
                    packaging(packaging)

                if(mvnProperties) {
                    delegate.properties {
                        mvnProperties.pairs.each { entry ->
                            "${entry.key}" ( "${entry.value}" )
                        }
                    }
                }

                if (dependencyManagement) {
                    delegate.dependencyManagement {
                        dependencies() {
                            dependencyManagement.dependencies.each { dep ->
                                dependency() {
                                    groupId(dep.groupId)
                                    artifactId(dep.artifactId)
                                    version(dep.version)
                                    if (dep.type != 'jar')
                                        type(dep.type)
                                    if (dep.classifier)
                                        classifier(dep.classifier)
                                    if (dep.scope)
                                        scope(dep.scope)
                                    if (dep.optional)
                                        optional(dep.optional)
                                }
                            }
                        }
                    }
                }

                if (parent) {
                    parent() {
                        groupId(parent.groupId)
                        artifactId(parent.artifactId)
                        version(parent.version)
                        if (parent.relativePath)
                            relativePath(parent.relativePath)
                    }
                }
                if (modules) {
                    modules() {
                        modules.each { submodule ->
                            module(submodule.name)
                        }
                    }
                }
                dependencies() {
                    dependencies.each { dep ->
                        dependency() {
                            groupId(dep.groupId)
                            artifactId(dep.artifactId)
                            version(dep.version)
                            if (dep.type != 'jar')
                                type(dep.type)
                            if (dep.classifier)
                                classifier(dep.classifier)
                            if (dep.scope)
                                scope(dep.scope)
                            if (dep.optional)
                                optional(dep.optional)
                        }
                    }
                }
            }

            artifacts << new Artifact(ext: 'pom', content: pom)

            def artifactPrefix = "$artifactId-$version"
            artifacts*.writeTo(projectDir, artifactPrefix)
        }
    }

    static interface ArchiveEntry {
        def writeTo(ZipOutputStream zipOutputStream)
    }

    static class ArchiveFileEntry implements ArchiveEntry {
        String path
        def content

        def writeTo(ZipOutputStream zipOutputStream) {
            def zipEntry = new ZipEntry(path)
            zipOutputStream.putNextEntry(zipEntry)
            zipOutputStream.write(content.bytes)
            zipOutputStream.closeEntry()
        }
    }

    static class ArchiveDirectoryEntry implements ArchiveEntry {
        String path

        def writeTo(ZipOutputStream zipOutputStream) {
            def zipEntry = new ZipEntry(path.endsWith('/') ? path : "$path/")
            zipOutputStream.putNextEntry(zipEntry)
            zipOutputStream.closeEntry()
        }
    }

    static
    final methodClassMap = ['artifact': Artifact, 'dependency': Dependency, 'parent': Parent,
                            'project': Project, 'repository': Repository, 'dependencyManagement': DependencyManagement,
                            'mvnProperties': MvnProperties]

    @Override
    protected void setParent(parent, child) {
        def classes = [parent.getClass(), child.getClass()]

        if (classes == [Repository, Project]) {
            parent.projects << child
            return
        }

        if (classes == [Project, MvnProperties]) {
            parent.mvnProperties = child
            return
        }

        if (classes == [Project, Dependency]) {
            parent.dependencies << child
            return
        }

        if (classes == [Project, DependencyManagement]) {
            parent.dependencyManagement = child
            return
        }

        if (classes == [DependencyManagement, Dependency]) {
            parent.dependencies << child
            return
        }

        if (classes == [Project, Parent]) {
            parent.parent = child
            return
        }

        if (classes == [Project, Module]) {
            parent.modules << child
            return
        }

        if (classes == [Project, Artifact]) {
            parent.artifacts << child
            return
        }

        if (classes == [Artifact, ArchiveFileEntry]) {
            if (parent.content != null) {
                throw new IllegalArgumentException("An artifact can either have content or archive entries, but not both.")
            }

            parent.entries = parent.entries ?: []
            parent.entries << child
            return
        }

        if (classes == [Artifact, ArchiveDirectoryEntry]) {
            if (parent.content != null) {
                throw new IllegalArgumentException("An artifact can either have content or archive entries, but not both.")
            }

            parent.entries = parent.entries ?: []
            parent.entries << child
            return
        }


        throw new IllegalArgumentException("Child of type ${child.getClass()} cannot have parent of type ${parent.getClass()}")
    }

    @Override
    protected Object getName(String name) {
        return name
    }

    @Override
    protected createNode(name) {
        methodClassMap[name].newInstance()
    }

    @Override
    protected createNode(name, value) {
        switch (name) {
            case 'artifact':
                return new Artifact(content: value)
            case 'dir':
                return new ArchiveDirectoryEntry(path: value)
            case 'module':
                return new Module(name: value)
        }
    }

    @Override
    protected Object createNode(name, Map attributes) {
        methodClassMap[name].newInstance(attributes)
    }

    @Override
    protected Object createNode(name, Map attributes, value) {
        def result = ['artifact': Artifact, 'file': ArchiveFileEntry][name].newInstance(attributes)
        result.content = value
        result
    }

    String declareRepositoryForRepositoryHandler(File repoDir, String name) {
        def _name = ''
        if (name) {
            _name = "name '$name'"
        }

        String returnValue = """
        maven {
            ${_name}
            url "${getURL(repoDir)}"
        }
        """.stripIndent()

        return returnValue
    }

    static String declareRepository(File repoDir) {
        String returnValue = """
        repositories {
        maven {
                url "${getURL(repoDir)}"
            }
        }
        """.stripIndent()

        return returnValue
    }

    static String declareRepositoryKotlin(File repoDir) {
        String returnValue = """
        repositories {
            maven {
                setUrl("${getURL(repoDir)}")
            }
        }
        """.stripIndent()

        return returnValue
    }

    static void declareRepository(org.gradle.api.Project project, File repoDir) {
        project.repositories {
            maven {
                url getURL(repoDir)
            }
        }
    }

    static String getURL(File repoDir) {
        "file://${repoDir.absolutePath.replace('\\', '/')}/"
    }
}
