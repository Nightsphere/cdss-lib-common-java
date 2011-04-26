package RTi.Util.Time;

/**
This class stores a range defined by two DateTime end-points.
It is useful for specifying a processing period.
Currently the instance is immutable and copies of the DateTime data are copied at construction.
Null date/times are allowed.
Currently there is no validation done.
*/
public class DateTimeRange
{
    /**
     * Starting DateTime for the range.
     */
    private DateTime __start = null;
    
    /**
     * Ending DateTime for the range.
     */
    private DateTime __end = null;
    
    /**
     * Constructor.
     * @param start starting date/time in the range.  Can be null to indicate open-ended range (from available start).
     * @param end ending date/time in the range.  Can be null to indicate open-ended range (to available end).
     */
    public DateTimeRange ( DateTime start, DateTime end )
    {
        if ( start == null ) {
            __start = null;
        }
        else {
            __start = new DateTime ( start );
        }
        if ( end == null ) {
            __end = null;
        }
        else {
            __end = new DateTime ( end );
        }
    }
    
    /**
     * Return the ending date/time in the range (can be null) if open-ended.
     * @return the ending date/time in the range (can be null) if open-ended.
     */
    public DateTime getEnd ()
    {
        return __end;
    }
    
    /**
     * Return the starting date/time in the range (can be null) if open-ended.
     * @return the starting date/time in the range (can be null) if open-ended.
     */
    public DateTime getStart ()
    {
        return __start;
    }
}