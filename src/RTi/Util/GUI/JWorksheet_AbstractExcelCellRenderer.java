// ----------------------------------------------------------------------------
// JWorksheet_AbstractExcelCellRenderer - Renderer that displays things
//	in a fashion (i.e., left-justified, right-justified) similar to
//	Microsoft Excel
// ----------------------------------------------------------------------------
// Copyright:   See the COPYRIGHT file
// ----------------------------------------------------------------------------
// History:
// 2003-06-09	J. Thomas Sapienza, RTI	Initial version from 
//					HydroBase_CellRenderer_Default
// 2003-06-20	JTS, RTi		Added call to getAbsoluteColumn in
//					order to get the proper column even
//					when columns have been hidden in the
//					JWorksheet.
// 2003-10-13	JTS, RTi		Alignment can now be overridden based
//					on values set in the worksheet.
// 2004-02-03	JTS, RTi		Added support for Float.
// 2004-11-01	JTS, RTi		Added the renderBooleanAsCheckBox() 
//					method to allow booleans to come through
//					as they do in the core JTable code.
// 2005-04-26	JTS, RTi		Added finalize().
// 2005-06-02	JTS, RTi		Added checks so that NaN values will
//					be shown in the table as empty Strings.
// 2007-05-08	SAM, RTi		Cleanup code based on Eclipse feedback.
// ----------------------------------------------------------------------------

package RTi.Util.GUI;

import java.awt.Color;
import java.awt.Component;

import java.util.Date;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import RTi.DMI.DMIUtil;

import RTi.Util.GUI.JWorksheet;
import RTi.Util.GUI.JWorksheet_DefaultTableCellRenderer;

import RTi.Util.String.StringUtil;

/**
This class is the class from which other Cell Renderers for HydroBase
should be built. <p>
REVISIT (JTS - 2006-05-25)<p>
If I could do this over again, I would have combined AbstractTableCellRenderer,
DefaultTableCellRenderer and AbstractExcelCellRenderer into a single cell 
renderer.  The reasoning for having the separation came about from the 
way the JWorksheet was designed originally.<p>
AbstractTableCellRenderer was supposed to be The Base Class for all other 
renderers, providing the basic outline of what they would do.<p>
DefaultTableCellRenderer was supposed to be used for worksheets that didn't
require any special cell formatting.<p>
AbstractExcelCellRenderer was supposed to be the base class for cell renderers
that would do formatting of cell contents.<p>
In theory.<p>
In practice, ALL cell renderers are doing cell formatting, so the 
AbstractTableCellRenderer and DefaultTableCellRenderer are unnecessary overhead.
<p>
<b>Also</b><p>
I really don't see much of a good reason to even REQUIRE cell renderers for
most classes.  There are a lot of cell renderers out there that are almost 100%
the same class.  At this point there's little chance of going back and 
eliminating them, but if I could I would.  Use a default cell renderer for all
those classes and eliminate a lot of maintenance woes.

*/
public abstract class JWorksheet_AbstractExcelCellRenderer
extends JWorksheet_DefaultTableCellRenderer {

/**
Whether to render a boolean value as text or as a checkbox.
*/
private boolean __renderBooleanAsCheckBox = false;

/**
The border to use when the cell is not selected.
*/
protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1); 

/**
The colors that have been set to use as the unselected foreground and background
colors.
*/
private Color 
	unselectedForeground,
	unselectedBackground;

/**
Cleans up member variables.
*/
public void finalize()
throws Throwable {
	unselectedForeground = null;
	unselectedBackground = null;
	super.finalize();
}

/**
Method to return the format for a given column.
@param column the colum for which to return the format.
@return the format (as used by StringUtil.formatString()) for a column.
*/
public abstract String getFormat(int column);

/**
Renders a value for a cell in a JTable.  This method is called automatically
by the JTable when it is rendering its cells.  This overrides some code from
DefaultTableCellRenderer.
@param table the JTable (in this case, JWorksheet) in which the cell
to be rendered will appear.
@param value the cell's value to be rendered.
@param isSelected whether the cell is selected or not.
@param hasFocus whether the cell has focus or not.
@param row the row in which the cell appears.
@param column the column in which the cell appears.
@return a properly-rendered cell that can be placed in the table.
*/
public Component getTableCellRendererComponent(JTable table, Object value,
boolean isSelected, boolean hasFocus, int row, int column) {
	String str = "";
 	if (value != null) {
		str = value.toString();
	}
	
	int abscolumn = ((JWorksheet)table).getAbsoluteColumn(column);
	
	String format = getFormat(abscolumn);
	
	int justification = SwingConstants.LEFT;

	if (value instanceof Integer) {
		if (DMIUtil.isMissing(((Integer)value).intValue())) {
			str = "";
		} 
		else {
			justification = SwingConstants.RIGHT;
			str = StringUtil.formatString(value, format);
		}
	}	
	else if (value instanceof Double) {
		double d = ((Double)value).doubleValue();
		if (DMIUtil.isMissing(d) || Double.isNaN(d)) {
			str = "";
		}	
		else {
			justification = SwingConstants.RIGHT;
			str = StringUtil.formatString(value, format);
		}
	}
	else if (value instanceof Date) {
		justification = SwingConstants.LEFT;		
		// FYI: str has been set above with str = value.toString()
	}
	else if (value instanceof String) {
		justification = SwingConstants.LEFT;
		str = StringUtil.formatString(value, format);
	}
	else if (value instanceof Float) {
		float f = ((Float)value).floatValue();
		if (DMIUtil.isMissing(f) || Float.isNaN(f)) {
			str = "";
		}
		else {
			justification = SwingConstants.RIGHT;
			str = StringUtil.formatString(value, format);
		}
	}
	else if (value instanceof Boolean && __renderBooleanAsCheckBox) {
		JCheckBox component = new JCheckBox((String)null,
			((Boolean)value).booleanValue());
		setProperColors(component, table, isSelected, hasFocus, row, 
			column);
	}
	else {
		justification = SwingConstants.LEFT;
	}

	str = str.trim();

	// call DefaultTableCellRenderer's version of this method so that
	// all the cell highlighting is handled properly.
	super.getTableCellRendererComponent(table, str, 
		isSelected, hasFocus, row, column);	
	
	int tableAlignment = ((JWorksheet)table).getColumnAlignment(abscolumn);
	if (tableAlignment != JWorksheet.DEFAULT) {
		justification = tableAlignment;
	}
	
	setHorizontalAlignment(justification);
	setFont(((JWorksheet)table).getCellFont());

	return this;
}

/**
Sets the color to use as the unselected background color.
@param c the Color to use as the unselected background color.
*/
public void setBackground(Color c) {
	super.setBackground(c); 
	unselectedBackground = c; 
}

/**
Sets the color to use as the unselected foreground color.
@param c the Color to use as the unselected foreground color.
*/
public void setForeground(Color c) {
	super.setForeground(c); 
	unselectedForeground = c; 
}


/**
Sets whether to render booleans as text (false) or checkboxes (true).
@param renderAsCheckBox if true, booleans are not rendered as text in a cell
but as a checkbox.
*/
public void setRenderBooleanAsCheckBox(boolean renderBooleanAsCheckBox) {
	__renderBooleanAsCheckBox = renderBooleanAsCheckBox;
}

/**
Sets the colors for the rendered cell properly.  From the original Java code.
*/
public void setProperColors(JComponent component, JTable table, 
boolean isSelected, boolean hasFocus, int row, int column) {
	if (isSelected) {
		component.setForeground(table.getSelectionForeground());
		component.setBackground(table.getSelectionBackground());
	}
	else {
		component.setForeground((unselectedForeground != null) 
			? unselectedForeground 
			: table.getForeground());
		component.setBackground((unselectedBackground != null) 
			? unselectedBackground 
			: table.getBackground());
	}
	setFont(table.getFont());

	if (hasFocus) {
		component.setBorder(UIManager.getBorder(
			"Table.focusCellHighlightBorder"));
		if (table.isCellEditable(row, column)) {
			component.setForeground(UIManager.getColor(
				"Table.focusCellForeground"));
			component.setBackground(UIManager.getColor(
				"Table.focusCellBackground"));
		}
	} 
	else {
		component.setBorder(noFocusBorder);
	}
}

}
