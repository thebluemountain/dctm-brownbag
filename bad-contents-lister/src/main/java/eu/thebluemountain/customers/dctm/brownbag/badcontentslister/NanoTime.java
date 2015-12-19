/*
 * Copyright (C) 2013 thebluemountain@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.thebluemountain.customers.dctm.brownbag.badcontentslister;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import java.util.concurrent.TimeUnit;

/**
 * The class that displays elapsed time expressed in nano-seconds.
 *
 * <p>It is copied from the guava's {@code Stopwatch} class</p>
 *
 */
public final class NanoTime
{
    private static TimeUnit chooseUnit (long nanos)
    {
        if (TimeUnit.SECONDS.convert (nanos, TimeUnit.NANOSECONDS) > 0)
        {
            return TimeUnit.SECONDS;
        }
        if (TimeUnit.MILLISECONDS.convert (nanos, TimeUnit.NANOSECONDS) > 0)
        {
            return TimeUnit.MILLISECONDS;
        }
        if (TimeUnit.MICROSECONDS.convert (nanos, TimeUnit.NANOSECONDS) > 0)
        {
            return TimeUnit.MICROSECONDS;
        }
        return TimeUnit.NANOSECONDS;
    }

    private static String abbreviate (TimeUnit unit)
    {
        
        switch (unit)
        {
            case NANOSECONDS:
                return "ns";
            case MICROSECONDS:
                return "\u03bcs"; // μs
            case MILLISECONDS:
                return "ms";
            case SECONDS:
                return "s";
            case MINUTES:
                return "min";
            case HOURS:
                return "h";
            case DAYS:
                return "d";
            default:
                throw new AssertionError();
        }
    }

    /**
     * Returns a string representation of the current elapsed time, choosing an
     * appropriate unit and using the specified number of significant figures.
     * For example, at the instant when {@code elapsedTime(NANOSECONDS)} would
     * return {1234567}, {@code toString(4)} returns {@code "1.235 ms"}.
     * @param  nanos carries the number of nano-seconds
     * @param significantDigits carries the number of digits after comma to
     *                          display
     * @return the matching display value
     */
    public static String toString (long nanos, int significantDigits)
    {
        TimeUnit unit = chooseUnit (nanos);
        double value = (double) nanos / TimeUnit.NANOSECONDS.convert (1, unit);

        // Too bad this functionality is not exposed as a regular method call
        return String.format (
            "%." + significantDigits + "f%s", value, abbreviate (unit));
    }

    private static final ImmutableSet <TimeUnit> SMALLS =
        ImmutableSet.of (TimeUnit.SECONDS, TimeUnit.MINUTES);

    /**
     * The method that displays a human-readable form for elapsed time.
     *
     * <p>If the elapsed time exceeds seconds, the representation will display in
     * day, hour and minutes</p>
     * @param nanos carries the number of nano-seconds
     * @return the human readable elapsed time
     */
    public static String humanString (long nanos)
    {
        StringBuilder sb = new StringBuilder ();
        long days = TimeUnit.DAYS.convert (nanos, TimeUnit.NANOSECONDS);
        Optional <TimeUnit> top = Optional.absent ();
        if (0L < days)
        {
            sb.append (days).append (abbreviate (TimeUnit.DAYS));
            nanos -= TimeUnit.NANOSECONDS.convert (days, TimeUnit.DAYS);
            top = Optional.of (TimeUnit.DAYS);
        }
        long hours = TimeUnit.HOURS.convert (nanos, TimeUnit.NANOSECONDS);
        if (0L < hours)
        {
            if (top.isPresent ())
            {
                sb.append (' ');
            }
            else
            {
                top = Optional.of (TimeUnit.HOURS);
            }
            sb.append (hours).append (abbreviate (TimeUnit.HOURS));
            nanos -= TimeUnit.NANOSECONDS.convert (hours, TimeUnit.HOURS);
        }
        long mins = TimeUnit.MINUTES.convert (nanos, TimeUnit.NANOSECONDS);
        if (0L < mins)
        {
            if (top.isPresent ())
            {
                sb.append (' ');
            }
            else
            {
                top = Optional.of (TimeUnit.MINUTES);
            }
            sb.append (mins).append (abbreviate (TimeUnit.MINUTES));
            nanos -= TimeUnit.NANOSECONDS.convert (mins, TimeUnit.MINUTES);
        }
        long secs = TimeUnit.SECONDS.convert (nanos, TimeUnit.NANOSECONDS);
        if (0L < secs)
        {
            if (top.isPresent ())
            {
                sb.append (' ');
            }
            else
            {
                top = Optional.of (TimeUnit.SECONDS);
            }
            sb.append (secs).append (abbreviate (TimeUnit.SECONDS));
            nanos -= TimeUnit.NANOSECONDS.convert (secs, TimeUnit.SECONDS);
        }
        if ((! top.isPresent ()) || (SMALLS.contains (top.get ())))
        {
            long ms = TimeUnit.MILLISECONDS.convert (
                nanos, TimeUnit.NANOSECONDS);
            if (0L < ms)
            {
                if (top.isPresent ())
                {
                    sb.append (' ');
                }
                else
                {
                    top = Optional.of (TimeUnit.MILLISECONDS);
                }
                sb.append (ms).append (abbreviate (TimeUnit.MILLISECONDS));
                nanos -=
                    TimeUnit.NANOSECONDS.convert (secs, TimeUnit.MILLISECONDS);
            }
            if (! top.isPresent ())
            {
                long mis = TimeUnit.MICROSECONDS.convert (
                    nanos, TimeUnit.NANOSECONDS);
                if (0L < mis)
                {
                    sb.append (mis).append (
                        abbreviate (TimeUnit.MICROSECONDS));
                }
                else
                {
                    sb.append (nanos).append (
                        abbreviate (TimeUnit.NANOSECONDS));
                }
            }
        }
        return sb.toString ();
    }

    private NanoTime () { super (); }
}