/*
 * Copyright (c) 2018, The Modern Way. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.themodernway.logback.json.gson.test

import com.themodernway.logback.json.gson.test.util.AbstractSpecification

public class BasicTestsSpecification extends AbstractSpecification
{
    def setupSpec()
    {
    }

    def cleanupSpec()
    {
    }

    def "dummy(1)"()
    {
        setup:
        echo "dummy(1)"
        echo "dummy(1)"
        echo "dummy(1)"

        expect:
        true == true
    }

    def "dummy(2)"()
    {
        setup:
        echo "dummy(2)"

        expect:
        true == true
    }

    def "dummy(3)"()
    {
        setup:
        echo "dummy(3)"

        logger().error("oops", new NullPointerException("hi"))

        expect:
        true == true
    }

    def "dummy(4)"()
    {
        setup:
        echo "dummy(4)"

        logger().info("hello", 6, 'Dean', [name: 'Maël Hörz\u00A9\n'])

        expect:
        true == true
    }
}
