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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import eu.thebluemountain.customers.dctm.brownbag.badcontentslister.config
    .JDBCConfig;
import eu.thebluemountain.customers.dctm.brownbag.badcontentslister.config
    .parser.JDBCXMLParser;
import eu.thebluemountain.customers.dctm.brownbag.badcontentslister.db.DCReader;
import eu.thebluemountain.customers.dctm.brownbag.badcontentslister.db.JDBCConnection;
import eu.thebluemountain.customers.dctm.brownbag.badcontentslister.db.StoresReader;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;

import javax.xml.stream.XMLStreamException;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;


import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;

/**
 * The class provides the main entry point to list failing contents
 */
public final class Main
{
    /**
     * The several return codes for the program
     */
    private enum RetCode
    {
        ERR_OK
        , ERR_FILE_NOT_EXISTS
        , ERR_NO_FILE_READ
        , ERR_BAD_CONFIG_FILE
        , ERR_SQL
        , ERR_CANCELLED
        , ERR_OTHER
    }

    private static final class ExitException extends RuntimeException
    {
        /**
         * The method that exits current process with the ordinal of the
         * related return code
         */
        public void exit ()
        {
            System.exit (this.code.ordinal ());
        }

        /**
         * The return code
         */
        public final RetCode code;

        private ExitException (RetCode code) { this.code = code; }
    }

    private static ExitException error (RetCode code, String errmsg)
    {
        System.err.println (errmsg);
        System.err.flush ();
        System.out.println ();
        usage ();
        return new ExitException (Preconditions.checkNotNull (code));
    }

    private static ExitException error (RetCode code, Throwable t)
    {
        t.printStackTrace (System.err);
        System.err.flush ();
        System.out.println ();
        usage ();
        return new ExitException (Preconditions.checkNotNull (code));
    }

    private static void usage ()
    {
        System.out.println (
            "usage: java -jar bad-contents-lister-full.jar " +
                "[--config | -C ${config}] [--help | -H]");
        System.out.println ("where:");
        System.out.println (
            " --config (-C) ${config} is the path to an XML");
        System.out.println (
            "  file carrying information about the database to connect to");
        System.out.println (
            " --help (-H) to display help");
        System.out.flush ();
    }

    private static JDBCConfig config (String filename)
    {
        final File file = new File (filename);
        if (! file.exists ())
        {
            throw (error (RetCode.ERR_FILE_NOT_EXISTS,
                "there is no file " + filename));
        }
        else if (! file.canRead ())
        {
            throw (error (RetCode.ERR_NO_FILE_READ,
                "cannot read file " + filename));
        }

        try (FileInputStream in = new FileInputStream (file))
        {
            return JDBCXMLParser.parse (in, "jdbc");
        }
        catch (XMLStreamException | IOException e)
        {
            throw (error (RetCode.ERR_NO_FILE_READ, e));
        }
        catch (RuntimeException e)
        {
            throw (error (RetCode.ERR_BAD_CONFIG_FILE, e));
        }
    }

    /**
     * The method that reads password from console
     * @param what indicates what to enter password for
     * @param user carries the name of the user to enter password for
     * @return the matching password if any
     */
    private static Optional <String> passwordOf (String what, String user)
    {
        char [] chars = System.console ().readPassword (
            "enter password for user %2 in %1: ", user, what);
        if (null == chars) return Optional.absent ();
        return Optional.of (new String (chars));
    }

    /**
     * The method that creates a new JDBC connection
     * @param config carries information to connect
     * @param password carries the actual password to use
     * @return the matching connection
     * @throws SQLException when attempting to connect to database
     */
    public static JDBCConnection create (
        JDBCConfig config, String password) throws SQLException
    {
        Preconditions.checkNotNull (config, "null config supplied");
        Preconditions.checkNotNull (password, "null password supplied");
        Preconditions.checkNotNull (config.schema, "null schema supplied");
        Connection cnx = DriverManager.getConnection (
            config.url, config.user, password);
        return JDBCConnection.create (cnx, config.schema);
    }

    private static CSVWriter makeLog (String user) throws FileNotFoundException
    {
        DateTime now = DateTime.now (DateTimeZone.UTC);
        String name = user + "-" +
            ISODateTimeFormat.basicDateTimeNoMillis ().print (now) + ".log";
        File file = new File (name).getAbsoluteFile ();
        System.out.println ("logging into " + file.getPath ());
        PrintWriter printer = new PrintWriter (file);
        return CSVWriter.create (printer, '|');
    }
    public static void main (String[] args)
    {
        try
        {
            Map<Command, Optional<String>> cmds = Command.parse (args);
            if ((cmds.containsKey (Command.HELP)) ||
                (! cmds.containsKey (Command.CONFIG)))
            {
                usage ();
                return;
            }
            final JDBCConfig config =
                config (cmds.get (Command.CONFIG).get ());

            String pwd = config.password.orNull ();
            if (null == pwd)
            {
                Optional <String> opt = passwordOf ("database", config.user);
                if (! opt.isPresent ())
                {
                    throw new ExitException (RetCode.ERR_CANCELLED);
                }
                pwd = opt.get ();
            }
            try (JDBCConnection from = create (config, pwd);
                 CSVWriter writer = makeLog (config.user))
            {
                Stopwatch watch = Stopwatch.createStarted ();
                Stores stores = StoresReader.STORESREADER.apply (from);
                System.out.println (
                    "spent " + watch.stop () + " to load stores");
                final Function <DecoratedContent, Checks.Result> checker =
                    Checks.checker (stores);
                final Multiset <Checks.Code> codes = TreeMultiset.create ();
                watch.reset ().start ();
                ResponseUI rui = ResponseUI.create (1024, 64);
                try (CloseableIterator <DecoratedContent> it =
                     DCReader.reader (from, stores))
                {
                    long count = 0L;
                    while (it.hasNext ())
                    {
                        DecoratedContent dc = it.next ();
                        count++;
                        final Checks.Result result = checker.apply (dc);
                        rui.onResponse (result);
                        final Checks.Code code = result.code;
                        codes.add (code);
                        if (code != Checks.Code.OK)
                        {
                            // we've got an error then ....
                            writer.writeError (dc, result);
                        }
                    }
                    rui.finish ();
                    System.out.println (
                        "spent " + watch.stop () +
                        " to read " + count + " d.c.");
                    System.out.println ("stats: " + codes);
                    System.out.println ("bye");
                }
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace (System.err);
            System.err.flush ();
            System.out.println ();
            usage ();
            System.exit (RetCode.ERR_SQL.ordinal ());
        }
        catch (ExitException e)
        {
            e.exit ();
        }
        catch (RuntimeException | IOException e)
        {
            e.printStackTrace (System.err);
            System.err.flush ();
            System.out.println ();
            usage ();
            System.exit (RetCode.ERR_OTHER.ordinal ());
        }
    }
}
