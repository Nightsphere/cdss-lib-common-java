// ----------------------------------------------------------------------------
// TSProcessor - process time series for output products (graphs, reports, etc.)
// ----------------------------------------------------------------------------
// REVISIT SAM 2005-11-04
// This class should probably be named TSProductProcessor, in particular since
// there are now TSCommandsProcessor and TSAnalyst classes.
// ----------------------------------------------------------------------------
// History:
//
// 2001-11-07	Steven A. Malers, RTi	Initial version.  Exploring concept of
//					a generic TSEngine like that used in
//					TSTool.
// 2002-01-17	SAM, RTi		Rename TSProcessor to TSProcessorNoSwing
//					to allow support of Swing and AWT,
//					consistent with other RTi packages.
//					When time allows, a new TSProcessor
//					class will be implemented that uses
//					Swing.
// 2002-04-25	SAM, RTi		Update due to TSSupplier changes.
// 2002-06-01	SAM, RTi		Add ability to add a WindowListener to
//					the TSViewFrame that is created.
// ==================================
// 2002-11-12	SAM, RTi		Copy AWT version and update to Swing.
// 2002-11-18	J. Thomas Sapienza, RTi	Add stub so that BufferedImage code
//					can be compiled under 1.1.8 and 1.4.0.
// 2003-06-04	SAM, RTi		* Final updates to Swing using latest
//					  GR and TS.
//					* Change "Enabled" property to
//					  "IsEnabled" - consistent with
//					  RiversideDB conventions.  Include
//					  support for Enabled also.
//					* Remove batch image kludge for 1.1.8
//					  since 1.4.0 or greater will be
//					  supported.
//					* Add support for templates.
// 2004-10-11	JTS, RTi		Added the ability to retrieve the last
//					TSViewJFrame that was created when
//					a graph is opened.
// 2005-04-27	JTS, RTi		Added all data members to finalize().
// 2005-10-09	JTS, RTi		Added beginProcessProduct() and
//					finishProcessProduct() to use when
//					applying alert annotations to a product.
// 2005-10-27	SAM, RTi		* An exception creating the graph was
//					  being absorbed.  Now throw a new
//					  exception so that application code
//					  knows that there was a problem.
//					* Change level 1 status messages to
//					  level 2.
// 2005-10-28	SAM, RTi		After opening the graph window call
//					needToCloseGraph() and if true throw
//					an exception.  This indicates that the
//					data were bad.
// 2005-11-07	JTS, RTi		Removed beginProcessProduct() and 
//					endProcessProduct().
//
// 2006-09-11	Kurt Tometich, RTi	Added functionality for writing
//					ProductType=Report, with subproducts
// 					written to an output file.  Added
//					function processReportProduct() to
//					handle this.  The only report format
//					that is supported at this time is
//					DateValue; however, it has been 
// 					built to support other types in the
//					future.  An example product file would
//					have a property in the product section
//					with ProductType=Report.  Each
//					subproduct's data time series must have
//					the same interval and must have
//					ReportType and OutputFile property set.
// 2006-09-28	SAM, RTi		Review KAT work.
//					Change the default OutputFile extension
//					to png to produce higher quality files.
// 2007-05-08	SAM, RTi		Cleanup code based on Eclipse feedback.
// ----------------------------------------------------------------------------
// EndHeader

package RTi.GRTS;

import java.awt.event.WindowListener;
import java.util.Vector;

import javax.swing.JFrame;

import RTi.TS.TS;
import RTi.TS.TSSupplier;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.TS.DateValueTS;

import java.awt.image.BufferedImage;

/**
The TSProcessor class provides methods to query and process time series
into output products (graphs, reports, etc.).  It uses the AWT GRTS and GR
classes.  This class is under development.
An example of implementation is as follows:
<pre>
	try {	TSProcessor p = new TSProcessor ();
		p.addTSSupplier ( this );
		p.processProduct ( "C:\\tmp\\Test.tsp", new PropList("x") );
	}
	catch ( Exception e ) {
		Message.printWarning ( 1, "TSToolMainGUI.test",
		"Error processing the product." );
	}
</pre>
*/
public class TSProcessor
{

/**
TSSuppliers to use when reading time series.
*/
private TSSupplier[] _suppliers = null;

/**
The last TSViewJFrame created and opened when displaying a graph.
*/
private TSViewJFrame __lastTSViewJFrame = null;

/**
A single WindowListener that can be associated with the TSViewFrame.  This is
being tested to determine whether an application like TSTool can detect a
TSViewJFrame closing and close the application.
*/
private WindowListener _tsview_window_listener = null;

public TSProcessor ()
{
}

/**
Add a time series supplier.  Suppliers are used to query time series based on a
time series identifier.
@param supplier TSSupplier to use with the TSProcessor.
*/
public void addTSSupplier ( TSSupplier supplier )
{	// Use arrays to make a little simpler than Vectors to use later...
	if ( supplier != null ) {
		// Resize the supplier array...
		if ( _suppliers == null ) {
			_suppliers = new TSSupplier[1];
			_suppliers[0] = supplier;
		}
		else {	// Need to resize and transfer the list...
			int size = _suppliers.length;
			TSSupplier [] newsuppliers =
				new TSSupplier[size + 1];
			for ( int i = 0; i < size; i++ ) {
				newsuppliers[i] = _suppliers[i];
			}
			_suppliers = newsuppliers;
			_suppliers[size] = supplier;
			newsuppliers = null;
		}
	}
}

/**
Add a WindowListener for TSViewFrame instances that are created.  Currently
only one listener can be set.
@param listener WindowListener to listen to TSViewFrame WindowEvents.
*/
public void addTSViewWindowListener ( WindowListener listener )
{	_tsview_window_listener = listener;
}

/**
Clean up for garbage collection.
@exception Throwable if an error occurs.
*/
protected void finalize()
throws Throwable {
	IOUtil.nullArray(_suppliers);
	__lastTSViewJFrame = null;
	_tsview_window_listener = null;
	super.finalize();
}

/**
Returns the last TSViewJFrame created when displaying a graph.
@return the last TSViewJFrame created when displaying a graph.
*/
public TSViewJFrame getLastTSViewJFrame() {
	return __lastTSViewJFrame;
}

/**
Process a graph product.
@param tsproduct Time series product definition.
@exception Exception if the product cannot be processed (e.g., the graph cannot
be created due to a lack of data).
*/
private void processGraphProduct ( TSProduct tsproduct )
throws Exception
{	

	String routine = "TSProcessor.processGraphProduct";
	Vector tslist = new Vector(10);
	TS ts = null;
	// Loop through the sub-products (graphs on page).  Currently only
	// support one graph per page but this will change in the future...
	String tsid;
	String tsalias;
	String prop_value = null;
	DateTime date1 = null;
	DateTime date2 = null;
	Message.printStatus ( 2, "", "Processing product" );
	int nsubs = tsproduct.getNumSubProducts();
	prop_value = tsproduct.getLayeredPropValue("IsTemplate", -1, -1 );
	boolean is_template = false;
	if ( (prop_value != null) && prop_value.equalsIgnoreCase("true") ) {
		is_template = true;
		Message.printStatus ( 2, routine, "Processing template." );
	}
	for ( int isub = 0; isub < nsubs ;isub++ ) {
		Message.printStatus ( 2, routine,
		"Reading time series for subproduct [" + isub + "]" );
		// New...
		prop_value =
		tsproduct.getLayeredPropValue("IsEnabled", isub, -1 );
		// Old...
		if ( prop_value == null ) {
			prop_value =
			tsproduct.getLayeredPropValue("Enabled", isub, -1 );
		}
		if (	(prop_value != null) &&
			prop_value.equalsIgnoreCase("false") ) {
			continue;
		}
		// Loop through the time series in the subproduct
		for ( int i = 0; ; i++ ) {
			// New version...
			prop_value = tsproduct.getLayeredPropValue (
					"IsEnabled", isub, i );
			// Old version...
			if ( prop_value == null ) {
				prop_value = tsproduct.getLayeredPropValue (
					"Enabled", isub, i );
			}
			if (	(prop_value != null) &&
				prop_value.equalsIgnoreCase("false") ) {
				// Add a null time series...
				tslist.addElement ( (TS)null );
				continue;
			}
			prop_value = tsproduct.getLayeredPropValue (
					"PeriodStart", isub, i );
			if ( prop_value != null ) {
				try {	date1 = DateTime.parse ( prop_value );
				}
				catch ( Exception e ) {
					date1 = null;
				}
			}
			prop_value = tsproduct.getLayeredPropValue (
					"PeriodEnd", isub, i );
			if ( prop_value != null ) {
				try {	date2 = DateTime.parse ( prop_value );
				}
				catch ( Exception e ) {
					date2 = null;
				}
			}
			// Make sure this is last since the TSID is used in the
			// following readTimeSeries() call...
			if ( is_template ) {
				tsid = tsproduct.getLayeredPropValue (
				"TemplateTSID", isub, i, false );
			}
			else {	// Just get the normal property...
				tsid = tsproduct.getLayeredPropValue (
				"TSID", isub, i, false );
			}
			if ( tsid == null ) {
				// No more time series...
				break;
			}
			// Make sure we have both or none...
			if ( (date1 == null) || (date2 == null) ) {
				date1 = null;
				date2 = null;
			}
			// First try to read the time series using the
			// "TSAlias".  This normally will only return non-null
			// for something like TSTool where the time series may
			// be in memory.
			tsalias = tsproduct.getLayeredPropValue (
				"TSAlias", isub, i, false );
			if (	!is_template &&
				(tsalias != null) &&
				!tsalias.trim().equals("") ) {
				// Have the property so use the TSAlias instead
				// of the TSID...
				Message.printStatus ( 2, routine,
				"Reading TSAlias \"" + tsalias +
				"\" from TS suppliers." );
				try {	ts = readTimeSeries (	tsalias.trim(),
								date1, date2,
								null, true );
				}
				catch ( Exception e ) {
					// Always add a time series because
					// visual properties are going to be
					// tied to the position of the time
					// series.
					Message.printWarning ( 2, routine,
					"Error getting time series \"" +
					tsalias.trim() + "\"" );
					ts = null;
				}
			}
			else {	// Don't have a "TSAlias" so try to read the
				// time series using the full "TSID"...
				Message.printStatus ( 2, routine,
				"Reading TSID \"" + tsid +
				"\" from TS suppliers.");
				try {	ts = readTimeSeries (	tsid.trim(),
								date1, date2,
								null, true );
				}
				catch ( Exception e ) {
					// Always add a time series because
					// visual properties are going to be
					// tied to the position of the time
					// series.
					ts = null;
				}
				if ( ts == null  ) {
					Message.printWarning ( 2, routine,
					"Error getting time series \"" +
					tsid.trim() + "\".  Setting to null." );
				}
				else if ( is_template ) {
					// Non-null TS.
					// The TemplateTSID was requested but
					// now the actual TSID needs to be
					// set...
					tsproduct.setPropValue(
						"TSID",
						ts.getIdentifier().toString(),
						isub, i );
				}
			}
			tslist.addElement ( ts );
		}
	}

	// Now add the time series to the TSProduct...

	tsproduct.setTSList ( tslist );

	// Now create the graph.  For now use the PropList associated with the
	// TSProduct.  The use of a frame seems to be necessary to get this to
	// work (tried lots of other things including just declaring a TSGraph),
	// but could not get the combination of Graphics, Image, etc. to work.

	PropList tsviewprops = tsproduct.getPropList();

	JFrame f = new JFrame();
	f.addNotify();
	String graph_file =
		tsproduct.getLayeredPropValue ( "OutputFile", -1, -1 );
	if ( graph_file == null ) {
		if ( IOUtil.isUNIXMachine() ) {
			graph_file = "/tmp/tmp.png";
		}
		else {	graph_file = "C:\\TEMP\\tmp.png";
		}
	}
	String preview_output =
		tsproduct.getLayeredPropValue ( "PreviewOutput", -1, -1 );
	try {
		if (	(preview_output != null) &&
			preview_output.equalsIgnoreCase("true") ) {
			// Create a TSViewJFrame (an output file can still be created below)...
			TSViewJFrame tsview = new TSViewJFrame ( tsproduct );
			if ( tsview.needToCloseGraph() ) {
				throw new Exception (
					"Graph was automatically closed due to " +
				"data problem." );
			}
			__lastTSViewJFrame = tsview;
			// Put this in to test letting TSTool shut down when
			// a single TSView closes (and no main GUI is visible)..
			if ( _tsview_window_listener != null ) {
				tsview.addWindowListener (
						_tsview_window_listener );
			}
		}
		// TODO SAM 2007-06-22 Need to figure out how to combine on-screen
		// drawing with file to do one draw, if possible.
		if ( (graph_file != null) && (graph_file.length() > 0) ){
			// Create an in memory image and let the
			// TSGraphJComponent draw to it.  Use properties since
			// that was what was done before...
			// Image image = f.createImage(width,height);
			// Image ii = f.createImage(width, height);
			// Make this the same size as the TSGraph defaults and
			// then reset with the properties...
			int width = 400;
			int height = 400;
			prop_value = tsproduct.getLayeredPropValue (
				"TotalWidth", -1, -1 );
			if ( prop_value != null ) {
				width = StringUtil.atoi(prop_value);
			}
			prop_value = tsproduct.getLayeredPropValue (
				"TotalHeight", -1, -1 );
			if ( prop_value != null ) {
				height = StringUtil.atoi(prop_value);
			}
			BufferedImage image = new BufferedImage (width, 
				height,	BufferedImage.TYPE_3BYTE_BGR);
			if ( image == null ) {
				Message.printStatus ( 2, routine, "Image is null" );
			}
			tsviewprops.set(new Prop("Image", image, "") );
			TSGraphJComponent graph = 
				new TSGraphJComponent (	null, tsproduct, tsviewprops );
			if ( graph.needToClose() ) {
				throw new Exception (
				"Graph was automatically closed due to data problem." );
			}
			graph.paint ( image.getGraphics() );
			// Figure out the output file name for the product...
			Message.printStatus ( 2, routine, 
				"Saving graph to image file \"" + graph_file + "\"" );
			graph.saveAsFile ( graph_file );
			Message.printStatus ( 2, "", "Done" );
			graph_file = null;
			graph = null;
			image = null;
		}
	}
	catch ( Exception e ) {
		Message.printWarning ( 2, "TSProcessor.processGraphProduct",
		"Unable to create graph." );
		Message.printWarning ( 2, "TSProcessor.processGraphProduct", e);
		// Throw a new error...
		throw new Exception ( "Unable to create graph." );
	}

	// Clean up...
	date1 = null;
	date2 = null;
	ts = null;
	f = null;
	prop_value = null;
	ts = null;
	tsviewprops = null;
	tslist = null;
}

/**
Process a time series product file.
@param filename Name of time series product file.
@param override_props Properties to override the properties in the product file
(e.g., to set the period for the plot dynamically).
@exception Exception if the product cannot be processed (e.g., the graph cannot
be created due to a lack of data).
*/
public void processProduct ( String filename, PropList override_props ) throws Exception
{	Message.printStatus ( 2, "", "Processing time series product \"" +
		filename + "\"" );
	TSProduct tsproduct = new TSProduct (filename, override_props);	
	processProduct ( tsproduct );
	tsproduct = null;
}

/**
Process a time series product.
@param tsproduct Time series product definition.
@exception Exception if the product cannot be processed (e.g., the graph cannot
be created due to a lack of data).
*/
public void processProduct ( TSProduct tsproduct )
throws Exception
{	String prop_value = null;
	// Determine whether the product should be processed...
	// New version...
	prop_value = tsproduct.getLayeredPropValue ( "IsEnabled", -1, -1 );
	if ( prop_value == null ) {
		// Old version...
		prop_value = tsproduct.getLayeredPropValue ("Enabled", -1, -1 );
	}
	if ( (prop_value == null) || prop_value.equalsIgnoreCase("true") ) {
		// Determine if a graph or report product is being generated...
		prop_value =
			tsproduct.getLayeredPropValue ( "ProductType", -1, -1 );

		if ( prop_value.equalsIgnoreCase ( "Report" ) ) {
			processReportProduct ( tsproduct );
		}
		else if ( (prop_value == null) ||	// Default
			prop_value.equalsIgnoreCase("Graph") ) {
			processGraphProduct ( tsproduct );
		}
	}
	prop_value = null;
}

/**
Processes a time series product of type "Report" using its given properties.
Each subproduct in the product is processed, and will have an outfile
associated with it, in order to put time series of different interval in
separate files. The only supported ReportType is DateValue.
@param tsproduct Time series product.
*/
public void processReportProduct( TSProduct tsproduct ) throws Exception
{
	String routine = "TSProcessor.processReportProduct";
	String tsid;
	String tsalias;
	DateTime date1 = null;
	DateTime date2 = null;
	boolean is_template = false;

	// loop through each subproduct and print out the corresponding files 
	int nsubs = tsproduct.getNumSubProducts();	 
	for ( int isub = 0; isub < nsubs; isub++ ) {

		String fname = null;
		String prop_value = null;
		String report_type = null;
		Vector tslist = new Vector();

		// get file name for subproduct
		// if there isn't one set then set a temp file name
		fname = tsproduct.getLayeredPropValue ( "OutputFile", isub, -1);
		if ( fname == null ) {
		  if ( IOUtil.isUNIXMachine() ) {
			fname = "/tmp/tmp_report_" + isub;
		  }
		  else {fname = "C:\\TEMP\\tmp_report_" + isub;
		  }	
		}
				
		// Set report type for subproduct
		report_type = tsproduct.getLayeredPropValue(
			"ReportType", isub, -1);

		Message.printStatus ( 2, routine,
		"Reading time series for subproduct [" + isub + "]" );
		// New...
		prop_value =
		tsproduct.getLayeredPropValue("IsEnabled", isub, -1 );
		// Old...
		if ( prop_value == null ) {
			prop_value =
			tsproduct.getLayeredPropValue("Enabled", isub, -1 );
		}
		if (	(prop_value != null) &&
			prop_value.equalsIgnoreCase("false") ) {
			continue;
		}

		// Loop through the time series in the subproduct
		for ( int i = 0; ; i++ ) {

			TS ts = null;
			// New version...
			prop_value = tsproduct.getLayeredPropValue (
					"IsEnabled", isub, i );
			// Old version...
			if ( prop_value == null ) {
				prop_value = tsproduct.getLayeredPropValue (
					"Enabled", isub, i );
			}
			if (	(prop_value != null) &&
				prop_value.equalsIgnoreCase("false") ) {
				// Add a null time series...
				tslist.addElement ( (TS)null );
				continue;
			}
			prop_value = tsproduct.getLayeredPropValue (
					"PeriodStart", isub, i );
			if ( prop_value != null ) {
				try {	date1 = DateTime.parse ( prop_value );
				}
				catch ( Exception e ) {
					date1 = null;
				}
			}
			prop_value = tsproduct.getLayeredPropValue (
					"PeriodEnd", isub, i );
			if ( prop_value != null ) {
				try {	date2 = DateTime.parse ( prop_value );
				}
				catch ( Exception e ) {
					date2 = null;
				}
			}
			// Make sure this is last since the TSID is used in the
			// following readTimeSeries() call...
			if ( is_template ) {
				tsid = tsproduct.getLayeredPropValue (
				"TemplateTSID", isub, i, false );
			}
			else {	// Just get the normal property...
				tsid = tsproduct.getLayeredPropValue (
				"TSID", isub, i, false );
			}
			if ( tsid == null ) {
				// No more time series...
				break;
			}
			// Make sure we have both or none...
			if ( (date1 == null) || (date2 == null) ) {
				date1 = null;
				date2 = null;
			}
			// First try to read the time series using the
			// "TSAlias".  This normally will only return non-null
			// for something like TSTool where the time series may
			// be in memory.
			tsalias = tsproduct.getLayeredPropValue (
				"TSAlias", isub, i, false );
			if (	!is_template &&
				(tsalias != null) &&
				!tsalias.trim().equals("") ) {
				// Have the property so use the TSAlias instead
				// of the TSID...
				Message.printStatus ( 2, routine,
				"Reading TSAlias \"" + tsalias +
				"\" from TS suppliers." );
				try {	ts = readTimeSeries (	tsalias.trim(),
								date1, date2,
								null, true );
				}
				catch ( Exception e ) {
					// Always add a time series because
					// visual properties are going to be
					// tied to the position of the time
					// series.
					Message.printWarning ( 2, routine,
					"Error getting time series \"" +
					tsalias.trim() + "\"" );
					ts = null;
				}
			}
			else {	// Don't have a "TSAlias" so try to read the
				// time series using the full "TSID"...
				Message.printStatus ( 2, routine,
				"Reading TSID \"" + tsid +
				"\" from TS suppliers.");
				try {	ts = readTimeSeries (	tsid.trim(),
								date1, date2,
								null, true );
				}
				catch ( Exception e ) {
					// Always add a time series because
					// visual properties are going to be
					// tied to the position of the time
					// series.
					ts = null;
				}
				if ( ts == null  ) {
					Message.printWarning ( 2, routine,
					"Error getting time series \"" +
					tsid.trim() + "\".  Setting to null." );
				}
				else if ( is_template ) {
					// Non-null TS.
					// The TemplateTSID was requested but
					// now the actual TSID needs to be
					// set...
					tsproduct.setPropValue(
						"TSID",
						ts.getIdentifier().toString(),
						isub, i );
				}
			}
			
			tslist.addElement ( ts );
		}

		// done adding all time series for that subproduct
		// write output for this subproduct to a file

		if(report_type.equalsIgnoreCase("DateValue")) {

			DateValueTS.writeTimeSeriesList(tslist, fname);
 		}

		// REVISIT KAT 2006-09-11
		// May want to revisit this in the future to add
		// capability for other reports, such as Jasper Reports,
		// JFreeReports, HTML, delimited file, etc.
		// Right now only DateValue is supported.  Also might want to
		// add the ability to output time series with different
		// intervals to one file.  Right now the code complains if all
		// of a subproduct's time series don't have the same interval.
	}
}

/**
Read a time series using the time series suppliers.  The first supplier to
return a time series is assumed to be the correct supplier.
@param tsident TSIdent string indicating the time series to read.
@param date1 Starting date of read, or null.
@param date2 Ending date of read, or null.
@param req_units Requested units.
@param read_data Indicates whether to read data (false is header only).
@return a time series corresponding to the tsident.
@exception Exception if no time series can be found.
*/
public TS readTimeSeries (	String tsident, DateTime date1, DateTime date2,
				String req_units, boolean read_data )
throws Exception
{	String routine = "TSProcessor.readTimeSeries";
	int size = 0;
	if ( _suppliers != null ) {
		size = _suppliers.length;
	}
	TS ts = null;
	for ( int i = 0; i < size; i++ ) {
		Message.printStatus ( 2, routine, "Trying to get \"" + tsident +
			"\" from TSSupplier \"" +
			_suppliers[i].getTSSupplierName() + "\"" );
		try {	ts = _suppliers[i].readTimeSeries (	tsident,
								date1,
								date2,
								(String)null,
								true );
		}
		catch ( Exception e ) {
			Message.printWarning ( 2, routine,
			"Error reading time series.  Ignoring." );
			Message.printWarning ( 2, routine, e );
			continue;
		}
		if ( ts == null ) {
			Message.printStatus ( 2, routine,
			"Did not find TS \"" + tsident +
			"\" using TSSupplier \"" + 
			_suppliers[i].getTSSupplierName() + "\"" );
		}
		else {	// Found a time series so assume it is the one that is
			// needed...
			Message.printStatus ( 2, routine,
			"Found TS \"" + tsident + "\" (period " + ts.getDate1()
			+ " to " + ts.getDate2() + ")" );
			return ts;
		}
	}
	throw new Exception (
	"Unable to get time series \"" + tsident + "\" from any TSSupplier.");
}

} // End TSProcessor