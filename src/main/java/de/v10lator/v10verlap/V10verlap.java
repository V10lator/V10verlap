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
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;

@Mod(modid = "##MODID##", name = "##NAME##", version = "##VERSION##", acceptedMinecraftVersions = "1.12.2", serverSideOnly = true, acceptableRemoteVersions = "*", updateJSON="http://forge.home.v10lator.de/update.json?id=##MODID##&v=##VERSION##")
public class V10verlap {
	private final HashMap<V10verlapBlock, Integer> blocks = new HashMap<V10verlapBlock, Integer>();
	Configuration config;
	private final String ENTITY_FALL_TAG = "##MODID##.noFallDamage";
	private boolean noFallDamage;
	private int placeClimbBlock;
	
	@Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
		config = new Configuration(new File(event.getModConfigurationDirectory(), "##NAME##.cfg"));
		reloadConfig();
		MinecraftForge.EVENT_BUS.register(this);
		V10verlap_API.init(this);
	}
	
	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load event)
	{
		World dimension = event.getWorld();
		int id = dimension.provider.getDimension();
		String world = Integer.toString(id);
		config.getString("upper", world, id == 0 ? "1" : id == -1 ? "0" : "none", null);
		config.getString("lower", world, id == 0 ? "-1" : id == 1 ? "0" : "none", null);
		config.getInt("minY", world, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, null);
		config.getInt("maxY", world, dimension.getHeight(), Integer.MIN_VALUE, Integer.MAX_VALUE, null);
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
		
		int worldId;
		int lower, upper, to;
		BlockPos pos;
		double x, y, z;
		Entity[] entities;
		boolean down;
		NBTTagCompound data;
		for(WorldServer dimension: DimensionManager.getWorlds())
		{
			worldId = dimension.provider.getDimension();
			lower = V10verlap_API.getLowerWorld(worldId);
			upper = V10verlap_API.getUpperWorld(worldId);
			if(lower == worldId && upper == worldId)
				continue;
			
			entities = new Entity[dimension.loadedEntityList.size()];
			for(int i = 0; i < entities.length; i++)
				entities[i] = dimension.loadedEntityList.get(i);
			
			for(Entity entity: entities)
			{
				// TODO
				data = entity.getEntityData();
				if(noFallDamage && data.hasKey(ENTITY_FALL_TAG))
				{
					if(entity.onGround)
						data.setBoolean(ENTITY_FALL_TAG, true);
					else if(data.getBoolean(ENTITY_FALL_TAG))
						data.removeTag(ENTITY_FALL_TAG);
				}
				
				if(entity.isRiding() || entity.isDead)
					continue;
				x = entity.posX;
				y = entity.posY;
				z = entity.posZ;
				pos = new BlockPos(x, y, z).down();
				if(lower != worldId && y <= V10verlap_API.getMinY(worldId))
				{
					to = lower;
					y = V10verlap_API.getMaxY(to) - 1;
					down = true;
				}
				else if(upper != worldId && y >= V10verlap_API.getMaxY(worldId))
				{
					to = upper;
					y = V10verlap_API.getMinY(to) + 1;
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
					continue;
				
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
					WorldServer ows = ms.getWorld(worldId);
					ows.getEntityTracker().untrack(entity);
				    ows.removeEntityDangerously(entity);
				    
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
