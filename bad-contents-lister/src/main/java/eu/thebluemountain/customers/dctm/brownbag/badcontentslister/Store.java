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

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;

/**
 * The representation of a documentum store.
 *
 * <p>Both id and name identifies a store. The name is retrieved from the
 * root attribute of matching file store while the id matches the actual
 * file store's identifier.<br>
 * A store provides 2 information of interest:
 * <ul>
 * <li>a path that matches a local directory</li>
 * <li>a flag that indicates whether it uses related format's extension when
 * storing contents</li>
 * </ul>
 */
public final class Store
{
    /**
     * The method that returns a new store
     * @param id carries the store's identifier
     * @param name carries the unique store's name
     * @param path carries the unique store's path
     * @param extension indicates whether the store contains files with their
     *                  format extension
     * @return the matching store
     */
    public static Store create (
        String id, String name, String path, boolean extension)
    {
        Preconditions.checkNotNull (id);
        Preconditions.checkNotNull (name);
        Preconditions.checkNotNull (path);
        // we recompute the path to carry the docbase identifier on
        // 0-left-padded 8 length.
        final String dbid = Strings.padStart (id.substring (2, 8), 8, '0');
        path = path + "/" + dbid;
        return new Store (id, name, path, extension);
    }

    /**
     * The function that returns the id of a store
     */
    public static final Function<Store, String> ID =
        new Function<Store, String> ()
        {
            @Override
            public String apply (Store store) { return store.id; }
        };

    /**
     * The function that returns the name of a store
     */
    public static final Function <Store, String> NAME =
        new Function<Store, String> ()
        {
            @Override
            public String apply (Store store) { return store.name; }
        };

    /**
     * The predicate that accepts store with extensions
     */
    public static final Predicate<Store> EXTENSION = new Predicate<Store> ()
    {
        @Override
        public boolean apply (Store store) { return store.extension; }
    };

    /**
     * The store identifier
     */
    public final String id;

    /**
     * The store's name
     */
    public final String name;

    /**
     * The store local file path
     */
    public final String path;

    /**
     * Indicates the store carries file extensions
     */
    public final boolean extension;

    @Override
    public int hashCode ()
    {
        return Objects.hashCode (this.id, this.name, this.path);
    }

    @Override
    public boolean equals (Object o)
    {
        if (this == o) return true;
        else if (o instanceof Store)
        {
            Store other = (Store) o;
            return ((this.extension == other.extension) &&
                (this.id.equals (other.id)) &&
                (this.name.equals (other.name)) &&
                (this.path.equals (other.path)));
        }
        return false;
    }

    @Override
    public String toString ()
    {
        return new StringBuilder ().
            append ("{\"id\": \"").append (this.id).
            append ("\", \"name\": \"").append (this.name).
            append ("\", \"path\": \"").append (this.path).
            append ("\", \"extension\": ").append (this.extension).
            append ('}').toString ();
    }

    private Store (String id, String name, String path, boolean extension)
    {
        this.id  = id;
        this.name = name;
        this.path = path;
        this.extension = extension;
    }
}
