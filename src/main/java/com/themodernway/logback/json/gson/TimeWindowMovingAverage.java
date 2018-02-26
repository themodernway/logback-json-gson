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

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public class TimeWindowMovingAverage
{
    private long               m_window;

    private final TimeUnit     m_baseof;

    private final LongSupplier m_ticker;

    private volatile long      m_moment;

    private volatile double    m_moving;

    public TimeWindowMovingAverage(final long window, final TimeUnit unit)
    {
        this(TimeUnit.MILLISECONDS, window, unit, System::currentTimeMillis);
    }

    public TimeWindowMovingAverage(final TimeUnit base, final long window, final TimeUnit unit, final LongSupplier tick)
    {
        m_baseof = Objects.requireNonNull(base);

        m_ticker = Objects.requireNonNull(tick);

        m_window = getUnitOf(validate(window, 1, 1), Objects.requireNonNull(unit));
    }

    public TimeUnit getUnit()
    {
        return m_baseof;
    }

    public long getWindow()
    {
        return m_window;
    }

    public void setWindow(final long window, final TimeUnit unit)
    {
        m_window = getUnitOf(validate(window, 1, 1), Objects.requireNonNull(unit));
    }

    public long getWindow(final TimeUnit unit)
    {
        return getUnitOf(getWindow(), Objects.requireNonNull(unit));
    }

    public double getAverage()
    {
        return m_moving;
    }

    public synchronized TimeWindowMovingAverage reset()
    {
        m_moment = 0;

        m_moving = 0;

        return this;
    }

    public synchronized TimeWindowMovingAverage add(final double sample)
    {
        final long moment = getMoment();

        if (m_moment == 0)
        {
            m_moving = sample;

            m_moment = moment;

            return this;
        }
        final long elapse = moment - m_moment;

        final double wcoeff = Math.exp(-1.0 * ((double) elapse / m_window));

        m_moving = ((1.0 - wcoeff) * sample) + (wcoeff * m_moving);

        m_moment = moment;

        return this;
    }

    public String toPlaces(final int places)
    {
        return String.format("%." + Math.min(Math.max(places, 0), 8) + "f", getAverage());
    }

    @Override
    public String toString()
    {
        return toPlaces(3);
    }

    protected long getUnitOf(final long duration, final TimeUnit unit)
    {
        return Objects.requireNonNull(getUnit()).convert(duration, Objects.requireNonNull(unit));
    }

    protected long validate(final long duration, final long lbounds, final long minimum)
    {
        final long result = Math.max(duration, lbounds);

        if (result < Math.abs(minimum))
        {
            throw new IllegalArgumentException("duration is < " + Math.abs(minimum));
        }
        return result;
    }

    public long getMoment()
    {
        return m_ticker.getAsLong();
    }
}
