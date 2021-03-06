// ChoiceFormatterJPanel - panel to provide simple editing capabilities to construct a string given a list of choices

/* NoticeStart

CDSS Common Java Library
CDSS Common Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2019 Colorado Department of Natural Resources

CDSS Common Java Library is free software:  you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    CDSS Common Java Library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with CDSS Common Java Library.  If not, see <https://www.gnu.org/licenses/>.

NoticeEnd */

package RTi.Util.GUI;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyListener;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.String.StringUtil;

/**
Panel to provide simple editing capabilities to construct a string given a list of choices from which
one or more selections can be made.  The panel exposes the getText() method from the JTextField so that
the edited contents can be retrieved.
*/
@SuppressWarnings("serial")
public class ChoiceFormatterJPanel extends JPanel implements ItemListener
{

/**
Hint to aid user.
*/
private String __hint = "-- Select Specifier --";

/**
Delimiter that separates choice from description (e.g., "Choice - Description...").
*/
private String __choiceDelim = null;

/**
Delimiter that separates selected choices (e.g., comma in "choice1,choice2").
*/
private String __insertDelim = null;

/**
Whether append is allowed.  If true, then append choices using the delimiter.  If false, overwrite the
text field contents with choice.
*/
private boolean __append = true;

/**
Text field containing the edited format specifier.
*/
private JTextField __inputJTextField = null;

/**
Choices for the list of format specifiers.
*/
private SimpleJComboBox __formatJComboBox = null;

/**
Control constructor.
@param choices string choices that will be displayed to the user; choices can be simple strings or compound
strings consisting of values and description (the "delim" parameter indices if a delimiter is used, for
example as VALUE - DESCRIPTION)
@param choiceDelim delimiter that is used to separate data choices and description in the choices string;
specify as null or blank if not used
@param tooltip tooltip for the choice component
@param hint string to display by default to guide the user like "Please select...", and which will
be ignored as a valid choice
@param insertDelim delimiter to be inserted when transferring choices (e.g, if a comma is specified then
a comma is automatically inserted when choices are selected; specify as null or blank if not used
@param width width of the JTextField to be included in the control (or -1) to not specify.
@param append if true, then choices are appended to the text field, separated by the delimited; if false,
choices replace the contents of the text field
*/
public ChoiceFormatterJPanel ( List<String> choices, String choiceDelim, String tooltip, String hint,
    String insertDelim, int width, boolean append )
{   __hint = hint;
    __choiceDelim = choiceDelim;
    __insertDelim = insertDelim;
    __append = append;
    setLayout ( new GridBagLayout() );
    Insets insetsTLBR = new Insets(0,0,0,0);

    int y = 0;
    int x = 0;
    __formatJComboBox = new SimpleJComboBox ( false );
    if ( (hint != null) && (hint.length() > 0) ) {
        choices.add(0,__hint);
    }
    __formatJComboBox.setData(choices);
    __formatJComboBox.addItemListener ( this );
    JGUIUtil.addComponent(this, __formatJComboBox,
        x++, y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(this, new JLabel(" => "),
        x++, y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    if ( width > 0 ) {
        __inputJTextField = new JTextField ( width );
    }
    else {
        __inputJTextField = new JTextField ();
    }
    if ( (tooltip != null) && (tooltip.length() > 0) ) {
        __formatJComboBox.setToolTipText(tooltip);
    }
    // Make sure caret stays visible even when not in focus, but use light gray so as to not
    // confuse with the component that is in focus
    __inputJTextField.setCaretColor( Color.lightGray );
    __inputJTextField.getCaret().setVisible ( true );
    __inputJTextField.getCaret().setSelectionVisible ( true );
    JGUIUtil.addComponent(this, __inputJTextField,
        x++, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
}

/**
 * Add a KeyListener for the text field.
 */
public void addKeyListener ( KeyListener listener )
{
    __inputJTextField.addKeyListener ( listener );
}

/**
 * Add a DocumentListener for the text field.
 */
public void addDocumentListener ( DocumentListener listener )
{
    __inputJTextField.getDocument().addDocumentListener ( listener );
}

/**
Return the Document associated with the text field.
*/
public Document getDocument()
{
    return __inputJTextField.getDocument();
}

/**
Return the SimpleJComboBox used in the panel, useful for setting properties in calling code.
*/
public SimpleJComboBox getSimpleJComboBox()
{
    return __formatJComboBox;
}

/**
Return the text in the text field.
*/
public String getText()
{
    return __inputJTextField.getText();
}

/**
Respond to ItemEvents - user has selected from the list so insert into the cursor position in the
text field.
@param evt Item event due to list change, etc.
*/
public void itemStateChanged ( ItemEvent evt )
{
    // Only insert on select..
    if ( evt.getStateChange() == ItemEvent.SELECTED ) {
        String selection = __formatJComboBox.getSelected();
        if ( (__hint == null) || (__hint.length() == 0) || !selection.equals(__hint)) {
            // Selection is not the hint so process the selection
            if ( (__choiceDelim != null) && (__choiceDelim.length() != 0) ) {
                // Further split out the selection
                selection = StringUtil.getToken ( __formatJComboBox.getSelected(), __choiceDelim, 0, 0 ).trim();
            }
            if ( __append ) {
                int pos = __inputJTextField.getCaretPosition();
                String text = __inputJTextField.getText();
                String delim1 = "", delim2 = "";
                if ( (__insertDelim != null) && !__insertDelim.equals("") ) {
                    if ( pos == 0 ) {
                        // Inserting at the start
                        if ( (text.length() > 0) && (text.charAt(0) != ',') ) {
                            delim2 = ",";
                        }
                    }
                    else if ( pos == text.length() ) {
                        // Inserting at the end
                        if ( (text.charAt(pos - 1) != ',') ) {
                            delim1 = ",";
                        }
                    }
                    else {
                        // Inserting in the middle
                        if ( (text.charAt(pos - 1) != ',') ) {
                            delim1 = ",";
                        }
                        if ( (text.charAt(pos) != ',') ) {
                            delim2 = ",";
                        }
                    }
                }
                String newText = text.substring(0,pos) + delim1 + selection + delim2 + text.substring(pos);
                __inputJTextField.setText ( newText );
                // Make sure caret stays visible even when not in focus
                __inputJTextField.getCaret().setVisible ( true );
                __inputJTextField.getCaret().setSelectionVisible ( true );
            }
            else {
                // Just transfer the value to the text field.
                __inputJTextField.setText ( selection );
            }
        }
    }
}

/**
Set the components enabled state.
@param enabled whether or not the components should be enabled
*/
public void setEnabled ( boolean enabled )
{
    __inputJTextField.setEnabled ( enabled );
    __formatJComboBox.setEnabled ( enabled );
}

/**
 * Set the text in the text field.
 * @param text text to set in the textfield
 */
public void setText( String text )
{
    __inputJTextField.setText ( text );
}

}
