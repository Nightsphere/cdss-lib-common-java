//------------------------------------------------------------------------------
// TSPropertiesJFrame - Frame to display a single time series' properties.
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
// 
// 21 Feb 2001	Steven A. Malers,	Initial version.  Copy
//		Riverside Technology,	TSViewPropertiesGUI and modify
//		inc.			as necessary.
// 2001-11-05	SAM, RTi		Clean up javadoc.  Set unused variables
//					to null for garbage collection.
// 2002-01-17	SAM, RTi		Change name of class from
//					TSPropertiesGUI to TSPropertiesFrame to
//					allow support for Swing version.
// 2002-02-11	SAM, RTi		Change to use TabbedPane for properties.
// 2002-04-05	SAM, RTi		Add Print button for use with the
//					history.
// 2002-05-28	SAM, RTi		Update tabSelected() call to agree with
//					code update.
// =================================
// 2002-11-12	SAM, RTi		Copy AWT version and update to use
//					Swing.
// 2003-06-04	SAM, RTi		* Update for latest Swing GR and TS
//					  changes.
//					* Add JScrollPane around JTextArea.
//					* Corrected some addComponent()
//					  parameters that were inconsistent.
// 2003-09-30	SAM, RTi		* Use the icon/title from the main
//					  application.
// 2003-11-21	SAM, RTi		* Enable printing of the comments and
//					  history text areas.
// 2004-01-31	SAM, RTi		* Fix bug where null time series dates
//					  was causing an exception in
//					  initialization.
// 2004-03-16	SAM, RTi		* Add checkboxes under the general tab
//					  for whether the time series is
//					  selected, and dirty.
// 2004-05-21	SAM, RTi		* Handle null dates better.
// 2005-04-27	J. Thomas Sapienza, RTi	Added all data members to finalize().
// 2007-05-08	SAM, RTi		Cleanup code based on Eclipse feedback.
//------------------------------------------------------------------------------
// EndHeader

package RTi.GRTS;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import RTi.TS.MonthTS;
import RTi.TS.MonthTSLimits;
import RTi.TS.TS;
import RTi.TS.TSUtil;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.IO.PrintJGUI;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.TimeInterval;

/**
The TSPropertiesJFrame displays properties for a time series, including
information from the TSIdent and also basic statistics from TSLimits.  The
properties are typically shown from a parent JFrame window.
*/
public class TSPropertiesJFrame extends JFrame
implements ActionListener, ChangeListener, WindowListener
{

// Private data...

private TS __ts;		// Time series to display
private SimpleJButton __print_JButton;
				// Print button to be enabled only with the
				// History tab.
private JTabbedPane __props_JTabbedPane;
				// Tabbed pane to manage panels with properties.
private JTextArea __history_JTextArea;
				// JTextArea for history tab.
private JTextArea __comments_JTextArea;
				// JTextArea for comments tab.
private JPanel __history_JPanel;// Panel for time series history
private JPanel __comments_JPanel;// Panel for time series comments

/**
Construct a TSPropertiesJFrame.
@param gui Parent JFrame.  Currently this is ignored and can be set to null.
@param ts Time series for which to display properties.
@exception Exception if there is an error displaying properties.
*/
public TSPropertiesJFrame ( JFrame gui, TS ts )
throws Exception
{	super ( "Time Series Properties" );
	__ts = ts;
	JGUIUtil.setIcon ( this, JGUIUtil.getIconImage() );
	openGUI ( true );
}

/**
Handle action events (button press, etc.)
@param e ActionEvent to handle.
*/
public void actionPerformed ( ActionEvent e )
{	String command = e.getActionCommand();
	if ( command.equals("Close") ) {
		JGUIUtil.close(this);
	}
	else if ( command.equals("Print") ) {
		try {	//PrintJGUI.print ( this,
			//JGUIUtil.toVector(__history_JTextArea), null, 8 );
			if (	__props_JTabbedPane.getSelectedComponent() ==
				__comments_JPanel ) {
				PrintJGUI.printJTextAreaObject(this, null,
				__comments_JTextArea);
			}
			else if ( __props_JTabbedPane.getSelectedComponent() ==
				__history_JPanel ) {
				PrintJGUI.printJTextAreaObject(this, null,
				__history_JTextArea);
			}
		}
		catch ( Exception ex ) {
			Message.printWarning ( 1,
			"TSPropertiesJFrame.actionPerformed",
			"Error printing." );
			Message.printWarning ( 2,
				"TSPropertiesJFrame.actionPerformed", ex );
		}
	}
	command = null;
}

/**
Clean up before garbage collection.
@exception Throwable if there is an error.
*/
protected void finalize ()
throws Throwable
{	__ts = null;
	__props_JTabbedPane = null;
	__comments_JTextArea = null;
	__history_JTextArea = null;
	__print_JButton = null;
	__history_JPanel = null;
	__comments_JPanel = null;
	super.finalize();
}

/**
Open the properties GUI.
@param mode Indicates whether the GUI is visible at creation.
*/
private void openGUI ( boolean mode )
{	String	routine = "TSViewPropertiesJFrame.openGUI";

	// Start a big try block to set up the GUI...
	try {

	// Add a listener to catch window manager events...

	addWindowListener ( this );

	GridBagLayout gbl = new GridBagLayout();

	Insets insetsTLBR = new Insets ( 2, 2, 2, 2 );	// space around text
							// area
	
	// Font for reports (fixed width)...

	Font report_Font = new Font ( "Courier", Font.PLAIN, 11 );
	
	// Add a panel to hold the main components...

	JPanel display_JPanel = new JPanel ();
	display_JPanel.setLayout ( gbl );
	getContentPane().add ( display_JPanel );

	__props_JTabbedPane = new JTabbedPane ();
	__props_JTabbedPane.addChangeListener ( this );
	JGUIUtil.addComponent ( display_JPanel, __props_JTabbedPane,
			0, 0, 10, 1, 1.0, 1.0,
			insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.CENTER );

	//
	// General Tab...
	//

	JPanel general_JPanel = new JPanel();
	general_JPanel.setLayout ( gbl );
	__props_JTabbedPane.addTab ( "General", null, general_JPanel,
		"General properties" );

	int y = 0;
	JGUIUtil.addComponent ( general_JPanel, new JLabel("Identifier:"),
			0, y, 1, 1, 0.0, 0.0,
			insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST );
	JTextField identifier_JTextField = new JTextField(
			__ts.getIdentifierString(), 30);
	identifier_JTextField.setEditable ( false );
	JGUIUtil.addComponent ( general_JPanel, identifier_JTextField,
			1, y, 6, 1, 0.0, 0.0,
			insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	identifier_JTextField = null;

	JGUIUtil.addComponent ( general_JPanel, new JLabel(
			"Identifier (with input):"),
			0, ++y, 1, 1, 0.0, 0.0,
			insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST );
	// Limit the length of this field...
	JTextField input_JTextField = new JTextField(
			__ts.getIdentifier().toString(true), 50 );
	input_JTextField.setEditable ( false );
	JGUIUtil.addComponent ( general_JPanel, input_JTextField,
			1, y, 6, 1, 1.0, 0.0,
			insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );

	JGUIUtil.addComponent ( general_JPanel, new JLabel("Alias:"),
			0, ++y, 1, 1, 0.0, 0.0,
			insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST );
	JTextField alias_JTextField = new JTextField(
			__ts.getAlias(), 30 );
	alias_JTextField.setEditable ( false );
	JGUIUtil.addComponent ( general_JPanel, alias_JTextField,
			1, y, 2, 1, 0.0, 0.0,
			insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	alias_JTextField = null;

	JGUIUtil.addComponent ( general_JPanel, new JLabel("Sequence Number:"),
			0, ++y, 1, 1, 0.0, 0.0,
			insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST );
	JTextField seqnum_JTextField = new JTextField(
			"" + __ts.getSequenceNumber(), 5);
	seqnum_JTextField.setEditable ( false );
	JGUIUtil.addComponent ( general_JPanel, seqnum_JTextField,
			1, y, 2, 1, 0.0, 0.0,
			insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	alias_JTextField = null;

	JGUIUtil.addComponent ( general_JPanel, new JLabel("Description:"),
			0, ++y, 1, 1, 0.0, 0.0,
			insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST );
	// Set a maximum size so this does not get outragously big...
	JTextField description_JTextField=new JTextField(
		__ts.getDescription(),50);
	description_JTextField.setEditable ( false );
	JGUIUtil.addComponent ( general_JPanel, description_JTextField,
			1, y, 6, 1, 1.0, 0.0,
			insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
	description_JTextField = null;

	JGUIUtil.addComponent ( general_JPanel, new JLabel("Units (Current):"),
			0, ++y, 1, 1, 0.0, 0.0,
			insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST );
	JTextField units_JTextField = new JTextField( __ts.getDataUnits(), 10);
	units_JTextField.setEditable ( false );
	JGUIUtil.addComponent ( general_JPanel, units_JTextField,
			1, y, 1, 1, 0.0, 0.0,
			insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	units_JTextField = null;

	JGUIUtil.addComponent ( general_JPanel, new JLabel("Units (Original):"),
			0, ++y, 1, 1, 0.0, 0.0,
			insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST );
	JTextField unitsorig_JTextField =
			new JTextField( __ts.getDataUnitsOriginal(), 10);
	unitsorig_JTextField.setEditable ( false );
	JGUIUtil.addComponent ( general_JPanel, unitsorig_JTextField,
			1, y, 1, 1, 0.0, 0.0,
			insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	unitsorig_JTextField = null;

	JCheckBox isselected_JCheckBox =
		new JCheckBox ( "Is Selected", __ts.isSelected() );
	isselected_JCheckBox.setEnabled ( false );
	JGUIUtil.addComponent ( general_JPanel, isselected_JCheckBox,
			1, ++y, 1, 1, 1.0, 0.0,
			insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
	isselected_JCheckBox = null;

	JCheckBox isdirty_JCheckBox =
		new JCheckBox ( "Is Dirty", __ts.isDirty() );
	isdirty_JCheckBox.setEnabled ( false );
	JGUIUtil.addComponent ( general_JPanel, isdirty_JCheckBox,
			1, ++y, 1, 1, 1.0, 0.0,
			insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
	isdirty_JCheckBox = null;

	//
	// Comments Tab...
	//

	__comments_JPanel = new JPanel();
	__comments_JPanel.setLayout ( gbl );
	__props_JTabbedPane.addTab ( "Comments", null, __comments_JPanel,
		"Comments" );

	y = 0;
	__comments_JTextArea = new JTextArea(
			StringUtil.toString( __ts.getComments(),
			System.getProperty("line.separator")),5,80);
	__comments_JTextArea.setFont ( report_Font );
	__comments_JTextArea.setEditable ( false );
	JGUIUtil.addComponent ( __comments_JPanel,
			new JScrollPane (__comments_JTextArea),
			0, y, 6, 1, 1.0, 1.0,
			insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.CENTER );

	//
	// Period Tab...
	//

	JPanel period_JPanel = new JPanel();
	period_JPanel.setLayout ( gbl );
	__props_JTabbedPane.addTab ( "Period", null, period_JPanel, "Period" );

	y = 0;
	JGUIUtil.addComponent ( period_JPanel, new JLabel(
			"Current (reflects manipulation):"),
			0, y, 1, 1, 0.0, 0.0,
			insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST );
	JTextField period_JTextField = new JTextField( __ts.getDate1() + " to "+
		__ts.getDate2(), 30);
	period_JTextField.setEditable(false);
	JGUIUtil.addComponent ( period_JPanel, period_JTextField,
		1, y, 2, 1, 0.0, 0.0,
		insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	period_JTextField = null;

	JGUIUtil.addComponent ( period_JPanel, new JLabel(
		"Original (from input):"), 0, ++y, 1, 1, 0.0, 0.0,
		insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST );
	JTextField origperiod_JTextField =
		new JTextField( __ts.getDate1Original() +
		" to " + __ts.getDate2Original(), 30  );
	origperiod_JTextField.setEditable ( false );
	JGUIUtil.addComponent ( period_JPanel, origperiod_JTextField,
		1, y, 2, 1, 0.0, 0.0,
		insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	origperiod_JTextField = null;

	JGUIUtil.addComponent ( period_JPanel, new JLabel("Total Points:"),
			0, ++y, 1, 1, 0, 0,
			insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST );
	JTextField points_JTextField = new JTextField( "" + __ts.getDataSize());
	points_JTextField.setEditable ( false );
	JGUIUtil.addComponent ( period_JPanel, points_JTextField,
			1, y, 1, 1, 0.0, 0.0,
			insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	points_JTextField = null;

	//
	// Limits Tab...
	//

	JPanel limits_JPanel = new JPanel();
	limits_JPanel.setLayout ( gbl );
	__props_JTabbedPane.addTab ( "Limits", null, limits_JPanel, "Limits" );

	y = 0;
	JGUIUtil.addComponent ( limits_JPanel, new JLabel(
			"Current (reflects manipulation):"),
			0, y, 6, 1, 0.0, 0.0,
			insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	JTextArea limits_JTextArea = null;
	if ( __ts.getDataIntervalBase() == TimeInterval.MONTH ) {
		try {	limits_JTextArea = new JTextArea(
			new MonthTSLimits((MonthTS)__ts).toString(),12,80);
		}
		catch ( Exception e ) {
			limits_JTextArea = new JTextArea(
			"No Limits Available",5,80);
		}
	}
	else {	try {	limits_JTextArea = new JTextArea(
				(TSUtil.getDataLimits(__ts, __ts.getDate1(),
				__ts.getDate2())).toString(),15,80 );
		}
		catch ( Exception e ) {
			limits_JTextArea = new JTextArea(
			"No Limits Available",5,80);
		}
	}
	limits_JTextArea.setEditable ( false );
	limits_JTextArea.setFont ( report_Font );
	JGUIUtil.addComponent ( limits_JPanel,
			new JScrollPane ( limits_JTextArea ),
			0, ++y, 6, 1, 1.0, 1.0,
			insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.CENTER );
	limits_JTextArea = null;

	++y;
	JGUIUtil.addComponent(limits_JPanel,
			new JLabel("Original (from input):"),
			0, ++y, 6, 1, 0.0, 0.0,
			insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	JTextArea origlim_JTextArea = null;
	if ( __ts.getDataLimitsOriginal() == null ) {
		origlim_JTextArea = new JTextArea( "No Limits Available");
	}
	else {	origlim_JTextArea = new JTextArea(
			__ts.getDataLimitsOriginal().toString(),10,80);
		origlim_JTextArea.setFont ( report_Font );
		origlim_JTextArea.setEditable ( false );
	}
	origlim_JTextArea.setEditable(false);
	JGUIUtil.addComponent ( limits_JPanel,
			new JScrollPane ( origlim_JTextArea ),
			0, ++y, 6, 1, 1.0, 1.0,
			insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.CENTER );

	//
	// History Tab...
	//

	__history_JPanel = new JPanel();
	__history_JPanel.setLayout ( gbl );
	__props_JTabbedPane.addTab("History", null, __history_JPanel,"History");
	y = 0;
	__history_JTextArea = new JTextArea(
			StringUtil.toString(__ts.getGenesis(),
			System.getProperty("line.separator")),5,80);
	__history_JTextArea.setFont ( report_Font );
	__history_JTextArea.setEditable ( false );
	JGUIUtil.addComponent ( __history_JPanel,
			new JScrollPane (__history_JTextArea),
			0, y, 7, 1, 1.0, 1.0,
			insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.CENTER );

	JGUIUtil.addComponent ( __history_JPanel, new JLabel("Read From:"),
			0, ++y, 1, 1, 0.0, 0.0,
			insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	JTextField inputname_JTextField = new JTextField( __ts.getInputName());
	inputname_JTextField.setEditable ( false );
	JGUIUtil.addComponent ( __history_JPanel, inputname_JTextField,
			1, y, 6, 1, 1.0, 0.0,
			insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER );
	inputname_JTextField = null;

	//
	// Data Flags Tab...
	//

	JPanel dataflags_JPanel = new JPanel();
	dataflags_JPanel.setLayout ( gbl );
	__props_JTabbedPane.addTab ( "Data Flags", dataflags_JPanel );

	y = 0;
	JGUIUtil.addComponent ( dataflags_JPanel, new JLabel(
			"Missing Data Value:"), 0, y, 1, 1, 0.0, 0.0,
			insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST );
	JTextField missing_JTextField = new JTextField( StringUtil.formatString(
			__ts.getMissing(),"%.4f"), 15);
	missing_JTextField.setEditable(false);
	JGUIUtil.addComponent ( dataflags_JPanel, missing_JTextField,
			1, y, 1, 1, 0.0, 0.0,
			insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	missing_JTextField = null;

	JCheckBox hasdataflags_JCheckBox =
		new JCheckBox ( "Has Data Flags", __ts.hasDataFlags() );
	hasdataflags_JCheckBox.setEnabled ( false );
	JGUIUtil.addComponent ( dataflags_JPanel, hasdataflags_JCheckBox,
			0, ++y, 2, 1, 1.0, 0.0,
			insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
	hasdataflags_JCheckBox = null;

	// Put the buttons on the bottom of the window...

	JPanel button_JPanel = new JPanel ();
	button_JPanel.setLayout ( new FlowLayout(FlowLayout.CENTER) );

	button_JPanel.add ( new SimpleJButton("Close", "Close",this) );
	__print_JButton = new SimpleJButton("Print", "Print", this );
	__print_JButton.setEnabled ( false );
	button_JPanel.add ( __print_JButton );

	getContentPane().add ( "South", button_JPanel );
	button_JPanel = null;

	if (	(JGUIUtil.getAppNameForWindows() == null) ||
		JGUIUtil.getAppNameForWindows().equals("") ) {
		setTitle ( __ts.getIdentifier().toString() + " - Properties" );
	}
	else {	setTitle( JGUIUtil.getAppNameForWindows() +
		" - " + __ts.getIdentifier().toString() + " - Properties" );
	}

	pack ();
	JGUIUtil.center ( this );
	setResizable ( false );
	setVisible ( mode );
	// Clean up...
	gbl = null;
	insetsTLBR = null;
	display_JPanel = null;
	//SAMX
	//report_Font = null;
	} // end of try
	catch ( Exception e ) {
		Message.printWarning ( 2, routine, e );
	}
	routine = null;
}

/**
React to tab selections.  Currently all that is done is the Print button
is enabled or disabled.
@param e the ChangeEvent that happened.
*/
public void stateChanged ( ChangeEvent e )
{	// Check for null because events are sometimes generated at startup...
	if (	(__props_JTabbedPane.getSelectedComponent()==__history_JPanel)||
		(__props_JTabbedPane.getSelectedComponent()==
		__comments_JPanel)){
		JGUIUtil.setEnabled ( __print_JButton, true );
	}
	else {	JGUIUtil.setEnabled ( __print_JButton, false );
	}
}

// WindowListener functions...

public void windowActivated( WindowEvent evt )
{
}

public void windowClosed( WindowEvent evt )
{
}

/**
Close the GUI.
*/
public void windowClosing( WindowEvent event )
{	JGUIUtil.close( this);
}

public void windowDeactivated( WindowEvent evt )
{
}

public void windowDeiconified( WindowEvent evt )
{
}

public void windowOpened( WindowEvent evt )
{
}

public void windowIconified( WindowEvent evt )
{
}

} // End of TSPropertiesJFrame