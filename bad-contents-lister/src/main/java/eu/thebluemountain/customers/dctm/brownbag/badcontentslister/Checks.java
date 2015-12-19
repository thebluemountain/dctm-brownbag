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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/**
 * The class provides a function that check at the content and returns
 * a check code.
 */
public final class Checks
{
    /**
     * The several codes
     */
    public enum Code
    {
        OK, NOTFOUND, EMPTYNOTFOUND, BADSIZE, EMPTY, ERROR
    }

    public static abstract class Result
    {
        public static final Result OK = new Result (Code.OK, null) { };

        /**
         * The class when size reported is different from expected one
         */
        public static final class Size extends Result
        {

            /**
             * The actual size
             */
            public final long actual;

            /**
             * The expected size
             */
            public final long expected;

            /**
             * @return the string representation (aka: the code)
             */
            public String toString ()
            {
                return "Result: {code: " + this.code +
                    ", path: " + this.path +
                    ", expected: " + this.expected +
                    ", actual: " + this.actual + "}";
            }

            private Size (Path path, long expected, long actual)
            {
                super (Code.BADSIZE, path);
                this.expected = expected;
                this.actual = actual;
            }
        }

        /**
         * The class when size reported is 0 when expected one is not
         */
        public static final class Empty extends Result
        {
            /**
             * The expected size
             */
            public final long expected;

            /**
             * @return the string representation (aka: the code)
             */
            public String toString ()
            {
                return "Result: {code: " + this.code +
                    ", path: " + this.path +
                    ", expected: " + this.expected + "}";
            }

            private Empty (Path path, long expected)
            {
                super (Code.EMPTY, path);
                this.expected = expected;
            }
        }

        /**
         * The class of use when a file was not found when expected
         */
        public static final class NotFound extends Result
        {
            @Override
            public String toString ()
            {
                return "Result {code: " + this.code +
                    ", path: " + this.path + "}";
            }
            private NotFound (Path path)
            {
                super (Code.NOTFOUND, path);
            }
        }

        /**
         * The class of use when a file was not found when expected
         * but it was 0-bytes one
         */
        public static final class EmptyNotFound extends Result
        {
            @Override
            public String toString ()
            {
                return "Result {code: " + this.code +
                    ", path: " + this.path + "}";
            }
            private EmptyNotFound (Path path)
            {
                super (Code.EMPTYNOTFOUND, path);
            }
        }

//        /**
//         * The class of use when a file was not found because extension was
//         * different from the expected one
//         */
//        public static final class BadExt extends Result
//        {
//            public final Optional <String> actual;
//            @Override
//            public String toString ()
//            {
//                return "Result {code: " + this.code +
//                    ", path: " + this.path +
//                    ", ext: " + this.actual.or ("(none)") +
//                    "}";
//            }
//            private BadExt (Path path, Optional <String> actual)
//            {
//                super (Code.BADEXT, path);
//                this.actual = actual;
//            }
//        }
//
//        /**
//         * The class of use when a file was not found because extension was
//         * missing
//         */
//        public static final class NoExt extends Result
//        {
//            @Override
//            public String toString ()
//            {
//                return "Result {code: " + this.code +
//                    ", path: " + this.path +
//                    "}";
//            }
//            private NoExt (Path path)
//            {
//                super (Code.NOEXT, path);
//            }
//        }

        /**
         * The class of use when an error occurs attempting to read file size
         */
        public static final class SizeError extends Result
        {
            /**
             * The exception
             */
            public final IOException e;

            @Override
            public String toString ()
            {
                return "Result {code: " + this.code +
                    ", path: " + this.path +
                    ", error: " + this.e + "}";
            }
            private SizeError (Path path, IOException e)
            {
                super (Code.ERROR, path);
                this.e = e;
            }
        }

        /**
         * The return code
         */
        public final Code code;

        /**
         * The path whose file size does not match
         */
        public final Path path;

        /**
         * @return the string representation (aka: the code)
         */
        public String toString ()
        {
            return "Result: {code: " + this.code + "}";
        }
        private Result (Code code, Path path)
        {
            this.code = code;
            this.path = path;
        }
    }

    /**
     * The function that performs the check
     */
    public static final class ContentsChecker
        implements Function <DecoratedContent, Result>
    {
        private static final class CheckedPath
        {
            final Path path;
            final boolean found;
            private CheckedPath (Path path, boolean found)
            {
                this.path = path;
                this.found = found;
            }
            private CheckedPath (Path path)
            {
                this.path = path;
                this.found = true;
            }
        }
        @Override
        public Result apply (DecoratedContent dc)
        {
            CheckedPath cpath = pathOf (dc.content);
            if (! cpath.found)
            {
                if (0 == dc.content.size)
                {
                    return new Result.EmptyNotFound (cpath.path);
                }
                return new Result.NotFound (cpath.path);
            }

            final Path path = cpath.path;
            try
            {
                long actual = Files.size (path);
                if (dc.content.size != actual)
                {
                    if (0L == actual)
                    {
                        return new Result.Empty (path, dc.content.size);
                    }
                    return new Result.Size (path, dc.content.size, actual);
                }
                return Result.OK;
            }
            catch (IOException e)
            {
                return new Result.SizeError (path, e);
            }
        }

        private CheckedPath pathOf (Content content)
        {
            final Path path = this.path.apply (content);
            assert path != null;
            if (Files.exists (path))
            {
                return new CheckedPath (path);
            }
            // OK, the file does not exists ...
            // but possibly exists with another extension or
            // with no extension
            final String name = path.getFileName ().toString ();
            int ext = name.lastIndexOf ('.');
            // files should share same base name as the match
            final String match = (-1 == ext) ? name : name.substring (0, ext);
            // will help select eligible files ...
            FileFilter filter = new FileFilter ()
            {
                @Override
                public boolean accept (File pathname)
                {
                    // sub-directory ??? OK, just return false
                    if (pathname.isDirectory ()) return false;
                    final String fname = pathname.getName ();
                    // looking for something different that original one
                    if (fname.equals (name)) return false;
                    // OK, build the base name ...
                    int dot = fname.lastIndexOf ('.');
                    final String base =
                        (-1 == dot) ? fname : fname.substring (0, dot);
                    return base.equals (match);
                }
            };
            final File dir = path.getParent ().toFile ();
            File [] files = dir.listFiles (filter);
            final int count = files.length;
            if (0 == count)
            {
                // nothing found: returns the original one
                return new CheckedPath (path, false);
            }
            else if (1 == count)
            {
                return new CheckedPath (files [0].toPath ());
            }
            // get the last modified one
            File found = null;
            long modified = -1;
            for (File current : files)
            {
                long curmodified = current.lastModified ();
                if (curmodified > modified)
                {
                    modified = curmodified;
                    found = current;
                }
            }
            assert found != null;
            return new CheckedPath (found.toPath ());
        }

        private ContentsChecker (Function <Content, Path> path)
        {
            this.path = path;
        }
        private final Function <Content, Path> path;
    }

    /**
     * The function that returns the path matching a content
     */
    private static final class PathOf implements Function <Content, Path>
    {
        @Override
        public Path apply (Content content)
        {
            Store store = this.storeof.apply (content.store);
            Preconditions.checkNotNull (
                store, "null store found for content %s", content);
            String relative = Content.makeRelative (content);
            return Paths.get (store.path, relative);
        }
        private PathOf (
            Function <String, Store> storeof)
        {
            this.storeof = storeof;
        }
        private final Function <String, Store> storeof;
    }

    private static Function <Content, Path> create (final Stores stores)
    {
        Function <String, Store> storeof =
            new Function <String, Store> ()
        {
            @Override
            public Store apply (String id)
            {
                return this.byids.get (id);
            }
            private final ImmutableMap <String, Store> byids = stores.byids ();
        };
        return new PathOf (storeof);
    }

    /**
     * The method return the function that verifies the content.
     * @param stores carries the stores of use when accessing the files
     * @return the matching result
     */
    public static Function <DecoratedContent, Result> checker (Stores stores)
    {
        Preconditions.checkNotNull (stores);
        Function <Content, Path> path = create (stores);
        return new ContentsChecker (path);
    }

    private Checks () { super(); }
}
