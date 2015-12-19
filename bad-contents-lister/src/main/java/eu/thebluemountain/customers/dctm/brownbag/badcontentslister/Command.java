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

import java.util.EnumSet;
import java.util.Map;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 * The several commands that can be supplied when invoking the
 * process
 */
public enum Command
{
    /**
     * Indicates requirement to have program's usage
     *
     */
    HELP ("--help", "-H", false)
    ,
    /**
     * The specification of the configuration file to use.
     *
     * <p>It expects an XML/JSON file name to be associated
     */
    CONFIG ("--config", "-C", true)
    ;

    /**
     * The method that parses the elements in the arguments to build a map
     * containing the commands and the related value if any
     * @param args carries the command-line arguments
     * @return the commands
     */
    public static Map<Command, Optional <String>> parse (String [] args)
    {
        final int size = args.length;
        Map <Command, Optional <String>> cmds = Maps.newHashMap ();
        for (int index = 0; index < size; index++)
        {
            Command cmd = of (args [index]);
            Preconditions.checkArgument (
                ! cmds.containsKey (cmd),
                "duplicate arguments introducing command %s", cmd);
            if (cmd.requiresvalue)
            {
                index++;
                Preconditions.checkArgument (
                    index < size,
                    "command '%s' requires missing value", cmd);
                String value = args [index];
                cmds.put (cmd, Optional.of (value));
            }
            else
            {
                cmds.put (cmd, Optional.<String>absent ());
            }
        }
        return cmds;
    }

    /**
     * The method that returns the matching command
     *
     * <p>It returns the command whose long name or short name matches
     * supplied value.
     * @param value is the value being parsed
     * @return the matching command
     */
    public static Command of (String value)
    {
        for (Command command : EnumSet.allOf (Command.class))
        {
            if (command.match (value))
            {
                return command;
            }
        }
        throw new IllegalArgumentException (
            "invalid command name supplied: '" + value + '\'');
    }

    /**
     * The command's short name (if any)
     */
    public final Optional<String> shortname;

    /**
     * The command's full name
     */
    public final String longname;

    /**
     * whether the command requires a value
     */
    public final boolean requiresvalue;

    /**
     * The method that checks whether the value matches the command's
     * long name of short name
     * @param value is the value
     * @return whether it matches
     */
    public boolean match (String value)
    {
        return ((this.longname.equals (value)) ||
            ((this.shortname.isPresent ()) &&
                (this.shortname.get ().equals (value))));
    }

    @Override
    public String toString ()
    {
        if (this.shortname.isPresent ())
        {
            return this.longname + " (" + this.shortname.get () + ')';
        }
        return this.longname;
    }

    Command (String longname, boolean requiresvalue)
    {
        this.longname = longname;
        this.shortname = Optional.absent ();
        this.requiresvalue = requiresvalue;
    }
    Command (String longname, String shortname, boolean requiresvalue)
    {
        this.longname = longname;
        this.shortname = Optional.of (shortname);
        this.requiresvalue = requiresvalue;
    }
}
