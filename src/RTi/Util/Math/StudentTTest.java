package RTi.Util.Math;

import RTi.Util.Message.Message;

/**
Student T distribution.
*/
public class StudentTTest
{
    
/**
Compute student T quantile for single tail probability level and number of degrees of freedom.
This code was ported from the FORTRAN Mixed Station Model from Colorado's Decision Support Systems, reference:
G. W. HILL (1970) ACM ALGO 396.  COMM ACM 13(10)619-20, revised by WKIRBY 10/76, 2/79, 10/79.
@param p single-tail probability level.  For example, if checking for a 95% confidence level, the double-tail
probability level would be (1 - .95) = .05.  The single-tail probability level is therefore .025.
@param df number of degrees of freedom (must be >= 1).  For example, if checking the slope of a regression line,
use the sample size minus 2 (representing intercept and slope as estimating parameters).
@return T(P,N) = X such that prob(student T with DF <= X) = P.
NOTE - ABS(T) HAS PROB Q OF EXCEEDING STUTX( 1.-Q/2., N ).
@exception IllegalArgumentException if input is invalid
*/
public double getStudentTQuantile ( double p, int df )
{
    if ( df < 1 ) {
        throw new IllegalArgumentException ( "Number of degrees of freedom (" + df + ") must be >= 1.");
    }
    double HPI = Math.PI/2.0; // 1.5707963268; Original Mixed Station Model code
    // Commented out in original code - because it worked with double tail probability and this
    // version requires a single-tail probability p -
    // default is no negative sign applied anywhere because 2 tailed
    // IF(P.LT.0.5)SIGN=-1.
    double q = 2.*p; // q is the double-tail probability used by the original algorithm below
    /* TODO SAM 2010-06-24 not sure why the following is used - special case?
    if ( q > 1. ) {
        q = 2.*(1.-p);
    }
    if ( q >= 1. ) {
        return 0.;
    }
    */
    if ( (q <= 0.) || (q > 1.) ) {
        throw new IllegalArgumentException ( "Requested two-tailed probability (" + q + ") must be > 0 and <= 1.");
    }

    if ( df == 1 ) {
        // 1 degree of freedom - compute explicitly
        return 1.0/Math.tan(HPI*q);
    }
    else if ( df == 2 ) {
        // 2 degrees of freedom - compute explicitly
       return Math.sqrt(2.0/(q*(2.0-q))-2.0);
    }
    else {
        // EXPANSION FOR N > 2
        double a = 1.0/(df - 0.5);
        double b = 48.0/(a*a);
        double c = ((20700.*a/b - 98.)*a - 16.)*a + 96.36;
        double d = ((94.5/(b + c) - 3.)/b + 1.)*Math.sqrt(a*HPI)*df;
        double x = d*q;
        double y = Math.pow(x, 2.0/df);
        // TODO SAM 2010-06-24 Code below is not the same as the ACM paper
        if ( y <= a + .05 ) {
           y = ((1.0/(((df + 6.)/(df*y) - 0.089*d - 0.822)*(df + 2.)*3.)+ 0.5/(df + 4.))*y - 1.)*(df + 1.)/(df + 2.) + 1.0/y;
           return Math.sqrt(df*y);
        }
    
        // ASYMPTOTIC INVERSE EXPANSION ABOUT NORMAL
        x = GaussianDistribution.ab (0.5*q);
        Message.printStatus(2,"","Probability density function for " + .5*q + "=" + x );
        Message.printStatus(2,"","Probability density function for " + .5*.5*q + "=" + GaussianDistribution.ab (0.5*.5*q) );
        y = x*x;
        if ( df < 5 ) {
            c = c + 0.3*(df - 4.5)*(x + 0.6);
        }
        c = (((.05*d*x - 5.)*x - 7.)*x - 2.)*x + b + c;
        y = (((((0.4*y + 6.3)*y + 36.)*y + 94.5)/c - y - 3.)/b + 1.)*x;
        x = a*y*y;
        y = x + 0.5*x*x;
        if ( x > .002) {
            y = Math.exp(x) - 1.0;
        }
        return Math.sqrt(df*y);
    }
}

}