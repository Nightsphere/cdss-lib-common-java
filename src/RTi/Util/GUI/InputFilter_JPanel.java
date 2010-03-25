//-----------------------------------------------------------------------------
// InputFilter_JPanel - class to display and manage a Vector of InputFilter
//-----------------------------------------------------------------------------
// Copyright: See the COPYRIGHT file.
//-----------------------------------------------------------------------------
// History: 
//
// 2003-12-18	Steven A. Malers, RTi	Initial version, for use with software
//					that queries databases.
// 2004-05-19	SAM, RTi		Fix bug where a change in the operator
//					JComboBox was not causing the entry
//					component to show the proper component.
//					Actually - after review it seems OK.
// 2004-08-26	SAM, RTi		* Add toString() to return a string
//					  representation of the filter.
//					* Add addEventListener() to allow
//					  higher level code when to refresh
//					  based on a change in the filter panel.
// 2004-08-28	SAM, RTi		To facilitate this class being a base
//					class, modify setInputFilters() to
//					take the list of filters and PropList -
//					then it can be called separately.
// 2004-09-10	SAM, RTi		* Add getInput() method to return the
//					  input that has been entered for a
//					  requested item.
// 2005-01-06	J. Thomas Sapienza, RTi	Fixed bugs that were making it so that
//					single filters could not be displayed in
//					the panel.
// 2004-01-11	JTS, RTi		Added the property 
//					'NumWhereRowsToDisplay'.  If set, it 
//					specifies the number of rows to be 
//					displayed when the drop-down list is
//					opened on one of the combo boxes that
// 					lists all the fields that a Where clause
//					can operated on.
// 2005-01-12	JTS, RTi		Changed slightly the formatting of 
//					errors in checkInput() to be cleaner.
// 2005-01-31	JTS, RTi		* fillOperatorJComboBox() now takes 
//					  another parameter that specifies the
//					  constraints that should NOT appear in
//					  the combo box.
//					* When the input text field is created
//					  its editability is set according to
//					  the value stored in the filter in:
//					  isInputJTextFieldEditable().
// 2005-02-02	JTS, RTi		When the input JComboBoxes are made
//					they now have a parameter to set the 
//					number of visible rows.
// 2005-04-26	JTS, RTi		Added finalize().
// 2006-04-25	SAM, RTi		Minor change to better handle input
//					filter string at initialization - was
//					having problem with ";Matches;".
// 2007-05-08	SAM, RTi		Cleanup code based on Eclipse feedback.
//-----------------------------------------------------------------------------
// EndHeader

package RTi.Util.GUI;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;

import java.util.List;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJComboBox;

import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;

import RTi.Util.Message.Message;

import RTi.Util.String.StringUtil;

// TODO SAM 2004-05-20 - see limitation below.
/**
This class provides a JPanel to display a list of InputFilter.  The
constructor should indicate the number of input filter groups to be displayed,
where an input filter group lists all input filters.  For example, if three
input filters are specified (in the constructor or later with setInputFilters),
and NumFilterGroups=2 in the constructor properties, then two rows of input will
be shown, each with the "where" listing each input filter.  The
InputFilter_JPanel internally maintains components to properly display the
correct visible components based on user selections.
There is a limitation in that if an input filter has a list of string choices
and an operator like "Ends with" is chosen, the full list of string choices is
still displayed in the input.  Therefore, a substring cannot be operated on.  This
limitation needs to be removed in later versions of the software if it impacts functionality.
In addition to the fully functional input filter, a constructor is provided for a blank panel, and
a constructor is provided to display text in the panel.  These variations can be used as place holders
or to provide information to users.  Often the panels are added in the same layout position and made active
by setting visible under appropriate conditions.
*/
public class InputFilter_JPanel extends JPanel implements ItemListener
{

/**
Number of filter groups to display.  One filter group will list all filters. 
*/
private int __numFilterGroups = 0;

/**
List of InputFilter to display.  The original input filter that is supplied is copied for as many input
filter groups as necessary, with the InputFilter instances managing the input field components for the
specific filter.
*/
private List [] __inputFilterListArray = null;
	
/**
List of JComponent that display the "where" label for each filter group that is displayed.  This may be
a SimpleJComboBox or a JLabel, depending on how many filters are available.  Each item in the list
corresponds to a different input filter group.
*/
private List __whereComponentList = new Vector();

/**
List of the operator components between the where an input components, one SimpleJComboBox per input filter.
Each input filter group has a list of operators and the operators are reset as needed in the filter group.
*/
private List __operatorComponentList = null;

/**
Properties to control the display.
*/
private PropList __props = null;

/**
Text area to display text (if the text version of constructor is used).
*/
private JTextArea __textArea = null;

/**
Construct an input filter panel.  The setInputFilters() method must be called
at some point during initialization in the calling code.  Or, use an empty input filter
for cases where the filters do not apply (see also overloaded version with text).
*/
public InputFilter_JPanel ()
{	GridBagLayout gbl = new GridBagLayout();
	setLayout ( gbl );
}

/**
Construct an input filter panel that will include a text area for a message.
This version is used as a place holder with a message, with visibility being swapped with
standard (or empty input filter panel).
Use setText() to update the text that is displayed.
@param text text to display in the input filter panel.  Use \n to indicate newline characters.
*/
public InputFilter_JPanel ( String text )
{   GridBagLayout gbl = new GridBagLayout();
    setLayout ( gbl );
    __textArea = new JTextArea(text);
    Insets insetsNNNN = new Insets(0,0,0,0);
    JGUIUtil.addComponent(this, __textArea,
        0, 0, 1, 1, 0.0, 0.0, insetsNNNN,
        GridBagConstraints.BOTH, GridBagConstraints.CENTER);
}

/**
Construct an input filter panel.
@param inputFilters A list of InputFilter, to be displayed.
@param props Properties to control the input filter panel, as described in the setInputFilters() method.
*/
public InputFilter_JPanel ( List inputFilters, PropList props )
{	GridBagLayout gbl = new GridBagLayout();
	setLayout ( gbl );
	if ( props == null ) {
		props = new PropList ( "InputFilter" );
	}
	setInputFilters ( inputFilters, props );
}

/**
Add listeners for events generated in the input components in the input
filter.  The code must have implemented ItemListener and KeyListener.
@param component The component that wants to listen for events from InputFilter_JPanel components.
*/
public void addEventListeners ( Component component )
{	// Component is used above (instead of JComponent) because JDialog is not derived from JComponent.
	// Loop through the filter groups...
	boolean isKeyListener = false;
	boolean isItemListener = false;
	if ( component instanceof KeyListener ) {
		isKeyListener = true;
	}
	if ( component instanceof ItemListener ) {
		isItemListener = true;
	}
	SimpleJComboBox cb;
	for ( int ifg = 0; ifg < __numFilterGroups; ifg++ ) {
		// The where...
		if ( isItemListener ) {
			if ( __whereComponentList.get(ifg) instanceof SimpleJComboBox ) {
				cb = (SimpleJComboBox)__whereComponentList.get(ifg);
				cb.addItemListener ( (ItemListener)component );
			}
			// The operator...
			cb = (SimpleJComboBox)__operatorComponentList.get(ifg);
			cb.addItemListener ( (ItemListener)component );
		}
		// The input...
		int numFilters = 0;
		if ( __inputFilterListArray[ifg] != null ) {
			numFilters = __inputFilterListArray[ifg].size();
		}
		InputFilter filter = null;
		JComponent input_component;
		JTextField tf;
		for ( int ifilter = 0; ifilter < numFilters; ifilter++ ) {
			filter = (InputFilter)__inputFilterListArray[ifg].get(ifilter);
			input_component = filter.getInputComponent();
			if ( input_component instanceof JTextField ) {
				if ( isKeyListener ) {
					tf = (JTextField)input_component;
					tf.addKeyListener ( (KeyListener)component );
				}
			}
			else if ( isItemListener ) {
				// Combo box...
				cb = (SimpleJComboBox)input_component;
				cb.addItemListener ( (ItemListener)component );
			}
		}
	}
}

/**
Check the input for the current input filter selections.  For example, if an
input filter is for a text field, verify that the contents of the field are
appropriate for the type of input.
@param displayWarning If true, display a warning if there are errors in the
input.  If false, do not display a warning (the calling code should generally display a warning.
@return true if no errors occur or false if there are input errors.
*/
public boolean checkInput ( boolean displayWarning )
{	String warning = "\n";
	// Loop through the filter groups...
	InputFilter filter;
	String input; // Input string selected by user.
	String where; // Where label for filter selected by user.
	int inputType; // Input type for the filter.
	for ( int ifg = 0; ifg < __numFilterGroups; ifg++ ) {
		filter = getInputFilter ( ifg );
		where = filter.getWhereLabel();
		if ( where.equals("") ) {
			// Blank where indicates that filter is disabled...
			continue;
		}
		input = filter.getInput(false).trim();
		inputType = filter.getInputType();
		if ( inputType == StringUtil.TYPE_STRING ) {
			// Any limitations?  For now assume not.
		}
		else if ( (inputType == StringUtil.TYPE_DOUBLE) || (inputType == StringUtil.TYPE_FLOAT) ) {
			if ( !StringUtil.isDouble(input) ) {
				warning += "Input filter \"" + filter.getWhereLabel() +
				"\", input is not a number:  \"" + input + "\"" + "\n";
			}
		}
		else if	( inputType == StringUtil.TYPE_INTEGER ) {
			if ( !StringUtil.isInteger(input) ) {
				warning += "Input filter \"" + filter.getWhereLabel() +
				"\", input is not an integer:  \""+input + "\"" + "\n";
			}
		}
	}
	if ( warning.length() > 1 ) {
		if ( displayWarning ) {
			Message.printWarning ( 1, "InputFilter_JPanel.checkInput", warning );
		}
		return false;
	}
	else {
	    return true;
	}
}

/**
Clears all the selections the user has made to the combo boxes in the panel.
*/
public void clearInput() {
	SimpleJComboBox cb = null;
	for ( int ifg = 0; ifg < __numFilterGroups; ifg++ ) {
		if (__whereComponentList.get(ifg)
			instanceof SimpleJComboBox) {
			cb = (SimpleJComboBox)__whereComponentList.get(ifg);
			cb.select(0);
		}
	}
}

/**
Fill a SimpleJComboBox with the appropriate operators for the data type.
The previous contents are first removed.
@param cb the SimpleJComboBox to fill.
@param type See StringUtil.TYPE_*.
@param constraintsToRemove the constraints that should NOT appear in the 
combo box.  Can be null, if all the appropriate constraints should be shown.
*/
private void fillOperatorJComboBox ( SimpleJComboBox cb, int type, List constraintsToRemove )
{	if ( cb == null ) {
		return;
	}
	// Remove existing...
	cb.removeAll();
	if ( type == StringUtil.TYPE_STRING ) {
		cb.add ( InputFilter.INPUT_MATCHES );
		// TODO - add later
		//cb.add ( InputFilter.INPUT_ONE_OF );
		cb.add ( InputFilter.INPUT_STARTS_WITH );
		cb.add ( InputFilter.INPUT_ENDS_WITH );
		cb.add ( InputFilter.INPUT_CONTAINS );
	}
	else if ( (type == StringUtil.TYPE_DOUBLE) || (type == StringUtil.TYPE_FLOAT) ||
		(type == StringUtil.TYPE_INTEGER) ) {
		cb.add ( InputFilter.INPUT_EQUALS );
		// TODO - add later
		//cb.add ( InputFilter.INPUT_ONE_OF );
		//cb.add ( InputFilter.INPUT_BETWEEN );
		cb.add ( InputFilter.INPUT_LESS_THAN );
		cb.add ( InputFilter.INPUT_LESS_THAN_OR_EQUAL_TO );
		cb.add ( InputFilter.INPUT_GREATER_THAN );
		cb.add ( InputFilter.INPUT_GREATER_THAN_OR_EQUAL_TO );
	}

	// Remove any constraints that have been explicitly set to NOT appear in the combo box.
	if (constraintsToRemove != null) {
		int size = constraintsToRemove.size();
		for (int i = 0; i < size; i++) {
			cb.remove((String)constraintsToRemove.get(i));
		}
	}
	
	if (cb.getItemCount() > 0) {
		// Select the first one...
		cb.select ( 0 );
	}

	// TODO - need to handle "NOT" and perhaps null
}

/**
Cleans up member variables.
*/
public void finalize()
throws Throwable {
	IOUtil.nullArray(__inputFilterListArray);
	__whereComponentList = null;
	__operatorComponentList = null;
	__props = null;		
	super.finalize();
}

/**
Return the input that has been entered in the panel, for a requested parameter.
If the requested where_label is not selected in any of the input filters, a zero
length vector will be returned.
Currently only the string input type is enabled.
@return the input that has been entered in the panel, for a requested parameter.
@param whereLabel The visible label for the input filter.
@param useWildcards If true, the returned information will be returned using
wildcards, suitable for "matches" calls (e.g., "*inputvalue*").  If false,
the returned information will be returned in verbose format as per the
toString() method (e.g., "contains;inputvalue").
@param delim Delimiter character to use if use_wildcards=false.  See the
toString() method.  If null, use ";".
*/
public List getInput ( String whereLabel, boolean useWildcards, String delim )
{	List inputList = new Vector();
	if ( delim == null ) {
		delim = ";";
	}
	InputFilter filter;
	String input; // Input string selected by user.
	String where; // Where label for filter selected by user.
	int inputType; // Input type for the filter.
	for ( int ifg = 0; ifg < __numFilterGroups; ifg++ ) {
		filter = getInputFilter ( ifg );
		where = filter.getWhereLabel();
		if ( !where.equalsIgnoreCase(whereLabel) || where.equals("") ) {
			// No need to evaluate...
			continue;
		}
		input = filter.getInput(false).trim();
		inputType = filter.getInputType();
		if ( inputType == StringUtil.TYPE_STRING ) {
			if ( useWildcards ) {
				if ( getOperator(ifg).equals( InputFilter.INPUT_MATCHES) ) {
					inputList.add( input );
				}
				else if ( getOperator(ifg).equals(InputFilter.INPUT_CONTAINS) ) {
					inputList.add("*" +input+"*");
				}
				else if ( getOperator(ifg).equals(InputFilter.INPUT_STARTS_WITH) ) {
					inputList.add( input + "*" );
				}
				else if ( getOperator(ifg).equals(InputFilter.INPUT_ENDS_WITH) ) {
					inputList.add( "*" + input );
				}
			}
			else {
				inputList.add (getOperator(ifg) + delim + input );
			}
		}
		/* TODO SAM 2004-09-10 Need to enable at some point
		else if ( (inputType == StringUtil.TYPE_DOUBLE) || (inputType == StringUtil.TYPE_FLOAT) ) {
		}
		else if	( inputType == StringUtil.TYPE_INTEGER ) {
		}
		*/
	}
	return inputList;
}

/**
Return the current filter for a filter group.
@return the current filter for a filter group.
@param ifg Input filter group.
*/
public InputFilter getInputFilter ( int ifg )
{	int filterPos = 0;
	if (__whereComponentList.get(ifg) instanceof SimpleJComboBox){
		// The where lists all the filter where labels so the current
		// filter is given by the position in the combo box...
		SimpleJComboBox cb = (SimpleJComboBox)__whereComponentList.get(ifg);
		filterPos = cb.getSelectedIndex();
	}
	else {
	    // A simple JLabel so there is only one item in the filter...
		filterPos = 0;
	}
	return (InputFilter)__inputFilterListArray[ifg].get(filterPos);
}

/**
Return the number of filter groups.
@return the number of filter groups.
*/
public int getNumFilterGroups ()
{	return __numFilterGroups;
}

/**
Return the operator for a filter group (one of InputFilter.INPUT_*).
@return the operator for a filter group.
@param ifg Filter group.
*/
public String getOperator ( int ifg )
{	SimpleJComboBox cb = (SimpleJComboBox)__operatorComponentList.get(ifg);
	return cb.getSelected();
}

/**
Return the text that is displayed in the panel, when the text constructor is used.
@return the text displayed in the panel text area.
*/
public String getText ()
{
    if ( __textArea == null ) {
        return "";
    }
    else {
        return __textArea.getText();
    }
}

/**
Handle item events for JComboBox selections.
*/
public void itemStateChanged ( ItemEvent event )
{	Object o = event.getItemSelectable();

	if ( event.getStateChange() != ItemEvent.SELECTED ) {
		// No reason to process the event...
		return;
	}

	InputFilter filter;
	// Loop through the filter groups, checking the where component to find the match...
	SimpleJComboBox where_JComboBox, operator_JComboBox;
	int filterPos = 0;	// Position of the selected filter in the group.
	int operatorPos = 0; // Position of the selected operator in the filter group.

	JComponent component = null;
				
	for ( int ifg = 0; ifg < __numFilterGroups; ifg++ ) {
		component = (JComponent)__whereComponentList.get(ifg);
		if (component instanceof SimpleJComboBox) {
			where_JComboBox = (SimpleJComboBox)component;
		}
		else {
			where_JComboBox = null;
		}

		operator_JComboBox = (SimpleJComboBox)__operatorComponentList.get(ifg);
		if (where_JComboBox != null && o == where_JComboBox ) {
			// Found the component, which indicates which filter group was changed.  Update the operator list.
			// Note that if the original __whereComponentList item was a JLabel, we would not even be here because
			// no ItemEvent would have been generated.
			//Message.printStatus ( 2, "", "SAMX Found where component: " + ifg + " resetting operators..." );
			// Figure out which filter is selected for the filter group.  Because all groups have the same list of
			// filters, the absolute position will be the same in all the lists.
			filterPos = where_JComboBox.getSelectedIndex();
			filter =(InputFilter)__inputFilterListArray[ifg].get( filterPos);
			//Message.printStatus ( 2, "", "Where changed." );
			fillOperatorJComboBox ( (SimpleJComboBox)__operatorComponentList.get(ifg), filter.getInputType(),
				filter.getConstraintsToRemove());
			// Set the appropriate component visible and all others not visible.
			// There is an input component for each filter for this filter group...
			operatorPos = 0;
			showInputFilterComponent ( ifg, filterPos, operatorPos );
			// No need to keep searching components...
			break;
		}
		else if ( o == operator_JComboBox ) {
			// Set the appropriate component visible and all others not visible.
		    // There is an input component for each filter for this filter group...
			// filterPos = getInputFilter();
			// Test...
			//Message.printStatus ( 2, "", "Operator changed." );
			// Figure out which operator...
			if (where_JComboBox == null) {
				showInputFilterComponent ( ifg, 0, operator_JComboBox.getSelectedIndex() );
			}
			else {	
				showInputFilterComponent ( ifg, where_JComboBox.getSelectedIndex(),
					operator_JComboBox.getSelectedIndex() );
			}
			// No need to keep searching components...
			break;
		}
	}
}

/**
Remove input filters and related components from the panel.
*/
private void removeInputFilters()
{	int size = 0;
	if ( __inputFilterListArray != null ) {
		size = __inputFilterListArray[0].size();
	}
	InputFilter filter;
	for ( int ifg = 0; ifg < __numFilterGroups; ifg++ ) {
		// Each group contains the same filter information, but using distinct components...
		for ( int ifilter = 0; ifilter < size; ifilter++ ) {
			filter = (InputFilter)__inputFilterListArray[ifg].get(ifilter);
			// Remove the input components for the filter...
			filter.setInputComponent(null);
		}
	}
	// Remove all the components from this JPanel...
	removeAll();
	__inputFilterListArray = null;
	__whereComponentList = null;
	__operatorComponentList = null;
}

/**
Set the contents of an input filter.
@param ifg The Filter group to be set (0+).
@param inpu_filter_string The where clause as a string, using visible information in the input filters:
<pre>
   WhereValue; Operator; InputValue
</pre>
@param delim The delimiter used for the above information.
@exception Exception if there is an error setting the filter data.
*/
public void setInputFilter ( int ifg, String inputFilterString, String delim )
throws Exception
{	List v = StringUtil.breakStringList ( inputFilterString, delim, 0 );
	String where = ((String)v.get(0)).trim();
	String operator = ((String)v.get(1)).trim();
	// Sometimes during initialization an ending token may not be provided
	// (e.g., ";Matches;") so handle below...
	String input = "";
	if ( v.size() > 2 ) {
		input = ((String)v.get(2)).trim();
	}

	// Set the where...

	JComponent component;
	component = (JComponent)__whereComponentList.get ( ifg );
	SimpleJComboBox cb;
	JTextField tf;
	JLabel label;
	if ( component instanceof SimpleJComboBox ) {
		cb = (SimpleJComboBox)component;
		JGUIUtil.selectIgnoreCase ( cb, where );
	}
	else if ( component instanceof JLabel ) {
		label = (JLabel)component;
		label.setText ( where );
	}

	// Set the operator...

	cb = (SimpleJComboBox)__operatorComponentList.get ( ifg );
	JGUIUtil.selectIgnoreCase ( cb, operator );

	// Set the input...

	InputFilter input_filter = getInputFilter ( ifg );
	component = input_filter.getInputComponent();
	if ( component instanceof SimpleJComboBox ) {
		cb = (SimpleJComboBox)component;
		JGUIUtil.selectTokenMatches ( cb, true, input_filter.getChoiceDelimiter(),
			0, input_filter.getChoiceToken(), input, null, true );
	}
	else if ( component instanceof JTextField ) {
		tf = (JTextField)component;
		tf.setText ( input );
	}
}

/**
Set the input filters.  The previous contents of the panel are removed and new
contents are created based on the parameters.
@param inputFilters A Vector of InputFilter, containing valid data.  The input
component will be set in each input filter as the GUI is defined.
@param props Properties to control the display of the panel, as described in the following table.
<table width=100% cellpadding=10 cellspacing=0 border=2>
<tr>
<td><b>Property</b></td>	<td><b>Description</b></td>	
<td><b>Default</b></td>
</tr>

<tr>
<td><b>NumFilterGroups</b></td>
<td><b>Indicates how many filter groups should be displayed.  
Each group will include all the filters supplied at construction or with a setInputFilters() call.
<td>1</td>
</tr>

<tr>
<td><b>NumWhereRowsToDisplay</b></td>
<td><b>Indicates the number of rows to be displayed in the drop-down list of
one of the combo boxes that lists the fields for a filter.
<td>(system-dependent JComboBox default)</td>
</tr>

</table>
*/
public void setInputFilters ( List inputFilters, PropList props )
{	// Make sure non-null properties are available internally...
	if ( props == null ) {
		__props = new PropList ( "InputFilter_JPanel" );
	}
	else {
	    __props = props;
	}
	// First remove the existing input filters...
	removeInputFilters();
	// Duplicate the input filters for each filter group...
	String prop_val = __props.getValue ( "NumFilterGroups" );
	__numFilterGroups = 0;	// Number of filter groups
	int numFilters = inputFilters.size();	// Number of filters in a filter group
	if ( inputFilters != null ) {
		numFilters = inputFilters.size();
	}
	if ( (inputFilters == null) || (inputFilters.size() == 0) ) {
		// Only display if we actually have data...
		__numFilterGroups = 0;
	}
	else {
	    if ( prop_val != null ) {
			// Calling code has requested the number of filters...
			__numFilterGroups = StringUtil.atoi(prop_val);
		}
		else {
		    // Assume one filter group...
			__numFilterGroups = 1;
		}
	}
	__inputFilterListArray = new Vector[__numFilterGroups];
	InputFilter filter;
	for ( int ifg = 0; ifg < __numFilterGroups; ifg++ ) {
		if ( ifg == 0 ) {
			// Assign the original...
			__inputFilterListArray[0] = inputFilters;
		}
		else {
			// Copy the original...
			__inputFilterListArray[ifg] = new Vector(numFilters);
			for ( int ifilter = 0; ifilter < numFilters; ifilter++ ) {
				filter = (InputFilter)inputFilters.get(ifilter);
				__inputFilterListArray[ifg].add ( filter.clone() );
			}
		}
	}
	// Now add the new input filters...
	Insets insetsNNNN = new Insets(0,0,0,0);
	int x = 0, y = 0;
	int num = 0;
	// Layout is as follows...
	//
	//      0              1            2         3          4
	//   where_label   operators   ......SimpleJComboBox............
	//
	//				OR...
	//
	//                             JTextField    AND     JTextField
	//
	//				Depending on whether the choices are
	//				provided.
	//
	// where positions 2-4 are used as necessary based on the type of the input.
	__whereComponentList = new Vector(__numFilterGroups);
	__operatorComponentList = new Vector(__numFilterGroups);

	String numRowsToDisplayString=__props.getValue("NumWhereRowsToDisplay");
	int numRowsToDisplay = -1;
	if (numRowsToDisplayString != null) {
		numRowsToDisplay = StringUtil.atoi(numRowsToDisplayString);
	}
	
	for ( int ifg = 0; ifg < __numFilterGroups; ifg++, y++ ) {
		x = 0;
		if ( numFilters == 1 ) {
			// Just use a label since the user cannot pick...
			filter = (InputFilter)__inputFilterListArray[ifg].get(0);
			JLabel where_JLabel = new JLabel ( "Where " + filter.getWhereLabel() + ":" );
        		JGUIUtil.addComponent(this, where_JLabel,
				x++, y, 1, 1, 0.0, 0.0, insetsNNNN,
				GridBagConstraints.NONE, GridBagConstraints.EAST);
			__whereComponentList.add ( where_JLabel );
		}
		else {
		    // Put the labels in a combo box so the user can pick...
        	JGUIUtil.addComponent(this, new JLabel("Where:"),
				x++, y, 1, 1, 0.0, 0.0, insetsNNNN,
				GridBagConstraints.NONE, GridBagConstraints.EAST);
			SimpleJComboBox where_JComboBox = new SimpleJComboBox ( false );
			List whereList = new Vector(numFilters);
			for ( int ifilter = 0; ifilter < numFilters; ifilter++ ) {
				filter = (InputFilter)__inputFilterListArray[ifg].get(ifilter);
				whereList.add(filter.getWhereLabel());
			}
			where_JComboBox.setData ( whereList );
			where_JComboBox.addItemListener ( this );

			if (numRowsToDisplay > -1) {
				where_JComboBox.setMaximumRowCount(numRowsToDisplay);
			}
				
    		JGUIUtil.addComponent(this, where_JComboBox,
			x++, y, 1, 1, 0.0, 0.0, insetsNNNN,
			GridBagConstraints.NONE, GridBagConstraints.EAST);
			__whereComponentList.add (where_JComboBox);
		}
		// The operators are reused in the filter group.  Initialize to the first filter...
		filter = (InputFilter)__inputFilterListArray[ifg].get(0);
		// Initialize operators to the first filter.
		// This is reused because it is a simple list based on the current input type.
		SimpleJComboBox operator_JComboBox = new SimpleJComboBox(false);
		fillOperatorJComboBox(operator_JComboBox, filter.getInputType(), filter.getConstraintsToRemove());
		operator_JComboBox.addItemListener ( this );
		__operatorComponentList.add ( operator_JComboBox );
		JGUIUtil.addComponent(this, operator_JComboBox,
			x++, y, 1, 1, 0.0, 0.0, insetsNNNN,
			GridBagConstraints.NONE, GridBagConstraints.EAST);
		// Now initialize the components used for input, one component per filter in the group...
		for ( int ifilter = 0; ifilter < numFilters; ifilter++ ) {
			filter =(InputFilter)__inputFilterListArray[ifg].get(ifilter);
			if ( filter.getChoiceLabels() == null ) {
				// No choices are provided so use a text field...
				num = filter.getInputJTextFieldWidth();
				JTextField input_JTextField=new JTextField(num);
        			JGUIUtil.addComponent(this, input_JTextField,
					x, y, 1, 1, 0.0, 0.0, insetsNNNN,
					GridBagConstraints.NONE, GridBagConstraints.WEST);
				List listeners = filter.getInputComponentMouseListeners();
				if (listeners != null) {
					int lsize = listeners.size();
					for (int l = 0; l < lsize; l++) {
						input_JTextField.addMouseListener((MouseListener)listeners.get(l));
					}
				}
				// TODO - need to be distinct for each group, not shared...
				//Message.printStatus ( 2, "", "SAMX adding text field as input component ");
				input_JTextField.setEditable(filter.isInputJTextFieldEditable());
				filter.setInputComponent( input_JTextField);
				if ( ifilter != 0 ) {
					input_JTextField.setVisible ( false );
				}
			}
			else {
			    // Add the choices...
				SimpleJComboBox input_JComboBox = new SimpleJComboBox ( filter.getChoiceLabels(),
					filter.areChoicesEditable() );
				num = filter.getNumberInputJComboBoxRows();
				if (num > 0) {
					input_JComboBox.setMaximumRowCount(num);
				}
				else if ( num == InputFilter.JCOMBOBOX_ROWS_DISPLAY_DEFAULT) {
				    // do nothing, display in the default way
				}
				else if ( num == InputFilter.JCOMBOBOX_ROWS_DISPLAY_ALL) {
					num = input_JComboBox.getItemCount();
					input_JComboBox.setMaximumRowCount(num);
				}
				JGUIUtil.addComponent(this, input_JComboBox,
					x, y, 1, 1, 0.0, 0.0, insetsNNNN,
					GridBagConstraints.NONE, GridBagConstraints.WEST);
				// TODO - need to be distinct for each group, not shared...
				//Message.printStatus ( 2, "", "SAMX adding combo box as input component ");
				filter.setInputComponent( input_JComboBox );
				// Only set the first input component visible...
				if ( ifilter != 0 ) {
					input_JComboBox.setVisible ( false );
				}
				List listeners = filter.getInputComponentMouseListeners();
				if (listeners != null) {
					int lsize = listeners.size();
					for (int l = 0; l < lsize; l++) {
						input_JComboBox.addMouseListener((MouseListener)listeners.get(l));
					}
				}				
			}
			// Always add a blank panel on the right side that allows expansion, to fill up the right
			// side of the panel.  Otherwise, each component is not resizable and the filter panel tends
			// to set centered with a lot of space on either side in container panels.
			JGUIUtil.addComponent(this, new JPanel(),
                (x + 1), y, 1, 1, 1.0, 0.0, insetsNNNN,
                GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
		}
	}
}

/**
Set the text that is displayed in the panel, when the text constructor is used.
@param text the text displayed in the panel text area.
*/
public void setText ( String text )
{
    if ( __textArea != null ) {
        __textArea.setText(text);
    }
}

/**
Show the appropriate input filter component.  This is the component that the
user will enter a value to check for.  It can be either a predefined list
(JComboBox) or a text field, depending on how the InputFilter was initially
defined and depending on the current operator that is selected.
@param ifg the InputFilter group to be updated.
@param filterPos the input filter component that is currently selected (the where item).
@param operatorPos the operator item that is currently selected.  The input
component that matches this criteria is set visible and all others are set to
not visible.  In actuality, the input component is the same regardless of the
operator component.  If the input are available as choices, then the operator
will use these choices.  If the input is a text field, then the operator will
require user text.  The only limitation is that for a string input type where
the input choices have been supplied, the user will be limited to only available
strings and will therefore not be able to do substrings.
*/
private void showInputFilterComponent ( int ifg, int filterPos, int operatorPos )
{	int nfilters = __inputFilterListArray[0].size();
	InputFilter filter; // Input filter to check
	for ( int ifilter = 0; ifilter < nfilters; ifilter++) {
		filter = (InputFilter)__inputFilterListArray[ifg].get(ifilter);
		if ( ifilter == filterPos ) {
			// The input component for the selected filter needs to be visible...
			//Message.printStatus ( 2, "","SAMX enabling input component " + ifilter );
			((JComponent)filter.getInputComponent()).setVisible(true);
		}
		else {
		    // All other input components should not be visible...
			//Message.printStatus ( 2, "","SAMX disabling input component " + ifilter );
			((JComponent)filter.getInputComponent()).setVisible(false);
		}
	}
}

/**
Return a string representation of an input filter group.  This can be used, for
example, with software that may place a filter in a command.
@param ifg the Input filter group
@param delim Delimiter for the returned filter information.  If null, use ";".
*/
public String toString ( int ifg, String delim )
{	InputFilter filter = getInputFilter ( ifg );
	if ( delim == null ) {
		delim = ";";
	}
	return filter.getWhereLabel() + delim + getOperator(ifg) + delim + filter.getInput(false);
}

}
