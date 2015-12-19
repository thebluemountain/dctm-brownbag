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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

/**
 * A collection of stores.
 *
 * <p>given the properties of stores, it provides an access by id and by
 * (related root's) name</p>
 */
public class Stores
{
    /**
     * The method that creates stores given the supplied collection.
     *
     * <p>It ensures each store has unique name and id</p>
     * @param stores carries individual stores
     * @return the matching stores
     */
    public static Stores create (ImmutableSet <Store> stores)
    {
        Set <String> ids = Sets.newHashSet ();
        Set <String> names = Sets.newHashSet ();
        for (Store store : stores)
        {
            Preconditions.checkArgument (
                ids.add (store.id),
                "duplicate store id found for store %s", store);
            Preconditions.checkArgument (
                names.add (store.name),
                "duplicate store name found for store %s", store);
        }
        return new Stores (stores);
    }

    /**
     * The class that acts as function to return the name of a store
     * given it's id
     */
    private class NameOf implements Function <String, String>
    {
        @Override
        public String apply (String id)
        {
            Store store = this.ids.get (id);
            Preconditions.checkNotNull (
                store, "there is no store matching id %s", id);
            return store.name;
        }
        private NameOf ()
        {
            this.ids = Stores.this.byids ();
        }
        private final Map <String, Store> ids;
    }

    /**
     * The class that acts as function to return the id of a store
     * given it's name
     */
    private class IdOf implements Function <String, String>
    {
        @Override
        public String apply (String name)
        {
            Store store = this.names.get (name);
            Preconditions.checkNotNull (
                store, "there is no store matching name %s", name);
            return store.id;
        }
        private IdOf ()
        {
            this.names = Stores.this.bynames ();
        }
        private final Map <String, Store> names;
    }

    /**
     * All used stores
     */
    public final ImmutableSet <Store> all;

    /**
     * @return the stores identified by related identifier
     */
    public ImmutableMap <String, Store> byids ()
    {
        return Maps.uniqueIndex (this.all, Store.ID);
    }

    /**
     * @return the stores identified by related name
     */
    public ImmutableMap <String, Store> bynames ()
    {
        return Maps.uniqueIndex (this.all, Store.NAME);
    }

    /**
     * @return the function which, given a store id, returns it's name
     */
    public Function <String, String> nameOf () { return new NameOf (); }

    /**
     * @return the function which, given a store name, returns it's id
     */
    public Function <String, String> idOf () { return new IdOf (); }

    @Override
    public int hashCode () { return Objects.hashCode (this.all); }

    @Override
    public boolean equals (Object obj)
    {
        if (this == obj) return true;
        else if (obj instanceof Stores)
        {
            Stores other = (Stores) obj;
            return this.all.equals (other.all);
        }
        return false;
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder ().append ('[');
        Joiner.on (", ").appendTo (sb, this.all);
        return sb.append (']').toString ();
    }

    private Stores (ImmutableSet <Store> all)
    {
        this.all = all;
    }
}
