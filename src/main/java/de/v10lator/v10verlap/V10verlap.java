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
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;

@Mod(modid = "##MODID##", name = "##NAME##", version = "##VERSION##", acceptedMinecraftVersions = "1.12.2", serverSideOnly = true, acceptableRemoteVersions = "*", updateJSON="http://forge.home.v10lator.de/update.json?id=##MODID##&v=##VERSION##")
public class V10verlap {
	private final HashMap<V10verlapBlock, Integer> blocks = new HashMap<V10verlapBlock, Integer>();
	Configuration config;
	private final String ENTITY_FALL_TAG = "##MODID##.noFallDamage";
	private boolean noFallDamage, relativeToSpawn, respectNetherScale;
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
	public void start(FMLServerStartingEvent event) {
		PermissionAPI.registerNode(permNode, DefaultPermissionLevel.OP, "Use the /dimmode command");
		event.registerServerCommand(new V10verlapCommand(this));
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
	
	private void reloadConfig()
	{
		config.load();
		placeClimbBlock = config.getInt("placeClimbBlock", Configuration.CATEGORY_GENERAL, 0, 0, 6000, "Place a temporary block when a player climbs up a world");
		noFallDamage = config.getBoolean("noFallDamage", Configuration.CATEGORY_GENERAL, false, "Don't apply fall damage when falling from one dimension to another");
		relativeToSpawn = config.getBoolean("relativeToSpawn", Configuration.CATEGORY_GENERAL, false, "Overlap worlds relative to spawn points");
		respectNetherScale = config.getBoolean("respectNetherScale", Configuration.CATEGORY_GENERAL, false, "Respect the Nethers 8x scale");
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
			V10verlapBlock block;
			WorldServer ws;
			for(Iterator<Entry<V10verlapBlock, Integer>> iter = blocks.entrySet().iterator(); iter.hasNext();)
			{
				entry = iter.next();
				c = entry.getValue() - 1;
				if(c < 0)
				{
					block = entry.getKey();
					ws = ms.getWorld(block.dim);
					if(ws.getBlockState(block.pos).getMaterial() == Material.GLASS)
						ws.setBlockState(block.pos, Blocks.AIR.getDefaultState());
					iter.remove();
				}
				else
					entry.setValue(c);
			}
		}
		
		int worldId, lower = 0, upper = 0, to, minY = 0, maxY = 0;
		BlockPos pos, oldWorldSpawnPos, newWorldSpawnPos;
		double x, y, z;
		Entity[] entities;
		boolean down, lowerAvail, upperAvail;
		NBTTagCompound data;
		DimensionType typeFrom, typeTo;
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
			typeFrom = dimension.provider.getDimensionType();
			
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
				
				if(respectNetherScale)
				{
					typeTo = ws.provider.getDimensionType();
					if(typeTo != typeFrom)
					{
						if(typeTo == DimensionType.NETHER)
						{
							x /= 8.0D;
							z /= 8.0D;
						}
						else if(typeFrom == DimensionType.NETHER)
						{
							x *= 8.0D;
							z *= 8.0D;
						}
					}
				}
				
				if(relativeToSpawn)
				{
					newWorldSpawnPos = ws.getSpawnPoint();
					x -= oldWorldSpawnPos.getX();
					x += newWorldSpawnPos.getX();
					z -= oldWorldSpawnPos.getZ();
					z += oldWorldSpawnPos.getZ();
				}
				
				if(down && noFallDamage)
					entity.getEntityData().setBoolean(ENTITY_FALL_TAG, false);
				
				if(entity instanceof EntityPlayerMP)
				{
					if(!down)
					{
						if(placeClimbBlock > 0)
						{
							pos = new BlockPos(x, y, z).down();
							if(ws.isAirBlock(pos))
							{
								ws.setBlockState(pos, Blocks.GLASS.getDefaultState());
								blocks.put(new V10verlapBlock(to, pos), placeClimbBlock);
							}
						}
					}
					ms.getPlayerList().transferPlayerToDimension((EntityPlayerMP)entity, to, new V10verlapTeleporter(ws, x, y, z));
				}
				else
				{
					dimension.getEntityTracker().untrack(entity);
					dimension.removeEntityDangerously(entity);
				    
				    entity.isDead = false;
				    entity.world = ws;
				    entity.dimension = to;
				    entity.setPosition(x, y, z);
				    
				    ws.getChunkFromChunkCoords(pos.getX() >> 4, pos.getZ() >> 4).addEntity(entity);
		            ws.loadedEntityList.add(entity);
		            ws.onEntityAdded(entity);
				}
			}
		}
	}
}
