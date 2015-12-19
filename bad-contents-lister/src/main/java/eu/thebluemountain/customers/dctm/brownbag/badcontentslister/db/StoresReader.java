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

import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;

import eu.thebluemountain.customers.dctm.brownbag.badcontentslister
    .CloseableIterator;
import eu.thebluemountain.customers.dctm.brownbag.badcontentslister.Store;
import eu.thebluemountain.customers.dctm.brownbag.badcontentslister.Stores;

/**
 * The class that reads file store information of use for the check.
 */
public final class StoresReader
{
    private static final String SQL = "SELECT " +
        " f.r_object_id, f.root, f.use_extensions, " +
        " l.file_system_path " +
        "FROM dm_filestore_s f " +
        " INNER JOIN dm_location_sv l ON (l.object_name = f.root)";

    /**
     * The class that reads a row to build a store.
     */
    private static final DBIO.Reader <Store> STOREREADER =
        new DBIO.Reader <Store> ()
    {
        @Override
        public Store read (ResultSet rs) throws SQLException
        {
            String id = rs.getString (1);
            String name = rs.getString (2);
            boolean extension = rs.getBoolean (3);
            String path = rs.getString (4);
            return Store.create (id, name, path, extension);
        }
    };

    /**
     * The function that returns the stores given a database connection.
     */
    public static final Function <JDBCConnection, Stores> STORESREADER =
        new Function <JDBCConnection, Stores> ()
    {
        @Override
        public Stores apply (JDBCConnection jdbc)
        {
            try (CloseableIterator<Store> it =
                 DBIO.createIterator (jdbc, SQL, STOREREADER))
            {
                ImmutableSet.Builder <Store> builder = ImmutableSet.builder ();
                while (it.hasNext ())
                {
                    Store store = it.next ();
                    builder.add (store);
                }
                return Stores.create (builder.build ());
            }
        }
    };

    private StoresReader () { super(); }
}
