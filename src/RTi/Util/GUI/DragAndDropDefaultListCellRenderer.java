// DragAndDropDefaultListCellRenderer - a cell renderer for JComboBox lists, for drag and drop

/* NoticeStart

CDSS Common Java Library
CDSS Common Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2019 Colorado Department of Natural Resources

CDSS Common Java Library is free software:  you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    CDSS Common Java Library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with CDSS Common Java Library.  If not, see <https://www.gnu.org/licenses/>.

NoticeEnd */

// ----------------------------------------------------------------------------
// DragAndDropDefaultListCellRenderer - a cell renderer for JComboBox lists,
// 	allowing data to be dragged onto a combo box.
// ----------------------------------------------------------------------------
// Copyright:   See the COPYRIGHT file
// ----------------------------------------------------------------------------
// History:
// 2004-03-01	J. Thomas Sapienza, RTi	Initial version.
// 2004-04-27	JTS, RTi		Revised after SAM's review.
// 2005-04-26	JTS, RTi		Added finalize().
// 2007-05-08	SAM, RTi		Cleanup code based on Eclipse feedback.
// ----------------------------------------------------------------------------

package RTi.Util.GUI;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import javax.swing.border.EmptyBorder;

/**
This class is a cell renderer that allows the dragging of data onto a 
SimpleJComboBox.
*/
@SuppressWarnings("serial")
public class DragAndDropDefaultListCellRenderer
extends DefaultListCellRenderer {

/**
The combo box for which this class will render data.
*/
private DragAndDropSimpleJComboBox __comboBox = null;

/**
Constructs a default renderer object for an item
in a list.
*/
public DragAndDropDefaultListCellRenderer(DragAndDropSimpleJComboBox comboBox) {
	super();
	__comboBox = comboBox;
	setOpaque(true);
	setBorder(new EmptyBorder(1, 1, 1, 1));
}

/**
Cleans up member variables.
*/
public void finalize()
throws Throwable {
	__comboBox = null;
	super.finalize();
}

public Component getListCellRendererComponent(JList list, Object value,
int index, boolean isSelected, boolean cellHasFocus) {
	// set the item that is to be dragged or dropped in the combo box
	if (isSelected) {
		__comboBox.setLastSelectedItem("" + value);
	}

	return super.getListCellRendererComponent(list, value, index, 
		isSelected, cellHasFocus);
}

}
