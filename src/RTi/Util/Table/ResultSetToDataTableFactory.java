package RTi.Util.Table;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;

import RTi.DMI.DMI;
import RTi.Util.Message.Message;
import RTi.Util.Time.DateTime;

/**
This class will process an SQL ResultSet into a DataTable.
*/
public class ResultSetToDataTableFactory
{
    
/**
Constructor.
*/
public ResultSetToDataTableFactory ()
{
    
}

/**
Create a DataTable from a ResultSet.
@param dbengineType a value from DMI.DBENGINE_*, used for fine-grain handling
of column data type mapping.  Specify as -1 to ignore.
@param rs the ResultSet from an SQL query
@param tableID the identifier to use for the table
*/
public DataTable createDataTable ( int dbengineType, ResultSet rs, String tableID )
throws SQLException
{   String routine = getClass().getSimpleName() + ".createDataTable";
    DataTable table = new DataTable();
    table.setTableID ( tableID );
    // Define the table columns from the ResultSet metadata
    ResultSetMetaData meta = rs.getMetaData();
    int columnCount = meta.getColumnCount();
    String columnName;
    int columnType, precision, scale;
    int [] columnTypes = new int[columnCount];
    for ( int i = 1; i <= columnCount; i++ ) {
        columnName = meta.getColumnName(i);
        columnType = sqlToDMIColumnType(meta.getColumnType(i));
        columnTypes[i - 1] = columnType;
        precision = meta.getPrecision(i); // More like width
        scale = meta.getScale(i); // Precision for floating point
        //Message.printStatus(2,routine,"Column name=\"" + columnName + "\", sqlColumnType=" + meta.getColumnType(i) +
        //    ", tableColumnType=" + columnType +
        //    ", SQL precision (table width)=" + precision + ", SQL scale (table precision)=" + scale +
        //    ", displayWidth=" + meta.getColumnDisplaySize(i));
        // TODO SAM 2012-06-12
        // SQL Server behaves oddly in that the "scale" can be set to 0 (zero) but SQL Server Management Studio
        // still shows digits after the decimal point.  None of the properties on a column appear to be usable
        // to determine the number of digits to display.  To compensate, for now set the scale to 6 if
        // floating point and not specified
        if ( dbengineType == DMI.DBENGINE_SQLSERVER ) {
	        if ( ((columnType == TableField.DATA_TYPE_DOUBLE) || (columnType == TableField.DATA_TYPE_FLOAT)) &&
	            (scale == 0) ) {
	            scale = 6;
	        }
        }
        table.addField( new TableField(columnType,columnName,precision,scale), null);
    }
    // Transfer each record in the ResultSet to the table
    String s;
    double d;
    float f;
    int i;
    Date date;
    Array a;
    int baseType; // Used with Array.getBaseType(), the original SQL type
    int baseType2 = TableField.DATA_TYPE_STRING; // The internal type, after conversion from SQL type
    TableRecord rec = null;
    boolean isNull;
    while (rs.next()) {
        rec = new TableRecord(columnCount);
        for ( int iCol = 1; iCol <= columnCount; iCol++ ) {
            int i0 = iCol - 1;
            try {
                isNull = false;
                if ( columnTypes[i0] == TableField.DATA_TYPE_DATE ) {
                    date = rs.getTimestamp(iCol);
                    if (!rs.wasNull()) {
                    	DateTime dt = new DateTime(date);
                        rec.addFieldValue(dt);
                    }
                    else {
                        isNull = true;
                    }
                }
                else if ( columnTypes[i0] == TableField.DATA_TYPE_DOUBLE ) {
                    d = rs.getDouble(iCol);
                    if (!rs.wasNull()) {
                        rec.addFieldValue(new Double(d));
                    }
                    else {
                        isNull = true;
                    }
                }
                else if ( columnTypes[i0] == TableField.DATA_TYPE_FLOAT ) {
                    f = rs.getFloat(iCol);
                    if (!rs.wasNull()) {
                        rec.addFieldValue(new Float(f));
                    }
                    else {
                        isNull = true;
                    }
                }
                else if ( columnTypes[i0] == TableField.DATA_TYPE_INT ) {
                    i = rs.getInt(iCol);
                    if (!rs.wasNull()) {
                        rec.addFieldValue(new Integer(i));
                    }
                    else {
                        isNull = true;
                    }
                }
                else if ( columnTypes[i0] == TableField.DATA_TYPE_STRING ) {
                    s = rs.getString(iCol);
                    if (!rs.wasNull()) {
                        rec.addFieldValue(s.trim());
                    }
                    else {
                        isNull = true;
                    }
                }
                else if ( table.isColumnArray(columnTypes[i0]) ) {
                	// Column contains an array of other data, generally primitives
                    a = rs.getArray(iCol);
                    if (!rs.wasNull()) {
                    	baseType = a.getBaseType();
                    	if ( columnTypes[i0] == TableField.DATA_TYPE_ARRAY ) {
                    		// The column type does not yet have the base type so add...
                    		baseType2 = sqlToDMIColumnType(baseType);
                    		columnTypes[i0] = columnTypes[i0] + baseType2;
                    	}
                    	// Now need to interpret the base type...
                        if ( baseType == Types.DATE ) {
                        	// Know that the array will contain Date
                        	Date [] da = (Date [])(a.getArray());
                        	// Convert to DateTime objects
                        	DateTime [] dta = new DateTime[da.length];
                        	for ( int ic = 0; ic < da.length; ic++ ) {
                        		dta[ic] = new DateTime(da[ic]);
                        	}
                            rec.addFieldValue(dta);
                        }
                        else if ( baseType == Types.DOUBLE ) {
                        	double [] da = (double [])(a.getArray());
                            rec.addFieldValue(da);
                        }
                        else if ( baseType == Types.FLOAT ) {
                        	float [] fa = (float [])(a.getArray());
                            rec.addFieldValue(fa);
                        }
                        else if ( baseType == Types.INTEGER ) {
                        	int [] ia = (int [])(a.getArray());
                            rec.addFieldValue(ia);
                        }
                        else if ( baseType == Types.BIGINT ) {
                        	long [] la = (long [])(a.getArray());
                            rec.addFieldValue(la);
                        }
                        else if ( (baseType == Types.CHAR) || (baseType == Types.VARCHAR) || (baseType == Types.NVARCHAR) ) {
                        	String [] sa = (String [])(a.getArray());
                            rec.addFieldValue(sa);
                        }
                        else {
                        	// Don't know the type
                        	// TODO SAM 2015-09-06 Need to confirm handling of the above baseType
                        	isNull = true;
                        }
                    }
                    else {
                        isNull = true;
                    }
                }
                else {
                    // Default is string
                    s = rs.getString(iCol);
                    if (!rs.wasNull()) {
                        rec.addFieldValue(s.trim());
                    }
                    else {
                        isNull = true;
                    }
                }
                if ( isNull ) {
                    rec.addFieldValue(null);
                }
            }
            catch ( Exception e ) {
                // Leave as null
                Message.printWarning(3,routine,e);
            }
        }
        try {
            table.addRecord(rec);
        }
        catch ( Exception e ) {
            // Should not happen
            Message.printWarning(3,routine,e);
        }
    }
    return table;
}

/**
Lookup the SQL column type to the DataTable type.
@param sqlColumnType SQL column type from Types
@return DataTable column type from TableField
*/
private int sqlToDMIColumnType(int sqlColumnType)
{
    switch ( sqlColumnType ) {
    	case Types.ARRAY: return TableField.DATA_TYPE_ARRAY;
        case Types.BIGINT: return TableField.DATA_TYPE_LONG;
        // BINARY not handled
        case Types.BIT: return TableField.DATA_TYPE_INT;
        // BLOB not handled
        case Types.BOOLEAN: return TableField.DATA_TYPE_INT;
        case Types.CHAR: return TableField.DATA_TYPE_STRING;
        // CLOB not handled
        case Types.DATE: return TableField.DATA_TYPE_DATE;
        case Types.DECIMAL: return TableField.DATA_TYPE_DOUBLE;
        case Types.DOUBLE: return TableField.DATA_TYPE_DOUBLE;
        case Types.FLOAT: return TableField.DATA_TYPE_FLOAT;
        case Types.INTEGER: return TableField.DATA_TYPE_INT;
        case Types.LONGVARCHAR: return TableField.DATA_TYPE_STRING;
        case Types.NVARCHAR: return TableField.DATA_TYPE_STRING;
        case Types.NUMERIC: return TableField.DATA_TYPE_DOUBLE; // internally a BigDecimal - check the decimals to evaluate whether to use an integer
        case Types.REAL: return TableField.DATA_TYPE_DOUBLE;
        // REF not handled
        case Types.SMALLINT: return TableField.DATA_TYPE_INT;
        // STRUCT not handled
        case Types.TIME: return TableField.DATA_TYPE_DATE;
        case Types.TIMESTAMP: return TableField.DATA_TYPE_DATE;
        case Types.TINYINT: return TableField.DATA_TYPE_INT;
        case Types.VARCHAR: return TableField.DATA_TYPE_STRING;
        // VERBINARY not handled
        default:
            Message.printWarning(2,"sqlToDMIColumnType",
                "Unknown SQL type for conversion to table: " + sqlColumnType + ", using string.");
            return TableField.DATA_TYPE_STRING;
    }
}

}