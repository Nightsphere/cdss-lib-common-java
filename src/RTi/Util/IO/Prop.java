// ----------------------------------------------------------------------------
// Prop - use to hold an object's properties
// ----------------------------------------------------------------------------
// Notes:	(1)	This is useful for program or other component
//			information.
//		(2)	PropList manages a list of these properties.
// ----------------------------------------------------------------------------
// History:
//
// Sep 1997?	Steven A. Malers,	Initial version.  Start dabbling to
//		Riverside Technology,	formalize update of legacy setDef/
//		inc.			getDef code.
// 02 Feb 1998	SAM, RTi		Get all the Prop* classes working
//					together and start to use in
//					production.
// 24 Feb 1998	SAM, RTi		Add the javadoc comments.
// 13 Apr 1999	SAM, RTi		Add finalize.
// 10 May 2001	SAM, RTi		Add ability to expand the property
//					contents as per makefile and RTi
//					property file.  Do so by overloading
//					getValue() to take a persistent format
//					flag to indicate that expansion should
//					be checked.  Also add refresh().
// 2002-02-03	SAM, RTi		Change long _flags integer _how_set and
//					clean up the SET_* values - use with
//					PropList's _how_set flag to streamline
//					tracking user input.  Change methods to
//					be of void type rather than return an
//					int (since the return type is of no
//					importance).
// 2004-02-19	J. Thomas Sapienza, RTi	Implements the Comparable interface so
//					that a PropList can be sorted.
// 2004-05-10	SAM, RTi		Add a "how set" option of
//					SET_AT_RUNTIME_FOR_USER.
// 2005-10-20	JTS, RTi		Added a "how set" option of 
//					SET_HIDDEN for Properties that are 
//					always behind-the-scenes, which should
//					never be saved, viewed, or known by
//					users.
// ----------------------------------------------------------------------------

package RTi.Util.IO;

/**
This class provides a way to generically store property information and can
be used similar to Java properties, environment variables, etc.  The main
difference is that it allows the contents of a property to be an Object, which
allows flexibility to use the property for anything in Java.
<p>

A property essentially consists of a string key and an associated object.
The functions that deal with property "contents" return the literal object.
The functions that deal with property "value" return a string representation of
the value.  In many cases, the Object will actually be a String and therefore
the contents and value will be the same (there is currently no constructor to
take contents only - if it is added, then the value will be set to
contents.toString() at construction).

@see PropList
@see PropListManager
*/
public class Prop
implements Comparable
{

/**
Indicates that it is unknown how the property was set (this is the default).
*/
public static final int SET_UNKNOWN = 0;

/**
Indicates that the property was set from a file or database.  In this case,
when a PropList is saved, the property should typically be saved.
*/
public static final int SET_FROM_PERSISTENT = 1;

/**
Indicates that the property was set at run-time as a default value.  In this
case, when a PropList is saved, the property often may be ignored because it
will be set to the same default value the next time.
*/
public static final int SET_AS_RUNTIME_DEFAULT = 2;

/**
Indicates that the property was set by the user at run-time.  In this case,
when a PropList is saved, the property should likely be saved because the user
has specified a value different from internal defaults.
*/
public static final int SET_AT_RUNTIME_BY_USER = 3;

/**
Indicates that the property was automatically set for the user at run-time.  In
this case, when a PropList is saved, the property should likely be saved because
the user the property is considered important in defining something.  However,
for all practical purposes, it is a run-time default and, in and of itself,
should not force the user to save.
*/
public static final int SET_AT_RUNTIME_FOR_USER = 4;

/**
Indicates that the property was set behind the scenes in a way that should be
invisible to the user.  Users cannot edit hidden properties, will never see
hidden properties, and should never be able to save hidden properties to a
persistent source.
*/
public static final int SET_HIDDEN = 5;

private int	_how_set;	// Indicates whether property is read from a
				// persistent source, set internally as a
				// run-time default, or is set at runtime by the
				// user.
private int	_intkey;	// Integer key for faster lookups.
private String	_key;		// String to look up property.
private Object	_contents;	// Contents of property (anything derived from
				// Object).  This may be a string or another
				// object.  If a string, it contains the value
				// before expanding wildcards, etc.
private String	_value;		// Value of the object as a string.  In most
				// cases, the object will be a string.  The
				// value is the fully-expanded string (wildcards
				// and other variables are expanded).  If not
				// a string, this may contain the toString()
				// representation.

/**
Construct a property having no key and no object (not very useful!).
*/
public Prop ()
{	initialize ( SET_UNKNOWN, 0, "", null, null );
}

/**
Construct using a string key and a string.
@param key String to use as key to look up property.
@param contents The contents of the property (in this case the same as the
value.
*/
public Prop ( String key, String contents )
{	// Contents and value are the same.
	initialize ( SET_UNKNOWN, 0, key, contents, contents );
}

/**
Construct using a string key, and both contents and string value.
@param key String to use as key to look up property.
@param contents The contents of the property (in this case the same as the
@param value The value of the property as a string.
*/
public Prop ( String key, Object contents, String value )
{	// Contents and string are different...
	initialize ( SET_UNKNOWN, 0, key, contents, value );
}

/**
Construct using a string key, and both contents and string value.
@param key String to use as key to look up property.
@param contents The contents of the property (in this case the same as the
@param value The value of the property as a string.
@param how_set Indicates how the property is being set.
*/
public Prop ( String key, Object contents, String value, int how_set )
{	// Contents and string are different...
	initialize ( how_set, 0, key, contents, value );
}

/**
Construct using a string key, an integer key, and string contents.
@param key String to use as key to look up property.
@param intkey Integer to use to look up the property (integer keys can be used
in place of strings for lookups).
@param contents The contents of the property (in this case the same as the
*/
public Prop ( String key, int intkey, String contents )
{	initialize ( SET_UNKNOWN, intkey, key, contents, contents );
}

/**
Construct using a string key, an integer key, and both contents and value.
@param key String to use as key to look up property.
@param intkey Integer to use to look up the property (integer keys can be used
in place of strings for lookups).
@param contents The contents of the property.
@param value The string value of the property.
*/
public Prop ( String key, int intkey, Object contents, String value )
{	initialize ( SET_UNKNOWN, intkey, key, contents, value );
}

/**
Construct using a string key, an integer key, and both contents and value.
@param key String to use as key to look up property.
@param intkey Integer to use to look up the property (integer keys can be used
in place of strings for lookups).
@param contents The contents of the property.
@param value The string value of the property.
@param how_set Indicates how the property is being set.
*/
public Prop ( String key, int intkey, Object contents, String value,int how_set)
{	initialize ( how_set, intkey, key, contents, value );
}

/**
Construct using a string key, an integer key, string contents, and specify
modifier flags.
@param key String to use as key to look up property.
@param intkey Integer to use to look up the property (integer keys can be used
in place of strings for lookups).
@param contents The contents of the property (in this case the same as the
value.
@param how_set Indicates how the property is being set (see SET_*).
*/
public Prop ( String key, int intkey, String contents, int how_set )
{	initialize ( how_set, intkey, key, contents, contents );
}

/**
Used to compare this Prop to another Prop in order to sort them.  Inherited
from Comparable interface.
@param o the Prop to compare against.
@return 0 if the Props' keys and values are the same, or -1 if this Prop sorts
earlier than the other Prop, or 1 if this Prop sorts higher than the other Prop.
*/
public int compareTo(Object o) {
	Prop p = (Prop)o;

	int result = 0;

	result = _key.compareTo(p.getKey());
	if (result != 0) {
		return result;
	}

	result = _value.compareTo(p.getValue());
	return result;
}

/**
Finalize before garbage collection.
@exception Throwable if there is an error.
*/
protected void finalize ()
throws Throwable
{	_key = null;
	_contents = null;
	_value = null;
	super.finalize();
}

/**
Return the contents (Object) for the property.
@return The contents (Object) for the property (note: the original is returned,
not a copy).
*/
public Object getContents ()
{	return _contents;
}

/**
Return the way that the property was set (see SET_*).
@return the way that the property was set.
*/
public int getHowSet ()
{	return _how_set;
}

/**
Return the integer key for the property.
@return The integer key for the property.
*/
public int getIntKey ()
{	return _intkey;
}

/**
Return the string key for the property.
@return The string key for the property.
*/
public String getKey ()
{	return _key;
}

/**
Return the string value for the property.
@return The string value for the property.
*/
public String getValue ()
{	return _value;
}

/**
Return the string value for the property expanding the contents if necessary.
@param props PropList to search.
@return The string value for the property.
*/
public String getValue ( PropList props )
{	// This will expand contents if necessary...
	refresh ( props );
	return _value;
}

/**
Initialize member data.
*/
private void initialize (	int how_set, int intkey, String key,
				Object contents, String value )
{	_how_set = how_set;
	_intkey	= intkey;
	if ( key == null ) {
		_key = ""; 
	}
	else {	_key = key;
	}
	_contents = contents;
	if ( value == null ) {
		_value = "";
	}
	else {	_value = value;
	}
}

/**
Refresh the contents by resetting the value by expanding the contents.
@param props PropList to search.
@return The string value for the property.
*/
public void refresh ( PropList props )
{	int persistent_format = props.getPersistentFormat();
 	if (	(persistent_format == PropList.FORMAT_MAKEFILE) ||
		(persistent_format == PropList.FORMAT_NWSRFS) ||
		(persistent_format == PropList.FORMAT_PROPERTIES) ) {
		// Try to expand the contents...
		if ( _contents instanceof String ) {
			_value = PropListManager.resolveContentsValue(
			props,(String)_contents);
		}
	}
}

/**
Set the contents for a property.
@param contents The contents of a property as an Object.
*/
public void setContents ( Object contents )
{	// Use a reference here (do we need a copy?)...

	if ( contents != null ) {
		_contents = contents;
	}
}

/**
Set how the property is being set (see SET_*).
Set how the property is being set.
*/
public void setHowSet ( int how_set )
{	_how_set = how_set;
}

/**
Set the integer key for the property.  This is usually maintained by
PropList.
@param intkey Integer key for the property.
@see PropList
*/
public void setIntKey ( int intkey )
{	_intkey = intkey;
}

/**
Set the string key for the property.
@param key String key for the property.
*/
public void setKey ( String key )
{	if ( key != null ) {
		_key = key;
	}
}

/**
Set the string value for the property.
@param value The string value for the property.
*/
public void setValue ( String value )
{	if ( value != null ) {
		_value = value;
	}
}

/**
Return a string representation of the property (a verbose output).
@return a string representation of the property.
*/
public String toString ()
{	return "Key=\"" + _key + "\" (" + _intkey + "), value = \"" + _value +
		"\" _how_set= " + _how_set;
}

} // End Prop