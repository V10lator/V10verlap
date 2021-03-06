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

import java.lang.reflect.Field;

import de.v10lator.v10verlap.api.Hooks;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.server.permission.PermissionAPI;

public class V10verlapCommand extends CommandBase {
	private final V10verlap mod;
	
	V10verlapCommand(V10verlap mod)
	{
		this.mod = mod;
	}
	
	@Override
	public String getName() {
		return "v10verlap";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/v10verlap <link|unlink>";
	}
	
	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
		return sender instanceof EntityPlayer ? PermissionAPI.hasPermission((EntityPlayer) sender, mod.permNode) : true;
	}
	
	private int parseDim(String dim) throws NumberFormatException
	{
		return Integer.parseInt(dim.toUpperCase().startsWith("DIM") ? dim.substring(3) : dim);
	}
	
	private void changeBoolConf(String key, ICommandSender sender, String[] args)
	{
		if(args.length != 2)
		{
			sender.sendMessage(makeMessage(TextFormatting.RED, "/v10verlap " + key + " <true|false>"));
			return;
		}
		boolean nv = Boolean.parseBoolean(args[1]);
		try {
			Field f = mod.getClass().getDeclaredField(key);
			if(f.getBoolean(mod) == nv)
			{
				sender.sendMessage(makeMessage(TextFormatting.RED, "No change!"));
				return;
			}
			f.setBoolean(mod, nv);
		} catch (Exception e) {} // Should never happen
		mod.configManager.getLockedConfig().get(Configuration.CATEGORY_GENERAL, key, false).set(nv);
		mod.configManager.releaseLock();
		sender.sendMessage(makeMessage(TextFormatting.GREEN, "Set " + key + " to " + nv + "!"));
	}
	
	private void link(ICommandSender sender, String[] args)
	{
		if(args.length != 5)
		{
			sender.sendMessage(makeMessage(TextFormatting.RED, "/v10verlap link <upperDimension> <lowerDimension> <maxHeight> <minHeight>"));
			return;
		}
		int dimA;
		try
		{
			dimA = parseDim(args[1]);
		}
		catch(NumberFormatException e)
		{
			sender.sendMessage(makeMessage(TextFormatting.RED, "Invalid dimension: " + args[1]));
			return;
		}
		if(mod.server.getWorld(dimA) == null)
		{
			sender.sendMessage(makeMessage(TextFormatting.RED, "Invalid dimension: " + args[1]));
			return;
		}
		int dimB;
		try
		{
			dimB = parseDim(args[2]);
		}
		catch(NumberFormatException e)
		{
			sender.sendMessage(makeMessage(TextFormatting.RED, "Invalid dimension: " + args[2]));
			return;
		}
		if(mod.server.getWorld(dimB) == null)
		{
			sender.sendMessage(makeMessage(TextFormatting.RED, "Invalid dimension: " + args[2]));
			return;
		}
		int maxY;
		try
		{
			maxY = Integer.parseInt(args[3]);
		}
		catch(NumberFormatException e)
		{
			sender.sendMessage(makeMessage(TextFormatting.RED, "Invalid max height: " + args[3]));
			return;
		}
		int minY;
		try
		{
			minY = Integer.parseInt(args[4]);
		}
		catch(NumberFormatException e)
		{
			sender.sendMessage(makeMessage(TextFormatting.RED, "Invalid min height: " + args[4]));
			return;
		}
		
		args[1] = Integer.toString(dimA);
		args[2] = Integer.toString(dimB);
		
		Configuration config = mod.configManager.getLockedConfig();
		config.get(args[2], "upper", args[1]).set(args[1]);
		config.get(args[2], "maxY", maxY).set(maxY);
		config.get(args[1], "lower", args[2]).set(args[2]);
		config.get(args[1], "minY", minY).set(minY);
		mod.configManager.releaseLock();
		mod.lowerCache.put(dimA, dimB);
		mod.upperCache.put(dimB, dimA);
		mod.minCache.put(dimA, minY);
		mod.maxCache.put(dimB, maxY);
		sender.sendMessage(makeMessage(TextFormatting.GREEN, "Dimensions linked!"));
	}
	
	private void unlink(ICommandSender sender, String[] args)
	{
		if(args.length != 3)
		{
			sender.sendMessage(makeMessage(TextFormatting.RED, "/v10verlap unlink <dimension 1> <dimension 2>"));
			return;
		}
		
		int dimA;
		try
		{
			dimA = parseDim(args[1]);
		}
		catch(NumberFormatException e)
		{
			sender.sendMessage(makeMessage(TextFormatting.RED, "Invalid dimension: " + args[1]));
			return;
		}
		int dimB;
		try
		{
			dimB = parseDim(args[2]);
		}
		catch(NumberFormatException e)
		{
			sender.sendMessage(makeMessage(TextFormatting.RED, "Invalid dimension: " + args[2]));
			return;
		}
		args[1] = Integer.toString(dimA);
		args[2] = Integer.toString(dimB);
		
		Configuration config = mod.configManager.getLockedConfig();
		Property propA = config.get(args[1], "upper", "none");
		boolean upperA = propA.getString().equals(args[2]);
		if(!upperA)
		{
			propA = config.get(args[1], "lower", "none");
			if(!propA.getString().equals(args[2]))
			{
				sender.sendMessage(makeMessage(TextFormatting.RED, "Invalid links"));
				return;
			}
		}
		Property propB = config.get(args[2], upperA ? "lower" : "upper", "none");
		if(!propB.getString().equals(args[1]))
		{
			sender.sendMessage(makeMessage(TextFormatting.RED, "Invalid links"));
			return;
		}
		
		propA.set("none");
		propB.set("none");
		
		mod.configManager.releaseLock();
		
		mod.scaleCache.remove(dimA);
		mod.scaleCache.remove(dimB);
		mod.lowerCache.remove(dimA);
		mod.lowerCache.remove(dimB);
		mod.upperCache.remove(dimA);
		mod.upperCache.remove(dimB);
		mod.minCache.remove(dimA);
		mod.minCache.remove(dimB);
		mod.maxCache.remove(dimA);
		mod.maxCache.remove(dimB);
		
		sender.sendMessage(makeMessage(TextFormatting.GREEN, "Dimensions unlinked!"));
	}
	
	private void setTmpBlockTime(ICommandSender sender, String[] args)
	{
		if(args.length != 2)
		{
			sender.sendMessage(makeMessage(TextFormatting.RED, "/v10verlap placeTmpBlocks <timeInSeconds>"));
			return;
		}
		int nv;
		try
		{
			nv = Integer.parseInt((args[1]));
		}
		catch(NumberFormatException e)
		{
			sender.sendMessage(makeMessage(TextFormatting.RED, "Invalid seconds: " + args[1]));
			return;
		}
		if(mod.placeTmpBlocks == nv)
		{
			sender.sendMessage(makeMessage(TextFormatting.RED, "No change!"));
			return;
		}
		mod.placeTmpBlocks = nv;
		mod.configManager.getLockedConfig().get(Configuration.CATEGORY_GENERAL, "placeTmpBlocks", 0).set(nv);
		mod.configManager.releaseLock();
		sender.sendMessage(makeMessage(TextFormatting.GREEN, nv == 0 ? "Disabled tmp block spawning!" : "Set tmp block live time to " + nv + " seconds!"));
	}
	
	private void setScale(ICommandSender sender, String[] args)
	{
		String scales;
		int dim;
		if(args.length != 3)
		{
			if(!(sender instanceof EntityPlayer) || args.length != 2)
			{
				sender.sendMessage(makeMessage(TextFormatting.RED, "/v10verlap scale <world> <scale>"));
				return;
			}
			scales = args[1];
			dim = ((EntityPlayer)sender).dimension;
		}
		else
		{
			scales = args[2];
			try
			{
				dim = parseDim(args[1]);
			}
			catch(NumberFormatException e)
			{
				sender.sendMessage(makeMessage(TextFormatting.RED, "Invalid dimension: " + args[2]));
				return;
			}
			if(mod.server.getWorld(dim) == null)
			{
				sender.sendMessage(makeMessage(TextFormatting.RED, "Invalid dimension: " + args[2]));
				return;
			}
		}
		int scale;
		try
		{
			scale = Integer.parseInt(scales);
		}
		catch(NumberFormatException e)
		{
			sender.sendMessage(makeMessage(TextFormatting.RED, "Invalid scale: " + scales));
			return;
		}
		if(scale == Hooks.getScale(dim))
		{
			sender.sendMessage(makeMessage(TextFormatting.RED, "No change!"));
			return;
		}
		mod.scaleCache.put(dim, scale);
		mod.configManager.getLockedConfig().get(Integer.toString(dim), "scale", 1).set(scale);
		mod.configManager.releaseLock();
		sender.sendMessage(makeMessage(TextFormatting.GREEN, "New scale for DIM" + Integer.toString(dim) + ": " + Double.toString(scale)));
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if(args.length < 1)
		{
			sender.sendMessage(makeMessage(TextFormatting.RED, getUsage(sender)));
			return;
		}
		args[0] = args[0].toLowerCase();
		switch(args[0])
		{
			case "link":
				link(sender, args);
				break;
			case "unlink":
				unlink(sender, args);
				break;
			case "placetmpblocks":
			case "ptb":
				setTmpBlockTime(sender, args);
				break;
			case "nofalldamage":
			case "nfd":
				changeBoolConf("noFallDamage", sender, args);
				break;
			case "relativetospawn":
			case "rts":
				changeBoolConf("relativeToSpawn", sender, args);
				break;
			case "scale":
				setScale(sender, args);
				break;
			case "playeronly":
			case "po":
				changeBoolConf("playerOnly", sender, args);
				break;
			default:
				sender.sendMessage(makeMessage(TextFormatting.RED, getUsage(sender)));
				break;
		}
	}
	
	private TextComponentString makeMessage(TextFormatting color, String message) {
		TextComponentString ret = new TextComponentString(message);
		ret.setStyle((new Style()).setColor(color));
		return ret;
	}
}
