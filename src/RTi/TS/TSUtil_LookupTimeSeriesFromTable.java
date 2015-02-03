package RTi.TS;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import RTi.Util.Math.DataTransformationType;
import RTi.Util.Math.MathUtil;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
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
List of problems generated by this command that are warnings, guaranteed to be non-null.
*/
private List<String> __problemsWarning = new ArrayList();

/**
List of problems generated by this command that are failures, guaranteed to be non-null.
*/
private List<String> __problemsFailure = new Vector();

/**
Date/time column for effective date 0+ (or -1 to indicate no effective date column).
*/
private int __effectiveDateColumn = -1;

/**
Input time series to process.
*/
private TS __inputTS = null;

/**
Data table for lookup.
*/
private DataTable __lookupTable = null;

/**
Indicate whether input needs to be sorted (performance hit but necessary in some cases).
*/
private boolean __sortInput = false;

/**
Output time series to process.
*/
private TS __outputTS = null;

/**
Lookup method type.
*/
private LookupMethodType __lookupMethodType = null;

/**
Out of range lookup method type.
*/
private OutOfRangeLookupMethodType __outOfRangeLookupMethodType = null;

// TODO SAM 2012-02-11 Need to make an enumeration to avoid errors, but this requires
// some additional standardization throughout
/**
Out of range notification method ("Ignore", "Warn", "Fail").
*/
private String __outOfRangeNotification = "Ignore";

/**
Lookup table column matching the input time series values, 0+.
*/
private int __value1Column = -1;

/**
Lookup table column matching the output time series values, 0+.
*/
private int __value2Column = -1;

/**
Data transformation type.
*/
private DataTransformationType __transformation = null;

/**
Data value to use when the original data value is <= 0 and a log transformation is used.
*/
private Double __leZeroLogValue = .001;

/**
Start of analysis (null to analyze all from input time series).
*/
private DateTime __analysisStart = null;

/**
End of analysis (null to analyze all from input time series).
*/
private DateTime __analysisEnd = null;

/**
Window within the year to transfer data values.
*/
private DateTimeWindow __analysisWindow = null;

/**
Constructor.
@param inputTS input time series used for lookup
@param outputTS output time series used for lookup
@param lookupTable Data table being filled with time series.  Column names need to have been defined but the
table is expected to be empty (no rows)
@param value1Column the column matching the input time series values for the lookup (0+)
@param sortInput indicate whether input column should be sorted (performance hit but necessary sometimes)
@param value2Column the column matching the output time series values for the lookup (0+)
@param effectiveDateColumn date/time column (0+)
@param lookupMethodType the lookup method type to use (if null, use INTERPOLATE)
@param outOfRangeLookupMethodType the lookup method for out of range values (if null use SET_MISSING)
@param outOfRangeNotification the type of notification when out of range values are estimated ("Ignore", "Warn",
or "Fail"); if null use "Ignore"
@param transformation how to transform the data before the lookup (if null use NONE)
@param leZeroLogValue when using a log transformation,
the value to use for data when the original data value is <=0 (if null use .001).
@param analysisStart first date/time to be transferred (if null, process input time series period)
@param analysisEnd last date/time to be transferred (if null, process input time series period)
@param analysisWindow window within a year to process (year generally set to 2000).
*/
public TSUtil_LookupTimeSeriesFromTable ( TS inputTS, TS outputTS, DataTable lookupTable, int value1Column, boolean sortInput,
    int value2Column, int effectiveDateColumn, LookupMethodType lookupMethodType,
    OutOfRangeLookupMethodType outOfRangeLookupMethodType, String outOfRangeNotification,
    DataTransformationType transformation, Double leZeroLogValue,
    DateTime analysisStart, DateTime analysisEnd, DateTimeWindow analysisWindow )
{   //String message;
    //String routine = getClass().getName() + ".constructor";
	// Save data members.
    if ( inputTS == null ) {
        throw new InvalidParameterException ( "Input time series is null." );
    }
    __inputTS = inputTS;
    if ( outputTS == null ) {
        throw new InvalidParameterException ( "Output time series is null." );
    }
    __outputTS = outputTS;
    if ( lookupTable == null ) {
        throw new InvalidParameterException ( "Lookup table is null." );
    }
    __lookupTable = lookupTable;
    if ( lookupTable.getNumberOfRecords() < 2  ) {
        throw new InvalidParameterException ( "Lookup table must have at lest 2 rows." );
    }
    if ( (value1Column < 0) || (value1Column >= lookupTable.getNumberOfFields())  ) {
        throw new InvalidParameterException ( "Value1 column (" + value1Column +
             ") is < 0 or < number of table columns - 1 (0+ indexm " + lookupTable.getNumberOfFields() + ")." );
    }
    __value1Column = value1Column;
    if ( (value2Column < 0) || (value2Column >= lookupTable.getNumberOfFields())  ) {
        throw new InvalidParameterException ( "Value2 column (" + value2Column +
            ") is < 0 or < number of table columns - 1 (0+ index, " + lookupTable.getNumberOfFields() + ")." );
    }
    __sortInput = sortInput;
    __value2Column = value2Column;
    if ( effectiveDateColumn >= 0 ) {
        throw new InvalidParameterException ( "Effective date column is not yet supported - future enhancement." );
    }
    __effectiveDateColumn = effectiveDateColumn;
    if ( lookupMethodType == null ) {
        lookupMethodType = LookupMethodType.INTERPOLATE;
    }
    __lookupMethodType = lookupMethodType;
    if ( outOfRangeLookupMethodType == null ) {
        outOfRangeLookupMethodType = OutOfRangeLookupMethodType.SET_MISSING;
    }
    __outOfRangeLookupMethodType = outOfRangeLookupMethodType;
    if ( outOfRangeNotification == null ) {
        outOfRangeNotification = "Ignore";
    }
    __outOfRangeNotification = outOfRangeNotification;
    if ( transformation == null ) {
        transformation = DataTransformationType.NONE;
    }
    __transformation = transformation;
    if ( leZeroLogValue == null ) {
        leZeroLogValue = new Double(.001);
    }
    __leZeroLogValue = leZeroLogValue;
    __analysisStart = analysisStart;
    __analysisEnd = analysisEnd;
    __analysisWindow = analysisWindow;
    // Make sure that the time series are regular and of the same interval
    if ( !TSUtil.intervalsMatch(inputTS, outputTS) ) {
        // TODO SAM 2012-02-10 Might be able to relax this constraint in the future
        throw new UnequalTimeIntervalException (
            "Time series don't have the same interval - cannot perform lookup.");
    }
}

/**
Determine whether the lookup table is sorted by the lookup value.
Any nulls in data will result in a zero.
@param lookupTable the lookup table
@param value1Column the column number for the lookup values (0+)
@return -1 if sorted descending (row 0=maximum), 1 (row 0=minimum), or zero (not sorted)
*/
private int checkLookupTableSortedAndNonNull ( DataTable lookupTable, int value1Column )
{
    double value, valuePrev;
    try {
        int countDescend = 0;
        int countAscend = 0;
        boolean isNaN;
        valuePrev = getTableCellDouble(lookupTable, 0, value1Column);
        int nRows = lookupTable.getNumberOfRecords();
        for ( int iRow = 1; iRow < nRows; iRow++ ) {
            value = getTableCellDouble(lookupTable,iRow, value1Column);
            isNaN = Double.isNaN(value);
            if ( isNaN ) {
                // TODO SAM 2014-01-21 evaluate what to do but for now just skip - won't be able to do lookup later
                ++countDescend;
                ++countAscend;
                //return 0;
            }
            if ( value <= valuePrev ) {
                ++countDescend;
            }
            else if ( value >= valuePrev ) {
                ++countAscend;
            }
            if ( !isNaN ) {
                valuePrev = value;
            }
        }
        Message.printStatus(2,"","countAscend=" + countAscend + " countDescend=" + countDescend );
        if ( (countDescend + 1) == nRows ) {
            return -1;
        }
        else if ( (countAscend + 1) == nRows ) {
            return 1;
        }
        else {
            return 0;
        }
    }
    catch ( Exception e ) {
        // For getFieldValue exceptions
        return 0;
    }
}

// TODO SAM 2012-02-11 Need to handle the effective date and possibly shift
/**
Return the lookup table.
@return the lookup table.
*/
private DataTable getLookupTable ()
{
    return __lookupTable;
}

/**
Return the lookup table considering the effective date.
@return the lookup table considering the effective date
@param fullLookupTable the full lookup table, which may have multiple effective dates
@param lookupTablePrev previous lookup table, which may still be appropriate
@param date the date for which the lookup table is being retrieved
@param effectiveDateColumn the column in the full table that contains the effective date (0+)
*/
private DataTable getLookupTableForEffectiveDate ( DataTable fullLookupTable,
    DataTable lookupTablePrev, DateTime date, int effectiveDateColumn )
{
    // For now always return the full lookup table since effective date is not supported
    // TODO SAM 2012-02-11 Need to enable effective date
    return fullLookupTable;
}

/**
Return a list of problems for the time series, failure messages.
@return a list of problems for the time series, failure messages.
*/
public List<String> getProblemsFailure ()
{
    return __problemsFailure;
}

/**
Return a list of problems for the time series, warning messages.
@return a list of problems for the time series, warning messages.
*/
public List<String> getProblemsWarning ()
{
    return __problemsWarning;
}

/**
Return the value of a lookup table cell as a double.
This is needed because sometimes the lookup table column contains integers.
@return the value of a lookup table cell as a double, or NaN if not a number.
@param lookupTable the lookup table being processed
@param iRow the row being accessed
@param iColumn the column being accessed
*/
private double getTableCellDouble(DataTable lookupTable, int iRow, int iColumn )
{   Object o;
    try {
        o = lookupTable.getFieldValue(iRow, iColumn);
    }
    catch ( Exception e ) {
        return Double.NaN;
    }
    if ( o == null ) {
        return Double.NaN;
    }
    else if ( o instanceof Double ) {
        return (Double)o;
    }
    else if ( o instanceof Float ) {
        return (Float)o;
    }
    else if ( o instanceof Integer ) {
        return (double)(Integer)o;
    }
    else if ( o instanceof Short ) {
        return (double)(Short)o;
    }
    else {
        return Double.NaN;
    }
}

/**
Lookup the last row for the input value that is less than or equal to the value being looked up.  This provides the lower bound.
@param lookupTable the lookup table
@param lookupOrder the order of the lookup column (-1=descending, row 0 has max value; 1=ascending, row 0 has min value)
@param value1Column the column to use for the lookup (0+)
@param inputValue the value being looked up
*/
private int lookupFloorRow ( DataTable lookupTable, int lookupOrder, int value1Column, double inputValue )
{   double value;
    if ( lookupOrder == 1 ) {
        // Ascending - start at last row and search up
        for ( int iRow = lookupTable.getNumberOfRecords() - 1; iRow >= 0; iRow-- ) {
            try {
                value = getTableCellDouble(lookupTable, iRow, value1Column);
                if ( Double.isNaN(value) ) {
                    return -1;
                }
                if ( value <= inputValue ) {
                    return iRow;
                }
            }
            catch ( Exception e ) {
                return -1;
            }
        }
    }
    else if ( lookupOrder == -1 ) {
        // Descending
        // Ascending - start at last row and search up
        for ( int iRow = lookupTable.getNumberOfRecords() - 1; iRow >= 0; iRow-- ) {
            try {
                value = getTableCellDouble(lookupTable, iRow, value1Column);
                if ( Double.isNaN(value) ) {
                    return -1;
                }
                if ( value <= inputValue ) {
                    return iRow;
                }
            }
            catch ( Exception e ) {
                return -1;
            }
        }
    }
    return -1;
}

/**
Set the output time series values by looking up from the time series and table.
*/
public void lookupTimeSeriesValuesFromTable ()
{   String message = "", routine = getClass().getSimpleName() + ".lookupTimeSeriesValuesFromTable";
    // Create a new list of problems
    __problemsWarning = new ArrayList();
    
    // If the output start and end are not specified, use the period from the input time series
    DateTime analysisStart = null, analysisEnd = null;
    if ( __analysisStart == null ) {
        analysisStart = new DateTime(__inputTS.getDate1());
    }
    else {
        analysisStart = new DateTime(__analysisStart);
    }
    if ( __analysisEnd == null ) {
        analysisEnd = new DateTime(__inputTS.getDate2());
    }
    else {
        analysisEnd = new DateTime(__analysisEnd);
    }
    DateTimeWindow analysisWindow = __analysisWindow;
    
    TS inputTS = __inputTS;
    TS outputTS = __outputTS;
    TSIterator tsi = null;
    try {
        tsi = inputTS.iterator(analysisStart, analysisEnd);
    }
    catch ( Exception e ) {
        // Should not happen
        throw new RuntimeException ( "Error creating iterator (" + e + ").");
    }
    DateTime date = null;
    double inputValue, outputValue = 0.0;
    TSData tsdata;
    DataTable fullLookupTable = getLookupTable(); // TODO SAM 2012-02-11 Need to handle effectiveDate and return lookupOrder
    DataTable lookupTable = fullLookupTable; // Look table for the effective date (initialize different from lookupTablePrev)
    DataTable lookupTablePrev = null; // The lookup table from the previous date
    int lookupOrder = 0; // The order of the table lookup column
    LookupMethodType lookupMethodType = __lookupMethodType;
    OutOfRangeLookupMethodType outOfRangelookupMethodType = __outOfRangeLookupMethodType;
    String outOfRangeNotification = __outOfRangeNotification;
    boolean outOfRangeNotifyWarn = false; // Default is to ignore out of range
    boolean outOfRangeNotifyFail = false;
    if ( outOfRangeNotification.toUpperCase().indexOf("WARN") >= 0 ) {
        outOfRangeNotifyWarn = true;
    }
    if ( outOfRangeNotification.toUpperCase().indexOf("FAIL") >= 0 ) {
        outOfRangeNotifyFail = true;
    }
    int value1Column = __value1Column;
    boolean sortInput = __sortInput;
    int value2Column = __value2Column;
    int effectiveDateColumn = __effectiveDateColumn;
    DataTransformationType transformation = __transformation;
    double leZeroLogValue = __leZeroLogValue;
    List<String> problemsWarning = __problemsWarning;
    List<String> problemsFailure = __problemsFailure;
    double inputValueMin = 0;
    double inputValueMax = 0;
    double outputValueMin = 0;
    double outputValueMax = 0;
    double inputValue1 = 0, inputValue2 = 0, outputValue1 = 0, outputValue2 = 0;
    int lowRow = 0; // The lookupTable row that has a value <= to the input value
    int highRow = 0; // The lookupTable row that has a value >= the input value
    double missing = outputTS.getMissing();
    boolean canSetOutput = false;
    int nRows = 0; // Number of rows in the lookup table
    int nRowsM1 = 0, nRowsM2 = 0;
    int setCount = 0;
    while ( (tsdata = tsi.next()) != null ) {
        date = tsi.getDate();
        if ( (analysisWindow != null) && !analysisWindow.isDateTimeInWindow(date) ) {
            // Date is not in window so don't process the date...
            continue;
        }
        // Get the lookup table for the current date (might re-use the previous lookup)
        lookupTable = getLookupTableForEffectiveDate ( fullLookupTable, lookupTablePrev, date, effectiveDateColumn );
        if ( lookupTable != lookupTablePrev ) {
            if ( sortInput ) {
                lookupOrder = 1;
                lookupTable = sortTable ( lookupTable, value1Column, lookupOrder );
            }
            else {
                lookupOrder = checkLookupTableSortedAndNonNull(lookupTable, value1Column);
            }
            if ( lookupOrder == 0 ) {
                throw new RuntimeException ( "Lookup table cannot be sorted." );
            }
            // Need to get some information about the table for further calculations
            nRows = lookupTable.getNumberOfRecords();
            Message.printStatus(2,routine,"Lookup table is sorted in order " + lookupOrder + " and has " + nRows + " rows");
            nRowsM1 = nRows - 1;
            nRowsM2 = nRows - 2;
            try {
                if ( lookupOrder == 1 ) {
                    // Ascending
                    inputValueMin = getTableCellDouble(lookupTable, 0, value1Column);
                    inputValueMax = getTableCellDouble(lookupTable, nRowsM1, value1Column);
                    outputValueMin = getTableCellDouble(lookupTable, 0, value2Column);
                    outputValueMax = getTableCellDouble(lookupTable, nRowsM1, value2Column);
                }
                else {
                    // Descending
                    inputValueMin = getTableCellDouble(lookupTable, nRowsM1, value1Column);
                    inputValueMax = getTableCellDouble(lookupTable, 0, value1Column);
                    outputValueMin = getTableCellDouble(lookupTable, nRowsM1, value2Column);
                    outputValueMax = getTableCellDouble(lookupTable, 0, value2Column);
                }
            }
            catch ( Exception e ) {
                throw new RuntimeException ( "Error looking up extreme values in lookup table." );
            }
        }
        // Save here in case there is a jump in logic
        lookupTablePrev = lookupTable;
        inputValue = tsdata.getDataValue();
        canSetOutput = true; // May not be able to compute
        if ( inputTS.isDataMissing(inputValue) ) {
            // Can't process value
            canSetOutput = false;
        }
        else {
            // Have an input value to look up.
            // Some of this code is inlined - it is easier to understand the logic this way than having
            // complicated "if" statements or calling other methods to perform basic processing
            if ( inputValue < inputValueMin ) {
                if ( outOfRangelookupMethodType == OutOfRangeLookupMethodType.SET_MISSING ) {
                    outputValue = missing;
                }
                else if ( outOfRangelookupMethodType == OutOfRangeLookupMethodType.USE_END_VALUE ) {
                    outputValue = outputValueMin;
                }
                else if ( outOfRangelookupMethodType == OutOfRangeLookupMethodType.EXTRAPOLATE ) {
                    try {
                        if ( lookupOrder == 1 ) {
                            // Ascending so smallest values in row 0 - point the extrapolation past the min
                            inputValue1 = getTableCellDouble(lookupTable, 1, value1Column);
                            inputValue2 = getTableCellDouble(lookupTable, 0, value1Column);
                            outputValue1 = getTableCellDouble(lookupTable, 1, value2Column);
                            outputValue2 = getTableCellDouble(lookupTable, 0, value2Column);
                        }
                        else {
                            // Descending so smallest values at end of table - point the extrapolation past the max
                            inputValue1 = getTableCellDouble(lookupTable, nRowsM2, value1Column);
                            inputValue2 = getTableCellDouble(lookupTable, nRowsM1, value1Column);
                            outputValue1 = getTableCellDouble(lookupTable, nRowsM2, value2Column);
                            outputValue2 = getTableCellDouble(lookupTable, nRowsM1, value2Column);
                        }
                    }
                    catch ( Exception e ) {
                        // Should not happen
                        problemsWarning.add ( "Error looking up values from table for " + date + " value=" + inputValue );
                        canSetOutput = false;
                    }
                    if ( inputTS.isDataMissing(inputValue1) || inputTS.isDataMissing(inputValue1) ||
                        inputTS.isDataMissing(inputValue1) || inputTS.isDataMissing(inputValue1) ) {
                        canSetOutput = false;
                    }
                    else {
                        if ( transformation == DataTransformationType.LOG ) {
                            if ( inputValue <= 0 ) {
                                inputValue = leZeroLogValue;
                            }
                            inputValue = Math.log10(inputValue);
                            if ( inputValue1 <= 0 ) {
                                inputValue1 = leZeroLogValue;
                            }
                            inputValue1 = Math.log10(inputValue1);
                            if ( inputValue2 <= 0 ) {
                                inputValue2 = leZeroLogValue;
                            }
                            inputValue2 = Math.log10(inputValue2);
                            if ( outputValue1 <= 0 ) {
                                outputValue1 = leZeroLogValue;
                            }
                            outputValue1 = Math.log10(outputValue1);
                            if ( outputValue2 <= 0 ) {
                                outputValue2 = leZeroLogValue;
                            }
                            outputValue2 = Math.log10(outputValue2);
                        }
                        outputValue = MathUtil.interpolate(inputValue, inputValue1, inputValue2, outputValue1, outputValue2);
                        if ( transformation == DataTransformationType.LOG ) {
                            // Convert back to normal value
                            outputValue = Math.pow(10.0, outputValue);
                        }
                    }
                }
                // Check the notification, only when output is actually computed
                if ( canSetOutput ) {
                    message = "Lookup value " + StringUtil.formatString(inputValue, "%.6f") +
                    " for " + date + " is less than minimum lookup table value " +
                    StringUtil.formatString(inputValueMin, "%.6f") + " - setting output to " +
                    StringUtil.formatString(outputValue, "%.6f");
                    if ( outOfRangeNotifyWarn ) {
                        problemsWarning.add ( message );
                    }
                    else if ( outOfRangeNotifyFail ) {
                        problemsFailure.add ( message );
                    }
                }
            }
            else if ( inputValue > inputValueMax ) {
                if ( outOfRangelookupMethodType == OutOfRangeLookupMethodType.SET_MISSING ) {
                    outputValue = missing;
                }
                else if ( outOfRangelookupMethodType == OutOfRangeLookupMethodType.USE_END_VALUE ) {
                    outputValue = outputValueMax;
                }
                else if ( outOfRangelookupMethodType == OutOfRangeLookupMethodType.EXTRAPOLATE ) {
                    try {
                        if ( lookupOrder == 1 ) {
                            // Ascending, max value at end of table, point extrapolation past end
                            inputValue1 = getTableCellDouble(lookupTable, nRows - 2, value1Column);
                            inputValue2 = getTableCellDouble(lookupTable, nRows - 1, value1Column);
                            outputValue1 = getTableCellDouble(lookupTable, nRows - 2, value2Column);
                            outputValue2 = getTableCellDouble(lookupTable, nRows - 1, value2Column);
                        }
                        else {
                            // Descending, max value in row 0, point extrapolation past row 0
                            inputValue1 = getTableCellDouble(lookupTable, 1, value1Column);
                            inputValue2 = getTableCellDouble(lookupTable, 0, value1Column);
                            outputValue1 = getTableCellDouble(lookupTable, 1, value2Column);
                            outputValue2 = getTableCellDouble(lookupTable, 0, value2Column);
                        }
                    }
                    catch ( Exception e ) {
                        // Should not happen
                        problemsWarning.add ( "Error looking up values from table for " + date + " value=" + inputValue );
                        canSetOutput = false;
                    }
                    if ( inputTS.isDataMissing(inputValue1) || inputTS.isDataMissing(inputValue1) ||
                        inputTS.isDataMissing(inputValue1) || inputTS.isDataMissing(inputValue1) ) {
                        canSetOutput = false;
                    }
                    else {
                        if ( transformation == DataTransformationType.LOG ) {
                            if ( inputValue <= 0 ) {
                                inputValue = leZeroLogValue;
                            }
                            inputValue = Math.log10(inputValue);
                            if ( inputValue1 <= 0 ) {
                                inputValue1 = leZeroLogValue;
                            }
                            inputValue1 = Math.log10(inputValue1);
                            if ( inputValue2 <= 0 ) {
                                inputValue2 = leZeroLogValue;
                            }
                            inputValue2 = Math.log10(inputValue2);
                            if ( outputValue1 <= 0 ) {
                                outputValue1 = leZeroLogValue;
                            }
                            outputValue1 = Math.log10(outputValue1);
                            if ( outputValue2 <= 0 ) {
                                outputValue2 = leZeroLogValue;
                            }
                            outputValue2 = Math.log10(outputValue2);
                        }
                        outputValue = MathUtil.interpolate(inputValue, inputValue1, inputValue2, outputValue1, outputValue2);
                        if ( transformation == DataTransformationType.LOG ) {
                            // Convert back to normal value
                            outputValue = Math.pow(10.0, outputValue);
                        }
                    }
                }
                // Check the notification, only when output is actually computed
                if ( canSetOutput ) {
                    if ( outOfRangeNotifyWarn || outOfRangeNotifyFail ) {
                        message = "Lookup value " + StringUtil.formatString(inputValue, "%.6f") +
                        " for " + date + " is greater than maximum lookup table value " +
                        StringUtil.formatString(inputValueMax, "%.6f") + " - setting output to " +
                        StringUtil.formatString(outputValue, "%.6f");
                    }
                    if ( outOfRangeNotifyWarn ) {
                        problemsWarning.add ( message );
                    }
                    else if ( outOfRangeNotifyFail ) {
                        problemsFailure.add ( message );
                    }
                }
            }
            else {
                // In the range of values so find the value to interpolate
                lowRow = lookupFloorRow ( lookupTable, lookupOrder, value1Column, inputValue );
                highRow = lowRow + 1;
                try {
                    inputValue1 = getTableCellDouble(lookupTable, lowRow, value1Column);
                    inputValue2 = getTableCellDouble(lookupTable, highRow, value1Column);
                    outputValue1 = getTableCellDouble(lookupTable, lowRow, value2Column);
                    outputValue2 = getTableCellDouble(lookupTable, highRow, value2Column);
                }
                catch ( Exception e ) {
                    // Should not happen
                    problemsWarning.add ( "Error looking up values from table for " + date + " value=" + inputValue );
                    canSetOutput = false;
                }
                if ( ((lookupMethodType == LookupMethodType.PREVIOUS_VALUE) ||
                    (lookupMethodType == LookupMethodType.INTERPOLATE)) &&
                    (inputTS.isDataMissing(inputValue1) || inputTS.isDataMissing(inputValue1)) ) {
                    canSetOutput = false;
                }
                if ( ((lookupMethodType == LookupMethodType.NEXT_VALUE) ||
                    (lookupMethodType == LookupMethodType.INTERPOLATE)) &&
                    (outputTS.isDataMissing(outputValue1) || outputTS.isDataMissing(outputValue2)) ) {
                    canSetOutput = false;
                }
                if ( !canSetOutput ) {
                    // Nothing to do
                }
                if ( inputValue == inputValue1 ) {
                    // Value is exactly on a lookup table value.  Need to handle special
                    // Regardless of the lookup method, set to the output value
                    outputValue = outputValue1;
                }
                else if ( lookupMethodType == LookupMethodType.INTERPOLATE ) {
                    if ( transformation == DataTransformationType.LOG ) {
                        if ( inputValue <= 0 ) {
                            inputValue = leZeroLogValue;
                        }
                        inputValue = Math.log10(inputValue);
                        if ( inputValue1 <= 0 ) {
                            inputValue1 = leZeroLogValue;
                        }
                        inputValue1 = Math.log10(inputValue1);
                        if ( inputValue2 <= 0 ) {
                            inputValue2 = leZeroLogValue;
                        }
                        inputValue2 = Math.log10(inputValue2);
                        if ( outputValue1 <= 0 ) {
                            outputValue1 = leZeroLogValue;
                        }
                        outputValue1 = Math.log10(outputValue1);
                        if ( outputValue2 <= 0 ) {
                            outputValue2 = leZeroLogValue;
                        }
                        outputValue2 = Math.log10(outputValue2);
                    }
                    outputValue = MathUtil.interpolate(inputValue, inputValue1, inputValue2, outputValue1, outputValue2);
                    if ( transformation == DataTransformationType.LOG ) {
                        // Convert back to normal value
                        outputValue = Math.pow(10.0, outputValue);
                    }
                }
                else if ( lookupMethodType == LookupMethodType.PREVIOUS_VALUE ) {
                    outputValue = outputValue1;
                }
                else if ( lookupMethodType == LookupMethodType.NEXT_VALUE ) {
                    outputValue = outputValue2;
                }
            }
        }
        if ( canSetOutput ) {
            ++setCount;
            Message.printStatus(2,routine,"Looked up time series value " + outputValue + " from " + inputValue + " for " + date );
            outputTS.setDataValue(date, outputValue);
        }
        else {
            // Can't compute output.  However, set the output to missing
            ++setCount;
            outputTS.setDataValue(date, missing);
        }
    }
    outputTS.addToGenesis("Set " + setCount + " values from lookup table \"" + lookupTable.getTableID() +
        "\" and input time series \"" + inputTS.getIdentifierString() +
        "\" in period " + analysisStart + " to " + analysisEnd );
}

/**
Sort the lookup table by the input column.
*/
private DataTable sortTable ( DataTable table, int sortCol, int sortOrder )
{
    // Do not want to sort the original table.  Consequently copy the table and then sort
    DataTable tableSorted = table.createCopy(table, table.getTableID() + "-sorted", null, null, null, null, null);
    // Sort the table
    String [] sortCols = new String[1];
    sortCols[0] = table.getFieldName(sortCol);
    int [] sortOrderArray = new int[1];
    sortOrderArray[0] = sortOrder;
    tableSorted.sortTable(sortCols,sortOrderArray);
    return tableSorted;
}

}