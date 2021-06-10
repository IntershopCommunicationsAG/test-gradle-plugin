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
import spock.lang.Unroll

@Unroll
class AbstractIntegrationSpecSpec extends AbstractIntegrationSpec {

    def 'test testProjectDir'() {
        when:
        File javaFile = writeJavaTestClass('com.intershop')

        then:
        testProjectDir.exists()
        javaFile.toURI().toString().contains('build/test-working/AbstractIntegrationSpecSpec/test-testProjectDir/src')
    }

    def 'test gradle version configuration'() {
        when:
        List<String> versions = supportedGradleVersions

        then:
        versions.size() == 1
    }

    def 'create hello world file'() {
        when:
        writeJavaTestClass('com.intershop')
        File f = new File(testProjectDir, 'src/main/java/com/intershop/HelloWorld.java')

        then:
        f.exists()
    }

    def 'create dir'() {
        when:
        File dir = directory('test/testdir')

        then:
        dir.exists()
        dir.isDirectory()
    }

    def 'create file'() {
        when:
        File file = file('test/testdir/testfile.test')

        then:
        file.exists()
        file.isFile()
    }

    def 'copyResources test'() {
        when:
        copyResources('conf','test/conf')
        File f = new File(testProjectDir, 'test/conf/configuration/corporate.properties')

        then:
        f.exists()
        f.isFile()
    }

    def 'copyResources to test dir'() {
        when:
        copyResources('conf')
        File f = new File(testProjectDir, 'configuration/corporate.properties')

        then:
        f.exists()
        f.isFile()
    }

    def 'test IVY repo builder'() {
        when:
        File repoDir = new File(testProjectDir, 'build/ivyrepo')
        new TestIvyRepoBuilder().repository {
            module(org:'com.intershop.component', name:'component', rev:'1.1.2.3')
            module(org:'com.intershop.component', name:'component', rev:'2.1.2.3')
        }.writeTo(repoDir)

        then:
        repoDir.exists()
        new File(repoDir, 'com.intershop.component/component/1.1.2.3/ivy-1.1.2.3.xml').exists()
        new File(repoDir, 'com.intershop.component/component/2.1.2.3/ivy-2.1.2.3.xml').exists()
    }

    def 'test IVY repo builder with different pattern'() {
        when:
        File repoDir = new File(testProjectDir, 'build/ivyrepo')

        new TestIvyRepoBuilder().repository(
                ivyPattern: '[organisation]/[module]/[revision]/[type]s/ivy-[revision].xml',
                artifactPattern: '[organisation]/[module]/[revision]/[ext]s/[artifact]-[type](-[classifier])-[revision].[ext]') {
            module(org:'com.intershop.component', name:'component', rev:'1.1.2.3')
            module(org:'com.intershop.component', name:'component', rev:'2.1.2.3')
        }.writeTo(repoDir)

        then:
        repoDir.exists()
        new File(repoDir, 'com.intershop.component/component/1.1.2.3/ivys/ivy-1.1.2.3.xml').exists()
        new File(repoDir, 'com.intershop.component/component/2.1.2.3/ivys/ivy-2.1.2.3.xml').exists()
    }

    def 'test MVN repo builder'() {
        when:
        File repoDir = new File(testProjectDir, 'build/mvnrepo')
        new TestMavenRepoBuilder().repository {
            project(artifactId:'component', groupId:'com.intershop.component', version: '1.1.2.3') {
                artifact('content')
            }
            project(artifactId:'component', groupId:'com.intershop.component', version: '2.1.2.3') {
                artifact('content')
            }
        }.writeTo(repoDir)

        then:
        repoDir.exists()
        new File(repoDir, 'com/intershop/component/component/1.1.2.3/component-1.1.2.3.pom').exists()
        new File(repoDir, 'com/intershop/component/component/2.1.2.3/component-2.1.2.3.pom').exists()
    }

    def 'test MVN repo builder - dependencyManagement'() {
        when:
        File repoDir = new File(testProjectDir, 'build/mvnrepo')
        new TestMavenRepoBuilder().repository {
            project(artifactId:'component', groupId:'com.intershop.component', version: '1.1.2') {
                mvnProperties pairs: [ "key1":"value1", "key2":"value2"]
                dependencyManagement {
                    dependency groupId: 'com.other', artifactId: 'library2', version: '1.0.0'
                }
            }
        }.writeTo(repoDir)

        then:
        repoDir.exists()
        new File(repoDir, 'com/intershop/component/component/1.1.2/component-1.1.2.pom').exists()
        new File(repoDir, 'com/intershop/component/component/1.1.2/component-1.1.2.pom').text.contains("dependencyManagement")
    }
}
