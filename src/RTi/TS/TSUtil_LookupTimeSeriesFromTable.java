package RTi.TS;

import java.util.List;
import java.util.Vector;

import RTi.Util.Math.DataTransformationType;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.LookupMethodType;
import RTi.Util.Table.OutOfRangeLookupMethodType;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.DateTimeWindow;

/**
Lookup time series values from a time series and lookup table.
*/
public class TSUtil_LookupTimeSeriesFromTable
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
Date/time column for time series, 0+.
*/
private int __dateTimeColumn = -1;

/**
Data columns for time series, 0+.
*/
private int [] __dataColumns = null;

/**
First data row for time series, 0+.
*/
private int __dataRow = -1;

/**
Start of analysis (null to analyze all).
*/
private DateTime __outputStart = null;

/**
End of analysis (null to analyze all).
*/
private DateTime __outputEnd = null;

/**
Window within the year to transfer data values.
*/
private DateTimeWindow __outputWindow = null;

/**
Indicate whether missing values in time series should be transferred as null (true) or the
missing values transferred (false).
*/
private boolean __useNullForMissing = false;

/**
Constructor.
@param inputTS input time series used for lookup
@param outputTS output time series used for lookup
@param lookupTable Data table being filled with time series.  Column names need to have been defined but the
table is expected to be empty (no rows)
@param effectiveDateColumn date/time column (0+)
@param dataColumns data columns (0+) corresponding to the correct column names for the time series
@param dataRow data row (0+) corresponding to first date/time to be transferred
@param analysisStart first date/time to be transferred (if null, output all)
@param analysisEnd last date/time to be transferred (if null, output all)
*/
public TSUtil_LookupTimeSeriesFromTable ( TS inputTS, TS outputTS, DataTable lookupTable, int value1Col,
    int value2Col, int effectiveDateColumn, LookupMethodType lookupMethodType,
    OutOfRangeLookupMethodType outOfRangeLookupMethodType, String outOfRangeNotification,
    DataTransformationType transformation, double leZeroLogValue,
    DateTime analysisStart, DateTime analysisEnd )
{   //String message;
    //String routine = getClass().getName() + ".constructor";
	// Save data members.
    /*
    __table = lookupTable;
    __tsList = inputTS;
    __dateTimeColumn = effectiveDateColumn;
    __dataColumns = dataColumns;
    __dataRow = dataRow;
    __outputStart = analysisStart;
    __outputEnd = analysisEnd;
    __outputWindow = outputWindow; // Allow null to speed performance checks
    __useNullForMissing = useNullForMissing;
    // Make sure that the time series are regular and of the same interval
    if ( !TSUtil.intervalsMatch(inputTS) ) {
        throw new UnequalTimeIntervalException (
            "Time series don't have the same interval - cannot convert to table.");
    }
    if ( TSUtil.areAnyTimeSeriesIrregular(inputTS) ) {
        throw new IrregularTimeSeriesNotSupportedException (
            "Irregular time series cannot be converted to a table.");
    }
    */
}

/**
Set the time series values by looking up from the time series and table.
*/
public void lookupTimeSeriesFromTable ()
{
    // Create a new list of problems
    __problems = new Vector();
    
    /*
    // If the output start and end are not specified, use the maximum period
    if ( (__outputStart == null) || (__outputEnd == null) ) {
        // One or more of the requested dates is null so find the full period of the data
        TSLimits limits = null;
        try {
            limits = TSUtil.getPeriodFromTS(__tsList, TSUtil.MAX_POR );
            if ( __outputStart == null ) {
                __outputStart = new DateTime(limits.getDate1());
            }
            if ( __outputEnd == null ) {
                __outputEnd = new DateTime(limits.getDate2());
            }
        }
        catch ( Exception e ) {
            // Worst case use the period from the first time series
            __outputStart = __tsList.get(0).getDate1();
            __outputEnd = __tsList.get(0).getDate2();
        }
    }
    
    // Iterate through the dates
    int its; // iterator for time series
    TS ts; // time series being processed
    int tsListSize = __tsList.size();
    int intervalBase = __tsList.get(0).getDataIntervalBase();
    int intervalMult = __tsList.get(0).getDataIntervalMult();
    int rowCount = 0;
    int setRow, setColumn; // Row and column for data set
    double value; // Data value from time series
    DateTime date = null;
    for (date = new DateTime(__outputStart); date.lessThanOrEqualTo(__outputEnd);
        date.addInterval(intervalBase,intervalMult) ) {
        if ( (__outputWindow != null) && !__outputWindow.isDateTimeInWindow(date) ) {
            // Don't add the row...
            continue;
        }
        // Set the date/time
        setRow = __dataRow + rowCount;
        try {
            __table.setFieldValue(setRow, __dateTimeColumn, new DateTime(date), true );
        }
        catch ( Exception e ) {
            __problems.add ( "Error setting date " + date + " for row [" + setRow + "] (" + e + ").");
        }
        // Iterate through the time series
        for ( its = 0; its < tsListSize; its++ ) {
            ts = __tsList.get(its);
            value = ts.getDataValue(date);
            setColumn = __dataColumns[its];
            try {
                if ( ts.isDataMissing(value) && __useNullForMissing ) {
                    __table.setFieldValue(setRow, setColumn, null, true );
                }
                else {
                    // Set as a double because non-missing or missing and the missing value should be used
                    __table.setFieldValue(setRow, setColumn, new Double(value), true );
                }
            }
            catch ( Exception e ) {
                __problems.add ( "Error setting data value " + value +
                    " at " + date + " [" + setRow + "][" + setColumn + "] (" + e + ").");
            }
        }
        // If here the row was added so increment for the next add
        ++rowCount;
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
Return the output window.
@return the output window.
*/
public DateTimeWindow getOutputWindow ()
{
    return __outputWindow;
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