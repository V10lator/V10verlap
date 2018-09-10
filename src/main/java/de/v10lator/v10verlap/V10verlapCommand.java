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
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
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
	
	private void changeBoolConf(String key, ICommandSender sender, String[] args)
	{
		if(args.length != 2)
		{
			sender.sendMessage(makeMessage(TextFormatting.RED, "/v10verlap " + key + " <true|false>"));
			return;
		}
		boolean nv = Boolean.parseBoolean(args[1]);
		Property prop = mod.configManager.getLockedConfig().get(Configuration.CATEGORY_GENERAL, key, false);
		boolean value = prop.getBoolean();
		if(value == nv)
		{
			sender.sendMessage(makeMessage(TextFormatting.RED, "No change!"));
			return;
		}
		prop.set(nv);
		mod.configManager.releaseLock();
		sender.sendMessage(makeMessage(TextFormatting.GREEN, "Set " + key + " to " + nv + "!"));
	}
	
	private void link(MinecraftServer server, ICommandSender sender, String[] args)
	{
		if(args.length != 5)
		{
			sender.sendMessage(makeMessage(TextFormatting.RED, "/v10verlap link <upperDimension> <lowerDimension> <maxHeight> <minHeight>"));
			return;
		}
		args[1] = args[1].toUpperCase();
		int dimA;
		try
		{
			dimA = Integer.parseInt(args[1].startsWith("DIM") ? args[1].substring(3) : args[1]);
		}
		catch(NumberFormatException e)
		{
			sender.sendMessage(makeMessage(TextFormatting.RED, "Invalid dimension: " + args[1]));
			return;
		}
		if(server.getWorld(dimA) == null)
		{
			sender.sendMessage(makeMessage(TextFormatting.RED, "Invalid dimension: " + args[1]));
			return;
		}
		args[2] = args[2].toUpperCase();
		int dimB;
		try
		{
			dimB = Integer.parseInt(args[2].startsWith("DIM") ? args[2].substring(2) : args[2]);
		}
		catch(NumberFormatException e)
		{
			sender.sendMessage(makeMessage(TextFormatting.RED, "Invalid dimension: " + args[2]));
			return;
		}
		if(server.getWorld(dimB) == null)
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
		sender.sendMessage(makeMessage(TextFormatting.GREEN, "Dimensions linked!"));
	}
	
	private void unlink(MinecraftServer server, ICommandSender sender, String[] args)
	{
		if(args.length != 3)
		{
			sender.sendMessage(makeMessage(TextFormatting.RED, "/v10verlap unlink <dimension 1> <dimension 2>"));
			return;
		}
		
		args[1] = args[1].toUpperCase();
		int dimA;
		try
		{
			dimA = Integer.parseInt(args[1].startsWith("DIM") ? args[1].substring(3) : args[1]);
		}
		catch(NumberFormatException e)
		{
			sender.sendMessage(makeMessage(TextFormatting.RED, "Invalid dimension: " + args[1]));
			return;
		}
		args[2] = args[2].toUpperCase();
		int dimB;
		try
		{
			dimB = Integer.parseInt(args[2].startsWith("DIM") ? args[2].substring(2) : args[2]);
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
		sender.sendMessage(makeMessage(TextFormatting.GREEN, "Dimensions unlinked!"));
	}
	
	private void setTmpBlockTime(ICommandSender sender, String[] args)
	{
		if(args.length != 2)
		{
			sender.sendMessage(makeMessage(TextFormatting.RED, "/v10verlap placeClimbBlocks <timeInSeconds>"));
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
		Property prop = mod.configManager.getLockedConfig().get(Configuration.CATEGORY_GENERAL, "placeClimbBlock", 0);
		int value = prop.getInt();
		if(value == nv)
		{
			sender.sendMessage(makeMessage(TextFormatting.RED, "No change!"));
			return;
		}
		prop.set(nv);
		mod.configManager.releaseLock();
		sender.sendMessage(makeMessage(TextFormatting.GREEN, nv == 0 ? "Disabled tmp block spawning!" : "Set tmp block live time to " + nv + " seconds!"));
	}
	
	private void setScale(MinecraftServer server, ICommandSender sender, String[] args)
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
			scales = args[2];
			dim = ((EntityPlayer)sender).dimension;
		}
		else
		{
			scales = args[3];
			try
			{
				dim = Integer.parseInt(args[2]);
			}
			catch(NumberFormatException e)
			{
				sender.sendMessage(makeMessage(TextFormatting.RED, "Invalid dimension: " + args[2]));
				return;
			}
			if(server.getWorld(dim) == null)
			{
				sender.sendMessage(makeMessage(TextFormatting.RED, "Invalid dimension: " + args[2]));
				return;
			}
		}
		double scale;
		try
		{
			scale = Double.parseDouble(scales);
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
		mod.configManager.getLockedConfig().get(Integer.toString(dim), "scale", 1.0D).set(scale);
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
				link(server, sender, args);
				break;
			case "unlink":
				unlink(server, sender, args);
				break;
			case "placeclimbblock":
			case "pcb":
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
				setScale(server, sender, args);
				break;
			default:
				sender.sendMessage(makeMessage(TextFormatting.RED, getUsage(sender)));
				break;
		}
	}
	
	private TextComponentString makeMessage(TextFormatting color, String message) {
		color.getColorIndex();
		TextComponentString ret = new TextComponentString(String.format("\u00A7%x", color.getColorIndex()));
		ret.appendText(message);
		return ret;
	}
}
