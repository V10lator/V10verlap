package de.v10lator.v10verlap;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;

import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class V10verlapConfigHandler extends Thread {
	private boolean running = true;
	private final V10verlap mod;
	private final Configuration config;
	private final AtomicBoolean lock = new AtomicBoolean(false);
	private final String[] defaultWhitelist = new String[] {
			"minecraft:stone@0",
			"minecraft:bedrock",
			"minecraft:netherrack",
			"minecraft:water",
			"minecraft:lava",
			"minecraft:coal_ore",
			"minecraft:iron_ore",
			"minecraft:lapis_ore",
			"minecraft:gold_ore",
			"minecraft:diamond_ore",
			"minecraft:redstone_ore",
			"minecraft:emerald_ore",
			"minecraft:stone@3",
			"minecraft:gravel",
			"minecraft:stone@1",
			"minecraft:dirt@0",
			"minecraft:stone@5",
			"minecraft:cobblestone@0"
		};
	
	V10verlapConfigHandler(V10verlap mod, Configuration config)
	{
		this.mod = mod;
		this.config = config;
		reloadConfig();
	}
	
	void die()
	{
		running = false;
		this.interrupt();
		getLockedConfig();
		if(config.hasChanged())
			config.save();
	}
	
	@Override
	public void run()
	{
		while(running)
		{
			while(!lock.compareAndSet(false, true))
			{
				try
				{
					Thread.sleep(5L);
				}
				catch (InterruptedException e) {
					if(!running)
						return;
				}
			}
			if(config.hasChanged())
				config.save();
			releaseLock();
			try
			{
				Thread.sleep(300000L);
			}
			catch (InterruptedException e) {
			}
		}
	}
	
	private void reloadConfig()
	{
		getLockedConfig();
		config.load();
		double version = config.get(Configuration.CATEGORY_GENERAL, "version", 0.0D).getDouble();
		if(version < 2.D)
		{
			if(version < 1.0D) // Transform respectNetherScale to custom scale
			{
				if(config.hasKey(Configuration.CATEGORY_GENERAL, "respectNetherScale"))
				{
					Property prop = config.get(Configuration.CATEGORY_GENERAL, "respectNetherScale", false);
					mod.transformNetherScale = prop.getBoolean();
					config.getCategory(Configuration.CATEGORY_GENERAL).remove("respectNetherScale");
				}
			}
			if(config.hasKey(Configuration.CATEGORY_GENERAL, "placeClimbBlock")) // Transform placeClimbBlock to placeTmpBlocks
			{
				config.get(Configuration.CATEGORY_GENERAL, "placeTmpBlocks", 30).set(config.get(Configuration.CATEGORY_GENERAL, "placeClimbBlock", 30).getInt());
				config.getCategory(Configuration.CATEGORY_GENERAL).remove("placeClimbBlock");
			}
			config.get(Configuration.CATEGORY_GENERAL, "version", 0.0D).set(2.0D);
		}
		mod.placeTmpBlocks = config.get(Configuration.CATEGORY_GENERAL, "placeTmpBlocks", 30).getInt() * 20;
		mod.noFallDamage = config.get(Configuration.CATEGORY_GENERAL, "noFallDamage", true).getBoolean();
		mod.relativeToSpawn = config.get(Configuration.CATEGORY_GENERAL, "relativeToSpawn", false).getBoolean();
		mod.playerOnly = config.get(Configuration.CATEGORY_GENERAL, "playerOnly", false).getBoolean();
		mod.whitelist.clear();
		int seperator, meta, id;
		boolean[] mask;
		ResourceLocation key;
		for(String mat: config.get(Configuration.CATEGORY_GENERAL, "blockWhitelist", defaultWhitelist).getStringList())
		{
			seperator = mat.indexOf('@');
			if(seperator > 0)
			{
				try
				{
					meta = Integer.parseInt(mat.substring(seperator + 1));
				}
				catch(NumberFormatException e)
				{
					LogManager.getLogger("##NAME##").error("Invalid metadata in whitelist: " + mat.substring(seperator + 1));
					continue;
				}
				mat = mat.substring(0, seperator);
			}
			else
				meta = -1;
			
			key = new ResourceLocation(mat);
			if(!ForgeRegistries.BLOCKS.containsKey(key))
			{
				LogManager.getLogger("##NAME##").error("Invalid ID in whitelist: " + mat);
				continue;
			}
			
			id = Block.getIdFromBlock(ForgeRegistries.BLOCKS.getValue(key));
			
			if(meta == -1)
				mod.whitelist.put(id, null);
			else
			{
				mask = mod.whitelist.get(id);
				if(mask == null)
					mask = new boolean[16];
				mask[meta] = true;
				mod.whitelist.put(id, mask);
			}
		}
		if(config.hasChanged())
			config.save();
		releaseLock();
		mod.scaleCache.clear();
		mod.lowerCache.clear();
		mod.upperCache.clear();
		mod.minCache.clear();
		mod.maxCache.clear();
	}
	
	public Configuration getLockedConfig()
	{
		while(!lock.compareAndSet(false, true))
		{
			try {
				Thread.sleep(1);
			}
			catch (InterruptedException e) {}
		}
		return config;
	}
	
	public void releaseLock()
	{
		lock.set(false);
	}
}
