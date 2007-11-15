//------------------------------------------------------------------------------
// FrostDatesYearTS - class to hold annual time series data of frost dates
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
// 
// 28 Jul 1998	Catherine E.		Created initial class description.
//		Nutting-Lane, RTi	
// 01 Aug 1998	Steven A. Malers, RTi	Finish code for use in TSTool.
// 31 Aug 1998	SAM, RTi		Change so the order on formatted output
//					is 28S 32S 32F 28F.
// 03 Dec 1998	SAM, RTi		Fix bug where missing years of record
//					in the middle of a time series were
//					causing an exception in fillweights.
// 07 Jan 1999	SAM, RTi		Update to allow averaging period to be
//					specified.  Add an add() method to
//					support adding frost dates time series
//					(appending, really).  Enable the
//					functionality of the
//					changePeriodOfRecord() method.
// 12 Apr 1999	SAM, RTi		Add finalize.
// 11 Sep 2001	SAM, RTi		Change formatOutput() to be static so
//					it can be called more easily.  This
//					should also still work as an instance
//					method.
// 2001-11-06	SAM, RTi		Review javadoc.  Verify that variables
//					are set to null when no longer used.
//					Delete large chunk of commented-out code
//					from changePeriodOfRecord().
// 2003-01-08	SAM, RTi		Add hasData().
// 2003-06-02	SAM, RTi		Upgrade to use generic classes.
//					* Change TSDate to DateTime.
//					* Change TS.INTERVAL* to TimeInterval.
// 2004-03-04	J. Thomas Sapienza, RTi	* Class now implements Serializable.
//					* Class now implements Transferable.
//					* Class supports being dragged or 
//					  copied to clipboard.
// 2007-05-08	SAM, RTi		Cleanup code based on Eclipse feedback.
//------------------------------------------------------------------------------
// EndHeader

package RTi.TS;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

import java.io.IOException;
import java.io.Serializable;
import java.util.Vector;

import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.TimeUtil;

/**
The FrostDatesYearTS class provides storage for year time series for frost
dates.  Frost dates consist of four dates:  Last 28F in the spring, last 32F in
the spring, first 32F in the fall, and first 28F in the fall.  This time series
is currently used in the CDSS project.  The formatOutput() method currently
prints a "StateCU" format file.
*/
public class FrostDatesYearTS 
extends YearTS
implements Serializable, Transferable {

/**
The DataFlavor for transferring this specific class.
*/
public static DataFlavor frostDatesYearTSFlavor = new DataFlavor(
	RTi.TS.FrostDatesYearTS.class, "RTi.TS.FrostDatesYearTS");

// Store the frost dates data as an array of calendar years and Vectors for
// the frost dates.  This class will make sure that there is consistency in the
// data.  Individual set/gets are required to handle the different data.

protected Vector _last_28F_spring;	// of DateTime
protected Vector _last_32F_spring;	// of DateTime
protected Vector _first_32F_fall;	// of DateTime
protected Vector _first_28F_fall;	// of DateTime

/**
Constructor.  Use allocateDataSpace() and setDataValue() to set data.
*/
public FrostDatesYearTS()
{	super ();
	initialize ();
}

/**
Add one time series to another.  The receiving time series description and
genesis information are updated to reflect the addition.  The resulting period
is the maximum overlapping period.
@return The sum of the time series (the original time series is updated).
@param ts Time series to be added to.
@param ts_to_add Time series to add to "ts".
@exception TSException if there is an error adding the time series.
*/
public static FrostDatesYearTS add (	FrostDatesYearTS ts,
					FrostDatesYearTS ts_to_add )
throws TSException
{	String message, routine = "FrostDatesYearTS.add";

	if ( ts == null ) {
		// Nothing to do...
		message = "Null time series";
		Message.printWarning ( 2, routine, message );
		throw new TSException ( message );
	}
	if ( ts_to_add == null ) {
		// Nothing to do...
		message = "Null time series to add";
		Message.printWarning ( 2, routine, message );
		throw new TSException ( message );
	}
	// Else, set up a vector and call the overload routine...
	try {	Vector v = new Vector ( 1, 1 );
		v.addElement ( ts_to_add );
		double [] factor = new double[1];
		factor[0] = 1.0;
		FrostDatesYearTS ts2 = add ( ts, v, factor, TSUtil.MAX_POR );
		v = null;
		factor = null;
		message = null;
		routine = null;
		return ts2;
	}
	catch ( TSException e ) {
		// Just rethrow...
		message = null;
		routine = null;
		throw e;
	}
}

/**
Add a list of time series to another.  The receiving time series description and
genesis information are updated to reflect the addition.  The resulting
time series will have the maximum overlapping period.
@return The sum of the time series (the original time series is updated).
@param ts Time series to be added to.
@param ts_to_add List of time series to add to "ts".
@param flag TSUtil.MAX_POR to specify that output should be the maximum 
overlapping period or 0 to use the period of the first time series.
@exception TSException if an error occurs adding the time series.
*/
public static FrostDatesYearTS add (	FrostDatesYearTS ts, Vector ts_to_add,
					int flag )
throws TSException
{	String message, routine = "FrostDatesYearTS.add";

	// Call the main overload routine...
	if ( ts_to_add == null ) {
		message = "Null time series to add.";
		Message.printWarning ( 2, routine, message );
		throw new TSException ( message );
	}
	try {	int size = ts_to_add.size();
		double [] factor = new double[size];
		for ( int i = 0; i < size; i++ ) {
			factor[i] = 1.0;
		}
		FrostDatesYearTS ts2 = add ( ts, ts_to_add, factor, flag );
		factor = null;
		message = null;
		routine = null;
		return ts2;
	}
	catch ( TSException e ) {
		// Just rethrow...
		message = null;
		routine = null;
		throw e;
	}
}

/**
Add a list of time series to another.  The receiving time series description and
genesis information are updated to reflect the addition.
@return The sum of the time series (the original time series is modified).
@param ts Time series to be added to.
@param ts_to_add List of time series to add to "ts".
@param factor Used by subtract() or directly.  Specifies the factors to multiply
each time series by before adding.  The factors are applied after units
conversion.  Not currently used.
@param flag TSUtil.MAX_POR if the resulting time series is to be extended
to have a period encompassing all the time series, or zero to use the
original period of the first time series.
@exception RTi.TS.TSException if there is an error adding the time series.
*/
public static FrostDatesYearTS add (	FrostDatesYearTS ts, Vector ts_to_add,
					double factor[], int flag )
throws TSException
{	String	message, routine = "FrostDatesYearTS.add(TS,Vector,double[])";
	int	dl = 20;
	FrostDatesYearTS tspt = null;

	// Make sure that the pointers are OK...

	if ( ts_to_add == null ) {
		message = "NULL time series pointer for TS list";
		Message.printWarning ( 2, routine, message );
		throw new TSException ( message );
	}
	if ( ts == null ) {
		message = "NULL time series pointer for TS to receive sum";
		Message.printWarning ( 2, routine, message );
		throw new TSException ( message );
	}

	if ( Message.isDebugOn ) {
		Message.printDebug ( dl, routine,
		"Adding to " + ts.getIdentifierString() );
	}

	// Make sure that the intervals match.  Currently this is a requirement
	// to make sure that the results are not misrepresented.  At some point
	// may want to overload to allow a period to be added.

	try {
	if (	!TSUtil.intervalsMatch(ts_to_add, ts.getDataIntervalBase(),
		ts.getDataIntervalMult()) ) {
		message =
		"All time series in the list are not of interval " +
		ts.getDataIntervalBase() + "," + ts.getDataIntervalMult();
		Message.printWarning ( 2, routine, message );
		throw new TSException ( message );
	}

	// If we want the period of record to be all-inclusive, resize the
	// period of record...

	DateTime start_date = null;
	DateTime end_date = null;
	if ( flag == TSUtil.MAX_POR ) {
		// Get the POR from the list of time series...
		TSLimits limits = null;
		try {
		limits = TSUtil.getPeriodFromTS ( ts, ts_to_add,
			TSUtil.MAX_POR );
		start_date = limits.getDate1();
		end_date = limits.getDate2();
		if ( Message.isDebugOn ) {
			Message.printDebug ( dl, routine,
			"Resulting TS will have period " + start_date +
			" to " + end_date );
		}
		}
		catch ( Exception e ) {
			message = "Can't get maximum period for time series.";
			Message.printWarning ( 2, routine, message );
			throw new TSException ( message );
		}
		// Change the period of record...
		try {	ts.changePeriodOfRecord ( start_date, end_date );
		}
		catch ( Exception e ) {
			message = "Can't change period of time series from " +
			start_date + " to " + end_date;
			Message.printWarning ( 2, routine, message );
			throw new TSException ( message );
		}
		limits = null;
	}

	// Now loop through the time series list and add to the primary time
	// series...

	// Set starting and ending time for time loop based on period of
	// "tsadd"...

	int ntslist = ts_to_add.size();
	int interval_base = ts.getDataIntervalBase();
	int interval_mult = ts.getDataIntervalMult();
	int tspt_interval_base;
	DateTime	data_value_to_add = null;
	DateTime	transfer_start_date = null;
	DateTime	transfer_end_date = null;
	DateTime	date = null;
	int	year = 0;
	for ( int i = 0; i < ntslist; i++ ) {
		tspt = (FrostDatesYearTS)ts_to_add.elementAt(i);
		if ( tspt == null ) {
			message =
			"Trouble getting [" + i + "]-th time series in list";
			Message.printWarning ( 2, routine, message );
			throw new TSException ( message );
		}

		// Work on the one time series...

		tspt_interval_base = tspt.getDataIntervalBase();

		if ( tspt_interval_base == TimeInterval.IRREGULAR ) {
			// add to that date.  Otherwise, add a new data point
			// for the date...  For now, don't support...
			message = "IrregularTS not supported.  Not adding." +
			tspt.getIdentifier().toString();
			Message.printWarning ( 2, routine, message );
			throw new TSException ( message );
		}
		else {	// Regular interval.  Loop using addInterval...
			if ( (start_date != null) && (end_date != null) ) {
				// Period was specified above...
				transfer_start_date = new DateTime(start_date );
				transfer_end_date = new DateTime ( end_date );
			}
			else {	// Just use the receiving time series period...
				transfer_start_date = new DateTime (
					ts.getDate1() );
				transfer_end_date = new DateTime (
					ts.getDate2() );
			}
			date = new DateTime ( transfer_start_date );
			if ( Message.isDebugOn ) {
				Message.printDebug ( dl, routine,
				"Adding from " + tspt.getIdentifierString() +
				" for " + transfer_start_date + " to " +
				transfer_end_date );
			}
			
			for (	;
				date.lessThanOrEqualTo( transfer_end_date);
				date.addInterval(interval_base, interval_mult)){
				year = date.getYear();
				// Get the value to add...
				data_value_to_add = tspt.getLast28Spring (year);
				if ( data_value_to_add == null ) {
					// The value to add is missing so don't
					// do it...
					continue;
				}
				// Do the to reset.  Don't consider the
				// factors right now...
				if ( Message.isDebugOn ) {
					Message.printDebug ( dl, routine,
					"Setting last 28 Spring " +
					data_value_to_add + " for " + year );
				}
				ts.setLast28Spring ( year, new DateTime (
					data_value_to_add ) );

				// Get the value to add...
				data_value_to_add = tspt.getLast32Spring (year);
				if ( data_value_to_add == null ) {
					// The value to add is missing so don't
					// do it...
					continue;
				}
				// Do the to reset.  Don't consider the
				// factors right now...
				ts.setLast32Spring ( year, new DateTime (
					data_value_to_add ) );

				// Get the value to add...
				data_value_to_add = tspt.getFirst32Fall (year);
				if ( data_value_to_add == null ) {
					// The value to add is missing so don't
					// do it...
					continue;
				}
				// Do the to reset.  Don't consider the
				// factors right now...
				ts.setFirst32Fall ( year, new DateTime (
					data_value_to_add )  );

				// Get the value to add...
				data_value_to_add = tspt.getFirst28Fall (year);
				if ( data_value_to_add == null ) {
					// The value to add is missing so don't
					// do it...
					continue;
				}
				// Do the to reset.  Don't consider the
				// factors right now...
				ts.setFirst28Fall ( year,
					new DateTime ( data_value_to_add ) );

				//if ( Message.isDebugOn ) {
					//Message.printDebug ( dl, routine,
					//"At " + date.toString() +
					//", adding " + data_value_to_add );
				//}
			}
			ts.setDescription ( ts.getDescription() + " + " +
			tspt.getDescription () );
			ts.addToGenesis("Added \"" + tspt.getIdentifier()+"\"");
		}
	}
	// Clean up...
	message = null;
	routine = null;
	start_date = null;
	end_date = null;
	data_value_to_add = null;
	transfer_start_date = null;
	transfer_end_date = null;
	date = null;
	return ts;
	}
	catch ( Exception e ) {
		message = "Error adding time series.";
		Message.printWarning ( 2, routine, message );
		Message.printWarning ( 2, routine, e );
		throw new TSException ( message );
	}
}

/**
Allocate the data space for the data based on the period that has been set.
@return 0 if successful, 1 if not.
*/
public int allocateDataSpace ()
{	String routine = "FrostDatesYearTS.allocateDataSpace";

	// Need to throw an exception when we rework...

	if ( _date1 == null ) {
		Message.printWarning ( 2, routine, "First date is null." );
		return 1;
	}
	if ( _date2 == null ) {
		Message.printWarning ( 2, routine, "Last date is null." );
		return 1;
	}
	if ( _date1.greaterThan(_date2) ) {
		Message.printWarning ( 2, routine,
		"First date (" + _date1 + ") is later than last date (" +
		_date2 + ")." );
		return 1;
	}

	// Allocate the arrays and fill with missing (null dates)...

	int year1 = _date1.getYear ();
	int nyears = _date2.getYear() - year1 + 1;

	_last_28F_spring = new Vector ( nyears, 5 );
	_last_32F_spring = new Vector ( nyears, 5 );
	_first_32F_fall = new Vector ( nyears, 5 );
	_first_28F_fall = new Vector ( nyears, 5 );

	for ( int i = 0; i < nyears; i++ ) {
		_last_28F_spring.addElement ( (DateTime)null );
		_last_32F_spring.addElement ( (DateTime)null );
		_first_32F_fall.addElement ( (DateTime)null );
		_first_28F_fall.addElement ( (DateTime)null );
	}

	routine = null;
	return 0;
}

/** 
Change the period of record to the specified dates.  If the period is extended,
missing data will be used to fill the time series.  If the period is shortened,
data will be lost.
@param date1 New start date of time series.
@param date2 New end date of time series.
@exception RTi.TS.TSException if there is a problem extending the data.
*/
public void changePeriodOfRecord ( DateTime date1, DateTime date2 )
throws TSException
{	String	message, routine="FrostDatesYearTS.changePeriodOfRecord";

	// To transfer, we need to allocate a new data space.  In any case, we
	// need to get the dates established...
	if ( (date1 == null) && (date2 == null) ) {
		// No dates.  Cannot change.
		message = "\"" + _id +
		"\": period dates are null.  Cannot change the period.";
		Message.printWarning ( 2, routine, message );
		throw new TSException ( message );
	}

	DateTime new_date1 = null;
	if ( date1 == null ) {
		// Use the original date...
		new_date1 = new DateTime ( _date1 );
	}
	else {	// Use the date passed in...
		new_date1 = new DateTime ( date1 );
	}
	DateTime new_date2 = null;
	if ( date2 == null ) {
		// Use the original date...
		new_date2 = new DateTime ( _date2 );
	}
	else {	// Use the date passed in...
		new_date2 = new DateTime ( date2 );
	}

	// Do not change the period if the dates are the same...

	if ( _date1.equals(new_date1) && _date2.equals(new_date2) ) {
		// No need to change period...
		return;
	}

	// To transfer the data (later), we get the old position and then set
	// in the new position.  To get the right data position, declare new
	// Vectors and save references to the old data...

	Vector save_last_28F_spring = _last_28F_spring;
	Vector save_last_32F_spring = _last_32F_spring;
	Vector save_first_32F_fall = _first_32F_fall;
	Vector save_first_28F_fall = _first_28F_fall;
	DateTime save_date1 = new DateTime ( _date1 );

	// Optimize the transfer dates to speed performance and so we only
	// attempt to transfer existing data from the old lists...

	DateTime transfer_date1 = null;
	DateTime transfer_date2 = null;
	if ( new_date1.lessThan(_date1) ) {
		// Extending so use the old date...
		transfer_date1 = new DateTime ( _date1 );
	}
	else {	// Shortening so use the new...
		transfer_date1 = new DateTime ( new_date1 );
	}
	if ( new_date2.greaterThan(_date2) ) {
		// Extending so use the old date...
		transfer_date2 = new DateTime ( _date2 );
	}
	else {	// Shortening so use the new...
		transfer_date2 = new DateTime ( new_date2 );
	}

	// Now reset the dates and reallocate the period...

	setDate1 ( new_date1 );
	setDate2 ( new_date2 );
	allocateDataSpace();

	// At this point the data space will be completely filled with missing
	// data.

	// Now transfer the data.  To do so, get the old position and then set
	// in the new position.  We are only concerned with transferring the
	// values for the the old time series that are within the new period...

	// First year to be transferred...
	int transfer_year1 = transfer_date1.getYear();
	int oldpos, old_year1 = save_date1.getYear(), year;
	int nyears = transfer_date2.getYear() - transfer_date1.getYear() + 1;
	DateTime date = null;
	for (	int i = 0; i < nyears; i++ ) {
		// Year that we are transferring...
		year = transfer_year1 + i;
		// Position in old lists...
		oldpos = year - old_year1; 
		// Now transfer each the values...
		date = (DateTime)save_last_28F_spring.elementAt(oldpos);
		if ( date != null ) {
			setLast28Spring ( year, new DateTime ( date ) );
		}

		date = (DateTime)save_last_32F_spring.elementAt(oldpos);
		if ( date != null ) {
			setLast32Spring ( year, new DateTime ( date ) );
		}

		date = (DateTime)save_first_32F_fall.elementAt(oldpos);
		if ( date != null ) {
			setFirst32Fall ( year, new DateTime ( date ) );
		}

		date = (DateTime)save_first_28F_fall.elementAt(oldpos);
		if ( date != null ) {
			setFirst28Fall ( year, new DateTime ( date ) );
		}
	}
	// Clean up...
	message = null;
	routine = null;
	new_date1 = null;
	new_date2 = null;
	save_last_28F_spring = null;
	save_last_32F_spring = null;
	save_first_32F_fall = null;
	save_first_28F_fall = null;
	save_date1 = null;
	transfer_date1 = null;
	transfer_date2 = null;
	date = null;

	// Add to the genesis...

	addToGenesis ( "Changed period: " + new_date1 + " to " + new_date2 );
}

/**
Fill the time series using weights and neighboring stations.  This method was
written for use by TSTool.  Filled values are simply the weights multiplied
by the neighboring value.  If any of the neighboring values are missing, use
the long-term average for this time series if "fill_ave" is true.  This also
applies if no weights are specified.  Floating point numbers are used for
computations to allow for any station weights, etc.
@param weights Array of weights to use.  Set to null if not filling with
weights.
@param tslist List of time series to use for filling.  Set to null if not
filling with weights.
@param date1 The starting date for filling.
@param date2 The ending date for filleding.
@param fill_ave If true fill missing data with the long-term average of the
filled station, if available.  This is applied after filling with weighted
stations.
@param limits Data limits for the FrostDatesYearTS to be used for filling with
averages.  If the fill_ave parameter is true and the limits are not given,
the limits for the entire time series period are computed and used.  If you
want to fill with averages for only a certain period, compute the limits before
calling this method.
*/
public void fillWeights (	double [] weights, FrostDatesYearTS [] tslist,
				DateTime date1, DateTime date2,
				boolean fill_ave,
				FrostDatesYearTSLimits limits )
throws TSException
{	String message, routine = "FrostDatesYearTS.fillWeights";

	try {	// Main try

	// Get the time series limits.  Note that this is done before any
	// filling so that the averages are only computed from raw data...

	FrostDatesYearTSLimits data_limits = limits;
	DateTime date32fall = null, date28fall = null, date28spring = null,
		date32spring = null, filldate = null;
	DateTime date28spring_ave = null;
	DateTime date32spring_ave = null;
	DateTime date32fall_ave = null;
	DateTime date28fall_ave = null;
	if ( fill_ave ) {
		// We are filling with average.  Make sure that we have
		// limits...
		if ( data_limits == null ) {
			// Get the limits using the time series...
			try {
			data_limits = new FrostDatesYearTSLimits ( this );
			}
			catch ( Exception e ) {
				message = "Error getting limits for averaging.";
				Message.printWarning ( 2, routine, message );
				throw new TSException ( message );
			}
		}
		// Averages that will be used for filling...
		date28spring_ave = data_limits.getMeanLast28Spring();
		date32spring_ave = data_limits.getMeanLast32Spring();
		date32fall_ave = data_limits.getMeanFirst32Fall();
		date28fall_ave = data_limits.getMeanFirst28Fall();
	}

	// Now loop through the time series, and if any dates are missing, use
	// the other dates to weight or use the long-term average.  First get
	// the valid dates for filling...

	TSLimits valid_dates = TSUtil.getValidPeriod ( this, date1, date2 );
	DateTime start	= valid_dates.getDate1();
	DateTime end	= valid_dates.getDate2();
	valid_dates = null;

	if ( (start == null) || (end == null) ) {
		message = "Period is not set.  Will not fill.";
		Message.printWarning ( 2, routine, message );
		throw new TSException ( message );
	}

	// Loop by year.  For averages, we do not want the year considered so
	// subtract it as appropriate.

	int	count = 0, nweights = 0, year = 0;
	double	sum = 0.0;
	for (	DateTime date = new DateTime ( start );
		date.lessThanOrEqualTo ( end );
		date.addYear(1) ) {
		year = date.getYear();
		date28spring = getLast28Spring ( year );
		if ( date28spring == null ) {
			sum = 0.0;
			count = 0;
			for ( int j = 0; j < nweights; j++ ) {
				filldate = tslist[j].getLast28Spring(year);
				if ( filldate == null ) {
					if ( fill_ave ) {
						date28spring = date28spring_ave;
					}
					break;
				}
				// Else, convert to a double and add to sum...
				sum += weights[j]*(filldate.toDouble() -
					(double)filldate.getYear());
				++count;
			}
			if ( (count != 0) && (count == nweights) ) {
				// Convert weighted sum back to a DateTime...
				date28spring = new DateTime ( sum, true );
			}
			else if ( (count == 0) && fill_ave ) {
				// The data value is the average...
				date28spring = date28spring_ave;
			}
			// Set the date...
			setLast28Spring(year,date28spring);
		}
		date32spring = getLast32Spring ( year );
		if ( date32spring == null ) {
			sum = 0.0;
			count = 0;
			for ( int j = 0; j < nweights; j++ ) {
				filldate = tslist[j].getLast32Spring(year);
				if ( filldate == null ) {
					if ( fill_ave ) {
						date32spring = date32spring_ave;
					}
					break;
				}
				// Else, convert to a double and add to sum...
				sum += weights[j]*(filldate.toDouble() -
					(double)filldate.getYear());
				++count;
			}
			if ( (count != 0 ) && (count == nweights) ) {
				// Now convert weighted sum back to a
				// DateTime...
				date32spring = new DateTime ( sum, true );
			}
			else if ( (count == 0) && fill_ave ) {
				// The data value is the average...
				date32spring = date32spring_ave;
			}
			setLast32Spring ( year, date32spring );
		}
		date32fall = getFirst32Fall ( year );
		if ( date32fall == null ) {
			sum = 0.0;
			count = 0;
			for ( int j = 0; j < nweights; j++ ) {
				filldate = tslist[j].getFirst32Fall(year);
				if ( filldate == null ) {
					if ( fill_ave ) {
						date32fall = date32fall_ave;
					}
					break;
				}
				// Else, convert to a double and add to sum...
				sum += weights[j]*(filldate.toDouble() -
					(double)filldate.getYear());
				++count;
			}
			if ( (count != 0) && (count == nweights) ) {
				// Now convert weighted sum to a DateTime...
				date32fall = new DateTime ( sum, true );
			}
			else if ( (count == 0) && fill_ave ) {
				// The data value is the average...
				date32fall = date32fall_ave;
			}
			setFirst32Fall ( year, date32fall );
		}
		date28fall = getFirst28Fall ( year );
		if ( date28fall == null ) {
			sum = 0.0;
			count = 0;
			for ( int j = 0; j < nweights; j++ ) {
				filldate = tslist[j].getFirst28Fall(year);
				if ( filldate == null ) {
					if ( fill_ave ) {
						date28fall = date28fall_ave;
					}
					break;
				}
				// Else, convert to a double and add to sum...
				sum += weights[j]*(filldate.toDouble() -
					(double)filldate.getYear());
				++count;
			}
			if ( (count != 0) && (count == nweights) ) {
				// Now convert weighted sum to a DateTime...
				date28fall = new DateTime ( sum, true );
			}
			else if ( (count == 0) && fill_ave ) {
				// The data value is the average...
				date28fall = date28fall_ave;
			}
			setFirst28Fall ( year, date28fall );
		}
	}
	// Clean up...
	data_limits = limits;
	date32fall = null;
	date28fall = null;
	date28spring = null;
	date32spring = null;
	filldate = null;
	date28spring_ave = null;
	date32spring_ave = null;
	date32fall_ave = null;
	date28fall_ave = null;
	start = null;
	end = null;
	}
	catch ( Exception e ) {
		message = "Error filling frost dates.";
		Message.printWarning ( 2, routine, message );
		Message.printWarning ( 2, routine, e );
		throw new TSException ( message );
	}
	message = null;
	routine = null;
}

/**
Finalize before garbage collection.
@exception Throwable if there is an error.
*/
protected void finalize()
throws Throwable
{	_last_28F_spring = null;
	_last_32F_spring = null;
	_first_32F_fall = null;
	_first_28F_fall = null;
	super.finalize();
}

/**
Return the date corresponding to the first 28 F temperature in the fall.
@return The date corresponding to the first 28 F temperature in the fall given
the year requested, or null if missing.
@param year Calendar year.
*/
public DateTime getFirst28Fall ( int year )
{	if ( _first_28F_fall == null ) {
		return null;
	}
	try {	int year1 = _date1.getYear();
		int year2 = _date2.getYear();
		if ( (year < year1) || (year > year2) ) {
			// Outside the period so return null...
			return null;
		}
		int pos = year - year1;
		return (DateTime)_first_28F_fall.elementAt(pos);
	}
	catch ( Exception e ) {
		return null;
	}
}

/**
Return the date corresponding to the first 32 F temperature in the fall.
@return The date corresponding to the first 32 F temperature in the fall given
the year requested, or null if missing.
@param year Calendar year.
*/
public DateTime getFirst32Fall ( int year )
{	if ( _first_32F_fall == null ) {
		return null;
	}
	try {	int year1 = _date1.getYear();
		int year2 = _date2.getYear();
		if ( (year < year1) || (year > year2) ) {
			// Outside the period so return null...
			return null;
		}
		int pos = year - year1;
		return (DateTime)_first_32F_fall.elementAt(pos);
	}
	catch ( Exception e ) {
		return null;
	}
}

/**
Return The date corresponding to the last 28 F temperature in the spring.
@return The date corresponding to the last 28 F temperature in the spring given
the year requested, or null if missing.
@param year Calendar year.
*/
public DateTime getLast28Spring ( int year )
{	if ( _last_28F_spring == null ) {
		return null;
	}
	try {	int year1 = _date1.getYear();
		int year2 = _date2.getYear();
		if ( (year < year1) || (year > year2) ) {
			// Outside the period so return null...
			return null;
		}
		int pos = year - year1;
		return (DateTime)_last_28F_spring.elementAt(pos);
	}
	catch ( Exception e ) {
		return null;
	}
}

/**
Return The date corresponding to the last 32 F temperature in the spring.
@return The date corresponding to the last 32 F temperature in the spring given
the year requested, or null if missing.
@param year Calendar year.
*/
public DateTime getLast32Spring ( int year )
{	if ( _last_32F_spring == null ) {
		return null;
	}
	try {	int year1 = _date1.getYear();
		int year2 = _date2.getYear();
		if ( (year < year1) || (year > year2) ) {
			// Outside the period so return null...
			return null;
		}
		int pos = year - year1;
		return (DateTime)_last_32F_spring.elementAt(pos);
	}
	catch ( Exception e ) {
		return null;
	}
}

/**
Format output for printing or display.  This formats the time series similar
to a standard StateMod time series, but for the dates in the data.
@param ts_list List of time series to format.
@param date1_req Requested starting date.
@param date2_req Requested ending date.
@return a Vector of String that can be displayed or output to a file.
*/
public static Vector formatOutput (	Vector ts_list, 
					DateTime date1_req, DateTime date2_req )
throws IOException
{	String	cmnt = "#>";	// Non-permanent comment for header.
	String	message = null;
	String	rtn="FrostDatesYearTS.formatOutput";
	Vector	strings = new Vector ( 50, 50 );
	Vector	v = new Vector ( 50 );

	try {

	if ( Message.isDebugOn ) {
		Message.printStatus ( 1, rtn, "Creating frost dates summary " +
		"for " + date1_req + " to " + date1_req );
	}

	int year1_req = date1_req.getYear(), year2_req = date2_req.getYear();

	String	iline, iline_format;
	int	i, j, year, year1 = year1_req, year2 = year2_req,
		num_years;

	// count the number of series in list
	if ( ts_list == null ) {
		Message.printWarning ( 2, rtn,
		"Null time series list" );
		return strings;
	}
	int nseries = ts_list.size();

	// If period of record of interest was not requested, find period of
	// record which covers all time series
	if ((year1_req==0) || (year2_req==0))
	{
		int earliest_year, latest_year;

		// use first time series dates as first and last
		// then compare to remaining time series for earlier
		// and later dates
		earliest_year = 
			((FrostDatesYearTS)
			ts_list.elementAt(0)).getDate1().getYear();
		latest_year = 
			((FrostDatesYearTS)
			ts_list.elementAt(0)).getDate2().getYear();

		for ( i=1; i<nseries; i++ )
		{
			year = ((FrostDatesYearTS)
				ts_list.elementAt(i)).getDate1().getYear();
			if (( year < earliest_year ) && ( year != 0)) {
				earliest_year = year;
			}
			year = ((FrostDatesYearTS)
				ts_list.elementAt(i)).getDate2().getYear();
			if ( year > latest_year ) {
				latest_year = year;
			}
		}

		if  (year1==0) {
			year1 = earliest_year;
		}
		if  (year2==0) {
			year2 = latest_year;
		}
	}

	// Print comments at the top of the file 

	strings.addElement ( cmnt );
	strings.addElement ( cmnt );
	strings.addElement ( cmnt + " Frost dates time series" );
	strings.addElement ( cmnt + " ***********************" );
	strings.addElement ( cmnt );
	strings.addElement ( cmnt );

	// Always do calendar year...

	strings.addElement ( cmnt + " Years Shown = Calendar Years" );
	strings.addElement ( cmnt + 
		"(requested period of record for time series data may be" );
	strings.addElement( cmnt + 
		"different from each station's recorded period of record" );
	strings.addElement ( cmnt + 
		"as shown in the following table)" );
	strings.addElement ( cmnt );

	// print each time series id, description, and type

	strings.addElement ( cmnt + "     TS ID                    Type" +
	"   Source   Units  Period of Record    Location    Description");

	String			empty_string = "-", tmpdesc, tmpid, tmplocation,
				tmpsource, tmptype, tmpunits;
	FrostDatesYearTS	tsptr = null;
	String			format;
	for ( i=0; i < nseries; i++ ) {
		tsptr = (FrostDatesYearTS)ts_list.elementAt(i);

		tmpid = tsptr.getIdentifierString();
		if (tmpid.length() == 0 ) {
			tmpid = empty_string;
		}

		tmpdesc = tsptr.getDescription();
		if ( tmpdesc.length() == 0 ) {
			tmpdesc = empty_string;
		}

		tmptype = tsptr.getIdentifier().getType();
		if ( tmptype.length() == 0 ) {
			tmptype = empty_string;
		}

		tmpsource = tsptr.getIdentifier().getSource();
		if ( tmpsource.length() == 0 ) {
			tmpsource = empty_string;
		}

		tmpunits = tsptr.getDataUnits();
		if ( tmpunits.length() == 0) {
			tmpunits = empty_string;
		}

		tmplocation = tsptr.getIdentifier().getLocation();
		if (tmplocation.length() == 0) {
			tmplocation = empty_string;
		}

		format= "%s %3d %-24.24s %-6.6s %-8.8s %-6.6s %3.3s/%d - "
			+ "%3.3s/%d %-12.12s%-24.24s";
		v.removeAllElements();
		v.addElement ( cmnt );
		v.addElement ( new Integer ( i+1 ));
		v.addElement ( tmpid );
		v.addElement ( tmptype );
		v.addElement ( tmpsource );
		v.addElement ( tmpunits );
		v.addElement ( 
			TimeUtil.monthAbbreviation(
			tsptr.getDate1().getMonth()));
		v.addElement ( new Integer (
			tsptr.getDate1().getYear()));
		v.addElement ( 
			TimeUtil.monthAbbreviation(
			tsptr.getDate2().getMonth()));
		v.addElement ( new Integer (
			tsptr.getDate2().getYear()));
		v.addElement ( tmplocation );
		v.addElement ( tmpdesc );

		iline = StringUtil.formatString ( v, format );
		strings.addElement ( iline );
	}
	strings.addElement ( cmnt );
	strings.addElement ( cmnt );
	strings.addElement ( cmnt );

	// Ready to print table
	//
	// check a few conditions which would end this routine...
	if ( nseries == 0 ) {
		strings.addElement ( "No time series data." );
		return strings;
	}
	for (i=0; i<nseries; i++ )
	{
		tsptr = (FrostDatesYearTS)ts_list.elementAt(i);
		if ( tsptr.getDataIntervalBase() != TimeInterval.YEAR ) {
			message = "A TS interval other than year detected:" +
		   	tsptr.getDataIntervalBase();		
			Message.printWarning ( 2, rtn, message );
			throw new IOException ( message );
		}
	}

	strings.addElement( cmnt + "Temperatures are degrees F" );
	strings.addElement( cmnt );
	strings.addElement( cmnt +
	"               Last    Last    First   First" );
	strings.addElement( cmnt +
	" Yr ID         Spr 28  Spr 32  Fall 32 Fall 28" );

	strings.addElement ( 
		cmnt + "-e-b----------eb------eb------eb------eb------e" );

	// For now, always output in calendar year...

	strings.addElement ( new String ("    1/" +
	StringUtil.formatString(year1,"%4d") + "  -     12/" +
	StringUtil.formatString(year2,"%4d") + " DATE  CYR" ));

	DateTime	ts_date32fall = null, ts_date28fall = null,
		ts_date28spring = null, ts_date32spring = null;
	
	num_years = year2 - year1 + 1;
	Vector iline_v = new Vector(30);
	Integer missing_Integer = new Integer ( -999 );
	for ( i = 0; i < num_years; i++ ) {
		year = year1 + i;
		for ( j = 0; j < nseries; j++ ) {
			// first, clear this string out, then append to it
			iline = new String();	
			iline_format = new String();	
			iline_v.removeAllElements();
			tsptr = (FrostDatesYearTS)ts_list.elementAt(j);

			iline_format = "%4d %-12.12s";
			iline_v.addElement ( new Integer (year));
			iline_v.addElement ( 
				tsptr.getIdentifier().getLocation());

			// Get the data from the time series (null if not
			// found)...

			ts_date32fall = tsptr.getFirst32Fall(year);
			ts_date28fall = tsptr.getFirst28Fall(year);
			ts_date28spring = tsptr.getLast28Spring(year);
			ts_date32spring = tsptr.getLast32Spring(year);

			// Now format for output...

			if ( ts_date28spring == null ) {
				iline_format += "%8d";
				// No date...
				iline_v.addElement ( missing_Integer );
			}
			else {	iline_format += "%8.8s";
				iline_v.addElement (
				ts_date28spring.toString(
				DateTime.FORMAT_MM_SLASH_DD));
			}
			if ( ts_date32spring == null ) {
				iline_format += "%8d";
				// No date...
				iline_v.addElement ( missing_Integer );
			}
			else {	iline_format += "%8.8s";
				iline_v.addElement (
				ts_date32spring.toString(
				DateTime.FORMAT_MM_SLASH_DD));
			}
			if ( ts_date32fall == null ) {
				iline_format += "%8d";
				// No date...
				iline_v.addElement ( missing_Integer );
			}
			else {	iline_format += "%8.8s";
				iline_v.addElement (
				ts_date32fall.toString(
				DateTime.FORMAT_MM_SLASH_DD));
			}
			if ( ts_date28fall == null ) {
				iline_format += "%8d";
				// No date...
				iline_v.addElement ( missing_Integer );
			}
			else {	iline_format += "%8.8s";
				iline_v.addElement (
				ts_date28fall.toString(
				DateTime.FORMAT_MM_SLASH_DD));
			}
			strings.addElement ( StringUtil.formatString(
				iline_v, iline_format) );
		}
	}
	// Clean up...
	iline = null;
	iline_format = null;
	empty_string = null;
	tmpdesc = null;
	tmpid = null;
	tmplocation = null;
	tmpsource = null;
	tmptype = null;
	tmpunits = null;
	tsptr = null;
	format = null;
	ts_date32fall = null;
	ts_date28fall = null;
	ts_date28spring = null;
	ts_date32spring = null;
	iline_v = null;
	missing_Integer = null;
	} catch ( Exception e ) {
		message = "Unable to format frost dates output.";
		Message.printWarning ( 2, rtn, message );
		throw new IOException ( message );
	}
	cmnt = null;
	message = null;
	rtn=null;
	v = null;
	return strings;
}

/**
Returns the data in the specified DataFlavor, or null if no matching flavor
exists.  From the Transferable interface.  Supported dataflavors are:<br>
<ul>
<li>FrostDatesYearTS - FrostDatesYearTS.class / RTi.TS.FrostDatesYearTS</li>
<li>YearTS - TS.class / RTi.TS.YearTS</li>
<li>TS - TS.class / RTi.TS.TS</li>
<li>TSIdent - TSIdent.class / RTi.TS.TSIdent</li></ul> 
@param flavor the flavor in which to return the data.
@return the data in the specified DataFlavor, or null if no matching flavor
exists.
*/
public Object getTransferData(DataFlavor flavor) {
	if (flavor.equals(frostDatesYearTSFlavor)) {
		return this;
	}
	else if (flavor.equals(YearTS.yearTSFlavor)) {
		return this;
	}
	else if (flavor.equals(TS.tsFlavor)) {
		return this;
	}
	else if (flavor.equals(TSIdent.tsIdentFlavor)) {
		return _id;
	}
	else {
		return null;
	}
}

/**
Returns the flavors in which data can be transferred.  From the Transferable
interface.  
The order of the dataflavors that are returned are:<br>
<ul>
<li>FrostDatesYearTS - FrostDatesYearTS.class / RTi.TS.FrostDatesYearTS</li>
<li>YearTS - TS.class / RTi.TS.YearTS</li>
<li>TS - TS.class / RTi.TS.TS</li>
<li>TSIdent - TSIdent.class / RTi.TS.TSIdent</li></ul>
@return the flavors in which data can be transferred.
*/
public DataFlavor[] getTransferDataFlavors() {
	DataFlavor[] flavors = new DataFlavor[4];
	flavors[0] = frostDatesYearTSFlavor;
	flavors[1] = YearTS.yearTSFlavor;
	flavors[2] = TS.tsFlavor;
	flavors[3] = TSIdent.tsIdentFlavor;
	return flavors;
}

/**
Indicate whether the time series has data, determined by checking to see whether
the data space has been allocated.  This method can be called after a time
series has been read - even if no data are available, the header information
may be complete.  The alternative of returning a null time series from a read
method if no data are available results in the header information being
unavailable.  Instead, return a TS with only the header information and call
hasData() to check to see if the data space has been assigned.
@return true if data are available (the data space has been allocated).
Note that true will be returned even if all the data values are set to the
missing data value.
*/
public boolean hasData ()
{	if (	(_last_28F_spring != null) ||
		(_last_32F_spring != null) ||
		(_first_32F_fall != null) ||
		(_first_28F_fall != null) ) {
		// Assume that we have enough data to be considered "having
		// data"
		return true;
	}
	else {	return false;
	}
}

/**
Initialize the data.
*/
private void initialize ()
{	setDataUnits ( "DATE" );
	_last_28F_spring = null;
	_last_32F_spring = null;
	_first_32F_fall = null;
	_first_28F_fall = null;
}

/**
Determines whether the specified flavor is supported as a transfer flavor.
From the Transferable interface.  Supported dataflavors are:<br>
<ul>
<li>FrostDatesYearTS - FrostDatesYearTS.class / RTi.TS.FrostDatesYearTS</li>
<li>YearTS - TS.class / RTi.TS.YearTS</li>
<li>TS - TS.class / RTi.TS.TS</li>
<li>TSIdent - TSIdent.class / RTi.TS.TSIdent</li></ul> 
@param flavor the flavor to check.
@return true if data can be transferred in the specified flavor, false if not.
*/
public boolean isDataFlavorSupported(DataFlavor flavor) {
	if (flavor.equals(frostDatesYearTSFlavor)) {
		return true;
	}
	else if (flavor.equals(YearTS.yearTSFlavor)) {
		return true;
	}
	else if (flavor.equals(TS.tsFlavor)) {
		return true;
	}
	else if (flavor.equals(TSIdent.tsIdentFlavor)) {
		return true;
	}
	else {
		return false;
	}
}

/**
Set the date corresponding to the first 28 F temperature in the fall given
the year.
@param year Calendar year.
@param date Frost date value to use.
*/
public void setFirst28Fall ( int year, DateTime date )
{	if ( _first_28F_fall == null ) {
		return;
	}
	int year1 = _date1.getYear();
	int year2 = _date2.getYear();
	if ( (year < year1) || (year > year2) ) {
		// Outside the period so return...
		return;
	}
	int pos = year - year1;
	_first_28F_fall.setElementAt(date,pos);
}

/**
Set the date corresponding to the first 32 F temperature in the fall
given the year requested.
@param year Calendar year.
@param date Frost date value to use.
*/
public void setFirst32Fall ( int year, DateTime date )
{	if ( _first_32F_fall == null ) {
		return;
	}
	int year1 = _date1.getYear();
	int year2 = _date2.getYear();
	if ( (year < year1) || (year > year2) ) {
		// Outside the period so return...
		return;
	}
	int pos = year - year1;
	_first_32F_fall.setElementAt( date, pos);
}

/**
Set the date corresponding to the last 28 F temperature in the spring
given the year requested.
@param year Calendar year.
@param date Frost date value to use.
*/
public void setLast28Spring ( int year, DateTime date )
{	if ( _last_28F_spring == null ) {
		return;
	}
	int year1 = _date1.getYear();
	int year2 = _date2.getYear();
	if ( (year < year1) || (year > year2) ) {
		// Outside the period so return...
		return;
	}
	int pos = year - year1;
	_last_28F_spring.setElementAt(date,pos);
}

/**
Set the date corresponding to the last 32 F temperature in the spring
given the year requested.
@param year Calendar year.
@param date Frost date value to use.
*/
public void setLast32Spring ( int year, DateTime date )
{	if ( _last_32F_spring == null ) {
		return;
	}
	int year1 = _date1.getYear();
	int year2 = _date2.getYear();
	if ( (year < year1) || (year > year2) ) {
		// Outside the period so return...
		return;
	}
	int pos = year - year1;
	_last_32F_spring.setElementAt(date,pos);
}

} // End FrostDatesYearTS