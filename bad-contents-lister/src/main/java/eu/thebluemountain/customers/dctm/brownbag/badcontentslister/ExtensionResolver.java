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
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

/**
 * The class that resolves an object's extension given a function that returns
 * a format extension and a collection of stores that accept extensions.
 */
public final class ExtensionResolver
{
    /**
     * The method that creates a new extension resolver
     * @param stores carries the identifiers of stores using extensions
     * @param extensions is the function which, given a format name, returns
     *                   the matching file's extension.
     * @return the matching extension resolver
     */
    public static ExtensionResolver create (
        ImmutableSet <String> stores, Function <String, String> extensions)
    {
        return new ExtensionResolver (
            Preconditions.checkNotNull (stores),
            Preconditions.checkNotNull (extensions));
    }

    /**
     * The method that returns the extension to use for the format in the store
     * @param store carries a storage identifier
     * @param format carries a format's name
     * @return the extension if any
     */
    public Optional<String> resolve (String store, String format)
    {
        if (this.stores.contains (store))
        {
            return Optional.fromNullable (
                Strings.emptyToNull (this.extensions.apply (format)));
        }
        return Optional.absent ();
    }

    private ExtensionResolver (
        ImmutableSet <String> stores, Function <String, String> extensions)
    {
        this.stores = stores;
        this.extensions = extensions;
    }
    private final ImmutableSet <String> stores;
    private final Function <String, String> extensions;
}
