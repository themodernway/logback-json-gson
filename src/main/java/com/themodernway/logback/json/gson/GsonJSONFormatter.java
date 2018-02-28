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
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.themodernway.logback.json.core.IJSONCommon;
import com.themodernway.logback.json.core.IJSONFormatter;
import com.themodernway.logback.json.core.JSONFormattingException;

import net.dongliu.gson.GsonJava8TypeAdapterFactory;

public class GsonJSONFormatter implements IJSONFormatter, IJSONCommon
{
    private static final int              RND_BUFF_SZ = 16;

    private static final int              MID_BUFF_SZ = 4096;

    private static final int              MIN_BUFF_SZ = MID_BUFF_SZ / 4;

    private static final int              MAX_BUFF_SZ = MID_BUFF_SZ * 4;

    private static final long             MIN_WINDOWS = 1L;

    private static final long             MID_WINDOWS = 60000L;

    private static final long             MAX_WINDOWS = 3600000L;

    private Gson                          m_format;

    private long                          m_window;

    private boolean                       m_pretty;

    private final TimeWindowMovingAverage m_moving;

    private static final GsonBuilder makeGsonBuilder()
    {
        return new GsonBuilder().registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory()).disableHtmlEscaping().serializeNulls().serializeSpecialFloatingPointValues().registerTypeAdapter(Date.class, (JsonDeserializer<Date>) (json, type, ctxt) -> new Date(json.getAsJsonPrimitive().getAsLong())).registerTypeAdapter(Date.class, (JsonSerializer<Date>) (date, type, ctxt) -> new JsonPrimitive(date.getTime()));
    }

    public GsonJSONFormatter()
    {
        m_pretty = false;

        m_window = MID_WINDOWS;

        m_format = makeGsonBuilder().create();

        m_moving = new TimeWindowMovingAverage(m_window, TimeUnit.MILLISECONDS).add(MID_BUFF_SZ);
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
            m_pretty = pretty;

            if (m_pretty)
            {
                m_format = makeGsonBuilder().setPrettyPrinting().create();
            }
            else
            {
                m_format = makeGsonBuilder().create();
            }
        }
    }

    public void setAverageWindow(long window)
    {
        window = Math.min(Math.max(window, MIN_WINDOWS), MAX_WINDOWS);

        if (window != getAverageWindow())
        {
            m_window = window;

            final int last = getAverageBufferingSize();

            m_moving.reset().setWindow(m_window, TimeUnit.MILLISECONDS).add(last);
        }
    }

    public long getAverageWindow()
    {
        return m_window;
    }

    private static final int rup(final int n, final int m)
    {
        return (n % m) + n;
    }

    public int getAverageBufferingSize()
    {
        return rup((int) Math.round(Math.min(Math.max(m_moving.getAverage(), MIN_BUFF_SZ), MAX_BUFF_SZ)), RND_BUFF_SZ);
    }

    @Override
    public String toJSONString(final Map<String, Object> target) throws JSONFormattingException
    {
        try (GsonEscapedStringBuilderWriter writer = new GsonEscapedStringBuilderWriter(getAverageBufferingSize()))
        {
            m_format.toJson(target, writer);

            m_moving.add(writer.capacity());

            return writer.toString();
        }
        catch (final IOException e)
        {
            throw new JSONFormattingException(e);
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

        protected void escape(final char c)
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
        public void write(final char[] chr, final int off, final int len) throws IOException
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
            // do nothing for flush
        }

        @Override
        public void close() throws IOException
        {
            // do nothing for close
        }

        public GsonEscapedStringBuilderWriter clear()
        {
            m_builder.ensureCapacity(m_maxsize);

            m_builder.setLength(0);

            return this;
        }
    }
}
