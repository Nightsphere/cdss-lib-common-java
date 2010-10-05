// ----------------------------------------------------------------------------
// DataTable - class to hold tabular data from a database
// ----------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
// ----------------------------------------------------------------------------
// History:
//
// 23 Jun 1999	Catherine E.
//		Nutting-Lane, RTi	Initial version
// 2001-09-17	Steven A. Malers, RTi	Change the name of the class from Table
//					to DataTable to avoid conflict with the
//					existing C++ class.  Review code.
//					Remove unneeded messages to increase
//					performance.  Add get methods for fields
//					for use when writing table.  Make data
//					protected to allow extension to derived
//					classes (e.g., DbaseDataTable).
// 2001-10-04	SAM, RTi		Add getFormatFormat(int) to allow
//					operation on a single field.
// 2001-10-10	SAM, RTi		Add getFieldNames().
// 2001-10-12	SAM, RTi		By default, trim character fields.  Add
//					the trimStrings() method to allow an
//					override.  This data member should be
//					checked by the specific read code in
//					derived classes.  Also change the
//					format for strings to %- etc. because
//					strings are normally left justified.
// 2001-12-06	SAM, RTi		Change so that getNumberOfRecords()
//					returns the value of _num_records and
//					not _table_records.size().  The latter
//					produces errors when records are read
//					on the fly.  Classes that allow on-the-
//					fly reads will need to set the number of
//					records.
// 2002-07-27	SAM, RTi		Trim the column names when reading the
//					header.
// 2003-12-16	J. Thomas Sapienza, RTi	* Added code for writing a table out to
//					  a delimited file.
//					* Added code for dumping a table to 
//					  Status level 1 (for debugging).
//					* Added code to trim spaces from values
//					  read in from a table.
// 2003-12-18	JTS, RTi		Added deleteRecord().
// 2004-02-25	JTS, RTi		Added parseFile().
// 2004-03-11	JTS, RTi		Added isDirty().
// 2004-03-15	JTS, RTi		Commented out the DELIM_SKIP_BLANKS 
//					from the delimited file read, so to 
//					allow fields with no data.
// 2004-04-04	SAM, RTi		Fix bug where the first non-comment line
//					was being ignored.
// 2004-08-03	JTS, RTi		Added setFieldValue().
// 2004-08-05	JTS, RTi		Added version of parseDelimitedFile()
//					that takes a parameter specifying the 
//					max number of lines to read from the 
//					file.
// 2004-10-21	JTS, RTi		Added hasField().
// 2004-10-26	JTS, RTi		Added deleteField().
// 2005-01-03	JTS, RTi		* Added setFieldWidth()
//					* When a table is read in with
//					  parseDelimitedFile(), String columns
//					  are now checked for the longest string
//					  and the width of that column is set
//					  so that the entire string will
//					  be displayed.
// 2005-01-27	JTS, RTi		Corrected null pointer bug in 
//					parseDelimitedFile().
// 2005-11-16	SAM, RTi		Add MergeDelimiters and TrimInput
//					properties to parseDelimitedFile().
// 2006-03-02	SAM, RTi		Change so that when on the fly reading
//					is occurring, getTableRecord() returns
//					null.
// 2006-03-13	JTS, RTi		Correct bug so that parsed data tables
//					have _have_data set to true.
// 2006-06-21	SAM, RTi		Change so that when writing a delimited
//					file the contents are quoted if the data
//					contain the delimiter.
// 2007-05-08	SAM, RTi		Cleanup code based on Eclipse feedback.
// ----------------------------------------------------------------------------
// EndHeader

package RTi.Util.Table;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Date;
import java.util.List;
import java.util.Vector;

import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;

import RTi.Util.Message.Message;

import RTi.Util.String.StringUtil;

/**
This class contains records of data as a table, using a Vector of TableRecord
instances.  The format of the table is defined using the TableField class.
Tables can be used to store record-based data.  This class was originally
implemented to store Dbase files associated with ESRI shapefile GIS data.
Consequently, although a data table theoretically can store a variety of
data types (see TableField), in practice only String and double types are used
for some applications.
Full handling of other data types will be added in the future.
An example of a DataTable instantiation is:
<p>

<pre>
try {
	/// First, create define the table by assembling a vector of TableField objects...
	List<TableField> myTableFields = new Vector(3);
	myTableFields.add ( new TableField ( TableField.DATA_TYPE_STRING, "id_label_6", 12 ) );
	myTableFields.add ( new TableField ( TableField.DATA_TYPE_INT, "Basin", 12 ) );
	myTableFields.add ( new TableField ( TableField.DATA_TYPE_STRING, "aka", 12 ) );

	// Now define table with one simple call...
	DataTable myTable = new DataTable ( myTableFields );

	// Now define a record to be included in the table...
	TableRecord contents = new TableRecord (3);
	contents.addFieldValue ( "123456" );
	contents.addFieldValue ( new Integer (6));
	contents.addFieldValue ( "Station ID" );

	myTable.addRecord ( contents );

	// Get the 2nd field from the first record (fields and records are zero-index based)...
	system.out.println ( myTable.getFieldValue ( 0, 1 ));

} catch (Exception e ) {
	// process exception
}
</pre>

@see RTi.Util.Table.TableField
@see RTi.Util.Table.TableRecord
*/
public class DataTable
{
/**
The identifier for the table.
*/
private String __table_id = "";

/**
List of TableField that define the table columns.
*/
protected List<TableField> _table_fields;

/**
List of TableRecord, that contains the table data.
*/
protected List<TableRecord> _table_records;

/**
Number of records in the table (kept for case where records are not in memory).
*/
protected int _num_records = 0;

/**
Indicates if data records have been read into memory.  This can be reset by derived classes that
instead keep open a binary database file (e.g., dBase) and override the read/write methods.
*/
protected boolean _haveDataInMemory = true;

/**
Indicates whether string data should be trimmed on retrieval.  In general, this
should be true because older databases like Dbase pad data with spaces but seldom
are spaces actually actual data values.
*/
protected boolean _trim_strings = true;

/**
Indicates whether addRecord() has been called.  If so, assume that the data records
are in memory for calls to getNumberOfRecords(). Otherwise, just return the _num_records value.
 */
protected boolean _add_record_called = false;

/**
Construct a new table.  Use setTableFields() at a later time to define the table.
*/
public DataTable ()
{	// Estimate that 100 is a good increment for the data vector...
	initialize ( new Vector(), 10, 100 );
}

/**
Construct a new table.  The list of TableRecord will increment in size by 100.
@param tableFieldsVector a list of TableField objects defining table contents.
*/
public DataTable ( List<TableField> tableFieldsVector )
{	// Guess that 100 is a good increment for the data vector...
	initialize ( tableFieldsVector, 10, 100 );
}

/**
Construct a new table.
@param tableFieldsVector a vector of TableField objects defining table contents.
@param Vector_size Initial list size for the Vector holding records.  This
can be used to optimize performance.
@param Vector_increment Increment for the list holding records.  This
can be used to optimize performance.
*/
public DataTable ( List<TableField> tableFieldsVector, int Vector_size, int Vector_increment )
{	initialize ( tableFieldsVector, Vector_size, Vector_increment );
}

/**
Adds a record to the vector of TableRecords maintained in the DataTable.
@param new_record new record to be added.
@exception Exception when the number of fields in new_record is not equal to th
number of fields in the current TableField declaration.
*/
public void addRecord ( TableRecord new_record )
throws Exception
{	int num_table_fields = _table_fields.size();
	int num_new_record_fields = new_record.getNumberOfFields();
	_add_record_called = true;
	if ( num_new_record_fields == num_table_fields ) {
		_table_records.add ( new_record );
	}
	else {
        throw new Exception ( "Number of fields in the new record (" +
		num_new_record_fields + ") does not match current description of the table fields (" +
		num_table_fields + ")." );
	}
}

/**
Add a field to the table and each entry in TableRecord.  The field is added
at the end of the other fields.  The added fields are initialized with blank
strings or NaN, as appropriate.
@param tableField information about field to add.
@param initValue the initial value to set (can be null).
*/
public void addField ( TableField tableField, Object initValue )
{	_table_fields.add ( tableField );

	int num = _table_records.size();
	TableRecord tableRecord;
	for ( int i=0; i<num; i++ ) {
		tableRecord = _table_records.get(i);

		// Add element and set default to 0 or ""
		// these are ordered in the most likely types to optimize
		int data_type = tableField.getDataType();
		if ( data_type == TableField.DATA_TYPE_STRING ) {
			tableRecord.addFieldValue( initValue );
		}
		else if ( data_type == TableField.DATA_TYPE_INT ) {
			tableRecord.addFieldValue( initValue );
		}
		else if ( data_type == TableField.DATA_TYPE_DOUBLE ) {
			tableRecord.addFieldValue( initValue );
		}
		else if ( data_type == TableField.DATA_TYPE_SHORT ) {
			tableRecord.addFieldValue( initValue );
		}
		else if ( data_type == TableField.DATA_TYPE_FLOAT ) {
			tableRecord.addFieldValue( initValue );
		}
	}
	tableRecord = null;
}

/**
Create a copy of the table.
*/
public DataTable createCopy ( DataTable table, String newTableID, String [] reqIncludeColumns )
{   String routine = getClass().getName() + ".createCopy";
    // List of columns that will be copied
    String [] columnNames = null;
    if ( (reqIncludeColumns != null) && (reqIncludeColumns.length > 0) ) {
        // Copy only the requested names
        columnNames = reqIncludeColumns;
    }
    else {
        // Copy all
        columnNames = table.getFieldNames();
    }
    // Create a new data table
    DataTable newTable = new DataTable();
    newTable.setTableID ( newTableID );
    // Get the column information from the original table
    int errorCount = 0;
    for ( int iField = 0; iField < table.getNumberOfFields(); iField++ ) {
        for ( int iReqField = 0; iReqField < columnNames.length; iReqField++ ) {
            if ( table.getFieldName(iField).equalsIgnoreCase(columnNames[iReqField])) {
                // Copy the data from the original table
                newTable.addField(new TableField ( table.getTableField(iField)), null );
                int icol = newTable.getNumberOfFields() - 1;
                for ( int irow = 0; irow < table.getNumberOfRecords(); irow++ ) {
                    try {
                        newTable.setFieldValue(irow, icol, table.getFieldValue(irow, iField), true );
                    }
                    catch ( Exception e ) {
                        // Should not happen
                        Message.printWarning(3, routine, "Error setting new table data (" + e + ")." );
                        ++errorCount;
                    }
                }
                // No need to keep looking for a matching column
                break;
            }
        }
    }
    if ( errorCount > 0 ) {
        throw new RuntimeException ( "There were + " + errorCount + " errors transferring data to new table." );
    }
    return newTable;
}

/**
Deletes a field and all the field's data from the table.
@param fieldNum the number of the field to delete.
*/
public void deleteField(int fieldNum) 
throws Exception
{
	if (fieldNum < 0 || fieldNum > (_table_fields.size() - 1)) {
		throw new Exception ("Field number " + fieldNum + " out of bounds.");
	}
	_table_fields.remove(fieldNum);

	int size = _table_records.size();
	TableRecord record = null;
	for (int i = 0; i < size; i++) {
		record = _table_records.get(i);
		record.deleteField(fieldNum);
	}
}

/**
Deletes a record from the table.
@param recordNum the number of the record to delete.
*/
public void deleteRecord(int recordNum) 
throws Exception {
	if (recordNum < 0 || recordNum > (_table_records.size() - 1)) {
		throw new Exception ("Record number " + recordNum + " out of bounds.");
	}
	
	_table_records.remove(recordNum);
}

/**
Dumps a table to Status level 1.
@param delimiter the delimiter to use.
@throws Exception if an error occurs.
*/
public void dumpTable(String delimiter) 
throws Exception
{
	String routine = "DataTable.dumpTable";
	int cols = getNumberOfFields();
	int rows = getNumberOfRecords();
	String rowPlural = "s";
	if (rows == 1) {
		rowPlural = "";
	}
	String colPlural = "s";
	if (cols == 1) {
		colPlural = "";
	}
	Message.printStatus(1, "", "Table has " + rows + " row" + rowPlural
		+ " and " + cols + " column" + colPlural + ".");
		
	if (cols == 0) {
		Message.printWarning(2, routine, "Table has 0 columns!  Nothing will be written.");
		return;
	}

	String line = null;

	line = "";
	for (int col = 0; col < (cols - 1); col++) {
		line += getFieldName(col) + delimiter;
	}
	line += getFieldName((cols - 1));
	Message.printStatus(1, "", line);
	
	for (int row = 0; row < rows; row++) {
		line = "";
		for (int col = 0; col < (cols - 1); col++) {
			line += "" + getFieldValue(row, col) + delimiter;
		}
		line += getFieldValue(row, (cols - 1));

		Message.printStatus(2, "", line);
	}
}

/**
Copies a DataTable.
@param originalTable the table to be copied.
@param cloneData if true, the data in the table will be cloned.  If false, both
tables will have pointers to the same data.
@return the new copy of the table.
*/
public static DataTable duplicateDataTable(DataTable originalTable, boolean cloneData)
{
	String routine = "DataTable.duplicateDataTable";
	
	DataTable newTable = null;
	int numFields = originalTable.getNumberOfFields();

	TableField field = null;
	TableField newField = null;
	List<TableField> tableFields = new Vector();
	for (int i = 0; i < numFields; i++) {
		field = originalTable.getTableField(i);
		newField = new TableField(field.getDataType(), 
			new String(field.getName()), field.getWidth(), field.getPrecision());
		tableFields.add(newField);
	}
	newTable = new DataTable(tableFields);
	if (!cloneData) {
		return newTable;
	}
	newTable._haveDataInMemory = true;

	int numRecords = originalTable.getNumberOfRecords();
	int type = -1;
	TableRecord newRecord = null;
	for (int i = 0; i < numRecords; i++) {
		try {
    		newRecord = new TableRecord(numFields);
    		for (int j = 0; j < numFields; j++) {
    			type = newTable.getFieldDataType(j);
    			if (type == TableField.DATA_TYPE_INT) {
    	        	newRecord.addFieldValue(new Integer(((Integer)originalTable.getFieldValue(i, j)).intValue()));
    			}
    			else if (type == TableField.DATA_TYPE_SHORT) {
    	        	newRecord.addFieldValue(new Short(((Short)originalTable.getFieldValue(i, j)).shortValue()));
    			}
    			else if (type == TableField.DATA_TYPE_DOUBLE) {
    	        	newRecord.addFieldValue(new Double(((Double)originalTable.getFieldValue(i, j)).doubleValue()));
    			}
    			else if (type == TableField.DATA_TYPE_FLOAT) {
    	        	newRecord.addFieldValue(new Float(((Float)originalTable.getFieldValue(i, j)).floatValue()));
    			}
    			else if (type == TableField.DATA_TYPE_STRING) {
    	        	newRecord.addFieldValue(new String((String)originalTable.getFieldValue(i, j)));
    			}
    			else if (type == TableField.DATA_TYPE_DATE) {
    	        	newRecord.addFieldValue( ((Date)originalTable.getFieldValue(i, j)).clone());
    			}
    		}
    		newTable.addRecord(newRecord);
		}
		catch (Exception e) {
			Message.printWarning(2, routine, "Error adding record " + i + " to table.");
			Message.printWarning(2, routine, e);
		}
	}
	return newTable;
}

/**
Return a new TableRecord that is compatible with this table, where all values are null.  This is useful
for inserting new table records where only specific column value is known, in which case the record can be
modified with TableRecord.setFieldValue().
@return a new record with null objects in each value.
*/
public TableRecord emptyRecord ()
{
    TableRecord newRecord = new TableRecord();
    int nCol = getNumberOfFields();
    for ( int i = 0; i < nCol; i++ ) {
        newRecord.addFieldValue( null );
    }
    return newRecord;
}

/**
Clean up before garbage collection.
*/
protected void finalize()
throws Throwable
{	_table_fields = null;
	_table_records = null;
	super.finalize();
}

/**
Used internally to determine whether a field name is already present in a 
table's fields, so as to avoid duplication.
@param tableFields a list of the tableFields created so far for a table.
@param name the name of the field to check.
@return true if the field name already is present in the table fields, false if not.
*/
private static boolean findPreviousFieldNameOccurances(List<TableField> tableFields, String name) {
	int size = tableFields.size();
	TableField field = null;
	String fieldName = null;
	for (int i = 0; i < size; i++) {
		field = tableFields.get(i);
		fieldName = field.getName();
		if (name.equals(fieldName)) {
			return true;
		}
	}
	return false;
}

/**
Return the field data type, given an index.
@return Data type for specified zero-based index.
@param index field index.
*/
public int getFieldDataType ( int index )
{	return (_table_fields.get ( index )).getDataType();
}

/**
Return the field data types for all of the fields.  This is useful because
code that processes all the fields can request the information once and then re-use.
@return Data types for all fields, in an integer array or null if no fields.
*/
public int[] getFieldDataTypes ()
{	int size = getNumberOfFields();
	if ( size == 0 ) {
		return null;
	}
	int types[] = new int[size];
	for ( int i = 0; i < size; i++ ) {
		types[i] = getFieldDataType(i);
	}
	return types;
}

/**
Get C-style format specifier that can be used to format field values for
output.  This format can be used with StringUtil.formatString().  All fields
formats are set to the full width and precision defined for the field.  Strings
are left-justified and numbers are right justified.
@return a String format specifier.
@param index Field index (zero-based).
*/
public String getFieldFormat ( int index )
{	int field_type = getFieldDataType(index);
    int fieldWidth = getFieldWidth(index);
	if ( field_type == TableField.DATA_TYPE_STRING ) {
		// Output left-justified and padded...
	    if ( fieldWidth < 0 ) {
	        // Variable width strings
	        return "%-s";
	    }
	    else {
	        return "%-" + fieldWidth + "." + getFieldWidth(index) + "s";
	    }
	}
	else {
        if ( (field_type == TableField.DATA_TYPE_FLOAT) || (field_type == TableField.DATA_TYPE_DOUBLE) ) {
			return "%" + fieldWidth + "." + getFieldPrecision(index) + "f";
		}
		else {
		    return "%" + fieldWidth + "d";
		}
	}
}

/**
Get C-style format specifiers that can be used to format field values for
output.  These formats can be used with StringUtil.formatString().
@return a String array with the format specifiers.
*/
public String[] getFieldFormats()
{	int nfields = getNumberOfFields();
	String [] format_spec = new String[nfields];
	for ( int i = 0; i < nfields; i++ ) {
		format_spec[i] = getFieldFormat ( i );
	}
	return format_spec;
}

/**
Return the field index associated with the given field name.
@return Index of table entry associated with the given field name.
@param field_name Field name to look up.
@exception Exception if the field name is not found.
*/
public int getFieldIndex ( String field_name )
throws Exception
{	int num = _table_fields.size();
	for ( int i=0; i<num; i++ ) {
		if ((_table_fields.get(i)).getName().equalsIgnoreCase(field_name)) {
			return i;
        }
	}

	// if this line is reached, the given field was never found
	throw new Exception( "Unable to find table field with name \"" + field_name + "\"" );
}

/**
Return the field name, given an index.
@return Field name for specified zero-based index.
@param index field index.
*/
public String getFieldName ( int index )
{	return (_table_fields.get ( index )).getName();
}

/**
Return the field names for all fields.
@return a String array with the field names.
*/
public String[] getFieldNames ()
{	int nfields = getNumberOfFields();
	String [] field_names = new String[nfields];
	for ( int i = 0; i < nfields; i++ ) {
		field_names[i] = getFieldName ( i );
	}
	return field_names;
}

/**
Return the field precision, given an index.
@return Field precision for specified zero-based index.
@param index field index.
*/
public int getFieldPrecision ( int index )
{	return (_table_fields.get ( index )).getPrecision();
}

/**
Return the field value for the requested record and field name.
The overloaded method that takes integers should be called for optimal
performance (so the field name lookup is avoided).
@param record_index zero-based index of record
@param field_name Field name of field to read.
@return field value for the specified field name of the specified record index
The returned object must be properly cast.
*/
public Object getFieldValue ( long record_index, String field_name )
throws Exception
{	return getFieldValue ( record_index, getFieldIndex(field_name) );
}

/**
Return the field value for the requested record and field index.  <b>Note that
this method can be overruled to implement on-the-fly data reads.  For example,
the DbaseDataTable class overrules this method to allow data to be read from the
binary Dbase file, as needed, at run-time, rather than reading from memory.  In
this case, the haveData() method can be used to indicate if data should be
taken from memory (using this method) or read from file (using a derived class method).</b>
@param record_index zero-based index of record
@param field_index zero_based index of desired field
@return field value for the specified index of the specified record index
The returned object must be properly cast.
*/
public Object getFieldValue ( long record_index, int field_index )
throws Exception
{	int num_recs = _table_records.size();
	int num_fields = _table_fields.size();

	if ( num_recs <= record_index ) {
		throw new Exception ( "Requested record index " + record_index +
		" is not available (only " + num_recs + " are available)." );
	}

	if ( num_fields <= field_index ) {
		throw new Exception ( "Requested field index " + field_index +
		" is not available (only " + num_fields + " have been established." );
	}

	TableRecord tableRecord = _table_records.get((int)record_index);
	Object o = tableRecord.getFieldValue(field_index);
	tableRecord = null;
	return o;
}

/**
Return the field width, given an index.
@return Field width for specified zero-based index.
@param index field index.
*/
public int getFieldWidth ( int index )
{	return (_table_fields.get ( index )).getWidth();
}

/**
Return the number of fields in the table.
@return number of fields in the table.
*/
public int getNumberOfFields ()
{	return _table_fields.size();
}

// TODO SAM 2010-09-22 Evaluate whether the records list size should be returned if records in memory?
/**
Return the number of records in the table.  <b>This value should be set by
code that manipulates the data table.  If the table records Vector has been
manipulated with a call to addRecord(), the size of the Vector will be returned.
Otherwise, the setNumberOfRecords() methods should be called appropriately and
its the value that is set will be returned.  This latter case
will be in effect if tables are being read on-the-fly.</b>
@return number of records in the table.
*/
public int getNumberOfRecords ()
{	if ( _add_record_called ) {
		return _table_records.size();
	}
	else {
	    return _num_records;
	}
}

/**
Return the TableRecord at a record index.
@param record_index Record index (zero-based).
@return TableRecord at specified record_index
*/
public TableRecord getRecord ( int record_index )
throws Exception
{	if ( !_haveDataInMemory ) {
		// Most likely a derived class is not handling on the fly
		// reading of data and needs more development.  Return null
		// because the limitation is likely handled elsewhere.
		return null;
	}
	if ( _table_records.size() <= record_index ) {
		throw new Exception ( 
		"Unable to return TableRecord at index [" + record_index +
		"].  Max value allowed is " + (_table_records.size() - 1) +".");
	}
	return (_table_records.get(record_index));
}

/**
Return the TableRecord for the given column and column value.
@param columnName name of column (field), case-insensitive.
@param columnValue column value to match in the records.  The first matching record is returned.
The type of the object will be checked before doing the comparison.
@return TableRecord matching the specified column value.
*/
public TableRecord getRecord ( String columnName, Object columnValue )
throws Exception
{   if ( !_haveDataInMemory ) {
        // Most likely a derived class is not handling on the fly
        // reading of data and needs more development.  Return null
        // because the limitation is likely handled elsewhere.
        return null;
    }
    // Figure out which column to search
    int columnNumber = getFieldIndex ( columnName );
    // Convert the item to search for to a specific object type
    String columnValueString = null;
    Double columnValueDouble = null;
    Float columnValueFloat = null;
    Integer columnValueInteger = null;
    Short columnValueShort = null;
    Date columnValueDate = null;
    if ( columnValue instanceof String ) {
        columnValueString = (String)columnValue;
    }
    else if ( columnValue instanceof Double ) {
        columnValueDouble = (Double)columnValue;
    }
    else if ( columnValue instanceof Float ) {
        columnValueFloat = (Float)columnValue;
    }
    else if ( columnValue instanceof Integer ) {
        columnValueInteger = (Integer)columnValue;
    }
    else if ( columnValue instanceof Short ) {
        columnValueShort = (Short)columnValue;
    }
    else if ( columnValue instanceof Date ) {
        columnValueDate = (Date)columnValue;
    }
    int size = _table_records.size();
    TableRecord rec = null;
    Object columnContents;
    for ( int i = 0; i < size; i++ ) {
        rec = _table_records.get(i);
        columnContents = rec.getFieldValue(columnNumber);
        if ( columnValueString != null ) {
            // Do case insensitive comparison
            String s = (String)columnContents;
            if ( s.equalsIgnoreCase(columnValueString)) {
                return rec;
            }
        }
        // TODO SAM 2009-07-26 Evaluate if more elegant approach is OK
        // Although could use generic "equals" do the following to ensure that expected types are used, and
        // throw exception if casts are wrong
        else if ( columnValueDouble != null ) {
            Double d = (Double)columnContents;
            if ( d.equals(columnValueDouble)) {
                return rec;
            }
        }
        else if ( columnValueFloat != null ) {
            Float f = (Float)columnContents;
            if ( f.equals(columnValueFloat)) {
                return rec;
            }
        }
        else if ( columnValueInteger != null ) {
            Integer ii = (Integer)columnContents;
            if ( ii.equals(columnValueInteger)) {
                return rec;
            }
        }
        else if ( columnValueShort != null ) {
            Short s = (Short)columnContents;
            if ( s.equals(columnValueShort)) {
                return rec;
            }
        }
        else if ( columnValueDate != null ) {
            Date d = (Date)columnContents;
            if ( d.equals(columnValueDate)) {
                return rec;
            }
        }
    }
    // Not found
    return null;
}

/**
Return the table identifier.
@return the table identifier.
*/
public String getTableID ()
{
    return __table_id;
}

/**
Return the list of TableRecords.
@return vector of TableRecord.
*/
public List<TableRecord> getTableRecords ( )
{	return _table_records;
}

/**
Return the TableField object for the requested column.
@param index Table field index (zero-based).
@return TableField object for the specified zero-based index.
*/
public TableField getTableField ( int index )
{	return (_table_fields.get( index ));
}

/**
Get the data type for the field.
@return the data type for the field (see TableField.DATA_TYPE_*).
@param index index of field (zero-based).
@exception If the index is out of range.
*/
public int getTableFieldType ( int index )
throws Exception
{	if ( _table_fields.size() <= index ) {
		throw new Exception ( "Index " + index + " is not valid." );
	}
	TableField tableField = _table_fields.get(index);
	int type = tableField.getDataType ();
	tableField = null;
	return type;
}

/**
Return the unique field values for the requested field index.  This is used,
for example, when displaying unique values on a map display.  The calling code
will need to cast the returned objects appropriately.  The performance of this
operation will degrade if a large number of unique values are present.  This
should not normally be the case if the end-user is intelligent about their
choice of the field that is being analyzed.
@param field_index zero_based index of desired field
@return Simple array (e.g., double[]) of unique data values from the field.
Depending on the field data type, a double[], int[], short[], or String[] will be returned.
@exception if the field index is not in the allowed range.
*/
/* TODO SAM Implement this later.
public Object getUniqueFieldValues ( int field_index )
throws Exception
{	int num_recs = _table_records.size();
	int num_fields = _table_fields.size();

	if ( num_fields <= field_index ) {
		throw new Exception ( "Requested field index " + field_index +
		" is not available (only " + num_fields +
		" are available)." );
	}

	// Use a temporary vector to get the unique values...
	Vector u = new Vector ( 100, 100 );

	// Determine the field type...
	int field_type = getTableFieldType ( field_index );
	//String rtn = "getFieldValue";
	//Message.printStatus ( 10, rtn, "Getting table record " +
	//	record_index + " from " + num_recs + " available records." );
	TableRecord tableRecord = null;
	Object o = null;
	for ( int i = 0; i < num_recs; i++ ) {
		tableRecord = (TableRecord)_table_records.elementAt(i);
		o = tableRecord.getFieldValue(field_index);
		// Now search through the list of known unique values...
		usize = u.size();
		for ( j = 0; j < usize; j++ ) {
		}
	}
	// Now return the values in an array of the appropriate type...
}
*/

/**
Checks to see if the table has a field with the given name.
@param fieldName the name of the field to check for (case-sensitive).
@return true if the table has the field, false otherwise.
*/
public boolean hasField(String fieldName) {
	String[] fieldNames = getFieldNames();
	for (int i = 0; i < fieldNames.length; i++) {
		if (fieldNames[i].equals(fieldName)) {
			return true;
		}
	}
	return false;
}


/**
Indicate whether the table has data in memory.  This will be true if any table records
have been added during a read or write operation.  This method is meant to be called by derived classes
that allow records to be accessed on the fly rather than from memory (e.g., dBase tables).
*/
public boolean haveDataInMemory ()
{	return _haveDataInMemory;
}

/**
Initialize the data.
@param tableFieldsVector list of TableField used to define the DataTable.
@param listSize Initial list size for the Vector holding records.
@param sizeIncrement Increment for the list holding records.
*/
private void initialize ( List<TableField> tableFieldsVector, int listSize, int sizeIncrement )
{	_table_fields = tableFieldsVector;
	_table_records = new Vector ( 10, 100 );
}

/**
Returns whether any of the table records are dirty or not.
@return whether any of the table records are dirty or not.
*/
public boolean isDirty() {
	TableRecord record = null;
	int recordCount = getNumberOfRecords();

	for (int i = 0; i < recordCount; i++) {
		record = _table_records.get(i);
		if (record.isDirty()) {
			return true;
		}
	}
	return false;
}

/**
Given a definition of what data to expect, read a simple delimited file and
store the data in a table.  Comment lines start with # and are not considered part of the header.
@return new DataTable containing data.
@param filename name of file containing delimited data.
@param delimiter string representing delimiter in data file (typically a comma).
@param tableFields list of TableField objects defining data expectations.
@param num_lines_header number of lines in header (typically 1).  The header
lines are read and ignored.
@exception Exception if there is an error parsing the file.
*/
public static DataTable parseDelimitedFile ( String filename, String delimiter,
    List<TableField> tableFields, int num_lines_header )
throws Exception {
	return parseDelimitedFile(filename, delimiter, tableFields,	num_lines_header, false);
}

/**
Given a definition of what data to expect, read a simple delimited file and
store the data in a table.  Comment lines start with # and are not considered part of the header.
@return new DataTable containing data.
@param filename name of file containing delimited data.
@param delimiter string representing delimiter in data file (typically a comma).
@param tableFields list of TableField objects defining data expectations.
@param num_lines_header number of lines in header (typically 1).  The header
lines are read and ignored.
@param trim_spaces if true, then when a column value is read between delimiters,
it will be .trim()'d before being parsed into a number or String. 
@exception Exception if there is an error parsing the file.
*/
public static DataTable parseDelimitedFile ( String filename, String delimiter, List<TableField> tableFields,
	int num_lines_header, boolean trim_spaces)
throws Exception {
	return parseDelimitedFile(filename, delimiter, tableFields, num_lines_header, trim_spaces, -1);
}

/**
Given a definition of what data to expect, read a simple delimited file and
store the data in a table.  Comment lines start with # and are not considered part of the header.
This method may not be maintained in the future.
The parseFile() method is more flexible.
@return new DataTable containing data.
@param filename name of file containing delimited data.
@param delimiter string representing delimiter in data file (typically a comma).
@param tableFields list of TableField objects defining data expectations.
@param num_lines_header number of lines in header (typically 1).  The header
lines are read and ignored.
@param trim_spaces if true, then when a column value is read between delimiters,
it will be .trim()'d before being parsed into a number or String. 
@param maxLines the maximum number of lines to read from the file.  If less than
or equal to 0, all lines will be read.
@exception Exception if there is an error parsing the file.
*/
public static DataTable parseDelimitedFile ( String filename, String delimiter, List<TableField> tableFields,
	int num_lines_header, boolean trim_spaces, int maxLines)
throws Exception
{
	String iline;
	boolean processed_header = false;
	List<String> columns;
	int num_fields=0, num_lines_header_read=0;
	int lineCount = 0;
	DataTable table;

	BufferedReader in = new BufferedReader ( new FileReader ( filename ));

	table = new DataTable( tableFields );
	table._haveDataInMemory = true;
	int field_types[] = table.getFieldDataTypes();
	if ( num_lines_header == 0 ) {
		processed_header = true;
		num_fields = field_types.length;
	}

	String col = null;

	// Create an array to use for determining the maximum size of all the
	// fields that are Strings.  This will be used to set the width of
	// the data values for those fields so that the width of the field is
	// equal to the width of the longest string.  This is mostly important
	// for when the table is to be placed within a DataTable_TableModel, 
	// so that the String field data are not truncated.
	int numFields = tableFields.size();
	int[] stringLengths = new int[numFields];
	for (int i = 0; i < numFields; i++) {	
		stringLengths[i] = 0;
	}
	int length = 0;

	while (( iline = in.readLine ()) != null ) {
		// check if read comment or empty line
		if ( iline.startsWith("#") || iline.trim().length()==0) {
			continue;
		}

		columns = StringUtil.breakStringList ( iline, delimiter, StringUtil.DELIM_ALLOW_STRINGS);

		// line is part of header ... 
		if ( !processed_header ) {
			num_fields = columns.size();
			if ( num_fields < tableFields.size() ) {
				throw new IOException ( "Table fields specifications do not match data found in file." );
			}
			
			num_lines_header_read++;
			if ( num_lines_header_read == num_lines_header ) {
				processed_header = true;
			}
		}
		else {
		    // line contains data - store in table as record
			TableRecord contents = new TableRecord(num_fields);
			try {						
    			for ( int i=0; i<num_fields; i++ ) {
    				col = columns.get(i);
    				if (trim_spaces) {
    					col = col.trim();
    				}
    				if ( field_types[i] == TableField.DATA_TYPE_STRING ) {
    					contents.addFieldValue(col);
    					length = col.length();
    					if (length > stringLengths[i]) {
    						stringLengths[i] = length;
    					}
    				}
    				else if ( field_types[i] ==	TableField.DATA_TYPE_DOUBLE ){
    					contents.addFieldValue(	new Double(col));
    				}
    				else if ( field_types[i] ==	TableField.DATA_TYPE_INT ) {
    					contents.addFieldValue(	new Integer(col));
    				}
    				else if ( field_types[i] ==	TableField.DATA_TYPE_SHORT ) {
    					contents.addFieldValue(	new Short(col));
    				}
    				else if ( field_types[i] ==	TableField.DATA_TYPE_FLOAT ) {
    					contents.addFieldValue(	new Float(col));
    				}
    			}
    			table.addRecord ( contents );
    			contents = null;
			} catch ( Exception e ) {
				if (IOUtil.testing()) {
					e.printStackTrace();
				}
				Message.printWarning ( 2, "DataTable.parseDelimitedFile", e );
			}
		}
		lineCount++;
		if (maxLines > 0 && lineCount >= maxLines) {
			in.close();

			// Set the widths of the string fields to the length
			// of the longest strings within those fields
			for (int i = 0; i < num_fields; i++) {
				col = columns.get(i);
				if (field_types[i] == TableField.DATA_TYPE_STRING) {
					table.setFieldWidth(i, stringLengths[i]);
				}
			}
							
			return table;
		}
	}
	in.close();
	return table;
}

/**
Reads the header of a comma-delimited file and return Vector of TableField objects.
@return list of TableField objects (only field names will be set).
@param filename name of file containing delimited data.
*/
public static List<TableField> parseDelimitedFileHeader ( String filename )
throws Exception
{	return parseDelimitedFileHeader ( filename, "," );
}

/**
Reads the header of a delimited file and return vector of TableField objects.
The field names will be correctly returned.  The data type, however, will be set
to TableField.DATA_TYPE_STRING.  This should be changed if not appropriate.
@return list of TableField objects (field names will be correctly set but data type will be string).
@param filename name of file containing delimited data.
@param delimiter string representing delimiter in data file.
@exception Exception if there is an error reading the file.
*/
public static List<TableField> parseDelimitedFileHeader ( String filename, String delimiter )
throws Exception
{	String iline;
	List<String> columns;
	List<TableField> tableFields = null;
	int num_fields=0;
	TableField newTableField = null;

	BufferedReader in = new BufferedReader ( new FileReader ( filename ));

	try {
    	while (( iline = in.readLine ()) != null ) {
    
    		// check if read comment or empty line
    		if ( iline.startsWith("#") || iline.trim().length()==0) {
    			continue;
    		}
    
    		columns = StringUtil.breakStringList ( iline, delimiter, 0);
    //			StringUtil.DELIM_SKIP_BLANKS );
    
    		num_fields = columns.size();
    		tableFields = new Vector ( num_fields, 1 );
    		for ( int i=0; i<num_fields; i++ ) {
    			newTableField = new TableField ( );
    			newTableField.setName (	columns.get(i).trim());
    			newTableField.setDataType(TableField.DATA_TYPE_STRING);
    			tableFields.add ( newTableField );
    		}
    		break;
    	}
	}
	finally {
	    if ( in != null ) {
	        in.close();
	    }
	}
	return tableFields;
}

/**
Parses a file and returns the DataTable for the file.  Currently only does
delimited files, and the data type for a column must be consistent.
The lines in delimited files do not need to all have the same
number of columns: the number of columns in the returned DataTable will be 
the same as the line in the file with the most delimited columns, all others
will be padded with empty value columns on the right of the table.
@param filename the name of the file from which to read the table data.
@param props a PropList with settings for how the file should be read and handled.<p>
Properties and their effects:<br>
<table width=100% cellpadding=10 cellspacing=0 border=2>
<tr>
<td><b>Property</b></td>    <td><b>Description</b></td> <td><b>Default</b></td>
</tr>

<tr>
<td><b>ColumnDataTypes</b></td>
<td>The data types for the column, either "Auto" (determine from column contents,
"AllStrings" (all are strings, the default from historical behavior), or a list of
data types (to be implemented in the future).
Lines starting with this character are skipped (TrimInput is applied after checking for comments).</td>
<td>AllStrings.</td>
</tr>

<tr>
<td><b>CommentLineIndicator</b></td>
<td>The characters with which comment lines begin.
Lines starting with this character are skipped (TrimInput is applied after checking for comments).</td>
<td>No default.</td>
</tr>

<tr>
<td><b>Delimiter</b></td>
<td>The character (s) that should be used to delimit fields in the file.  Fields are broken
using the following StringUtil.breakStringList() call (the flag can be modified by MergeDelimiters):<br>
<blockquote>
    v = StringUtil.breakStringList(line, delimiters, 0);
</blockquote><br></td>
<td>Comma (,).</td>
</tr>

<tr>
<td><b>FixedFormat</b></td>
<td>"True" or "False".  Currently ignored.</td>
<td></td>
</tr>

<tr>
<td><b>HeaderLines (previously HeaderRows)</b></td>
<td>The lines containing the header information, specified as single number or a range (e.g., 2-3).
Multiple lines will be separated with a newline when displayed, or Auto to automatically treat the
first non-comment row as a header if the value is double-quoted.</td>
<td>Auto</td>
</tr>

<tr>
<td><b>MergeDelimiters</b></td>
<td>"True" or "False".  If true, then adjoining delimiter characters are treated as one by using
StringUtil.breakStringList(line,delimiters,StringUtil.DELIM_SKIP_BLANKS.</td>
<td>False (do not merge blank columns).</td>
</tr>

<tr>
<td><b>SkipLines (previously SkipRows)</b></td>
<td>Lines from the original file to skip (each value 0+), as list of comma-separated individual row or
ranges like 3-6.  Skipped lines are generally information that cannot be parsed.  The lines are skipped after
the initial read and are not available for further processing.</td>
<td>Don't skip any lines.</td>
</tr>

<tr>
<td><b>TrimInput</b></td>
<td>"True" or "False".  Indicates input strings should be trimmed before parsing.</td>
<td>False</td>
</tr>

<tr>
<td><b>TrimStrings</b></td>
<td>"True" or "False".  Indicates whether strings should
be trimmed before being placed in the data table (after parsing).</td>
<td>False</td>
</tr>

</table>
@return the DataTable that was created.
@throws Exception if an error occurs
*/
public static DataTable parseFile(String filename, PropList props) 
throws Exception
{   String routine = "DataTable.parseFile";
	// TODO SAM 2005-11-16 why is FixedFormat included?  Future feature?
	/*String propVal = props.getValue("FixedFormat");
	if (propVal != null) {
		if (propVal.equalsIgnoreCase("false")) {
			fixed = false;
		}
	}
	*/
   
    // FIXME SAM 2008-01-27 Using other than the default of strings does not seem to work
    // The JWorksheet does not display correctly.
    boolean ColumnDataTypes_Auto_boolean = false;   // To improve performance below
    // TODO SAM 2008-04-15 Evaluate whether the following should be used
    //String ColumnDataTypes = "AllStrings";  // Default for historical reasons
    String propVal = props.getValue("ColumnDataTypes");
    if ( (propVal != null) && (propVal.equalsIgnoreCase("Auto"))) {      
        //ColumnDataTypes = "Auto";
        ColumnDataTypes_Auto_boolean = true;
    }

    String Delimiter = "";
	propVal = props.getValue("Delimiter");
	if (propVal != null) {		
        Delimiter = propVal;
	}
	else {
        Delimiter = ",";
	}
	
    propVal = props.getValue("HeaderLines");
    if ( propVal == null ) {
        // Use older form...
        propVal = props.getValue("HeaderRows");
        if ( propVal != null ) {
            Message.printWarning(3, routine, "Need to convert HeaderRows parameter to HeaderLines in software." );
        }
    }
    List<Integer> HeaderLines_Vector = new Vector();
    int HeaderLines_Vector_maxval = -1;  // Used to optimize code below
    boolean HeaderLines_Auto_boolean = false;    // Are header rows to be determined automatically?
    if ( (propVal == null) || (propVal.length() == 0) ) {
        // Default...
        HeaderLines_Auto_boolean = true;
    }
    else {
        // Interpret the property.
        Message.printStatus ( 2, routine, "HeaderLines=\"" + propVal + "\"" );
        if ( propVal.equalsIgnoreCase("Auto")) {
            HeaderLines_Auto_boolean = true;
        }
        else {
            // Determine the list of rows to skip.
            List<String> v = StringUtil.breakStringList ( propVal, ", ", StringUtil.DELIM_SKIP_BLANKS );
            int vsize = 0;
            if ( v != null ) {
                vsize = v.size();
            }
            // FIXME SAM 2008-01-27 Figure out how to deal with multi-row headings.  For now only handle first
            if ( vsize > 1 ) {
                Message.printWarning ( 3, routine,
                   "Only know how to handle single-row headings.  Ignoring other heading rows." );
                vsize = 1;
            }
            for ( int i = 0; i < vsize; i++ ) {
                String vi = v.get(i);
                if ( StringUtil.isInteger(vi)) {
                    int row = Integer.parseInt(vi);
                    Message.printStatus ( 2, routine, "Header row is [" + row + "]");
                    HeaderLines_Vector.add(new Integer(row));
                    HeaderLines_Vector_maxval = Math.max(HeaderLines_Vector_maxval, row);
                }
                else {
                    int pos = vi.indexOf("-");
                    if ( pos >= 0 ) {
                        // Specifying a range of values...
                        int first_to_skip = -1;
                        int last_to_skip = -1;
                        if ( pos == 0 ) {
                            // First index is 0...
                            first_to_skip = 0;
                        }
                        else {
                            // Get first to skip...
                            first_to_skip = Integer.parseInt(vi.substring(0,pos).trim());
                        }
                        last_to_skip = Integer.parseInt(vi.substring(pos+1).trim());
                        for ( int is = first_to_skip; is <= last_to_skip; is++ ) {
                            HeaderLines_Vector.add(new Integer(is));
                            HeaderLines_Vector_maxval = Math.max(HeaderLines_Vector_maxval, is);
                        }
                    }
                }
            }
        }
    }
    // Use to speed up code below.
    int HeaderLines_Vector_size = HeaderLines_Vector.size();

	propVal = props.getValue("MergeDelimiters");
	int parse_flag = StringUtil.DELIM_ALLOW_STRINGS;
	if (propVal != null) {		
		parse_flag |= StringUtil.DELIM_SKIP_BLANKS;
	}

    String CommentLineIndicator = null;
	propVal = props.getValue("CommentLineIndicator");
	if (propVal != null) {
        CommentLineIndicator = propVal;
	}

    propVal = props.getValue("SkipLines");
    if ( propVal == null ) {
        // Try the older form...
        propVal = props.getValue("SkipRows");
        if ( propVal != null ) {
            Message.printWarning(3, routine, "Need to convert SkipRows parameter to SkipLines in software." );
        }
    }
    List<Integer> SkipLines_Vector = new Vector();
    int SkipLines_Vector_maxval = - 1;
    if ( (propVal != null) && (propVal.length() > 0) ) {
        // Determine the list of rows to skip.
        List<String> v = StringUtil.breakStringList ( propVal, ", ", StringUtil.DELIM_SKIP_BLANKS );
        int vsize = 0;
        if ( v != null ) {
            vsize = v.size();
        }
        for ( int i = 0; i < vsize; i++ ) {
            String vi = v.get(i);
            if ( StringUtil.isInteger(vi)) {
                int row = Integer.parseInt(vi);
                SkipLines_Vector.add(new Integer(row));
                SkipLines_Vector_maxval = Math.max(SkipLines_Vector_maxval, row);
            }
            else {
                int pos = vi.indexOf("-");
                if ( pos >= 0 ) {
                    // Specifying a range of values...
                    int first_to_skip = -1;
                    int last_to_skip = -1;
                    if ( pos == 0 ) {
                        // First index is 0...
                        first_to_skip = 0;
                    }
                    else {
                        // Get first to skip...
                        first_to_skip = Integer.parseInt(vi.substring(0,pos).trim());
                    }
                    last_to_skip = Integer.parseInt(vi.substring(pos+1).trim());
                    for ( int is = first_to_skip; is <= last_to_skip; is++ ) {
                        SkipLines_Vector.add(new Integer(is));
                        SkipLines_Vector_maxval = Math.max(SkipLines_Vector_maxval, is);
                    }
                }
            }
        }
    }
    // Use to speed up code below.
    int SkipLines_Vector_size = SkipLines_Vector.size();
	
	propVal = props.getValue("TrimInput");
	boolean TrimInput_Boolean = false;	// Default
	if ( (propVal != null) && propVal.equalsIgnoreCase("true") ) {
		TrimInput_Boolean = true;
	}

    boolean TrimStrings_boolean = false;
	propVal = props.getValue("TrimStrings");
	if ( (propVal != null) && propVal.equalsIgnoreCase("true") ) {
		TrimStrings_boolean = true;
	}

	List<List<String>> data_record_tokens = new Vector();
	List<String> v = null;
	int maxColumns = 0;
	int size = 0;

	BufferedReader in = new BufferedReader(new FileReader(filename));
	String line;

	// TODO JTS 2006-06-05
	// Found a bug in DataTable.  If you attempt to call
	// parseFile() on a file of size 0 (no lines, no characters)
	// it will throw an exception.  This should be checked out in the future.
	
	// Read until the end of the file...
	
	int linecount = 0; // linecount = 1 for first line in file, for user perspective.
	int linecount0;    // linecount0 = linecount - 1 (zero index), for code perspective.
	boolean headers_found = false; // Indicates whether the headers have been found
	List<TableField> tableFields = null; // Table fields as controlled by header or examination of data records
	int numFields = -1;    // Number of table fields.
	TableField tableField = null;  // Table field added below
	while ( true ) {
		line = in.readLine();
		if ( line == null ) {
		    // End of file...
		    break;
		}
		++linecount;
		linecount0 = linecount - 1;
		
		if ( Message.isDebugOn ) {
			Message.printDebug ( 10, routine, "Line [" + linecount0 + "]: " + line );
		}
		
		// Skip any comments anywhere in the file.
		if ( (CommentLineIndicator != null) && line.startsWith(CommentLineIndicator) ) {
		    continue;
		}
		
		// Also skip the requested lines to skip linecount is 1+ while lines to skip are 0+
		
		if ( linecount0 <= SkipLines_Vector_maxval ) {
		    // Need to check it...
		    if ( parseFile_LineMatchesLineFromList(linecount0,SkipLines_Vector, SkipLines_Vector_size)) {
		        // Skip the line as requested
                continue;
		    }
		}
		
		// "line" now contains the latest non-comment line so evaluate whether
	    // the line contains the column names.
	    
		if ( !headers_found && (HeaderLines_Auto_boolean ||
		    ((HeaderLines_Vector != null) && linecount0 <= HeaderLines_Vector_maxval)) ) {
		    if ( HeaderLines_Auto_boolean ) {
		        // If a quote is detected, then this line is assumed to contain the name of the fields.
        	    if (line.startsWith("\"")) {
        	        tableFields = parseFile_ParseHeaderLine ( line, linecount0, TrimInput_Boolean, Delimiter, parse_flag );
        	        numFields = tableFields.size();
        	        // Read another line of data to be used below
        	        headers_found = true;
        	        continue;
        	    }
		    }
		    else if ( HeaderLines_Vector != null ) {
		        // Calling code has specified the header rows.  Check to see if this is a row.
		        if ( parseFile_LineMatchesLineFromList(linecount0,HeaderLines_Vector, HeaderLines_Vector_size)) {
		            // This row has been specified as a header row so process it.
		            tableFields = parseFile_ParseHeaderLine ( line, linecount0, TrimInput_Boolean, Delimiter, parse_flag );
		            numFields = tableFields.size();
		                
                    //FIXME SAM 2008-01-27 Figure out how to deal with multi-row headings
                    // What is the column name?
		            // If the maximum header row has been processed, indicate that headers have been found.
		            //if ( linecount0 == HeaderLines_Vector_maxval ) {
		                headers_found = true;
		            //}
		            // Now read another line of data to be used below.
		            continue;
		        }
		    }
		}
		
		if ( linecount0 <= HeaderLines_Vector_maxval ) {
		    // Currently only allow one header row so need to ignore other rows that are found
		    // (don't want them considered as data).
		    if ( parseFile_LineMatchesLineFromList(linecount0,HeaderLines_Vector, HeaderLines_Vector_size)) {
		        continue;
		    }
		}

    	// Now evaluate the data lines.  Parse into tokens to allow evaluation of the number of columns below.
    	
        if ( TrimInput_Boolean ) {
			v = StringUtil.breakStringList(line.trim(), Delimiter, parse_flag );
		}
		else {
            v = StringUtil.breakStringList(line, Delimiter, parse_flag );
		}
		size = v.size();
		if (size > maxColumns) {
			maxColumns = size;
		}
		// Save the tokens from the data rows - this will NOT include comments, headers, or lines to be excluded.
		data_record_tokens.add(v);
	}
	// Close the file...
	in.close();
	
	// Make sure that the table fields are in place for the maximum number of columns.

	if (tableFields == null) {
		tableFields = new Vector();
		for (int i = 0; i < maxColumns; i++) {
			// default field definition builds String fields
			tableFields.add(new TableField());
		}
	}
	else {
		// Add enough fields to account for the maximum number of columns in the table.  
		String temp = null;
		for (int i = numFields; i < maxColumns; i++) {
			tableField = new TableField();
			temp = "Field_" + (i + 1);
			while (findPreviousFieldNameOccurances(tableFields,temp)) {
				temp = temp + "_2";
			}
			tableField.setName(temp);
			tableField.setDataType(TableField.DATA_TYPE_STRING);
			tableFields.add(tableField);
		}
	}
	
	// Loop through the data and determine what type of data are in each column.
	// Do this in any case because the length of the string columns needs to be determined.
	
	numFields = tableFields.size();
	size = data_record_tokens.size();
	int [] count_int = new int[maxColumns];
    int [] count_double = new int[maxColumns];
    int [] count_string = new int[maxColumns];
    int [] lenmax_string = new int[maxColumns];
    int [] precision = new int[maxColumns];
    for ( int icol = 0; icol < maxColumns; icol++ ) {
        count_int[icol] = 0;
        count_double[icol] = 0;
        count_string[icol] = 0;
        lenmax_string[icol] = 0;
        precision[icol] = 0;
    }
    // Loop through all rows of data that were read
    int vsize;
    String cell;
    String cell_trimmed; // Must have when checking for types.
    int periodPos; // Position of period in floating point numbers
	for ( int irow = 0; irow < size; irow++ ) {
	    v = data_record_tokens.get(irow);
	    vsize = v.size();
	    // Loop through all columns in the row.
	    for ( int icol = 0; icol < vsize; icol++ ) {
	        cell = v.get(icol);
	        cell_trimmed = cell.trim();
	        if ( StringUtil.isInteger(cell_trimmed)) {
	            ++count_int[icol];
	            // Length needed in case handled as string data
	            lenmax_string[icol] = Math.max(lenmax_string[icol], cell_trimmed.length());
	        }
            if ( StringUtil.isDouble(cell_trimmed)) {
                ++count_double[icol];
                // Length needed in case handled as string data
                lenmax_string[icol] = Math.max(lenmax_string[icol], cell_trimmed.length());
                // Precision to help with visualization
                periodPos = cell_trimmed.indexOf(".");
                if ( periodPos >= 0 ) {
                    precision[icol] = Math.max(precision[icol], (cell_trimmed.length() - periodPos - 1) );
                }
                
            }
            // TODO SAM 2008-01-27 Need to handle date/time?
            else {
                // String
                ++count_string[icol];
                if ( TrimStrings_boolean ) {
                    lenmax_string[icol] = Math.max(lenmax_string[icol], cell_trimmed.length());
                }
                else {
                    lenmax_string[icol] = Math.max(lenmax_string[icol], cell.length());
                }
            }
	    }
	}
	
	// Loop through the table fields and based on the examination of data above,
	// set the table field type and if a string, max width.
	
	int [] tableFieldType = new int[maxColumns];
	if ( ColumnDataTypes_Auto_boolean ) {
    	for ( int icol = 0; icol < maxColumns; icol++ ) {
    	    tableField = (TableField)tableFields.get(icol);
    	    if ( (count_int[icol] > 0) && (count_double[icol] == 0) && (count_string[icol] == 0) ) {
    	        // All data are integers so assume column type is integer
    	        tableField.setDataType(TableField.DATA_TYPE_INT);
    	        tableFieldType[icol] = TableField.DATA_TYPE_INT;
    	        tableField.setWidth (lenmax_string[icol] );
    	        Message.printStatus ( 2, routine, "Column [" + icol +
    	            "] type is integer as determined from examining data (" + count_int[icol] +
    	            " integers, " + count_double[icol] + " doubles, " + count_string[icol] + " strings).");
    	    }
    	    else if ( (count_double[icol] > 0) && (count_string[icol] == 0) ) {
    	        // All data are double (integers will also count as double) so assume column type is double
                tableField.setDataType(TableField.DATA_TYPE_DOUBLE);
                tableFieldType[icol] = TableField.DATA_TYPE_DOUBLE;
                tableField.setWidth (lenmax_string[icol] );
                tableField.setPrecision ( precision[icol] );
                Message.printStatus ( 2, routine, "Column [" + icol +
                    "] type is double as determined from examining data (" + count_int[icol] +
                    " integers, " + count_double[icol] + " doubles, " + count_string[icol] + " strings), " +
                    " width=" + lenmax_string[icol] + ", precision=" + precision[icol] + ".");
            }
    	    else {
    	        // Based on what is known, can only treat column as containing strings.
    	        tableField.setDataType(TableField.DATA_TYPE_STRING);
    	        tableFieldType[icol] = TableField.DATA_TYPE_STRING;
    	        tableField.setWidth (lenmax_string[icol] );
    	        Message.printStatus ( 2, routine, "Column [" + icol +
                    "] type is string as determined from examining data (" + count_int[icol] +
                    " integers, " + count_double[icol] + " doubles, " + count_string[icol] + " strings).");
    	        Message.printStatus ( 2, routine, "length max =" + lenmax_string[icol] );
    	    }
    	}
	}
	else {
	    // All are strings (from above but reset just in case)...
	    for ( int icol = 0; icol < maxColumns; icol++ ) {
	        tableField = (TableField)tableFields.get(icol);
	        tableField.setDataType(TableField.DATA_TYPE_STRING);
	        tableFieldType[icol] = TableField.DATA_TYPE_STRING;
	        tableField.setWidth (lenmax_string[icol] );
	        Message.printStatus ( 2, routine,"Column [" + icol + "] type is " +
	            tableField.getDataType() + " all strings assumed, width =" + tableField.getWidth() );
	    }
	}
	
	// Create the table from the field information.

	DataTable table = new DataTable(tableFields);
	table._haveDataInMemory = true;
	TableRecord tablerec = null;
	
	// Now transfer the data records to table records.
	
	int cols = 0;
	int icol = 0;
	for (int irow = 0; irow < size; irow++) {
		v = data_record_tokens.get(irow);

		tablerec = new TableRecord(maxColumns);
		cols = v.size();
		for (icol = 0; icol < cols; icol++) {
			if (TrimStrings_boolean) {
			    cell = v.get(icol).trim();
			}
			else {
				cell = v.get(icol);
			}
			if ( ColumnDataTypes_Auto_boolean ) {
			    // Set the data as an object of the column type.
			    if ( tableFieldType[icol] == TableField.DATA_TYPE_INT ) {
			        tablerec.addFieldValue( Integer.valueOf(cell.trim()) );
			    }
			    else if ( tableFieldType[icol] == TableField.DATA_TYPE_DOUBLE ) {
	                tablerec.addFieldValue( Double.valueOf(cell.trim()) );
	            }
			    else {
			        // Add as string
	                tablerec.addFieldValue( cell );
	            }
			}
			else {
			    // Set as the string value.
			    tablerec.addFieldValue( cell );
	        }
		}
		
		// If the specific record does not have enough columns, pad the columns at the end with blanks,
		// using blank strings or NaN for number fields.
		
		if (icol < maxColumns) {
			for (; icol < maxColumns; icol++) {
			    if ( ColumnDataTypes_Auto_boolean ) {
			        // Add values based on the column type.
			        if ( tableFieldType[icol] == TableField.DATA_TYPE_INT ) {
	                    tablerec.addFieldValue( null );
	                }
	                else if ( tableFieldType[icol] == TableField.DATA_TYPE_DOUBLE ) {
	                    tablerec.addFieldValue( null );
	                }
	                else {
	                    // Add as string
	                    tablerec.addFieldValue( "" );
	                }
			    }
			    else {
			        // Add a blank string.
			        tablerec.addFieldValue("");
			    }
			}
		}
		
		table.addRecord(tablerec);
	}

	return table;		
}

/**
Determine whether a line from the file matches the list of rows that are of interest.
@param linecount0
@param rows_List list of Integer objects that are row numbers (0+) of interest.
@param rows_List_size Size of rows_List - used to speed up performance.
@return true if the line matches an item in the list.
*/
private static boolean parseFile_LineMatchesLineFromList( int linecount0, List<Integer> rows_List, int rows_List_size )
{
    Integer int_object;
    if ( rows_List != null ) {
        rows_List_size = rows_List.size();
    }
    for ( int is = 0; is < rows_List_size; is++ ) {
        int_object = rows_List.get(is);
        if ( linecount0 == int_object.intValue() ) {
            // Skip the line as requested
            return true;
        }
    }
    return false;
}

/**
Parse a line that is known to be a header line to initialize the table fields.
All fields are set to type String, although this will be reset when data records are processed.
@param line Line to parse.
@param linecount0 Line number (0+).
@param TrimInput_Boolean Indicates whether input rows should be trimmed before parsing.
@param Delimiter The delimiter characters for parsing the line into tokens.
@param parse_flag the flag to be passed to StringUtil.breakStringList() when parsing the line.
@return A list of TableField describing the table columns.
*/
private static List<TableField> parseFile_ParseHeaderLine (
    String line, int linecount0, boolean TrimInput_Boolean, String Delimiter, int parse_flag )
{   String routine = "DataTable.parseFile_ParseHeaderLine";
    Message.printStatus ( 2, routine, "Adding column headers from line [" + linecount0 + "]: " + line );
    List<String> columns = null;
    if ( TrimInput_Boolean ) {
        columns = StringUtil.breakStringList( line.trim(), Delimiter, parse_flag );
    }
    else {
        columns = StringUtil.breakStringList(line, Delimiter, parse_flag );
    }
    
    int numFields = columns.size();
    List<TableField> tableFields = new Vector();
    TableField tableField = null;
    String temp = null;
    for (int i = 0; i < numFields; i++) {
        temp = columns.get(i).trim();
        while (findPreviousFieldNameOccurances(tableFields, temp)) {
            temp = temp + "_2";
        }
        tableField = new TableField();
        tableField.setName(temp);
        // All table fields by default are treated as strings.
        tableField.setDataType(TableField.DATA_TYPE_STRING);
        tableFields.add(tableField);
    }
    return tableFields;
}

/**
Sets the value of a specific field. 
@param row the row (0+) in which to set the value.
@param col the column (0+) in which to set the value.
@param value the value to set.
@exception Exception if the field value cannot be set, including if the row does not exist.
*/
public void setFieldValue(int row, int col, Object value) 
throws Exception
{
    setFieldValue ( row, col, value, false );
}

/**
Sets the value of a specific field. 
@param row the row (0+) in which to set the value .
@param col the column (0+) in which to set the value.
@param value the value to set.
@param createIfNecessary if true and the requested row is not in the existing rows, create
intervening rows, initialize to missing (null objects), and then set the data.
*/
public void setFieldValue(int row, int col, Object value, boolean createIfNecessary ) 
throws Exception
{
    int nRows = getNumberOfRecords();
    if ( (row > (nRows - 1)) && createIfNecessary ) {
        // Create empty rows
        for ( int i = nRows; i <= row; i++ ) {
            addRecord(emptyRecord());
        }
    }
    // Now set the value (will throw ArrayIndexOutOfBoundsException if row is out of range)...
    TableRecord record = _table_records.get(row);
    record.setFieldValue(col, value);
}

/**
Sets the width of the field.
@param col the column for which to set the width.
@param width the width to set.
*/
public void setFieldWidth(int col, int width) 
throws Exception {
	TableField field = _table_fields.get(col);
	field.setWidth(width);
}

/**
Set the table identifier.
@param table_id Identifier for the table
*/
public void setTableID ( String table_id )
{
    __table_id = table_id;
}

/**
Set the number of records in the table.  This method should typically only be
called when data are read on-the-fly (and are not stored in memory in the table records).
@param num_records Number of records in the table.
*/
public void setNumberOfRecords ( int num_records )
{	_num_records = num_records;
}

/**
Set field data type and header for the specified zero-based index.
@param index index of field to set
@param data_type data type; use TableField.DATA_TYPE_*
@param name name of the field.
*/
public void setTableField ( int index, int data_type, String name )
throws Exception
{	if ( _table_fields.size() <= index ) {
		throw new Exception ( "Index " + index + " is not valid." );
	}
	TableField tableField = _table_fields.get(index);
	tableField.setDataType ( data_type );
	tableField.setName ( name );
	tableField = null;
}

/**
Set the table fields to define the table.
@param tableFieldsVector a list of TableField objects defining table contents.
*/
public void setTableFields ( List<TableField> tableFieldsVector )
{	_table_fields = tableFieldsVector;
}

/**
Set table field name.
@param index index of field to set (zero-based).
@param name Field name.
@exception If the index is out of range.
*/
public void setTableFieldName ( int index, String name )
throws Exception 
{	if ( _table_fields.size() <= index ) {
		throw new Exception ( "Index " + index + " is not valid." );
	}
	TableField tableField = _table_fields.get(index);
	tableField.setName ( name );
	tableField = null;
}

/**
Set field data type for the specified zero-based index.
@param index index of field to set
@param data_type data type; use TableField.DATA_TYPE_*
@exception If the index is out of range.
*/
public void setTableFieldType ( int index, int data_type )
throws Exception
{	if ( _table_fields.size() <= index ) {
		throw new Exception ( "Index " + index + " is not valid." );
	}
	TableField tableField = _table_fields.get(index);
	tableField.setDataType ( data_type );
	tableField = null;
}

/**
Set whether strings should be trimmed at read.
@param trim_strings If true, strings will be trimmed at read.
@return Boolean value indicating whether strings should be trimmed, after reset.
*/
public boolean trimStrings ( boolean trim_strings )
{	_trim_strings = trim_strings;
	return _trim_strings;
}

/**
Indicate whether strings should be trimmed at read.
@return Boolean value indicating whether strings should be trimmed.
*/
public boolean trimStrings ( )
{	return _trim_strings;
}

// TODO SAM 2006-06-21
// Need to check for delimiter in header and make this code consistent with
// the RTi.Util.GUI.JWorksheet file saving code, or refactor to use the same code.
/**
Writes a table to a delimited file.  If the data items contain the delimiter,
they will be written surrounded by double quotes.
@param filename the file to write to.
@param delimiter the delimiter to use.
@param writeHeader If true, the field names will be read from the fields 
and written as a one-line header of field names.  The headers are double-quoted.
If all headers are missing, then the header line will not be written.
@param comments a list of Strings to put at the top of the file as comments.
@throws Exception if an error occurs.
*/
public void writeDelimitedFile(String filename, String delimiter, boolean writeHeader, List<String> comments) 
throws Exception {
	String routine = "DataTable.writeDelimitedFile";
	
	if (filename == null) {
		Message.printWarning(1, routine, "Cannot write to file '" + filename + "'");
		throw new Exception("Cannot write to file '" + filename + "'");
	}
		
	PrintWriter out = new PrintWriter( new BufferedWriter(new FileWriter(filename)));
	try {
	    // Put the standard header at the top of the file
	    IOUtil.printCreatorHeader ( out, "#", 80, 0 );
    	// If any comments have been passed in, print them at the top of the file
    	if (comments != null && comments.size() > 0) {
    		int size = comments.size();
    		for (int i = 0; i < size; i++) {
    			out.println("# " + comments.get(i) );
    		}
    	}
    
    	int cols = getNumberOfFields();
    	if (cols == 0) {
    		Message.printWarning(3, routine, "Table has 0 columns!  Nothing will be written.");
    		return;
    	}
    
    	StringBuffer line = new StringBuffer();
    
        int nonBlank = 0; // Number of nonblank table headings
    	if (writeHeader) {
    	    // First determine if any headers are non blank
            for (int col = 0; col < cols; col++) {
                if ( getFieldName(col).length() > 0 ) {
                    ++nonBlank;
                }
            }
            if ( nonBlank > 0 ) {
                out.println ( "# Column headings are first line below, followed by data lines.");
        		line.setLength(0);
        		for (int col = 0; col < (cols - 1); col++) {
        			line.append( "\"" + getFieldName(col) + "\"" + delimiter);
        		}
        		line.append( "\"" + getFieldName((cols - 1)) + "\"");
        		out.println(line);
            }
    	}
    	if ( !writeHeader || (nonBlank == 0) ) {
    	    out.println ( "# No column headings are defined - data lines follow below.");
    	}
    	
    	int rows = getNumberOfRecords();
    	String cell;
    	int tableFieldType;
    	int precision;
    	for (int row = 0; row < rows; row++) {
    		line.setLength(0);
    		for (int col = 0; col < cols; col++) {
    		    if ( col > 0 ) {
    		        line.append ( delimiter );
    		    }
    		    tableFieldType = getTableFieldType(col);
    		    precision = getFieldPrecision(col);
                if ( ((tableFieldType == TableField.DATA_TYPE_FLOAT) ||
                    (tableFieldType == TableField.DATA_TYPE_DOUBLE)) && (precision > 0) ) {
                    // Format according to the precision if floating point
                    cell = StringUtil.formatString(getFieldValue(row,col),"%." + precision + "f");
                }
                else {
                    // Use default formatting.
                    cell = "" + getFieldValue(row,col);
                }
    			// If the field contains the delimiter, surround with double quotes...
    			if ( cell.indexOf(delimiter) > -1 ) {
    				cell = "\"" + cell + "\"";
    			}
    			line.append ( cell );
    		}
    		out.println(line);
    	}
	}
	catch ( Exception e ) {
	    // Log and rethrow
	    Message.printWarning(3, routine, "Unexpected error writing delimited file (" + e + ")." );
	    Message.printWarning(3, routine, e);
	}
	finally {
    	out.flush();
    	out.close();
	}
}

}