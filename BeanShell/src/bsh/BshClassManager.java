package bsh;

import java.net.*;
import java.util.*;
import java.lang.ref.WeakReference;
import java.io.IOException;
import BshClassPath.DirClassSource;

/**
	Manage all classloading in BeanShell.
	Allows classpath extension and class file reloading.

	Currently relies on 1.2 for BshClassLoader and weak references.
	Is there a workaround for weak refs?  If so we could make this work
	with 1.1 by supplying our own classloader code...

	@see http://www.beanshell.org/manual/classloading.html for details
	on the bsh classloader architecture.

	Bsh has a multi-tiered class loading architecture.  No class loader is
	used unless/until the classpath is modified or a class is reloaded.

	Note: need some synchronization in here
*/
public class BshClassManager
{
	/**
		The classpath of the base loader.  Initially empty.
		This grows as paths are added or is reset when the classpath 
		is explicitly set.
	*/
	BshClassPath baseClassPath;

	// ClassPath Change listeners
	Vector listeners = new Vector();

	/**
		This handles extension / modification of the base classpath
		The loader to use where no mapping of reloaded classes exists.

		The baseLoader is initially null meaning no class loader is used.
	*/
	BshClassLoader baseLoader;

	/**
		Map by classname of loaders to use for reloaded classes
	*/
	Map loaderMap;

	// Singleton for now

	private static BshClassManager manager = new BshClassManager();
	public static BshClassManager getClassManager() {
		return manager;
	}
	private BshClassManager() { 
		reset();
	}

	public Class getClassForName( String name ) {
		Class c = null;

		ClassLoader overlayLoader = getLoaderForClass( name );
		if ( overlayLoader != null ) {
			try {
				c = overlayLoader.loadClass(name);
			} catch ( Exception e ) {
			} catch ( NoClassDefFoundError e2 ) {
			}

			// Should be there since it was explicitly mapped
			// throw an error?
			if ( c != null )
				return c;
		}

		if ( baseLoader != null )
			try {
				c = baseLoader.loadClass( name );
			} catch ( ClassNotFoundException e ) {}
		else
			try {
				c = loadSystemClass( name );
			} catch ( ClassNotFoundException e ) {}

		return c;
	}

	protected ClassLoader getBaseLoader() {
		return baseLoader;
	}

	protected Class loadSystemClass( String name ) 
		throws ClassNotFoundException 
	{
		try {
			return Class.forName(name);
		} catch ( NoClassDefFoundError e ) {
			/*
			This is weird... jdk under Win is throwing these to
			warn about lower case / upper case possible mismatch.
			e.g. bsh.console bsh.Console
			*/
			throw new ClassNotFoundException( e.toString() );
		}
	}

	public ClassLoader getLoaderForClass( String name ) {
		return (ClassLoader)loaderMap.get( name );
	}

	// Classpath mutators

	/**
	*/
	public void addClassPath( URL path ) throws IOException {
		if ( baseLoader == null )
			setClassPath( new URL [] { path } );
		else {
// opportunitty here for listener in classpath
			baseLoader.addURL( path );
			baseClassPath.add( path );
			classLoaderChanged();
		}
	}

	/**
		Clear all loaders and start over.
	*/
	public void reset()
	{
		baseClassPath = new BshClassPath();
		baseLoader = null;
		loaderMap = new HashMap();
		classLoaderChanged();
	}

	/**
		Set the base classpath with a new classloader.
		This means all types change.  This also resets the bsh classpath 
		object which may be expensive to regenerate.
	*/
	public void setClassPath( URL [] cp ) {
		reset();
		baseClassPath.add( cp );
		initBaseLoader();

		// fire after change...  semantics are "has changed"
		classLoaderChanged();
	}

	/**
		init the base loader from the current classpath
	*/
	private void initBaseLoader() {
		baseLoader = new BshClassLoader( baseClassPath );
	}

	// class reloading

	/**
		Reloading classes means creating a new classloader and using it
		whenever we are asked for classes in the appropriate space.
		For this we use a DiscreteFilesClassLoader
	*/
	public void reloadClasses( String [] classNames ) 
		throws ClassPathException
	{
		// validate that it is a class here?

		// init base class loader if there is none...
		if ( baseLoader == null )
			initBaseLoader();

		DiscreteFilesClassLoader.ClassSourceMap map = 
			new DiscreteFilesClassLoader.ClassSourceMap();

		for (int i=0; i< classNames.length; i++) {
			String name = classNames[i];

			// look in base loader class path 
			Object o = baseClassPath.getClassSource( name );

			// look in user class path 
			if ( o == null )
				o = BshClassPath.getUserClassPath().getClassSource( name );
				
			if ( o == null )
				throw new ClassPathException("Nothing known about class: "
					+name );

			if ( ! (o instanceof DirClassSource) )
				throw new ClassPathException("Cannot reload class: "+name+
					" from source: "+o);

			map.put( name, ((DirClassSource)o).getDir() );
		}

		// Create classloader for the set of classes with the base loader
		// as the parent
		ClassLoader cl = new DiscreteFilesClassLoader( map, baseLoader );

		// map those classes the loader in the overlay map
		Iterator it = map.keySet().iterator();
		while ( it.hasNext() )
			loaderMap.put( (String)it.next(), cl );

		classLoaderChanged();
	}

	/**
		Reload all classes in the specified package: e.g. "com.sun.tools"

		The special package name "<unpackaged>" can be used to refer 
		to unpackaged classes.
	*/
	public void reloadPackage( String pack ) throws ClassPathException {
		Collection classes = 
			baseClassPath.getClassesForPackage( pack );

		if ( classes == null )
			classes = 
				BshClassPath.getUserClassPath().getClassesForPackage( pack );

		if ( classes == null )
			throw new ClassPathException("No classes found for package: "+pack);

		reloadClasses( (String[])classes.toArray( new String[0] ) );
	}

	/**
		Unimplemented
		For this we'd have to store a map by location as well as name...

	public void reloadPathComponent( URL pc ) throws ClassPathException {
		throw new ClassPathException("Unimplemented!");
	}
	*/

	// end reloading

	/**
	which one?
	public BshClassPath getClassPath() {
		return baseClassPath;
	}
	*/

	public static Class classForName( String name ) {
		return getClassManager().getClassForName( name );
	}

	static interface Listener {
		public void classLoaderChanged();
	}

	public void addListener( Listener l ) {
		listeners.addElement( new WeakReference(l) );
	}
	public void removeListener( Listener l ) {
		listeners.removeElement( l );
	}

	/**
		Clear global class cache and notify namespaces to clear their 
		class caches.

		The listener list is implemented with weak references so that we 
		will not keep every namespace in existence forever.
	*/
	void classLoaderChanged() {
    	NameSpace.absoluteNonClasses = new Hashtable();
    	NameSpace.absoluteClassCache = new Hashtable();

		for (Enumeration e = listeners.elements(); e.hasMoreElements(); ) {
			WeakReference wr = (WeakReference)e.nextElement();
			Listener l = (Listener)wr.get();
			if ( l == null )  // garbage collected
				listeners.removeElement( wr );
			else
				l.classLoaderChanged();
		}

	}

	public void dump( Interpreter i ) {
		i.println("Bsh Class Manager Dump: ");
		i.println("----------------------- ");
		i.println("baseLoader = "+baseLoader);
		i.println("loaderMap= "+loaderMap);
		i.println("----------------------- ");
		i.println("baseClassPath = "+baseClassPath);
	}

}
