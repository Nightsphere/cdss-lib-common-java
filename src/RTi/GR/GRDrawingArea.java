// ----------------------------------------------------------------------------
// GRDrawingArea - GR drawing area base class
// ----------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
// ----------------------------------------------------------------------------
// Notes:	(1)	This is the base class for drawing areas.  It maintains
//			all of the generic data for drawing ares (scaling
//			information, etc.).  The set/get methods handle data
//			for the class.  The drawing routines should be
//			overruled in the derived class since drawing is specific
//			to the device.
// ----------------------------------------------------------------------------
// History:
//
// 10 Aug 1997	Steven A. Malers, RTi	Initial Java version as port of C++/C
//					code.
// 28 Mar 1998	SAM, RTi		Revisit the code and implement a
//					a totally new design that is 100%
//					object oriented.
// 08 Jul 1999	SAM, RTi		Add the _global_xs and _global_ys arrays
//					for use by drawing code so that
//					memory is not abused.  _global_len_s
//					is the length of the arrays.  The
//					arrays are guaranteed to be at least
//					10 elements so that many small drawing
//					tasks such as points, rectangles, and
//					line segments can use the arrays.  The
//					arrays are totally dynamic and should
//					not be used for persistent (multi-step
//					tasks).
// 08 Nov 1999	SAM			Verify setLineDash works.
// 25 May 2000	SAM			Add getTextExtents().
// 2002-01-07	SAM, RTi		Update so that there is no real-time
//					dependence on the device.  The _graphics
//					reference and _reverse_y value from the
//					GR device is also saved here.  This
//					increases performance some but is being
//					done mainly to allow GR devices to be
//					created for Canvas and JComponent.
//					Change setFont() to take a style.
//					Change getPlotLimits() flags and add
//					support for COORD_DATA to clarify the
//					flag.  Remove the default
//					getPlotLimits() to make calls explicit.
// ----------------------------------------------------------------------------
// 2003-05-02	J. Thomas Sapienza, RTi	Made class abstract.
// 2003-05-07	JTS, RTi		Made changes following SAM's review.
// 2004-05-25	JTS, RTi		For some reason the log scaling code 
//					was not transferred over when the
//					library was redesigned last year.  
//					Reinstated the log scaling.
// 2004-06-03	JTS, RTi		
// 2005-04-20	JTS, RTi		* Added setClip().
//					* Added getClip().
// 2005-04-26	JTS, RTi		Added finalize().
// 2005-04-29	JTS, RTi		Added isDeviceAntiAliased() and
//					setDeviceAntiAlias().
// 2006-08-22	SAM, RTi		Increased debug level to 100 for X,Y
//					scaling messages.
// 2007-05-08	SAM, RTi		Cleanup code based on Eclipse feedback.
// ----------------------------------------------------------------------------
// EndHeader

package RTi.GR;

import java.awt.Graphics;
import java.awt.Shape;

import java.util.Vector;

import RTi.Util.IO.PropList;

import RTi.Util.Math.MathUtil;
import RTi.Util.Math.StandardNormal;

import RTi.Util.Message.Message;


/**
This class is the base class for GR drawing areas, which are virtual drawing 
areas within a GRDevice.  Open a GRDrawingArea and then draw to it using the
GRDrawingAreaUtil class draw functions.
@see GR
@see GRDevice
*/
public abstract class GRDrawingArea
{

/**
Flag used to indicate raw device coordinates (used by getPlotLimits() and
getDataXY()).
*/
public static final int COORD_DEVICE = 0;

/**
Flag used to indicate GR plotting coordinates (same units as COORD_DEVICE but
Y-axis may be flipped).
*/
public static final int COORD_PLOT = 1;	

/**
Flag used to indicate data coordinates.
*/
public static final int COORD_DATA = 2;	

/**
Shared data arrays to minimize dynamic memory issues by drawing code.  
Will be dimensioned to 10 upon initial object instantiation.  Later
may increase the dimension to support polylines and polygons if necessary.
*/
protected static double [] _global_xs = null;
protected static double [] _global_ys = null;
protected static int _global_len_s = 0;
protected static int [] _global_ixs = null;
protected static int [] _global_iys = null;
protected static int _global_len_is = 0;

/**
Indicates whether the data limits have been set.
*/
protected boolean _dataset;
/**
Indicates whether drawing limits have been set.
*/
protected boolean _drawset;
/**
use to shift y-axis to normal coordinates.
*/
protected boolean _reverse_y;

/**
Left-most X coordinate (data units)
*/
protected double _datax1;
/**
Right-most X coordinate (data units)
*/
protected double _datax2;
/**
Bottom-most Y coordinate (data units)
*/
protected double _datay1;
/**
Top-most Y coordinate (data units)
*/
protected double _datay2;
/**
Left-most plotting X coordinate (device units)
*/
protected double _drawx1;
/**
Right-most plotting X coordinate (device units)
*/
protected double _drawx2;
/**
Bottom-most plotting Y coordinate (device units)
*/
protected double _drawy1;
/**
Top-most plotting Y value (device units)
*/
protected double _drawy2;


/**
Last X coordinate drawn to in data units.
*/
protected double _lastx;
/**
Last X coordinate drawn to in plotting units.
*/
protected double _lastxp;
/**
Last Y coordinate drawn to in data units.
*/
protected double _lasty;
/**
Last Y coordinate drawn to in plotting units.
*/
protected double _lastyp;

/**
Linearized value corresponding to the Z-value for probability axes, the log10
value for log axes, and the data value for linear axes.
*/
protected double _linearx1;
/**
Linearized value corresponding to the Z-value for probability axes, the log10
value for log axes, and the data value for linear axes.
*/
protected double _linearx2;
/**
Linearized value corresponding to the Z-value for probability axes, the log10
value for log axes, and the data value for linear axes.
*/
protected double _lineary1;
/**
Linearized value corresponding to the Z-value for probability axes, the log10
value for log axes, and the data value for linear axes.
*/
protected double _lineary2;

/**
Use to shift Y-axis to normal coordinates.
*/
protected double _devyshift;

/**
Font height used for text.
*/
protected double _fontht;

/**
Line width.
*/
protected double _linewidth;

/**
Left-most plotting X value (device units), taking into account the aspect
of the axes.
*/
protected double _plotx1;
/**
Right-most plotting X value (device units), taking into account the aspect
of the axes.
*/
protected double _plotx2;
/**
Bottom-most plotting Y value (device units), taking into account the aspect
of the axes.
*/
protected double _ploty1;
/**
Top-most plotting Y value (device units), taking into account the aspect
of the axes.
*/
protected double _ploty2;

/**
Graphics instance to use for drawing.  This is set every time a drawing area
is created or when the device's graphics are set.
*/
protected Graphics _graphics;

/**
Drawing color.
*/
protected GRColor _color;
/**
Device associated with drawing area.  This is a canvas.
*/
protected GRDevice _dev;

/**
Aspect as in GRAspect_*.
*/
protected int _aspect;
/**
Type of X axis as in GRAxis.
*/
protected int _axisx;
/**
Type of Y axis as in GRAxis.
*/
protected int _axisy;

/**
Line cap type as defined in GRDrawingAreaUtil.CAP*
*/
protected int _linecap;
/**
Line join type as defined in GRDrawingAreaUtil.JOIN*
*/
protected int _linejoin;
/**
Drawing area status as defined in GRUtil.STAT*
*/
protected int _status;

/**
Name of font to use for text.
*/
protected String _font;
/**
Drawing area name.
*/
protected String _name;

/**
Default constructor.  <b>Do not use this!</b>
*/
public GRDrawingArea ()
{	String routine = "GRDrawingArea()";

	if ( Message.isDebugOn ) {
		Message.printDebug ( 10, routine,
		"Constructing using no arguments" );
	}
	Message.printWarning ( 2, routine, "Should not use void constructor" );
	//initialize ( null, "", GRAspect.TRUE, null, 0, 0, null );
}

/**
Constructor.
@param dev GRDevice associated with the drawing area.
@param name A name for the drawing area.
@param aspect Aspect for the axes of the drawing area.
@param draw_limits Drawing limits (device coordinates to attach the lower-left
and upper-right corner of the drawing area).
@param units Units of the limits (will be converted to device units).
@param flag Modifier for drawing limits.  If GRLimits.UNIT, then the limits are
assumed to be percentages of the device (0.0 to 1.0) and the units are not
used.
@param data_limits Data limits associated with the lower-left and upper-right
corners of the drawing area.
@see GRAspect
*/
public GRDrawingArea (	GRDevice dev, String name, int aspect,
			GRLimits draw_limits, int units, int flag,
			GRLimits data_limits )
{	String routine = "GRDrawingArea(...)";

	if ( Message.isDebugOn ) {
		Message.printDebug ( 10, routine,
		"Constructing using all arguments, name=\"" + name + "\"" );
	}
	initialize ( dev, name, aspect, draw_limits, units, flag, data_limits );
}

/**
Constructor.
@param dev the device associated with the drawing area
@param props a PropList containing the settings for the drawing area.
*/
public GRDrawingArea ( GRDevice dev, PropList props )
throws GRException
{	String routine = "GRDrawingArea(PropList)";
	if ( Message.isDebugOn ) {
		Message.printDebug ( 10, routine,
		"Constructing using PropList" );
	}
	try {	initialize ( dev, props );
	}
	catch ( GRException e ) {
		throw e;
	}
}

/**
Clear the drawing area (draw in the current color).
*/
public abstract void clear ( );

/**
Set the clip on/off for the drawing area.
@param flag Indicates whether clipping should be on or off.
*/
public abstract void clip ( boolean flag );

/**
Comment (not really applicable other than for hard-copy)
@param comment the comment to set
*/
public abstract void comment ( String comment );

/**
Draw an arc using the current color, line, etc.
@param x X-coordinate of center.
@param y Y-coordinate of center.
@param rx X-radius.
@param ry Y-radius.
@param a1 Initial angle to start drawing (0 is at 3 o'clock, then
counterclockwise).
@param a2 Ending angle.
*/
public abstract void drawArc(double x, double y, double rx, double ry,double a1,
			double a2 );

/**
Draw compound text.
@param text Vector of text to draw.
@param color the color to draw the text in.
@param x the x coordinate from which to start drawing the text
@param y the y coordinate from which to start drawing the text
@param angle the angle at which to draw the text
@param flag GRText.* value denoting where to draw the text
*/
public abstract void drawCompoundText (	Vector text, GRColor color, double x, 
double y, double angle, int flag );

/**
Draw a line.
@param x X-coordinates in data units.
@param y Y-coordinates in data units.
*/
public abstract void drawLine ( double [] x, double [] y );

/**
Draw a polygon in the current color.
@param npts the number of points in the polygon.
@param x array of x coordinates
@param y array of y coordinates
*/
public abstract void drawPolygon (	int npts, double x[], double y[] );

/**
Draws a polyline in the current color.
@param npts the number of points in the polyline
@param x array of x coordinates
@param y array of y coordinates
*/
public abstract void drawPolyline (	int npts, double x[], double y[] );

/**
Draws a rectangle in the current color.
@param xll the lower-left x coordinate
@param yll the lower-left y coordinate
@param width the width of the rectangle
@param height the height of the rectangle.
*/
public void drawRectangle ( double xll, double yll, double width, double height)
{
	double[]	x = new double[4], y = new double[4];

	x[0] = xll;
	y[0] = yll;
	x[1] = xll + width;
	y[1] = yll;
	x[2] = x[1];
	y[2] = yll + height;
	x[3] = xll;
	y[3] = y[2];
	drawPolygon ( 4, x, y );
	_lastx = x[0];
	_lasty = y[0];
}

/**
Draws text.
@param text the text to draw
@param x the x location of the text
@param y the y location of the text
@param a the alpha value of the text
@param flag the GRText.* flag to determine how text is drawn
*/
public abstract void drawText ( String text, double x, double y, double a, 
int flag );

/**
Draws text.
@param text the text to draw
@param x the x location of the text
@param y the y location of the text
@param a the alpha value of the text
@param flag the GRText.* flag to determine how text is drawn
@param degrees the number of degrees to rotate the text clock-wise.
*/
public abstract void drawText ( String text, double x, double y, double a, 
int flag, double degrees);

/**
Fills an arc using the current color, line, etc.
@param x X-coordinate of center.
@param y Y-coordinate of center.
@param rx X-radius.
@param ry Y-radius.
@param a1 Initial angle to start drawing (0 is at 3 o'clock, then
counterclockwise).
@param a2 Ending angle.
*/
public abstract void fillArc (	double x, double y, double rx, double ry,
				double a1, double a2, int fillmode );

/**
Draw a polygon in the current color.
@param npts the number of points in the polygon.
@param x array of x coordinates
@param y array of y coordinates
@param transparency transparency with which to draw the polygon
*/
public abstract void fillPolygon ( int npts, double x[], double y[], 
int transparency );

/**
Draw a polygon in the current color.
@param npts the number of points in the polygon.
@param x array of x coordinates
@param y array of y coordinates
*/
public abstract void fillPolygon (	int npts, double x[], double y[] );

/**
Fills a rectangle in the current color.
@param xll the lower-left x coordinate
@param yll the lower-left y coordinate
@param width the width of the rectangle
@param height the height of the rectangle.
*/
public abstract void fillRectangle (	double xll, double yll, double width,
				double height );

/**
Fills a rectangle in the current color.
@param limits GRLimits denoting the bounds of the rectangle.
*/
public abstract void fillRectangle ( GRLimits limits );

/**
Cleans up member variables.
*/
public void finalize()
throws Throwable {
	_graphics = null;
	_color = null;
	_dev = null;
	_font = null;
	_name = null;
	super.finalize();
}

/**
Flushes the drawing.
*/
public abstract void flush ();

/**
Returns the clipping shape on the current graphics context.
@return the clipping shape on the current graphics context.
*/
public abstract Shape getClip();

/**
The the data extents of the drawing area.
@param limits limits for drawing area.
*/
public abstract GRLimits getDataExtents ( GRLimits limits, int flag );

/**
Gets the data limits for the drawing area as a new copy of the data limits.
*/
public GRLimits getDataLimits ()
{
	return new GRLimits ( _datax1, _datay1, _datax2, _datay2 );
}

/**
Get the data values for device coordinates.
@return A GRPoint with the data point.
@param devx Device x-coordinate.
@param devy Device y-coordinate.
@param flag GR.COORD_DEVICE if the coordinates are originating with the device
(e.g., a mouse) or GR.COORD_PLOT if the coordinates are plotting coordinates
(this flag affects how the y-axis is reversed on some devices).
*/
public abstract GRPoint getDataXY ( double devx, double devy, int flag );

/**
Gets the drawing limits for the drawing area as a new copy of the limits.
*/
public GRLimits getDrawingLimits ()
{	return new GRLimits ( _drawx1, _drawy1, _drawx2, _drawy2 );
}

/**
Return the name of the drawing area.
@return The name of the drawing area.
*/
public String getName ()
{	return _name;
}

/**
Return the plotting limits for the drawing area in plotting or data units.
@param flag COORD_PLOT if the plotting units should be returned,
COORD_DEVICE if the limits should be returned in device units,
COORD_DATA if the limits should be returned in data units.
@return The plotting limits for the drawing area.
*/
public GRLimits getPlotLimits ( int flag )
{	
	// Get the limits in device units...

	GRLimits limits = new GRLimits ( _plotx1, _ploty1, _plotx2, _ploty2 );
	if ( Message.isDebugOn ) {
		Message.printDebug ( 10, "GR.getPlotLimits",
		"Plot limits in device units are " + limits );
	}

	if ( flag == COORD_PLOT ) {
		return limits;
	}
	else if ( flag == COORD_DATA ) {
		GRPoint p1 = getDataXY ( _plotx1, _ploty1, COORD_PLOT );
		GRPoint p2 = getDataXY ( _plotx2, _ploty2, COORD_PLOT );
		limits = new GRLimits(p1.x, p1.y, p2.x, p2.y );
		if ( Message.isDebugOn ) {
			Message.printDebug ( 10, "GR.getPlotLimits",
			"Plot limits in data units are " + limits );
		}
		return limits;
	}
	else {	// Raw device units.  Correct for flipped y-axis...
		if ( _dev.getReverseY() ) {
			GRLimits devlimits = _dev.getLimits();
			double maxy = devlimits.getMaxY();
			limits = new GRLimits ( _drawx1, (maxy - _drawy1),
				_drawx2, (maxy - _drawy2) );
			return limits;
		}
		else {	// Return limits computed in first step...
			return limits;
		}
	}
}

/**
Returns the extents of a string.  
@param text the text to get the extents for
@param flag
@return the extents of a string.
*/
public abstract GRLimits getTextExtents ( String text, int flag );

/**
Returns the units.
@return the units.
*/
public int getUnits ()
{
	return 0;
}

/**
Returns the x data point for the x device point.
@param xdev the x device point
@return the x data point for the x device point.
*/
public abstract double getXData ( double xdev );

/**
Returns the y data point for the y device point.
@param ydev the y device point
@return the y data point for the y device point.
*/
public abstract double getYData ( double ydev );

/**
Initialize the drawing area data.
Rely on default fonts, etc., for now.
@param dev the GRDevice associated with this drawing area
@param name the name of the drawing area
@param aspect the aspect of the drawing area, as defined in GRAspect.*
@param draw_limits the drawing limits of the drawing area
@param units the units of the drawing area
@param flag passed to setDrawingLimits()
@param data_limits the drawing area data limits
*/
private void initialize ( GRDevice dev, String name, int aspect,
			GRLimits draw_limits, int units, int flag,
			GRLimits data_limits )
{	String	routine = "GRDrawingArea.initialize";
	int	dl = 10;

	if ( Message.isDebugOn ) {
		Message.printDebug ( dl, routine, "Initializing" );
	}

	if ( dev == null ) {
		Message.printWarning ( 2, routine, "Null device." );
	}

	// Initialize the basic data members...

	initializeCommon();

	_plotx1		= 0.0;
	_plotx2		= 0.0;
	_ploty1		= 0.0;
	_ploty2		= 0.0;

	if ( name != null ) {
		_name = name;
	}

	// Set the device and let the device know that the drawing area is
	// associated with the device...
	_dev = dev;
	_dev.addDrawingArea ( this );
	_aspect = aspect;
	setDrawingLimits ( draw_limits, units, flag );
	setDataLimits ( data_limits );
	// Save local copies of data...
	_devyshift = _dev.getLimits().getTopY ();
	_reverse_y = _dev.getReverseY();
	_graphics = _dev.getPaintGraphics();
	if ( Message.isDebugOn ) {
		Message.printDebug ( 10, routine, "Device height is " +
		_devyshift );
	}

	// Initialize the global arrays used for plotting...

	if ( _global_xs == null ) {
		_global_xs = new double[10];
		_global_ys = new double[10];
		_global_len_s = 10;
		_global_ixs = new int[10];
		_global_iys = new int[10];
		_global_len_is = 10;
	}
}

/**
Initialize the drawing area.
@param dev the GRDevice this drawing area is associated with
@param props PropList with drawing area settings.
*/
private void initialize ( GRDevice dev, PropList props )
throws GRException
{	String	message, routine = "GRDrawingArea.initialize";
	int	dl = 10;

	if ( Message.isDebugOn ) {
		Message.printDebug ( dl, routine, "Initializing" );
	}

	if ( dev == null ) {
		message = "Null GRDevice.  Cannot create drawing area.";
		Message.printWarning ( 2, routine, message );
		throw new GRException ( message );
	}

	// Initialize the basic data members which are not configurable at
	// creation...

	initializeCommon();
	
	// Set passed-in values...

	_dev = dev;

	String prop_value;
	prop_value = props.getValue ( "Name" );
	if ( prop_value != null ) {
		_name = prop_value;
	}
	prop_value = props.getValue ( "Aspect" );
	if ( prop_value != null ) {
		if ( prop_value.equalsIgnoreCase("true") ) {
			_aspect = GRAspect.TRUE;
		}
		if ( prop_value.equalsIgnoreCase("fill") ) {
			_aspect = GRAspect.FILL;
		}
		if ( prop_value.equalsIgnoreCase("FIllX") ) {
			_aspect = GRAspect.FILLX;
		}
		if ( prop_value.equalsIgnoreCase("true") ) {
			_aspect = GRAspect.TRUE;
		}
	}

	// Initialize the global arrays used for plotting...

	if ( _global_xs == null ) {
		_global_xs = new double[10];
		_global_ys = new double[10];
		_global_len_s = 10;
		_global_ixs = new int[10];
		_global_iys = new int[10];
		_global_len_is = 10;
	}
}

/**
Common initialization to both initialize methods above
*/
private void initializeCommon() {
		_status		= GRUtil.STAT_OPEN;
	_axisx		= GRAxis.LINEAR;
	_axisy		= GRAxis.LINEAR;
	_color		= GRColor.white;
	_dataset	= false;
	// Initialize values in case there are problems setting from limits
	// below...
	_datax1		= 0.0;
	_datax2		= 0.0;
	_datay1		= 0.0;
	_datay2		= 0.0;
	_drawset	= false;
	_drawx1		= 0.0;
	_drawx2		= 0.0;
	_drawy1		= 0.0;
	_drawy2		= 0.0;	
	_font		= "Helvetica";
	_fontht		= 0.0;
	_lastx		= 0.0;
	_lasty		= 0.0;
	_linearx1	= 0.0;
	_linearx2	= 0.0;
	_lineary1	= 0.0;
	_lineary2	= 0.0;
	_name		= "";
}

/**
Do a linear, logarithmic, or probability interpolation for a drawing area.
This method determines what type of axes a drawing area has.  It then performs
the proper interpolation necessary to map the data value to the proper 
coordinate in the output. This method can also be used independently of a
drawing area by specifying a value of zero for 'axis'.
@param x the point to interpolate.
@param xmin the known minimum of the x data.
@param xmax the known maximum of the x data.
@param ymin the known minimum of the y data.
@param ymax the known maximum of the y data.
@param axis the axis to interpolate for. 
@return the interpolated value.
*/
/*
public double interp (	double x, double xmin, double xmax, double ymin,
			double ymax, int axis )
{
	double	y = x, z, zmax, zmin;
	int	flag;

	if ( axis == 0 ) {
		flag = GRAxis.LINEAR;
	}
	if (	((_axisx == GRAxis.LOG) && (axis == GRAxis.X)) ||
		((_axisy == GRAxis.LOG) && (axis == GRAxis.Y)) )
		flag = GRAxis.LOG;
	else if(((_axisx == GRAxis.STANDARD_NORMAL_PROBABILITY) 
		&& (axis == GRAxis.X)) ||
		((_axisy == GRAxis.STANDARD_NORMAL_PROBABILITY) 
		&& (axis == GRAxis.Y)) )
		flag = GRAxis.STANDARD_NORMAL_PROBABILITY;
	else	flag = GRAxis.LINEAR;

	if ( (xmax - xmin) == 0.0 ) {
		y = ymin;
	}
	else if ( flag == GRAxis.LINEAR ) {
		y = ymin + (ymax - ymin)*(x - xmin)/(xmax - xmin);
	}
	else if ( flag == GRAxis.LOG ) {
		/*
		** Axes look like:
		**
		**		|
		**		|
		** y=linear	|
		**   scale	|
		**		------------------
		**		 x=log scale
		*/
		/*
		if ( (x <= 0.0) || (xmin <= 0.0) || (xmax <= 0.0) ) {
			y = ymin;
		}
		else {	y = ymin +
				(ymax - ymin)*(MathUtil.log10(x/xmin))/
				(MathUtil.log10(xmax/xmin));
		}
	}
	else if ( flag == GRAxis.STANDARD_NORMAL_PROBABILITY ) {
		/*
		** Probability scale...
		**
		** We are given a probability in the range 0.0 to 1.0.  To
		** calculate the plotting point, we need to back-calculate the
		** linearized data value ("z") and map this onto the axis.  By
		** doing so for the tenth probability values, we will end up
		** with a scale that has more "tic-marks" in the center of the
		** page, with wider spacing farther from the center.
		**
		** If the "left" probability limit for the axis is less than the
		** "right" limit, the value calculated is the probability of the
		** value being less than the given value.  If the "left"
		** probability limit is greater than the "right" value, then the
		** value calculated is the probability of the value being
		** exceeded.
		*/
		/*
		NOT SUPPORTED AT THIS TIME
		if ( x == xmin ) {
			y = ymin;
		}
		else if ( x == xmax ) {
			y = ymax;
		}
		else {	if ( axis == GRAxis.X ) {
				zmin = _linearx1;
				zmax = _linearx2;
			}
			else {	zmin = _lineary1;
				zmax = _lineary2;
			}
			/*
			z = GRFuncZStdNormCum ( x );
			y = GRInterp ( z, zmin, zmax, ymin, ymax );
			*/
		/*
		}
		*/
		/*
	}
	return y;
}
*/

/**
Checks to see if this drawing area's device is drawing antialised.
@return true if the device is drawing anti aliased, false if not.
*/
public boolean isDeviceAntiAliased() {
	return _dev.isAntiAliased();
}

/**
Draw a line from the current pen position to a point.
@param x the x coordinate to draw to
@param y the y coordinate to draw to
*/
public abstract void lineTo ( double x, double y );

/**
Draw a line from the current pen position to a point.
@param point the GRPoint to draw to
*/
public abstract void lineTo ( GRPoint point );

/**
Move the pen to a point.
@param x x coordinate to move to
@param y y cooridnate to move to
*/
public abstract void moveTo ( double x, double y );

/**
Move the pen position to a point
@param point point to move to
*/
public abstract void moveTo ( GRPoint point );

/**
Scale x data value to device plotting coordinate.
@param xdata value to scale
*/
public double scaleXData ( double xdata )
{	double xdev;

	if (_axisx == GRAxis.LOG) {
		if ( (_datax2 - _datax1) == 0.0 ) {
			xdev = _plotx1;
		}	
		else {
			/*
			** Axes look like:
			**
			**		|
			**		|
			** y=linear	|
			**   scale	|
			**		------------------
			**		 x=log scale
			*/	
			if ( (xdata <= 0.0) || (_datax1 <= 0.0) 
				|| (_datax2 <= 0.0) ) {
				xdev = _plotx1;
			}
			else {	xdev = _plotx1 +
					(_plotx2 - _plotx1)
					*(MathUtil.log10(xdata/_datax1))/
					(MathUtil.log10(_datax2/_datax1));
			}
		}
	}
	else if (_axisx == GRAxis.STANDARD_NORMAL_PROBABILITY) {
		/*
		** Probability scale...
		**
		** We are given a probability in the range 0.0 to 1.0.  To
		** calculate the plotting point, we need to back-calculate the
		** linearized data value ("z") and map this onto the axis.  By
		** doing so for the tenth probability values, we will end up
		** with a scale that has more "tic-marks" in the center of the
		** page, with wider spacing farther from the center.
		**
		** If the "left" probability limit for the axis is less than the
		** "right" limit, the value calculated is the probability of the
		** value being less than the given value.  If the "left"
		** probability limit is greater than the "right" value, then the
		** value calculated is the probability of the value being
		** exceeded.
		*/
		double xtemp = -1;
		if (xdata == _datax1) {
			xdev = _plotx1;
		}
		else if (xdata == _datax2) {
			xdev = _plotx2;
		}
		else {	
			double dataMin = 0;
			double dataMax = 0;
			dataMin = _linearx1;
			dataMax = _linearx2;

			try {
				xtemp = MathUtil.interpolate(xdata, 0, 1,
					-5, 5);
				xtemp = 
					StandardNormal.cumulativeStandardNormal(
					xtemp);
				xdev = MathUtil.interpolate(xtemp, dataMin, 
					dataMax, _plotx1, _plotx2);
			}
			catch (Exception e) {
				String routine = "GRDrawingArea.scaleXData";
				Message.printWarning(2, routine, 
					"Error calculating standard normal "
					+ "value.");
				Message.printWarning(2, routine, e);
				xdev = _plotx1;
			}
		}
	}
	else {
		xdev= MathUtil.interpolate ( xdata, _datax1, _datax2, 
			_plotx1, _plotx2);
	}

	if ( Message.isDebugOn ) {
		String routine = "GRDrawingArea.scaleXData";
		Message.printDebug ( 100, routine,
		"Scaled X data " + xdata + " to dev " + xdev );
	}
	return xdev;
}

/**
Scale y data value to device plotting coordinate.
@param ydata value to scale.
*/
public double scaleYData ( double ydata )
{	double ydev;

	if (_axisy == GRAxis.LOG) {
		if ( (_datay2 - _datay1) == 0.0 ) {
			ydev = _ploty1;
		}	
		else {
			/*
			** Axes look like:
			**
			**		|
			**		|
			** y=linear	|
			**   scale	|
			**		------------------
			**		 x=log scale
			*/	
			if ( (ydata <= 0.0) || (_datay1 <= 0.0) 
				|| (_datay2 <= 0.0) ) {
				ydev = _ploty1;
			}
			else {	ydev = _ploty1 +
					(_ploty2 - _ploty1)
					*(MathUtil.log10(ydata/_datay1))/
					(MathUtil.log10(_datay2/_datay1));
			}
		}
	}
	else if (_axisx == GRAxis.STANDARD_NORMAL_PROBABILITY) {
		/*
		** Probability scale...
		**
		** We are given a probability in the range 0.0 to 1.0.  To
		** calculate the plotting point, we need to back-calculate the
		** linearized data value ("z") and map this onto the axis.  By
		** doing so for the tenth probability values, we will end up
		** with a scale that has more "tic-marks" in the center of the
		** page, with wider spacing farther from the center.
		**
		** If the "left" probability limit for the axis is less than the
		** "right" limit, the value calculated is the probability of the
		** value being less than the given value.  If the "left"
		** probability limit is greater than the "right" value, then the
		** value calculated is the probability of the value being
		** exceeded.
		*/
		if (ydata == _datay1) {
			ydev = _ploty1;
		}
		else if (ydata == _datay2) {
			ydev = _ploty2;
		}
		else {	
			double dataMin = 0;
			double dataMax = 0;
			dataMin = _lineary1;
			dataMax = _lineary2;

			try {
				double ytemp = 
					StandardNormal.cumulativeStandardNormal(
					ydata);
				ydev = MathUtil.interpolate(ytemp, dataMin, 
					dataMax, _ploty1, _ploty2);
			}
			catch (Exception e) {
				String routine = "GRDrawingArea.scaleYData";
				Message.printWarning(2, routine, 
					"Error calculating standard normal "
					+ "value.");
				Message.printWarning(2, routine, e);
				ydev = _ploty1;
			}
		}
	}
	else {
		ydev=MathUtil.interpolate ( ydata, _datay1, _datay2, 
			_ploty1, _ploty2);
	}

	if ( Message.isDebugOn ) {
		String routine = "GRDrawingArea.scaleYData";
		Message.printDebug ( 100, routine,
		"Scaled Y data " + ydata + " to dev " + ydev );
	}
	return ydev;
}

/**
Sets the axes
@param axisx the x axis to set (see GRAxis.LINEAR, etc)
@param axisy the y axis to set (see GRAxis.LINEAR, etc)
*/
public void setAxes ( int axisx, int axisy )
{
	_axisx = axisx;
	_axisy = axisy;

	if ( _drawset ) {
		setPlotLimits ();
	}
}

/**
Sets the clipping area, in data limits.  If null, the clip is removed.
@param dataLimits the limits of the data area that will be clipped.
*/
public abstract void setClip(GRLimits dataLimits);

/**
Sets the clipping area, in data limits.  If null, the clip is removed.
@param clip the shape to clip to
*/
public abstract void setClip(Shape clip);

/**
Set the current color.
@param color GRColor to use.
@see GRColor
*/
public abstract void setColor( GRColor color );

/**
Set the current color.
@param r Red component in range 0.0 to 1.0.
@param g Green component in range 0.0 to 1.0.
@param b Blue component in range 0.0 to 1.0.
*/
public abstract  void setColor ( float r, float g, float b );

/**
Set the current color.
@param r Red component in range 0.0 to 1.0.
@param g Green component in range 0.0 to 1.0.
@param b Blue component in range 0.0 to 1.0.
*/
public abstract void setColor ( double r, double g, double b );

/**
Set the data limits for the drawing area.
@param xleft the leftmost x coordinate
@param ybottom the bottom y coordinate
@param xright the rightmost x coordinate
@param ytop the top y coordinate
*/
public void setDataLimits (	double xleft, double ybottom, double xright,
				double ytop )
{	int	dl = 10;

	_datax1 = xleft;
	_datay1 = ybottom;
	_datax2 = xright;
	_datay2 = ytop;
	_dataset = true;
	if ( Message.isDebugOn ) {
		Message.printDebug ( dl,
		"GRDrawingArea.setDataLimits(x1,y1,x2,y2)",
		"Set data limits to (" + _datax1 + "," + _datay1 + ") (" +
		_datax2 + "," + _datay2 + ")" );
	}
	if ( _drawset ) {
		setPlotLimits ();
	}
}

/**
Set the data limits for the drawing area.
@param limits GRLimits containing data limits.
*/
public void setDataLimits ( GRLimits limits )
{ 	if ( limits == null ) {
		if ( Message.isDebugOn ) {
			Message.printDebug ( 10, "GRDrawingArea.setDataLimits",
			"Null GRLimits" );
		}
		return;
	}
	setDataLimits ( limits.getLeftX(), limits.getBottomY(),
			limits.getRightX (), limits.getTopY () );
}

/**
Sets whether this drawing area's device should begin drawing antialiased.
Currently, on GRJComponentDevice Objects support this.
@param antiAlias if true, the device will be told to begin drawing antialiased.
*/
public void setDeviceAntiAlias(boolean antiAlias) {
	_dev.setAntiAlias(antiAlias);
}

/**
Set the drawing limits (device limits) for the drawing area.
@param limits Drawing limits (device coordinates to attach the lower-left
and upper-right corner of the drawing area).
@param units Units of the limits (will be converted to device units).
@param flag Modifier for drawing limits.  If GRLimits.UNIT, then the limits are
assumed to be percentages of the device (0.0 to 1.0) and the units are not
used.
@see GRUnits
*/
public void setDrawingLimits ( GRLimits limits, int units, int flag )
{	if ( limits == null ) {
		return;
	}
	if ( Message.isDebugOn ) {
		Message.printDebug ( 10,
		"GRDrawingArea.setDrawingLimits(GRLimits)",
		"Limits values are: " + limits );
	}
	setDrawingLimits (	limits.getLeftX(), limits.getBottomY(),
				limits.getRightX(), limits.getTopY(), units,
				flag );
}

/**
Sets the drawing limits (device limits) for the drawing area.
@param xmin the lowest x value
@param ymin the lowest y value
@param xmax the highest x value
@param ymax the highest y value
@param units the units for the drawing area
@param flag kind of limits (either GRLimits.DEVICE or GRLimits.UNIT)
*/
public void setDrawingLimits (	double xmin, double ymin, double xmax,
				double ymax, int units, int flag )
{	String	routine = "GRDrawingArea.setDrawingLimits(x1,y1,x2,y2)";
	int	dl = 10;
	if ( flag == GRLimits.DEVICE ) {
		int dev_units = _dev.getUnits();
		if ( units == GRUnits.DEVICE ) {
			// Just in case developer passes in generic device
			// units instead of actual units...
			units = dev_units;
		}
		_drawx1 = GRUnits.convert ( xmin, units, dev_units );
		_drawy1 = GRUnits.convert ( ymin, units, dev_units );
		_drawx2 = GRUnits.convert ( xmax, units, dev_units );
		_drawy2 = GRUnits.convert ( ymax, units, dev_units );
	}
	else if ( flag == GRLimits.UNIT ) {
		GRLimits devlimits = _dev.getLimits ();
		_drawx1 = MathUtil.interpolate ( xmin, 0.0, 1.0, 
			devlimits.getLeftX(), devlimits.getRightX());
		_drawy1 = MathUtil.interpolate ( ymin, 0.0, 1.0, 
			devlimits.getBottomY(), devlimits.getTopY());
		_drawx2 = MathUtil.interpolate ( xmax, 0.0, 1.0, 
			devlimits.getLeftX(), devlimits.getRightX());
		_drawy2 = MathUtil.interpolate ( ymax, 0.0, 1.0, 
			devlimits.getBottomY(), devlimits.getTopY());
	}
	if ( Message.isDebugOn ) {
		Message.printDebug ( dl, routine,
		"Set drawing limits to (" + _drawx1 + "," + _drawy1 + ") (" +
		_drawx2 + "," + _drawy2 + ")" );
	}
	_drawset = true;
	if ( _dataset ) {
		setPlotLimits ();
	}
}

//	int setDrawMask (	int drawmask );

/**
Set the font for the drawing area.
@param name Font name (e.g., "Helvetica").
@param style Font style ("Plain", "Bold", or "Italic").
@param size Font height in points.
*/
public abstract void setFont ( String name, String style, double size );

/**
Set the last X data value drawn.
@param lastx the lastx data value drawn
*/
public void setLastX ( double lastx )
{	_lastx = lastx;
}

/**
Set the last X, Y data value drawn.
@param lastx the last x data value drawn
@param lasty the last y data value drawn
*/
public void setLastXY ( double lastx, double lasty )
{
	setLastX(lastx);
	setLastY(lasty);
}

/**
Set the last Y data value drawn.
@param lasty the last y data value drawn
*/
public void setLastY ( double lasty )
{	_lasty = lasty;
}

/**
Set the line pattern
@param dash an array defining the dash pattern
@param offset the line offset
*/
public abstract void setLineDash ( double dash[], double offset );
/*
{	String routine = "GRDrawingArea.setLineDash";
	Message.printWarning ( 2, routine, "Define in derived class" );
}
*/

/**
Set the line join style.
@param join the line join style, one of GRDrawingAreaUtil.JOIN*
public abstract void setLineJoin ( int join );
/*
{	String routine = "GRDrawingArea.setLineJoin";
	Message.printWarning ( 2, routine, "Define in derived class" );
}
*/

/**
Set the line width.
REVISIT (SAM - 2003-05-07)
Need to standardize on points, pixels, etc.  Both, depending on output?
@param linewidth the width of the line
*/
public abstract void setLineWidth ( double linewidth );

/**
Set the plot limits knowing that the data and device limits are set.
*/
public void setPlotLimits ()
{	String		routine = "GRDrawingArea.setPlotLimits";
	double		height, width, xpercent, xrange, ypercent, yrange;
	int		dl = 10;

	if ( Message.isDebugOn ) {
		Message.printDebug ( dl, routine,
		"Setting the plot limits._axisx=" + _axisx + " _axisy=" +
		_axisy );
	}

	if ( !_dataset ) {
		if ( Message.isDebugOn ) {
			Message.printDebug ( dl, routine,
			"Data limits are not set.  Not setting plot limits." );
		}
		return;
	}
	if ( !_drawset ) {
		if ( Message.isDebugOn ) {
			Message.printDebug ( dl, routine,
			"Drawing limits are not set.  Not setting plot " +
			"limits." );
		}
		return;
	}
	if ( _aspect == GRAspect.TRUE ) {
		// Plot is to maintain true scale aspect of data
		// in horizontal and vertical directions...
		width	= _drawx2 - _drawx1;
		height	= _drawy2 - _drawy1;
		xrange	= _datax2 - _datax1;
		if ( xrange < 0.0 ) {
			xrange *= -1.0;
		}
		yrange	= _datay2 - _datay1;
		if ( yrange < 0.0 ) {
			yrange *= -1.0;
		}
		if ( (xrange/yrange) > (width/height) ) {
			xpercent	= 100.0;
			ypercent	= 100.0*yrange*width/(xrange*height);
		}
		else {	ypercent	= 100.0;
			xpercent	= 100.0*xrange*height/(yrange*width);
		}
		_ploty1 =	height/2.0 - height*ypercent/200.0 + _drawy1;
		_ploty2 =	height/2.0 + height*ypercent/200.0 + _drawy1;
		_plotx1 =	width/2.0 - width*xpercent/200.0 + _drawx1;
		_plotx2 =	width/2.0 + width*xpercent/200.0 + _drawx1;
	}
	else if ( _aspect == GRAspect.FILL ) {
		// Fill the drawing area in both directions...
		_plotx1 =	_drawx1;
		_ploty1 =	_drawy1;
		_plotx2 =	_drawx2;
		_ploty2 =	_drawy2;
	}
	else if ( _aspect == GRAspect.FILLX ) {
		// Fill the drawing area in the X-direction...
		_plotx1 =	_drawx1;
		_plotx2 =	_drawx2;
	}
	else if ( _aspect == GRAspect.FILLY ) {
		// Only fill the drawing area in the Y-direction...
		_ploty1 =	_drawy1;
		_ploty2 =	_drawy2;
	}
	// Save the linearized data limits used for interpolating plotting
	// positions.  For a linear scale, these are just the normal limits.
	// For a log scale, these are the log10 values.  If we are using a
	// probability axis, need to calculate linearized Z data values
	// corresponding to probabilities (to be used in interpolating plotting
	// coordinates)...
	if ( _axisx == GRAxis.LINEAR ) {
		_linearx1 = _datax1;
		_linearx2 = _datax2;
	}
	else if ( _axisx == GRAxis.LOG ) {
		_linearx1 = MathUtil.log10(_datax1);
		_linearx2 = MathUtil.log10(_datax2);
	}
	else if ( _axisx == GRAxis.STANDARD_NORMAL_PROBABILITY ) {
		try {
			_linearx1 = StandardNormal.cumulativeStandardNormal(-5);
			_linearx2 = StandardNormal.cumulativeStandardNormal(5);
		}
		catch (Exception e) {
			Message.printWarning(2, routine, 
				"Error calculating data limits for standard "
				+ "normal X.  Drawing will not work properly --"
				+ " find and fix the error!");
			Message.printWarning(2, routine, e);
		}
		if (Message.isDebugOn) {
			Message.printDebug(10, routine,
				"Set linearized X data limits to "
				+ _linearx1 + " to " + _linearx2);
		}
	}
	else {	Message.printWarning ( 2, routine,
		"X axis type " + _axisx + " is not recognized.  Big problem!");
	}
	if ( _axisy == GRAxis.LINEAR ) {
		_lineary1 = _datay1;
		_lineary2 = _datay2;
	}
	else if ( _axisy == GRAxis.LOG ) {
		_lineary1 = MathUtil.log10(_datay1);
		_lineary2 = MathUtil.log10(_datay2);
	}
	else if ( _axisy == GRAxis.STANDARD_NORMAL_PROBABILITY ) {
		try {
			_lineary1 = StandardNormal.cumulativeStandardNormal(-5);
			_lineary2 = StandardNormal.cumulativeStandardNormal(5);
		}
		catch (Exception e) {
			Message.printWarning(2, routine, 
				"Error calculating data limits for standard "
				+ "normal X.  Drawing will not work properly --"
				+ " find and fix the error!");
			Message.printWarning(2, routine, e);
		}		
		if (Message.isDebugOn) {
			Message.printDebug(10, routine,
				"Set linearized Y data limits to "
				+ _lineary1 + " to " + _lineary2);
		}		
	}
	else {	Message.printWarning ( 2, routine,
		"Y axis type " + _axisy + " is not recognized.  Big problem!" );
	}

	if ( Message.isDebugOn ) {
		Message.printDebug ( 10, routine, "Plot limits are: " +
		_plotx1 + "," + _ploty1 + " " + _plotx2 + "," + _ploty2 );
	}
}

public abstract void pageEnd();
public abstract void pageStart();


} // End GRDrawingArea