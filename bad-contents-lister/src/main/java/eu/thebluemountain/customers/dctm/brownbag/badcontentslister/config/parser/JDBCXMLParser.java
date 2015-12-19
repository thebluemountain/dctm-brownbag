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
import com.google.common.base.Preconditions;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import eu.thebluemountain.customers.dctm.brownbag.badcontentslister.config
    .JDBCConfig;


import java.io.InputStream;

/**
 * The class that provides the mean to parse XML to build a JDBC configuration
 */
public final class JDBCXMLParser
{
    /**
     * The parser that builds a JDBC configuration
     */
    public static final XMLParser<JDBCConfig> JDBC =
        new XMLParser<JDBCConfig> ()
    {
        @Override
        public JDBCConfig parse (XMLStreamReader in) throws
            XMLStreamException
        {
            String url = null;
            String user = null;
            Optional<String> schema = Optional.absent ();
            Optional <String> password = Optional.absent ();
            boolean ended = false;
            while ((! ended) && (in.hasNext ()))
            {
                int event = in.next ();
                if (XMLStreamConstants.START_ELEMENT == event)
                {
                    final QName name = in.getName ();
                    if (name.equals (QName.valueOf ("url")))
                    {
                        url = XMLParsers.getTrimmedString (in);
                    }
                    else if (name.equals (QName.valueOf ("user")))
                    {
                        user = XMLParsers.getTrimmedString (in);
                    }
                    else if (name.equals (QName.valueOf ("password")))
                    {
                        password =
                            Optional.of (XMLParsers.getTrimmedString (in));
                    }
                    else if (name.equals (QName.valueOf ("schema")))
                    {
                        schema =
                            Optional.of (XMLParsers.getTrimmedString (in));
                    }
                    else
                    {
                        // finishes current element
                        XMLParsers.endElement (in);
                    }
                }
                else if (XMLStreamConstants.END_ELEMENT == event)
                {
                    ended = true;
                }
            }
            Preconditions.checkArgument (ended);
            Preconditions.checkArgument (
                null != url, "missing url element");
            Preconditions.checkArgument (
                null != user, "missing user element");
            if (! schema.isPresent ())
            {
                // KISS: just use the case for SQL server
                schema = Optional.of ("dbo");
            }
            return JDBCConfig.create (url, user, password, schema.get ());
        }
    };

    /**
     * The method that parses a stream to returns the matching JDBC
     * configuration
     * @param ins is the input stream that provides access to XML
     *            representation of a JDBC connection
     * @param top is the top level element matching the JDBC configuration
     * @return the matching configuration
     * @throws XMLStreamException can be thrown while reading the XML data
     */
    public static JDBCConfig parse (InputStream ins, String top)
        throws XMLStreamException
    {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader in = factory.createXMLStreamReader (ins);
        // move to first START_ELEMENT
        while ((XMLStreamConstants.START_ELEMENT !=
            in.getEventType ()) && (in.hasNext ()))
        {
            in.next ();
        }
        Preconditions.checkArgument (
            XMLStreamConstants.START_ELEMENT == in.getEventType ());
        String current = in.getName ().getLocalPart ();
        while ((! current.equals (top)) && (in.hasNext ()))
        {
            int event = in.next ();
            if (XMLStreamConstants.START_ELEMENT == event)
            {
                QName name = in.getName ();
                current = name.getLocalPart ();
            }
        }
        Preconditions.checkArgument (
            current.equals (top),
            "there is no element '%s' in supplied XML document", top);
        return JDBC.parse (in);
    }
    private JDBCXMLParser () { super (); }
}
