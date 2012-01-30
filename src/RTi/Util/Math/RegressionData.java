package RTi.Util.Math;

/**
Data used in a regression analysis.  The data are simple arrays; however, when combined to handle monthly
regression or other complex analysis, the object help manage data.
*/
public class RegressionData
{

// Data for overlapping X (X1)...
    
/**
The independent data array, for points that overlap the dependent data array.
*/
private double [] __x1 = new double[0];

/**
Minimum value of X in N1.
*/
private Double __minX1 = null;

/**
Maximum value of X in N1.
*/
private Double __maxX1 = null;

/**
Mean of X in N1.
*/
private Double __meanX1 = null;

/**
Standard deviation of X in N1.
*/
private Double __stddevX1 = null;

// Data for overlapping Y (Y1)...

/**
The dependent data array, for points that overlap the independent data array.
*/
private double [] __y1 = new double[0];

/**
Minimum value of Y in N1.
*/
private Double __minY1 = null;

/**
Maximum value of Y in N1.
*/
private Double __maxY1 = null;

/**
Mean of Y in N1.
*/
private Double __meanY1 = null;

/**
Standard deviation of Y in N1.
*/
private Double __stddevY1 = null;

// Data for non-overlapping X (X2)...

/**
The independent data array, for points that DO NOT overlap the dependent data array.
*/
private double [] __x2 = new double[0];

/**
Mean of X in N2.
*/
private Double __meanX2 = null;

/**
Standard deviation of X in N2.
*/
private Double __stddevX2 = null;

// Data for non-overlapping dependent (Y3)...

/**
The dependent data array, for points that do not overlap the independent.
*/
private double [] __y3 = new double[0];

// Data for full X (X1 and X2)...

/**
The independent data array, for all points.
This is constructed from getX1() and getX2() if requested by the get method.
*/
private double [] __x = null;

/**
Mean of X in N1 + N2.
*/
private Double __meanX = null;

/**
Standard deviation of X in N1 + N2.
*/
private Double __stddevX = null;

// Data for full Y (Y1 and Y3)...

/**
The dependent data array, for all original points.
This is constructed from getY1() and getY3() if requested by the get method.
*/
private double [] __y = null;

/**
Mean of Y.
*/
private Double __meanY = null;

/**
Standard deviation of Y.
*/
private Double __stddevY = null;

/**
Constructor.  Set the independent and dependent data arrays, which should exactly overlap and not contain
missing values.  The data arrays are used to compute basic statistics such as mean and standard deviation for
each array, but only when the get methods are called.  Null arrays will be set to empty arrays.
@param x1 independent data array, for points that DO overlap the dependent data array
@param y1 dependent data array, for points that DO overlap the independent data array
@param x2 independent data array, for points that DO NOT overlap the dependent data array
@param y3 dependent data array, for points that DO NOT overlap the independent data array (this sample should
not be confused with the "2" designation)
*/
public RegressionData ( double [] x1, double [] y1, double [] x2, double y3[] )
{
    if ( x1 == null ) {
        __x1 = new double[0];
    }
    else {
        __x1 = x1;
    }
    if ( y1 == null ) {
        __y1 = new double[0];
    }
    else {
        __y1 = y1;
    }
    if ( x2 == null ) {
        x2 = new double[0];
    }
    else {
        __x2 = x2;
    }
    if ( y3 == null ) {
        y3 = new double[0];
    }
    else {
        __y3 = y3;
    }
    // The lengths of the overlapping arrays are required to be same
    if ( __x1.length != __y1.length ) {
        throw new IllegalArgumentException( "Independent and dependent arrays are not the same length." );
    }
    // Basic statistics will be calculated lazily (if requested)
}

/**
Return the maximum data value for the independent array in the N1 sample, or null if not analyzed.
@return the maximum data value for the independent array in the N1 sample, or null if not analyzed.
*/
public Double getMaxX1 ()
{   Double maxX1 = __maxX1;
    if ( maxX1 == null ) {
        maxX1 = MathUtil.max(getX1());
        setMaxX1 ( maxX1 );
    }
    return maxX1;
}

/**
Return the maximum data value for the dependent array in the N1 sample, or null if not analyzed.
@return the maximum data value for the dependent array in the N1 sample, or null if not analyzed.
*/
public Double getMaxY1 ()
{   Double maxY1 = __maxY1;
    if ( maxY1 == null ) {
        maxY1 = MathUtil.max(getY1());
        setMaxY1 ( maxY1 );
    }
    return maxY1;
}

/**
Return the mean for the independent array in the N1 + N2 sample, or null if not analyzed.
@return the mean for the independent array in the N1 + N2 sample, or null if not analyzed.
*/
public Double getMeanX ()
{   Double meanX = __meanX;
    if ( meanX == null ) {
        meanX = MathUtil.mean(getX());
        setMeanX ( meanX );
    }
    return meanX;
}

/**
Return the mean for the independent array in the N1 sample, or null if not analyzed.
@return the mean for the independent array in the N1 sample, or null if not analyzed.
*/
public Double getMeanX1 ()
{   Double meanX1 = __meanX1;
    if ( meanX1 == null ) {
        meanX1 = MathUtil.mean(getX1());
        setMeanX1 ( meanX1 );
    }
    return meanX1;
}

/**
Return the mean for the independent array in the N2 sample, or null if not analyzed.
@return the mean for the independent array in the N2 sample, or null if not analyzed.
*/
public Double getMeanX2 ()
{   Double meanX2 = __meanX2;
    if ( meanX2 == null ) {
        meanX2 = MathUtil.mean(getX2());
        setMeanX2 ( meanX2 );
    }
    return meanX2;
}

/**
Return the mean for the dependent array in the N1 + N2 sample, or null if not analyzed.
@return the mean for the dependent array in the N1 + N2 sample, or null if not analyzed.
*/
public Double getMeanY ()
{   Double meanY = __meanY;
    if ( meanY == null ) {
        meanY = MathUtil.mean(getY());
        setMeanY ( meanY );
    }
    return meanY;
}

/**
Return the mean for the dependent array in the N1 sample, or null if not analyzed.
@return the mean for the dependent array in the N1 sample, or null if not analyzed.
*/
public Double getMeanY1 ()
{   Double meanY1 = __meanY1;
    if ( meanY1 == null ) {
        meanY1 = MathUtil.mean(getY1());
        setMeanY1 ( meanY1 );
    }
    return meanY1;
}

/**
Return the minimum data value for the independent array in the N1 sample, or null if not analyzed.
@return the minimum data value for the independent array in the N1 sample, or null if not analyzed.
*/
public Double getMinX1 ()
{   Double minX1 = __minX1;
    if ( minX1 == null ) {
        minX1 = MathUtil.min(getX1());
        setMinX1 ( minX1 );
    }
    return minX1;
}

/**
Return the minimum data value for the dependent array in the N1 sample, or null if not analyzed.
@return the minimum data value for the dependent array in the N1 sample, or null if not analyzed.
*/
public Double getMinY1 ()
{   Double minY1 = __minY1;
    if ( minY1 == null ) {
        minY1 = MathUtil.min(getY1());
        setMinY1 ( minY1 );
    }
    return minY1;
}

/**
Return the full data size (N1 + N2).
*/
public int getN ()
{
    return getN1() + getN2();
}

/**
Return the size of the overlapping arrays.
*/
public int getN1 ()
{
    if ( __x1 == null ) {
        return 0;
    }
    else {
        return __x1.length;
    }
}

/**
Return the size of the non-overlapping independent (X2) array.
*/
public int getN2 ()
{
    if ( __x2 == null ) {
        return 0;
    }
    else {
        return __x2.length;
    }
}

/**
Return the size of the non-overlapping dependent array (Y3).
*/
public int getN3 ()
{
    if ( __y3 == null ) {
        return 0;
    }
    else {
        return __y3.length;
    }
}

/**
Return the standard deviation for the independent array in the N1 + N2 sample, or null if not analyzed.
@return the standard deviation for the independent array in the N1 + N2 sample, or null if not analyzed.
*/
public Double getStandardDeviationX ()
{   Double stddevX = __stddevX;
    if ( stddevX == null ) {
        stddevX = MathUtil.standardDeviation(getX());
        setStandardDeviationX( stddevX );
    }
    return stddevX;
}

/**
Return the standard deviation for the independent array in the N1 sample, or null if not analyzed.
@return the standard deviation for the independent array in the N1 sample, or null if not analyzed.
*/
public Double getStandardDeviationX1 ()
{   Double stddevX1 = __stddevX1;
    if ( stddevX1 == null ) {
        stddevX1 = MathUtil.standardDeviation(getX1());
        setStandardDeviationX1( stddevX1 );
    }
    return stddevX1;
}

/**
Return the standard deviation for the independent array in the N2 sample, or null if not analyzed.
@return the standard deviation for the independent array in the N2 sample, or null if not analyzed.
*/
public Double getStandardDeviationX2 ()
{   Double stddevX2 = __stddevX2;
    if ( stddevX2 == null ) {
        stddevX2 = MathUtil.standardDeviation(getX2());
        setStandardDeviationX2( stddevX2 );
    }
    return stddevX2;
}

/**
Return the standard deviation for the dependent array in the N1 + N2 sample, or null if not analyzed.
@return the standard deviation for the dependent array in the N1 + N2 sample, or null if not analyzed.
*/
//public Double getStandardDeviationY ()
//{   return __stddevY;
//}

/**
Return the standard deviation for the dependent array in the N1 sample, or null if not analyzed.
@return the standard deviation for the dependent array in the N1 sample, or null if not analyzed.
*/
public Double getStandardDeviationY1 ()
{   Double stddevY1 = __stddevY1;
    if ( stddevY1 == null ) {
        stddevY1 = MathUtil.standardDeviation(getY1());
        setStandardDeviationY1( stddevY1 );
    }
    return stddevY1;
}

/**
Return the full independent data array (X1 and X2).
*/
public double [] getX ()
{
    double [] x = __x;
    if ( x == null ) {
        // Allocate an array that is the size of X1 and X2
        x = new double[getN()];
        System.arraycopy(getX1(), 0, x, 0, getN1());
        System.arraycopy(getX2(), 0, x, getN1(), getN2());
        setX ( x );
    }
    return x;
}

/**
Return the independent data array that overlaps the dependent array.
*/
public double [] getX1 ()
{
    return __x1;
}

/**
Return the independent data array that DOES NOT overlap the dependent array.
*/
public double [] getX2 ()
{
    return __x2;
}

/**
Return the full dependent data array (Y1 and Y3).
*/
public double [] getY ()
{
    double [] y = __y;
    if ( y == null ) {
        // Allocate an array that is the size of Y1 and Y3
        y = new double[getN1() + getN3()];
        System.arraycopy(getY1(), 0, y, 0, getN1());
        System.arraycopy(getY3(), 0, y, getN3(), getN3());
        setY ( y );
    }
    return y;
}

/**
Return the dependent data array that overlaps the independent array.
*/
public double [] getY1 ()
{
    return __y1;
}

/**
Return the dependent data array that DOES NOT overlap the dependent array.
*/
public double [] getY3 ()
{
    return __y3;
}

/**
Set the maximum data value for the independent data in the N1 sample.
@param maxX1 Maximum data value for the independent data in the N1 sample.
*/
private void setMaxX1 ( Double maxX1 )
{   __maxX1 = maxX1;
}

/**
Set the maximum data value for the dependent data in the N1 sample.
@param maxY1 Maximum data value for the dependent data in the N1 sample.
*/
private void setMaxY1 ( Double maxY1 )
{   __maxY1 = maxY1;
}

/**
Set the mean for the independent data in the N1 + N2 sample.
@param meanX Mean for the independent data in the N1 + N2 sample.
*/
private void setMeanX ( Double meanX )
{   __meanX = meanX;
}

/**
Set the mean for the independent data in the N1 sample.
@param meanX1 Mean for the independent data in the N1 sample.
*/
private void setMeanX1 ( Double meanX1 )
{   __meanX1 = meanX1;
}

/**
Set the mean for the independent data in the N2 sample.
@param meanX2 Mean for the independent data in the N2 sample.
*/
private void setMeanX2 ( Double meanX2 )
{   __meanX2 = meanX2;
}

/**
Set the mean for the dependent data in the N1 + N3 sample.
@param meanY Mean for the dependent data in the N1 + N3 sample.
*/
private void setMeanY ( Double meanY )
{   __meanY = meanY;
}

/**
Set the mean for the dependent data in the N1 sample.
@param meanY1 Mean for the dependent data in the N1 sample.
*/
private void setMeanY1 ( Double meanY1 )
{   __meanY1 = meanY1;
}

/**
Set the minimum data value for the independent data in the N1 sample.
@param minX1 Minimum data value for the independent data in the N1 sample.
*/
private void setMinX1 ( Double minX1 )
{   __minX1 = minX1;
}

/**
Set the minimum data value for the dependent data in the N1 sample.
@param minY1 Minimum data value for the dependent data in the N1 sample.
*/
private void setMinY1 ( Double minY1 )
{   __minY1 = minY1;
}

/**
Set the standard deviation for the independent data in the N1 + N2 sample.
@param stddevX Standard deviation for the independent data in the N1 + N2 sample.
*/
private void setStandardDeviationX ( Double stddevX )
{   __stddevX = stddevX;
}

/**
Set the standard deviation for the independent data in the N1 sample.
@param stddevX1 Standard deviation for the independent data in the N1 sample.
*/
private void setStandardDeviationX1 ( Double stddevX1 )
{   __stddevX1 = stddevX1;
}

/**
Set the standard deviation for the independent data in the N2 sample.
@param stddevX2 Standard deviation for the independent data in the N2 sample.
*/
private void setStandardDeviationX2 ( Double stddevX2 )
{   __stddevX2 = stddevX2;
}

/**
Set the standard deviation for the dependent data in the N1 + N2 sample.
@param stddevY Standard deviation for the dependent data in the N1 + N2 sample.
*/
//private void setStandardDeviationY ( Double stddevY )
//{   __stddevY = stddevY;
//}

/**
Set the standard deviation for the dependent data in the N1 sample.
@param stddevY1 Standard deviation for the dependent data in the N1 sample.
*/
private void setStandardDeviationY1 ( Double stddevY1 )
{   __stddevY1 = stddevY1;
}

/**
Set the X array - this is only called by the getX() method if the array has not been constructed.
@param X X1 and X2.
*/
private void setX ( double [] x )
{   __x = x;
}

/**
Set the Y array - this is only called by the getY() method if the array has not been constructed.
@param y Y1 and Y3.
*/
private void setY ( double [] y )
{   __y = y;
}

}