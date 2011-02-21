package RTi.TS;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.Vector;

import RTi.Util.Math.MathUtil;
import RTi.Util.Message.Message;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.TimeUtil;

/**
Create a new time series that is a running statistic of values from the input time series.
*/
public class TSUtil_RunningStatistic
{
    
/**
Time series to process.
*/
private TS __ts = null;

/**
Sample type.
*/
private RunningAverageType __SampleType = null;

/**
Statistic.
*/
private TSStatisticType __statistic = null;

/**
Bracket or N for N-year running statistic, as per the running average type.
*/
private int __n;

/**
Construct the object and check for valid input.
@param ts regular-interval time series for which to create the running average time series
@param n N for N-year running statistic and otherwise the bracket for centered,
previous, and future running statistics
@param statisticType statistic to compute
@param sampleType type of data sampling for statistic
*/
public TSUtil_RunningStatistic ( TS ts, int n, TSStatisticType statisticType, RunningAverageType sampleType )
{   String message;
    String routine = getClass().getName();

    if ( ts == null ) {
        message = "Input time series is null.";
        Message.printWarning ( 2, routine, message );
        throw new InvalidParameterException ( message );
    }
    
    if ( ts.getDataIntervalBase() == TimeInterval.IRREGULAR ) {
        message = "Converting irregular time series to running statistic is not supported.";
        Message.printWarning ( 2, routine, message );
        throw new IrregularTimeSeriesNotSupportedException ( message );
    }

    boolean found = false;
    for ( TSStatisticType s : getStatisticChoices() ) {
        if ( s == statisticType ) {
            found = true;
            break;
        }
    }
    if ( !found ) {
        message = "Statistic \"" + statisticType + "\" is not supported.";
        Message.printWarning ( 2, routine, message );
        throw new InvalidParameterException ( message );
    }

    found = false;
    for ( RunningAverageType t : getRunningAverageTypeChoices() ) {
        if ( t == sampleType ) {
            found = true;
            break;
        }
    }
    if ( !found ) {
        message = "Sample type type \"" + sampleType + "\" is not supported.";
        Message.printWarning ( 2, routine, message );
        throw new InvalidParameterException ( message );
    }
    
    setTS ( ts );
    setN ( n );
    setStatisticType ( statisticType );
    setSampleType ( sampleType );
}

/**
Return the N-year N or bracket.
*/
public int getN ()
{
    return __n;
}

/**
Return the running average type.
*/
public RunningAverageType getSampleType ()
{
    return __SampleType;
}

/**
Return the running average type.
*/
public TSStatisticType getStatisticType ()
{
    return __statistic;
}

/**
Return the running average types that are supported by the class.
*/
public static RunningAverageType[] getRunningAverageTypeChoices ()
{
    RunningAverageType[] types = {
        RunningAverageType.CENTERED,
        RunningAverageType.FUTURE,
        RunningAverageType.FUTURE_INCLUSIVE,
        RunningAverageType.NYEAR,
        RunningAverageType.N_ALL_YEAR,
        RunningAverageType.PREVIOUS,
        RunningAverageType.PREVIOUS_INCLUSIVE
    };
    return types;
}

/**
Get the list of statistics that can be performed.
*/
public static List<TSStatisticType> getStatisticChoices()
{
    // Enable statistics that illustrate how things change over time
    List<TSStatisticType> choices = new Vector();
    choices.add ( TSStatisticType.LAG1_AUTO_CORRELATION );
    choices.add ( TSStatisticType.MAX );
    choices.add ( TSStatisticType.MEAN );
    choices.add ( TSStatisticType.MEDIAN );
    choices.add ( TSStatisticType.MIN );
    choices.add ( TSStatisticType.SKEW );
    choices.add ( TSStatisticType.STD_DEV );
    choices.add ( TSStatisticType.VARIANCE );
    return choices;
}

/**
Get the list of statistics that can be performed.
@return the statistic display names as strings.
*/
public static List<String> getStatisticChoicesAsStrings()
{
    List<TSStatisticType> choices = getStatisticChoices();
    List<String> stringChoices = new Vector();
    for ( int i = 0; i < choices.size(); i++ ) {
        stringChoices.add ( "" + choices.get(i) );
    }
    return stringChoices;
}

/**
Return the input time series being processed.
*/
public TS getTS ()
{
    return __ts;
}
    
/**
Create a running average time series where the time series value is the
average of 1 or more values from the original time series.  The description is
appended with ", centered [N] running average" or ", N-year running average", etc.
@return The new running statistic time series, which is a copy of the original metadata
but with data being the running statistic.
@exception RTi.TS.TSException if there is a problem creating and filling the new time series.
*/
public TS runningStatistic ( boolean createData )
throws TSException, IrregularTimeSeriesNotSupportedException
{   String  genesis = "", message, routine = getClass().getName() + ".runningStatistic";
    TS newts = null;

    TS ts = getTS();
    TSStatisticType statisticType = getStatisticType();
    RunningAverageType sampleType = getSampleType();
    int n = getN();
  
    if ( sampleType == RunningAverageType.NYEAR ) {
        if ( n <= 1 ) {
            // Just return the original time series...
            return ts;
        }
    }
    else if ( (sampleType != RunningAverageType.N_ALL_YEAR) && (n == 0) ) {
        // Just return the original time series...
        return ts;
    }

    // Get a new time series of the proper type...

    int intervalBase = ts.getDataIntervalBase();
    int intervalMult = ts.getDataIntervalMult();
    String newinterval = "" + intervalMult + TimeInterval.getName(intervalBase);
    try {
        newts = TSUtil.newTimeSeries ( newinterval, false );
    }
    catch ( Exception e ) {
        message = "Unable to create new time series of interval \"" + newinterval + "\"";
        Message.printWarning ( 3, routine, message );
        throw new RuntimeException ( message );
    }
    newts.copyHeader ( ts );
    // Set the data type in the new time series to reflect the running statistic
    String statString = "" + statisticType;
    if ( sampleType == RunningAverageType.NYEAR ) {
        statString = "" + n + statisticType;
    }
    newts.setDataType(ts.getDataType() + "-Running-" + statString );
    newts.setDate1 ( ts.getDate1() );
    newts.setDate2 ( ts.getDate2() );
    if ( (statisticType == TSStatisticType.LAG1_AUTO_CORRELATION) ||
        (statisticType == TSStatisticType.SKEW) ) {
        newts.setDataUnits ( "" );
    }
    else if ( statisticType == TSStatisticType.VARIANCE ) {
        String units = ts.getDataUnits();
        newts.setDataUnits ( units + "^2" );
    }
    
    if ( createData ) {
        // Actually create the data (otherwise only the header information is populated)
        newts.allocateDataSpace();
    
        // Set the offsets for getting data around the current date/time
    
        int neededCount = 0; // Used initially to size the sample array
        int offset1 = 0;
        int offset2 = 0;
        if ( sampleType == RunningAverageType.N_ALL_YEAR ) {
            genesis = "NAll-year";
            neededCount = newts.getDate2().getYear() - newts.getDate1().getYear() + 1;
        }
        else if ( sampleType == RunningAverageType.CENTERED ) {
            genesis = "bracket=" + n + " centered";
            // Offset brackets the date...
            offset1 = -1*n;
            offset2 = n;
            neededCount = n*2 + 1;
        }
        else if ( sampleType == RunningAverageType.FUTURE ) {
            genesis = "bracket=" + n + " future (not inclusive)";
            // Offset brackets the date...
            offset1 = 1;
            offset2 = n;
            neededCount = n;
        }
        else if ( sampleType == RunningAverageType.FUTURE_INCLUSIVE ) {
            genesis = "bracket=" + n + " future (inclusive)";
            // Offset brackets the date...
            offset1 = 0;
            offset2 = n;
            neededCount = n + 1;
        }
        else if ( sampleType == RunningAverageType.NYEAR ) {
            genesis = n + "-year";
            // Offset is to the left but remember to include the time step itself...
            offset1 = -1*(n - 1);
            offset2 = 0;
            neededCount = n;
        }
        else if ( sampleType == RunningAverageType.PREVIOUS ) {
            genesis = "bracket=" + n + " previous (not inclusive)";
            // Offset brackets the date...
            offset1 = -n;
            offset2 = -1;
            neededCount = n;
        }
        else if ( sampleType == RunningAverageType.PREVIOUS_INCLUSIVE ) {
            genesis = "bracket=" + n + " previous (inclusive)";
            // Offset brackets the date...
            offset1 = -n;
            offset2 = 0;
            neededCount = n + 1;
        }
        
        // Size the sample array (count will be <= the max and control the calculations)
        double [] sampleArray = new double[neededCount];
        
        // Iterate through the full period of the output time series
    
        DateTime date = new DateTime ( ts.getDate1() );
        DateTime end = new DateTime ( ts.getDate2() );
        DateTime valueDateTime = new DateTime(newts.getDate1());  // Used to access data values for statistic
        int count, i;
        double value = 0.0;
        double missing = ts.getMissing();
        boolean doCalc = true;
        for ( ; date.lessThanOrEqualTo( end ); date.addInterval(intervalBase, intervalMult) ) {
            // Initialize the date for looking up values to the initial offset from the loop date...
            valueDateTime.setDate ( date );
            // Offset from the current date/time to the start of the bracket
            if ( sampleType == RunningAverageType.NYEAR ) {
                valueDateTime.addInterval ( TimeInterval.YEAR, offset1 );
            }
            else if ( sampleType == RunningAverageType.N_ALL_YEAR ) {
                // Reset to the start of the period and set the offsets to process the start year to the
                // current year
                valueDateTime.setYear ( newts.getDate1().getYear() );
                if ( valueDateTime.lessThan(newts.getDate1())) {
                    // Has wrapped around since the first date/time was not the start of a year so add another year
                    valueDateTime.addYear(1);
                }
                offset1 = valueDateTime.getYear();
                offset2 = date.getYear();
            }
            else {
                valueDateTime.addInterval ( intervalBase, offset1*intervalMult );
            }
            // Now loop through the intervals in the bracket and get the sample set...
            count = 0;
            for ( i = offset1; i <= offset2; i++ ) {
                // This check should fail harmlessly if dealing with intervals greater than a day
                if ( (valueDateTime.getMonth() == 2) && (valueDateTime.getDay() == 29) &&
                    !TimeUtil.isLeapYear(valueDateTime.getYear()) ) {
                    // The Feb 29 that we are requesting in another year does not exist.  Set to missing
                    // This will result in the final output also being missing.
                    value = missing;
                }
                else {
                    // Normal data access.
                    value = ts.getDataValue ( valueDateTime );
                }
                if ( ts.isDataMissing(value) ) {
                    if ( sampleType != RunningAverageType.N_ALL_YEAR ) {
                        // Break because no missing are allowed.
                        // Below detect whether have the right count to calculate the statistic...
                        // TODO SAM 2011-02-09 Here is where check for allowed missing would be added
                        break;
                    }
                }
                else {
                    // Add the value to the sample (which has been initialized to zero above...
                    sampleArray[count++] = value;
                }
                // Reset the dates for the input data value...
                if ( (sampleType == RunningAverageType.NYEAR) || (sampleType == RunningAverageType.N_ALL_YEAR) ) {
                    // Get the value for the next year (last value will be the current year).
                    valueDateTime.addInterval ( TimeInterval.YEAR, 1 );
                }
                else {
                    // Just move forward incrementally between end points
                    valueDateTime.addInterval ( intervalBase, intervalMult );
                }
            }
            // Now set the data value to the computed statistic...
            doCalc = false;
            if ( sampleType == RunningAverageType.N_ALL_YEAR ) {
                // Always compute for count > 0
                if ( count > 0 ) {
                    doCalc = true;
                }
            }
            else if ( count == neededCount ) {
                if ( count > 0 ) {
                    doCalc = true;
                }
            }
            if ( doCalc ) {
                // Handle the statistics that are supported...
                try {
                    if ( statisticType == TSStatisticType.LAG1_AUTO_CORRELATION ) {
                        newts.setDataValue(date,MathUtil.lagAutoCorrelation(count, sampleArray, 1));
                    }
                    else if ( statisticType == TSStatisticType.MAX ) {
                        newts.setDataValue(date,MathUtil.max(count, sampleArray));
                    }
                    else if ( statisticType == TSStatisticType.MEAN ) {
                        newts.setDataValue(date,MathUtil.mean(count, sampleArray));
                    }
                    else if ( statisticType == TSStatisticType.MEDIAN ) {
                        newts.setDataValue(date,MathUtil.median(count, sampleArray));
                    }
                    else if ( statisticType == TSStatisticType.MIN ) {
                        newts.setDataValue(date,MathUtil.min(count, sampleArray));
                    }
                    else if ( statisticType == TSStatisticType.SKEW ) {
                        newts.setDataValue(date,MathUtil.skew(count, sampleArray));
                    }
                    else if ( statisticType == TSStatisticType.STD_DEV ) {
                        newts.setDataValue(date,MathUtil.standardDeviation(count, sampleArray));
                    }
                    else if ( statisticType == TSStatisticType.VARIANCE ) {
                        newts.setDataValue(date,MathUtil.variance(count, sampleArray));
                    }
                }
                catch ( Exception e ) {
                    // Should not happen but could if bracket is too small, etc. - just don't set the value...
                }
            }
        }
    
        // Add to the genesis...
    
        newts.addToGenesis ( "Created " + genesis + " running " + statString + " time series from original data" );
        newts.setDescription ( newts.getDescription() + ", " + genesis + " run stat" );
    }
    return newts;
}

/**
Set the N for N-Year or bracket for other running statistic types.
*/
private void setN ( int n )
{
    __n = n;
}

/**
Set the method by which the data sample is determined.
*/
private void setSampleType ( RunningAverageType runningAverageType )
{
    __SampleType = runningAverageType;
}

/**
Set the statistic.
*/
private void setStatisticType ( TSStatisticType statistic )
{
    __statistic = statistic;
}

/**
Set the time series to process.
*/
private void setTS ( TS ts )
{
    __ts = ts;
}

}