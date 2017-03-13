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
 *  limitations under the License.
 */
package com.intershop.gradle.test.util

import groovy.transform.CompileStatic

import java.util.regex.Matcher
import java.util.regex.Pattern

import org.codehaus.groovy.runtime.powerassert.PowerAssertionError

/**
 * Help full assertions for tests
 */
@CompileStatic
class Assertions {

    private static class Region {
        int start
        int end
    }

    /**
     * This verifies that the file exists, it is not a directory
     * and contains the content.
     *
     * @param file      File for verification
     * @param content   Content of the file
     */
    static void fileHasContent(File file, String content) {
        assert file.exists()
        assert file.isFile()
        assert file.text == content
    }

    /**
     * This checks a file for failures and ignores allowed failures.
     *
     * @param file              File for verification
     * @param failures          List of failure patterns for verification
     * @param allowedFailures   List of allowed failures (optional)
     */
    static void isErrorFree(File file, List<String> failures, List<String> allowedFailures = []) {
        isErrorFree("File '$file.absolutePath'", file.text, failures, allowedFailures)
    }

    /**
     * This checks a string for failures and ignores allowed failures.
     *
     * @param contextDescription    Description for error messages
     * @param content               Content, that is verified
     * @param failures              List of failure patterns for verification
     * @param allowedFailures       List of allowed failures (optional)
     */
    static void isErrorFree(String contextDescription, String content, List<String> failures, List<String> allowedFailures = []) {
        Pattern failuresPattern = Pattern.compile('(?i)' + failures.collect { Pattern.quote(it) }.join('|'))

        List<Region> foundAllowedFailures = []

        if (allowedFailures) {
            Pattern allowedFailuresPattern = Pattern.compile(allowedFailures.collect { Pattern.quote(it) }.join('|'))

            Matcher allowedFailuresMatcher = allowedFailuresPattern.matcher(content)

            while (allowedFailuresMatcher.find()) {
                foundAllowedFailures << new Region(start:allowedFailuresMatcher.start(), end:allowedFailuresMatcher.end())
            }
        }

        Matcher failuresMatcher = failuresPattern.matcher(content)

        StringBuilder contentWithMarkers = new StringBuilder()

        List<Region> foundFailures = []

        while (failuresMatcher.find()) {
            boolean allowed = foundAllowedFailures.find {
                it.start <= failuresMatcher.start() && failuresMatcher.end() <= it.end
            }

            if (!allowed) {
                foundFailures << new Region(start:failuresMatcher.start(), end:failuresMatcher.end())
            }
        }

        if (!foundFailures.empty) {
            throw new PowerAssertionError(
                    """
            ${contextDescription} contains failure: 
            ${content}
            
            Failure indicators:
            ${failures.join(',')}
            
            Allowed failures:
            ${allowedFailures ? allowedFailures.join(',') : '(none)'}
            """)
        }
    }
}