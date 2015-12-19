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

package eu.thebluemountain.customers.dctm.brownbag.badcontentslister.config.parser;

import com.google.common.base.Optional;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * The class provides some method to perform common operation when reading
 * XML data
 */
public class XMLParsers
{
    /**
     * The method returns the attribute value if any
     * @param in provides access to the XML data
     * @param name identifies the attribute to get value of
     * @return the matching value if any
     */
    public static Optional<String> getAttribute (
        XMLStreamReader in, String name)
    {
        int size = in.getAttributeCount ();
        for (int index = 0; index < size; index++)
        {
            if (in.getAttributeLocalName (index).equals (name))
            {
                return Optional.of (in.getAttributeValue (index));
            }
        }
        return Optional.absent ();
    }

    /**
     * The method returns the trimmed value for an XML element
     * @param in provides access to the XML data
     * @return the matching trimmed text
     * @throws XMLStreamException can be thrown while reading the
     * XML data
     */
    public static String getTrimmedString (XMLStreamReader in)
        throws XMLStreamException
    {
        return in.getElementText ().trim ();
    }

    /**
     * The method that moves the reader until it exits current element
     * @param in provides access to the XML data
     * @throws XMLStreamException can be thrown while skipping
     */
    public static void endElement (XMLStreamReader in)
        throws XMLStreamException
    {
        int depth = 0;
        while (in.hasNext ())
        {
            int event = in.next ();
            if (XMLStreamConstants.START_ELEMENT == event)
            {
                depth++;
            }
            else if (XMLStreamConstants.END_ELEMENT == event)
            {
                if (0 == depth)
                {
                    return;
                }
                depth--;
            }
        }
        throw new IllegalStateException ("unexpected end of events");
    }
}
