package riverside.datastore;

import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import RTi.DMI.AbstractDatabaseDataStore;
import RTi.DMI.DMI;
import RTi.DMI.DMISelectStatement;
import RTi.DMI.DMIUtil;
import RTi.DMI.GenericDMI;
import RTi.TS.TS;
import RTi.TS.TSIdent;
import RTi.TS.TSUtil;
import RTi.Util.GUI.InputFilter;
import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Time.DateTime;

/**
Data store for Generic database, to allow table/view queries.
This class maintains the database connection information in a general way.
@author sam
*/
public class GenericDatabaseDataStore extends AbstractDatabaseDataStore
{

/**
Datastore configuration properties that map the database time series metadata table/view
to the datastore.
*/
public static final String TS_META_TABLE_PROP = "TimeSeriesMetadataTable";
public static final String TS_META_TABLE_LOCTYPE_COLUMN_PROP =
    "TimeSeriesMetadataTable_LocationTypeColumn";
public static final String TS_META_TABLE_LOCATIONID_COLUMN_PROP =
    "TimeSeriesMetadataTable_LocationIdColumn";
public static final String TS_META_TABLE_DATASOURCE_COLUMN_PROP =
    "TimeSeriesMetadataTable_DataSourceColumn";
public static final String TS_META_TABLE_DATATYPE_COLUMN_PROP =
    "TimeSeriesMetadataTable_DataTypeColumn";
public static final String TS_META_TABLE_DATAINTERVAL_COLUMN_PROP =
    "TimeSeriesMetadataTable_DataIntervalColumn";
public static final String TS_META_TABLE_SCENARIO_COLUMN_PROP =
    "TimeSeriesMetadataTable_ScenarioColumn";
public static final String TS_META_TABLE_UNITS_COLUMN_PROP =
    "TimeSeriesMetadataTable_DataUnitsColumn";
public static final String TS_META_TABLE_ID_COLUMN_PROP =
    "TimeSeriesMetadataTable_MetadataIdColumn";

public static final String TS_DATA_TABLE_PROP = "TimeSeriesDataTable";
public static final String TS_DATA_TABLE_METAID_COLUMN_PROP = "TimeSeriesDataTable_MetadataIdColumn";
public static final String TS_DATA_TABLE_DATETIME_COLUMN_PROP = "TimeSeriesDataTable_DateTimeColumn";
public static final String TS_DATA_TABLE_VALUE_COLUMN_PROP = "TimeSeriesDataTable_ValueColumn";
public static final String TS_DATA_TABLE_FLAG_COLUMN_PROP = "TimeSeriesDataTable_FlagColumn";

/**
Database metadata, stored here to speed up database interactions.
*/
private DatabaseMetaData databaseMetadata = null;
    
/**
Construct a data store given a DMI instance, which is assumed to be open.
@param name identifier for the data store
@param description name for the data store
@param dmi DMI instance to use for the data store.
*/
public GenericDatabaseDataStore ( String name, String description, DMI dmi )
{
    setName ( name );
    setDescription ( description );
    setDMI ( dmi );
    // Rely on other authentication to prevent writing.
    // TODO SAM 2013-02-26 Perhaps use a database configuration file property to control
    dmi.setEditable ( true );
}
    
/**
Factory method to construct a data store connection from a properties file.
@param filename name of file containing property strings
*/
public static GenericDatabaseDataStore createFromFile ( String filename )
throws IOException, Exception
{
    // Read the properties from the file
    PropList props = new PropList ("");
    props.setPersistentName ( filename );
    props.readPersistent ( false );
    String name = IOUtil.expandPropertyForEnvironment("Name",props.getValue("Name"));
    String description = IOUtil.expandPropertyForEnvironment("Description",props.getValue("Description"));
    String databaseEngine = IOUtil.expandPropertyForEnvironment("DatabaseEngine",props.getValue("DatabaseEngine"));
    String databaseServer = IOUtil.expandPropertyForEnvironment("DatabaseServer",props.getValue("DatabaseServer"));
    String databaseName = IOUtil.expandPropertyForEnvironment("DatabaseName",props.getValue("DatabaseName"));
    String odbcName = props.getValue ( "OdbcName" );
    String systemLogin = IOUtil.expandPropertyForEnvironment("SystemLogin",props.getValue("SystemLogin"));
    String systemPassword = IOUtil.expandPropertyForEnvironment("SystemPassword",props.getValue("SystemPassword"));
    
    // Get the properties and create an instance
    GenericDMI dmi = null;
    if ( (odbcName != null) && !odbcName.equals("") ) {
        // An ODBC connection is configured so use it
        dmi = new GenericDMI (
            databaseEngine, // Needed for internal SQL handling
            odbcName, // Must be configured on the machine
            systemLogin, // OK if null - use read-only guest
            systemPassword ); // OK if null - use read-only guest
    }
    else {
        // Use the parts and create the connection string on the fly
        dmi = new GenericDMI( databaseEngine, databaseServer, databaseName, -1, systemLogin, systemPassword );
    }
    dmi.open();
    GenericDatabaseDataStore ds = new GenericDatabaseDataStore( name, description, dmi );
    ds.setProperties(props);
    return ds;
}

/**
Return the database metadata associated with the database.  If metadata have not been retrieved, retrieve and save.
*/
private DatabaseMetaData getDatabaseMetaData ()
throws Exception
{
    if ( this.databaseMetadata == null ) {
        this.databaseMetadata = getDMI().getConnection().getMetaData();
    }
    return this.databaseMetadata;
}

/**
Create a list of where clauses give an InputFilter_JPanel.  The InputFilter
instances that are managed by the InputFilter_JPanel must have been defined with
the database table and field names in the internal (non-label) data.
@return a list of where clauses, each of which can be added to a DMI statement.
@param dmi The DMI instance being used, which may be checked for specific formatting.
@param panel The InputFilter_JPanel instance to be converted.  If null, an empty list will be returned.
*/
private List<String> getWhereClausesFromInputFilter ( DMI dmi, InputFilter_JPanel panel ) 
{
    // Loop through each filter group.  There will be one where clause per filter group.

    if (panel == null) {
        return new Vector();
    }

    int nfg = panel.getNumFilterGroups ();
    InputFilter filter;
    List<String> whereClauses = new Vector();
    String whereClause = ""; // A where clause that is being formed.
    for ( int ifg = 0; ifg < nfg; ifg++ ) {
        filter = panel.getInputFilter ( ifg );  
        whereClause  = DMIUtil.getWhereClauseFromInputFilter(dmi, filter,panel.getOperator(ifg), true);
        if (whereClause != null) {
            whereClauses.add(whereClause );
        }
    }
    return whereClauses;
}

/**
Create a where string given an InputFilter_JPanel.  The InputFilter
instances that are managed by the InputFilter_JPanel must have been defined with
the database table and field names in the internal (non-label) data.
@return a list of where clauses as a string, each of which can be added to a DMI statement.
@param dmi The DMI instance being used, which may be checked for specific formatting.
@param panel The InputFilter_JPanel instance to be converted.  If null, an empty list will be returned.
@param tableAndColumnName the name of the table for which to get where clauses in format TableName.ColumnName.
@param useAnd if true, then "and" is used instead of "where" in the where strings.  The former can be used
with "join on" SQL syntax.
@param addNewline if true, add a newline if the string is non-blank - this simply helps with formatting of
the big SQL, so that logging has reasonable line breaks
*/
private String getWhereClauseStringFromInputFilter ( DMI dmi, InputFilter_JPanel panel, String tableAndColumnName,
   boolean addNewline )
{
    List<String> whereClauses = getWhereClausesFromInputFilter ( dmi, panel );
    StringBuffer whereString = new StringBuffer();
    for ( String whereClause : whereClauses ) {
        Message.printStatus(2,"","Comparing where clause \"" + whereClause + "\" to \"" + tableAndColumnName + "\"");
        if ( whereClause.toUpperCase().indexOf(tableAndColumnName.toUpperCase()) < 0 ) {
            // Not for the requested table so don't include the where clause
            Message.printStatus(2, "", "Did not match");
            continue;
        }
        Message.printStatus(2, "", "Matched");
        if ( (whereString.length() > 0)  ) {
            // Need to concatenate
            whereString.append ( " and ");
        }
        whereString.append ( "(" + whereClause + ")");
    }
    if ( addNewline && (whereString.length() > 0) ) {
        whereString.append("\n");
    }
    return whereString.toString();
}

/**
Indicate whether properties have been defined to allow querying time series from the datastore.
Only the minimal properties are checked.
@param checkDatabase check to see whether tables and columns mentioned in the configuration actually
exist in the database
@return true if the datastore has properties defined to support reading time series
*/
public boolean hasTimeSeriesInterface ( boolean checkDatabase )
{   String routine = "GenericDatabaseDataStore.hasTimeSeriesInterface";
    DatabaseMetaData meta = null;
    if ( checkDatabase ) {
        try {
            meta = getDatabaseMetaData();
        }
        catch ( Exception e ) {
            return false;
        }
    }
    String table, column;
    try {
        // Must have metadata table
        table = getProperty(TS_META_TABLE_PROP);
        if ( table == null ) {
            Message.printStatus(2,routine,"Datastore \"" + getName() +
                "\" does not have configuration property \"" + TS_META_TABLE_PROP + "\"." );
            return false;
        }
        else if ( checkDatabase && !DMIUtil.databaseHasTable(meta, table) ) {
             Message.printStatus(2,routine,"Datastore \"" + getName() +
                 "\" does not have table/view \"" + table + "\"." );
             return false;
        }
        // Must have location, data type, and interval columns
        column = getProperty(TS_META_TABLE_LOCATIONID_COLUMN_PROP);
        if ( column == null ) {
            Message.printStatus(2,routine,"Datastore \"" + getName() +
                "\" does not have configuration property \"" + TS_META_TABLE_LOCATIONID_COLUMN_PROP + "\"." );
            return false;
        }
        else if ( checkDatabase && !DMIUtil.databaseTableHasColumn(meta, table, column) ) {
            Message.printStatus(2,routine,"Datastore \"" + getName() +
                "\" table/view \"" + table + "\" does not have column \"" + column + "\"." );
            return false;
        }
        column = getProperty(TS_META_TABLE_DATASOURCE_COLUMN_PROP);
        if ( column == null ) {
            Message.printStatus(2,routine,"Datastore \"" + getName() +
                "\" does not have configuration property \"" + TS_META_TABLE_DATASOURCE_COLUMN_PROP + "\"." );
            return false;
        }
        else if ( checkDatabase && !DMIUtil.databaseTableHasColumn(meta, table, column) ) {
            Message.printStatus(2,routine,"Datastore \"" + getName() +
                "\" table/view \"" + table + "\" does not have column \"" + column + "\"." );
            return false;
        }
        column = getProperty(TS_META_TABLE_DATAINTERVAL_COLUMN_PROP);
        if ( column == null ) {
            Message.printStatus(2,routine,"Datastore \"" + getName() +
                "\" does not have configuration property \"" + TS_META_TABLE_DATAINTERVAL_COLUMN_PROP + "\"." );
            return false;
        }
        else if ( checkDatabase && !DMIUtil.databaseTableHasColumn(meta, table, column) ) {
            Message.printStatus(2,routine,"Datastore \"" + getName() +
                "\" table/view \"" + table + "\" does not have column \"" + column + "\"." );
            return false;
        }
    }
    catch ( Exception e ) {
        // Could not get database information
        return false;
    }
    return true;
}

/**
Read a time series from the datastore.
*/
public TS readTimeSeries ( String tsidentString, DateTime inputStart, DateTime inputEnd, boolean readData )
{   String routine = "GenericDatabaseDataStore.readTimeSeries", message;
    TS ts = null;
    TSIdent tsident = null;
    try {
        tsident = TSIdent.parseIdentifier(tsidentString);
    }
    catch ( Exception e ) {
        message = "Time series identifier \"" + tsidentString + "\" is invalid (" + e + ")";
        Message.printWarning(3,routine,message);
        throw new RuntimeException ( message );
    }
    // Get the time series metadata record
    TimeSeriesMeta tsMeta = readTimeSeriesMeta ( tsident.getLocationType(), tsident.getLocation(),
        tsident.getSource(), tsident.getType(), tsident.getInterval(), tsident.getScenario() );
    if ( tsMeta == null ) {
        return null;
    }
    // Create the time series
    double missing = Double.NaN;
    try {
        ts = TSUtil.newTimeSeries(tsident + "~" + getName(), true);
        ts.setIdentifier(tsident);
        ts.setDataUnits ( tsMeta.getUnits() );
        ts.setDataUnitsOriginal ( tsMeta.getUnits() );
        ts.setDescription(tsident.getLocation()); // TODO SAM evaluate adding to metadata
        ts.setMissing(missing);
    }
    catch ( Exception e ) {
        Message.printWarning(3,routine,"Error creating time series (" + e + ")." );
        return null;
    }
    if ( !readData ) {
        return ts;
    }
    // Read the time series data
    DMI dmi = getDMI();
    DMISelectStatement ss = new DMISelectStatement(dmi);
    String dataTable = getProperty ( GenericDatabaseDataStore.TS_DATA_TABLE_PROP );
    Message.printStatus(2, routine, "Data table = \"" + dataTable + "\"");
    if ( dataTable != null ) {
        // Table name may contain formatting like %I, etc.
        dataTable = ts.formatLegend(dataTable);
    }
    String dtColumn = getProperty ( GenericDatabaseDataStore.TS_DATA_TABLE_DATETIME_COLUMN_PROP );
    String valColumn = getProperty ( GenericDatabaseDataStore.TS_DATA_TABLE_VALUE_COLUMN_PROP );
    String flagColumn = getProperty ( GenericDatabaseDataStore.TS_DATA_TABLE_FLAG_COLUMN_PROP );
    ss.addTable(dataTable);
    ss.addField(dtColumn);
    ss.addField(valColumn);
    if ( flagColumn != null ) {
        ss.addField(flagColumn);
    }
    ss.addOrderByClause(dtColumn);
    if ( inputStart != null ) {
        try {
            ss.addWhereClause(dtColumn + " <= " + DMIUtil.formatDateTime(dmi, inputStart) );
        }
        catch ( Exception e ) {
            Message.printWarning(3, routine, "Error setting input start for query (" + e + ")." );
        }
    }
    if ( inputEnd != null ) {
        try {
            ss.addWhereClause(dtColumn + " >= " + DMIUtil.formatDateTime(dmi, inputEnd) );
        }
        catch ( Exception e ) {
            Message.printWarning(3, routine, "Error setting input end for query (" + e + ")." );
        }
    }
    String sqlString = ss.toString();
    ResultSet rs = null;
    double d, value;
    Date dt;
    String s, flag = "";
    DateTime dateTime;
    int i;
    int index;
    List<TimeSeriesData> tsdataList = new Vector<TimeSeriesData>();
    boolean dateTimeInt = true; // Indicates year
    try {
        rs = dmi.dmiSelect(ss);
        while (rs.next()) {
            index = 1;
            if ( dateTimeInt ) {
                i = rs.getInt(index++);
                if (rs.wasNull()) {
                    continue;
                }
                else {
                    dateTime = new DateTime(DateTime.PRECISION_YEAR);
                    dateTime.setYear(i);
                }
            }
            else {
                dt = rs.getTimestamp(index++);
                if (rs.wasNull()) {
                    continue;
                }
                else {
                    dateTime = new DateTime(dt);
                }
            }
            d = rs.getDouble(index++);
            if (rs.wasNull()) {
                value = missing;
            }
            else {
                value = d;
            }
            if ( flagColumn != null ) {
                s = rs.getString(index);
                if (rs.wasNull()) {
                    flag = s;
                }
                else {
                    flag = "";
                }
            }
            tsdataList.add(new TimeSeriesData(dateTime,value,flag));
        }
    }
    catch ( Exception e ) {
        Message.printWarning ( 3, routine, "Error reading time series data from database with statement \"" + sqlString + "\" (" + e + ")."); 
    }
    finally {
        DMI.closeResultSet(rs);
    }
    // Process the data records into the time series
    if ( tsdataList.size() > 0 ) {
        ts.setDate1(tsdataList.get(0).getDateTime());
        ts.setDate1Original(ts.getDate1());
        ts.setDate2(tsdataList.get(tsdataList.size() - 1).getDateTime());
        ts.setDate2Original(ts.getDate2());
        ts.allocateDataSpace();
        for ( TimeSeriesData tsdata : tsdataList ) {
            if ( flagColumn == null ) {
                ts.setDataValue(tsdata.getDateTime(), tsdata.getValue() );
            }
            else {
                ts.setDataValue(tsdata.getDateTime(), tsdata.getValue(), tsdata.getFlag(), -1 );
            }
        }
    }
    return ts;
}

/**
Read time series metadata for one time series.
@return the time series metadata object, or null if not exactly 1 metadata records match.
*/
public TimeSeriesMeta readTimeSeriesMeta ( String locType, String locID,
    String dataSource, String dataType, String interval, String scenario )
{   String routine = "GenericDatabaseDataStore.readTimeSeriesMeta";
    DMI dmi = getDMI();
    // Create a statement to read the specific metadata record
    DMISelectStatement ss = new DMISelectStatement(dmi);
    String metaTable = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_PROP );
    String idColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_ID_COLUMN_PROP );
    String ltColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_LOCTYPE_COLUMN_PROP );
    String locIdColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_LOCATIONID_COLUMN_PROP );
    String sourceColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_DATASOURCE_COLUMN_PROP );
    String dtColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_DATATYPE_COLUMN_PROP );
    String intervalColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_DATAINTERVAL_COLUMN_PROP );
    String scenarioColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_SCENARIO_COLUMN_PROP );
    String unitsColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_UNITS_COLUMN_PROP );
    ss.addTable(metaTable);
    ss.addField(idColumn);
    ss.addField(ltColumn);
    ss.addField(locIdColumn);
    ss.addField(sourceColumn);
    ss.addField(dtColumn);
    ss.addField(intervalColumn);
    ss.addField(scenarioColumn);
    ss.addField(unitsColumn);
    readTimeSeriesMetaAddWhere(ss,metaTable,ltColumn,locType);
    readTimeSeriesMetaAddWhere(ss,metaTable,locIdColumn,locID);
    readTimeSeriesMetaAddWhere(ss,metaTable,sourceColumn,dataSource);
    readTimeSeriesMetaAddWhere(ss,metaTable,dtColumn,dataType);
    readTimeSeriesMetaAddWhere(ss,metaTable,intervalColumn,interval);
    readTimeSeriesMetaAddWhere(ss,metaTable,scenarioColumn,scenario);
    String sqlString = ss.toString();
    ResultSet rs = null;
    long l, id = -1;
    String s, units = "";
    int count = 0;
    try {
        rs = dmi.dmiSelect(ss);
        while (rs.next()) {
            // Since the calling arguments include everything of interest, really only need the ID and units from the query
            ++count;
            l = rs.getLong(1);
            if (!rs.wasNull()) {
                id = l;
            }
            s = rs.getString(8);
            if (!rs.wasNull()) {
                units = s;
            }
        }
    }
    catch ( Exception e ) {
        Message.printWarning ( 3, routine, "Error reading time series metadata from database with statement \"" + sqlString + "\" (" + e + ")."); 
    }
    finally {
        DMI.closeResultSet(rs);
    }
    if ( count != 1 ) {
        Message.printWarning(3, routine, "Expecting 1 time series meta object for \"" + sqlString + "\" but have " + count );
        return null;
    }
    return new TimeSeriesMeta(locType, locID, dataSource, dataType, interval, scenario, units, id);
}

/**
Read location type strings for the data store, if time series support is configured.
Not a lot of error checking is done because the data store should have been checked out by this point
@param locID location ID to use as filter (ignored if blank or null)
@param locType location type to use as filter (ignored if blank or null)
@param dataType data type to use as filter (ignored if blank or null)
@param interval interval to use as filter (ignored if blank or null)
@param scenario scenario to use as filter (ignored if blank or null)
@return the list of interval strings by making a unique query of the 
*/
public List<String> readTimeSeriesMetaDataSourceList ( String locType, String locID,
    String dataType, String interval, String scenario )
{   String routine = "GenericDatabaseDataStore.readDataSourceStrings";
    DMI dmi = getDMI();
    List<String> dataSources = new Vector<String>();
    // Create a statement to read distinct data types from the time series metadata table
    DMISelectStatement ss = new DMISelectStatement(dmi);
    String metaTable = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_PROP );
    String ltColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_LOCTYPE_COLUMN_PROP );
    String idColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_LOCATIONID_COLUMN_PROP );
    String sourceColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_DATASOURCE_COLUMN_PROP );
    String dtColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_DATATYPE_COLUMN_PROP );
    String intervalColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_DATAINTERVAL_COLUMN_PROP );
    String scenarioColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_SCENARIO_COLUMN_PROP );
    ss.addTable(metaTable);
    ss.addField(sourceColumn);
    readTimeSeriesMetaAddWhere(ss,metaTable,ltColumn,locType);
    readTimeSeriesMetaAddWhere(ss,metaTable,idColumn,locID);
    // LocationType is what is being read so don't filter
    readTimeSeriesMetaAddWhere(ss,metaTable,dtColumn,dataType);
    readTimeSeriesMetaAddWhere(ss,metaTable,intervalColumn,interval);
    readTimeSeriesMetaAddWhere(ss,metaTable,scenarioColumn,scenario);
    ss.selectDistinct(true);
    ss.addOrderByClause(sourceColumn);
    String sqlString = ss.toString();
    Message.printStatus(2,routine,"Running:" + sqlString );
    ResultSet rs = null;
    String s;
    try {
        rs = dmi.dmiSelect(ss);
        while (rs.next()) {
            s = rs.getString(1);
            if (!rs.wasNull()) {
                dataSources.add(s);
            }
        }
    }
    catch ( Exception e ) {
        Message.printWarning ( 3, routine, "Error reading time series metadata from database with statement \"" + sqlString + "\" (" + e + ")."); 
    }
    finally {
        DMI.closeResultSet(rs);
    }
    return dataSources;
}

/**
Read data type strings for the data store, if time series support is configured.
Not a lot of error checking is done because the data store should have been checked out by this point
@param includeNotes if true, include notes in the data type strings, like "DataType - Note"
(currently does nothing)
@param locType location type to use as filter (ignored if blank or null)
@param locID location ID to use as filter (ignored if blank or null)
@param dataSource data source to use as filter (ignored if blank or null)
@param interval interval to use as filter (ignored if blank or null)
@param scenario scenario to use as filter (ignored if blank or null)
@return the list of data type strings by making a unique query of the 
*/
public List<String> readTimeSeriesMetaDataTypeList ( boolean includeNotes,
    String locType, String locID, String dataSource, String interval, String scenario )
{   String routine = "GenericDatabaseDataStore.readDataTypeStrings";
    DMI dmi = getDMI();
    List<String> dataTypes = new Vector<String>();
    // Create a statement to read distinct data types from the time series metadata table
    DMISelectStatement ss = new DMISelectStatement(dmi);
    String metaTable = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_PROP );
    String ltColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_LOCTYPE_COLUMN_PROP );
    String idColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_LOCATIONID_COLUMN_PROP );
    String sourceColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_DATASOURCE_COLUMN_PROP );
    String dtColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_DATATYPE_COLUMN_PROP );
    String intervalColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_DATAINTERVAL_COLUMN_PROP );
    String scenarioColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_SCENARIO_COLUMN_PROP );
    ss.addTable(metaTable);
    ss.addField(dtColumn);
    ss.selectDistinct(true);
    readTimeSeriesMetaAddWhere(ss,metaTable,ltColumn,locType);
    readTimeSeriesMetaAddWhere(ss,metaTable,idColumn,locID);
    readTimeSeriesMetaAddWhere(ss,metaTable,sourceColumn,dataSource);
    // Data type is what is being read so don't filter
    readTimeSeriesMetaAddWhere(ss,metaTable,intervalColumn,interval);
    readTimeSeriesMetaAddWhere(ss,metaTable,scenarioColumn,scenario);
    ss.addOrderByClause(dtColumn);
    String sqlString = ss.toString();
    ResultSet rs = null;
    String s;
    try {
        rs = dmi.dmiSelect(ss);
        while (rs.next()) {
            s = rs.getString(1);
            if (!rs.wasNull()) {
                dataTypes.add(s);
            }
        }
    }
    catch ( Exception e ) {
        Message.printWarning ( 3, routine, "Error reading time series metadata from database with statement \"" + sqlString + "\" (" + e + ")."); 
    }
    finally {
        DMI.closeResultSet(rs);
    }
    return dataTypes;
}

/**
Read interval strings for the data store, if time series support is configured.
Not a lot of error checking is done because the data store should have been checked out by this point
@param locType location type to use as filter (ignored if blank or null)
@param locID location ID to use as filter (ignored if blank or null)
@param dataSource data source to use as filter (ignored if blank or null)
@param dataType data type to use as filter (ignored if blank or null)
@param scenario scenario to use as filter (ignored if blank or null)
@return the list of distinct location ID strings from time series metadata
*/
public List<String> readTimeSeriesMetaIntervalList ( String locType, String locID, String dataSource,
    String dataType, String scenario )
{   String routine = "GenericDatabaseDataStore.readIntervalStrings";
    DMI dmi = getDMI();
    List<String> intervals = new Vector<String>();
    // Create a statement to read distinct data types from the time series metadata table
    DMISelectStatement ss = new DMISelectStatement(dmi);
    String metaTable = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_PROP );
    String ltColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_LOCTYPE_COLUMN_PROP );
    String idColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_LOCATIONID_COLUMN_PROP );
    String sourceColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_DATASOURCE_COLUMN_PROP );
    String dtColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_DATATYPE_COLUMN_PROP );
    String intervalColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_DATAINTERVAL_COLUMN_PROP );
    String scenarioColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_SCENARIO_COLUMN_PROP );
    ss.addTable(metaTable);
    ss.addField(intervalColumn);
    readTimeSeriesMetaAddWhere(ss,metaTable,ltColumn,locType);
    readTimeSeriesMetaAddWhere(ss,metaTable,idColumn,locID);
    readTimeSeriesMetaAddWhere(ss,metaTable,sourceColumn,dataSource);
    readTimeSeriesMetaAddWhere(ss,metaTable,dtColumn,dataType);
    // Interval is what is being read so don't filter
    readTimeSeriesMetaAddWhere(ss,metaTable,scenarioColumn,scenario);
    ss.selectDistinct(true);
    ss.addOrderByClause(intervalColumn);
    String sqlString = ss.toString();
    Message.printStatus(2,routine,"Running:" + sqlString );
    ResultSet rs = null;
    String s;
    try {
        rs = dmi.dmiSelect(ss);
        while (rs.next()) {
            s = rs.getString(1);
            if (!rs.wasNull()) {
                intervals.add(s);
            }
        }
    }
    catch ( Exception e ) {
        Message.printWarning ( 3, routine, "Error reading time series metadata from database with statement \"" + sqlString + "\" (" + e + ")."); 
    }
    finally {
        DMI.closeResultSet(rs);
    }
    return intervals;
}

/**
Read a list of TimeSeriesMeta for the specified criteria.
*/
public List<TimeSeriesMeta> readTimeSeriesMetaList ( String dataType, String interval,
    GenericDatabaseDataStore_TimeSeries_InputFilter_JPanel filterPanel )
{
    List<TimeSeriesMeta> metaList = new Vector<TimeSeriesMeta>();
    String routine = "GenericDatabaseDataStore.readTimeSeriesMetaList";
    DMI dmi = getDMI();
    // Create a statement to read the specific metadata record
    DMISelectStatement ss = new DMISelectStatement(dmi);
    String metaTable = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_PROP );
    String idColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_ID_COLUMN_PROP );
    String ltColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_LOCTYPE_COLUMN_PROP );
    String locIdColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_LOCATIONID_COLUMN_PROP );
    String sourceColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_DATASOURCE_COLUMN_PROP );
    String dtColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_DATATYPE_COLUMN_PROP );
    String intervalColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_DATAINTERVAL_COLUMN_PROP );
    String scenarioColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_SCENARIO_COLUMN_PROP );
    String unitsColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_UNITS_COLUMN_PROP );
    ss.addTable(metaTable);
    ss.addField(idColumn);
    ss.addField(ltColumn);
    ss.addField(locIdColumn);
    ss.addField(sourceColumn);
    ss.addField(dtColumn);
    ss.addField(intervalColumn);
    ss.addField(scenarioColumn);
    ss.addField(unitsColumn);
    String locType = null;
    String locID = null;
    String dataSource = null;
    String scenario = null;
    readTimeSeriesMetaAddWhere(ss,metaTable,dtColumn,dataType);
    readTimeSeriesMetaAddWhere(ss,metaTable,intervalColumn,interval);
    List<String> whereClauses = new Vector<String>();
    whereClauses.add ( getWhereClauseStringFromInputFilter ( dmi, filterPanel, metaTable + "." + ltColumn, true ) );
    whereClauses.add ( getWhereClauseStringFromInputFilter ( dmi, filterPanel, metaTable + "." + locIdColumn, true ) );
    whereClauses.add ( getWhereClauseStringFromInputFilter ( dmi, filterPanel, metaTable + "." + sourceColumn, true ) );
    whereClauses.add ( getWhereClauseStringFromInputFilter ( dmi, filterPanel, metaTable + "." + scenarioColumn, true ) );
    try {
        ss.addWhereClauses(whereClauses);
    }
    catch ( Exception e ) {
        Message.printWarning(3, routine, "Error adding where clauses (" + e + ")." );
    }
    String sqlString = ss.toString();
    Message.printStatus(2, routine, "Running:  " + sqlString );
    ResultSet rs = null;
    long l, id = -1;
    String s, units = "";
    int index;
    try {
        rs = dmi.dmiSelect(ss);
        while (rs.next()) {
            index = 1;
            l = rs.getLong(index++);
            if (!rs.wasNull()) {
                id = l;
            }
            s = rs.getString(8);
            if (!rs.wasNull()) {
                units = s;
            }
            metaList.add(new TimeSeriesMeta(locType, locID, dataSource, dataType, interval, scenario, units, id));
        }
    }
    catch ( Exception e ) {
        Message.printWarning ( 3, routine, "Error reading time series metadata from database with statement \"" + sqlString + "\" (" + e + ")."); 
    }
    finally {
        DMI.closeResultSet(rs);
    }
    return metaList;
}

/**
Read location ID strings for the data store, if time series features are configured.
Not a lot of error checking is done because the data store should have been checked out by this point
@param locType location type to use as filter (ignored if blank or null)
@param dataSource data source to use as filter (ignored if blank or null)
@param dataType data type to use as filter (ignored if blank or null)
@param interval interval to use as filter (ignored if blank or null)
@param scenario scenario to use as filter (ignored if blank or null)
@return the list of distinct location ID strings from time series metadata
*/
public List<String> readTimeSeriesMetaLocationIDList ( String locType, String dataSource,
    String dataType, String interval, String scenario )
{   String routine = "GenericDatabaseDataStore.readLocationIDStrings";
    DMI dmi = getDMI();
    List<String> locIDs = new Vector<String>();
    // Create a statement to read distinct data types from the time series metadata table
    DMISelectStatement ss = new DMISelectStatement(dmi);
    String metaTable = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_PROP );
    String ltColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_LOCTYPE_COLUMN_PROP );
    String idColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_LOCATIONID_COLUMN_PROP );
    String sourceColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_DATASOURCE_COLUMN_PROP );
    String dtColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_DATATYPE_COLUMN_PROP );
    String intervalColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_DATAINTERVAL_COLUMN_PROP );
    String scenarioColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_SCENARIO_COLUMN_PROP );
    ss.addTable(metaTable);
    ss.addField(idColumn);
    readTimeSeriesMetaAddWhere(ss,metaTable,ltColumn,locType);
    // LocationID is what is being read so don't filter
    readTimeSeriesMetaAddWhere(ss,metaTable,sourceColumn,dataSource);
    readTimeSeriesMetaAddWhere(ss,metaTable,dtColumn,dataType);
    readTimeSeriesMetaAddWhere(ss,metaTable,intervalColumn,interval);
    readTimeSeriesMetaAddWhere(ss,metaTable,scenarioColumn,scenario);
    ss.selectDistinct(true);
    ss.addOrderByClause(idColumn);
    String sqlString = ss.toString();
    Message.printStatus(2,routine,"Running:" + sqlString );
    ResultSet rs = null;
    String s;
    try {
        rs = dmi.dmiSelect(ss);
        while (rs.next()) {
            s = rs.getString(1);
            if (!rs.wasNull()) {
                locIDs.add(s);
            }
        }
    }
    catch ( Exception e ) {
        Message.printWarning ( 3, routine, "Error reading time series metadata from database with statement \"" + sqlString + "\" (" + e + ")."); 
    }
    finally {
        DMI.closeResultSet(rs);
    }
    return locIDs;
}

/**
Read location type strings for the data store, if time series features are configured.
Not a lot of error checking is done because the data store should have been checked out by this point
@param locID location ID to use as filter (ignored if blank or null)
@param dataSource data source to use as filter (ignored if blank or null)
@param dataType data type to use as filter (ignored if blank or null)
@param interval interval to use as filter (ignored if blank or null)
@param scenario scenario to use as filter (ignored if blank or null)
@return the list of interval strings by making a unique query of the 
*/
public List<String> readTimeSeriesMetaLocationTypeList ( String locID, String dataSource,
    String dataType, String interval, String scenario )
{   String routine = "GenericDatabaseDataStore.readLocationTypeStrings";
    DMI dmi = getDMI();
    List<String> locTypes = new Vector<String>();
    // Create a statement to read distinct data types from the time series metadata table
    DMISelectStatement ss = new DMISelectStatement(dmi);
    String metaTable = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_PROP );
    String ltColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_LOCTYPE_COLUMN_PROP );
    String idColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_LOCATIONID_COLUMN_PROP );
    String sourceColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_DATASOURCE_COLUMN_PROP );
    String dtColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_DATATYPE_COLUMN_PROP );
    String intervalColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_DATAINTERVAL_COLUMN_PROP );
    String scenarioColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_SCENARIO_COLUMN_PROP );
    ss.addTable(metaTable);
    ss.addField(ltColumn);
    // LocationType is what is being read so don't filter
    readTimeSeriesMetaAddWhere(ss,metaTable,idColumn,locID);
    readTimeSeriesMetaAddWhere(ss,metaTable,sourceColumn,dataSource);
    readTimeSeriesMetaAddWhere(ss,metaTable,dtColumn,dataType);
    readTimeSeriesMetaAddWhere(ss,metaTable,intervalColumn,interval);
    readTimeSeriesMetaAddWhere(ss,metaTable,scenarioColumn,scenario);
    ss.selectDistinct(true);
    ss.addOrderByClause(ltColumn);
    String sqlString = ss.toString();
    Message.printStatus(2,routine,"Running:" + sqlString );
    ResultSet rs = null;
    String s;
    try {
        rs = dmi.dmiSelect(ss);
        while (rs.next()) {
            s = rs.getString(1);
            if (!rs.wasNull()) {
                locTypes.add(s);
            }
        }
    }
    catch ( Exception e ) {
        Message.printWarning ( 3, routine, "Error reading time series metadata from database with statement \"" + sqlString + "\" (" + e + ")."); 
    }
    finally {
        DMI.closeResultSet(rs);
    }
    return locTypes;
}

/**
Utility method to add a where clause to the metadata select statement.
@param ss select statement to execute
@param table table to query
@param column for where clause
@param value value to use in where clause
*/
private void readTimeSeriesMetaAddWhere ( DMISelectStatement ss, String table, String column, String value )
{
    if ( (value != null) && !value.equals("") && !value.equals("*") ) {
        try {
            ss.addWhereClause(table + "." + column + " = '" + value + "'" );
        }
        catch ( Exception e ) {
            // Should not happen
        }
    }
}

/**
Read location type strings for the data store, if time series features are configured.
Not a lot of error checking is done because the data store should have been checked out by this point
@param locType location type to use as filter (ignored if blank or null)
@param locID location ID to use as filter (ignored if blank or null)
@param dataSource data source to use as filter (ignored if blank or null)
@param dataType data type to use as filter (ignored if blank or null)
@param interval interval to use as filter (ignored if blank or null)
@return the list of interval strings by making a unique query of the 
*/
public List<String> readTimeSeriesMetaScenarioList ( String locType, String locID,
    String dataSource, String dataType, String interval )
{   String routine = "GenericDatabaseDataStore.readDataSourceStrings";
    DMI dmi = getDMI();
    List<String> scenarios = new Vector<String>();
    // Create a statement to read distinct data types from the time series metadata table
    DMISelectStatement ss = new DMISelectStatement(dmi);
    String metaTable = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_PROP );
    String ltColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_LOCTYPE_COLUMN_PROP );
    String idColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_LOCATIONID_COLUMN_PROP );
    String sourceColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_DATASOURCE_COLUMN_PROP );
    String dtColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_DATATYPE_COLUMN_PROP );
    String intervalColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_DATAINTERVAL_COLUMN_PROP );
    String scenarioColumn = getProperty ( GenericDatabaseDataStore.TS_META_TABLE_SCENARIO_COLUMN_PROP );
    ss.addTable(metaTable);
    ss.addField(scenarioColumn);
    readTimeSeriesMetaAddWhere(ss,metaTable,ltColumn,locType);
    readTimeSeriesMetaAddWhere(ss,metaTable,idColumn,locID);
    readTimeSeriesMetaAddWhere(ss,metaTable,sourceColumn,dataSource);
    readTimeSeriesMetaAddWhere(ss,metaTable,dtColumn,dataType);
    readTimeSeriesMetaAddWhere(ss,metaTable,intervalColumn,interval);
    // Scenario is what is being read so don't filter
    ss.selectDistinct(true);
    ss.addOrderByClause(scenarioColumn);
    String sqlString = ss.toString();
    Message.printStatus(2,routine,"Running:" + sqlString );
    ResultSet rs = null;
    String s;
    try {
        rs = dmi.dmiSelect(ss);
        while (rs.next()) {
            s = rs.getString(1);
            if (!rs.wasNull()) {
                scenarios.add(s);
            }
        }
    }
    catch ( Exception e ) {
        Message.printWarning ( 3, routine, "Error reading time series metadata from database with statement \"" + sqlString + "\" (" + e + ")."); 
    }
    finally {
        DMI.closeResultSet(rs);
    }
    return scenarios;
}

}