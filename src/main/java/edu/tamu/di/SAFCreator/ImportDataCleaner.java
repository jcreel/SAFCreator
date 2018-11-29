package edu.tamu.di.SAFCreator;

import java.io.File;
import java.io.IOException;

import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import org.apache.commons.io.FileUtils;

import edu.tamu.di.SAFCreator.model.Batch;
import edu.tamu.di.SAFCreator.model.Flag;
import edu.tamu.di.SAFCreator.model.FlagPanel;
import edu.tamu.di.SAFCreator.model.ImportDataOperator;
import edu.tamu.di.SAFCreator.model.Item;

public class ImportDataCleaner extends SwingWorker<Boolean, ImportDataOperator.Updates> implements ImportDataOperator {
    private Batch batch = null;
    private JTextArea console = null;
    private FlagPanel flags = null;

    @Override
    protected Boolean doInBackground() {
        boolean noErrors = true;
        int itemCount = 1;
        int totalItems = batch.getItems().size();

        for (Item item : batch.getItems()) {
            if (isCancelled()) {
                break;
            }

            itemCount++;

            if (batch.isFailedRow(itemCount)) {
                File directory = new File(item.getSAFDirectory());

                if (directory.isDirectory()) {
                    try {
                        FileUtils.deleteDirectory(directory);
                        console.append("\tDeleted directory for (row " + itemCount + "), because of write failure.\n");
                    } catch (IOException e) {
                        noErrors = false;
                        Flag flag = new Flag(Flag.DELETE_FAILURE, "Failed to delete directory.", "", "", "", "" + itemCount, batch.getAction());
                        flags.appendRow(flag);
                        console.append("\tFailed to delete directory for (row " + itemCount + "), due to an error.\n");
                    }
                }
            }

            publish(new ImportDataOperator.Updates(itemCount - 1, totalItems));
        }

        if (isCancelled()) {
            console.append("Cancelled cleaning SAF.\n");
            return null;
        }

        console.append("Done cleaning SAF data.\n");
        return noErrors;
    }

    @Override
    public Batch getBatch() {
        return batch;
    }

    @Override
    public JTextArea getConsole() {
        return console;
    }

    @Override
    public FlagPanel getFlags() {
        return flags;
    }

    @Override
    public void setBatch(Batch batch) {
        this.batch = batch;
    }

    @Override
    public void setConsole(JTextArea console) {
        this.console = console;
    }

    @Override
    public void setFlags(FlagPanel flags) {
        this.flags = flags;
    }
}
