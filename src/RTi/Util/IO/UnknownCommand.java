package RTi.Util.IO;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;

import javax.swing.JFrame;	// For the editor

/**
This class is essentially the same as GenericCommand.  However, whereas GenericCommand might
be used for a known command, this UnknownCommand class can be used in cases where a command is
not known but needs to be managed.  The GenericCommand_JDialog is used to edit the command.
*/
public class UnknownCommand extends AbstractCommand
{

/**
Default constructor for a command.
*/
public UnknownCommand ()
{
}

/**
Check the command parameter for valid values, combination, etc.
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
Initialize the command by parsing the command and indicating warnings.
@param command A string command to parse.
@param full_initialization If true, the command string will be parsed and
checked for errors.  If false, a blank command will be initialized (e.g.,
suitable for creating a new command instance before editing in the command editor).
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
syntax of the command are bad.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void initializeCommand ( String command, CommandProcessor processor, boolean full_initialization )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	// Save the processor...
	super.initializeCommand ( command, processor, full_initialization );
	if ( full_initialization ) {
		// Parse the command...
		parseCommand ( command );
	}
}

/**
Parse the command string into a PropList of parameters.  Does nothing in this base class.
@param command A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
syntax of the command are bad.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand (	String command )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	// Does nothing.
}

/**
Run the command.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "UnknownCommand.runCommand";
    int warning_count = 0;
    int warning_level = 2;
    String command_tag = "" + command_number;

    if ( getCommandString().trim().equals("") ) {
        // Empty line so don't do anything
    }
    else {
        Message.printStatus ( 2, "UnknownCommand.runCommand", "In runCommand().");
        CommandStatus status = getCommandStatus();
        status.clearLog(CommandPhaseType.RUN);
        
        String message = "Do not know how to run unknown command \"" + toString() + "\"";
        Message.printWarning(warning_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE, message,
                "Verify command spelling and if necessary report the problem to software support." ) );
        
        // Throw an exception because if something tries to run with this it needs
        // to be made known that nothing is happening.
        
        throw new CommandException ( getCommandName() + " run() method is not enabled.");
    }
}

// TODO SAM 2005-05-31 If the editor is ever implemented with a tabular
// display for parameters, will need to deal with the parsing.  For now, this
// will at least allow unrecognized commands to be edited using the string representation.
/**
Return the string representation of the command.
This always returns the command string.
*/
public String toString()
{	return getCommandString();
}

}