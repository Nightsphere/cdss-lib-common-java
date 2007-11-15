// ----------------------------------------------------------------------------
// GRLocatorArc - GR arc (ellipsoid) with cross-hairs that can be used for
//			selecting or locating a region
// ----------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
// ----------------------------------------------------------------------------
// History:
//
// 2002-05-17	Steven A. Malers	Initial version.
// ----------------------------------------------------------------------------

package RTi.GR;

/**
The GRLocatorArc class stores an ellipsoid shape and centered cross-hair
information.  Data are public but the set methods
should be called to set data so that coordinate limits can be computed.
*/
public class GRLocatorArc 
extends GRArc
{

/**
Width of the cross-hair (distance from center to one end).
*/
public double xcross_width = 0.0;

/**
Height of the cross-hair (distance from center to one end).
*/
public double ycross_height = 0.0;

/**
Construct and initialize to (0,0).
*/
public GRLocatorArc ()
{	super ();
	type = LOCATOR_ARC;
}

/**
Construct given necessary data pair.
@param pt_set Coordinates of center.
@param xradius_set Radius in X direction.
@param yradius_set Radius in Y direction.
@param angle1_set Starting angle for draw.
@param angle2_set Ending angle for draw.
@param xcross_width_set Width for centering cross (distance from center to one
end).
@param ycross_height_set Height for centering cross (distance from center to one
end).
*/
public GRLocatorArc (	GRPoint pt_set,
			double xradius_set, double yradius_set,
			double angle1_set, double angle2_set,
			double xcross_width_set, double ycross_height_set )
{	super ( pt_set, xradius_set, yradius_set, angle1_set, angle2_set );
	type = LOCATOR_ARC;
	xcross_width = xcross_width_set;
	ycross_height = ycross_height_set;
}

/**
Construct given necessary data pair.
@param x_set X-coordinate of center.
@param y_set Y-coordinate of center.
@param xradius_set Radius in X direction.
@param yradius_set Radius in Y direction.
@param angle1_set Starting angle for draw.
@param angle2_set Ending angle for draw.
@param xcross_width_set Width for centering cross (distance from center to one
end).
@param ycross_height_set Height for centering cross (distance from center to one
end).
*/
public GRLocatorArc (	double x_set, double y_set,
			double xradius_set, double yradius_set,
			double angle1_set, double angle2_set,
			double xcross_width_set, double ycross_height_set )
{	super ( x_set, y_set, xradius_set, yradius_set, angle1_set, angle2_set);
	type = LOCATOR_ARC;
	xcross_width = xcross_width_set;
	ycross_height = ycross_height_set;
}

/**
Construct given the attribute lookup key and shape data.
@param attkey Attribute lookup key.
@param pt_set Coordinates of center.
@param xradius_set Radius in X direction.
@param yradius_set Radius in Y direction.
@param angle1_set Starting angle for draw.
@param angle2_set Ending angle for draw.
@param xcross_width_set Width for centering cross (distance from center to one
end).
@param ycross_height_set Height for centering cross (distance from center to one
end).
*/
public GRLocatorArc (	long attkey, GRPoint pt_set,
			double xradius_set, double yradius_set,
			double angle1_set, double angle2_set,
			double xcross_width_set, double ycross_height_set )
{	super(attkey, pt_set, xradius_set, yradius_set, angle1_set, angle2_set);
	type = LOCATOR_ARC;
	xcross_width = xcross_width_set;
	ycross_height = ycross_height_set;
}

/**
Copy constructor.
@param locator_arc GRLocatorArc to copy.
*/
public GRLocatorArc ( GRLocatorArc locator_arc )
{	super ( locator_arc );
	type = LOCATOR_ARC;
	xcross_width = locator_arc.xcross_width;
	ycross_height = locator_arc.ycross_height;
	// Base class does not have a constructor for this yet...
	is_visible = locator_arc.is_visible;
	is_selected = locator_arc.is_selected;
	associated_object = locator_arc.associated_object;
	limits_found = locator_arc.limits_found;
}

/**
Return a string representation of the locator arc.
@return A string representation of the arc in the format
"GRLocatorArc(x,y,xradius,yradius,angle1,angle2,xcross_width,ycross_height)".
*/
public String toString ()
{	return "GRArc(" + pt.x + "," + pt.y + "," + xradius + "," + yradius +
		"," + angle1 + "," + angle2 + xcross_width + "," +
		ycross_height + ")";
}

} // End GRLocatorArc class