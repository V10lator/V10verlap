/*
 * This file is part of V10verlap.
 *
 * V10verlap is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * V10verlap is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General
 * Public License along with V10verlap.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package de.v10lator.v10verlap;

import de.v10lator.v10verlap.api.Hooks;

/**
 * @author V10lator
 * @deprecated Use de.v10lator.v10verlap.api.Hooks
 */
@Deprecated
public class V10verlap_API
{
	private static final V10verlap_API self = new V10verlap_API();
	
	/** Returns if the API is ready to use.
	 *  Call this before any other call to the API and don't do
	 *  anything if it returns false!
	 * 
	 * @return boolean
	 * @deprecated Use de.v10lator.v10verlap.api.Hooks.isReady()
	 */
	@Deprecated
	public static boolean isReady()
	{
		return Hooks.isReady();
	}

	/** Returns the APIs version as a double.
	 *  The first number will change whenever there's an API breakage while
	 *  the second will change whenever there are new things.
	 * 
	 * @return double
	 * @deprecated Use de.v10lator.v10verlap.api.Hooks.getVersion()
	 */
	@Deprecated
	public static double getVersion()
	{
		return Hooks.getVersion();
	}

	/** Returns the minimum Y
	 * 
	 * @param world - The world
	 * @return int
	 * @deprecated Use de.v10lator.v10verlap.api.Hooks.getMinY(int)
	 */
	@Deprecated
	public static int getMinY(int world) throws NotLinkedException
	{
		try
		{
			return Hooks.getMinY(world);
		}
		catch(de.v10lator.v10verlap.api.V10verlapException e)
		{
			throw self.new NotLinkedException(e);
		}
	}

	/** Returns the maximum Y
	 * 
	 * @param world - The world
	 * @return int
	 * @deprecated Use de.v10lator.v10verlap.api.Hooks.getMaxY(int)
	 */
	@Deprecated
	public static int getMaxY(int world) throws NotLinkedException
	{
		try
		{
			return Hooks.getMaxY(world);
		}
		catch(de.v10lator.v10verlap.api.V10verlapException e)
		{
			throw self.new NotLinkedException(e);
		}
	}
	
	/** Returns the upper world
	 * 
	 * @param world - The world id
	 * @return World - The upper world id
	 * @deprecated Use de.v10lator.v10verlap.api.Hooks.getUpperWorld(int)
	 */
	@Deprecated
	public static int getUpperWorld(int world) throws NotLinkedException
	{
		try
		{
			return Hooks.getUpperWorld(world);
		}
		catch(de.v10lator.v10verlap.api.V10verlapException e)
		{
			throw self.new NotLinkedException(e);
		}
	}
	
	/** Returns the upper world
	 * 
	 * @param world - The world name
	 * @return World - The upper world id
	 * @deprecated Use de.v10lator.v10verlap.api.Hooks.getUpperWorld(String)
	 */
	@Deprecated
	public static int getUpperWorld(String worldName) throws NotLinkedException
	{
		try
		{
			return Hooks.getUpperWorld(worldName);
		}
		catch(de.v10lator.v10verlap.api.V10verlapException e)
		{
			throw self.new NotLinkedException(e);
		}
	}

	/** Returns the lower world
	 * 
	 * @param world - The world id
	 * @return World - The lower world id
	 * @deprecated Use de.v10lator.v10verlap.api.Hooks.getLowerWorld(int)
	 */
	@Deprecated
	public static int getLowerWorld(int world) throws NotLinkedException
	{
		try
		{
			return Hooks.getLowerWorld(world);
		}
		catch(de.v10lator.v10verlap.api.V10verlapException e)
		{
			throw self.new NotLinkedException(e);
		}
	}
	
	/** Returns the lower world
	 * 
	 * @param world - The world name
	 * @return World - The lower world id
	 * @deprecated Use de.v10lator.v10verlap.api.Hooks.getLowerWorld(String)
	 */
	@Deprecated
	public static int getLowerWorld(String worldName) throws NotLinkedException
	{
		try
		{
			return Hooks.getLowerWorld(worldName);
		}
		catch(de.v10lator.v10verlap.api.V10verlapException e)
		{
			throw self.new NotLinkedException(e);
		}
	}
	
	/** Returns the world scale. This is per default 1.0D, 8.0D if respectNetherScale is activated and the world is a END world or some custom value setted in the config
	 * 
	 * @param world - The world id
	 * @return World - The scale
	 * @derecated Use de.v10lator.v10verlap.api.Hooks.getScale(int)
	 */
	@Deprecated
	public static double getScale(int world) throws NotLinkedException
	{
		try
		{
			return Hooks.getScale(world);
		}
		catch(de.v10lator.v10verlap.api.V10verlapException e)
		{
			throw self.new NotLinkedException(e);
		}
	}
	
	/**
	 * The class {@code NotLinkedException} are a form of
	 * {@code Exception} which gets thrown in case an API
	 * function is called with for worlds which aren't connected.
	 *
	 * @author  Thomas "V10lator" Rohloff
	 * @since   1.4
	 */
	public class NotLinkedException extends RuntimeException
	{
		static final long serialVersionUID = 8175875770576887164L;

		NotLinkedException() {
			super();
		}

		NotLinkedException(Exception e) {
			super(e);
		}
	}
}
