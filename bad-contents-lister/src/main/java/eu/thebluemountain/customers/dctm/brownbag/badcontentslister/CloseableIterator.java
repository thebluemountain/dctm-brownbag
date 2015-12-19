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

import java.util.Iterator;

/**
 * Provides access to T instances using iterator but needs to be
 * closed to ensure resources are released.
 */
public interface CloseableIterator <T> extends Iterator <T>, AutoCloseable
{
    /**
     * The method that closes the resources associated to the iterator.
     *
     * @throws IllegalStateException if any error occurs
     */
    @Override
    void close () throws IllegalStateException;
}
