/*****************************************************************************
 *                                                                           *
 *  This file is part of the BeanShell Java Scripting distribution.          *
 *  Documentation and updates may be found at http://www.beanshell.org/      *
 *                                                                           *
 *  Sun Public License Notice:                                               *
 *                                                                           *
 *  The contents of this file are subject to the Sun Public License Version  *
 *  1.0 (the "License"); you may not use this file except in compliance with *
 *  the License. A copy of the License is available at http://www.sun.com    * 
 *                                                                           *
 *  The Original Code is BeanShell. The Initial Developer of the Original    *
 *  Code is Pat Niemeyer. Portions created by Pat Niemeyer are Copyright     *
 *  (C) 2000.  All Rights Reserved.                                          *
 *                                                                           *
 *  GNU Public License Notice:                                               *
 *                                                                           *
 *  Alternatively, the contents of this file may be used under the terms of  *
 *  the GNU Lesser General Public License (the "LGPL"), in which case the    *
 *  provisions of LGPL are applicable instead of those above. If you wish to *
 *  allow use of your version of this file only under the  terms of the LGPL *
 *  and not to allow others to use your version of this file under the SPL,  *
 *  indicate your decision by deleting the provisions above and replace      *
 *  them with the notice and other provisions required by the LGPL.  If you  *
 *  do not delete the provisions above, a recipient may use your version of  *
 *  this file under either the SPL or the LGPL.                              *
 *                                                                           *
 *  Patrick Niemeyer (pat@pat.net)                                           *
 *  Author of Learning Java, O'Reilly & Associates                           *
 *  http://www.pat.net/~pat/                                                 *
 *                                                                           *
 *****************************************************************************/

package bsh.util;

import java.util.*;
import bsh.StringUtil;
import bsh.NameSource;

/**
	NameCompletionTable is a utility that implements simple name completion for 
	a collection.  This uses a trivial linear search and comparison...  
	
	NameCompletionTable can compose children of other NameCompletionTables
	and NameSources.
*/
public class NameCompletionTable extends ArrayList
	implements NameCompletion
{
	/** Unimplemented - need a collection here */
	NameCompletionTable table;

	/** Unimplemented - need a collection of sources here*/

	/**
	*/
	public NameCompletionTable() { }

	/**
		Add a NameCompletionTable, which is more optimized than the more
		general NameSource
	*/
	public void add( NameCompletionTable table ) {
		/** Unimplemented - need a collection here */
		if ( this.table != null )
			throw new RuntimeException("Unimplemented error");

		this.table = table;
	}

	/**
		Add a NameSource which is monitored for names.
		Unimplemented - behavior is broken... no updates
	*/
	public void add( NameSource source ) {
		/** Unimplemented - need a collection here */
		/*
			Unimplemented -
			Need to add an inner class util here that holds the source and
			monitors it by registering a listener
		*/
		addAll( Arrays.asList(source.getAllNames()) );
	}

	/**
		Add any matching names to list (including any from other tables)
	*/
	protected void getMatchingNames( String part, List found ) 
	{
		Iterator it = iterator();
		while( it.hasNext() ) {
			String name = (String)it.next();
			if ( name.startsWith( part ) )
				found.add( name );
		}

		/** Unimplemented - need a collection here */
		if ( table != null )
			table.getMatchingNames( part, found );
	}

	public String [] completeName( String part ) 
	{
		List found = new ArrayList();
		getMatchingNames( part, found );

		if ( found.size() == 0 )
			return new String [0];

		// Find the max common prefix
		String maxCommon = (String)found.get(0);
		for(int i=1; i<found.size() && maxCommon.length() > 0; i++) {
			maxCommon = StringUtil.maxCommonPrefix( 
				maxCommon, (String)found.get(i) );

			// if maxCommon gets as small as part, stop trying
			if ( maxCommon.equals( part ) )
				break;
		}

		// Return max common or all ambiguous
		if ( maxCommon.length() > part.length() )
			return new String [] { maxCommon };
		else
			return (String[])(found.toArray(new String[0]));
	}

	/**
		Unimplemented -
	class SourceMonitor implements NameSource.Listener
	{
		public void nameSourceChanged( NameSource src ) { }
	}
	*/
}
