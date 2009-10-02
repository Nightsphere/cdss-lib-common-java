package RTi.TS;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.Vector;

import RTi.Util.Message.Message;
import RTi.Util.Table.DataTable;
import RTi.Util.Time.DateTime;

/**
Check time series values.
*/
public class TSUtil_TimeSeriesToTable
{

/**
List of problems generated by this command, guaranteed to be non-null.
*/
private List<String> __problems = new Vector();

/**
Data table being filled with time series.
*/
private DataTable __table = null;

/**
Time series to process.
*/
private List<TS> __tsList = null;

/**
Start of analysis (null to analyze all).
*/
private DateTime __outputStart = null;

/**
End of analysis (null to analyze all).
*/
private DateTime __outputEnd = null;

/**
Constructor.
*/
public TSUtil_TimeSeriesToTable ( DataTable table, List<TS> tslist, DateTime outputStart, DateTime outputEnd )
{   //String message;
    //String routine = getClass().getName() + ".constructor";
	// Save data members.
    __table = table;
    __tsList = tslist;
    __outputStart = outputStart;
    __outputEnd = outputEnd;
}

/**
Copy the time series into the table.
*/
public void timeSeriesToTable ()
throws Exception
{
    // Create a new list of problems
    __problems = new Vector();
    
    /*
    // Iterate through the time series
    TS ts = getTimeSeries();
    TSIterator tsi = ts.iterator(getAnalysisStart(), getAnalysisEnd());
    TSData data = null;
    String valueToCheck = getValueToCheck();
    String checkCriteria = getCheckCriteria();
    double value1 = (getValue1() == null) ? -999.0 : getValue1().doubleValue();
    double value2 = (getValue2() == null) ? -999.0 : getValue2().doubleValue();
    String tsid = ts.getIdentifier().toString();
    if ( (ts.getAlias() != null) && !ts.getAlias().equals("") ) {
        tsid = ts.getAlias();
    }
    double tsvalue; // time series data value
    double tsvaluePrev = 0; // time series data value (previous)
    int tsvalueCount = 0; // Number of values processed
    String message = null;
    DateTime date; // Date corresponding to data value
    boolean isMissing;
    double diff;
    while ( (data = tsi.next()) != null ) {
        // Analyze the value - do this brute force with string comparisons and improve performance once logic is in place
        message = null;
        date = tsi.getDate();
        if ( valueToCheck.equals("DataValue") ) {
            tsvalue = data.getData();
            isMissing = ts.isDataMissing(tsvalue);
            if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_AbsChangeGreaterThan) ) {
                if ( (tsvalueCount > 0) && !ts.isDataMissing(tsvaluePrev) && !isMissing ) {
                     diff = tsvalue - tsvaluePrev;
                    if ( Math.abs(diff) > value1 ) {
                        message = "Time series " + tsid + " value " + tsvalue + " at " + date + " changed more than " +
                            value1 + " since previous value " + tsvaluePrev + " (diff=" + diff + ")";
                    }
                }
            }
            else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_AbsChangePercentGreaterThan) ) {
                if ( (tsvalueCount > 0) && !ts.isDataMissing(tsvaluePrev) && !isMissing ) {
                    diff = ((tsvalue - tsvaluePrev)/tsvaluePrev)*100.0;
                    if ( Math.abs(diff) > value1 ) {
                        message = "Time series " + tsid + " value " + tsvalue + " at " + date + " changed more than " +
                            value1 + " since previous value " + tsvaluePrev + " (diff=" + diff + " %)";
                    }
                }
            }
            else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_EqualTo) ) {
                if ( !isMissing && (tsvalue == value1) ) {
                    message = "Time series " + tsid + " value " + tsvalue + " at " + date + " is = test value " + value1;
                }
            }
            else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_GreaterThan) ) {
                if ( !isMissing && (tsvalue > value1) ) {
                    message = "Time series " + tsid + " value " + tsvalue + " at " + date + " is > limit " + value1;
                }
            }
            else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_GreaterThanOrEqualTo) ) {
                if ( !isMissing && (tsvalue >= value1) ) {
                    message = "Time series " + tsid + " value " + tsvalue + " at " + date + " is >= limit " + value1;
                }
            }
            else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_InRange) ) {
                if ( !isMissing && (tsvalue >= value1) && (tsvalue <= value2) ) {
                    message = "Time series " + tsid + " value " + tsvalue + " at " + date + " is in range " + value1 +
                    " to " + value2;
                }
            }
            else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_LessThan) ) {
                if ( !isMissing && (tsvalue < value1) ) {
                    message = "Time series " + tsid + " value " + tsvalue + " at " + date + " is < limit " + value1;
                }
            }
            else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_LessThanOrEqualTo) ) {
                if ( !isMissing && (tsvalue <= value1) ) {
                    message = "Time series " + tsid + " value " + tsvalue + " at " + date + " is <= limit " + value1;
                }
            }
            else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_Missing) ) {
                if ( isMissing ) {
                    message = "Time series " + tsid + " value " + tsvalue + " at " + date + " is missing";
                }
            }
            else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_OutOfRange) ) {
                if ( !isMissing && (tsvalue < value1) || (tsvalue > value2) ) {
                    message = "Time series " + tsid + " value " + tsvalue + " at " + date + " is out of range " + value1 +
                    " to " + value2;
                }
            }
            else if ( checkCriteria.equalsIgnoreCase(__CHECK_TYPE_Repeat) ) {
                if ( !isMissing && (tsvalueCount > 0) && !ts.isDataMissing(tsvalue) && !ts.isDataMissing(tsvalue) &&
                        (tsvalue == tsvaluePrev) ) {
                    message = "Time series " + tsid + " value " + tsvalue + " at " + date + " repeated previous value";
                }
            }
            if ( message != null ) {
                // Add to the problems list
                __problems.add ( message );
            }
            // Increment the count and save the previous value
            ++tsvalueCount;
            tsvaluePrev = tsvalue;
        }
        else if ( valueToCheck.equals("Statistic") ) {
            // TODO SAM 2009-04-20 Need to implemented
        }
    }
    */
}

/**
Return the output end date/time.
@return the output end date/time.
*/
public DateTime getOutputEnd ()
{
    return __outputEnd;
}

/**
Return the output start date/time.
@return the output start date/time.
*/
public DateTime getOutputStart ()
{
    return __outputStart;
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
public List<TS> getTimeSeriesList ()
{
    return __tsList;
}

}