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

import java.util.Objects;

/**
 * The class carries information about a content object
 */
public final class DecoratedContent
{
    public static DecoratedContent create (Content content, Parent parent)
    {
        return new DecoratedContent (
            Preconditions.checkNotNull (content, "null content supplied"),
            Preconditions.checkNotNull (parent, "null parent supplied")
        );
    }

    /**
     * The carries content
     */
    public final Content content;

    /**
     * The related parent
     */
    public final Parent parent;

    @Override
    public int hashCode ()
    {
        return Objects.hash (this.content, this.parent);
    }

    @Override
    public boolean equals (Object obj)
    {
        if (this == obj) return true;
        else if (obj instanceof DecoratedContent)
        {
            DecoratedContent other = (DecoratedContent) obj;
            return ((this.content.equals (other.content)) &&
                (this.parent.equals (other.content)));
        }
        return false;
    }

    @Override
    public String toString ()
    {
        return "DecoratedContent {content: " + this.content +
            ", parent: " + this.parent + "}";
    }

    private DecoratedContent (Content content, Parent parent)
    {
        this.content = content;
        this.parent = parent;
    }
}