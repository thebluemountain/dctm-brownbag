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

package eu.thebluemountain.customers.dctm.brownbag.badcontentslister.db;

import com.google.common.base.Preconditions;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * The class that carries a connection and provides information about
 * default schema/owner
 */
public final class JDBCConnection implements AutoCloseable
{
    /**
     * The method that create a JDBC connection from a connection and a
     * (default) schema.
     *
     * <p>The method resets the auto-commit to {@code false}</p>
     * @param cnx is an (opened) connection
     * @param schema is the default schema
     * @return the matching JDBC connection
     * @throws SQLException can be thrown while resetting auto commit
     */
    public static JDBCConnection create (Connection cnx, String schema)
        throws SQLException
    {
        Preconditions.checkNotNull (cnx, "null connection supplied");
        Preconditions.checkNotNull (schema, "null schema supplied");
        cnx.setAutoCommit (false);
        return new JDBCConnection (cnx, schema);
    }

    @Override
    public void close () throws IllegalStateException
    {
        try
        {
            if (!this.connection.isClosed ())
            {
                this.connection.close ();
            }
        }
        catch (SQLException e)
        {
            throw new IllegalStateException (e);
        }
    }

    /**
     * The connection to the database
     *
     * <p>It is expected to have autocommit disabled.
     * Upon closing the object, the connection is closed as well</p>
     */
    public final Connection connection;

    /**
     * The default schema.
     *
     * <p>When fixing table names's owner, if not present, it is set
     * to the schema</p>
     */
    public final String schema;

    private JDBCConnection (
        Connection connection, String schema)
    {
        this.connection = connection;
        this.schema = schema;
    }
}