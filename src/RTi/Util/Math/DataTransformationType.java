package RTi.Util.Math;

/**
Data transformations that may be applied to data before analysis,
for example before performing a regression analysis.
*/
public enum DataTransformationType
{
    /**
    Data values are transformed by log10() prior to analysis.
    */
    LOG("Log"),
    /**
    Data values are not transformed prior to analysis.
    */
    NONE("None");
    
    private final String displayName;

    /**
     * Name that should be displayed in choices, etc.
     * @param displayName
     */
    private DataTransformationType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Return the display name.
     * @return the display name.
     */
    @Override
    public String toString() {
        return displayName;
    }
    
    /**
     * Return the enumeration value given a string name (case-independent).
     * @return the enumeration value given a string name (case-independent), or null if not matched.
     */
    public static DataTransformationType valueOfIgnoreCase(String name)
    {
        DataTransformationType [] values = values();
        for ( DataTransformationType t : values ) {
            if ( name.equalsIgnoreCase(t.toString()) ) {
                return t;
            }
        } 
        return null;
    }
}