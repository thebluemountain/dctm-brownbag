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
import com.google.common.base.Strings;

/**
 * The carrier for content's parent metadata
 */
public final class Parent
{
    /**
     * The method that does create a new parent
     * @param id carries the system object's identifier
     * @param name is the related object's name
     * @param type is the related object's type
     * @param current indicates whether the parent is the current version
     * @return the matching parent
     */
    public static Parent create (
        String id, String name, String type, boolean current)
    {
        return new Parent (Preconditions.checkNotNull (id, "null id supplied"),
            Strings.nullToEmpty (name),
            Preconditions.checkNotNull (type, "null type supplied"), current);
    }

    /**
     * The object's identifier
     */
    public final String id;

    /**
     * The object's name
     */
    public final String name;

    /**
     * The object's type name
     */
    public final String type;

    /**
     * Whether the object is in current version
     */
    public final boolean current;

    @Override
    public int hashCode ()
    {
        // be minimal here: it assumes a same object carries the same
        // name, type and current flag !
        return this.id.hashCode ();
    }

    @Override
    public boolean equals (Object obj)
    {
        if (this == obj) return true;
        else if (obj instanceof Parent)
        {
            Parent other = (Parent) obj;
            return ((this.current == other.current) &&
                (this.id.equals (other.id)) &&
                (this.name.equals (other.name)) &&
                (this.type.equals (other.type)));
        }
        return false;
    }

    @Override
    public String toString ()
    {
        return "Parent {\"id\": \"" + this.id +
            "\", \"name\": \"" + this.name +
            "\", \"type\": \"" + this.type +
            "\", \"current\": " + this.current + "}";
    }

    private Parent (String id, String name, String type, boolean current)
    {
        this.id = id;
        this.name = name;
        this.type = type;
        this.current = current;
    }
}
