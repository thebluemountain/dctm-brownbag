/*
 * Copyright (C) 2015 thebluemountain@gmail.com
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

import java.io.Closeable;
import java.io.PrintWriter;

import com.google.common.base.Preconditions;

/**
 * The class that writes a CSV file
 */
final class CSVWriter implements Closeable
{
    static CSVWriter create (PrintWriter printer, char separator)
    {
        Preconditions.checkNotNull (printer);
        return new CSVWriter (printer, separator);
    }
    /**
     * The method that writes the (CSV like) line matching an error
     * @param dc is the content
     * @param result is the result of the check
     */
    public void writeError (DecoratedContent dc, Checks.Result result)
    {
        Preconditions.checkNotNull (dc, "null dc supplied");
        Preconditions.checkNotNull (result, "null result supplied");
        final Checks.Code code = result.code;
        String error;
        switch (code)
        {
            case OK:
                return;
            case NOTFOUND:
                error = "content not found";
                break;
            case EMPTYNOTFOUND:
                error = "content not found but was empty !";
                break;
            case ERROR:
                error = "error accessing the content: " +
                    Checks.Result.SizeError.class.cast (result).
                        e.getMessage ();
                break;
            case BADSIZE:
                error = "bad size (" +
                    Checks.Result.Size.class.cast (result).actual +
                    ") when expecting " + dc.content.size + " bytes";
                break;
            case EMPTY:
                error = "empty size when expecting " +
                    dc.content.size + " bytes";
                break;
            default:
                error = "!!! unhandled Code case";
                break;
        }

        StringBuilder sb = new StringBuilder ().
            append (dc.parent.id).append (this.separator).
            append (dc.parent.name).append (this.separator).
            append (dc.parent.type).append (this.separator).
            append (dc.parent.current).append (this.separator).
            append (dc.content.format).append (this.separator).
            append (dc.content.rendition).append (this.separator).
            append (dc.content.page).append (this.separator).
            append (dc.content.modified).append (this.separator).
            append (dc.content.extension.or ("")).append (this.separator).
            append (dc.content.size).append (this.separator).
            append (dc.content.ticket).append (this.separator).
            append (result.code).append (this.separator).
            append (result.path).append (this.separator).
            append (error);
        this.printer.println (sb.toString ());
        this.printer.flush ();
    }
    @Override
    public void close ()
    {
        this.printer.close ();
    }
    private CSVWriter (PrintWriter printer, char separactor)
    {
        this.printer = printer;
        this.separator = separactor;
    }
    private final char separator;
    private final PrintWriter printer;
}
