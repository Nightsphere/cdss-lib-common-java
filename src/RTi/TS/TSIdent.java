// ----------------------------------------------------------------------------
// TSIdent - time series identifier class
// ----------------------------------------------------------------------------
// History:
//
// Apr 96	Steven A. Malers, RTi	Start developing the class based on
//					lots of previous work.
// 11 Nov 1996	Matthew J. Rutherford,	Changed to dynamic allocation.
//		RTi
// 21 May 1997	MJR, RTi		Added getBaseMultInterval.
// 16 Sep 1997	SAM, RTi		Did a substantial overhaul in order to
//					get the code ready for a Java port.
//					This involved adding new functions for
//					the sub/main location and source and
//					carrying around integer and string
//					versions of data, where appropriate.
// 02 Jan 1998	SAM, RTi		Wrap all the debugs with
//					Message.isDebugOn to improve
//					performance.
// 13 Apr 1999	SAM, RTi		Add finalize.
// 30 Oct 2000	SAM, RTi		Change toString() to return a nice
//					TSIDent rather than the verbose version.
// 18 Dec 2000	SAM, RTi		Overload equals() to take a string and
//					change to case-insensite compare.  Also
//					check the parts if a simple compare
//					fails, just to be sure.
// 20 Feb 2001	SAM, RTi		Change so that source is set if zero
//					length (otherwise old contents may not
//					be reset).
// 11 Apr 2001	SAM, RTi		Bump up debug to 100 for everything.
// 29 Aug 2001	SAM, RTi		Implement clone() consistent with other
//					TS classes.  Clean up Javadoc (remove
//					old C documentation).  Set unused
//					variables to null.  Remove most debug
//					information to increase performance.
// 2001-11-06	SAM, RTi		Review javadoc.  Verify that variables
//					are set to null when no longer used.
//					Deprecate notEquals().  Change set
//					methods to have void return type.
// 2002-02-06	SAM, RTi		Add _input_type and _input_name to the
//					TSIdent string.  Overload methods to
//					handle TSID strings with and without the
//					input type/name information.  Search for
//					"include_input" to find methods that
//					are overloaded.  Remove notEquals().
// 2002-05-30	SAM, RTi		Add support for a sub data type with a
//					default "-" delimiter in that field.
//					In the future may add methods to
//					change the delimiter but for now make
//					it the default.  This will better
//					support RiversideDB.
// 2003-03-13	SAM, RTi		Add matches(), similar to equals but
//					allow wildcards.
// 2003-06-02	SAM, RTi		Upgrade to use generic classes.
//					* Change TS.INTERVAL* to TimeInterval.
//					* Throw exceptions if the interval is
//					  invalid
//					* Overload matches() to indicate whether
//					  the input type and name should be
//					  compared.
// 2003-12-22	SAM, RTi		* Fix bug in equals() - was using
//					  regionMatches(true,...) rather than
//					  just equalsIgnoreCase(), possibly
//					  resulting in error due to different
//					  string lengths.
// 2004-03-04	JTS, RTi		Class is now serializable.
// 2004-03-15	SAM, RTi		* Overload matches() to allow the alias
//					  to be specifically checked (or not).
//					* Allow the regular expression for
//					  matches() to be a time series
//					  identifier with parts or a full
//					  string.
// 2004-07-11	SAM, RTi		* Update matches() to allow a pattern
//					  other than just * to be specified for
//					  a part.
// 2004-09-22	SAM, RTi		* Change the delimiter for sublocation
//					  from _ to -.  This was planned but
//					  was not previously implemented.  The
//					  delimiter for sublocation and subtype
//					  are now both "-".
// 2004-11-23	SAM, RTi		* Update to put [SeqNum] at the end of
//					  the main TSIdent string.
//					* Move the sequence number from the TS
//					  class to this class.
//					* Update private data members to use
//					  __ notation, as per RTi standard.
// 2005-11-07	JTS, RTi		Added toStringVerbose() for debugging
//					purposes.
// 2005-12-14	SAM, RTi		Initialize the interval to
//					TimeInterval.UNKNOWN instead of zero,
//					because the latter corresponds to
//					IRREGULAR.  UNKNOWN is needed to check
//					for errors in other code.
// 2007-05-08	SAM, RTi		Cleanup code based on Eclipse feedback.
// ----------------------------------------------------------------------------
// EndHeader

package RTi.TS;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

import java.io.Serializable;

import java.lang.StringBuffer;

import java.util.Vector;

import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.TimeInterval;

/**
The TSIdent class stores and manipulates a time series identifier, or
TSID string. The TSID string consists of the following parts:
<p>
<pre>
Location[-SubLoc].Source.Type[-Subtype].Interval.Scenario[Seq]~InputType~InputName
</pre>
<p>
TSID's as TSIdent objects or strings can be used to pass unique time series
identifiers and are used throughout the time series package.  Some TS object
data, including data type, are stored only in the TSIdent, to avoid
redundant data.
*/
public class TSIdent
implements Cloneable, Serializable, Transferable
{

/**
Flag indicating that no sub-location should be allowed (treat as
part of the main location).
*/
public static final int NO_SUB_LOCATION	= 0x1;	// Mask for setLocation()
						// Do not use a sub-location.
/**
Flag indicating that no sub-source should be allowed (treat as
part of the main source).
*/
public static final int NO_SUB_SOURCE = 0x2;	// Mask for setSource()
						// Do not use a sub-source.
/**
Flag indicating that no sub-type should be allowed (treat as
part of the main type).
*/
public static final int NO_SUB_TYPE = 0x4;	// Mask for setType()
						// Do not use a sub-type.

/**
Separator string for TSIdent string parts.
*/
public static final String SEPARATOR = ".";	// Separator for identifier
						// parts.
/**
Separator string for TSIdent location parts.
*/
public static final String LOCATION_SEPARATOR = "-";
						// Separator character for the
						// location information.
/**
Separator string for TSIdent source parts.
*/
public static final String SOURCE_SEPARATOR = "-";
						// Separator character for the
						// source information.
/**
Separator string for TSIdent data type parts.
*/
public static final String TYPE_SEPARATOR = "-";
						// Separator character for the
						// data type information.


private static final String __SEQUENCE_NUMBER_LEFT = "[";
						// Start of sequence number.
private static final String __SEQUENCE_NUMBER_RIGHT = "]";
						// End of sequence number.

/**
The DataFlavor for transferring this specific class.
*/
public static DataFlavor tsIdentFlavor = new DataFlavor(TSIdent.class, 
	"TSIdent");

// Data members...

private String	__identifier;		// The whole identifier, including the
					// input type.
private String	__alias;		// A short alias for the time series
					// identifier.

private String	__full_location;	// The location (combining the main
					// location and the sub-location).
private String	__main_location;	// The main location.
private String	__sub_location;		// The sub-location (2nd+ parts of the
					// location, using the
					// LOCATION_SEPARATOR.

private String	__full_source;		// The time series data source
					// (combining the main source and the
					// sub-source).
private String	__main_source;		// The main source.
private String	__sub_source;		// The sub-source.

private String	__full_type;		// The time series data type (combining
					// the main and sub types).
private String	__main_type;		// The main data type.
private String	__sub_type;		// The sub data type.

private String	__interval_string;	// The time series interval as a
					// string.
private int	__interval_base;	// The base data interval.
private int	__interval_mult;	// The data interval multiplier.

private String	__scenario;		// The time series scenario.

private int	__sequence_number;	// Number used when more than one time
					// series has the same identifier
					// (e.g., if a Vector of time series is
					// grouped as a set of traces, the
					// sequence number can be the year that
					// the trace starts).

private String	__input_type;		// Type of input (e.g., "DateValue",
					// "RiversideDB")
private String	__input_name;		// Name of input (e.g., a file name or
					// database connection name)
private int	__behavior_mask;	// Mask that controls behavior (e.g.,
					// how sub-fields are handled).

/**
Construct and initialize each part to empty strings.  Do handle sub-location
and sub-source.
*/
public TSIdent ()
{	init();
}

/**
Construct using modifiers, indicating how to handle sub-location, etc.
@param mask Behavior mask (see NO_SUB*).
*/
public TSIdent ( int mask )
{	init ();
	setBehaviorMask ( mask );
}

/**
Construct using a full string identifier, which will be parsed and the
individual parts of the identifier set.
@param identifier Full stirng identifier (optionally, with right-most fields
omitted).
@exception if the identifier is invalid (usually the interval is incorrect).
*/
public TSIdent ( String identifier )
throws Exception
{	init();
	setIdentifier ( identifier );

}

/**
Construct using a full string identifier, which will be parsed and the
individual parts of the identifier set.
@param identifier Full stirng identifier (optionally, with right-most fields
omitted).
@param mask Modifier to control behavior of TSIdent.
@exception if the identifier is invalid (usually the interval is incorrect).
*/
public TSIdent ( String identifier, int mask )
throws Exception
{	init();
	setBehaviorMask ( mask );
	setIdentifier ( identifier );
}

/**
Construct using each identifier part.
@param full_location Full location string.
@param full_source Full source string.
@param full_type Full data type.
@param interval_string Data interval string.
@param scenario Scenario string.
@exception if the identifier is invalid (usually the interval is incorrect).
*/
public TSIdent (	String full_location, String full_source,
			String full_type, String interval_string,
			String scenario )
throws Exception
{	init();
	setIdentifier ( full_location, full_source, full_type, interval_string,
			scenario, "", "" );
}

/**
Construct using each identifier part.
@param full_location Full location string.
@param full_source Full source string.
@param full_type Full data type.
@param interval_string Data interval string.
@param scenario Scenario string.
@param mask Modifier to control behavior of TSIdent.
@exception if the identifier is invalid (usually the interval is incorrect).
*/
public TSIdent (	String full_location, String full_source,
			String full_type, String interval_string,
			String scenario,
			int mask )
throws Exception
{	init();
	setBehaviorMask ( mask );
	setIdentifier ( full_location, full_source, full_type, interval_string,
			scenario, "", "" );
}

/**
Construct using each identifier part.
@param full_location Full location string.
@param full_source Full source string.
@param full_type Full data type.
@param interval_string Data interval string.
@param scenario Scenario string.
@param input_type Input type.
@param input_name Input name.
@exception if the identifier is invalid (usually the interval is incorrect).
*/
public TSIdent (	String full_location, String full_source,
			String full_type, String interval_string,
			String scenario, String input_type, String input_name )
throws Exception
{	init();
	setIdentifier ( full_location, full_source, full_type, interval_string,
			scenario, input_type, input_name );
}

/**
Copy constructor.
@param tsident TSIdent to copy.
@exception if the identifier is invalid (usually the interval is incorrect).
*/
public TSIdent ( TSIdent tsident )
throws Exception
{	// Identifier will get set from its parts
	init();
	setAlias ( tsident.getAlias() );
	setBehaviorMask ( tsident.getBehaviorMask() );
	// Do not use the following!  It triggers infinite recursion!
	//setIdentifier ( tsident.__identifier );
	setIdentifier ( tsident.getLocation(), tsident.getSource(),
			tsident.getType(), tsident.getInterval(),
			tsident.getScenario(), tsident.getSequenceNumber(),
			tsident.getInputType(), tsident.getInputName() );
	__interval_base = tsident.getIntervalBase ();
	__interval_mult = tsident.getIntervalMult ();
}

/**
Clone the object.  The Object base class clone() method is called, which clones
all the TSIdent primitive data.  The result is a complete deep copy.
*/
public Object clone ()
{	try {	TSIdent tsident = (TSIdent)super.clone();
		return tsident;
	}
	catch ( CloneNotSupportedException e ) {
		// Should not happen because everything is cloneable.
		throw new InternalError();
	}
}

/**
Compare identifiers, using a string comparison of the major parts (so that the
number of "." does not cause a problem).  This version <b>does not</b> compare
the input type and name (use the overloaded version to do so).
@return true if the identifier string equals the instance (if the string does
not match, the individual five main parts are also compared and if they match
true is returned).
@param id String identifier to compare.
*/
public boolean equals ( String id )
{	return equals ( id, false );
}

/**
Compare identifiers, using a string comparison of the major parts (so that the
number of "." does not cause a problem).
@return true if the identifier string equals the instance (if the string does
not match, the individual five main parts are also compared and if they match
true is returned).
@param id String identifier to compare.
@param include_input If true, the input type and name are included in the
comparison.  If false, only the 5-part TSID are checked.
*/
public boolean equals ( String id, boolean include_input )
{	boolean is_equal = false;
	if ( include_input ) {
		// Do a full comparison...
		if ( __identifier.equalsIgnoreCase(id) ) {
			// Simple compare...
			is_equal = true;
		}
		else {	// Compare parts to be sure...
			try {	TSIdent ident = new TSIdent ( id );
				if (	ident.getLocation().equalsIgnoreCase(
					__full_location) &&
					ident.getSource().equalsIgnoreCase(
					__full_source) &&
					ident.getType().equalsIgnoreCase(
					__full_type) &&
					ident.getInterval().equalsIgnoreCase(
					__interval_string)&&
					ident.getScenario().equalsIgnoreCase(
					__scenario) &&
					(ident.getSequenceNumber() ==
					__sequence_number) &&
					ident.getInputType().equalsIgnoreCase(
					__input_type) &&
					ident.getInputName().equalsIgnoreCase(
					__input_name)){
					is_equal = true;
				}
			}
			catch ( Exception e ) {
				// Usually because the interval is bad...
				is_equal = false;
			}
		}
	}
	else {	// Only compare the 5-part identifier...
		// Get the part of the identifier before the first ~
		int pos = id.indexOf('~');
		if ( pos >= 0 ) {
			// Has a ~ so get the leading part for the remainder
			// of the comparison...
			id = id.substring(0,pos);
		}
		if ( __identifier.equalsIgnoreCase(id) ) {
			// Simple compare...
			is_equal = true;
		}
		else {	// Compare parts to be sure...
			try {	TSIdent ident = new TSIdent ( id );
				if (	ident.getLocation().equalsIgnoreCase(
					__full_location) &&
					ident.getSource().equalsIgnoreCase(
					__full_source) &&
					ident.getType().equalsIgnoreCase(
					__full_type) &&
					ident.getInterval().equalsIgnoreCase(
					__interval_string)&&
					ident.getScenario().equalsIgnoreCase(
					__scenario) &&
					(ident.getSequenceNumber() ==
					__sequence_number) ) {
					is_equal = true;
				}
			}
			catch ( Exception e ) {
				// Usually because the interval is bad...
				is_equal = false;
			}
		}
	}
	return is_equal;
}

/**
Compare identifiers, using TSIdent.  The input type and name are not compared.
@return true if the TSIdent equals the instance.
@param id TSIdent to compare.
*/
public boolean equals ( TSIdent id )
{	return equals ( id.getIdentifier(), false );
}

/**
Compare identifiers, using TSIdent.
@return true if the TSIdent equals the instance.
@param id TSIdent to compare.
@param include_input If true, the input type and name are included in the
comparison.  If false, only the 5-part TSID is used in the comparison.
*/
public boolean equals ( TSIdent id, boolean include_input )
{	return equals ( id.getIdentifier(), include_input );
}

/**
Finalize before garbage collection.
@exception Throwable if there is an error.
*/
protected void finalize ()
throws Throwable
{	__identifier = null;
	__alias = null;
	__full_location = null;
	__main_location = null;
	__sub_location = null;
	__full_source = null;
	__main_source = null;
	__sub_source = null;
	__full_type = null;
	__main_type = null;
	__sub_type = null;
	__interval_string = null;
	__scenario = null;
	__input_name = null;
	__input_type = null;
	super.finalize();
}

/**
Return the time series alias.
@return The alias for the time series.
*/
public String getAlias ()
{	return __alias;
}

/**
Return the behavior mask.
@return The behavior mask.
*/
public int getBehaviorMask( )
{	return __behavior_mask;
}

/**
Return the full identifier String.
@return The full identifier string.
*/
public String getIdentifier()
{	return toString ( false );
}

/**
Return the full identifier String.
@param include_input  If true, the input type and name will be included in the
identifier.  If false, only the 5-part TSID will be included.
@return The full identifier string.
*/
public String getIdentifier ( boolean include_input )
{	return toString ( include_input );
}

/**
Return the full identifier given the parts.  This method may be called
internally.  Null fields are treated as empty strings.
@return The full identifier string given the parts.
@param full_location Full location string.
@param full_source Full source string.
@param full_type Full data type.
@param interval_string Data interval string.
@param scenario Scenario string.
*/
public String getIdentifierFromParts (	String full_location,String full_source,
					String full_type,
					String interval_string, String scenario)
{	return getIdentifierFromParts ( full_location, full_source, full_type,
					interval_string, scenario, -1, "", "" );
}

/**
Return the full identifier given the parts.  This method may be called
internally.  Null fields are treated as empty strings.
@return The full identifier string given the parts.
@param full_location Full location string.
@param full_source Full source string.
@param full_type Full data type.
@param interval_string Data interval string.
@param scenario Scenario string.
@param sequence_number Sequence number for the time series (in an ensemble).
*/
public String getIdentifierFromParts (	String full_location,String full_source,
					String full_type,
					String interval_string, String scenario,
					int sequence_number )
{	return getIdentifierFromParts ( full_location, full_source, full_type,
					interval_string, scenario,
					sequence_number, "", "" );
}

/**
Return the full identifier given the parts.  This method may be called
internally.  Null fields are treated as empty strings.
@return The full identifier string given the parts.
@param full_location Full location string.
@param full_source Full source string.
@param full_type Full data type.
@param interval_string Data interval string.
@param scenario Scenario string.
@param input_type Input type.
@param input_name Input name.
*/
public String getIdentifierFromParts (	String full_location,String full_source,
					String full_type,
					String interval_string, String scenario,
					String input_type, String input_name )
{	return getIdentifierFromParts ( full_location, full_source, full_type,
					interval_string, scenario,
					-1, "", "" );
}

/**
Return the full identifier given the parts.  This method may be called
internally.  Null fields are treated as empty strings.
@return The full identifier string given the parts.
@param full_location Full location string.
@param full_source Full source string.
@param full_type Full data type.
@param interval_string Data interval string.
@param scenario Scenario string.
@param sequence_number Sequence number for the time series (in an ensemble).
@param input_type Input type.  If blank, the input type will not be added.
@param input_name Input name.  If blank, the input name will not be added.
*/
public String getIdentifierFromParts (	String full_location,String full_source,
					String full_type,
					String interval_string, String scenario,
					int sequence_number,
					String input_type, String input_name )
{	StringBuffer	full_identifier = new StringBuffer();

	if ( full_location != null ) {
		full_identifier.append ( full_location );
	}
	full_identifier.append ( SEPARATOR );
	if ( full_source != null ) {
		full_identifier.append ( full_source );
	}
	full_identifier.append ( SEPARATOR );
	if ( full_type != null ) {
		full_identifier.append ( full_type );
	}
	full_identifier.append ( SEPARATOR );
	if ( interval_string != null ) {
		full_identifier.append ( interval_string );
	}
	if ( (scenario != null) && (scenario.length() != 0) ) {
		full_identifier.append ( SEPARATOR );
		full_identifier.append ( scenario );
	}
	if ( sequence_number > 0 ) {
		full_identifier.append ( __SEQUENCE_NUMBER_LEFT +
			sequence_number + __SEQUENCE_NUMBER_RIGHT );
	}
	if ( (input_type != null) && (input_type.length() != 0) ) {
		full_identifier.append ( "~" + input_type );
	}
	if ( (input_name != null) && (input_name.length() != 0) ) {
		full_identifier.append ( "~" + input_name );
	}
	return full_identifier.toString();
}

/**
Return the input name.
@return The input name.
*/
public String getInputName ()
{	return __input_name;
}

/**
Return the input type.
@return The input type.
*/
public String getInputType ()
{	return __input_type;
}

/**
Return the full interval string.
@return The full interval string.
*/
public String getInterval ()
{	return __interval_string;
}

/**
Return the data interval base as an int.
@return The data interval base (see TimeInterval.*).
*/
public int getIntervalBase ()
{	return __interval_base;
}

/**
Return the data interval multiplier.
@return The data interval multiplier.
*/
public int getIntervalMult ()
{	return __interval_mult;
}

/**
Return the full location.
@return The full location string.
*/
public String getLocation( )
{	return __full_location;
}

/**
Return the main location, which is the first part of the full location.
@return The main location string.
*/
public String getMainLocation( )
{	return __main_location;
}

/**
Return the main source string.
@return The main source string.
*/
public String getMainSource()
{	return __main_source;
}

/**
Return the main data type string.
@return The main data type string.
*/
public String getMainType()
{	return __main_type;
}

/**
Return the scenario string.
@return The scenario string.
*/
public String getScenario( )
{	return __scenario;
}

/**
Return the sequence number for the time series.
@return The sequence number for the time series.  This is meant to be used
when an array of time series traces is maintained.
@return time series sequence number.
*/
public int getSequenceNumber ()
{	return __sequence_number;
}

/**
Return the full source string.
@return The full source string.
*/
public String getSource()
{	return __full_source;
}

/**
Return the sub-location, which will be an empty string if __behavior_mask has
NO_SUB_LOCATION set.
@return The sub-location string.
*/
public String getSubLocation()
{	return __sub_location;
}

/**
Return the sub-source, which will be an empty string if __behavior_mask has
NO_SUB_SOURCE set.
@return The sub-source string.
*/
public String getSubSource( )
{	return __sub_source;
}

/**
Return the sub-data-type, which will be an empty string if __behavior_mask has
NO_SUB_TYPE set.
@return The data sub-type string.
*/
public String getSubType( )
{	return __sub_type;
}

/**
Returns the data in the specified DataFlavor, or null if no matching flavor
exists.  From the Transferable interface.
@param flavor the flavor in which to return the data.
@return the data in the specified DataFlavor, or null if no matching flavor
exists.
*/
public Object getTransferData(DataFlavor flavor) {
	if (flavor.equals(tsIdentFlavor)) {
		return this;
	}
	else {
		return null;
	}
}

/**
Returns the flavors in which data can be transferred.  From the Transferable
interface.  The order of the dataflavors that are returned are:<br>
<li>TSIdent - TSIdent.class / RTi.TS.TSIdent</li></ul>
@return the flavors in which data can be transferred.
*/
public DataFlavor[] getTransferDataFlavors() {
	DataFlavor[] flavors = new DataFlavor[1];
	flavors[0] = tsIdentFlavor;
	return flavors;
}

/**
Return the data type.
@return The full data type string.
*/
public String getType( )
{	return __full_type;
}

/**
Initialize data members.
*/
private void init ()
{	__behavior_mask = 0;	// Default is to process sub-location and
				// sub-source

	// Initialize to null strings so that we do not have problems with the
	// recursive stuff...

	__identifier = null;
	__full_location = null;
	__main_location = null;
	__sub_location = null;
	__full_source = null;
	__main_source = null;
	__sub_source = null;
	__full_type = null;
	__main_type = null;
	__sub_type = null;
	__interval_string = null;
	__scenario = null;
	__sequence_number = -1;
	__input_type = null;
	__input_name = null;

	setAlias("");

	// Initialize the overall identifier to an empty string...

	setFullIdentifier ("");

	// Initialize the location components...

	setMainLocation("");

	setSubLocation("");

	// Initialize the source...

	setMainSource("");
	setSubSource("");

	// Initialize the data type...

	setType("");
	setMainType("");
	setSubType("");

	// Initialize the interval...

	__interval_base = TimeInterval.UNKNOWN;
	__interval_mult = 0;

	try {	setInterval("");
	}
	catch ( Exception e ) {
		// Can ignore here.
	}

	// Initialize the scenario...

	setScenario("");

	// Initialize the input...

	setInputType("");
	setInputName("");
}

/**
Determines whether the specified flavor is supported as a transfer flavor.
From the Transferable interface.
The order of the dataflavors that are returned are:<br>
<li>TSIdent - TSIdent.class / RTi.TS.TSIdent</li></ul>
@param flavor the flavor to check.
@return true if data can be transferred in the specified flavor, false if not.
*/
public boolean isDataFlavorSupported(DataFlavor flavor) {
	if (flavor.equals(tsIdentFlavor)) {
		return true;
	}
	else {	return false;
	}
}

/**
Compare the string time series identifier to a regular expression, checking the
alias first if it is specified and then the identifier but not including the
input parts of the identifier.
See the overloaded version for more information.
@return true if the time series identifier matches the regular expression.
*/
public boolean matches ( String id_regexp )
{	return matches ( id_regexp, true, false );
}

/**
Compare the string time series identifier to a regular expression.  Because time
series identifiers use the "." character, the regular expression should only
contain the "*" wildcard, as needed.  If the
regular expression does not include a ".", then the full string is compared
using normal Java conventions.  if the regular expression does contain ".",
then the regular expression is split into time series identifier parts and
a string comparison of the major parts is done (so that the
number of "." does not cause a problem).  This version <b>does not</b> compare
the input type and name (use the overloaded version to do so).
See the overloaded version for more information.
@return true if the identifier string matches the instance (if the string does
not match, the individual five main parts are also compared and if they match
true is returned).  Wildcards are allowed in the identifier.
Coparisons are done case-independent by converting strings to uppercase.
@param id_regexp String identifier to compare, with identifier fields containing
regular expressions.
@param check_alias If true, check the alias first for a match.  If not matched,
the identifier is checked.  If false, the alias is not checked and only the
identifier string is checked.
*/
public boolean matches ( String id_regexp, boolean check_alias,
			boolean include_input )
{	if ( id_regexp.indexOf(".") >= 0 ) {
		// Regular expression contains parts so compare the parts...
		try {	TSIdent tsident = new TSIdent ( id_regexp );
			return matches ( tsident.getLocation(),
					tsident.getSource(),
					tsident.getType(),
					tsident.getInterval(),
					tsident.getScenario(),
					tsident.getSequenceNumber(),
					tsident.getInputType(),
					tsident.getInputName(),
					include_input );
		}
		catch ( Exception e ) {
			return false;
		}
	}
	else {	// The regular expression does not contain parts so do a
		// comparison on the whole string...
		// First replace * with .* so java string comparison works...
		String java_regexp=StringUtil.replaceString(id_regexp,"*",".*").
			toUpperCase();
		if (	check_alias && (__alias != null) &&
			(__alias.length() > 0) &&
			__alias.toUpperCase().matches(java_regexp) ) {
			return true;
		}
		if ( __identifier.toUpperCase().matches(java_regexp) ) {
			return true;
		}
		return false;
	}
}

/**
Compare identifiers, using a string comparison of all the identifier parts,
including the input type and name. 
See the overloaded version for more information.
@return true if the identifier string matches the instance (if the string does
not match, the individual five main parts are also compared and if they match
true is returned).  Wildcards are allowed in the identifier.
@param id_regexp String identifier to compare, with identifier fields containing
regular expressions.
@param include_input If true, the input type and name are included in the
comparison.  If false, only the 5-part TSID are checked.
@deprecated Use the overloaded method that takes an option for the alias.
*/
public boolean matches ( String id_regexp, boolean include_input )
{	try {	TSIdent tsident = new TSIdent ( id_regexp );
		return matches ( tsident.getLocation(), tsident.getSource(),
				tsident.getType(), tsident.getInterval(),
				tsident.getScenario(),
				tsident.getSequenceNumber(), null, null,
				include_input );
	}
	catch ( Exception e ) {
		return false;
	}
}

/**
Compare identifiers, using a string comparison of the major parts (so that the
number of "." does not cause a problem).  The instance fields will be compared
to the specified regular expressions.  The instance fields can also contain
regular expressions with "*" to indicate to match everything.  This allows some
fields to be ignored (always matched).  The regular expressions can be either
"*" (match all), a literal string (blank is a literal string) or a combination
using "*".  Each "*" is converted internally to ".*" and the String.matches()
method is called.
Comparisons are done case-independent by converting all strings to uppercase.
@return true if the identifier string equals the instance (if the string does
not match, the individual five main parts are also compared and if they match
true is returned).
@param location_regexp Location regular expression to compare.
@param source_regexp Data source regular expression to compare.
@param data_type_regexp Data type regular expression to compare.
@param interval_regexp Data interval regular expression to compare.
@param scenario_regexp Scenario regular expression to compare.
@param input_type_regexp Input type regular expression to compare.
@param input_name_regexp Input name regular expression to compare.
@param include_input If true, the input type and name are included in the
comparison.  If false, only the 5-part TSID are checked.
*/
public boolean matches (	String location_regexp, String source_regexp,
				String data_type_regexp, String interval_regexp,
				String scenario_regexp,
				String input_type_regexp,
				String input_name_regexp,
				boolean include_input )
{	return matches (	location_regexp, source_regexp,
				data_type_regexp, interval_regexp,
				scenario_regexp, -1,
				input_type_regexp,
				input_name_regexp,
				include_input );
}

/**
Compare identifiers, using a string comparison of the major parts (so that the
number of "." does not cause a problem).  The instance fields will be compared
to the specified regular expressions.  The instance fields can also contain
regular expressions with "*" to indicate to match everything.  This allows some
fields to be ignored (always matched).  The regular expressions can be either
"*" (match all), a literal string (blank is a literal string) or a combination
using "*".  Each "*" is converted internally to ".*" and the String.matches()
method is called.
Comparisons are done case-independent by converting all strings to uppercase.
If include_input=true, the input type and name will be checked, even if they
are blank.  Therefore, it may be important in some cases that calling code
check to see whether the input information is part of the time series identifier
and pass true only if such information is actually available in both the
instance and the values being checked.
@return true if the identifier string equals the instance (if the string does
not match, the individual five main parts are also compared and if they match
true is returned).
@param location_regexp Location regular expression to compare.
@param source_regexp Data source regular expression to compare.
@param data_type_regexp Data type regular expression to compare.
@param interval_regexp Data interval regular expression to compare.
@param scenario_regexp Scenario regular expression to compare.
@param sequence_number Sequence number for time series in an ensemble.
@param input_type_regexp Input type regular expression to compare.
@param input_name_regexp Input name regular expression to compare.
@param include_input If true, the input type and name are included in the
comparison.  If false, only the 5-part TSID are checked.
*/
public boolean matches (	String location_regexp, String source_regexp,
				String data_type_regexp, String interval_regexp,
				String scenario_regexp,
				int sequence_number,
				String input_type_regexp,
				String input_name_regexp,
				boolean include_input )
{	// Do the comparison by comparing each part.  Check for mismatches and
	// if any occur return false.  Then if at the end, the TSIdent must
	// match.
	String routine = "TSIdent.matches";
	if ( Message.isDebugOn ) {
		Message.printDebug ( 5, routine,
		"Checking match of \"" + toString(true) + " with loc=\"" +
		location_regexp + "\" source=\"" + source_regexp +
		"\" type=\"" + data_type_regexp + "\" interval=\"" +
		interval_regexp + "\" scenario=\"" + scenario_regexp +
		"\" sequence_number=" + sequence_number +
		" input_type=\"" +
		input_type_regexp + "\" inputname=\"" + input_name_regexp +
		"\" include_input=" +include_input );
	}
	// Replace "*" in the regular expressions with ".*", which is necessary
	// to utilize the Java matches() method...
	String location_regexp_Java = StringUtil.replaceString(
		location_regexp,"*",".*").toUpperCase();
	String source_regexp_Java = StringUtil.replaceString(
		source_regexp,"*",".*").toUpperCase();
	String data_type_regexp_Java = StringUtil.replaceString(
		data_type_regexp,"*",".*").toUpperCase();
	String interval_regexp_Java = StringUtil.replaceString(
		interval_regexp,"*",".*").toUpperCase();
	String scenario_regexp_Java = StringUtil.replaceString(
		scenario_regexp,"*",".*").toUpperCase();
	// Compare the 5-part identifier first...
	if ( !__full_location.toUpperCase().matches(location_regexp_Java) ) {
		return false;
	}
	if ( !__full_source.toUpperCase().matches(source_regexp_Java) ) {
		return false;
	}
	if ( !__full_type.toUpperCase().matches(data_type_regexp_Java) ) {
		return false;
	}
	if ( !__interval_string.toUpperCase().matches(interval_regexp_Java) ){
		return false;
	}
	if ( __sequence_number != sequence_number ) {
		return false;
	}
	if ( !__scenario.toUpperCase().matches(scenario_regexp_Java) ) {
		return false;
	}
	if ( include_input ) {
		String input_type_regexp_Java = StringUtil.replaceString(
			input_type_regexp,"*",".*").toUpperCase();
		String input_name_regexp_Java = StringUtil.replaceString(
			input_name_regexp,"*",".*").toUpperCase();
		// Sometimes input_type_regexp is blank.  In this case do not
		// use to compare
		// TODO SAM 2007-06-22 Not sure why regexp would not be OK here and below?
		if ( (input_type_regexp.length() > 0) &&
				!__input_type.toUpperCase().matches(input_type_regexp_Java) ) {
			return false;
		}
		// Typical that input_name_regexp is blank.  In this case do not
		// use to compare.
		if ( (input_name_regexp.length() > 0) &&
			!__input_name.toUpperCase().matches(input_name_regexp_Java) ) {
				return false;
			}
	}
	if ( Message.isDebugOn ) {
		Message.printDebug ( 5, routine, "TSIdent matches" );
	}
	return true;
}

/**
Parse a TSIdent instance given a String representation of the identifier.
The behavior flag is assumed to be zero.
@return A TSIdent instance given a full identifier string.
@param identifier Full identifier as string.
@exception if an error occurs (usually a bad interval).
*/
public static TSIdent parseIdentifier ( String identifier )
throws Exception
{	return parseIdentifier ( identifier, 0 );
}

/**
Parse a TSIdent instance given a String representation of the identifier.
@return A TSIdent instance given a full identifier string.
@param identifier Full identifier as string.
@param behavior_flag Behavior mask to use when creating instance.
@exception if an error occurs (usually a bad interval).
*/
public static TSIdent parseIdentifier (	String identifier,
					int behavior_flag )
throws Exception
{	String	routine="TSIdent.parseIdentifier";
	int	dl = 100;
	
	// Declare a TSIdent which we will fill and return...

	if ( Message.isDebugOn ) {
		Message.printDebug ( dl, routine,
		"Declare TSIdent within this routine..." );
	}
	TSIdent	tsident = new TSIdent ( behavior_flag );
	if ( Message.isDebugOn ) {
		Message.printDebug ( dl, routine, "...done declaring TSIdent" );
	}

	// First parse the input information...

	String identifier0 = identifier;
	Vector list = StringUtil.breakStringList ( identifier, "~", 0 );
	int	i, nlist1;
	if ( list != null ) {
		nlist1 = list.size();
		// Reset to first part for checks below...
		identifier = (String)list.elementAt(0);
		if ( nlist1 == 2 ) {
			tsident.setInputType ( (String)list.elementAt(1) );
		}
		else if ( nlist1 >= 3 ) {
			tsident.setInputType ( (String)list.elementAt(1) );
			// File name may have a ~ so find the second instance
			// of ~ and use the remaining string...
			int pos = identifier0.indexOf ( "~" );
			if ( (pos >= 0) && identifier0.length() > (pos + 1) ) {
				// Have something at the end...
				String sub = identifier0.substring ( pos + 1 );
				pos = sub.indexOf ( "~" );
				if ( (pos >= 0) && (sub.length() > (pos + 1))) {
					// The rest is the file...
					tsident.setInputName (
					sub.substring(pos + 1) );
				}
			}
		}
	}

	// Now parse the 5-part identifier...

	String	full_location = "", full_source = "", interval_string = "",
		scenario = "", full_type = "";

	// Figure out whether we are using the new or old conventions.  First
	// check to see if the number of fields is small.  Then check to see if
	// the data type and interval are combined.

	list = StringUtil.breakStringList ( identifier, ".", 0 );
	nlist1 = list.size();
	for ( i = 0; i < nlist1; i++ ) {
		if ( Message.isDebugOn ) {
			Message.printDebug ( dl, routine,
			"TS ID list[" + i + "]:  \"" + list.elementAt(i) +
			"\"" );
		}
	}

	if ( Message.isDebugOn ) {
		Message.printDebug ( dl, routine, "Full TS ID:  \"" +
		identifier +"\"");
	}

	// Parse out location and split the rest of the ID...
	//
	// This field is allowed to be surrounded by quotes since some
	// locations cannot be identified by a simple string.  Allow
	// either ' or " to be used and bracket it.

	if ( (identifier.charAt(0) == '\'') || (identifier.charAt(0) == '\"')) {
		full_location = StringUtil.readToDelim (
				identifier.substring(1), identifier.charAt(0) );
		// Get the 2nd+ fields...
		list =	StringUtil.breakStringList (
			identifier.substring(full_location.length()+1),
			".", 0 );
		nlist1 = list.size();
	}
	else {	list =	StringUtil.breakStringList ( identifier, ".", 0 );
		nlist1 = list.size();
		if ( nlist1 >= 1 ) {
			full_location = (String)list.elementAt(0);
		}
	}
	// Data source...
	if ( nlist1 >= 2 ) {
		full_source = (String)list.elementAt(1);
	}
	// Data type...
	if ( nlist1 >= 3 ) {
		full_type = (String)list.elementAt(2);
	}
	// Data interval...
	int sequence_number = -1;
	if ( nlist1 >= 4 ) {
		interval_string = (String)list.elementAt(3);
		// If no scenario is used, the interval string may have the
		// sequence number on the end, so search for the [ and split the
		// sequence number out of the interval string...
		int index = interval_string.indexOf ( __SEQUENCE_NUMBER_LEFT );
		// Get the sequence number...
		if ( index >= 0 ) {
			if ( interval_string.endsWith(__SEQUENCE_NUMBER_RIGHT)){
				// Should be a properly-formed sequence
				// number, but need to remove the brackets...
				String sequence_number_string =
					interval_string.substring(
					index + 1, interval_string.length() - 1)
					.trim();
				if (	StringUtil.isInteger(
					sequence_number_string) ) {
					sequence_number = StringUtil.atoi(
						sequence_number_string );
				}
			}
			if ( index == 0 ) {
				// There is no interval, just the sequence
				// number (should not happen)...
				interval_string = "";
			}
			else {	interval_string =
					interval_string.substring(0,index);
			}
		}
	}
	// Scenario...  It is possible that the scenario has delimeters
	// in it.  Therefore, we need to concatenate all the remaining
	// fields to compose the complete scenario...
	if ( nlist1 >= 5 ) {
		StringBuffer buffer = new StringBuffer();
		buffer.append ( list.elementAt(4) );
		for ( i = 5; i < nlist1; i++ ) {
			buffer.append ( "." );
			buffer.append ( list.elementAt(i) );
		}
		scenario = buffer.toString ();
		buffer = null;
	}
	// The scenario may now have the sequence number on the end, search for
	// the [ and split out of the scenario...
	int index = scenario.indexOf ( __SEQUENCE_NUMBER_LEFT );
	// Get the sequence number...
	if ( index >= 0 ) {
		if ( scenario.endsWith(__SEQUENCE_NUMBER_RIGHT) ) {
			// Should be a properly-formed sequence number...
			String sequence_number_string =
				scenario.substring ( index + 1,
				scenario.length() - 1 ).trim();
			if ( StringUtil.isInteger(sequence_number_string) ) {
				sequence_number = StringUtil.atoi(
					sequence_number_string );
			}
		}
		if ( index == 0 ) {
			// There is no scenario, just the sequence number...
			scenario = "";
		}
		else {	scenario = scenario.substring(0,index);
		}
	}
	if ( Message.isDebugOn ) {
		Message.printDebug ( dl, routine,
		"After split: fullloc=\"" + full_location + "\" fullsrc=\"" +
		full_source + "\" type=\"" + full_type + "\" int=\"" +
		interval_string + "\" scen=\"" + scenario + "\"" );
	}

	// Now set the identifier component parts...

	tsident.setLocation ( full_location );
	tsident.setSource ( full_source );
	tsident.setType ( full_type );
	tsident.setInterval ( interval_string );
	tsident.setScenario ( scenario );
	if ( sequence_number >= 0 ) {
		tsident.setSequenceNumber ( sequence_number );
	}

	// Return the TSIdent object for use elsewhere...

	if ( Message.isDebugOn ) {
		Message.printDebug ( dl, routine,
		"Returning local TSIdent..." );
	}
	full_location = null;
	full_source = null;
	full_type = null;
	interval_string = null;
	scenario = null;
	list = null;
	routine = null;
	return tsident;
}

/**
Set the time series alias.
@param alias Alias for the time series.
*/
public void setAlias ( String alias )
{	if ( alias != null ) {
		__alias = alias;
	}
}

/**
Set the behavior mask.  The behavior mask controls how identifier sub-parts are
joined into the full identifier.   Currently this routine does a full reset (not
bit-wise).
@param behavior_mask Behavior mask that controls how sub-fields are handled.
*/
public void setBehaviorMask( int behavior_mask )
{	__behavior_mask = behavior_mask;
}

/**
Set the full identifier (this does not result in a parse).  It is normally only
called from within this class.
@param full_identifier Full identifier string.
*/
private void setFullIdentifier ( String full_identifier )
{	if ( full_identifier == null ) {
		return;
	}
	__identifier = full_identifier;
	// DO NOT call setIdentifier() from here!
}

/**
Set the full location (this does not result in a parse).  It is normally only
called from within this class.
@param full_location Full location string.
*/
private void setFullLocation ( String full_location )
{	if ( full_location == null ) {
		return;
	}
	__full_location = full_location;
	// DO NOT call setIdentifier() from here!
}

/**
Set the full source (this does not result in a parse).  It is normally only
called from within this class.
@param full_source Full source string.
*/
private void setFullSource ( String full_source )
{	if ( full_source == null ) {
		return;
	}
	__full_source = full_source;
	// DO NOT call setIdentifier() from here!
}

/**
Set the full data type (this does not result in a parse).  It is normally only
called from within this class.
@param full_type Full data type string.
*/
private void setFullType ( String full_type )
{	if ( full_type == null ) {
		return;
	}
	__full_type = full_type;
	// DO NOT call setIdentifier() from here!
}

/**
Set the full identifier from its parts.
*/
public void setIdentifier ( )
{	String	routine = "TSIdent.setIdentifier(void)";
	int	dl = 100;

	// Assume that all the individual set routines have handled the
	// __behavior_mask accordingly and therefore we can just concatenate
	// strings here...

	if ( Message.isDebugOn ) {
		Message.printDebug ( dl, routine,
		"Setting full identifier from parts: \"" + __full_location +
		"." + __full_source + "." + __full_type +"."+__interval_string +
		"." + __scenario + "~" + __input_type + "~" + __input_name );
	}

	String	full_identifier;
	if ( Message.isDebugOn ) {
		Message.printDebug ( dl, routine,
		"Calling getIdentifierFromParts..." );
	}
	full_identifier = getIdentifierFromParts(__full_location, __full_source,
			__full_type, __interval_string, __scenario,
			__sequence_number, __input_type, __input_name );
	if ( Message.isDebugOn ) {
		Message.printDebug ( dl, routine,
		"...successfully called getIdentifierFromParts..." );
	}

	setFullIdentifier ( full_identifier );

	if ( Message.isDebugOn ) {
		Message.printDebug ( dl, routine, "ID..." );
		Message.printDebug ( dl, routine, "\"" + __identifier + "\"" );
	}
	full_identifier = null;
	routine = null;
}

/**
Set the identifier by parsing the given string.
@param identifier Full identifier string.
@exception if the identifier cannot be set (usually the interval is incorrect).
*/
public void setIdentifier ( String identifier )
throws Exception
{	String	routine = "TSIdent.setIdentifier";
	int	dl = 100;

	if ( identifier == null ) {
		return;
	}

	if ( Message.isDebugOn ) {
		Message.printDebug ( dl, routine,
		"Trying to set identifier to \"" + identifier + "\"" );
	}

	if ( identifier.length() == 0 ) {
		// Cannot parse the identifier because doing so would result in
		// an infinite loop.  If this routine is being called with an
		// empty string, it is a mistake.  The initialization code will
		// call setFullIdentifier() directly.
		if ( Message.isDebugOn ) {
			Message.printDebug ( dl, routine,
			"Identifier string is empty, not processing!" );
		}
		return;
	}

	// Parse the identifier using the public static function to create a
	// temporary identifier object...

	if ( Message.isDebugOn ) {
		Message.printDebug ( dl, routine,
		"Done declaring temp TSIdent." );
		Message.printDebug ( dl, routine,
		"Parsing identifier..." );
	}
	TSIdent tsident = parseIdentifier ( identifier, __behavior_mask );
	if ( Message.isDebugOn ) {
		Message.printDebug ( dl, routine,
		"...back from parsing identifier" );
	}

	// Now copy the temporary copy into this instance...

	if ( Message.isDebugOn ) {
		Message.printDebug ( dl, routine,
		"Setting the individual parts..." );
	}
	setLocation( tsident.getLocation() );
	setSource( tsident.getSource() );
	setType( tsident.getType() );
	setInterval( tsident.getInterval() );
	setScenario( tsident.getScenario() );
	setSequenceNumber ( tsident.getSequenceNumber() );
	setInputType ( tsident.getInputType() );
	setInputName ( tsident.getInputName() );
	if ( Message.isDebugOn ) {
		Message.printDebug ( dl, routine,
		"... done setting the individual parts" );
	}

	tsident = null;
	routine = null;
}

/**
Set the identifier given the parts.
@param full_location Full location string.
@param full_source Full source string.
@param full_type Full data type.
@param interval_string Data interval string.
@param scenario Scenario string.
@exception if the identifier cannot be set (usually the interval is incorrect).
*/
public void setIdentifier (	String full_location, String full_source,
				String full_type, String interval_string,
				String scenario )
throws Exception
{	setLocation ( full_location );
	setSource ( full_source );
	setType ( full_type );
	setInterval ( interval_string );
	setScenario ( scenario );
}

/**
Set the identifier given the parts.
@param full_location Full location string.
@param full_source Full source string.
@param type Data type.
@param interval_string Data interval string.
@param scenario Scenario string.
@param input_type Input type.
@param input_name Input name.
@exception if the identifier cannot be set (usually the interval is incorrect).
*/
public void setIdentifier (	String full_location, String full_source,
				String type, String interval_string,
				String scenario, String input_type,
				String input_name )
throws Exception
{	setLocation ( full_location );
	setSource ( full_source );
	setType ( type );
	setInterval ( interval_string );
	setScenario ( scenario );
	setInputType ( input_type );
	setInputName ( input_name );
}

/**
Set the identifier given the parts.
@param full_location Full location string.
@param full_source Full source string.
@param type Data type.
@param interval_string Data interval string.
@param scenario Scenario string.
@param sequence_number Sequence number (for time series in ensemble).
@param input_type Input type.
@param input_name Input name.
@exception if the identifier cannot be set (usually the interval is incorrect).
*/
public void setIdentifier (	String full_location, String full_source,
				String type, String interval_string,
				String scenario, int sequence_number,
				String input_type,
				String input_name )
throws Exception
{	setLocation ( full_location );
	setSource ( full_source );
	setType ( type );
	setInterval ( interval_string );
	setScenario ( scenario );
	setSequenceNumber ( sequence_number );
	setInputType ( input_type );
	setInputName ( input_name );
}

/**
Set the input name.
The input name.
*/
public void setInputName ( String input_name )
{	if ( input_name != null ) {
		__input_name = input_name;
	}
}

/**
Set the input type.
The input type.
*/
public void setInputType ( String input_type )
{	if ( input_type != null ) {
		__input_type = input_type;
	}
}

/**
Set the interval given the interval string.
@param interval_string Data interval string.
@exception if there is an error parsing the interval string.
*/
public void setInterval ( String interval_string )
throws Exception
{	String		routine="TSIdent.setInterval(String)";
	int		dl = 100;
	TimeInterval	tsinterval;

	if ( interval_string == null ) {
		return;
	}

	if ( Message.isDebugOn ) {
		Message.printDebug ( dl, routine, "Setting interval to \"" +
		interval_string + "\"" );
	}

	if (	!interval_string.equals("*") &&
		interval_string.length() > 0 ) {
		// First split the string into its base and multiplier...

		tsinterval = TimeInterval.parseInterval ( interval_string );

		// Now set the base and multiplier...

		__interval_base = tsinterval.getBase();
		__interval_mult = tsinterval.getMultiplier();
		if ( Message.isDebugOn ) {
			Message.printDebug ( dl, routine,
			"Setting interval base to " + __interval_base
			+ " mult: " + __interval_mult );
		}
	}
	// Else, don't do anything (leave as zero initialized values).

	// Now set the interval string.  Use the given interval base string
	// because we need to preserve existing file names, etc.

	setIntervalString ( interval_string );
	setIdentifier();
	routine = null;
	tsinterval = null;
}

/**
Set the interval given the interval integer values.
@param interval_base Base interval (see TimeInterval.*).
@param interval_mult Base interval multiplier.
*/
public void setInterval ( int interval_base, int interval_mult )
{	if ( interval_mult <= 0 ) {
		Message.printWarning ( 2, "TSIdent.setInterval",
		"Interval multiplier (" + interval_mult +
		" must be greater than zero" );
	}
	if (	(interval_base != TimeInterval.SECOND) &&
		(interval_base != TimeInterval.MINUTE) &&
		(interval_base != TimeInterval.HOUR) &&
		(interval_base != TimeInterval.DAY) &&
		(interval_base != TimeInterval.WEEK) &&
		(interval_base != TimeInterval.MONTH) &&
		(interval_base != TimeInterval.YEAR) &&
		(interval_base != TimeInterval.IRREGULAR) ) {
		Message.printWarning ( 2, "TSIdent.setInterval",
		"Base interval (" + interval_base + ") is not recognized" );
		return;
	}
	__interval_base = interval_base;
	__interval_mult = interval_mult;

	// Now we need to set the string representation of the interval...

	StringBuffer interval_string = new StringBuffer ();
	if (	(interval_base != TimeInterval.IRREGULAR) &&
		(interval_mult != 1) ) {
		interval_string.append ( interval_mult );
	}

	if ( interval_base == TimeInterval.SECOND ) {
		interval_string.append ( "sec" );
	}
	else if	( interval_base == TimeInterval.MINUTE ) {
		interval_string.append ( "min" );
	}
	else if	( interval_base == TimeInterval.HOUR ) {
		interval_string.append ( "hour" );
	}
	else if	( interval_base == TimeInterval.DAY ) {
		interval_string.append ( "day" );
	}
	else if	( interval_base == TimeInterval.WEEK ) {
		interval_string.append ( "week" );
	}
	else if	( interval_base == TimeInterval.MONTH ) {
		interval_string.append ( "month" );
	}
	else if	( interval_base == TimeInterval.YEAR ) {
		interval_string.append ( "year" );
	}
	else if	( interval_base == TimeInterval.IRREGULAR ) {
		interval_string.append ( "irreg" );
	}

	setIntervalString ( interval_string.toString() );
	interval_string = null;
	setIdentifier ();
}

/**
Set the interval string.  This is normally only called from this class.
@param interval_string Interval string.
*/
private void setIntervalString ( String interval_string )
{	if ( interval_string != null ) {
		__interval_string = interval_string;
	}
}

/**
Set the full location from its parts.  This method is generally called from
setMainLocation() and setSubLocation() methods to reset __full_location.
*/
public void setLocation ()
{	String	routine = "TSIdent.setLocation";
	int	dl = 100;
	
	if ( Message.isDebugOn ) {
		Message.printDebug ( dl, routine,
		"Resetting full location from parts..." );
	}
	if ( (__behavior_mask & NO_SUB_LOCATION) != 0 ) {
		// Just use the main location as the full location...
		if ( __main_location != null ) {
			// There should always be a main location after the
			// object is initialized...
			setFullLocation ( __main_location );
		}
	}
	else {	// Concatenate the main and sub-locations to get the full
		// location.
		StringBuffer	full_location = new StringBuffer ();
		// We may want to check for __main_location[] also...
		if ( __main_location != null ) {
			// This should always be the case after the object is
			// initialized...
			full_location.append ( __main_location );
			if ( __sub_location != null ) {
				// We only want to add the sublocation if it is
				// not an empty string (it will be an empty
				// string after the object is initialized).
				if ( __sub_location.length() > 0 ) {
					// We have a sub_location so append it
					// to the main location...
					full_location.append (
					LOCATION_SEPARATOR );
					full_location.append ( __sub_location );
				}
			}
			setFullLocation ( full_location.toString() );
		}
		full_location = null;
	}
	// Now reset the full identifier...
	setIdentifier ();
	routine = null;
}

/**
Set the full location from its parts.
@param main_location The main location string.
@param sub_location The sub-location string.
*/
public void setLocation ( String main_location, String sub_location )
{	setMainLocation ( main_location );
	setSubLocation ( sub_location );
	// The full location will be set when the parts are set.
}

/**
Set the full location from its full string.
@param full_location The full location string.
*/
public void setLocation( String full_location )
{	String	routine = "TSIdent.setLocation(String)";
	int	dl = 100;

	if ( full_location == null ) {
		return;
	}

	if ( Message.isDebugOn ) {
		Message.printDebug ( dl, routine,
		"Trying to set location to \"" + full_location + "\"" );
	}

	if ( (__behavior_mask & NO_SUB_LOCATION) != 0 ) {
		// The entire string passed in is used for the main location...
		setMainLocation ( full_location );
	}
	else {	// Need to split the location into main and sub-location...
		Vector		list;
		StringBuffer	sub_location = new StringBuffer ();
		int		nlist;
		list =	StringUtil.breakStringList ( full_location,
			LOCATION_SEPARATOR, 0 );
		nlist = list.size();
		if ( nlist >= 1 ) {
			// Set the main location...
			setMainLocation ( (String)list.elementAt(0) );
		}
		if ( nlist >= 2 ) {
			// Now set the sub-location.  This allows for multiple
			// delimited parts (everything after the first
			// delimiter is treated as the sublocation).
			int iend = nlist - 1;
			for ( int i = 1; i <= iend; i++ ) {
				if ( i != 1 ) {
					sub_location.append (
					LOCATION_SEPARATOR );
				}
				sub_location.append ((String)list.elementAt(i));
			}
			setSubLocation ( sub_location.toString() );
		}
		else {	// Since we are only setting the main location we
			// need to set the sub-location to an empty string...
			setSubLocation ( "" );
		}
		list = null;
		sub_location = null;
	}
	routine = null;
}

/**
Set the main location string (and reset the full location).
@param main_location The main location string.
*/
public void setMainLocation ( String main_location )
{	if ( main_location == null ) {
		return;
	}
	__main_location = main_location;
	setLocation();
}

/**
Set the main source string (and reset the full source).
@param main_source The main source string.
*/
public void setMainSource ( String main_source )
{	if ( main_source == null ) {
		return;
	}
	__main_source = main_source;
	setSource();
}
 
/**
Set the main data type string (and reset the full type).
@param main_type The main data type string.
*/
public void setMainType ( String main_type )
{	if ( main_type == null ) {
		return;
	}
	__main_type = main_type;
	setType();
}

/**
Set the scenario string.
@param scenario The scenario string.
*/
public void setScenario( String scenario )
{	if ( scenario == null ) {
		return;
	}
	__scenario = scenario;
	setIdentifier ();
}

/**
Set the sequence number.  This can be used to indicate a trace number.
@param sequence_number Sequence number for the time series.
*/
public void setSequenceNumber ( int sequence_number )
{	__sequence_number = sequence_number;
	setIdentifier ();
}

/**
Set the full source from its parts.
*/
public void setSource ( )
{	if ( (__behavior_mask & NO_SUB_SOURCE) != 0 ) {
		// Just use the main source as the full source...
		if ( __main_source != null ) {
			// There should always be a main source after the
			// object is initialized...
			setFullSource ( __main_source );
		}
	}
	else {	// Concatenate the main and sub-sources to get the full
		// source.
		StringBuffer full_source = new StringBuffer ();
		if ( __main_source != null ) {
			// We only want to add the subsource if it is not an
			// empty string (it will be an empty string after the
			// object is initialized).
			full_source.append ( __main_source );
			if ( __sub_source != null ) {
				// We have sub_source so append it to the main
				// source...
				// We have a sub_source so append it to the
				// main source...
				if ( __sub_source.length() > 0 ) {
					full_source.append ( SOURCE_SEPARATOR );
					full_source.append ( __sub_source );
				}
			}
			setFullSource ( full_source.toString() );
		}
		full_source = null;
	}
	// Now reset the full identifier...
	setIdentifier ();
}

/**
Set the full source from its parts.
@param main_source The main source string.
@param sub_source The sub-source string.
*/
public void setSource ( String main_source, String sub_source )
{	setMainSource ( main_source );
	setSubSource ( sub_source );
	// The full source will be set when the parts are set.
}

/**
Set the full source from a full string.
@param source The full source string.
*/
public void setSource( String source )
{	if ( source == null ) {
		return;
	}

	if ( source.equals("") ) {
		setMainSource ( "" );
		setSubSource ( "" );
	}
	else if ( (__behavior_mask & NO_SUB_SOURCE) != 0 ) {
		// The entire string passed in is used for the main source...
		setMainSource ( source );
	}
	else {	// Need to split the source into main and sub-source...
		Vector		list;
		StringBuffer	sub_source = new StringBuffer ();
		int		nlist;
		list =	StringUtil.breakStringList ( source,
			SOURCE_SEPARATOR, 0 );
		nlist = list.size();
		if ( nlist >= 1 )  {
			// Set the main source...
			setMainSource ( (String)list.elementAt(0) );
		}
		if ( nlist >= 2 ) {
			// Now set the sub-source...
			int iend = nlist - 1;
			for ( int i = 1; i <= iend; i++ ) {
				sub_source.append ((String)list.elementAt(i) );
				if ( i != iend ) {
					sub_source.append ( SOURCE_SEPARATOR );
				}
			}
			setSubSource ( sub_source.toString() );
		}
		else {	// Since we are only setting the main location we
			// need to set the sub-location to an empty string...
			setSubSource ( "" );
		}
		list = null;
		sub_source = null;
	}
}

/**
Set the sub-location string (and reset the full location).
@param sub_location The sub-location string.
*/
public void setSubLocation ( String sub_location )
{	if ( sub_location == null ) {
		return;
	}
	__sub_location = sub_location;
	setLocation();
}

/**
Set the sub-source string (and reset the full source).
@param sub_source The sub-source string.
*/
public void setSubSource ( String sub_source )
{	if ( sub_source == null ) {
		return;
	}
	__sub_source = sub_source;
	setSource();
}

/**
Set the sub-type string (and reset the full data type).
@param sub_type The sub-type string.
*/
public void setSubType ( String sub_type )
{	if ( sub_type == null ) {
		return;
	}
	__sub_type = sub_type;
	setType();
}

/**
Set the full data type from its parts.  This method is generally called from
setMainType() and setSubType() methods to reset __full_type.
*/
public void setType ()
{	String	routine = "TSIdent.setType";
	int	dl = 100;
	
	if ( Message.isDebugOn ) {
		Message.printDebug ( dl, routine,
		"Resetting full type from parts..." );
	}
	if ( (__behavior_mask & NO_SUB_TYPE) != 0 ) {
		// Just use the main type as the full type...
		if ( __main_type != null ) {
			// There should always be a main type after the
			// object is initialized...
			setFullType ( __main_type );
		}
	}
	else {	// Concatenate the main and sub-types to get the full type.
		StringBuffer full_type = new StringBuffer ();
		if ( __main_type != null ) {
			// This should always be the case after the object is
			// initialized...
			full_type.append ( __main_type );
			if ( __sub_type != null ) {
				// We only want to add the subtype if it is
				// not an empty string (it will be an empty
				// string after the object is initialized).
				if ( __sub_type.length() > 0 ) {
					// We have a sub type so append it
					// to the main type...
					full_type.append ( TYPE_SEPARATOR );
					full_type.append ( __sub_type );
				}
			}
			setFullType ( full_type.toString() );
		}
		full_type = null;
	}
	// Now reset the full identifier...
	setIdentifier ();
	routine = null;
}

/**
Set the full data type from its full string.
@param type The full data type string.
*/
public void setType ( String type )
{	String	routine = "TSIdent.setType";
	int	dl = 100;

	if ( type == null ) {
		return;
	}

	if ( Message.isDebugOn ) {
		Message.printDebug ( dl, routine,
		"Trying to set data type to \"" + type + "\"" );
	}

	if ( (__behavior_mask & NO_SUB_TYPE) != 0 ) {
		// The entire string passed in is used for the main data type...
		setMainType ( type );
	}
	else {	// Need to split the data type into main and sub-location...
		Vector		list;
		StringBuffer	sub_type = new StringBuffer ();
		int		nlist;
		list =	StringUtil.breakStringList ( type,
			TYPE_SEPARATOR, 0 );
		nlist = list.size();
		if ( nlist >= 1 ) {
			// Set the main type...
			setMainType ( (String)list.elementAt(0) );
		}
		if ( nlist >= 2 ) {
			// Now set the sub-type...
			int iend = nlist - 1;
			for ( int i = 1; i <= iend; i++ ) {
				sub_type.append ((String)list.elementAt(i));
				if ( i != iend ) {
					sub_type.append (
					TYPE_SEPARATOR );
				}
			}
			setSubType ( sub_type.toString() );
		}
		else {	// Since we are only setting the main type we
			// need to set the sub-type to an empty string...
			setSubType ( "" );
		}
		list = null;
		sub_type = null;
	}
	routine = null;
}

/**
Return a string representation of the TSIdent.  This <b>does not</b> include the
input type and name (use the overloaded version to do so).
@return A string representation of the TSIdent.
*/
public String toString ()
{	return toString ( false );
}

/**
Return a string representation of the TSIdent.
@return A string representation of the TSIdent.
@param include_input If true, the input type and name are included in the
identifier.  If false, the 5-part TSID is returned.
*/
public String toString ( boolean include_input )
{	String scenario = "";
	String sequence_number = "";
	String input_type = "";
	String input_name = "";
	if ( (__scenario != null) && (__scenario.length() > 0) ) {
		// Add the scenario if it is not blank...
		scenario = "." + __scenario;
	}
	if ( __sequence_number != -1 ) {
		// Add the sequence number if it is not the initial value...
		sequence_number = __SEQUENCE_NUMBER_LEFT +
			__sequence_number + __SEQUENCE_NUMBER_RIGHT;
	}
	if ( include_input ) {
		if ( (__input_type != null) && (__input_type.length() > 0) ) {
			input_type = "~" + __input_type;
		}
		if ( (__input_name != null) && (__input_name.length() > 0) ) {
			input_name = "~" + __input_name;
		}
	}
	return ( __full_location + "." + __full_source + "." +
		__full_type + "." + __interval_string + scenario +
		sequence_number + input_type + input_name );
}

/**
Returns a String representation of the TSIdent, with each individual piece of
the TSIdent explicity printed on a single line and labelled as to the part of
the TSIdent it is.
@return a String representation of the TSIdent, with each individual piece of
the TSIdent explicity printed on a single line and labelled as to the part of
the TSIdent it is.
*/
public String toStringVerbose() {
	return 
		"Identifier:      '" + __identifier + "'\n" +
		"Alias:           '" + __alias + "'\n" +
		"Full Location:   '" + __full_location + "'\n" + 
		"Main Location:   '" + __main_location + "'\n" + 
		"Sub Location:    '" + __sub_location + "'\n" +
		"Full Source:     '" + __full_source + "'\n" + 
		"Main Source:     '" + __main_source + "'\n" +
		"Sub Source:      '" + __sub_source + "'\n" +
		"Full Type:       '" + __full_type + "'\n" + 
		"Main Type:       '" + __main_type + "'\n" + 
		"Sub Type:        '" + __sub_type + "'\n" + 
		"Interval String: '" + __interval_string + "'\n" +
		"Interval Base:    " + __interval_base + "\n" +
		"Interval Mult:    " + __interval_mult + "\n" + 
		"Scenario:        '" + __scenario + "'\n" + 
		"Sequence Number:  " + __sequence_number + "\n" +
		"Input Type:      '" + __input_type + "'\n" + 
		"Input Name:      '" + __input_name + "'\n" + 
		"Behavior Mask:    " + __behavior_mask + "\n";
}

} // End of TSIdent