# SAF Creator
is a desktop application written in Java.  Its purpose is to prepare <a href="https://wiki.duraspace.org/display/DSDOC6x/Importing+and+Exporting+Items+via+Simple+Archive+Format">Simple Archive Format (SAF)</a> archives for importation into <a href="https://duraspace.org/dspace/">DSpace</a> repositories.  There are a number of good tools for this purpose, and every use case is different.  Many digital curators choose to package their SAF with local custom scripts.  But general purpose tools can be immensely useful, especially when supplemented by custom scripts.  Other popular general-purpose SAF support tools that may meet your needs include <a href="https://github.com/cstarcher/pysaf">PySAF</a> and <a href="https://github.com/DSpace-Labs/SAFBuilder">SAFBuilder</a>.

## Deployment basics
Requires Apache Maven and a JVM.
Build with "mvn clean package".  Run (replacing the version as appropriate) with "java -jar target/SAFCreator-0.0.2-SNAPSHOT.one-jar.jar"

If you prefer not to build from source, a compiled jar is provided at this link:
https://github.com/jcreel/SAFCreator/raw/master/jarfile/SAFCreator-0.0.2-SNAPSHOT.one-jar.jar

## Usage instructions
Basically, you need a spreadsheet (a CSV file) with the metadata and references to the files.  Each row represents one item, and each column a metadata field.  This is a typical starting format for digital library metadata.
 
To make the references to the files, there needs to be (at least) one special column having the heading “filename” or “bundle:ORIGINAL” if you want to specify the bundle.  You’d typically replace ORIGINAL with another bundle.  Just using “filename” defaults to the ORIGINAL bundle.  You can have multiple columns for multiple bundles.  Then in the column under that heading, you would have the filenames (separated by double bar ||) of the bitstreams to go in that item.  You can also use subpaths relative to the top level directory of the files, and * as a wildard to include everything under a subpath.
 
The other headings would be dc-style field labels, e.g. “dc.title” or “dc.description.abstract”.  And the cells in that column would be the values (again, double bar || delimited) for that field for each item.
 
In the SAFCreator, you need to use the file picker to select the CSV file, select the directory where the files are (that are referred to in the “filename” or bundle columns), and select the directory where you want to write the SAF.  You need load the batch with the button, then go over to the validation tab and validate the batch, and then go back to the first tab and do the writing.

Again, if you want a direct download to avoid building from source, you can grab it here:
https://github.com/jcreel/SAFCreator/raw/master/jarfile/SAFCreator-0.0.2-SNAPSHOT.one-jar.jar
