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

package eu.thebluemountain.customers.dctm.brownbag.badcontentslister.db;

import eu.thebluemountain.customers.dctm.brownbag.badcontentslister.Store;
import eu.thebluemountain.customers.dctm.brownbag.badcontentslister.CloseableIterator;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import eu.thebluemountain.customers.dctm.brownbag.badcontentslister.Content;
import eu.thebluemountain.customers.dctm.brownbag.badcontentslister.DecoratedContent;
import eu.thebluemountain.customers.dctm.brownbag.badcontentslister.ExtensionResolver;
import eu.thebluemountain.customers.dctm.brownbag.badcontentslister.Parent;
import eu.thebluemountain.customers.dctm.brownbag.badcontentslister.Stores;

/**
 * The class provides the mean to read data into decorated content.
 */
public final class DCReader
{
    /**
     * The method that retrieves contents and related parent's meta data.
     *
     * <p>Currently, parent's metadata consists in:
     * <ul>
     *  <li>r.parent_id: r_object_id</li>
     *  <li>d.object_name: the object's name</li>
     *  <li>d.r_object_type: the object's type name</li>
     *  <li>d.i_has_folder: indicate if it's current if greater than 0</li>
     * </ul>
     * </p>
     */
    private static final String SQL = "SELECT " +
        " s.storage_id, s.data_ticket, r.parent_id, s.full_format, " +
        " r.page, s.rendition, s.content_size, s.set_time, " +
        " d.object_name, d.r_object_type, d.i_has_folder " +
        "FROM dmr_content_s s " +
        " INNER JOIN dmr_content_r r ON (r.r_object_id = s.r_object_id) " +
        " INNER JOIN dm_sysobject_s d ON (r.parent_id = d.r_object_id) " +
        "WHERE s.storage_id != '0000000000000000' " +
        "ORDER BY s.storage_id, s.data_ticket";

    /**
     * The method returns the function which, given a format's name,
     * returns the format's extension.
     * @param jdbc provides access to the source
     * @return the function which, given a format name, returns the matching
     * extension if any.
     */
    private static Function<String, String>
        makeExtensions (JDBCConnection jdbc)
    {
        String sql = "SELECT name, dos_extension " +
            "FROM dm_format_s WHERE " +
            "(dos_extension IS NOT NULL AND dos_extension <> ' ')";
        try
        {
            Statement stmt = jdbc.connection.createStatement (
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            try
            {
                ResultSet rs = stmt.executeQuery (sql);
                try
                {
                    ImmutableMap.Builder <String, String> builder =
                        ImmutableMap.builder ();
                    while (rs.next ())
                    {
                        String name = rs.getString (1);
                        String ext = "." + rs.getString (2);
                        builder.put (name, ext);
                    }
                    return Functions.forMap (builder.build (), null);
                }
                finally
                {
                    rs.close ();
                }
            }
            finally
            {
                stmt.close ();
            }
        }
        catch (SQLException e)
        {
            throw new IllegalStateException (e);
        }
    }

    /**
     * The class that returns decorated contents
     */
    private static final class DCReaderImpl
        implements DBIO.Reader <DecoratedContent>
    {
        @Override
        public DecoratedContent read (ResultSet rs) throws SQLException
        {
            // the content ...
            final String store = rs.getString (1);
            final int ticket = rs.getInt (2);
            final String id = rs.getString (3);
            final String format = rs.getString (4);
            final int page = rs.getInt (5);
            final boolean rendition = (0 < rs.getInt (6));
            final int size = rs.getInt (7);
            final Timestamp ts = rs.getTimestamp (8);
            final DateTime dt =
                new DateTime (ts.getTime (), DateTimeZone.UTC);
            // figure whether we need the extension ?
            final Optional <String> extension =
                this.extension.resolve (store, format);
            final Content content = Content.create (
                store, ticket, id, rendition,
                format, page, extension, size, dt);

            // now, the parent
            final String name = rs.getString (9);
            final String type = rs.getString (10).intern ();
            final boolean current = (0 < rs.getInt (11));
            final Parent parent = Parent.create (id, name, type, current);
            // OK, that's it
            return DecoratedContent.create (content, parent);
        }
        private DCReaderImpl (ExtensionResolver extension)
        {
            this.extension = extension;
        }
        private final ExtensionResolver extension;
    }

    /**
     * The method that builds the iterator to examine all contents and related
     * parent meta data.
     * @param jdbc provides access to the database
     * @param stores carries the stores
     * @return the matching iterator
     */
    public static CloseableIterator<DecoratedContent> reader (
        JDBCConnection jdbc, Stores stores)
    {
        // manages the extensions: it will be added to content only
        // if from store with extension and a format with extension
        final ImmutableSet<String> accept = ImmutableSet.copyOf (
            Iterables.transform (
                Iterables.filter (stores.all, Store.EXTENSION), Store.ID));
        final ExtensionResolver extension =
            ExtensionResolver.create (accept, makeExtensions (jdbc));
        final DBIO.Reader <DecoratedContent> convert =
            new DCReaderImpl (extension);
        return DBIO.createIterator (jdbc, SQL, convert);
    }

    private DCReader () { super(); }
}
