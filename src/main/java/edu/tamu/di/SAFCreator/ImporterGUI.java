package edu.tamu.di.SAFCreator;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.EtchedBorder;

import edu.tamu.di.SAFCreator.model.Batch;
import edu.tamu.di.SAFCreator.model.Verifier;
import edu.tamu.di.SAFCreator.verify.FilesExistVerifierImpl;
import edu.tamu.di.SAFCreator.verify.ValidSchemaNameVerifierImpl;


public class ImporterGUI extends JFrame 
{
	private Batch batch;
	
	private List<Verifier> verifiers = new ArrayList<Verifier>();
	
	private enum ActionStatus {NONE_LOADED, LOADED, FAILED_VERIFICATION, VERIFIED, WRITTEN};
	private ActionStatus actionStatus = ActionStatus.NONE_LOADED;
	
	private static final long serialVersionUID = 1L;
	
	//tabbed views
	private final JTabbedPane tabs = new JTabbedPane();
	private final JPanel mainTab = new JPanel();
	private final JPanel licenseTab = new JPanel();
	private final JPanel validationTab = new JPanel();
	
	//Components of the Batch Detail tab
	private final JButton chooseInputFileBtn = new JButton("Select metadata CSV file");
	private final JButton chooseSourceDirectoryBtn = new JButton("Select source files directory");
	private final JButton chooseOutputDirectoryBtn = new JButton("Select SAF output directory");
	private final JFileChooser inputFileChooser = new JFileChooser(".");
	private final JFileChooser sourceDirectoryChooser = new JFileChooser(".");
	private final JFileChooser outputDirectoryChooser = new JFileChooser(".");;
	private final JTextField inputFileNameField = new JTextField("", 42);
	private final JTextField sourceDirectoryNameField = new JTextField("", 40);
	private final JTextField outputDirectoryNameField = new JTextField("", 40);
	private final JTextField actionStatusField = new JTextField("Please load a batch for processing.");
	private final JButton loadBatchBtn = new JButton("Load specified batch now!");
	private final JButton writeSAFBtn = new JButton("No batch loaded.");
	
	//Components of the License tab
	private final JPanel addLicenseFilePanel = new JPanel();
	private final JCheckBox addLicenseCheckbox = new JCheckBox("Add a license:");
	private final JLabel licenseFilenameFieldLabel = new JLabel("License bitstream filename:");
	private final JTextField licenseFilenameField = new JTextField("license.txt", 39);
	private final JLabel licenseBundleNameFieldLabel = new JLabel("License bundle name:");
	private final JTextField licenseBundleNameField = new JTextField("LICENSE", 42);
	//private final JLabel licenseTextFieldLabel = new JLabel("License text:");
	private final JTextArea licenseTextField = new JTextArea("This item is hereby licensed.", 6, 0);
	private final JScrollPane licenseTextScrollPane = new JScrollPane(licenseTextField);
	private final JCheckBox restrictToGroupCheckbox = new JCheckBox("Restrict read access to a group - Group name:");
	private final JTextField restrictToGroupField = new JTextField("member", 36);
	
	
	//Components of the Verify Batch tab
	private final JButton verifyBatchBtn = new JButton("Verify Batch");
	private JTable verifierTbl = null;
	
	
	//Components shown under any tab
	private final JPanel statusPanel = new JPanel();
	private final JTextArea statusIndicator = new JTextArea("No batch loaded.", 2, 10);
	private final JTextArea console = new JTextArea(20, 50);
	private final JScrollPane scrollPane;
	
	private String metadataInputFileName;
	private static String sourceDirectoryName;
	private static String outputDirectoryName;
	
	private final ImportDataProcessor processor;
	
	
	public ImporterGUI(final ImportDataProcessor processor)
	{	
		this.processor=processor;
		
		verifiers.add(new FilesExistVerifierImpl());
		verifiers.add(new ValidSchemaNameVerifierImpl());
		
		createBatchDetailsTab();
		
		createLicenseTab();
		
		createVerificationTab();
		
		this.setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		
//		JMenuBar bar = new JMenuBar();
//		bar.add(new JMenu("menu"));
//		this.setJMenuBar(bar);
		
		//add the tabbed views
		getContentPane().add(tabs);
		
		
		//add the status info area present under all tabs
		console.setEditable(false);
		console.setLineWrap(true);
		scrollPane = new JScrollPane(console);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		
		statusIndicator.setBackground(Color.blue);
		statusIndicator.setForeground(Color.white);
		statusIndicator.setEditable(false);
		statusIndicator.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
		scrollPane.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
		
		statusPanel.add(statusIndicator);
		statusPanel.add(scrollPane);
		getContentPane().add(statusPanel);
		
		
		initializeGUIState();
		
		
		this.pack();
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setTitle("DSpace Simple Archive Format Creator");
	
	}


	private void createVerificationTab() 
	{
		validationTab.setLayout(new BoxLayout(validationTab, BoxLayout.Y_AXIS));		
	
		//verifierTbl = new JTable(new VerifierTableModel());
		
		Vector<String>columnNames = new Vector<String>();
		Vector<Vector<Object>>rowData = new Vector<Vector<Object>>();
		
		columnNames.add("Verifier");
		columnNames.add("Generates Errors");
		
		for(Verifier verifier : verifiers)
		{
			//System.out.println("Adding verifier  " + verifier.getClass().getSimpleName());
			Vector<Object> row = new Vector<Object>();
			
			row.add(verifier.prettyName());
			row.add(verifier.generatesError());
			
			rowData.add(row);
		}
		
		
		verifierTbl = new JTable(rowData, columnNames);
		verifierTbl.setPreferredScrollableViewportSize(new Dimension(400, 50));
		verifierTbl.setEnabled(false);
		
		
		JScrollPane verifierTblScrollPane = new JScrollPane(verifierTbl);
		verifierTbl.setFillsViewportHeight(true);
		
		validationTab.add(verifierTblScrollPane);
		
		JPanel verifyBatchBtnPanel = new JPanel();
		verifyBatchBtnPanel.add(verifyBatchBtn);
		validationTab.add(verifyBatchBtnPanel);
		
		
	
		tabs.addTab("Batch Verification", validationTab);
		
		verifyBatchBtn.addActionListener
		(
			new ActionListener()
			{

				public void actionPerformed(ActionEvent e) 
				{
					if(actionStatus == ActionStatus.LOADED)
					{
					
						boolean verified=true;
						
						List<Verifier.Problem> problems = new ArrayList<Verifier.Problem>();
						for(Verifier verifier : verifiers)
						{
							problems.addAll(verifier.verify(batch));
						}
						
						for(Verifier.Problem problem : problems)
						{
							if(problem.isError()) verified = false;
							console.append(problem.toString()+"\n");
						}
					
						
						if(verified)
						{
							transitionToVerifySuccess();
						}
						else
						{
							transitionToVerifyFailed();
						}
					}
					else
					{
						console.append("Nothing new to verify.\n");
					}
				}
			}
		);
	}


	private void createLicenseTab() {
		
		licenseTab.setLayout(new BoxLayout(licenseTab, BoxLayout.Y_AXIS));
		
		addLicenseFilePanel.setLayout(new BoxLayout(addLicenseFilePanel, BoxLayout.Y_AXIS));
		
		
		addLicenseFilePanel.add(addLicenseCheckbox);
		
		JPanel licenseFilenameLine = new JPanel();
		licenseFilenameLine.add(licenseFilenameFieldLabel);
		licenseFilenameLine.add(licenseFilenameField);
		addLicenseFilePanel.add(licenseFilenameLine);
		
		JPanel licenseBundleNameLine = new JPanel();
		licenseBundleNameLine.add(licenseBundleNameFieldLabel);
		licenseBundleNameLine.add(licenseBundleNameField);
		addLicenseFilePanel.add(licenseBundleNameLine);
		
		
		licenseTextScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		addLicenseFilePanel.add(licenseTextScrollPane);
		
		JPanel restrictToGroupPanel = new JPanel();
		restrictToGroupPanel.add(restrictToGroupCheckbox);
		restrictToGroupPanel.add(restrictToGroupField);
		
		//TODO:
		//radio button complex for picking CC license
		//JPanel addCCLicensePanel = new JPanel();
		
		
		licenseTab.add(addLicenseFilePanel);
		licenseTab.add(restrictToGroupPanel);
		
		addLicenseCheckbox.addActionListener
		(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e) 
				{
					if(!actionStatus.equals(ActionStatus.NONE_LOADED) && !actionStatus.equals(ActionStatus.WRITTEN))
					{
						if(!addLicenseCheckbox.isSelected())
						{
							//user clicked to disable the license
							console.append("License disabled.\n");
							licenseTextField.setEditable(true);
							batch.unsetLicense();
						}
						else if(addLicenseCheckbox.isSelected())
						{
							//user clicked to enable the license
							console.append("License enabled.\n");
							licenseTextField.setEditable(false);
							batch.setLicense(licenseFilenameField.getText(), licenseBundleNameField.getText(), licenseTextField.getText());
						}
					}
					else
					{
						addLicenseCheckbox.setSelected(false);
						console.append("No active batch on which to change a license.\n");
					}
				}
			}
		);
		
		licenseTextField.addMouseListener
		(
			new MouseListener()
			{
				public void mouseClicked(MouseEvent e) 
				{
					if(!licenseTextField.isEditable())
					{
						console.append("License affixed to items - disable license to edit the license text.\n");
					}
				}

				public void mousePressed(MouseEvent e) {}

				public void mouseReleased(MouseEvent e) {}

				public void mouseEntered(MouseEvent e) {}

				public void mouseExited(MouseEvent e) {}
			}
		);
		
		
		restrictToGroupCheckbox.addActionListener
		(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e) 
				{
					if(!actionStatus.equals(ActionStatus.NONE_LOADED) && !actionStatus.equals(ActionStatus.WRITTEN))
					{
						if(!restrictToGroupCheckbox.isSelected())
						{
							//user clicked to disable the restriction
							console.append("Group restriction disabled.\n");
							restrictToGroupField.setEditable(true);
							batch.restrictItemsToGroup(null);
							
						}
						else if(restrictToGroupCheckbox.isSelected())
						{
							//user clicked to enable the restriction
							console.append("Group restriction to \"" + restrictToGroupField.getText() + "\" enabled.\n");
							restrictToGroupField.setEditable(false);
							batch.restrictItemsToGroup(restrictToGroupField.getText());
						}
					}
					else
					{
						restrictToGroupCheckbox.setSelected(false);
						console.append("No active batch on which to apply group restrictions.\n");
					}
				}
			}
		);
		
		restrictToGroupField.addMouseListener
		(
			new MouseListener()
			{
				public void mouseClicked(MouseEvent e) 
				{
					if(!restrictToGroupField.isEditable())
					{
						console.append("Restriction to group \"" + restrictToGroupField.getText() + "\" affixed to items - disable restriction to edit the group name.\n");
					}
				}

				public void mousePressed(MouseEvent e) {}

				public void mouseReleased(MouseEvent e) {}

				public void mouseEntered(MouseEvent e) {}

				public void mouseExited(MouseEvent e) {}
			}
		);
		
		
		tabs.addTab("License Settings", licenseTab);
		
		
	}


	private void createBatchDetailsTab() 
	{
		sourceDirectoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		outputDirectoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		metadataInputFileName = "";
		sourceDirectoryName = "";
		outputDirectoryName = "";
//		metadataInputFileName = "/Users/jcreel/Development/SAF/testo.csv";
//		sourceDirectoryName = "/Users/jcreel/Development/SAF/testfiles";
//		outputDirectoryName = "/Users/jcreel/Development/SAF/test-output";
//		
		
		inputFileNameField.setText(metadataInputFileName);
		inputFileNameField.setEditable(false);
		sourceDirectoryNameField.setText(sourceDirectoryName);
		sourceDirectoryNameField.setEditable(false);
		outputDirectoryNameField.setText(outputDirectoryName);
		outputDirectoryNameField.setEditable(false);
		
		
		mainTab.setLayout(new BoxLayout(mainTab, BoxLayout.Y_AXIS));
		
		JPanel inputCSVPanel = new JPanel();
		inputCSVPanel.add(chooseInputFileBtn);
		inputCSVPanel.add(inputFileNameField);
		mainTab.add(inputCSVPanel);
		
		JPanel sourceFilesDirPanel = new JPanel();
		sourceFilesDirPanel.add(chooseSourceDirectoryBtn);
		sourceFilesDirPanel.add(sourceDirectoryNameField);
		mainTab.add(sourceFilesDirPanel);
		
		JPanel outputSAFDirPanel = new JPanel();
		outputSAFDirPanel.add(chooseOutputDirectoryBtn);
		outputSAFDirPanel.add(outputDirectoryNameField);
		mainTab.add(outputSAFDirPanel);
		
		JPanel writeButtonPanel = new JPanel();
		actionStatusField.setEditable(false);
		
		writeButtonPanel.add(loadBatchBtn);
		writeButtonPanel.add(actionStatusField);
		writeButtonPanel.add(writeSAFBtn);
		mainTab.add(writeButtonPanel);
		
		tabs.addTab("Batch Details", mainTab);
		
		
		chooseInputFileBtn.addActionListener
		( 
			new ActionListener()
			{
			    public void actionPerformed( final ActionEvent e )
			    {   
			        if( inputFileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION )
			        {
			        	console.append("You have selected this input CSV file: " + inputFileChooser.getSelectedFile().getName() + ".\n");
			        	metadataInputFileName = inputFileChooser.getSelectedFile().getAbsolutePath();
			        	inputFileNameField.setText(metadataInputFileName);
			        }
			    }
			}
		);
		
		
		chooseSourceDirectoryBtn.addActionListener
		(
			new ActionListener()
			{
			    public void actionPerformed( final ActionEvent e )
			    {
			    	if( sourceDirectoryChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION )
			    	{
			    		console.append("You have selected this source directory: " + sourceDirectoryChooser.getSelectedFile().getName() + "\n");
			    		sourceDirectoryName = sourceDirectoryChooser.getSelectedFile().getAbsolutePath();
			    		sourceDirectoryNameField.setText(sourceDirectoryName);			    		
			    	}
			    }
			}
		);
		
		
		chooseOutputDirectoryBtn.addActionListener
		(
			new ActionListener()
			{
			    public void actionPerformed( final ActionEvent e )
			    {
			    	if( outputDirectoryChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION )
			    	{
			    		console.append("You have selected this target directory: " + outputDirectoryChooser.getSelectedFile().getName() + "\n");
			    		outputDirectoryName = outputDirectoryChooser.getSelectedFile().getAbsolutePath();
			    		outputDirectoryNameField.setText(outputDirectoryName);			    		
			    	}
			    }
			}
		);
		
		
		loadBatchBtn.addActionListener
		(
			new ActionListener()
			{
				public void actionPerformed( final ActionEvent e )
				{
					if(actionStatus.equals(ActionStatus.NONE_LOADED) || 
					   actionStatus.equals(ActionStatus.LOADED) || 
					   actionStatus.equals(ActionStatus.WRITTEN) || 
					   actionStatus.equals(ActionStatus.FAILED_VERIFICATION))
					{
						//Attempt to load the batch and set status to LOADED if successful, NONE_LOADED if failing
						console.append("Loading batch for " + metadataInputFileName + "...");
						batch = processor.loadBatch(metadataInputFileName, sourceDirectoryName, outputDirectoryName, console);
						if(batch != null)
						{
							console.append("success.\n");
						}
						else
						{
							console.append("\nFAILED TO READ BATCH.\n\n");
							actionStatus = ActionStatus.NONE_LOADED;
							
							statusIndicator.setText("No batch loaded.");
							statusIndicator.setForeground(Color.white);
							statusIndicator.setBackground(Color.blue);
							
							statusIndicator.setText("Please load a batch for processing.");
							actionStatusField.setForeground(Color.white);
							actionStatusField.setBackground(Color.blue);
							
							return;
						}
						
						transitionToLoaded();
					}										
				}
			}
		);		
						
		
		writeSAFBtn.addActionListener
		(
			new ActionListener()
			{
				public void actionPerformed( final ActionEvent e )
				{
	
					if(actionStatus.equals(ActionStatus.VERIFIED))
					{
						//Write the batch and set status to WRITTEN
						console.append("Parsing CSV file and writing output to DC XML files...\n");
						processor.writeBatchSAF(batch, console);
						
						transitionToWritten();
					}
					else if(actionStatus.equals(ActionStatus.LOADED) || actionStatus.equals(ActionStatus.FAILED_VERIFICATION))
					{
						console.append("Please verify the batch before writing.\n");
					}
					else if(actionStatus.equals(ActionStatus.WRITTEN) || actionStatus.equals(ActionStatus.NONE_LOADED))
					{
						//nothing to do
						console.append("No new SAF data to write.\n");
					}
				}
			}
		);
	}
	
	
	private void initializeGUIState()
	{
		actionStatus = ActionStatus.NONE_LOADED;
		batch=null;
		
		//actionStatusfield
		actionStatusField.setForeground(Color.white);
		actionStatusField.setBackground(Color.blue);
		
		//loadBatchBtn
		loadBatchBtn.setText("Load specified batch now!");
		
		//writeSAFBtn
		writeSAFBtn.setText("No batch loaded.");
		
		//statusIndicator
		statusIndicator.setForeground(Color.white);
		statusIndicator.setBackground(Color.blue);
		statusIndicator.setText("No batch loaded.");
		
	}
	
	private void transitionToLoaded()
	{
		actionStatusField.setText("Your batch nas not been verified.");
		actionStatusField.setForeground(Color.black);
		actionStatusField.setBackground(Color.red);
		actionStatus = ActionStatus.LOADED;
		statusIndicator.setText("Batch Status:\n Unverified");
		statusIndicator.setForeground(Color.white);
		statusIndicator.setBackground(Color.blue);
		loadBatchBtn.setText("Reload batch as specified");
		addLicenseCheckbox.setSelected(false);
		restrictToGroupCheckbox.setSelected(false);
		writeSAFBtn.setText("Verify batch before writing SAF");
	}
	

	private void transitionToVerifySuccess()
	{
		actionStatus = ActionStatus.VERIFIED;
		actionStatusField.setText("Your batch has been verified!");
		actionStatusField.setBackground(Color.green);

		statusIndicator.setText("Batch Status:\nVerified");
		statusIndicator.setForeground(Color.black);
		statusIndicator.setBackground(Color.green);
		
		writeSAFBtn.setText("Write SAF data now!");
	}
	
	private void transitionToVerifyFailed()
	{
		actionStatus = ActionStatus.FAILED_VERIFICATION;
		actionStatusField.setText("Your batch failed to verify.");
		writeSAFBtn.setText("No valid batch to write.");
		actionStatusField.setBackground(Color.red);

	}

	private void transitionToWritten()
	{

		loadBatchBtn.setText("Reload batch as specified.");
		writeSAFBtn.setText("Nothing to do - batch written.");
		actionStatusField.setForeground(Color.black);
		actionStatusField.setBackground(Color.white);
		actionStatusField.setText("Batch SAF writing finished.");
		statusIndicator.setText("Batch status:\n Written");
		statusIndicator.setBackground(Color.white);
		statusIndicator.setForeground(Color.black);
		actionStatus = ActionStatus.WRITTEN;
	}
	
}
