package riverside.datastore;

import RTi.DMI.GenericDMI;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import riverside.datastore.DataStore;
import riverside.datastore.DataStoreFactory;

/**
Factory to instantiate ODBCDataStore instances.
@author sam
*/
public class GenericDatabaseDataStoreFactory implements DataStoreFactory
{

/**
Create an ODBCDataStore instance and open the encapsulated DMI using the specified properties.
*/
public DataStore create ( PropList props )
{
    String name = props.getValue ( "Name" );
    String description = props.getValue ( "Description" );
    if ( description == null ) {
        description = "";
    }
    String databaseEngine = IOUtil.expandPropertyForEnvironment("DatabaseEngine",props.getValue ( "DatabaseEngine" ));
    String databaseServer = IOUtil.expandPropertyForEnvironment("DatabaseServer",props.getValue ( "DatabaseServer" ));
    String databaseName = IOUtil.expandPropertyForEnvironment("DatabaseName",props.getValue ( "DatabaseName" ));
    String odbcName = IOUtil.expandPropertyForEnvironment("OdbcName",props.getValue ( "OdbcName" ));
    String systemLogin = IOUtil.expandPropertyForEnvironment("SystemLogin",props.getValue ( "SystemLogin" ));
    String systemPassword = IOUtil.expandPropertyForEnvironment("SystemPassword",props.getValue ( "SystemPassword" ));
    try {
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
            // Use the parts to create the connection
            dmi = new GenericDMI( databaseEngine, databaseServer,
                databaseName, -1, systemLogin, systemPassword );
        }
        dmi.open();
        return new GenericDatabaseDataStore ( name, description, dmi );
    }
    catch ( Exception e ) {
        // TODO SAM 2010-09-02 Wrap the exception because need to move from default Exception
        throw new RuntimeException ( e );
    }
}

}