# What's lost contents for Documentum?

Recently, a documentum customer encountered hardware issues with storage system
of use by their Documentum servers.

Storage system was reporting over 10K inconsistencies for many directories and
files but could not let us know which directory or file exactly was concerned.

In particular, we wanted to know whether Documentum managed files were
concerned by the inconsistencies, and, for any of these files, we wanted
to list which document were impacted.

Documentum provides a tools, called _ConsistentChecker_, that performs
various checks in the docbase.

For content related checks, it ensures correct consistency between all
_dmr_content_ objects and related parents (system objects), related format and
related store (the storage area content is relative to).

Unfortunately for us, it does not check, in case the content is held in a file
store, whether file actually exists and, if it does, whether its size actually
matches the one expected by Documentum.

What we were needing was a report that would display, for each inconsistent
content:
1. Information about the system object: it's id, name, type, ...
2. Information about the inconsistent content: path on the file system,
(expected) size, ...

We've been therefore writing a little tool to figure this out ...

## What's a Documentum content?
Documentum content (aka _dmr_content_) is a place holder to reference a file, blob, object, ...
anything that provides access to binary data.

Such content is not accesses directly by Documentum clients: rather it is used
indirectly when accessing related document's contents.

Contrary to obvious thinking, a document (in fact, any system object that is not
a folder) does not point to a content: the content carries pointer to related
document(s).
This allows for having several documents sharing the same content.

For example, when saving an existing document as a new one, Documentum just
need to create a new copy of the document object and, for the content, just
adds a reference to the newly created document to the content: no content copy
is necessary then.

## How does Documentum get a content stored in a file store?

A _dmr_content_ object carries the following data:

* _parent_id_: An enumeration of its parents
* _storage_id_: A pointer to its storage object
* _data_ticket_: An numeric identifier that allows for retrieving the file in
the related store
* Information about the content self: its size, the date is was imported,
whether it is a primary content or a rendition, the format, ...

### The content
Following shows sample data carried by a _dmr_content_ object:
```
API> fetch,c,060186a1800328ca
...
OK
API> dump,c,l
...
USER ATTRIBUTES

  parent_id                    [0]: 090186a18008fcef
  page                         [0]: 0
  rendition                       : 0
  parent_count                    : 1
  storage_id                      : 280186a180000100
  data_ticket                     : -2147347476
  other_ticket                    : 0
  content_size                    : 146977
  full_format                     : pdf
  format                          : 270186a18000019f
  ...
  set_client                      : dctmserver.localdomain
  set_file                        : /tmp/sample.pdf
  set_time                        : 11/28/2015 5:49:19 PM
  ...
SYSTEM ATTRIBUTES

  r_content_hash                  :
  r_object_id                     : 060186a1800328ca

INTERNAL ATTRIBUTES

  ...
  i_index_format                []: <none>
  i_format                     [0]: 270186a18000019f
  ...
```

The _parent_id_ attribute carries a collection of system object identifiers.

On most Documentum server, there exist _dmr_content_ objects with no parents:
these are content objects that were having a parent that got deleted. When
deleting a system object, Documentum just remove reference to the deleted
object in the _parent_id_ attribute, but the content remains in the system (as
well as it's file in the case of a file storage).

### The store
The _storage_id_ attribute identifies a _dm_store_ object that provides
more information about the type of storage and, when matching a file store,
provide the top-level path in the file system containing files for the
Documentum system.

In this discussion, we are interested in contents whose related store is a
file store (_dm_filestore_). Beside data common to all type of Documentum
stores, a file store defines:
* _root_: the identifying name of a _dm_location_ object that carries a file
system path
* _use_extensions_: a flag that indicates whether, upon storing a file,
Documentum should append the related format's extension to the name of the
file

```
API> fetch,c,280186a180000100
...
OK
API> dump,c,l
...
USER ATTRIBUTES

  name                            : filestore_01
  ...
  root                            : storage_01
  ...
  use_extensions                  : T

SYSTEM ATTRIBUTES
  ...
  r_object_id                     : 280186a180000100
  ...
```

A _dm_location_ referred to by the file store carries the following attributes:

```
API> retrieve,c,dm_location WHERE object_name = 'storage_01'
...
3a0186a18000013f
API> dump,c,l
...
USER ATTRIBUTES

  object_name                     : storage_01
  ...
  path_type                       : directory
  file_system_path                : \\NAS01\dctmdata\EHRDM\content_storage_01
...
SYSTEM ATTRIBUTES

  r_object_type                   : dm_location
  ...
  r_object_id                     : 3a0186a18000013f
  ...
```
In particular, the _file_system_path_ attribute carries the path of the
top-level directory where related files reside in:
\\\NAS01\dctmdata\EHRDM\content_storage_01

### The format
A format, in Documentum, can be identified by its name: the _full_format_
carries the name of related format. (Note that we can use as well the
_i_format_ attribute to identify the format as well). Looking at the metadata
carried by the pdf format, we see:

```
USER ATTRIBUTES

  name                            : pdf
  description                     : Acrobat PDF
  ...
  dos_extension                   : pdf
  ...
SYSTEM ATTRIBUTES

  r_object_id                     : 270186a18000019f
  ...
```
The 'pdf' format defines, as file extension: 'pdf'.

### Resolving the path
The _data_ticket_ allows for identifying the actual file in the file store.

When accessing Documentum server using _iapi_, we can access a data associated
to an object using the _getfile_ api.

```
API> getfile,c,090186a18008fcef
...
/tmp/dctm/data/local/process7990757862341010350.tmp/1/060186a1800328ca.pdf

```

We can also ask the Documentum server the actual location, at server
side, of the file we just retrieved:
```
API> ?,c,EXECUTE get_path '060186a1800328ca'
result
------
\\NAS01\dctmdata\EHRDM\content_storage_01\000186a1\80\02\13\ec.pdf
```

The path is computed by the Documentum server concatenating:
1. **\\NAS01\dctmdata\EHRDM\content_storage_01**: the value of the
_file_system_path_ attribute is related location object
2. **000186a1**: the id of the docbase. This id is present in all object's id,
 left-padded with 0 to reach 6 characters. On the file system, it is
 left-padded with 0 to reach 8 characters
3. **80\02\13\ec**: the relative path of the file in its file store.
It is computed from the content's _data_ticket_'s value
4. **.pdf**: the extension to supply to the file, given the content's related
store and format

#### Using data ticket to build relative path

While computation of the first and second part is straightforward, the
data_ticket needs to be tweaked to serve its purpose.

It is stored in the database as a signed integer. However, it is used by
Documentum as an unsigned integer (using simple cast). Its value is then
displayed as an hexadecimal value, using the file separator character
between each 8-bits value.

The following C program exhibits the conversion:
```c
#include <stdio.h>

union  Ticket
{
 int value;
 unsigned char path [4];
};

int main()
{
    union Ticket ticket;
    ticket.value = -2147347476;
    printf("path:\\%x\\%x\\%x\\%x\n",
     ticket.path [3], ticket.path [2],
     ticket.path [1], ticket.path [0]);
    return 0;
}
```
Running such program produces:
```
path:\80\2\13\ec
```

#### Adding the extension

Adding the extension at the tail of the path requires 2 conditions to hold true:
1. The related file store's attribute _use_extensions_ must be true
2. The related format's _dos_extension_ must not be empty

The extension contained in content's related format is prefixed with '.' (dot)
character.

## Validating a file

The purpose of the tool is to ensure Documentum's file is present and of correct
size.
For each inconsistency, it report one of the following error:
* NOTFOUND: When a file is missing
* EMPTYNOTFOUND: When a file is missing, but its size is reported as 0 bytes
by Documentum (see the _dmr_content.content_size_ attribute)
* EMPTY: When a file is found with 0 byte length while Documentum expects non
empty file
* BADSIZE: When a file is found with a size different from documentum's
expected one
* ERROR: A special case when error occurs while attempting to verify the file
* OK: When everything is OK, no need to report

## Checking at files

Rather than querying repeatedly for each document and related contents, the
program fetches its data using 3 SQL queries:
1. A query to fetch file stores information. We didn't need to restrict on
the file system path as the same NAS resource was used for the stores. Such
query removes a few rows.
```
SELECT f.r_object_id, f.root, f.use_extensions, l.file_system_path
FROM dm_filestore_s f
 INNER JOIN dm_location_sv l ON (l.object_name = f.root)
WHERE f.current_use <> 0
```

2. A query to retrieve all format's name and related extension. The query
returns a few rows as well.
```
SELECT name, dos_extension
FROM dm_format_s
WHERE (dos_extension IS NOT NULL AND dos_extension <> ' ')
```

3. A query that retrieves all contents and related document's metadata of use
when reporting:
```
SELECT
 s.storage_id, s.data_ticket, r.parent_id, s.full_format,
 r.page, s.rendition, s.content_size, s.set_time,
 d.object_name, d.r_object_type, d.i_has_folder
FROM
 dmr_content_s s
 INNER JOIN dmr_content_r r ON (r.r_object_id = s.r_object_id)
 INNER JOIN dm_sysobject_s d ON (r.parent_id = d.r_object_id)
WHERE s.storage_id != '0000000000000000'
ORDER BY s.storage_id, s.data_ticket
```

### Testing the program
The program was run against small Documentum System (about 150K documents),
all packaged in a virtual machine with database and file storage: we know
there were no network communication involved in the test but it would give
us first feeling about the run of the checks.


```
java -jar bad-contents-lister-1.0-SNAPSHOT-full.jar -C jdbc.xml
logging into D:\TEMP\SAMPLE-20151219T032219Z.log
spent 17ms to execute: SELECT  f.r_object_id, f.root, f.use_extensions,  l.file_system_path FROM dm_filestore_s f  INNER JOIN dm_location_sv
 l ON (l.object_name = f.root)
spent 42.76 ms to load stores
spent 680ms to execute: SELECT  s.storage_id, s.data_ticket, r.parent_id, s.full_format,  r.page, s.rendition, s.content_size, s.set_time,
d.object_name, d.r_object_type, d.i_has_folder FROM dmr_content_s s  INNER JOIN dmr_content_r r ON (r.r_object_id = s.r_object_id)  INNER JO
IN dm_sysobject_s d ON (r.parent_id = d.r_object_id) WHERE s.storage_id != '0000000000000000' ORDER BY s.storage_id, s.data_ticket
...............................................................step: {count: 65536, elapsed: 3s 440ms, avg: 52µs, total errors: 1}
...............................................................step: {count: 65536, elapsed: 1s 576ms, avg: 24µs, total errors: 1}
.........step: {count: 9690, elapsed: 268ms, avg: 27µs, total errors: 1}
spent 5.288 s to read 140762 d.c.
stats: [OK x 140761, BADSIZE]
bye
```

Performance was good enough (it was in fact better than expected) so we
started our test on an environment closer to the PRODUCTION environment.

### Real servers difference
The program was executed from a Documentum server against a small docbase.
While activity on these servers is low, database, NAS and Documentum servers
reside on different machines.

Execution of the program from a Documentum server was exhibiting way slower
performances (> x 100). CPU and network activity on the machine, as reported
by system tools, were showing almost no impact of the check against the whole
system: but still, performances were unpleasant.

However, executing the program from the NAS server was effective, reducing
time by a 10 factor.

### Testing on real data
The program outputs a log (in CSV format) that allows for quickly loading in
spreadsheet programs for users to view.

Numerous content checks were reporting NOFOUND errors. These errors were
concerning files stored on the file system at dates earlier from first
inconsistencies reported by NAS server.

However, closer examination of actual files on the file system reveals:
* File was actually existing, but with no extension
* File was actually existing, but with a different extension

Accessing the Documentum system through _iapi_, we could access the contents
through _getfile_ api.

Performing a test on original sandbox environment, we could reproduce the behavior.

#### Documentum's real life

For example, for the file we dumped related _dmr_content_'s attributes of, earlier:

1. Removes or changes the extension
```
ren \\NAS01\dctmdata\EHRDM\content_storage_01\000186a1\80\02\13\ec.pdf \
 \\NAS01\dctmdata\EHRDM\content_storage_01\000186a1\80\02\13\ec.xxx
dir \\NAS01\dctmdata\EHRDM\content_storage_01\000186a1\80\02\13\ec.*
...
 Directory of \\NAS01\dctmdata\EHRDM\content_storage_01\000186a1\80\02\13
11/28/2015  05:49 PM           146,977 ec.xxx
                1 File(s)        146,977 bytes
 ...
```

2. It is still available through Documentum
```
API> getfile,c,090186a18008fcef,/tmp/doc.pdf
...
/tmp/doc.pdf
```

3. Looking at the file system
```
dir \\NAS01\dctmdata\EHRDM\content_storage_01\000186a1\80\02\13\ec.*
...
 Directory of \\NAS01\dctmdata\EHRDM\content_storage_01\000186a1\80\02\13
11/28/2015  05:49 PM           146,977 ec.pdf
                1 File(s)        146,977 bytes
 ...
```

The Documentum system finds the file with any extension.

Remember how Documentum gets content held in a file store ? It uses the
_dos_extension_ attribute of the format associated to a content: the
value of this attribute can be changed at anytime.
When changing such extension, Documentum does not need to rename all files
on file system (that could be associated to read-only file store!).
Rather, it provides support for the change when getting the file, changing
its extension on the file system if possible.

The program was changed to take this case into consideration when testing
files.

## Summing it up ...
Program was run against each docbase.

```
java -jar bad-contents-lister-1.0-SNAPSHOT-full.jar -C jdbc.xml
logging into C:\Temp\checks\xxx.log
spent 74ms to execute: SELECT  f.r_object_id, f.root, f.use_extensions,  l.file_system_path FROM dm_filestore_sv f  INNER JOIN dm_location_sv l ON (l.object_name = f.root) WHERE f.current_use <> 0
spent 121,3 ms to load stores
spent 37s 992ms to execute: SELECT  s.storage_id, s.data_ticket, r.parent_id,  s.full_format, r.page, s.rendition, s.content_size,  s.set_time,  d.object_name, d.r_object_type, d.i_has_folder FROM dmr_content_s s  INNER JOIN dmr_content_r r ON (r.r_object_id = s.r_object_id)  INNER JOIN dm_sysobject_s d ON (r.parent_id = d.r_object_id)  WHERE s.storage_id != '0000000000000000' ORDER BY s.storage_id, s.data_ticket
...............................................................step: {count: 65536, elapsed: 2min 39s 249ms, avg: 2ms, total errors: 0}
...............................................................step: {count: 65536, elapsed: 1min 18s 792ms, avg: 1ms, total errors: 0}
...............................................................step: {count: 65536, elapsed: 1min 27s 403ms, avg: 1ms, total errors: 0}
...............................................................step: {count: 65536, elapsed: 2min 30s 533ms, avg: 2ms, total errors: 0}
...............................................................step: {count: 65536, elapsed: 2min 16s 967ms, avg: 2ms, total errors: 0}
...............................................................step: {count: 65536, elapsed: 1min 59s 427ms, avg: 1ms, total errors: 0}
...............................................................step: {count: 65536, elapsed: 1min 39s 374ms, avg: 1ms, total errors: 0}
...............................................................step: {count: 65536, elapsed: 1min 42s 471ms, avg: 1ms, total errors: 0}
...............................................................step: {count: 65536, elapsed: 1min 33s 171ms, avg: 1ms, total errors: 0}
...............................................................step: {count: 65536, elapsed: 1min 29s 468ms, avg: 1ms, total errors: 0}
...............................................................step: {count: 65536, elapsed: 1min 46s 602ms, avg: 1ms, total errors: 0}
...............................................................step: {count: 65536, elapsed: 2min 12s 549ms, avg: 2ms, total errors: 0}
...............................................................step: {count: 65536, elapsed: 2min 11s 1ms, avg: 1ms, total errors: 0}
...............................................................step: {count: 65536, elapsed: 2min 33s 883ms, avg: 2ms, total errors: 0}
...............................................................step: {count: 65536, elapsed: 2min 11s 319ms, avg: 2ms, total errors: 0}
...............................................................step: {count: 65536, elapsed: 1min 27s 811ms, avg: 1ms, total errors: 0}
...............................................................step: {count: 65536, elapsed: 2min 14s 757ms, avg: 2ms, total errors: 0}
...............................................................step: {count: 65536, elapsed: 1min 36s 32ms, avg: 1ms, total errors: 0}
...............................................................step: {count: 65536, elapsed: 1min 46s 686ms, avg: 1ms, total errors: 0}
...............................................................step: {count: 65536, elapsed: 1min 38s 364ms, avg: 1ms, total errors: 0}
...............................................................step: {count: 65536, elapsed: 1min 38s 209ms, avg: 1ms, total errors: 0}
...............................................................step: {count: 65536, elapsed: 1min 32s 25ms, avg: 1ms, total errors: 0}
...............................................................step: {count: 65536, elapsed: 1min 46s 447ms, avg: 1ms, total errors: 0}
...............................................................step: {count: 65536, elapsed: 1min 47s 337ms, avg: 1ms, total errors: 0}
...............................................................step: {count: 65536, elapsed: 1min 43s 499ms, avg: 1ms, total errors: 0}
.................................step: {count: 34648, elapsed: 57s 555ms, avg: 1ms, total errors: 23}
spent 47,68 min to read 1673048 d.c.
stats: [OK x 1673025, NOTFOUND x 17, EMPTYNOTFOUND x 6]
bye
```

The generated logs, 1 for each docbase, were showing this time way less errors.

Each report was carrying the following columns:
1. parent.r_object_id
2. parent.object_name
3. parent.r_object_type
4. parent.i_has_folder (to figure whether its current revision)
5. content.full_format
6. content._is_rendition (matches true for rendition)
7. content.page
8. content.set_time
9. content._extension (matches the expected extension of the file)
10. content.content_size
11. content.data_ticket
12. error.code
13. file path
14. error message

Two error codes would appear:
* EMPTYNOTFOUND: This indicates the file can be recreated with 0 bytes size.
The process can be automated by reading the CSV log and recreate empty files
* NOTFOUND: Amongst all checks against different docbase, we would found at
most 17 of them. However, looking at the type of document and their name, we
realized they were associated with logs whose lost was not a problem.

Of all NOTFOUND errors, only 1 was reported against a user's file. Manual
operation will be performed to handle this loss.

For other files, they can either be regenerated as empty file of expected size.

Enjoy your next file store corruption ...
