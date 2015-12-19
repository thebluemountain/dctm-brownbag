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

package eu.thebluemountain.customers.dctm.brownbag.badcontentslister.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.NoSuchElementException;

import eu.thebluemountain.customers.dctm.brownbag.badcontentslister
    .CloseableIterator;
import eu.thebluemountain.customers.dctm.brownbag.badcontentslister.NanoTime;

import com.google.common.base.Preconditions;

/**
 * The class provides the mean to read columns from a result set to produce
 * objects
 */
public final class DBIO
{
    /**
     * The interface defines the requirements for classes that returns T
     * instance from a row of data.
     *
     * @param <T> is the type of object to return
     */
    public interface Reader <T>
    {
        /**
         * The method that returns a T instance from supplied result set.
         *
         * @param rs carries actual data
         * @return the matching T instance
         * @throws SQLException can be thrown while accessing the current
         * row of data from the result set
         */
        T read (ResultSet rs) throws SQLException;
    }

    /**
     * The actual class that behaves like an iterator of T and that can
     * be closed ... backed by a SQL's result set.
     *
     * @param <T> is the actual type of value to iterate over
     */
    private static final class CloseableRSIteratorImpl <T>
        implements CloseableIterator<T>
    {
        @Override
        public void close () throws IllegalStateException
        {
            this.hasNext = null;
            try
            {
                this.rs.close ();
                if (null != this.stmt)
                {
                    this.stmt.close ();
                }
            }
            catch (SQLException e)
            {
                //getLogger ().error ("SQLException", e);
                throw new IllegalStateException (e);
            }
        }

        /**
         * The method that indicates whether next T exists or not.
         *
         * @see java.util.Iterator#hasNext()
         * @throws IllegalStateException can be thrown by implementation
         * caused by an SQL Exception
         */
        @Override
        public boolean hasNext () throws IllegalStateException
        {
            if (null == this.hasNext)
            {
                this.hasNext = fetchHasNext ();
            }
            return this.hasNext;
        }

        /**
         * The method that returns the next T object.
         *
         * @see java.util.Iterator#next()
         * @throws NoSuchElementException if the iteration has no more elements
         * @throws IllegalStateException can be thrown when accessing the
         * database
         */
        @Override
        public T next () throws NoSuchElementException, IllegalStateException
        {
            if (null == this.hasNext)
            {
                this.hasNext = fetchHasNext ();
            }
            if (Boolean.FALSE == this.hasNext)
            {
                throw new NoSuchElementException ();
            }
            this.hasNext = null;
            try
            {
                return this.convert.read (this.rs);
            }
            catch (SQLException e)
            {
                throw new IllegalStateException (e);
            }
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#remove()
         */
        @Override
        public void remove ()
        {
            throw new UnsupportedOperationException (
                "cannot remove from RS iterator");
        }

        /**
         * @return whether there is a next row
         * @throws IllegalStateException can be thrown caused by SQL Exception
         * when moving to the next row.
         */
        private boolean fetchHasNext () throws IllegalStateException
        {
            try
            {
                return this.rs.next ();
            }
            catch (SQLException e)
            {
                //getLogger ().error ("SQLException", e);
                throw new IllegalStateException (e);
            }
        }

        /**
         * The constructor for iterator that manages statement as well result
         * set.
         * @param stmt is the statement to manage
         * @param rs is the result set to provide access to
         * @param convert is used to convert row into expected type
         */
        private CloseableRSIteratorImpl (
            Statement stmt, ResultSet rs, Reader <? extends T> convert)
        {
            this.stmt = stmt;
            this.rs = rs;
            this.convert = convert;
            this.hasNext = null;
        }
        /**
         * The constructor for iterator that manages statement as well result
         * set.
         * @param rs is the result set to provide access to
         * @param convert is used to convert row into expected type
         */
        private CloseableRSIteratorImpl (
            ResultSet rs, Reader < ? extends T> convert)
        {
            this.stmt = null;
            this.rs = rs;
            this.convert = convert;
            this.hasNext = null;
        }
        private Boolean hasNext;
        private final ResultSet rs;
        private final Reader <? extends T> convert;
        private final Statement stmt;
    }

    /**
     * The method that creates an iterator
     * @param rs is the result set to iterate over that can be closed by
     * returned iterator
     * @param convert converts rows into T object
     * @return the iterator over T objects
     * @throws IllegalStateException can be thrown if an SQLException
     * occurs while accessing the database
     *
     * @param  <T> is the type of object to produce when iterating
     */
    public static <T> CloseableIterator <T> createIterator (
        ResultSet rs, Reader <? extends T> convert)
    {
        Preconditions.checkNotNull (rs, "null resultset supplied");
        Preconditions.checkNotNull (convert, "null converter supplied");
        return new CloseableRSIteratorImpl <> (rs, convert);
    }

    /**
     * The method that creates an iterator over T in the docbase.
     *
     * @param jdbc provides access to the database
     * @param sql is the SQL command to execute
     * @param convert is the reader that performs the conversion
     * @param <T> is the type of object to iterate over
     * @return the matching iterator
     */
    public static <T> CloseableIterator <T> createIterator (
        JDBCConnection jdbc, String sql, Reader <? extends T> convert)
    {
        Preconditions.checkNotNull (jdbc, "null connection supplied");
        Preconditions.checkNotNull (sql, "null SQL query supplied");
        Preconditions.checkNotNull (convert, "null converter supplied");
        try
        {
            Statement stmt = jdbc.connection.createStatement (
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            try
            {
                final long start = System.nanoTime ();
                ResultSet rs = stmt.executeQuery (sql);
                final long elapsed = System.nanoTime () - start;
                System.out.println (
                    "spent " + NanoTime.humanString (elapsed) +
                        " to execute: " + sql);
                return new CloseableRSIteratorImpl <> (stmt, rs, convert);
            }
            catch (SQLException e)
            {
                //getLogger ().error ("SQLException", e);
                stmt.close ();
                throw new IllegalStateException (e);
            }
        }
        catch (SQLException e)
        {
            //getLogger ().error ("SQLException", e);
            throw new IllegalStateException (e);
        }
    }
}
