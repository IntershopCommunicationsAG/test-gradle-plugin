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
import java.util.regex.Matcher
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import org.gradle.api.Project

/**
 * A builder for creating local Ivy repositories.
 *
 * For information about its usage, consider the following example:
 * <pre>
 * new TestIvyRepoBuilder().repository (ivyPattern: ivyPattern, artifactPattern: artifactPattern) {
 *
 *      module(org: 'com.company', name: 'module', rev: '1.0.0') {
 *          dependency org: 'com.company', name: 'dep1', rev: '1.0.0'
 *          dependency org: 'com.company', name: 'dep2', rev: '1.0.0'
 *          dependency org: 'com.company', name: 'dep3', rev: '1.0.0'
 *      }
 *      module(org: 'com.company', name: 'dep1', rev: '1.0.0')
 *      module(org: 'com.company', name: 'dep2', rev: '1.0.0')
 *      module(org: 'com.company', name: 'dep3', rev: '1.0.0')
 *
 * }.writeTo(testDir)
 * </pre>
 */
class TestIvyRepoBuilder extends BuilderSupport
{   
    static final String defaultIvyPattern = "[organisation]/[module]/ivy-[revision].xml"
    static final String defaultArtifactPattern = "[organisation]/[module]/[artifact]-[revision].[ext]"
    
    static class Repository 
    {
        String name
        List<Module> modules = []
        String ivyPattern = defaultIvyPattern
        String artifactPattern = defaultArtifactPattern
        
        def writeTo(File directory) 
        {
            modules*.writeTo(directory, ivyPattern, artifactPattern)
        }       
        
        String declareRepository(File testDir)
        {
            def _name = ''
            if (name) {
                _name = "name '$name'"
            } 
            
            """
            repositories {
            ivy {
                ${_name}
                ivyPattern "${testDir.absolutePath.replace('\\', '/')}/${ivyPattern}"
                artifactPattern "${testDir.absolutePath.replace('\\', '/')}/${artifactPattern}"
                artifactPattern "${testDir.absolutePath.replace('\\', '/')}/${ivyPattern}"
                }
            }
            """
        }
        
        String declareRepositoryForRepositoryHandler(File testDir)
        {
            def _name = ''
            if (name) {
                _name = "name '$name'"
            }
            
            """
            ivy {
                ${_name}
                url "file:///${testDir.absolutePath.replace('\\', '/')}/"
                layout('pattern') {
                    ivy "${ivyPattern}"
                    artifact "${artifactPattern}"
                    artifact "${ivyPattern}"
                }
            }
            """
        }
    } 
    
    static class Artifact
    {
        String name
        String type
        String ext
        def content
        String classifier
        List<String> configurations
        List<ArchiveEntry> entries
        
        def writeTo(File repositoryDirectory, String artifactPattern)
        {
            if (entries == null && content == null) {
                content = ''
            }
            
            def artifactFileName =  artifactPattern.replaceAll(/\[artifact\]/, Matcher.quoteReplacement(name)).
                                    replaceAll(/\[ext\]/, Matcher.quoteReplacement(ext)).
                                    replaceAll(/\[type\]/, Matcher.quoteReplacement(type));
            
           if(classifier != null) {
               artifactFileName = artifactFileName.replaceAll(/\[classifier\]/, Matcher.quoteReplacement(classifier)).
                                    replaceAll(/\(/, Matcher.quoteReplacement('')).
                                    replaceAll(/\)/, Matcher.quoteReplacement(''));
           } else {
               artifactFileName = artifactFileName.replaceAll(/\(-\[classifier\]\)/, Matcher.quoteReplacement(''));
           }                         
                                    
             
            def artifactFile = new File(repositoryDirectory, artifactFileName) 
            
            artifactFile.parentFile.mkdirs()
            
            if (content != null) {
                if (content instanceof File)
                {
                    def File contentFile = content;                
                    Files.copy(contentFile.toPath(), artifactFile.toPath(), StandardCopyOption.REPLACE_EXISTING)   
                } else {            
                    artifactFile << content
                }
            }
            else {
                zipTo(artifactFile)
            }                            
        }
        
        def zipTo(File zipFile)
        {
            def zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile))
                                  
            try {
                entries*.writeTo(zipOutputStream)                                              
            }
            finally
            {
                zipOutputStream.close()
            }            
        }
    }          
   
    static class Dependency 
    {
        String org = 'com.example'
        String name
        String rev = '1'
        List<String> configurations
    }
            
    static class Module 
    {
        String org = 'com.example'
        String name
        String rev = '1'
        List<Artifact> artifacts = []
        List<Dependency> dependencies = []
        List<Configuration> configurations = []
        boolean transitive = true
        String defaultConfMapping
        Closure<?> extraElements
        Map<String, String> extraInfoAttributes = [:]
        
        String resolvePattern(String pattern)
        {
            pattern.replaceAll(/\[organisation\]/, Matcher.quoteReplacement(org)).
            replaceAll(/\[module\]/, Matcher.quoteReplacement(name)).
            replaceAll(/\[revision\]/, Matcher.quoteReplacement(rev))
        }
        
        def writeTo(File repositoryDirectory, String ivyPattern, String artifactPattern) 
        {
            def ivyFileName = resolvePattern(ivyPattern).
                                     replaceAll(/\[artifact\]/, 'ivy').
                                     replaceAll(/\[ext\]/, 'xml').
                                     replaceAll(/\[type\]/, 'ivy')
                  
            def ivyFile = new File(repositoryDirectory, ivyFileName)            
            ivyFile.parentFile.mkdirs()                                     
                                                     
            ivyFile.withWriter { writer ->
                def xml = new MarkupBuilder(writer)
                
                              
                xml.'ivy-module' ('xmlns:m':'http://ant.apache.org/ivy/maven', version:'2.0') {
                    def infoAttributes = [organisation:org, module:name, revision:rev]                    
                    infoAttributes += extraInfoAttributes
                    
                    info (infoAttributes) {
                        if (extraElements) {
                            extraElements.delegate = xml
                            extraElements.setResolveStrategy(Closure.DELEGATE_FIRST)
                            extraElements.call()
                        }
                    }
                    if (configurations) {
                        def configurationsAttributes = [:]
                        if (defaultConfMapping) {
                            configurationsAttributes['defaultconfmapping'] = defaultConfMapping
                        }                                               
                        
                        configurations (configurationsAttributes) {
                            configurations.each { configurationObject ->
                                if (configurationObject.extendedConfs) {
                                    conf (name:configurationObject.name, 'extends':configurationObject.extendedConfs.join(', '))
                                } else {
                                    conf (name:configurationObject.name)
                                }                                                       
                            }                        
                        }
                    }
                    publications {
                        artifacts.each { artifactObject ->
                            def artifactAttributes = [name:artifactObject.name, type:artifactObject.type, ext:artifactObject.ext]
                            
                            if (artifactObject.classifier) {
                                artifactAttributes['m:classifier'] = artifactObject.classifier
                            }
                            
                            if (artifactObject.configurations) {
                                artifactAttributes['conf'] = artifactObject.configurations.join(',')    
                            }
                            
                            artifact artifactAttributes
                        }
                    }                                       
                    dependencies {
                        dependencies.each { dependencyObject ->
                            def dependencyAttributes = [org:dependencyObject.org, name:dependencyObject.name, rev:dependencyObject.rev, transitive:transitive]
                            
                            if (dependencyObject.configurations) {
                                dependencyAttributes.conf = dependencyObject.configurations.join(',')                                
                            }
                            dependency dependencyAttributes
                        }
                    }                    
                }
            }   
            
            artifacts*.writeTo(repositoryDirectory, resolvePattern(artifactPattern))         
        }
    }
       
    static interface ArchiveEntry {
        def writeTo(ZipOutputStream zipOutputStream)
    }
    
    static class ArchiveFileEntry implements ArchiveEntry
    {
        String path
        def content 
        
        def writeTo(ZipOutputStream zipOutputStream)
        {            
            def zipEntry = new ZipEntry(path)
            zipOutputStream.putNextEntry(zipEntry)
            zipOutputStream.write(content.bytes)
            zipOutputStream.closeEntry()
        }
    }
    
    static class ArchiveDirectoryEntry  implements ArchiveEntry
    {
        String path
        
        def writeTo(ZipOutputStream zipOutputStream)
        {
            def zipEntry = new ZipEntry(path.endsWith("/") ? path : path + "/")            
            zipOutputStream.putNextEntry(zipEntry)            
            zipOutputStream.closeEntry()
        }
    }
    
    static class Configuration
    {
        String name
        List<String> extendedConfs = []
    }
    
    protected static final Map<String,Class> methodClassMap = ['artifact':Artifact, 'dependency':Dependency, 'module':Module, 'repository':Repository, 'configuration':Configuration]
    
    @Override
    protected void setParent(parent, child)
    {                
        def classes = [parent.getClass(), child.getClass()]           
         
        if (classes == [Repository, Module]) {
            parent.modules << child
            return
        }
                
        if (classes == [Module, Dependency]) {
            parent.dependencies << child
            return
        }
        
        if (classes == [Module, Artifact]) {
            parent.artifacts << child
            return
        }
        
        if (classes == [Module, Configuration]) {
            parent.configurations << child
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

    void extra(Closure<?> extraElements = null)
    {
        if (!getCurrent() instanceof Module) {
            throw new IllegalArgumentException("Extra-XML can only be added to modules. Tried to call extra-Method on: " + getCurrent())
        }
                
        getCurrent().extraElements = extraElements
    }
    
    void extra(Map<String, String> extraAttributes, Closure<?> extraElements = null)
    {               
        if (!getCurrent() instanceof Module) {
            throw new IllegalArgumentException("Extra-XML can only be added to modules. Tried to call extra-Method on: " + getCurrent())
        }         
        
        getCurrent().extraInfoAttributes = extraAttributes 
        getCurrent().extraElements = extraElements
    }
    
    @Override
    protected createNode(name)
    {                
        methodClassMap[name].newInstance()
    }

    @Override
    protected createNode(name, value)
    {
        def result = ['artifact':Artifact, 'dir':ArchiveDirectoryEntry][name].newInstance()
        
        if (name == 'artifact') {
            result.content = value
        } else {
            result.path = value
        }
        
        result        
    }

    @Override
    protected Object createNode(name, Map attributes)
    {
        methodClassMap[name].newInstance(attributes)        
    }

    @Override
    protected Object createNode(name, Map attributes, value)
    {
        def result = ['artifact':Artifact, 'file': ArchiveFileEntry][name].newInstance(attributes)
        result.content = value
        result
    }    
    
    static String declareRepository(File testDir)
    {
        """
        repositories {
        ivy {
            ivyPattern "${testDir.toURI().toURL()}/${defaultIvyPattern}"
            artifactPattern "${testDir.toURI().toURL()}/${defaultArtifactPattern}"
            }
        }
        """
    }       
    
    static void declareRepository(Project project, File testDir)
    {
        project.repositories {
            ivy {
                ivyPattern "${testDir.absolutePath.replace('\\', '/')}/${defaultIvyPattern}"
                artifactPattern "${testDir.absolutePath.replace('\\', '/')}/${defaultArtifactPattern}"
            }
        }
    }      
}
