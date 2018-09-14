package edu.tamu.di.SAFCreator.verify;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextArea;

import edu.tamu.di.SAFCreator.model.Batch;
import edu.tamu.di.SAFCreator.model.Bitstream;
import edu.tamu.di.SAFCreator.model.Bundle;
import edu.tamu.di.SAFCreator.model.Flag;
import edu.tamu.di.SAFCreator.model.FlagPanel;
import edu.tamu.di.SAFCreator.model.Item;
import edu.tamu.di.SAFCreator.model.VerifierBackground;
import edu.tamu.di.SAFCreator.model.VerifierProperty;

public class LocalFilesExistVerifierImpl extends VerifierBackground {

	public LocalFilesExistVerifierImpl() {
		super();
	}

	public LocalFilesExistVerifierImpl(VerifierProperty settings) {
		super(settings);
	}

	@Override
	public void doCancel() {
	}

	@Override
	protected List<Problem> doInBackground() {
		return new ArrayList<Problem>();
	}

	@Override
	public boolean generatesError() {
		return true;
	}

	@Override
	public boolean isSwingWorker() {
		return true;
	}

	@Override
	public String prettyName() {
		return "Local Content Files Exist Verifier";
	}

	@Override
	public List<Problem> verify(Batch batch) {
		return verify(batch, null, null);
	}

	@Override
	public List<Problem> verify(Batch batch, JTextArea console, FlagPanel flagPanel) {
		List<Problem> missingFiles = new ArrayList<Problem>();

		if (!batch.getIgnoreFiles()) {
			int totalItems = batch.getItems().size();
			int itemCount = 0;

			for (Item item : batch.getItems()) {
				for (Bundle bundle : item.getBundles()) {
					for (Bitstream bitstream : bundle.getBitstreams()) {
						if (isCancelled()) {
							if (console != null) {
								console.append("Cancelled " + prettyName() + ".\n");
							}
							return missingFiles;
						}

						URI source = bitstream.getSource();
						if (!source.isAbsolute() || source.getScheme().toString().equalsIgnoreCase("file")) {
							File file = new File(bitstream.getSource().getPath());

							if (!file.exists()) {
								Flag flag = new Flag(Flag.NOT_FOUND, "source file path was not found.", "local",
								        file.getAbsolutePath(), bitstream.getColumnLabel(), "" + bitstream.getRow(),
								        batch.getAction().toString());
								Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(),
								        generatesError(), "Source file path not found.", flag);
								missingFiles.add(missingFile);
								if (console != null) {
									console.append("\t" + missingFile.toString() + "\n");
								}
								if (flagPanel != null) {
									flagPanel.appendRow(flag);
								}
							}
						}
					}
				}

				if (isCancelled()) {
					if (console != null) {
						console.append("Cancelled " + prettyName() + ".\n");
					}
					return missingFiles;
				}

				itemCount++;
				publish(new VerifierBackground.VerifierUpdates(itemCount, totalItems));
			}
		}

		return missingFiles;
	}
}
