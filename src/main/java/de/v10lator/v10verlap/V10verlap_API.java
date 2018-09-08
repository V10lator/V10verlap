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

import net.minecraft.world.DimensionType;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class V10verlap_API
{
	private static V10verlap plugin = null;
	private static V10verlap_API self;
	private static final double version = 1.4D;

	static void init(V10verlap plugin)
	{
		self = new V10verlap_API();
		V10verlap_API.plugin = plugin;
	}

	/** Returns if the API is ready to use.
	 *  Call this before any other call to the API and don't do
	 *  anything if it returns false!
	 * 
	 * @return boolean
	 */
	public static boolean isReady()
	{
		return V10verlap_API.plugin != null;
	}

	/** Returns the APIs version as a double.
	 *  The first number will change whenever there's an API breakage while
	 *  the second will change whenever there are new things.
	 * 
	 * @return double
	 */
	public static double getVersion()
	{
		return V10verlap_API.version;
	}

	/** Returns the minimum Y
	 * 
	 * @param world - The world
	 * @return int
	 */
	public static int getMinY(int world) throws NotLinkedException
	{
		V10verlap_API.getLowerWorld(world);
		return V10verlap_API.plugin.config.get(Integer.toString(world), "minY", 0).getInt();
	}

	/** Returns the maximum Y
	 * 
	 * @param world - The world
	 * @return int
	 */
	public static int getMaxY(int world) throws NotLinkedException
	{
		V10verlap_API.getUpperWorld(world);
		return V10verlap_API.plugin.config.get(Integer.toString(world), "maxY", 128).getInt();
	}
	
	/** Returns the upper world
	 * 
	 * @param world - The world id
	 * @return World - The upper world id
	 */
	public static int getUpperWorld(int world) throws NotLinkedException
	{
		return V10verlap_API.getUpperWorld(Integer.toString(world));
	}
	
	/** Returns the upper world
	 * 
	 * @param world - The world name
	 * @return World - The upper world id
	 */
	public static int getUpperWorld(String worldName) throws NotLinkedException
	{
		if(!V10verlap_API.plugin.config.hasCategory(worldName))
			throw self.new NotLinkedException();
		String name = V10verlap_API.plugin.config.get(worldName, "upper", "none").getString();
		if(name.equals("none"))
			throw self.new NotLinkedException();
		try
		{
			return Integer.parseInt(name);
		}
		catch(NumberFormatException e)
		{
			throw self.new NotLinkedException(e);
		}
	}

	/** Returns the lower world
	 * 
	 * @param world - The world id
	 * @return World - The lower world id
	 */
	public static int getLowerWorld(int world) throws NotLinkedException
	{
		return V10verlap_API.getLowerWorld(Integer.toString(world));
	}
	
	/** Returns the lower world
	 * 
	 * @param world - The world name
	 * @return World - The lower world id
	 */
	public static int getLowerWorld(String worldName) throws NotLinkedException
	{
		if(!V10verlap_API.plugin.config.hasCategory(worldName))
			throw self.new NotLinkedException();
		String name = V10verlap_API.plugin.config.get(worldName, "lower", "none").getString();
		if(name.equals("none"))
			throw self.new NotLinkedException();
		try
		{
			return Integer.parseInt(name);
		}
		catch(NumberFormatException e)
		{
			throw self.new NotLinkedException(e);
		}
	}
	
	/** Returns the world scale. This is per default 1.0D, 8.0D if respectNetherScale is activated and the world is a END world or some custom value setted in the config
	 * 
	 * @param world - The world id
	 * @return World - The scale
	 */
	public static double getScale(int world) throws NotLinkedException
	{
		String worldName = Integer.toString(world);
		if(!V10verlap_API.plugin.config.hasCategory(worldName))
			throw self.new NotLinkedException();
		
		return V10verlap_API.plugin.config.hasKey(worldName, "scale") ? V10verlap_API.plugin.config.get(worldName, "scale", 1.0D).getDouble() : 
			plugin.respectNetherScale && FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(world).provider.getDimensionType() == DimensionType.THE_END ? 8.0D : 1.0D;
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
