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

package eu.thebluemountain.customers.dctm.brownbag.badcontentslister.config;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * The class that carries configuration about JDBC connection
 */
public final class JDBCConfig
{
    /**
     * The method that creates a new JDBC configuration
     *
     * @param url carries the URL to connect to
     * @param user is the user to connect as
     * @param password is the password to connect with if any
     * @param schema is the default schema to use if any
     * @return the matching configuration
     */
    public static JDBCConfig create (
        String url, String user,
        Optional <String>  password, String schema)
    {
        return new JDBCConfig (
            Preconditions.checkNotNull (url, "null url supplied"),
            Preconditions.checkNotNull (user, "null user supplied"),
            Preconditions.checkNotNull (password, "null password supplied"),
            Preconditions.checkNotNull (schema, "null schema supplied")
        );
    }

    /**
     * The JDBC url to connect to
     */
    public final String url;

    /**
     * The user to connect as
     */
    public final String user;

    /**
     * The password to connect with
     */
    public final Optional <String> password;

    /**
     * The schema or owner to use by default
     */
    public final String schema;

    @Override
    public boolean equals (Object o)
    {
        if (this == o) return true;
        if (o instanceof JDBCConfig)
        {
            JDBCConfig other = (JDBCConfig) o;
            return ((this.url.equals (other.url)) &&
                (this.user.equals (other.user)) &&
                (this.password.equals (other.password)) &&
                (this.schema.equals (other.schema)));
        }
        return false;
    }
    @Override
    public int hashCode ()
    {
        return Objects.hashCode (
            this.url, this.user, this.password, this.schema);
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder ().
            append ("{\"url\": \"").append (this.url).
            append ("\", \"user\": \"").append (this.user);
        if (this.password.isPresent ())
        {
            sb.append ("\", \"password\": \"***\"");
        }
        return sb.append (", \"schema\": \"").
            append (this.schema).append ("\"}").
            toString ();
    }
    private JDBCConfig (
        String url, String user,
        Optional <String> password, String schema)
    {
        this.url = url;
        this.user = user;
        this.password = password;
        this.schema = schema;
    }
}
