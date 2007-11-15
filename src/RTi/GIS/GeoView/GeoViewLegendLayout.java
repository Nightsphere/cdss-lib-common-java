//-----------------------------------------------------------------------------
// GeoViewLegendLayout - a layout manager that controls how the legend is
//	drawn on the map, also doing linking so the legend can be managed
// 	from the properties JFrame
//-----------------------------------------------------------------------------
// Copyright:  See the COPYRIGHT file.
//-----------------------------------------------------------------------------
// History:
// 2004-10-18	J. Thomas Sapienza, RTi	Initial version.
// 2005-04-27	JTS, RTi		Added finalize().
//-----------------------------------------------------------------------------

package RTi.GIS.GeoView;

import java.util.Vector;

import javax.swing.JCheckBox;

import RTi.Util.GUI.SimpleJTree_Node;

/**
A layout manager for the GeoViewLegend that controls how the legend is drawn
on the map.  This class also links the legend to the properties JFrame for
the GeoView so it can be changed there.
REVISIT (JTS - 2006-05-23)
How is this class used?
*/
public class GeoViewLegendLayout {

/**
Location constants that specify where the legend is placed on the GeoView.
*/
public static final int 
	NORTHWEST = 0,
	NORTHEAST = 2,
	SOUTHEAST = 4,
	SOUTHWEST = 6;

/**
The number of node layers added to the legend.
*/
private int count = 0;

/**
The position of the legend on the GeoView (one of NORTHWEST, NORTHEAST,
SOUTHWEST, SOUTHEAST).
*/
private int __position = 0;

/**
The title of the legend.
*/
private String __title = null;

/**
The checkboxes that were added to the legend.  The items in this Vector 
correspond to the items in the other Vectors at the same position.
*/
public Vector __checkboxes = new Vector();

/**
The nodes that were added to the legend.  The items in this Vector 
correspond to the items in the other Vectors at the same position.
*/
public Vector __nodes = new Vector();

/**
The layers that were added to the legend.  The items in this Vector 
correspond to the items in the other Vectors at the same position.
*/
public Vector __layers = new Vector();

/**
A Vector of Booleans that specify whether the layer in the layers Vector
at the same position is visible or not.  The items in this Vector 
correspond to the items in the other Vectors at the same position.
*/
public Vector __visibles = new Vector();

/**
Adds an item to the legend.
@param node the node in the legend JTree that corresponds to the item being
added to the legend.
@param layer the layer on the GeoView corresponding to the item being added
to the legend.
@param checkbox the checkbox in the legend JTree that corresponds to the item
being added.
@param visible whether the item is visible.
*/
public void addNodeLayerCheckBox(SimpleJTree_Node node, GeoLayerView layer,
JCheckBox checkbox, boolean visible) {
	__nodes.add(node);
	__layers.add(layer);
	__checkboxes.add(checkbox);
	__visibles.add(new Boolean(visible));
	count++;
}

/**
Clears everything from the legend.
*/
public void empty() {
	__nodes = new Vector();
	__layers = new Vector();
	__visibles = new Vector();
	__checkboxes = new Vector();
	count = 0;
}

/**
Cleans up member variables.
@throws Throwable if an error occurs.
*/
public void finalize() 
throws Throwable {
	__checkboxes = null;
	__nodes = null;
	__layers = null;
	__visibles = null;
	__title = null;
	super.finalize();
}

/**
Returns the position of the given checkbox in the legend, or -1 if the 
checkbox cannot be found.
@return the position of the given checkbox in the legen, or -1 if the 
checkbox cannot be found.
*/
public int findCheckBox(JCheckBox checkbox) {
	JCheckBox tempCheckbox = null;
	for (int i = 0; i < count; i++) {
		tempCheckbox = (JCheckBox)__checkboxes.elementAt(i);
		if (tempCheckbox == checkbox) {
			return i;
		}
	}
	return -1;
}

/**
Returns the position of the given layer in the legend, or -1 if the 
layer cannot be found.
@return the position of the given layer in the legen, or -1 if the 
layer cannot be found.
*/
public int findLayer(GeoLayerView layer) {
	GeoLayerView tempLayer = null;
	for (int i = 0; i < count; i++) {
		tempLayer = (GeoLayerView)__layers.elementAt(i);
		if (tempLayer == layer) {
			return i;
		}
	}
	return -1;
}

/**
Returns the position of the given node in the legend, or -1 if the 
node cannot be found.
@return the position of the given node in the legen, or -1 if the 
node cannot be found.
*/
public int findNode(SimpleJTree_Node node) {
	SimpleJTree_Node tempNode = null;
	for (int i = 0; i < count; i++) {
		tempNode = (SimpleJTree_Node)__nodes.elementAt(i);
		if (tempNode == node) {
			return i;
		}
	}
	return -1;
}

/**
Returns the count of items in this legend.
@return the count of items in this legend.
*/
public int getCount() {
	return count;
}

/**
Returns the title of the legend.
@return the title of the legend.
*/
public String getTitle() {
	return __title;
}

/**
Returns the position of the legend on the GeoView.
@return the position of the legend on the GeoView.
*/
public int getPosition() {
	return __position;
}

/**
Returns whether the given layer is visible.
@return whether the given layer is visible.
*/
public boolean isLayerVisible(GeoLayerView glv) {
	if (__layers.size() == 0) {
		return false;
	}

	int num = findLayer(glv);

	if (num == -1) {
		return false;
	}

	GeoLayerView layer = (GeoLayerView)__layers.elementAt(num);
	return layer.isVisible();
}

/**
Returns whether the given layer's legend is visible.
@param glv the layer to check.
@return whether the given layer's legend is visible.
*/
public boolean isLayerLegendVisible(GeoLayerView glv) {
	if (__layers.size() == 0) {
		return false;
	}

	int num = findLayer(glv);

	if (num == -1) {
		return false;
	}

	return ((Boolean)__visibles.elementAt(num)).booleanValue();
}

/**
Returns whether the given node's legend is visible.
@param node the node to check.
@return whether the given node's legend is visible.
*/
public boolean isNodeLegendVisible(SimpleJTree_Node node) {
	if (__nodes.size() == 0) {
		return false;
	}

	int num = findNode(node);

	if (num == -1) {
		return false;
	}

	return ((Boolean)__visibles.elementAt(num)).booleanValue();
}

/**
Removes the given layer from the legend.
@param layer the layer to remove.
*/
public void removeLayer(GeoLayerView layer) {
	int num = findLayer(layer);
	if (num == -1) {
		return;
	}
	__nodes.removeElementAt(num);
	__layers.removeElementAt(num);
	__visibles.removeElementAt(num);
	__checkboxes.removeElementAt(num);
	count--;
}

/**
Removes the given node from the legend.
@param node the node to remove.
*/
public void removeNode(SimpleJTree_Node node) {
	int num = findNode(node);
	if (num == -1) {
		return;
	}
	__nodes.removeElementAt(num);
	__layers.removeElementAt(num);
	__visibles.removeElementAt(num);
	__checkboxes.removeElementAt(num);
	count--;
}

/**
Sets the checkbox at the given legend position.
@param num the position at which to set the checkbox.
@param checkbox the checkbox to set.
*/
public void setCheckBox(int num, JCheckBox checkbox) {
	__checkboxes.setElementAt(checkbox, num);
}

/**
Sets the layer legend at the given position visible or not.
@param num the position at which to set the layer visible.
@param visible whether to set the layer visible or invisible.
*/
public void setLayerLegendVisible(int num, boolean visible) {
	__visibles.setElementAt(new Boolean(visible), num);
}

/**
Sets the position of the legend.
@param pos the position of the legend (NORTHWEST, NORTHEAST, SOUTHWEST,
SOUTHEAST).
*/
public void setPosition(int pos) {
	__position = pos;
}

/**
Sets the title of the legend.
@param title the title to set.
*/
public void setTitle(String title) {
	__title = title;
}

}