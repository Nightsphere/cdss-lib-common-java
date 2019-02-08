// TSViewType - time series view window types, used to manage windows

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

package RTi.GRTS;

/**
Time series view window types, used to manage windows.
*/
public enum TSViewType
{
    /**
    Graph view.
    */
    GRAPH("Graph"),
    /**
    Properties view, currently only used with graph but may properties may be used with other views.
    */
    PROPERTIES("Properties"),
    /**
    Properties view (not visible), used to programatically make quick changes to properties.
    */
    PROPERTIES_HIDDEN("PropertiesHidden"),
    /**
    Summary view.
    */
    SUMMARY("Summary"),
    /**
    Table view.
    */
    TABLE("Table");
    
    private final String displayName;

    /**
     * Name that should be displayed in choices, etc.
     * @param displayName
     */
    private TSViewType(String displayName) {
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
    public static TSViewType valueOfIgnoreCase(String name)
    {
        TSViewType [] values = values();
        for ( TSViewType t : values ) {
            if ( name.equalsIgnoreCase(t.toString()) ) {
                return t;
            }
        } 
        return null;
    }
}
