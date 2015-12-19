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

package eu.thebluemountain.customers.dctm.brownbag.badcontentslister.config.parser;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * The interface defines the requirements for classes that parse an
 * XML stream into a T instance.
 */
public interface XMLParser<T>
{
    /**
     * The method that parses the supplied reader to instantiate a T
     * @param in provides access to XML stream of events and data
     * @return the matching T
     * @throws XMLStreamException can be thrown while accessing the stream
     */
    T parse (XMLStreamReader in) throws XMLStreamException;
}
