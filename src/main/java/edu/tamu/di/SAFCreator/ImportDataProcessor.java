package edu.tamu.di.SAFCreator;


import javax.swing.JTextArea;

import edu.tamu.di.SAFCreator.model.Batch;
import edu.tamu.di.SAFCreator.model.FlagPanel;

public interface ImportDataProcessor 
{
	public Batch loadBatch(String metadataInputFileName, String sourceDirectoryName, String outputDirectoryName, JTextArea console);
	public void writeBatchSAF(Batch batch, JTextArea console, FlagPanel flags);
}
