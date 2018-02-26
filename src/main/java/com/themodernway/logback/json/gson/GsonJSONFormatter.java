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
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.themodernway.logback.json.core.IJSONCommon;
import com.themodernway.logback.json.core.IJSONFormatter;

import net.dongliu.gson.GsonJava8TypeAdapterFactory;

public class GsonJSONFormatter implements IJSONFormatter, IJSONCommon
{
    private final static int              RND_BUFF_SZ = 16;

    private final static int              MID_BUFF_SZ = 4096;

    private final static int              MIN_BUFF_SZ = MID_BUFF_SZ / 4;

    private final static int              MAX_BUFF_SZ = MID_BUFF_SZ * 4;

    private final static long             MIN_WINDOWS = 1L;

    private final static long             MAX_WINDOWS = 3600000L;

    private final static Gson             NORMAL_GSON = new GsonBuilder().registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory()).disableHtmlEscaping().serializeNulls().serializeSpecialFloatingPointValues().create();

    private final static Gson             PRETTY_GSON = new GsonBuilder().registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory()).disableHtmlEscaping().serializeNulls().serializeSpecialFloatingPointValues().setPrettyPrinting().create();

    private Gson                          m_format    = NORMAL_GSON;

    private long                          m_window    = MIN_WINDOWS;

    private boolean                       m_pretty    = Boolean.FALSE;

    private final TimeWindowMovingAverage m_moving    = new TimeWindowMovingAverage(MIN_WINDOWS, TimeUnit.MILLISECONDS).add(MID_BUFF_SZ);

    public GsonJSONFormatter()
    {
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
                m_format = PRETTY_GSON;
            }
            else
            {
                m_format = NORMAL_GSON;
            }
        }
    }

    public void setAverageWindow(long window)
    {
        if ((window = Math.min(Math.max(window, MIN_WINDOWS), MAX_WINDOWS)) != getAverageWindow())
        {
            final int last = getAverageBufferingSize();

            m_moving.reset();

            m_moving.setWindow(m_window = window, TimeUnit.MILLISECONDS);

            m_moving.add(last);
        }
    }

    public long getAverageWindow()
    {
        return m_window;
    }

    private final static int rup(final int n, final int m)
    {
        return (n % m) + n;
    }

    public int getAverageBufferingSize()
    {
        return rup((int) Math.round(Math.min(Math.max(m_moving.getAverage(), MIN_BUFF_SZ), MAX_BUFF_SZ)), RND_BUFF_SZ);
    }

    @Override
    public String toJSONString(final Map<String, Object> target) throws Exception
    {
        try (GsonEscapedStringBuilderWriter writer = new GsonEscapedStringBuilderWriter(getAverageBufferingSize()))
        {
            m_format.toJson(target, writer);

            m_moving.add(writer.capacity());

            return writer.toString();
        }
    }

    protected static class GsonEscapedStringBuilderWriter extends Writer
    {
        private final int           m_maxsize;

        private final StringBuilder m_builder;

        public GsonEscapedStringBuilderWriter(final int maxsize)
        {
            this(new StringBuilder(maxsize), maxsize);
        }

        protected GsonEscapedStringBuilderWriter(final StringBuilder builder, final int maxsize)
        {
            super(Objects.requireNonNull(builder));

            m_maxsize = maxsize;

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
            if ((null != chr) && (chr.length > 0) && (len > 0))
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
                final char[] chr = str.toCharArray();

                write(chr, 0, chr.length);
            }
        }

        @Override
        public void write(final String str, final int off, final int len) throws IOException
        {
            if (null != str)
            {
                write(str.substring(off, off + len));
            }
        }

        @Override
        public GsonEscapedStringBuilderWriter append(final CharSequence chs) throws IOException
        {
            if (null != chs)
            {
                write(chs.toString());
            }
            return this;
        }

        @Override
        public GsonEscapedStringBuilderWriter append(final CharSequence chs, final int beg, final int end) throws IOException
        {
            if (null != chs)
            {
                write(chs.subSequence(beg, end).toString());
            }
            return this;
        }

        @Override
        public GsonEscapedStringBuilderWriter append(final char c) throws IOException
        {
            escape(c);

            return this;
        }

        public int capacity()
        {
            return m_builder.capacity();
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
            flush();

            clear();
        }

        public GsonEscapedStringBuilderWriter clear()
        {
            m_builder.ensureCapacity(m_maxsize);

            m_builder.setLength(0);

            return this;
        }
    }
}
