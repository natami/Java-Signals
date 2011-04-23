/* 
 * Copyright (c) 2011 Paul Moore
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package com.paulm.jsignal;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Signal class acts as an event dispatcher for
 * one type of event.  Each listener object registers
 * directly to the object, no constant event type
 * identifiers are required.
 * 
 * This class logs to the </code>"com.paulm.jsignal"</code> Logger potential problems.
 * 
 * This is a port of Robert Penner's Signals for ActionScript 3.0
 * 
 * @author Paul Moore
 */
public class Signal implements ISignalOwner
{
	protected Class<?>[] params;
	
	protected Map<Object, Slot> listenerMap = new HashMap<Object, Slot>();
	
	protected static Logger logger;
	
	static
	{
		logger = Logger.getLogger(Signal.class.getPackage().getName());
		logger.setLevel(Level.ALL);
	}
	
	/**
	 * Constructor
	 * 
	 * @param params the parameter types (as a Class instance) that this signal will dispatch as event data
	 */
	public Signal (Class<?>... params)
	{
		this.params = params;
	}
	
	/**
	 * Adds a listener object to this signal.
	 * 
	 * All callback methods <b>must</b> be
	 * declared as <i>public</i> and <b>must</i> have the same <i>parameter signature</i> this signal
	 * was constructed with.
	 * 
	 * Listeners are put into a map and are keyed by their <code>hashCode()</code> value.  To ensure
	 * unique listenerMap are registered to this signal, ensure their <code>hashCode()</code> value is unique.
	 * 
	 * @param listener the listener object to add
	 * @param callback the callback method, as a String, to invoke when this signal is dispatched
	 * @param addOnce if true, once this signal has dispatched the listener is removed from the listener map
	 * @return the old listener keyed to the same <code>hashCode()</code> value, or null if no such listener was replaced
	 */
	@Override
	public Object add (Object listener, String callback, boolean addOnce)
	{
		Method delegate;
		
		try
		{
			delegate = listener.getClass().getMethod(callback, params);
		}
		catch (Exception e)
		{
			logger.throwing("Signal", "add", e);
			return null;
		}
		
		Slot previous = listenerMap.put(listener, new Slot(listener, delegate, addOnce));
		
		return previous == null ? null : previous.getListener();
	}
	
	/**
	 * Adds a listener object to this signal.
	 * 
	 * All callback methods <b>must</b> be
	 * declared as <i>public</i> and <b>must</i> have the same <i>parameter signature</i> this signal
	 * was constructed with.
	 * 
	 * Listeners are put into a map and are keyed by their <code>hashCode()</code> value.  To ensure
	 * unique listenerMap are registered to this signal, ensure their <code>hashCode()</code> value is unique.
	 * 
	 * This is an overloaded version of <code>Signal.add(Object, String, boolean)</code>.  By default, addOnce
	 * is <b>false</b>.
	 * 
	 * @param listener the listener object to add
	 * @param callback the callback method, as a String, to invoke when this signal is dispatched
	 * @return the old listener keyed to the same <code>hashCode()</code> value, or null if no such listener was replaced
	 */
	public Object add (Object listener, String callback)
	{
		return add(listener, callback, false);
	}
	
	/**
	 * Removes the listener from the listener map.  Listeners are found by the value of their <code>hashCode()</code> method.
	 * 
	 * @param listener the listener to remove
	 * @return if the listener was successfully removed
	 */
	@Override
	public boolean remove (Object listener)
	{
		return listenerMap.remove(listener) != null;
	}
	
	/* (non-Javadoc)
	 * @see com.paulm.jsignal.ISignalOwner#removeAll()
	 */
	public void removeAll ()
	{
		listenerMap.clear();
	}
	
	/**
	 * Dispatches this Signal to all listenerMap using the given arguments.
	 * 
	 * @param args the argument list to dispatch to listenerMap.  The argument
	 * list must have the same signature as the parameter list this Signal
	 * was constructed with
	 */
	@Override
	public void dispatch (Object... args)
	{
		Iterator<Slot> iterator = listenerMap.values().iterator();
		Slot slot;
		
		while (iterator.hasNext())
		{
			slot = iterator.next();
			
			try
			{
				slot.getDelegate().invoke(slot.getListener(), args);
			}
			catch (Exception e)
			{
				logger.throwing("Signal", "dispatch", e);
				return;
			}
			
			if (slot.getAddOnce())
			{
				iterator.remove();
			}
		}
	}
	
	/**
	 * Checks to see if a given listener has been registered to this signal.
	 * 
	 * @param listener the listener to check for
	 * @return if the listener found in the map given by the listener's <code>hashCode()</code> value
	 * is equal to the listener in question given by the result of the <code>equals(Object)</code> method
	 */
	@Override
	public boolean containsListener (Object listener)
	{
		Slot slot = listenerMap.get(listener);

		return slot == null ? false : slot.getListener().equals(listener);
	}
	
	/**
	 * Get the number of listenerMap currently registered to this Signal.
	 * 
	 * @return the number of listenerMap currently registered to this Signal
	 */
	@Override
	public int numListeners ()
	{
		return listenerMap.size();
	}
}