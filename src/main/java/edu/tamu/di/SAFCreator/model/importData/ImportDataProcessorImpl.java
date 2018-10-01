package edu.tamu.di.SAFCreator.model.importData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JTextArea;

import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypes;

import com.opencsv.CSVReader;

import edu.tamu.di.SAFCreator.Util;
import edu.tamu.di.SAFCreator.model.Batch;
import edu.tamu.di.SAFCreator.model.Bitstream;
import edu.tamu.di.SAFCreator.model.Bundle;
import edu.tamu.di.SAFCreator.model.ColumnLabel;
import edu.tamu.di.SAFCreator.model.Field;
import edu.tamu.di.SAFCreator.model.FieldLabel;
import edu.tamu.di.SAFCreator.model.FileLabel;
import edu.tamu.di.SAFCreator.model.FlagPanel;
import edu.tamu.di.SAFCreator.model.HandleLabel;
import edu.tamu.di.SAFCreator.model.Item;
import edu.tamu.di.SAFCreator.model.Problem;
import edu.tamu.di.SAFCreator.model.SchematicFieldSet;
import edu.tamu.di.SAFCreator.model.StubLabel;

public class ImportDataProcessorImpl implements ImportDataProcessor {
    private static String PdfPrefix = "document-";
    private static String PdfSuffix = ".pdf";

    public String columnNumberToLabel(int number) {
        int dividend = number;
        String label = "";
        int modulo;
        char character;

        while (dividend > 0) {
            modulo = (dividend - 1) % 26;
            character = Character.valueOf((char) (65 + modulo));

            label = character + label;
            dividend = (dividend - modulo) / 26;
        }

        return label;
    }

    /**
     * Stub function required by item.writeItemSAF.
     *
     * There is no background worker to cancel, so this always returns false.
     *
     * @return false
     */
    public boolean isCancelled() {
        return false;
    }

    @Override
    public Batch loadBatch(String metadataInputFileName, String sourceDirectoryName, String outputDirectoryName, JTextArea console) {
        File sourceDirFileForChecking = new File(sourceDirectoryName);
        File outputDirFileForChecking = new File(outputDirectoryName);

        if (!(sourceDirFileForChecking.exists() && sourceDirFileForChecking.isDirectory())) {
            console.append("\tERROR: Source file directory " + sourceDirectoryName + " is not a readable directory.\n");
            return null;
        }

        if (!(outputDirFileForChecking.exists() && outputDirFileForChecking.isDirectory())) {
            console.append("\tERROR: Designated SAF output directory " + outputDirectoryName + " is not an available directory.\n");
            return null;
        }

        {
            File metadataInputFile = new File(metadataInputFileName);
            if (!(metadataInputFile.exists() && metadataInputFile.isFile())) {
                console.append("\tERROR: input CSV file " + metadataInputFile + " is not an available file.\n");
                return null;
            }

            FileInputStream fileStream = null;
            TikaInputStream tikaStream = null;
            try {
                fileStream = new FileInputStream(metadataInputFile);
                tikaStream = TikaInputStream.get(fileStream);
                Metadata metadata = new Metadata();
                Detector detector = new DefaultDetector(MimeTypes.getDefaultMimeTypes());
                MediaType mediaType = detector.detect(tikaStream, metadata);

                if (!mediaType.toString().equalsIgnoreCase("text/plain")) {
                    console.append("\tERROR: input CSV file " + metadataInputFile + " is not a valid CSV file, reason: mime-type is: " + mediaType.toString() + ".\n");
                    return null;
                }
            } catch (IOException e) {
                console.append("\tERROR: input CSV file " + metadataInputFile + " had an I/O error, reason: " + e.getMessage() + ".\n");
                return null;
            } finally {
                if (tikaStream != null) {
                    try {
                        tikaStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }

        Batch batch = new Batch();
        boolean errorState = false;
        batch.setinputFilesDir(sourceDirectoryName);
        batch.setOutputSAFDir(outputDirectoryName);
        List<ColumnLabel> columnLabels = new ArrayList<ColumnLabel>();
        CSVReader reader = null;
        int linenumber = 1;

        try {
            reader = new CSVReader(new FileReader(metadataInputFileName));
            String[] labelLine;
            String[] nextLine;

            labelLine = reader.readNext();

            int column = 1;
            for (String cell : labelLine) {
                if (cell.toLowerCase().contains("bundle:") || cell.toLowerCase().contains("group:")) {
                    String bundleName = cell.split(":")[1];
                    FileLabel fileLabel = new FileLabel(bundleName);
                    fileLabel.setColumn(column);
                    fileLabel.setRow(1);
                    columnLabels.add(fileLabel);
                } else if (cell.toLowerCase().contains("filename")) {
                    FileLabel fileLabel = new FileLabel("ORIGINAL");
                    fileLabel.setColumn(column);
                    fileLabel.setRow(1);
                    columnLabels.add(fileLabel);
                } else if (cell.toLowerCase().contains("handle")) {
                    HandleLabel handleLabel = new HandleLabel();
                    handleLabel.setColumn(column);
                    handleLabel.setRow(1);
                    columnLabels.add(handleLabel);
                } else if (cell.contains(".")) {
                    FieldLabel fieldLabel = new FieldLabel();
                    fieldLabel.setSchema(Util.getSchemaName(cell));
                    fieldLabel.setElement(Util.getElementName(cell));
                    fieldLabel.setQualifier(Util.getElementQualifier(cell));
                    fieldLabel.setLanguage(Util.getLanguage(cell));
                    fieldLabel.setColumn(column);
                    fieldLabel.setRow(1);
                    columnLabels.add(fieldLabel);
                } else {
                    console.append("\tWARNING: Ignoring invalid label for column " + columnNumberToLabel(column) + ": " + cell + "\n");
                    StubLabel stubLabel = new StubLabel();
                    stubLabel.setColumn(column);
                    stubLabel.setRow(1);
                    columnLabels.add(stubLabel);
                }

                column++;
            }

            // record the column labels for verification purposes
            batch.setLabels(columnLabels);

            // if we encountered an error reading the labels, then exit
            if (errorState == true) {
                reader.close();
                return null;
            }

            while ((nextLine = reader.readNext()) != null) {
                linenumber++;
                Item item = new Item(linenumber, batch);
                boolean addItem = true;

                int totalLength = nextLine.length;
                if (nextLine.length < columnLabels.size()) {
                    console.append("\tWARNING: row " + linenumber + ": there are fewer columns (" + nextLine.length + ") than there are labels (" + columnLabels.size() + "), manually adding empty columns.\n");
                    totalLength = columnLabels.size();

                    int columnIndex = nextLine.length;
                    List<String> correctedSet = new ArrayList<String>(Arrays.asList(nextLine));
                    while (columnIndex < totalLength) {
                        correctedSet.add("");
                        columnIndex++;
                    }
                    nextLine = new String[totalLength];
                    nextLine = correctedSet.toArray(nextLine);
                } else if (nextLine.length > columnLabels.size()) {
                    console.append("\tWARNING: row " + linenumber + ": there are more columns (" + nextLine.length + ") than there are labels (" + columnLabels.size() + "), ignoring additional columns.\n");
                    totalLength = columnLabels.size();
                }

                int fileNumber = 0;
                for (column = 1; column <= totalLength; column++) {
                    ColumnLabel label = columnLabels.get(column - 1);
                    String cell = nextLine[column - 1];

                    if (cell.isEmpty()) {
                        continue;
                    }

                    if (label.isField()) {
                        // get the Field's schema
                        FieldLabel fieldLabel = (FieldLabel) label;
                        String schemaName = Util.getSchemaName(fieldLabel.getSchema());
                        SchematicFieldSet schema = item.getOrCreateSchema(schemaName);

                        // eliminate trailing ||
                        if (cell.endsWith("||")) {
                            cell = cell.substring(0, cell.length() - 2);
                        }

                        // create Field(s) within the schema
                        int numberOfValues = Util.regexMatchCounter("\\|\\|", cell) + 1;
                        String[] values = cell.split("\\|\\|");
                        for (int valueCounter = 0; valueCounter < numberOfValues; valueCounter++) {
                            String value = values[valueCounter].trim();
                            Field field = new Field();
                            field.setSchema(schema);
                            field.setLabel(fieldLabel);
                            field.setValue(value);
                            field.setColumn(column);
                            field.setRow(linenumber);

                            schema.addField(field);
                        }

                    } else if (label.isFile()) {
                        FileLabel fileLabel = (FileLabel) label;
                        String bundleName = fileLabel.getBundleName();
                        Bundle bundle = item.getOrCreateBundle(bundleName);
                        URI uri = null;

                        try {
                            uri = URI.create(cell);
                        } catch (IllegalArgumentException e1) {
                            console.append("\tERROR: row " + linenumber + " column " + columnNumberToLabel(column) + ": invalid file path/URI, reason: " + e1.getMessage() + ".\n");
                            errorState = true;
                            addItem = false;
                            break;
                        }

                        if (uri.isAbsolute() && !uri.getScheme().toString().equalsIgnoreCase("file")) {
                            String scheme = uri.getScheme().toString();
                            if (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("ftp")) {
                                Bitstream bitstream = new Bitstream();
                                bitstream.setBundle(bundle);
                                bitstream.setSource(uri);
                                bitstream.setRelativePath(PdfPrefix + (++fileNumber) + PdfSuffix);
                                bitstream.setColumn(column);
                                bitstream.setRow(linenumber);
                                bundle.addBitstream(bitstream);
                            } else {
                                console.append("\tWARNING: row " + linenumber + " column " + columnNumberToLabel(column) + ": URL protocol must be one of: HTTP, HTTPS, or FTP. ***\n");
                            }
                        } else {
                            int numberOfValues = Util.regexMatchCounter("\\|\\|", cell) + 1;
                            String[] values = cell.split("\\|\\|");
                            for (int valueCounter = 0; valueCounter < numberOfValues; valueCounter++) {
                                String value = values[valueCounter].trim();

                                value = value.replace("/", File.separator);

                                // if the value is of the form foo/* then get all the files in foo
                                // (note that at present this assumes no further subdirectories under foo)
                                // otherwise, just get the single named file
                                if (value.endsWith(File.separator + "*")) {
                                    String directoryName = value.substring(0, value.length() - 2);
                                    File directory = new File(batch.getinputFilesDir() + File.separator + directoryName);
                                    File[] files = directory.listFiles();
                                    if (files == null) {
                                        console.append("\nWARNING: No files found for item directory " + directory.getPath() + " ***\n");
                                    } else {
                                        for (File file : files) {
                                            Bitstream bitstream = new Bitstream();
                                            bitstream.setBundle(bundle);
                                            bitstream.setSource(file.toURI());
                                            bitstream.setRelativePath(directoryName + File.separator + file.getName());
                                            bitstream.setColumn(column);
                                            bitstream.setRow(linenumber);
                                            bundle.addBitstream(bitstream);
                                        }
                                    }
                                } else {
                                    URI fileUri = URI.create(batch.getinputFilesDir() + File.separator + value);
                                    if (fileUri != null) {
                                        Bitstream bitstream = new Bitstream();
                                        bitstream.setBundle(bundle);
                                        bitstream.setSource(fileUri);
                                        bitstream.setRelativePath(value);
                                        bitstream.setColumn(column);
                                        bitstream.setRow(linenumber);
                                        bundle.addBitstream(bitstream);
                                    }
                                }
                            }
                        }
                    } else if (label.isHandle()) {
                        item.setHandle(cell.trim());
                    } else {
                        // console.append("\tWARNING: Ignoring row " + linenumber + " column " + columnNumberToLabel(column) + "\n");
                    }
                }

                if (addItem) {
                    batch.addItem(item);
                }
            }
            reader.close();
            reader = null;
        } catch (FileNotFoundException e) {
            console.append("\tERROR: Metadata input file " + metadataInputFileName + " does not exist.\n");
            e.printStackTrace();
            errorState = true;
        } catch (IOException e) {
            console.append("\tERROR: CSV file reader failed to read line or failed to close.\n");
            e.printStackTrace();
            errorState = true;
        } catch (IllegalArgumentException e) {
            console.append("\tERROR: CSV file reader failed to read line " + linenumber + ".\n");
            e.printStackTrace();
            errorState = true;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                    reader = null;
                }
            } catch (IOException e) {
            }
        }

        if (errorState) {
            return null;
        } else {
            return batch;
        }
    }

    @Override
    public void writeBatchSAF(Batch batch, JTextArea console, FlagPanel flags) {
        int itemCount = 1;
        for (Item item : batch.getItems()) {
            if (batch.isIgnoredRow(++itemCount)) {
                File directory = new File(batch.getOutputSAFDir().getAbsolutePath() + File.separator + itemCount);
                directory.delete();

                console.append("\tSkipped item (row " + itemCount + "), because of verification failure.\n");
                continue;
            }

            boolean hasError = false;
            Method method;
            List<Problem> problems = null;
            try {
                method = this.getClass().getMethod("isCancelled");
                problems = item.writeItemSAF(this, method);

                for (Problem problem : problems) {
                    console.append("\t" + problem.toString() + "\n");
                    if (problem.isError()) {
                        hasError = true;
                    }
                    if (problem.isFlagged()) {
                        flags.appendRow(problem.getFlag());
                    }
                }
            } catch (NoSuchMethodException | SecurityException e) {
                e.printStackTrace();
            }

            if (hasError) {
                console.append("\tFailed to write item (row " + itemCount + ") " + item.getSAFDirectory() + ".\n");
            } else {
                console.append("\tWrote item (row " + itemCount + ") " + item.getSAFDirectory() + ".\n");
            }
        }

        console.append("Done writing SAF data.\n");
    }
}
