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

package de.v10lator.v10verlap.api;

import de.v10lator.v10verlap.V10verlap;
import net.minecraftforge.common.config.Configuration;

public class Hooks
{
	private static V10verlap plugin = null;
	private static final double version = 2.0D;
	
	private static final NotLinkedException nle = new NotLinkedException();

	/** This is a internal function - DO NOT USE!
	 * 
	 * @param plugin
	 */
	public static void init(V10verlap plugin)
	{
		Hooks.plugin = plugin;
	}

	/** Returns if the API is ready to use.
	 *  Call this before any other call to the API and don't do
	 *  anything if it returns false!
	 * 
	 * @return boolean
	 */
	public static boolean isReady()
	{
		return Hooks.plugin != null;
	}

	/** Returns the APIs version as a double.
	 *  The first number will change whenever there's an API breakage while
	 *  the second will change whenever there are new things.
	 * 
	 * @return double
	 */
	public static double getVersion()
	{
		return Hooks.version;
	}

	/** Returns the minimum Y
	 * 
	 * @param world - The world
	 * @return int
	 */
	public static int getMinY(int world) throws NotLinkedException, ConfigurationErrorException
	{
		if(plugin.minCache.containsKey(world))
			return plugin.minCache.get(world);
		
		Hooks.getLowerWorld(world);
		int ret = plugin.configManager.getLockedConfig().get(Integer.toString(world), "minY", 0).getInt();
		plugin.configManager.releaseLock();
		plugin.minCache.put(world, ret);
		return ret;
	}

	/** Returns the maximum Y
	 * 
	 * @param world - The world
	 * @return int
	 */
	public static int getMaxY(int world) throws NotLinkedException, ConfigurationErrorException
	{
		if(plugin.maxCache.containsKey(world))
			return plugin.maxCache.get(world);
		
		Hooks.getUpperWorld(world);
		int ret = plugin.configManager.getLockedConfig().get(Integer.toString(world), "maxY", 128).getInt();
		plugin.configManager.releaseLock();
		plugin.maxCache.put(world, ret);
		return ret;
	}
	
	/** Returns the upper world
	 * 
	 * @param world - The world id
	 * @return World - The upper world id
	 */
	public static int getUpperWorld(int world) throws NotLinkedException, ConfigurationErrorException
	{
		if(plugin.upperCache.containsKey(world))
		{
			Integer ret = plugin.upperCache.get(world);
			if(ret == null)
				throw Hooks.nle;
			return ret;
		}
		
		String worldName = Integer.toString(world);
		Configuration config = plugin.configManager.getLockedConfig();
		if(!config.hasCategory(worldName))
		{
			plugin.configManager.releaseLock();
			plugin.upperCache.put(world, null);
			throw Hooks.nle;
		}
		String name = config.get(worldName, "upper", "none").getString();
		plugin.configManager.releaseLock();
		if(name.equals("none"))
		{
			plugin.upperCache.put(world, null);
			throw Hooks.nle;
		}
		try
		{
			int ret = Integer.parseInt(name);
			plugin.upperCache.put(world, ret);
			return ret;
		}
		catch(NumberFormatException e)
		{
			throw new ConfigurationErrorException(e);
		}
	}
	
	/** Returns the upper world
	 * 
	 * @deprecated
	 * @param world - The world name
	 * @return World - The upper world id
	 */
	public static int getUpperWorld(String worldName) throws NotLinkedException, ConfigurationErrorException
	{
		try
		{
			return getUpperWorld(Integer.parseInt(worldName));
		}
		catch(NumberFormatException e)
		{
			throw Hooks.nle;
		}
	}

	/** Returns the lower world
	 * 
	 * @param world - The world id
	 * @return World - The lower world id
	 */
	public static int getLowerWorld(int world) throws NotLinkedException, ConfigurationErrorException
	{
		if(plugin.lowerCache.containsKey(world))
		{
			Integer ret = plugin.lowerCache.get(world);
			if(ret == null)
				throw Hooks.nle;
			return ret;
		}
		
		String worldName = Integer.toString(world);
		Configuration config = plugin.configManager.getLockedConfig();
		if(!config.hasCategory(worldName))
		{
			plugin.configManager.releaseLock();
			plugin.lowerCache.put(world, null);
			throw Hooks.nle;
		}
		String name = config.get(worldName, "lower", "none").getString();
		plugin.configManager.releaseLock();
		if(name.equals("none"))
		{
			plugin.lowerCache.put(world, null);
			throw Hooks.nle;
		}
		try
		{
			int ret = Integer.parseInt(name);
			plugin.lowerCache.put(world, ret);
			return ret;
		}
		catch(NumberFormatException e)
		{
			throw new ConfigurationErrorException(e);
		}
	}
	
	/** Returns the lower world
	 * 
	 * @deprecated
	 * @param world - The world name
	 * @return World - The lower world id
	 */
	public static int getLowerWorld(String worldName) throws NotLinkedException, ConfigurationErrorException
	{
		try
		{
			return getLowerWorld(Integer.parseInt(worldName));
		}
		catch(NumberFormatException e)
		{
			throw Hooks.nle;
		}
	}
	
	/** Returns the world scale. This is per default 1.0D, 8.0D if respectNetherScale is activated and the world is a END world or some custom value setted in the config
	 * 
	 * @param world - The world id
	 * @return World - The scale
	 */
	public static int getScale(int world) throws NotLinkedException
	{
		if(plugin.scaleCache.containsKey(world))
			return plugin.scaleCache.get(world);
		
		String worldName = Integer.toString(world);
		Configuration config = plugin.configManager.getLockedConfig();
		if(!config.hasCategory(worldName) || !config.hasKey(worldName, "scale"))
		{
			plugin.configManager.releaseLock();
			throw Hooks.nle;
		}
		int ret = config.get(worldName, "scale", 1).getInt();
		plugin.configManager.releaseLock();
		plugin.scaleCache.put(world, ret);
		return ret;
	}
}
