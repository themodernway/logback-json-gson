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

package com.themodernway.logback.json.gson;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.themodernway.logback.json.core.IJSONCommon;
import com.themodernway.logback.json.core.IJSONFormatter;

import net.dongliu.gson.GsonJava8TypeAdapterFactory;

public class GsonJSONFormatter implements IJSONFormatter, IJSONCommon
{
    private Gson    m_format;

    private boolean m_pretty;

    public GsonJSONFormatter()
    {
        m_format = new GsonBuilder().registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory()).disableHtmlEscaping().serializeNulls().serializeSpecialFloatingPointValues().create();
    }

    @Override
    public boolean isPretty()
    {
        return m_pretty;
    }

    @Override
    public void setPretty(final boolean pretty)
    {
        if (pretty != isPretty())
        {
            if (m_pretty = pretty)
            {
                m_format = new GsonBuilder().registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory()).disableHtmlEscaping().serializeNulls().serializeSpecialFloatingPointValues().setPrettyPrinting().create();
            }
            else
            {
                m_format = new GsonBuilder().registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory()).disableHtmlEscaping().serializeNulls().serializeSpecialFloatingPointValues().create();
            }
        }
    }

    @Override
    public String toJSONString(final Map<String, Object> target) throws Exception
    {
        final GsonEscapedStringBuilderWriter writer = new GsonEscapedStringBuilderWriter(4096);

        m_format.toJson(target, writer);

        return writer.toString();
    }

    protected static class GsonEscapedStringBuilderWriter extends Writer
    {
        private final StringBuilder m_builder;

        public GsonEscapedStringBuilderWriter(final int size)
        {
            this(new StringBuilder(size));
        }

        protected GsonEscapedStringBuilderWriter(final StringBuilder builder)
        {
            super(Objects.requireNonNull(builder));

            m_builder = builder;
        }

        private void escape(final char c)
        {
            if (c <= 0x7f)
            {
                m_builder.append(c);
            }
            else
            {
                m_builder.append(String.format("\\u%04x", (int) c));
            }
        }

        @Override
        public void write(final int c) throws IOException
        {
            escape((char) c);
        }

        @Override
        public void write(final char chr[], final int off, final int len) throws IOException
        {
            if (null != chr)
            {
                for (int i = 0; i < len; i++)
                {
                    escape(chr[i + off]);
                }
            }
        }

        @Override
        public void write(final String str) throws IOException
        {
            if (null != str)
            {
                append(str);
            }
        }

        @Override
        public void write(final String str, final int off, final int len) throws IOException
        {
            if (null != str)
            {
                append(str.substring(off, off + len));
            }
        }

        @Override
        public GsonEscapedStringBuilderWriter append(final CharSequence chs) throws IOException
        {
            if (null != chs)
            {
                final int size = chs.length();

                for (int i = 0; i < size; i++)
                {
                    escape(chs.charAt(i));
                }
            }
            return this;
        }

        @Override
        public GsonEscapedStringBuilderWriter append(final CharSequence chs, final int beg, final int end) throws IOException
        {
            if (null != chs)
            {
                append(chs.subSequence(beg, end));
            }
            return this;
        }

        @Override
        public GsonEscapedStringBuilderWriter append(final char c) throws IOException
        {
            escape(c);

            return this;
        }

        @Override
        public String toString()
        {
            return m_builder.toString();
        }

        @Override
        public void flush()
        {
        }

        @Override
        public void close() throws IOException
        {
        }

        public GsonEscapedStringBuilderWriter clear()
        {
            m_builder.setLength(0);

            return this;
        }
    }
}
