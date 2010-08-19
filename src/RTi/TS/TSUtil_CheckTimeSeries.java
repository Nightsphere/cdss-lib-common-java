package RTi.TS;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.Vector;

import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
Check time series values.
*/
public class TSUtil_CheckTimeSeries
{

/**
Check types that can be performed.
*/
private static String __CHECK_TYPE_AbsChangeGreaterThan = "AbsChange>";
private static String __CHECK_TYPE_AbsChangePercentGreaterThan = "AbsChangePercent>";
private static String __CHECK_TYPE_ChangeGreaterThan = "Change>";
private static String __CHECK_TYPE_ChangeLessThan = "Change<";
private static String __CHECK_TYPE_InRange = "InRange";
private static String __CHECK_TYPE_OutOfRange = "OutOfRange";
private static String __CHECK_TYPE_Missing = "Missing";
private static String __CHECK_TYPE_Repeat = "Repeat";
private static String __CHECK_TYPE_LessThan = "<";
private static String __CHECK_TYPE_LessThanOrEqualTo = "<=";
private static String __CHECK_TYPE_GreaterThan = ">";
private static String __CHECK_TYPE_GreaterThanOrEqualTo = ">=";
private static String __CHECK_TYPE_EqualTo = "==";

/**
List of problems generated by this command, guaranteed to be non-null.
*/
private List<String> __problems = new Vector();

/**
Time series to process.
*/
private TS __ts = null;

/**
Indicator for value to check, either "Raw" or "Statistic".
*/
private String __valueToCheck = null;

/**
Type of check to perform.
*/
private String __checkCriteria = null;

/**
Start of analysis (null to analyze all).
*/
private DateTime __analysisStart = null;

/**
End of analysis (null to analyze all).
*/
private DateTime __analysisEnd = null;

/**
Value as input to analysis, depending on checkType.
*/
private Double __value1 = null;

/**
Start of analysis (null to analyze all).
*/
private Double __value2 = null;

/**
Flag string for detected values.
*/
private String __flag = null;

/**
Description for __flag.
*/
private String __flagDesc = null;

/**
Action to be performed on detection (either null/blank for no action, "Remove" to remove the data point,
or "SetMissing" to set the data point to missing.  For regular, Remove and SetMissing are equivalent.
*/
private String __action = null;

/**
Constructor.
*/
public TSUtil_CheckTimeSeries ( TS ts, String valueToCheck, String checkType,
        DateTime analysisStart, DateTime analysisEnd, Double value1, Double value2, String problemType,
        String flag, String flagDesc, String action )
{   String message;
    String routine = getClass().getName() + ".constructor";
	// Save data members.
    __ts = ts;
    __valueToCheck = valueToCheck;
    if ( valueToCheck == null ) {
        __valueToCheck = "DataValue";
    }
    if ( !__valueToCheck.equalsIgnoreCase("DataValue") ) {
        message = "Only \"DataValue\" can be specified for value to check";
        Message.printWarning(3, routine, message);
        throw new InvalidParameterException ( message );
    }
    __checkCriteria = checkType;
    __analysisStart = analysisStart;
    __analysisEnd = analysisEnd;
    __value1 = value1;
    __value2 = value2;
    __flag = flag;
    __flagDesc = flagDesc;
    __action = action;
    if ( (__action != null) && __action.equals("") ) {
        // Set to null for internal handling
        __action = null;
    }
    if ( (action != null) && !action.equalsIgnoreCase("Remove") && !action.equalsIgnoreCase("SetMissing")) {
        message = "Action (" + action + ") is invalid.  Must be Remove or SetMissing if specified";
        Message.printWarning(3, routine, message);
        throw new InvalidParameterException ( message );
    }
}

/**
Check the time series.
*/
public void checkTimeSeries ()
throws Exception
{
    // Create a new list of problems
    __problems = new Vector();
    TS ts = getTimeSeries();
    String checkCriteria = getCheckCriteria();
    String tsid = ts.getIdentifier().toString();
    
    // If time series has no data and check is for missing, add a message.
    // Otherwise skip
    if ( !ts.hasData() ) {
        if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_Missing) ) {
            __problems.add ( "Time series " + tsid + " has no data." );
        }
        return;
    }
    
    // Iterate through the time series

    TSIterator tsi = ts.iterator(getAnalysisStart(), getAnalysisEnd());
    TSData data = null;
    String valueToCheck = getValueToCheck();
    double value1 = (getValue1() == null) ? -999.0 : getValue1().doubleValue();
    double value2 = (getValue2() == null) ? -999.0 : getValue2().doubleValue();
    if ( (ts.getAlias() != null) && !ts.getAlias().equals("") ) {
        tsid = ts.getAlias();
    }
    double tsvalue; // time series data value
    double tsvaluePrev = 0; // time series data value (previous)
    int tsvalueCount = 0; // Number of values processed
    String message = null;
    DateTime date; // Date corresponding to data value
    boolean isMissing;
    boolean matchDetected; // whether a data value matched the check criteria - used to trigger action
    double diff;
    TSData dataPoint = new TSData(); // Used when setting the flag
    // TODO SAM 2010 evaluate whether to check units for precision
    String tsValueFormat = "%.6f"; // Format for values for messages
    while ( (data = tsi.next()) != null ) {
        // Analyze the value - do this brute force with string comparisons and improve performance once logic is in place
        message = null; // A non-null message indicates that the check criteria was met for the value
        date = tsi.getDate();
        matchDetected = false;
        if ( valueToCheck.equals("DataValue") ) {
            tsvalue = data.getData();
            isMissing = ts.isDataMissing(tsvalue);
            if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_AbsChangeGreaterThan) ) {
                if ( (tsvalueCount > 0) && !ts.isDataMissing(tsvaluePrev) && !isMissing ) {
                    diff = tsvalue - tsvaluePrev;
                    if ( Math.abs(diff) > value1 ) {
                        message = "Time series " + tsid + " value " +
                            StringUtil.formatString(tsvalue,tsValueFormat)
                            + " at " + date + " changed more than " +
                            value1 + " since previous value " +
                            StringUtil.formatString(tsvaluePrev,tsValueFormat) + " (diff=" +
                            StringUtil.formatString(diff,tsValueFormat) + ")";
                        matchDetected = true;
                    }
                }
            }
            else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_AbsChangePercentGreaterThan) ) {
                if ( (tsvalueCount > 0) && !ts.isDataMissing(tsvaluePrev) && !isMissing ) {
                    diff = ((tsvalue - tsvaluePrev)/tsvaluePrev)*100.0;
                    if ( Math.abs(diff) > value1 ) {
                        message = "Time series " + tsid + " value " +
                            StringUtil.formatString(tsvalue,tsValueFormat) +
                            " at " + date + " changed more than " +
                            value1 + "% since previous value " +
                            StringUtil.formatString(tsvaluePrev,tsValueFormat) + " (diff %=" +
                            StringUtil.formatString(diff,tsValueFormat) + ")";
                        matchDetected = true;
                    }
                }
            }
            else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_ChangeGreaterThan) ) {
                if ( (tsvalueCount > 0) && !ts.isDataMissing(tsvaluePrev) && !isMissing ) {
                    diff = tsvalue - tsvaluePrev;
                    if ( diff > value1 ) {
                        message = "Time series " + tsid + " value " +
                            StringUtil.formatString(tsvalue,tsValueFormat)
                            + " at " + date + " change is > " +
                            value1 + " since previous value " +
                            StringUtil.formatString(tsvaluePrev,tsValueFormat) + " (diff=" +
                            StringUtil.formatString(diff,tsValueFormat) + ")";
                        matchDetected = true;
                    }
                }
            }
            else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_ChangeLessThan) ) {
                if ( (tsvalueCount > 0) && !ts.isDataMissing(tsvaluePrev) && !isMissing ) {
                    diff = tsvalue - tsvaluePrev;
                    if ( diff < value1 ) {
                        message = "Time series " + tsid + " value " +
                            StringUtil.formatString(tsvalue,tsValueFormat)
                            + " at " + date + " change is < " +
                            value1 + " since previous value " +
                            StringUtil.formatString(tsvaluePrev,tsValueFormat) + " (diff=" +
                            StringUtil.formatString(diff,tsValueFormat) + ")";
                        matchDetected = true;
                    }
                }
            }
            else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_EqualTo) ) {
                if ( !isMissing && (tsvalue == value1) ) {
                    message = "Time series " + tsid + " value " +
                    StringUtil.formatString(tsvalue,tsValueFormat) +
                    " at " + date + " is = test value " + value1;
                    matchDetected = true;
                }
            }
            else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_GreaterThan) ) {
                if ( !isMissing && (tsvalue > value1) ) {
                    message = "Time series " + tsid + " value " +
                    StringUtil.formatString(tsvalue,tsValueFormat) +
                    " at " + date + " is > limit " + value1;
                    matchDetected = true;
                }
            }
            else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_GreaterThanOrEqualTo) ) {
                if ( !isMissing && (tsvalue >= value1) ) {
                    message = "Time series " + tsid + " value " +
                    StringUtil.formatString(tsvalue,tsValueFormat) +
                    " at " + date + " is >= limit " + value1;
                    matchDetected = true;
                }
            }
            else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_InRange) ) {
                if ( !isMissing && (tsvalue >= value1) && (tsvalue <= value2) ) {
                    message = "Time series " + tsid + " value " +
                    StringUtil.formatString(tsvalue,tsValueFormat) +
                    " at " + date + " is in range " + value1 + " to " + value2;
                    matchDetected = true;
                }
            }
            else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_LessThan) ) {
                if ( !isMissing && (tsvalue < value1) ) {
                    message = "Time series " + tsid + " value " +
                        StringUtil.formatString(tsvalue,tsValueFormat) +
                        " at " + date + " is < limit " + value1;
                    matchDetected = true;
                }
            }
            else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_LessThanOrEqualTo) ) {
                if ( !isMissing && (tsvalue <= value1) ) {
                    message = "Time series " + tsid + " value " +
                    StringUtil.formatString(tsvalue,tsValueFormat) +
                    " at " + date + " is <= limit " + value1;
                    matchDetected = true;
                }
            }
            else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_Missing) ) {
                if ( isMissing ) {
                    message = "Time series " + tsid + " value " + 
                    StringUtil.formatString(tsvalue,tsValueFormat) +
                    " at " + date + " is missing";
                    matchDetected = true;
                }
            }
            else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_OutOfRange) ) {
                if ( !isMissing && (tsvalue < value1) || (tsvalue > value2) ) {
                    message = "Time series " + tsid + " value " +
                    StringUtil.formatString(tsvalue,tsValueFormat) +
                    " at " + date + " is out of range " + value1 + " to " + value2;
                    matchDetected = true;
                }
            }
            else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_Repeat) ) {
                if ( !isMissing && (tsvalueCount > 0) && !ts.isDataMissing(tsvalue) && !ts.isDataMissing(tsvalue) &&
                    (tsvalue == tsvaluePrev) ) {
                    message = "Time series " + tsid + " value " +
                    StringUtil.formatString(tsvalue,tsValueFormat) +
                    " at " + date + " repeated previous value";
                    matchDetected = true;
                }
            }
            if ( message != null ) {
                // Add to the problems list
                __problems.add ( message );
                if ( __flag != null ) {
                    // Update the flag value
                    dataPoint = ts.getDataPoint(date, dataPoint );
                    dataPoint.setDataFlag ( __flag );
                    ts.setDataValue(date, dataPoint.getData(), dataPoint.getDataFlag(), dataPoint.getDuration() );
                }
            }
            // If an action is required, do it
            if ( matchDetected && (__action != null) )  {
                if ( __action.equalsIgnoreCase("Remove") ) {
                    if ( ts instanceof IrregularTS ) {
                        // Remove the data point from memory
                        ((IrregularTS)ts).removeDataPoint(date);
                    }
                    else {
                        // Set to missing
                        ts.setDataValue(date, ts.getMissing() );
                    }
                }
                else if ( __action.equalsIgnoreCase("SetMissing") ) {
                    ts.setDataValue(date, ts.getMissing() );
                }
            }
            // Increment the count and save the previous value
            ++tsvalueCount;
            tsvaluePrev = tsvalue;
        }
        else if ( valueToCheck.equals("Statistic") ) {
            // TODO SAM 2009-04-20 Need to implement statistic checks
        }
    }
    if ( (__flag != null) && !__flag.equals("") && (__problems.size() > 0) ) {
        // Remove leading + on flag, used to indicate concatenation
        String flag = StringUtil.remove(__flag,"+");
        if ( (__flagDesc == null) || __flagDesc.equals("") ) {
            // Default description...
            message = "Detected " + __problems.size() + " values where " + formatCriteriaForFlagDesc() + ".";
            ts.addDataFlagMetadata(new TSDataFlagMetadata( flag, message));
        }
        else {
            // Use supplied description...
            message = "Detected " + __problems.size() + " values where " + __flagDesc + ".";
            ts.addDataFlagMetadata(new TSDataFlagMetadata( flag, message));
        }
        // Add a message to the genesis since flags have been set...
        ts.addToGenesis ( message + "  Set flag to " + flag + "." );
    }
}

/**
Format the criteria for use in output.
@return a string that describes the criteria, suitable for the flag description.
*/
private String formatCriteriaForFlagDesc ()
{
    if ( __checkCriteria.equalsIgnoreCase(__CHECK_TYPE_AbsChangeGreaterThan) ) {
        return "abs(change(value)) > " + StringUtil.formatString(__value1,"%.6f");
    }
    else if ( __checkCriteria.equalsIgnoreCase(__CHECK_TYPE_AbsChangePercentGreaterThan) ) {
        return "precent(abs(change(value))) > " + StringUtil.formatString(__value1,"%.6f");
    }
    else if ( __checkCriteria.equalsIgnoreCase(__CHECK_TYPE_ChangeGreaterThan) ) {
        return "change(value) > " + StringUtil.formatString(__value1,"%.6f");
    }
    else if ( __checkCriteria.equalsIgnoreCase(__CHECK_TYPE_ChangeLessThan) ) {
        return "change(value) < " + StringUtil.formatString(__value1,"%.6f");
    }
    else if ( __checkCriteria.equalsIgnoreCase(__CHECK_TYPE_InRange) ) {
        return StringUtil.formatString(__value1,"%.6f") + " <= value <= " +
            StringUtil.formatString(__value2,"%.6f");
    }
    else if ( __checkCriteria.equalsIgnoreCase(__CHECK_TYPE_OutOfRange) ) {
        return "value < " + StringUtil.formatString(__value1,"%.6f") + " OR value > " +
        StringUtil.formatString(__value2,"%.6f");
    }
    else if ( __checkCriteria.equalsIgnoreCase(__CHECK_TYPE_Missing) ) {
        return "value is missing";
    }
    else if ( __checkCriteria.equalsIgnoreCase(__CHECK_TYPE_Repeat) ) {
        return "value repeats previous value";
    }
    else if ( __checkCriteria.equalsIgnoreCase(__CHECK_TYPE_LessThan) ) {
        return "value < " + StringUtil.formatString(__value1,"%.6f");
    }
    else if ( __checkCriteria.equalsIgnoreCase(__CHECK_TYPE_LessThanOrEqualTo) ) {
        return "value <= " + StringUtil.formatString(__value1,"%.6f");
    }
    else if ( __checkCriteria.equalsIgnoreCase(__CHECK_TYPE_GreaterThan) ) {
        return "value > " + StringUtil.formatString(__value1,"%.6f");
    }
    else if ( __checkCriteria.equalsIgnoreCase(__CHECK_TYPE_GreaterThanOrEqualTo) ) {
        return "value >= " + StringUtil.formatString(__value1,"%.6f");
    }
    else if ( __checkCriteria.equalsIgnoreCase(__CHECK_TYPE_EqualTo) ) {
        return "value = " + StringUtil.formatString(__value1,"%.6f");
    }
    else {
        throw new InvalidParameterException ( "Unrecognized check criteria \"" + __checkCriteria + "\"" );
    }
}

/**
Return the analysis end date/time.
@return the analysis end date/time.
*/
public DateTime getAnalysisEnd ()
{
    return __analysisEnd;
}

/**
Return the analysis start date/time.
@return the analysis start date/time.
*/
public DateTime getAnalysisStart ()
{
    return __analysisStart;
}

/**
Return the check type.
@return the check type.
*/
public String getCheckCriteria ()
{
    return __checkCriteria;
}

/**
Get the list of check types that can be performed.
*/
public static List getCheckCriteriaChoices()
{
    List choices = new Vector();
    choices.add ( __CHECK_TYPE_AbsChangeGreaterThan );
    choices.add ( __CHECK_TYPE_AbsChangePercentGreaterThan );
    choices.add ( __CHECK_TYPE_ChangeGreaterThan );
    choices.add ( __CHECK_TYPE_ChangeLessThan );
    choices.add ( __CHECK_TYPE_InRange );
    choices.add ( __CHECK_TYPE_OutOfRange );
    choices.add ( __CHECK_TYPE_Missing );
    choices.add ( __CHECK_TYPE_Repeat );
    choices.add ( __CHECK_TYPE_LessThan );
    choices.add ( __CHECK_TYPE_LessThanOrEqualTo );
    choices.add ( __CHECK_TYPE_GreaterThan );
    choices.add ( __CHECK_TYPE_GreaterThanOrEqualTo );
    choices.add ( __CHECK_TYPE_EqualTo );
    return choices;
}

/**
Return the number of values that are required to evaluate a criteria.
@return the number of values that are required to evaluate a criteria.
@param checkCriteria the check criteria that is being evaluated.
*/
public static int getRequiredNumberOfValuesForCheckCriteria ( String checkCriteria )
{
    // TODO SAM 2009-04-23 Need to convert to enumeration or something other than simple strings
    if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_AbsChangePercentGreaterThan) ) {
        return 1;
    }
    else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_AbsChangeGreaterThan) ) {
        return 1;
    }
    else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_ChangeGreaterThan) ) {
        return 1;
    }
    else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_ChangeLessThan) ) {
        return 1;
    }
    else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_EqualTo) ) {
        return 1;
    }
    else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_GreaterThan) ) {
        return 1;
    }
    else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_GreaterThanOrEqualTo) ) {
        return 1;
    }
    else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_InRange) ) {
        return 2;
    }
    else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_LessThan) ) {
        return 1;
    }
    else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_LessThanOrEqualTo) ) {
        return 1;
    }
    else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_Missing) ) {
        return 0;
    }
    else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_OutOfRange) ) {
        return 2;
    }
    else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_Repeat) ) {
        return 0;
    }
    else {
        String message = "Requested criteria is not recognized: " + checkCriteria;
        String routine = "TSUtil_CheckTimeSeries.getRequiredNumberOfValuesForCheckCriteria";
        Message.printWarning(3, routine, message);
        throw new InvalidParameterException ( message );
    }
}

/**
Return a list of problems for the time series.
*/
public List<String> getProblems ()
{
    return __problems;
}

/**
Return the time series being analyzed.
@return the time series being analyzed.
*/
public TS getTimeSeries ()
{
    return __ts;
}

/**
Return Value1 for the check.
@return Value1 for the check.
*/
public Double getValue1 ()
{
    return __value1;
}

/**
Return Value2 for the check.
@return Value2 for the check.
*/
public Double getValue2 ()
{
    return __value2;
}

/**
Return the value to check.
@return the value to check.
*/
public String getValueToCheck ()
{
    return __valueToCheck;
}

}