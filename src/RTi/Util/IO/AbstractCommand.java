//------------------------------------------------------------------------------
// SkeletonCommand - a command base class skeleton, to hold typical data
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2005-04-29	Steven A. Malers, RTi	Initial version.
// 2005-05-19	SAM, RTi		Move from TSTool package.
// 2006-01-09	J. Thomas Sapienza, RTi	Added getCommandString().
// 2007-05-08	SAM, RTi		Cleanup code based on Eclipse feedback.
//------------------------------------------------------------------------------
// EndHeader

package RTi.Util.IO;

import java.util.List;
import javax.swing.JFrame;	// For the editor

import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;

/**
This class can be used as a base class for other commands.  It contains data
members and access methods that will commonly be used.
Note that the derived class should implement the Command interface methods.  Its
implementation here will be insufficient for most needs (e.g., editing).
*/
public abstract
class AbstractCommand extends Object implements Command, CommandStatusProvider, CommandProcessorEventProvider
{

/**
The full command string for the command, as specified during initialization.
This is initialized to blank.
*/
private String __command_string = "";

/**
The command name only, as taken from the command string.
This is initialized to blank;
*/
private String __command_name = "";

/**
The command processor, which will run the command and be associated with the command GUI.
*/
private CommandProcessor __processor = null;

/**
Array of CommandProcessorEventListeners.
*/
private CommandProcessorEventListener [] __CommandProcessorEventListener_array = null;

/**
Array of CommandProgressListeners.
*/
private CommandProgressListener [] __CommandProgressListener_array = null;

/**
The command parameters, determined from processing the command string.
This is initialized to an empty PropList and should be set when initializing the command.
*/
private PropList __parameters = new PropList ( "" );

/**
The run time for the command, in milliseconds, used to evaluate and optimize performance.
*/
private long __runTime = 0;

/**
The status for the command.
*/
private CommandStatus __status = new CommandStatus();

/**
Default constructor for a command.
*/
public AbstractCommand ()
{
}

/**
Add a CommandProcessorEventListener.
@param listener a CommandProcessorEventListener, to handle events generated by this command.
*/
public void addCommandProcessorEventListener ( CommandProcessorEventListener listener )
{
    // Use arrays to make a little simpler than Vectors to use later...
    if ( listener == null ) {
        return;
    }
    // See if the listener has already been added...
    // Resize the listener array...
    int size = 0;
    if ( __CommandProcessorEventListener_array != null ) {
        size = __CommandProcessorEventListener_array.length;
    }
    for ( int i = 0; i < size; i++ ) {
        if ( __CommandProcessorEventListener_array[i] == listener ) {
            return;
        }
    }
    if ( __CommandProcessorEventListener_array == null ) {
        __CommandProcessorEventListener_array = new CommandProcessorEventListener[1];
        __CommandProcessorEventListener_array[0] = listener;
    }
    else {
        // Need to resize and transfer the list...
        size = __CommandProcessorEventListener_array.length;
        CommandProcessorEventListener [] newlisteners = new CommandProcessorEventListener[size + 1];
        for ( int i = 0; i < size; i++ ) {
                newlisteners[i] = __CommandProcessorEventListener_array[i];
        }
        __CommandProcessorEventListener_array = newlisteners;
        __CommandProcessorEventListener_array[size] = listener;
        newlisteners = null;
    }
}

/**
Add a CommandProgressListener.
@param listener a CommandProgressListener, to handle events generated by this command.
*/
public void addCommandProgressListener ( CommandProgressListener listener )
{
    // Use arrays to make a little simpler than lists to use later...
    if ( listener == null ) {
        return;
    }
    // See if the listener has already been added...
    // Resize the listener array...
    int size = 0;
    if ( __CommandProgressListener_array != null ) {
        size = __CommandProgressListener_array.length;
    }
    for ( int i = 0; i < size; i++ ) {
        if ( __CommandProgressListener_array[i] == listener ) {
            return;
        }
    }
    if ( __CommandProgressListener_array == null ) {
    	__CommandProgressListener_array = new CommandProgressListener[1];
    	__CommandProgressListener_array[0] = listener;
    }
    else {
        // Need to resize and transfer the list...
        size = __CommandProgressListener_array.length;
        CommandProgressListener [] newlisteners = new CommandProgressListener[size + 1];
        for ( int i = 0; i < size; i++ ) {
            newlisteners[i] = __CommandProgressListener_array[i];
        }
        __CommandProgressListener_array = newlisteners;
        __CommandProgressListener_array[size] = listener;
        newlisteners = null;
    }
}

/**
Check the command parameter for valid values, combination, etc.
This should normally be implemented in the derived class.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{
}

/**
Clone the instance.  All command data are cloned except for, the following,
which use the same references as the original object:  CommandProcessor.
*/
public Object clone ()
{	try {
        AbstractCommand command = (AbstractCommand)super.clone();
		// _command_string and _command_name are automatically cloned
		// Processor is not cloned, use a reference to the same processor
		command.__processor = __processor;
		// Clone the status...
		command.__status = (CommandStatus)__status.clone();
		// Clone the parameters - do this the brute force way for now using
		// string properties but later need to evaluate, especially if
		// full objects are used for parameters...
		// TODO SAM 2007-09-02 Need full clone() on PropList
		PropList props = new PropList ( "" );
		int size = command.__parameters.size();
		Prop prop = null;
		for ( int i = 0; i < size; i++ ) {
			prop = command.__parameters.elementAt(i);
			if ( prop == null ) {
				// Should not happen.
			}
			else {
				props.set ( prop.getKey(),	prop.getValue() );
			}
		}
		command.__parameters = props;
		return command;
	}
	catch ( CloneNotSupportedException e ) {
		// Should not happen because everything is cloneable.
		throw new InternalError();
	}
}

/**
Edit a command instance.  The instance may be a newly created command or one
that has been created previously and is now being re-edited.
@return the Command instance that is created and edited, or null if the edit was cancelled.
@param parent Parent JFrame on which the model command editor dialog will be shown.
*/
public boolean editCommand ( JFrame parent )
{	// Use the generic command editor...	
	return (new GenericCommand_JDialog ( parent, this )).ok();
}

/**
Edit a new command.
@return the Command instance that is created and edited, or null if the edit was cancelled.
@param parent Parent JFrame on which the model command editor dialog will be shown.
*/
/* TODO SAM 2005-04-29 need to figure out a graceful way to do this...
CommandFactory?
public static Command editNewCommand ( JFrame parent )
{	Command c = new Command();
	if ( c.editCommand(parent).ok() ) {
		return c;
	}
	else {	return null;
	}
}
*/

/**
Return the command name, from the command string.
@return the command name, from the command string.
*/
public String getCommandName ()
{	return __command_name;
}

/**
Return the parameters being used by the command.  The Prop.getHowSet() method
can be used to determine whether a property was defined in the original command
string (Prop.SET_FROM_PERSISTENT) or is defaulted internally
(Prop.SET_AS_RUNTIME_DEFAULT).
TODO SAM 2005-04-29 Does this need a boolean parameter to allow dialogs to
see only the parameters in the command, so that defaults are not explicitly displayed?
@return the parameters being used by the command.  A non-null list is guaranteed.
*/
public PropList getCommandParameters ()
{	return __parameters;
}

/**
Return the command processor that is managing the command.
@return the CommandProcessor used to process the command.
*/
public CommandProcessor getCommandProcessor ()
{	return __processor;
}

/**
Return the status for the command.  The version provided in this abstract
version returns UNKNOWN for the status.  Commands that extend from this abstract
class should set the status more explicitly.
*/
public CommandStatus getCommandStatus ()
{
	return __status;
}

/**
Returns the original command string.
@return the original command string.
*/
public String getCommandString() {
	return __command_string;
}

/**
Return the runtime, in milliseconds.
*/
public long getRunTime () 
{
	return __runTime;
}

/**
Initialize the command by parsing the command and indicating warnings.
@param command A string command to parse.  This is necessary because the
command factory typically only uses a command string to instantiate the proper
Command class, but parameters are not parsed until this method is called.
@param full_initialization If true, the command string will be parsed and
checked for errors (by calling parseCommand()).  If false, a blank command will
be initialized (e.g., suitable for creating a new command instance before
editing in the command editor).
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void initializeCommand ( String command, CommandProcessor processor,	boolean full_initialization )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	// Save the processor...
	__processor = processor;
	__command_string = command;
	if ( full_initialization ) {
		// Parse the command...
		parseCommand ( command );
	}
}

// TODO SAM 2005-05-13 Might need to overload this to pass the routine
// from the derived class, and then rely on the standard method.

/**
Notify registered CommandProcessorEventListeners of a CommandProcessorEvent.
@param event event to pass to listeners.
*/
public void notifyCommandProcessorEventListeners ( CommandProcessorEvent event )
{
	if ( __CommandProcessorEventListener_array != null ) {
	    for ( int i = 0; i < __CommandProcessorEventListener_array.length; i++ ) {
	        __CommandProcessorEventListener_array[i].handleCommandProcessorEvent(event);
	    }
	}
}

/**
Notify registered CommandProgressListeners of a CommandProgressEvent.
@param istep The number of steps being executed in a command (0+), for example loop index of
objects being processed.
@param nstep The total number of steps to process within a command, for example total number of objects
being processed.
@param command The reference to the command that is starting to run,
provided to allow future interaction with the command.
@param percentComplete If >= 0, the value can be used to indicate progress
running a single command (not the single command).  If less than zero, then
no estimate is given for the percent complete and calling code can make its
own determination (e.g., ((istep + 1)/nstep)*100).
@param message A short message describing the status (e.g., "Running command ..." ).
*/
public void notifyCommandProgressListeners ( int istep, int nstep, float percentComplete, String message )
{
	if ( __CommandProgressListener_array != null ) {
	    for ( int i = 0; i < __CommandProgressListener_array.length; i++ ) {
	        __CommandProgressListener_array[i].commandProgress(istep, nstep, this, percentComplete, message);
	    }
	}
}

/**
Parse the command string into a PropList of parameters.  This method will
parse a standard syntax command:
<pre>
commandName(param=value,param="value",...)
</pre>
@param command A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	String routine = "SkeletonCommand.parseCommand", message;
	List tokens = StringUtil.breakStringList ( command, "()", StringUtil.DELIM_SKIP_BLANKS );
	if ( (tokens == null) ) {
		message = "Invalid syntax for \"" + command + "\".  Not enough tokens.";
		Message.printWarning ( 2, routine, message);
		throw new InvalidCommandSyntaxException ( message );
	}
	// Get the input needed to process the command...
	if ( tokens.size() > 1 ) {
		// Parameters are available to parse...
		try {
		    __parameters = PropList.parse ( Prop.SET_FROM_PERSISTENT, (String)tokens.get(1), routine,"," );
		}
		catch ( Exception e ) {
			message = "Syntax error in \"" + command + "\".  Not enough tokens.";
			Message.printWarning ( 2, routine, message);
			throw new InvalidCommandSyntaxException ( message );
		}
	}
}

/**
Run the command.
@param command_number The command number from the processor (0+), used to cross-reference
the log to command instances.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	// Does nothing.
}

/**
Set the command name, as taken from the command string.
@param command_name The command name.
*/
public void setCommandName ( String command_name )
{	__command_name = command_name;
}

/**
Set a command parameter.  This is used, for example, by a command editor dialog,
and results in command parameter PropList being updated and the command string being regenerated.
@param parameter Name of parameter to set.
@param value Value of parameter to set.  Passing a value of null will
effectively unset the parameter (null will be returned when retrieving the
parameter value, requiring handling).
*/
public void setCommandParameter ( String parameter, String value )
{	__parameters.set ( parameter, value );
	// Refresh the command string...
	__command_string = toString();
}

/**
Set the command parameters.  This is most often called when the parameters have
been parsed.
@param parameters The command parameters as a PropList - only String parameter values are recognized.
*/
public void setCommandParameters ( PropList parameters )
{	__parameters = parameters;
	//Refresh the command string...
	__command_string = toString();
}

/**
Set the command processor.  Normally this is set in the runCommand() method.
@param processor The CommandProcessor used to process the command.
*/
public void setCommandProcessor ( CommandProcessor processor )
{	__processor = processor;
}

/**
Set the command string.  This is currently used only by the generic command
editor and should only be implemented in this SkeletonCommand base class.
@param command_string Command string for the command.
*/
public void setCommandString ( String command_string )
{	__command_string = command_string;
}

/**
Set the runtime in milliseconds.  This is used to evaluate and optimize performance.
@param runTime the runtime in milliseconds.
*/
public void setRunTime ( long runTime )
{
	__runTime = runTime;
}

/**
Return the command string.  Note that this method is required by the Command
interface.  This version can be relied on to satisfy that requirement.
@return the command string.
*/
public String toString()
{	return toString ( __parameters );
}

/**
Return the command string, formed from the properties.  Note that this method
is required by the Command interface.  This version can be relied on to satisfy
that requirement but in most cases should be overruled.  No attempt is made to
determine the property type and all values will have surrounding double quotes.
@param parameters The list of parameters for the command.
@return the command string.
*/
public String toString ( PropList parameters )
{	if ( parameters == null ) {
		return __command_name + "()";
	}
	StringBuffer b = new StringBuffer ();
	List v = parameters.getList();
	int size = v.size();
	String value;
	Prop prop;
	for ( int i = 0; i < size; i++ ) {
		prop = (Prop)v.get(i);
		value = prop.getValue();
		if ( (value != null) && (value.length() > 0) ) {
			if ( b.length() > 0 ) {
				b.append ( "," );
			}
			b.append ( prop.getKey() + "=\"" + value + "\"" );
		}
	}
	return (__command_name + "(" + b.toString() + ")" );
}

}