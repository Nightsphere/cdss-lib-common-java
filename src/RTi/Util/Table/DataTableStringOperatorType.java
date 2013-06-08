package RTi.Util.Table;

/**
Enumeration of simple string operators that can be performed on table cells.
*/
public enum DataTableStringOperatorType
{

/*
Append string values.
*/
APPEND("Append"),
/*
Prepend string values.
*/
PREPEND ( "Prepend" ),
/*
Cast a string value to a date.
*/
TO_DATE("ToDate"),
/*
Cast a string value to a date/time.
*/
TO_DATE_TIME("ToDateTime"),
/*
Cast a string value to a double.
*/
TO_DOUBLE("ToDouble"),
/*
Cast a string value to an integer.
*/
TO_INTEGER("ToInteger");

/**
 * The name that should be displayed when used in UIs and reports.
 */
private final String displayName;

/**
 * Construct an enumeration value.
 * @param displayName name that should be displayed in choices, etc.
 */
private DataTableStringOperatorType(String displayName) {
    this.displayName = displayName;
}

/**
 * Return the display name for the string operator.  This is usually the same as the
 * value but using appropriate mixed case.
 * @return the display name.
 */
@Override
public String toString() {
    return displayName;
}

/**
 * Return the enumeration value given a string name (case-independent).
 * @return the enumeration value given a string name (case-independent), or null if not matched.
 */
public static DataTableStringOperatorType valueOfIgnoreCase(String name)
{
    DataTableStringOperatorType [] values = values();
    // Currently supported values
    for ( DataTableStringOperatorType t : values ) {
        if ( name.equalsIgnoreCase(t.toString()) ) {
            return t;
        }
    } 
    return null;
}
    
}