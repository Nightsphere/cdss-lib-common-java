package RTi.Util.IO;


/**
 * Provides support for creating the HTML string for Command Status.
 * <p>
 * 
 * @author dre
 */
public class HTMLStatusAssembler
{
  private final StringBuffer TRAILER = new StringBuffer("</body></html>");
  private StringBuffer buf = new StringBuffer("<html><body>");
  // Add some spaces around the row count because the Java HTML viewer smashes together.
  private final String TABLE_START =
          "<table border=1 width=650><tr bgcolor=\"CCCCFF\"><th align=left>"
          + "&nbsp;&nbsp;&nbsp#&nbsp;&nbsp;&nbsp;</th>"
          + "<th align=left>Phase</th><th align=left>Severity</th>"
          + "<th align=left width=300>Problem</th><th>Recommendation</th></tr>";
  
  private final String SUMMARY_TABLE_START =
         "<table border=1>"
         + "<tr bgcolor=\"CCCCFF\"><th align=left>Phase</th><th align=left>Status/Max Severity</th></tr>";
  
  private final String TABLE_END = "</table>";
  
  /**
   * Creates a new HTML assembler for command status assembly.
   */
  public HTMLStatusAssembler()
  {
  }

  /**
   * Returns a HTML string ready for display in a browser.
   * 
   * @return HTML string 
   */
  public String getHTML()
  {
    buf.append(TRAILER);
    return buf.toString();
  }

  /**
   * Adds entry for a phase in HTML.
   * 
   * @param phase One of: INITIALIZATION,DISCOVERY,RUN
   * @param severity One of : WARNING,ERROR
   * @param color color associated with severity
   * @param problem problem encountered
   * @param recommendation recommended solution
   */
  public void addPhase( int count, String phase, String severity, String color, String problem,
      String recommendation )
  {
    String bgcolor = "</td><td valign=top bgcolor=" + color + ">";
    
    buf.append("<tr><td valign=top>" + count + "</td><td valign=top>" + phase + bgcolor + severity
            +"</td><td valign=top>" + HTMLUtil.text2html(problem,false) + "</td>"
            +"<td valign=top>" + HTMLUtil.text2html(recommendation,false) + "</td></tr>"
            );
  }
  
  /**
   * Adds an entry for a command in HTML.
   * <p>
   * Note for each addCommand(), a endCommand() is required.
   * @param commandString
   */
  public void addCommand(String commandString)
  {
    buf.append("<p><font bgcolor=white").append("<strong>Command: "+commandString).append("</strong></font>");
  }
  
  /**
   * Add HTML to start status table
   * @param nlog the number of log messages that will be shown (WARNING and more severe).
   */
  public void startCommandStatusTable( int nwarn, int nfail )
  {
    buf.append("<p><b>Command Status Details (" + nwarn + " warnings, " + nfail + " failures):");
    buf.append(TABLE_START); 
  }
  
  /**
   * Add HTML to terminate a command initiated with addCommand()
   */
  public void endCommand()
  {
    buf.append(TABLE_END);
  }

  public void addNotACommandStatusProvider()
  {
    buf.append("<tr><td>Not a CommandStatusProvider</td></tr>");
  }

  /**
   * Add the command status summary table
   * @param commandStatus1
   * @param commandStatus2
   * @param commandStatus3
   */
  public void addCommandStatusSummary(CommandStatusType commandStatus1,
          CommandStatusType commandStatus2,
          CommandStatusType commandStatus3)
  {
    
    String bgColor1 = "<td bgcolor=" +CommandStatusUtil.getStatusColor(commandStatus1) + ">";
    String bgColor2 = "<td bgcolor=" +CommandStatusUtil.getStatusColor(commandStatus2) + ">";
    String bgColor3 = "<td bgcolor=" +CommandStatusUtil.getStatusColor(commandStatus3) + ">";
    buf.append("<p><b>Command Status Summary</b> (see below for details if problems exist):");

    buf.append(SUMMARY_TABLE_START)
    .append("<tr><td>INITIALIZATION</td>").append(bgColor1).append(commandStatus1.toString()).append("</td></tr>")
    .append("<tr><td>DISCOVERY</td>").append(bgColor2).append(commandStatus2.toString()).append("</td></tr>")
    .append("<tr><td>RUN</td>").append(bgColor3).append(commandStatus3.toString()).append("</td></tr>")
    .append(TABLE_END);

  }
  public String toString()
  {
    return buf.toString();
  }
/**
 * Adds a summary table indicating no issues found.
 *
 */
  public void addNoProblems()
  {
    String bgColor1 = "<td bgcolor=" +CommandStatusUtil.getStatusColor(CommandStatusType.SUCCESS) + ">";
    buf.append(SUMMARY_TABLE_START)
    .append("<tr><td>INITIALIZATION").append(bgColor1).append(CommandStatusType.SUCCESS.toString()).append("</tr>")
    .append("<tr><td>DISCOVERY").append(bgColor1).append(CommandStatusType.SUCCESS.toString()).append("</tr>")
    .append("<tr><td>RUN").append(bgColor1).append(CommandStatusType.SUCCESS.toString()).append("</tr>")
    .append(TABLE_END);
  }
}