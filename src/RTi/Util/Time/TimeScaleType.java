package RTi.Util.Time;

import java.util.List;
import java.util.Vector;

/**
Time scale types, which are important when making time-based data observations.
*/
public enum TimeScaleType
{
/**
Data are accumulated over the time interval prior to the recorded date/time.
*/
ACCM ( "ACCM", "Accumulated" ),
/**
Instantaneous data value is recorded at the date/time.
*/
INST ( "INST", "Instantaneous" ),
/**
Data are averaged over the time interval prior to the recorded date/time.
*/
MEAN ( "MEAN", "Mean" );

/**
The name that is used for choices and other technical code (terse).
*/
private final String displayName;

/**
The name that is used for notes and explanations (more verbose).
*/
private final String displayNameVerbose;

/**
Construct a time series statistic enumeration value.
@param displayName name that should be displayed in choices, etc.
*/
private TimeScaleType(String displayName, String displayNameVerbose ) {
    this.displayName = displayName;
    this.displayNameVerbose = displayNameVerbose;
}

/**
Get the list of time scales.
@return the list of time scales.
*/
public static List<TimeScaleType> getTimeScaleChoices()
{
    List<TimeScaleType> choices = new Vector();
    choices.add ( TimeScaleType.ACCM );
    choices.add ( TimeScaleType.INST );
    choices.add ( TimeScaleType.MEAN );
    return choices;
}

/**
Get the list of time scales.
@return the list of time scales as strings.
@param includeNote If true, the returned string will be of the form
"ACCM - Accumulated", using the sort and verbose display names.
If false, the returned string will be of the form "ACCM", using only the short display name.
*/
public static List<String> getTimeScaleChoicesAsStrings( boolean includeNote )
{
    List<TimeScaleType> choices = getTimeScaleChoices();
    List<String> stringChoices = new Vector();
    for ( int i = 0; i < choices.size(); i++ ) {
        TimeScaleType choice = choices.get(i);
        String choiceString = "" + choice;
        if ( includeNote ) {
            choiceString = choiceString + " - " + choice.toStringVerbose();
        }
        stringChoices.add ( choiceString );
    }
    return stringChoices;
}

/**
Return the short display name for the statistic.  This is the same as the value.
@return the display name.
*/
@Override
public String toString() {
    return displayName;
}

/**
Return the verbose display name for the statistic.
@return the verbose display name (e.g., "Accumulated" instead of the shorter "ACCM").
*/
public String toStringVerbose() {
    return displayNameVerbose;
}

/**
Return the enumeration value given a string name (case-independent).
@param name the time scale string to match, as either the short or verbose display name, or the
concatenated version "displayName - displayNameVerbose".  
@return the enumeration value given a string name (case-independent), or null if not matched.
@exception IllegalArgumentException if the name does not match a valid time scale.
*/
public static TimeScaleType valueOfIgnoreCase (String name)
{
    TimeScaleType [] values = values();
    for ( TimeScaleType t : values ) {
        if ( name.equalsIgnoreCase(t.toString()) || name.equalsIgnoreCase(t.toStringVerbose()) ||
            name.equalsIgnoreCase(t.toString() + " - " + t.toStringVerbose() )) {
            return t;
        }
    }
    throw new IllegalArgumentException ( "The following does not match a valid time scale: \"" + name + "\"");
}

}