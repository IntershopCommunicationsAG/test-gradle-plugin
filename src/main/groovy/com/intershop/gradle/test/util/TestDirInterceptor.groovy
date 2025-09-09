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
package com.intershop.gradle.test.util

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.apache.commons.io.FileUtils

import org.spockframework.runtime.extension.AbstractMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation

/**
 * {@link @TestDir} listener.
 */
@CompileStatic
class TestDirInterceptor extends AbstractMethodInterceptor
{
    private final String  baseDir
    private final boolean clean
    private final String  fieldName
    private final boolean shared
    private final boolean overwrite
    private final boolean large


    TestDirInterceptor ( String baseDir, boolean clean, String fieldName, boolean shared, boolean overwrite, boolean large)
    {
        this.baseDir   = baseDir
        this.clean     = clean
        this.fieldName = fieldName
        this.shared    = shared
        this.overwrite = overwrite
        this.large = large
    }


    /**
     * Deletes directory specified recursively.
     *
     * @param directory directory to delete
     * @return directory deleted
     */
    private File deleteUsingJava ( File directory )
    {
        FileUtils.deleteDirectory(directory)

        assert !directory.exists()
        directory
    }

    private void deleteUsingOSOnWindows ( File directory )
    {
        println "Deleting directory '${directory}' using rmdir..."

        def rmDirProc = ['cmd', '/c', "\"rmdir /S /Q ${directory.absolutePath}\""].execute()
        StreamGobbler errorGobbler = new StreamGobbler(rmDirProc.getErrorStream());
        StreamGobbler outputGobbler = new StreamGobbler(rmDirProc.getInputStream());
        errorGobbler.start();
        outputGobbler.start();

        int returnCode = rmDirProc.waitFor()

        if (returnCode || directory.exists()) {
            throw new RuntimeException("Unable to delete directory '${directory.absolutePath}' using rmdir. Return code: ${returnCode}.")
        }

        println "Done deleting directory '${directory}' using rmdir."
    }

    private void deleteUsingOSOnLinux ( File directory )
    {
        println "Deleting directory '${directory}' using rm -rf..."

        def rmDirProc = ['sh', '-c', "rm -rf \"${directory.absolutePath}\""].execute()
        StreamGobbler errorGobbler = new StreamGobbler(rmDirProc.getErrorStream());
        StreamGobbler outputGobbler = new StreamGobbler(rmDirProc.getInputStream());
        errorGobbler.start();
        outputGobbler.start();

        int returnCode = rmDirProc.waitFor()

        if (returnCode || directory.exists()) {
            throw new RuntimeException("Unable to delete directory '${directory.absolutePath}' using rm -rf. Return code: ${returnCode}.")
        }

        println "Done deleting directory '${directory}' using rm -rf."
    }


    @CompileStatic(TypeCheckingMode.SKIP)
    private void setupTestDir (testDirName, specInstance, IMethodInvocation invocation)
    {
        File  testDir = new File(baseDir, testDirName ).canonicalFile

        if ( testDir.directory )
        {
            // Cleaning directory
            if ( clean )
            {
                if (large) {
                    if (System.properties['os.name'].startsWith('Windows')) {
                        deleteUsingOSOnWindows testDir
                    } else {
                        deleteUsingOSOnLinux testDir
                    }
                } else {
                    deleteUsingJava testDir
                }

            }
            else
            {
                if (!overwrite)
                {
                    // Creating new directory next to existing one
                    for ( int counter = 1; testDir.directory; counter++ )
                    {
                        testDir = new File( baseDir, testDirName + "_$counter" ).canonicalFile
                    }
                }
            }
        }

        if (!testDir.exists()) {
            assert testDir.mkdirs(), "Failed to create test directory [$testDir]"
        }

        specInstance."$fieldName"         = testDir
        assert specInstance."$fieldName" == testDir
        invocation.proceed()
    }

    @Override
    void interceptSetupMethod ( IMethodInvocation invocation )
    {
        if (shared) {
            invocation.proceed()
            return;
        }

        final specInstance = invocation.instance
        String testDirName

        if (invocation.feature.parameterized)
        {
            testDirName  = "${ specInstance.class.simpleName }/${ invocation.iteration.name.replaceAll( /\W+/, '-' ) }"
        }
        else
        {
            testDirName  = "${ specInstance.class.simpleName }/${ invocation.feature.name.replaceAll( /\W+/, '-' ) }"
        }

        setupTestDir(testDirName, specInstance, invocation)
    }

    @Override
    void interceptSetupSpecMethod ( IMethodInvocation invocation )
    {
        if (!shared) {
            invocation.proceed()
            return;
        }

        final specInstance = invocation.instance
        String testDirName = specInstance.class.simpleName
        setupTestDir(testDirName, specInstance, invocation)
    }
}
