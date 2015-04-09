package edu.tamu.di.SAFCreator;



/**
 *
 */
public class App 
{
	static ImportDataProcessor processor = new ImportDataProcessorImpl();
	
	static ImporterGUI gui = new ImporterGUI(processor);
	
    public static void main( String[] args )
    {
    	gui.setSize(800,640);
    	gui.setResizable(false);
        gui.setVisible(true);
    }
}
