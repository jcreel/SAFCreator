package edu.tamu.di.SAFCreator.verify;

import java.util.ArrayList;
import java.util.List;

import edu.tamu.di.SAFCreator.Util;
import edu.tamu.di.SAFCreator.model.Batch;
import edu.tamu.di.SAFCreator.model.ColumnLabel;
import edu.tamu.di.SAFCreator.model.FieldLabel;
import edu.tamu.di.SAFCreator.model.Verifier;

public class ValidSchemaNameVerifierImpl implements Verifier {

	public List<Problem> verify(Batch batch) 
	{
		List<Problem> badSchemata = new ArrayList<Problem>();
		
		for(ColumnLabel label : batch.getLabels())
		{
			if(label.isField())
			{
				FieldLabel fieldLabel = (FieldLabel) label;
				if(Util.regexMatchCounter(".*\\W+.*", fieldLabel.getSchema()) > 0)
				{
					System.out.println("match in " + fieldLabel.getSchema());
					Problem badSchema = new Problem (label.getRow(), label.getColumn(), generatesError(), "Bad schema name " + fieldLabel.getSchema());
					badSchemata.add(badSchema);
			
				}
				else
				{
					//System.out.println("looks fine: " + fieldLabel.getSchema());
				}
			}
		}
		
		return badSchemata;
	}

	public boolean generatesError() 
	{
		return true;
	}
	
	public String prettyName()
	{
		return "Syntactically Valid Schema Names Verifier";
	}

}
