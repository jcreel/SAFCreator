package edu.tamu.di.SAFCreator.verify;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextArea;

import edu.tamu.di.SAFCreator.Util;
import edu.tamu.di.SAFCreator.model.Batch;
import edu.tamu.di.SAFCreator.model.ColumnLabel;
import edu.tamu.di.SAFCreator.model.FieldLabel;
import edu.tamu.di.SAFCreator.model.Flag;
import edu.tamu.di.SAFCreator.model.FlagPanel;
import edu.tamu.di.SAFCreator.model.VerifierBackground;
import edu.tamu.di.SAFCreator.model.VerifierProperty;

public class ValidSchemaNameVerifierImpl extends VerifierBackground {

	public ValidSchemaNameVerifierImpl() {
		super();
	}

	public ValidSchemaNameVerifierImpl(VerifierProperty settings) {
		super(settings);
	}

	@Override
	public void doCancel() {
	}

	@Override
	public List<Problem> verify(Batch batch)
	{
		return verify(batch, null, null);
	}

	@Override
	public List<Problem> verify(Batch batch, JTextArea console, FlagPanel flagPanel)
	{
		List<Problem> badSchemata = new ArrayList<Problem>();

		int totalLabels = batch.getLabels().size();
		int labelCount = 0;
		for(ColumnLabel label : batch.getLabels())
		{
			if(label.isField())
			{
				FieldLabel fieldLabel = (FieldLabel) label;
				if(Util.regexMatchCounter(".*\\W+.*", fieldLabel.getSchema()) > 0)
				{
					Flag flag = new Flag(Flag.BAD_SCHEMA_NAME, "Bad schema name for row " + labelCount + ".", "", "", label.getColumnLabel(), "" + label.getRow(), batch.getAction());
					Problem badSchema = new Problem (label.getRow(), label.getColumnLabel(), generatesError(), "Bad schema name " + fieldLabel.getSchema());
					badSchema.setFlag(flag);
					batch.failedRow(label.getRow());
					badSchemata.add(badSchema);
					if (console != null) console.append("\t" + badSchemata.toString()+"\n");
					if (flagPanel != null) flagPanel.appendRow(flag);
				}
				else
				{
					//System.out.println("looks fine: " + fieldLabel.getSchema());
				}
			}

			if (isCancelled()) {
				return badSchemata;
			}

			labelCount++;
			publish(new VerifierBackground.VerifierUpdates(labelCount, totalLabels));
		}

		return badSchemata;
	}

	@Override
	public boolean generatesError()
	{
		return true;
	}

	@Override
	public String prettyName()
	{
		return "Syntactically Valid Schema Names Verifier";
	}
}
