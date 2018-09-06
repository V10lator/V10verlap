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

	static public boolean isReady()
	{
		return V10verlap_API.plugin != null;
	}

	/** Returns the APIs version as a double.
	 *  The first number will change whenever there's an API breakage while
	 *  the second will change whenever there are new things.
	 * 
	 * @return double
	 */
	static public double getVersion()
	{
		return V10verlap_API.version;
	}

	/** Returns the minimum Y
	 * 
	 * @param world - The world
	 * @return int
	 */
	static public int getMinY(int world) throws NotLinkedException
	{
		V10verlap_API.getLowerWorld(world);
		return V10verlap_API.plugin.config.get(Integer.toString(world), "minY", 0).getInt();
	}

	/** Returns the maximum Y
	 * 
	 * @param world - The world
	 * @return int
	 */
	static public int getMaxY(int world) throws NotLinkedException
	{
		V10verlap_API.getUpperWorld(world);
		return V10verlap_API.plugin.config.get(Integer.toString(world), "maxY", 128).getInt();
	}

	/** Returns the upper world
	 * 
	 * @param world - The world
	 * @return World - The upper world
	 */
	static public int getUpperWorld(int world) throws NotLinkedException
	{
		String name = V10verlap_API.plugin.config.get(Integer.toString(world), "upper", "none").getString();
		if(name.equals("none"))
			throw self.new NotLinkedException();
		try {
			return Integer.parseInt(name);
		}
		catch(NumberFormatException e)
		{
			throw self.new NotLinkedException(e);
		}
	}

	/** Returns the lower world
	 * 
	 * @param world - The world
	 * @return World - The upper world
	 */
	static public int getLowerWorld(int world) throws NotLinkedException
	{
		String name = V10verlap_API.plugin.config.get(Integer.toString(world), "lower", "none").getString();
		if(name.equals("none"))
			throw self.new NotLinkedException();
		try {
			return Integer.parseInt(name);
		}
		catch(NumberFormatException e)
		{
			throw self.new NotLinkedException(e);
		}
	}

	public class NotLinkedException extends RuntimeException
	{
		public static final long serialVersionUID = 8175875770576887164L;

		public NotLinkedException() {
			super();
		}

		public NotLinkedException(Exception e) {
			super(e);
		}
	}
}
