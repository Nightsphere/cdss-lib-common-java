package riverside.datastore;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import RTi.Util.GUI.JGUIUtil;

/**
This class is a frame that displays data store information.  Currently data store information cannot be edited.
*/
public class DataStores_JFrame extends JFrame
{

/**
The data table that was passed in.
*/
private List<DataStore> __dataStoreList = null;

/**
The panel containing the worksheet that will be displayed in the frame.
*/
private DataStores_JPanel __dataTablePanel = null;

/**
Message bar text fields.
*/
private JTextField
	__messageJTextField,
	__statusJTextField;

/**
Constructor.
@param title the title to put on the frame.
@param dataStoreList the list of data stores to display in the worksheet.
@throws Exception if table is null.
*/
public DataStores_JFrame(String title, List<DataStore> dataStoreList) 
throws Exception
{	JGUIUtil.setIcon ( this, JGUIUtil.getIconImage() );
	if ( title == null ) {
		if ( (JGUIUtil.getAppNameForWindows() == null) || JGUIUtil.getAppNameForWindows().equals("") ) {
			setTitle ( "Table" );
		}
		else {
            setTitle( JGUIUtil.getAppNameForWindows() +	" - Table" );
		}
	}
	else {
        if ( (JGUIUtil.getAppNameForWindows() == null) || JGUIUtil.getAppNameForWindows().equals("") ) {
			setTitle ( title );
		}
		else {
            setTitle( JGUIUtil.getAppNameForWindows() +	" - " + title );
		}
	}
	__dataStoreList = dataStoreList;
	
	setupGUI();
}

/**
Cleans up member variables.
*/
public void finalize()
throws Throwable {
	__dataTablePanel = null;
	__messageJTextField = null;
	__statusJTextField = null;
	super.finalize();
}

/**
Sets the status bar's message and status text fields.
@param message the value to put into the message text field.
@param status the value to put into the status text field.
*/
public void setMessageStatus(String message, String status) {
	if (message != null) {
		__messageJTextField.setText(message);
	}
	if (status != null) {
		__statusJTextField.setText(status);
	}
}

/**
Sets up the GUI.
*/
private void setupGUI() 
throws Exception
{
	__dataTablePanel = new DataStores_JPanel(this, __dataStoreList);

	getContentPane().add("Center", __dataTablePanel);

	JPanel statusBar = new JPanel();
	statusBar.setLayout(new GridBagLayout());

	__messageJTextField = new JTextField(20);
	__messageJTextField.setEditable(false);
	__statusJTextField = new JTextField(10);
	__statusJTextField.setEditable(false);

	JGUIUtil.addComponent(statusBar, __messageJTextField,
		0, 0, 1, 1, 1, 1,
		GridBagConstraints.BOTH, GridBagConstraints.WEST);
	JGUIUtil.addComponent(statusBar, __statusJTextField,
		1, 0, 1, 1, 0, 0,
		GridBagConstraints.NONE, GridBagConstraints.WEST);
	getContentPane().add("South", statusBar);

	setSize(600, 400);
	JGUIUtil.center(this);

	int count = __dataTablePanel.getWorksheetRowCount();
	String plural = "s";
	if (count == 1) {
		plural = "";
	}
	int count_col = __dataTablePanel.getWorksheetColumnCount();
	String plural_col = "s";
    if (count_col == 1) {
        plural_col = "";
    }

	setMessageStatus("Displaying " + count + " row" + plural +
	        ", " + count_col + " column" + plural_col + ".", "Ready");

	setVisible(true);

	__dataTablePanel.setWorksheetColumnWidths();
}

}