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

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import com.google.common.primitives.UnsignedInteger;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.File;
import java.util.Comparator;

/**
 * The class that does represent a documentum content object
 *
 * <p>Being comparable, the class implements comparison by comparing,
 * in that order:
 * <ot>
 *  <li>rendition: {@code false} value is before, ensuring primary content
 *  to appear first</li>
 *  <li>format: used for renditions, allows for grouping then by format. In
 *  case of primary contents, it does not matter as all primary contents then
 *  share same format</li>
 *  <li>page: in ascending order. page 1 is less that page 2, allowing for
 *  sending first page before next one</li>
 *  <li>store: (of use to ensure complete ordering between content)</li>
 *  <li>ticket: (of use to ensure complete ordering between content)</li>
 * </ot>
 * It does provide static utilities to convert as a relative path
 * </p>
 */
public final class Content implements Comparable <Content>
{
    private static final class OptComparator <T extends Comparable <T>>
        implements Comparator <Optional <T>>
    {
        @Override
        public int compare (Optional<T> o1, Optional<T> o2)
        {
            if (! o1.isPresent ())
            {
                return o2.isPresent () ? 1 : 0;
            }
            else if (! o2.isPresent ())
            {
                return -1;
            }
            return o1.get ().compareTo (o2.get ());
        }
    }

    private static final Comparator <Optional <String>> OPTCOMP =
        new OptComparator <> ();

    /**
     * The method that returns the content matching supplied arguments.
     *
     * @param store carries the related store's identifier
     * @param ticket holds the data ticket for the content
     * @param parent carries the parent object identifier
     * @param rendition indicates whether content matches a rendition
     * @param format carries the related format's name
     * @param page is the page index of the content for the format
     * @param extension carries the extension ... if any
     * @param size carries the expected size of contents
     * @param modified holds the modified date for the content
     * @return the matching content object
     */
    public static Content create (
        String store, int ticket, String parent, boolean rendition,
        String format, int page, Optional <String> extension, int size,
        DateTime modified)
    {
        Preconditions.checkNotNull (store);
        Preconditions.checkNotNull (parent);
        Preconditions.checkNotNull (format);
        Preconditions.checkArgument (0 <= page);
        Preconditions.checkNotNull (extension);
        Preconditions.checkArgument (0 <= size);
        Preconditions.checkNotNull (modified);
        modified = modified.withZone (DateTimeZone.UTC);
        return new Content (
            store, ticket, parent, rendition,
            format, page, extension, size, modified);
    }

    /**
     * The method appends the path representation
     * @param sb is the string builder to append to
     * @param ticket is the actual ticket to represent as path
     * @param sep is the path separator to use
     * @return the supplied buffer
     */
    public static StringBuilder pathOf (
        StringBuilder sb, int ticket, char sep)
    {
        String path = Strings.padStart (
            UnsignedInteger.fromIntBits (ticket).toString (16), 8, '0');
        return sb.append (path.charAt (0)).
            append (path.charAt (1)).
            append (sep).
            append (path.charAt (2)).
            append (path.charAt (3)).
            append (sep).
            append (path.charAt (4)).
            append (path.charAt (5)).
            append (sep).
            append (path.charAt (6)).
            append (path.charAt (7));
    }

    /**
     * The method that computes the path of the content, relative to its
     * store.
     *
     * <p>It forwards to the {@link #makeRelative(Content, char)} using the
     * file's {@code pathSeparatorChar} as separator</p>
     *
     * @param content is the content
     * @return the relative path
     */
    public static String makeRelative (Content content)
    {
        return makeRelative (content, File.separatorChar);
    }

    /**
     * The method that computes the path of the content, relative to its
     * store.
     *
     * @param content is the content
     * @param sep is the path separator to use
     * @return the relative path
     */
    public static String makeRelative (Content content, char sep)
    {
        return makeRelative (
            new StringBuilder (48), content, sep).toString ();
    }

    /**
     * The method that computes the path of the content, relative to its
     * store.
     *
     * @param sb is builder to append to
     * @param content is the content
     * @param sep is the path separator to use
     * @return the supplied builder
     */
    public static StringBuilder makeRelative (
        StringBuilder sb, Content content, char sep)
    {
        return pathOf (sb, content.ticket, sep).
            append (content.extension.or (""));
    }

    /**
     * The method that returns a string that uniquely identifies a content
     *
     * <p>The method builds a key identifying contents that consists in:
     * ${content.store}/${content.ticket}
     * @param content is the content to build key for
     * @return the matching key
     */
    public static String keyOf (Content content)
    {
        return content.store + "/" + content.ticket;
    }

    /**
     * The related store's identifier
     */
    public final String store;

    /**
     * The content identifier ticket in the store
     */
    public final int ticket;

    /**
     * The identifier of the parent object
     */
    public final String parent;

    /**
     * That flag that indicates whether it matches a rendition
     */
    public final boolean rendition;

    /**
     * The related format's name
     */
    public final String format;

    /**
     * The page index in the format
     */
    public final int page;

    /**
     * The file extension
     */
    public final Optional <String> extension;

    /**
     * The content's size (in bytes)
     */
    public final int size;

    /**
     * The modification date (using UTC)
     */
    public final DateTime modified;

    @Override
    public int compareTo (Content o)
    {
        if (this == o) return 0;
        // comparison by ... rendition / format / page / store / ticket
        // ... size / modified / extension
        return ComparisonChain.start ().
            // is false: it's primary and it comes first !
                compareFalseFirst (this.rendition, o.rendition).
            // ok: format ?
                compare (this.format, o.format).
            // ok: page ?
                compare (this.page, o.page).
            // ok: store ?
                compare (this.store, o.store).
            // ok: ticket !
                compare (this.ticket, o.ticket).
            // ok: now, just to ensure 2 contents match is they are equal
            // size / extension / modified
                compare (this.size, o.size).
                compare (this.modified, o.modified).
                compare (this.extension, o.extension, OPTCOMP).
                result ();
    }

    @Override
    public int hashCode ()
    {
        return Objects.hashCode (
            this.store, this.ticket, this.parent, this.rendition,
            this.format, this.extension, this.size, this.modified);
    }

    @Override
    public boolean equals (Object o)
    {
        if (this == o) return true;
        else if (o instanceof Content)
        {
            Content other = (Content) o;
            return ((this.ticket == other.ticket)
                && (this.rendition == other.rendition)
                && (this.size == other.size)
                && (this.store.equals (other.store))
                && (this.parent.equals (other.parent))
                && (this.format.equals (other.format))
                && (this.extension.equals (other.extension))
                && (this.modified.equals (other.modified)));
        }
        return false;
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder ().
            append ("{\"parent\": \"").append (this.parent).
            append ("\", \"store\": \"").append (this.store).
            append ("\", \"ticket\": ").append (this.ticket).
            append (", \"rendition\": ").append (this.rendition).
            append (", \"format\": \"").append (this.format).
            append ("\", \"modified\": \"").append (this.modified).
            append ("\", \"size\": \"").append (this.size);
        if (this.extension.isPresent ())
        {
            sb.append (", \"extension\": \"").
                append (this.extension.get ()).append ('"');
        }
        return sb.append ('}').toString ();
    }

    private Content (
        String store, int ticket, String parent, boolean rendition,
        String format, int page, Optional <String> extension, int size,
        DateTime modified)
    {
        this.store = store;
        this.ticket = ticket;
        this.parent = parent;
        this.rendition = rendition;
        this.format = format;
        this.page = page;
        this.extension = extension;
        this.size = size;
        this.modified = modified;
    }
}
