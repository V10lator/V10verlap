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

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;

@Mod(modid = "##MODID##", name = "##NAME##", version = "##VERSION##", acceptedMinecraftVersions = "1.12.2", serverSideOnly = true, acceptableRemoteVersions = "*", updateJSON="http://forge.home.v10lator.de/update.json?id=##MODID##&v=##VERSION##")
public class V10verlap {
	private final HashMap<V10verlapBlock, Integer> blocks = new HashMap<V10verlapBlock, Integer>();
	Configuration config;
	private final String ENTITY_FALL_TAG = "##MODID##.noFallDamage";
	private boolean noFallDamage, relativeToSpawn;
	boolean respectNetherScale;
	private int placeClimbBlock;
	final String permNode = "##MODID##.command";
	
	@Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
		config = new Configuration(new File(event.getModConfigurationDirectory(), "##NAME##.cfg"));
		reloadConfig();
		MinecraftForge.EVENT_BUS.register(this);
		V10verlap_API.init(this);
	}
	
	@Mod.EventHandler
	public void onServerStart(FMLServerStartingEvent event) {
		PermissionAPI.registerNode(permNode, DefaultPermissionLevel.OP, "Use the /dimmode command");
		event.registerServerCommand(new V10verlapCommand(this));
	}
	
	@Mod.EventHandler
	public void onServerStop(FMLServerStoppingEvent event) {
		MinecraftServer ms = FMLCommonHandler.instance().getMinecraftServerInstance();
		for(V10verlapBlock block: blocks.keySet())
			resetBlock(ms, block);
		blocks.clear();
	}
	
	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load event)
	{
		World dimension = event.getWorld();
		int id = dimension.provider.getDimension();
		String world = Integer.toString(id);
		config.get(world, "upper", id == 0 ? "1" : id == -1 ? "0" : "none");
		config.get(world, "lower", id == 0 ? "-1" : id == 1 ? "0" : "none");
		config.get(world, "minY", 0);
		config.get(world, "maxY", dimension.getHeight());
		if(config.hasChanged())
			config.save();
	}
	
	@SubscribeEvent
	public void onBlockChange(BlockEvent.BreakEvent event)
	{
		World ws = event.getWorld();
		V10verlapBlock block = new V10verlapBlock(ws.provider.getDimension(), event.getPos());
		if(blocks.containsKey(block))
			blocks.remove(block);
	}
	
	@SubscribeEvent
	public void onEntityHurt(LivingHurtEvent event) {
		if(!noFallDamage || event.isCanceled() || event.getSource() != DamageSource.FALL)
			return;
		Entity entity = event.getEntity();
		NBTTagCompound data = entity.getEntityData();
		if(!data.hasKey(ENTITY_FALL_TAG))
			return;
		event.setCanceled(true);
		data.removeTag(ENTITY_FALL_TAG);
	}
	
	void reloadConfig()
	{
		config.load();
		placeClimbBlock = config.get(Configuration.CATEGORY_GENERAL, "placeClimbBlock", 0).getInt() * 20;
		noFallDamage = config.get(Configuration.CATEGORY_GENERAL, "noFallDamage", false).getBoolean();
		relativeToSpawn = config.get(Configuration.CATEGORY_GENERAL, "relativeToSpawn", false).getBoolean();
		respectNetherScale = config.get(Configuration.CATEGORY_GENERAL, "respectNetherScale", false).getBoolean();
		if(config.hasChanged())
			config.save();
	}
	
	@SubscribeEvent
	public void onTick(ServerTickEvent event) {
		MinecraftServer ms = FMLCommonHandler.instance().getMinecraftServerInstance();
		if(!blocks.isEmpty())
		{
			Entry<V10verlapBlock, Integer> entry;
			int c;
			for(Iterator<Entry<V10verlapBlock, Integer>> iter = blocks.entrySet().iterator(); iter.hasNext();)
			{
				entry = iter.next();
				c = entry.getValue() - 1;
				if(c < 0)
				{
					resetBlock(ms, entry.getKey());
					iter.remove();
				}
				else
					entry.setValue(c);
			}
		}
		
		int worldId, lower = 0, upper = 0, to, minY = 0, maxY = 0;
		BlockPos pos, oldWorldSpawnPos, newWorldSpawnPos = null;
		double x, y, z, oldScale, newScale;
		Entity[] entities;
		boolean down, lowerAvail, upperAvail;
		NBTTagCompound data;
		for(WorldServer dimension: DimensionManager.getWorlds())
		{
			worldId = dimension.provider.getDimension();
			try
			{
				lower = V10verlap_API.getLowerWorld(worldId);
				minY = V10verlap_API.getMinY(worldId);
				lowerAvail = true;
			}
			catch(V10verlap_API.NotLinkedException e)
			{
				lowerAvail = false;
			}
			try
			{
				upper = V10verlap_API.getUpperWorld(worldId);
				maxY = V10verlap_API.getMaxY(worldId);
				upperAvail = true;
			}
			catch(V10verlap_API.NotLinkedException e)
			{
				upperAvail = false;
			}
			
			entities = new Entity[dimension.loadedEntityList.size()];
			for(int i = 0; i < entities.length; i++)
				entities[i] = dimension.loadedEntityList.get(i);
			
			oldWorldSpawnPos = dimension.getSpawnPoint();
			oldScale = V10verlap_API.getScale(worldId);
			for(Entity entity: entities)
			{
				data = entity.getEntityData();
				if(noFallDamage && data.hasKey(ENTITY_FALL_TAG))
				{
					if(entity.onGround)
						data.setBoolean(ENTITY_FALL_TAG, true);
					else if(data.getBoolean(ENTITY_FALL_TAG))
						data.removeTag(ENTITY_FALL_TAG);
				}
				
				if((!lowerAvail && !upperAvail) || entity.isDead || entity.isRiding())
					continue;
				
				x = entity.posX;
				y = entity.posY;
				z = entity.posZ;
				pos = new BlockPos(x, y, z).down();
				if(lowerAvail && y <= minY)
				{
					try
					{
						y = V10verlap_API.getMaxY(lower) - 1;
					}
					catch(V10verlap_API.NotLinkedException e)
					{
						LogManager.getLogger("##NAME##").error("Invalid link between DIM" + worldId + " and DIM" + lower + ". Canceling teleport!");
						lowerAvail = false;
						continue;
					}
					to = lower;
					down = true;
				}
				else if(upperAvail && y >= maxY)
				{
					try
					{
						y = V10verlap_API.getMinY(upper) + 1;
					}
					catch(V10verlap_API.NotLinkedException e)
					{
						LogManager.getLogger("##NAME##").error("Invalid link between DIM" + worldId + " and DIM" + upper + ". Canceling teleport!");
						upperAvail = false;
						continue;
					}
					to = upper;
					down = false;
				}
				else
					continue;
				
				if (!ForgeHooks.onTravelToDimension(entity, to))
				{
					LogManager.getLogger("##NAME##").info("Another plugin blocked the teleport from DIM" + worldId + " to DIM" + to);
					continue;
				}
				
				WorldServer ws = ms.getWorld(to);
				if(ws == null)
				{
					LogManager.getLogger("##NAME##").info("Can't load DIM" + to);
					if(down)
						lowerAvail = false;
					else
						upperAvail = false;
					continue;
				}
				
				if(relativeToSpawn)
				{
					newWorldSpawnPos = ws.getSpawnPoint();
					x -= oldWorldSpawnPos.getX();
					z -= oldWorldSpawnPos.getZ();
				}
				
				newScale = V10verlap_API.getScale(to);
				if(oldScale != newScale)
				{
					if(oldScale != 1.0D)
					{
						x *= oldScale;
						z *= oldScale;
					}
					if(newScale != 1.0D)
					{
						x /= oldScale;
						z /= newScale;
					}
				}
				
				if(relativeToSpawn)
				{
					x += newWorldSpawnPos.getX();
					z += newWorldSpawnPos.getZ();
				}
				
				if(down && noFallDamage)
					entity.getEntityData().setBoolean(ENTITY_FALL_TAG, false);
				
				if(!down && placeClimbBlock > 0 && entity instanceof EntityPlayerMP)
				{
					pos = new BlockPos(x, y, z).down();
					if(ws.isAirBlock(pos))
					{
						ws.setBlockState(pos, Blocks.GLASS.getDefaultState());
						blocks.put(new V10verlapBlock(to, pos), placeClimbBlock);
					}
				}
				
				this.teleport(entity, ms, dimension, ws, x, y, z);
			}
		}
	}
	
	private void resetBlock(MinecraftServer ms, V10verlapBlock block)
	{
		WorldServer ws = ms.getWorld(block.dim);
		if(ws.getBlockState(block.pos).getMaterial() == Material.GLASS)
			ws.setBlockState(block.pos, Blocks.AIR.getDefaultState());
	}
	
	private void teleport(Entity entity, MinecraftServer ms, WorldServer from, WorldServer to, double x, double y, double z)
	{
		List<Entity> passengers = entity.getPassengers();
		if(!passengers.isEmpty())
		{
			for(Entity passenger: passengers)
			{
				passenger.dismountRidingEntity();
				teleport(passenger, ms, from, to, x, y, z);
			}
		}
		
		if(entity instanceof EntityPlayerMP)
			ms.getPlayerList().transferPlayerToDimension((EntityPlayerMP)entity, to.provider.getDimension(), new V10verlapTeleporter(to, x, y, z));
		else
		{
			from.getEntityTracker().untrack(entity);
			from.removeEntityDangerously(entity);
		    
		    entity.isDead = false;
		    entity.world = to;
		    entity.dimension = to.provider.getDimension();
		    entity.setPosition(x, y, z);
		    
		    to.getChunkFromChunkCoords(((int)Math.floor(x)) >> 4, ((int)Math.floor(z)) >> 4).addEntity(entity);
            to.loadedEntityList.add(entity);
            to.onEntityAdded(entity);
		}
		
		if(!passengers.isEmpty())
			for(Entity passenger: passengers)
				passenger.startRiding(entity, true);
	}
}
