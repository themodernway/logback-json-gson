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

package com.themodernway.logback.json.gson.test.util;

import java.util.Date;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

public class TestPOJO
{
    @SerializedName("date")
    private Date   m_date = new Date();

    @SerializedName("name")
    private String m_name = "TestPOJO";

    public TestPOJO()
    {
    }

    public void setDate(final Date date)
    {
        m_date = Objects.requireNonNull(date, "date is null");
    }

    public Date getDate()
    {
        return m_date;
    }

    public void setName(final String name)
    {
        m_name = Objects.requireNonNull(name, "name is null");
    }

    public String getName()
    {
        return m_name;
    }
}
