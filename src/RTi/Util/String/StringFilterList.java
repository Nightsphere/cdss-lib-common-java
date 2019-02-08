// StringFilterList - list of string filter data to be evaluated by include/exclude checks

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

package RTi.Util.String;

import java.util.ArrayList;
import java.util.List;

/**
 * List of string filter data to be evaluated by include/exclude checks.
 * @author sam
 *
 */
public class StringFilterList {
	
	/**
	 * List of keys, for example these can be column names or properties.
	 */
	private List<String> keys = new ArrayList<String>();
	
	/**
	 * List of filter patterns to match, no constraint on whether globbing or other regex.
	 */
	private List<String> patterns = new ArrayList<String>();

	/**
	 * Constructor.
	 */
	public StringFilterList () {
	}
	
	/**
	 * Add a filter.
	 * @param key key for filter
	 * @param pattern filter pattern
	 */
	public void add ( String key, String pattern ) {
		keys.add(key);
		patterns.add(pattern);
	}
	
	/**
	 * Return the key at the position.
	 * @param pos filter position 0+.
	 */
	public String getKey ( int pos ) {
		return keys.get(pos);
	}
	
	/**
	 * Return the filter pattern at the position.
	 * @param pos filter position 0+.
	 */
	public String getPattern ( int pos ) {
		return patterns.get(pos);
	}
	
	/**
	 * Return the size of the filter list.
	 */
	public int size () {
		return keys.size();
	}
}
