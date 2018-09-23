package de.v10lator.v10verlap;

import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public class V10verlapConfigHandler extends Thread {
	private boolean running = true;
	private V10verlap mod;
	private Configuration config;
	private AtomicBoolean lock = new AtomicBoolean(false);
	
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
		while(!lock.compareAndSet(false, true))
		{
			try
			{
				Thread.sleep(1L);
			}
			catch (InterruptedException e) {}
		}
		if(config.hasChanged())
			config.save();
	}
	
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
		config.load();
		double version = config.get(Configuration.CATEGORY_GENERAL, "version", 0.0D).getDouble();
		if(version < 1.0D) // Transform respectNetherScale to custom scale
		{
			if(config.hasKey(Configuration.CATEGORY_GENERAL, "respectNetherScale"))
			{
				Property prop = config.get(Configuration.CATEGORY_GENERAL, "respectNetherScale", false);
				mod.transformNetherScale = prop.getBoolean();
				config.getCategory(Configuration.CATEGORY_GENERAL).remove("respectNetherScale");
				config.get(Configuration.CATEGORY_GENERAL, "version", 0.0D).set(1.0D);
			}
			config.get(Configuration.CATEGORY_GENERAL, "version", 1.0D).set(1.0D);
		}
		mod.placeClimbBlock = config.get(Configuration.CATEGORY_GENERAL, "placeClimbBlock", 30).getInt() * 20;
		mod.noFallDamage = config.get(Configuration.CATEGORY_GENERAL, "noFallDamage", true).getBoolean();
		mod.relativeToSpawn = config.get(Configuration.CATEGORY_GENERAL, "relativeToSpawn", false).getBoolean();
		if(config.hasChanged())
			config.save();
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
