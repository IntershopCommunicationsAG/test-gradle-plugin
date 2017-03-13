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

import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import spock.lang.Specification

class AssertionsSpec extends Specification
{
    def "isErrorFree succeeds when text contains no failure"() {
        when:
        Assertions.isErrorFree('some context', """
        Text that does not contain any messages
        indicating failures at all""", ['error','exception'])

        then:
        noExceptionThrown()
    }

    def "isErrorFree fails when text contains any failure"(log) {
        when:
        Assertions.isErrorFree('some context', log, ['error','exception'])

        then:
        thrown(PowerAssertionError)

        where:
        log <<
                ["""Text with error on its first line
        and none on the second""",
                 """Text with mixed-case Error on its first line
        and none on the second""",
                 """Text with no error on its first line
        and mixed-case Error on the second""",
                 """Text with no error on its first line
        and mixed-case EXception on the second"""]
    }

    def "isErrorFree succeeds when text contains only allowed failures"(log) {
        when:
        Assertions.isErrorFree('some context', log, ['error','exception'],
                ['UnavoidableException: so be it','Starting error listener' ])

        then:
        noExceptionThrown();

        where:
        log <<
                ["""Text with UnavoidableException: so be it on its first line
        and none on the second""",
                 """Text with no failure on its first line, but
        the allowed message Starting error listener on the second""",
                ]
    }

    def "isErrorFree fails when text contains allowed failures and non-allowed failures"(log) {
        when:
        Assertions.isErrorFree('some context', log, ['error','exception'],
                ['UnavoidableException: so be it'])

        then:
        def powerAssertionError = thrown(PowerAssertionError)
        powerAssertionError.message.contains(log)
        powerAssertionError.message.contains('error')
        powerAssertionError.message.contains('exception')
        powerAssertionError.message.contains('UnavoidableException: so be it')


        where:
        log <<
                ["""Text with UnavoidableException: so be it on its first line
        and a real error on the second"""]
    }
}