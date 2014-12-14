package RTi.TS;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import RTi.Util.Message.Message;
import RTi.Util.Table.DataTable;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.DateTimeWindow;

/**
Fill a table with time series values.
*/
public class TSUtil_TimeSeriesToTable
{

/**
List of problems generated by this command, guaranteed to be non-null.
*/
private List<String> __problems = new ArrayList<String>();

/**
Data table being filled with time series.
*/
private DataTable __table = null;

/**
Time series to process.
*/
private List<TS> __tsList = null;

/**
Date/time column, 0+.
*/
private int __dateTimeColumn = -1;

/**
TSID column, 0+, for single-column output.
*/
private int __tableTSIDColumn = -1;

/**
TSID format, using time series %L, etc. specifiers.
*/
private String __tableTSIDFormat = null;

/**
When creating single column output, indicate whether missing values in time series should be transferred.
*/
private boolean __includeMissingValues = true;

/**
Data value columns for time series, 0+.
*/
private int [] __valueColumns = null;

/**
Data flag columns for time series, 0+.
*/
private int [] __flagColumns = null;

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
@param table Data table being filled with time series.  Column names need to have been defined but the
table is expected to be empty (no rows).
@param tslist list of time series being transferred to table.
@param dateTimeColumn date/time column (0+).
@param tableTSIDColumn name of column to contain TSID, for one-column output
@param tableTSIDFormat format of TSID corresponding to tableTSIDColumn
@param includeMissingValues indicates whether missing values should be output in single-column output (setting to
false can be advantageous when processing sparse time series)
@param valueColumns column numbers (0+) corresponding to the correct column names for the time series data values;
if a single column is output, then the first array position will be used for each time series
@param flagColumns column numbers (0+) corresponding to the correct column names for the time series data flags;
if a single column is output, then the first array position will be used for each time series
@param outputStart first date/time to be transferred (if null, output all)
@param outputEnd last date/time to be transferred (if null, output all)
@param outputWindow the window within a year to output (if null, output all)
@param useNullForMissing if true, use null in the table for missing values.  If false, transfer the
time series missing value indicator (e.g., -999 or NaN).  These values are not universally handled.
*/
public TSUtil_TimeSeriesToTable ( DataTable table, List<TS> tslist, int dateTimeColumn, int tableTSIDColumn,
    String tableTSIDFormat, boolean includeMissingValues, int [] valueColumns, int [] flagColumns,
    DateTime outputStart, DateTime outputEnd,
    DateTimeWindow outputWindow, boolean useNullForMissing )
{   __table = table;
    __tsList = tslist;
    __dateTimeColumn = dateTimeColumn;
    __tableTSIDColumn = tableTSIDColumn;
    __tableTSIDFormat = tableTSIDFormat;
    __includeMissingValues = includeMissingValues;
    __valueColumns = valueColumns;
    __flagColumns = flagColumns;
    __outputStart = outputStart;
    __outputEnd = outputEnd;
    __outputWindow = outputWindow; // Allow null to speed performance checks
    __useNullForMissing = useNullForMissing;
    // Make sure that the time series are regular and of the same interval if multi-column
    if ( (tslist.size() > 0) && (tableTSIDColumn < 0) ) {
        if ( !TSUtil.intervalsMatch(tslist) ) {
            throw new UnequalTimeIntervalException (
                "Time series don't have the same interval - cannot convert to multi-column table.");
        }
        if ( TSUtil.areAnyTimeSeriesIrregular(tslist) ) {
            throw new IrregularTimeSeriesNotSupportedException (
                "Irregular time series cannot be converted to a multi-column table.");
        }
    }
}

/**
Do the work of copying the time series into a table.  The table must already have had columns created.
*/
public void timeSeriesToTable ()
{   String routine = getClass().getName() + ".timeSeriesToTable";
    // Create a new list of problems
    __problems = new Vector<String>();

    if ( __tableTSIDColumn >= 0 ) {
        // Outputting single column table
        // Iterate through each time series and dump the values within the requested range
        TSIterator tsi = null;
        int valueColumn = __valueColumns[0];
        int flagColumn = -1;
        if ( (__flagColumns != null) && (__flagColumns.length == 1) ) {
            // Have a valid flag column
            flagColumn = __flagColumns[0];
        }
        int setRow;
        int rowCount = -1; // Will align properly when incremented below
        for ( TS ts : __tsList ) {
            if ( ts == null ) {
                continue;
            }
            try {
                tsi = ts.iterator(__outputStart,__outputEnd);
            }
            catch ( Exception e ) {
                Message.printWarning(3,routine,"Error initializing iterator for " +
                     ts.getIdentifier().toStringAliasAndTSID() + " (" + e + ")" );
                continue;
            }
            TSData tsdata;
            DateTime date;
            double value;
            String flag;
            String tsid;
            boolean isMissing;
            while ( (tsdata = tsi.next()) != null ) {
                // Set the date
                date = tsdata.getDate();
                if ( (__outputWindow != null) && !__outputWindow.isDateTimeInWindow(date) ) {
                    // Don't add the row...
                    continue;
                }
                // Check for missing
                value = tsdata.getDataValue();
                isMissing = false;
                if ( ts.isDataMissing(value) ) {
                    isMissing = true;
                }
                if ( isMissing && !__includeMissingValues ) {
                    // Don't want to include missing values
                    continue;
                }
                // Row is incremented for each value, but only after above checks
                ++rowCount;
                setRow = rowCount;
                // Set the date/time
                try {
                    __table.setFieldValue(setRow, __dateTimeColumn, new DateTime(date), true );
                }
                catch ( Exception e ) {
                    __problems.add ( "Error setting date " + date + " for row [" + setRow + "] (" + e + ").");
                }
                // Set the TSID
                if ( __tableTSIDFormat == null ) {
                    // Use the alias if available, otherwise the TSID
                    String alias = ts.getAlias();
                    if ( (alias != null) && !alias.equals("") ) {
                        tsid = alias;
                    }
                    else {
                        tsid = ts.getIdentifier().toString();
                    }
                }
                else {
                    tsid = ts.formatExtendedLegend(__tableTSIDFormat);
                }
                try {
                    __table.setFieldValue(setRow, __tableTSIDColumn, tsid, true );
                }
                catch ( Exception e ) {
                    __problems.add ( "Error setting TSID " + tsid + " for row [" + setRow + "] (" + e + ").");
                }
                // Set the data value and optionally the flag
                try {
                    if ( isMissing && __useNullForMissing ) {
                        __table.setFieldValue(setRow, valueColumn, null, true );
                    }
                    else {
                        // Set as a double because non-missing or missing and the missing value should be used
                        __table.setFieldValue(setRow, valueColumn, new Double(value), true );
                    }
                }
                catch ( Exception e ) {
                    __problems.add ( "Error setting data value " + value +
                        " at " + date + " [" + setRow + "][" + valueColumn + "] (" + e + ").");
                }
                if ( flagColumn >= 0 ) {
                    // Column has been specified for flag so output
                    flag = tsdata.getDataFlag();
                    if ( flag == null ) {
                        flag = "";
                    }
                    try {
                        __table.setFieldValue(setRow, flagColumn, flag, true );
                    }
                    catch ( Exception e ) {
                        __problems.add ( "Error setting data flag " + flag +
                            " at " + date + " [" + setRow + "][" + flagColumn + "] (" + e + ").");
                    }
                }
            }
        }
    }
    else {
        // Outputting a multi-column tables where the date/time column is used for all the time series
        // If the output start and end are not specified, use the maximum period
        DateTime outputStart = null, outputEnd = null;
        if ( (__outputStart == null) || (__outputEnd == null) ) {
            // One or more of the requested dates is null so find the full period of the data
            TSLimits limits = null;
            try {
                limits = TSUtil.getPeriodFromTS(__tsList, TSUtil.MAX_POR );
                if ( __outputStart == null ) {
                    outputStart = new DateTime(limits.getDate1());
                }
                if ( __outputEnd == null ) {
                    outputEnd = new DateTime(limits.getDate2());
                }
            }
            catch ( Exception e ) {
                // Worst case use the period from the first time series
                outputStart = __tsList.get(0).getDate1();
                outputEnd = __tsList.get(0).getDate2();
            }
        }
        else {
            outputStart = new DateTime(__outputStart);
            outputEnd = new DateTime(__outputEnd);
        }
        // Run through the table to make sure that the date/times increment without gaps
        int nrows = __table.getNumberOfRecords();
        Object o;
        DateTime dt, dtPrev = null;
        DateTime dtFirst = null; // DateTime matching dataRowFirst
        DateTime dtLast = null; // DateTime matching dataRowLast
        int outOfOrderCount = 0;
        int dataRowFirst = -1; // data data row with corresponding dtFirst
        int dataRowLast = -1; // last data row with corresponding dtLast
        int dataRowFound = -1; // data row found that matches output start
        DateTime dtWindowFirst = null;
        int intervalBase = __tsList.get(0).getDataIntervalBase();
        int intervalMult = __tsList.get(0).getDataIntervalMult();
        if ( __outputWindow != null ) {
            // It is more efficient to get the first value in the output window and then see if there is a match in the table
            dtWindowFirst = __outputWindow.getFirstMatchingDateTime(outputStart,outputEnd,intervalBase,intervalMult);
        }
        // Also figure out the first and last date from the data record block
        for ( int irow = 0; irow < nrows; irow++ ) {
            try {
                o = __table.getFieldValue(irow, __dateTimeColumn);
                if ( o == null ) {
                    dtPrev = null;
                }
                else {
                    dt = (DateTime)o;
                    if ( (__outputWindow != null) &&  !__outputWindow.isDateTimeInWindow(dt) ) {
                        // When using a window only check/match date/times that are in the window
                        continue;
                    }
                    if ( (dtPrev != null) && dtPrev.greaterThanOrEqualTo(dt) ) {
                        //Message.printWarning(3, routine, "Date " + dt + " is <= " + dtPrev );
                        ++outOfOrderCount;
                    }
                    if ( __outputWindow == null ) {
                        if ( outputStart.equals(dt) ) {
                            // Found the row in the table that matches the first time series output row
                            dataRowFound = irow;
                        }
                    }
                    else {
                        if ( (dtWindowFirst != null) && dt.equals(dtWindowFirst) ) {
                            dataRowFound = irow;
                        }
                    }
                    if ( dtFirst == null ) {
                        dtFirst = dt;
                        dataRowFirst = irow;
                    }
                    dtLast = dt; // Reset until all data are processed
                    dataRowLast = irow;
                    dtPrev = dt;
                }
            }
            catch ( Exception e ) {
                // Should not happen
            }
        }
        if ( outOfOrderCount > 0 ) {
            // TODO SAM 2014-02-01 This might be overcome in the future but for now is a reasonable check
            // Cannot continue
            __problems.add ( "Existing date/times in table are out of order.  Cannot add time series to table.  " +
                "If sorting the table, do it after all time series have been added." );
            return;
        }
        // If a data row was not found, then set it to the appropriate row, if necessary padding with blank rows before or
        // after the existing table rows to keep the time sequence intact.
        int setRow, setColumn; // Row and column for data set
        int setRowFirst = -1; // First row to set data in
        if ( dataRowFound < 0 ) {
            if ( __table.getNumberOfRecords() == 0 ) {
                // Will add rows below starting at this row
                setRowFirst = 0; // Set data at start of table
                Message.printStatus(2, routine, "Table is empty - rows will be added in order from time series.");
            }
            else {
                if ( dtFirst.greaterThan(outputStart) ) {
                    // Existing block of records in the table is AFTER at least some of data to be inserted
                    // Insert the full number of records with the date/time so that records don't get overwritten
                    // Below the time series values will be inserted to match the date/time
                    setRow = 0;
                    Message.printStatus(2, routine, "Time series is before records in existing table");
                    for ( dt = new DateTime(outputStart); dt.lessThan(dtFirst); dt.addInterval(intervalBase,intervalMult)) {
                        if ( (__outputWindow != null) && !__outputWindow.isDateTimeInWindow(dt) ) {
                            // Don't add the row...
                            continue;
                        }
                        try {
                            Message.printStatus(2, routine, "Adding blank row [" + setRow + "] at start of table for " + dt );
                            __table.insertRecord(setRow,__table.emptyRecord(),false);
                            __table.setFieldValue(setRow, __dateTimeColumn, new DateTime(dt), true );
                            ++setRow;
                        }
                        catch ( Exception e ) {
                            __problems.add ( "Unexpected error initializing blank record for date " + dt + " (" + e + ").");
                            Message.printWarning(3, routine, e);
                        }
                    }
                    setRowFirst = 0; // Set data at start of table
                }
                else if ( dtLast.lessThan(outputStart) ) {
                    // Existing block of records in the table is BEFORE the data to be inserted
                    // Insert the full number of records with the date/time to ensure alignment
                    // Below the time series values will be inserted to match the date/time
                    setRow = __table.getNumberOfRecords() - 1;
                    // Increment one to start adding after the previous table records
                    dt = new DateTime(dtLast);
                    dt.addInterval(intervalBase,intervalMult);
                    ++setRow;
                    Message.printStatus(2, routine, "Time series is after records in existing table");
                    for ( ; dt.lessThanOrEqualTo(outputEnd); dt.addInterval(intervalBase,intervalMult)) {
                        if ( (__outputWindow != null) && !__outputWindow.isDateTimeInWindow(dt) ) {
                            // Don't add the row...
                            continue;
                        }
                        try {
                            Message.printStatus(2, routine, "Adding blank row [" + setRow + "] at end of table for " + dt );
                            __table.setFieldValue(setRow, __dateTimeColumn, new DateTime(dt), true );
                            // The following check is tricky when the output window is used so check range
                            if ( (setRowFirst < 0) && dt.greaterThanOrEqualTo(outputStart) && dt.lessThanOrEqualTo(outputEnd) ) {
                                setRowFirst = setRow;
                            }
                            ++setRow;
                        }
                        catch ( Exception e ) {
                            __problems.add ( "Unexpected error initializing blank record for date " + dt + " (" + e + ").");
                            Message.printWarning(3, routine, e);
                        }
                    }
                }
            }
        }
        else {
            setRowFirst = dataRowFound;
        }
        // If here, the new time series date/times can be aligned with existing data records or added at the end with no issues
        // Iterate through the dates for the time series data
        // If a blank table is being added to, new records will be added.
        // If an existing table is being added to, existing records should be matched and values set in the existing records.
        int its; // iterator for time series
        TS ts; // time series being processed
        int tsListSize = __tsList.size();
        int rowCount = 0; // Rows processed from time series
        double value; // Data value from time series
        String flag; // Data flag from time series
        DateTime date = null;
        int flagColumn;
        TSData tsdata = new TSData();
        Message.printStatus(2, routine, "Adding time series for " + outputStart + " to " + outputEnd +
            " starting at table row [" + setRowFirst + "]");
        for (date = new DateTime(outputStart); date.lessThanOrEqualTo(outputEnd);
            date.addInterval(intervalBase,intervalMult) ) {
            if ( (__outputWindow != null) && !__outputWindow.isDateTimeInWindow(date) ) {
                // Don't add the row...
                continue;
            }
            // Set the value for the date/time and add the row if it does not exist
            // If the outputStart and outputEnd align with the table then setRow is accurate
            // if the outputStart and outputEnd do not align with the existing table, setRow has to be determined from the date/time
            setRow = setRowFirst + rowCount;
            // First try to get the date/time.  If not set, then just add.  If set, make sure it aligns with the time series
            try {
                o = __table.getFieldValue(setRow, __dateTimeColumn);
            }
            catch ( Exception e ) {
                // OK, means that no existing date/time column value exists in the table so append at end
                o = null;
            }
            if ( o == null ) {
                // Date/time was not found in the table.  Add a new record to the table and set the date/time value.
                // This should only happen for the first pass when the table was empty or append to the end of table
                // (otherwise records would have been added above)
                try {
                    if ( Message.isDebugOn ) {
                        Message.printDebug(1,routine,"Adding row for " + date );
                    }
                    __table.setFieldValue(setRow, __dateTimeColumn, new DateTime(date), true );
                }
                catch ( Exception e ) {
                    __problems.add ( "Error setting date " + date + " for col [" + __dateTimeColumn + "] row [" + setRow + "] (" + e + ").");
                    Message.printWarning(3,routine,e);
                }
            }
            else {
                // Check to make sure the date/time from the table matches what is expected
                // This should not happen with proper handling of table appends, but leave in check in case something was overlooked
                if ( !date.equals((DateTime)o) ) {
                    __problems.add ( "Existing date/time in table row [" + setRow + "] (" + o + ") does not match time series (" + date +
                         ", output start=" + outputStart + ", output end=" + outputEnd +
                         ") - table date/times are misaligned so cannot append time series value to table.");
                    break;
                }
            }
            // Iterate through the time series
            for ( its = 0; its < tsListSize; its++ ) {
                ts = __tsList.get(its);
                ts.getDataPoint(date,tsdata);
                value = tsdata.getDataValue();
                // Each time series is in a separate column
                // Set the data value...
                if ( (__valueColumns == null) || (__valueColumns[its] < 0) ) {
                    // Unable to set column so skip
                }
                else {
                    setColumn = __valueColumns[its];
                    try {
                        if ( ts.isDataMissing(value) && __useNullForMissing ) {
                            __table.setFieldValue(setRow, setColumn, null, true );
                            if ( Message.isDebugOn ) {
                                Message.printDebug(1,routine,"Set [" + setRow + "][" + setColumn + "]=null" );
                            }
                        }
                        else {
                            // Set as a double because non-missing or missing and the missing value should be used
                            __table.setFieldValue(setRow, setColumn, new Double(value), true );
                            if ( Message.isDebugOn ) {
                                Message.printDebug(1,routine,"Set [" + setRow + "][" + setColumn + "]=" + value );
                            }
                        }
                    }
                    catch ( Exception e ) {
                        __problems.add ( "Error setting data value " + value +
                            " at " + date + " [" + setRow + "][" + setColumn + "] (" + e + ").");
                    }
                }
                // Set the data flag...
                flagColumn = -1;
                if ( (__flagColumns != null) && (__flagColumns.length != 0) ) {
                    flagColumn = __flagColumns[its];
                }
                if ( flagColumn >= 0 ) {
                    // Column has been specified for flag so output
                    flag = tsdata.getDataFlag();
                    if ( flag == null ) {
                        flag = "";
                    }
                    try {
                        __table.setFieldValue(setRow, flagColumn, flag, true );
                    }
                    catch ( Exception e ) {
                        __problems.add ( "Error setting data flag " + flag +
                            " at " + date + " [" + setRow + "][" + flagColumn + "] (" + e + ").");
                    }
                }
            }
            // If here the row was added so increment for the next time increment
            ++rowCount;
        }
    }
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