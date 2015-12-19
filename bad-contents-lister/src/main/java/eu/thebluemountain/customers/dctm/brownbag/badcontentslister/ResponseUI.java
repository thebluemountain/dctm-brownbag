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

import com.google.common.base.Preconditions;

/**
 * The class that is responsible for displaying something to let
 * user know the program is not down !
 */
final class ResponseUI
{
    /**
     * The method that creates a new ui response
     * @param increment
     * @param count
     * @return
     */
    static ResponseUI create (int increment, int count)
    {
        Preconditions.checkArgument (0 < increment);
        Preconditions.checkArgument (0 < count);
        Preconditions.checkArgument (100 >= count);
        return new ResponseUI (increment, count);
    }

    void onResponse (Checks.Result result)
    {
        this.count++;
        if (Checks.Code.OK != result.code)
        {
            this.errors++;
        }
        if (this.count == this.max)
        {
            doReport ();
            this.count = 0;
        }
        else if (0 == (this.count % this.increment))
        {
            System.out.print ('.');
        }

    }
    void finish () { doReport (); }

    private void doReport ()
    {
        final long elapsed = System.nanoTime () - this.start;
        if (0 == this.count)
        {
            this.start = System.nanoTime ();
            return;
        }

        final long avg = elapsed / this.count;
        System.out.println ("step: {count: " + this.count +
            ", elapsed: " + NanoTime.humanString (elapsed) +
            ", avg: " + NanoTime.humanString (avg) +
            ", total errors: " + this.errors +
            "}");
        System.out.flush ();
        this.count = 0;
        this.start = System.nanoTime ();
    }
    private ResponseUI (int increment, int count)
    {
        this.increment = increment;
        this.max = increment * count;
        this.errors = 0;
        this.start = System.nanoTime ();
    }
    private int errors;
    // the start is reset to current time after each report
    private long start;
    private int count; // reset to 0 after each report
    private final int increment;
    private final int max; // the max number of notifications per line
}
