package edu.tamu.di.SAFCreator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.net.URI;

import javax.swing.JTextArea;

import com.opencsv.CSVReader;
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
import edu.tamu.di.SAFCreator.model.SchematicFieldSet;
import edu.tamu.di.SAFCreator.model.StubLabel;
import edu.tamu.di.SAFCreator.model.Verifier;
import edu.tamu.di.SAFCreator.model.Verifier.Problem;

public class ImportDataProcessorImpl implements ImportDataProcessor 
{
	private static String PdfPrefix = "document-";
	private static String PdfSuffix = ".pdf";

	public Batch loadBatch(String metadataInputFileName,
			String sourceDirectoryName, String outputDirectoryName,
			JTextArea console) 
	{
	
		File sourceDirFileForChecking = new File(sourceDirectoryName);
		File outputDirFileForChecking = new File(outputDirectoryName);
		
		if(!(sourceDirFileForChecking.exists() && sourceDirFileForChecking.isDirectory()))
		{
			console.append("Source file directory " + sourceDirectoryName + " is not a readable directory.\n");
			return null;
		}
		

		if(!(outputDirFileForChecking.exists() && outputDirFileForChecking.isDirectory()))
		{
			console.append("Designated SAF output directory " + outputDirectoryName + " is not an available directory.\n");
			return null;
		}
		
		
		
		Batch batch = new Batch();
		boolean errorState = false;
		batch.setinputFilesDir(sourceDirectoryName);
		batch.setOutputSAFDir(outputDirectoryName);
		List<ColumnLabel> columnLabels = new ArrayList<ColumnLabel>();
		CSVReader reader = null;
		
		try {
			
			reader = new CSVReader(new FileReader(metadataInputFileName));
			String[] labelLine;
			String[] nextLine;
			
				
			labelLine = reader.readNext();
			
			char columnCounter = 'A';
			for(String cell : labelLine)
			{
				if(cell.toLowerCase().contains("bundle:") || cell.toLowerCase().contains("group:"))
				{
					String bundleName = cell.split(":")[1];
					FileLabel fileLabel = new FileLabel(bundleName);
					fileLabel.setColumn(columnCounter);
					fileLabel.setRow(1);
					columnLabels.add(fileLabel);	
				}
				else if(cell.toLowerCase().contains("filename"))
				{
					FileLabel fileLabel = new FileLabel("ORIGINAL");
					fileLabel.setColumn(columnCounter);
					fileLabel.setRow(1);
					columnLabels.add(fileLabel);
				}
				else if(cell.toLowerCase().contains("handle"))
				{
					HandleLabel handleLabel = new HandleLabel();
					handleLabel.setColumn(columnCounter);
					handleLabel.setRow(1);
					columnLabels.add(handleLabel);
				}
				else if(cell.contains("."))
				{
					FieldLabel fieldLabel = new FieldLabel();
					fieldLabel.setSchema(Util.getSchemaName(cell));
					fieldLabel.setElement(Util.getElementName(cell));
					fieldLabel.setQualifier(Util.getElementQualifier(cell));
					fieldLabel.setLanguage(Util.getLanguage(cell));
					fieldLabel.setColumn(columnCounter);
					fieldLabel.setRow(1);
					columnLabels.add(fieldLabel);
				}
				else
				{
					console.append("\nWarning:  Ignoring invalid column label at column " + columnCounter + ": " + cell + "\n");
					StubLabel stubLabel = new StubLabel();
					stubLabel.setColumn(columnCounter);
					stubLabel.setRow(1);
					columnLabels.add(stubLabel);
				}
				columnCounter++;
			}
			
			//record the column labels for verification purposes
			batch.setLabels(columnLabels);
			
			
			//if we encountered an error reading the labels, then exit
			if ( errorState == true)
			{
				reader.close();
				return null;
			}
			
			int linenumber = 0;
			while((nextLine = reader.readNext()) != null)
			{
				linenumber++;
				
				Item item = new Item(linenumber, batch);
				
				
				columnCounter = 'A';
				for(int columnIndex = 0; columnIndex < nextLine.length; columnIndex++)
				{
					ColumnLabel label = columnLabels.get(columnIndex);
					String cell = nextLine[columnIndex];
					 
					if(label.isField())
					{
						//get the Field's schema
						FieldLabel fieldLabel = (FieldLabel) label;
						String schemaName = Util.getSchemaName(fieldLabel.getSchema());
						SchematicFieldSet schema = item.getOrCreateSchema(schemaName);
						
						//eliminate trailing ||
						if( cell.endsWith("||") ) cell = cell.substring(0, cell.length()-2);
						
						//create Field(s) within the schema
						int numberOfValues = Util.regexMatchCounter("\\|\\|", cell) + 1;
						String[] values = cell.split("\\|\\|");
						for(int valueCounter = 0; valueCounter < numberOfValues; valueCounter++)
						{
							String value = values[valueCounter].trim();
							Field field = new Field();
							field.setSchema(schema);
							field.setLabel(fieldLabel);
							field.setValue(value);
							field.setColumn(columnCounter);
							field.setRow(linenumber);
							
							schema.addField(field);
						}
						
					}
					else if(label.isFile())
					{
						FileLabel fileLabel = (FileLabel) label;
						String bundleName = fileLabel.getBundleName();
						Bundle bundle = item.getOrCreateBundle(bundleName);
						URI uri = URI.create(cell);
						
						if (uri.isAbsolute() && !uri.getScheme().toString().equalsIgnoreCase("file")) {
							String scheme = uri.getScheme().toString();
							if (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("ftp")) {
								Bitstream bitstream = new Bitstream();
								bitstream.setBundle(bundle);
								bitstream.setSource(uri);
								bitstream.setRelativePath(PdfPrefix + columnIndex + PdfSuffix);
								bitstream.setColumn(columnCounter);
								bitstream.setRow(linenumber);
								bundle.addBitstream(bitstream);
							}
							else {
								console.append("\n*** WARNING:  URL protocol on line " + linenumber + " cell " + columnIndex + " must be one of: HTTP, HTTPS, or FTP. ***\n");
							}
						}
						else if (cell.isEmpty()) {
							continue;
						}
						else {
							int numberOfValues = Util.regexMatchCounter("\\|\\|", cell) + 1;
							String[] values = cell.split("\\|\\|");
							for(int valueCounter = 0; valueCounter < numberOfValues; valueCounter++)
							{
								String value = values[valueCounter].trim();

								value = value.replace("/", File.separator);

								//if the value is of the form foo/* then get all the files in foo
								//(note that at present this assumes no further subdirectories under foo)
								//otherwise, just get the single named file
								if(value.endsWith(File.separator + "*"))
								{
									String directoryName = value.substring(0, value.length()-2);
									File directory = new File(batch.getinputFilesDir() + File.separator + directoryName);
									File[] files = directory.listFiles();
									if(files == null)
									{
									        console.append("\n*** WARNING:  No files found for item directory " + directory.getPath() + " ***\n");
									}
									else
									{
										for(File file : files)
										{
											Bitstream bitstream = new Bitstream();
											bitstream.setBundle(bundle);
											bitstream.setSource(file.toURI());
											bitstream.setRelativePath(directoryName + File.separator + file.getName());
											bitstream.setColumn(columnCounter);
											bitstream.setRow(linenumber);
											bundle.addBitstream(bitstream);
										}
									}
								}
								else
								{
									URI fileUri = URI.create(batch.getinputFilesDir() + File.separator + value);
									if (fileUri != null) {
										Bitstream bitstream = new Bitstream();
										bitstream.setBundle(bundle);
										bitstream.setSource(fileUri);
										bitstream.setRelativePath(value);
										bitstream.setColumn(columnCounter);
										bitstream.setRow(linenumber);
										bundle.addBitstream(bitstream);
									}
								}
							}
						}
					}
					else if(label.isHandle())
					{
					    item.setHandle(cell.trim());
					}
					else
					{
						console.append("Ignoring line " + linenumber + " column " + columnCounter + "\n");
					}
					columnCounter++;
				}
				
				batch.addItem(item);
				
			}
			reader.close();
		} catch (FileNotFoundException e) {
			console.append("Metadata input file " + metadataInputFileName + " does not exist.\n");
			e.printStackTrace();
			errorState = true;
		} catch (IOException e) {
			console.append("CSV file reader failed to read line or failed to close.\n");
			e.printStackTrace();
			errorState = true;
		}
		
		if(errorState)
		{
			return null;
		}
		else
		{
			return batch;
		}
		
	}

	public void writeBatchSAF(Batch batch, JTextArea console, FlagPanel flags)
	{
		for(Item item : batch.getItems())
		{
			boolean hasError = false;
			List<Problem> problems = item.writeItemSAF();
			for(Verifier.Problem problem : problems)
			{
				console.append(problem.toString()+"\n");
				if (problem.isError()) {
					hasError = true;
				}
				if (problem.isFlagged()) {
					flags.appendRow(problem.getFlag());
				}
			}
			if (hasError) {
				console.append("\tFailed to write item " + item.getSAFDirectory() + "\n");
			} else {
				console.append("\tWrote item " + item.getSAFDirectory() + "\n");
			}
		}
		console.append("Done writing SAF data.\n");
	}
}
