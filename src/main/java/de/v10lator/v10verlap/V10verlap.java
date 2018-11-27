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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;

import de.v10lator.v10verlap.api.Hooks;
import de.v10lator.v10verlap.api.V10verlapException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
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
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;

@Mod(modid = "##MODID##", name = "##NAME##", version = "##VERSION##", acceptedMinecraftVersions = "1.12.2", acceptableRemoteVersions = "*", updateJSON="http://forge.home.v10lator.de/update.json?id=##MODID##&v=##VERSION##")
public class V10verlap {
	private final HashMap<V10verlapBlock, Integer> blocks = new HashMap<V10verlapBlock, Integer>();
	private final String ENTITY_FALL_TAG = "##MODID##.noFallDamage";
	private final String ENTITY_OLD_Y = "##MODID##.oldY";
	boolean noFallDamage, relativeToSpawn, transformNetherScale = true, playerOnly;
	int placeTmpBlocks;
	final String permNode = "##MODID##.command";
	public V10verlapConfigHandler configManager;
	private final ArrayList<TeleportMetadata> metaData = new ArrayList<TeleportMetadata>();
	private File saveFile;
	final HashMap<Integer, boolean[]> whitelist = new HashMap<Integer, boolean[]>();
	public final HashMap<Integer, Double> scaleCache = new HashMap<Integer, Double>();
	public final HashMap<Integer, Integer> lowerCache = new HashMap<Integer, Integer>();
	public final HashMap<Integer, Integer> upperCache = new HashMap<Integer, Integer>();
	public final HashMap<Integer, Integer> minCache = new HashMap<Integer, Integer>();
	public final HashMap<Integer, Integer> maxCache = new HashMap<Integer, Integer>();
	public MinecraftServer server;
	
	@Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
		saveFile = new File(event.getModConfigurationDirectory(), "##NAME##.cfg");
	}
	
	@Mod.EventHandler
	public void onServerStart(FMLServerStartingEvent event) {
		if(FMLCommonHandler.instance().getEffectiveSide() != Side.SERVER)
		{
			saveFile = null;
			return;
		}
		server = event.getServer();
		PermissionAPI.registerNode(permNode, DefaultPermissionLevel.OP, "Use the /v10verlap command");
		configManager = new V10verlapConfigHandler(this, new Configuration(saveFile));
		Hooks.init(this);
		MinecraftForge.EVENT_BUS.register(this);
		for(World world: DimensionManager.getWorlds())
			this.onWorldLoad(new WorldEvent.Load(world));
		event.registerServerCommand(new V10verlapCommand(this));
		configManager.start();
	}
	
	@Mod.EventHandler
	public void onServerStop(FMLServerStoppingEvent event) {
		FMLCommonHandler ch = FMLCommonHandler.instance();
		if(ch.getEffectiveSide() != Side.SERVER)
			return;
		MinecraftForge.EVENT_BUS.unregister(this);
		for(V10verlapBlock block: blocks.keySet())
			resetBlock(block);
		blocks.clear();
		configManager.die();
		configManager = null;
	}
	
	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load event)
	{
		World dimension = event.getWorld();
		int id = dimension.provider.getDimension();
		String world = Integer.toString(id);
		Configuration config = configManager.getLockedConfig();
		config.get(world, "upper", id == 0 ? "1" : id == -1 ? "0" : "none");
		config.get(world, "lower", id == 0 ? "-1" : id == 1 ? "0" : "none");
		config.get(world, "minY", 0);
		config.get(world, "maxY", dimension.getHeight());
		config.get(world, "scale", transformNetherScale && dimension.provider.getDimensionType() == DimensionType.NETHER ? 8.0D : 1.0D);
		configManager.releaseLock();
	}
	
	@SubscribeEvent
	public void onBlockChange(BlockEvent.BreakEvent event)
	{
		V10verlapBlock block = new V10verlapBlock(event.getWorld().provider.getDimension(), event.getPos(), null);
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
	
	@SubscribeEvent
	public void onWorldTick(WorldTickEvent event)
	{
		if(event.phase == TickEvent.Phase.START)
			return;
		
		int worldId = event.world.provider.getDimension();
		int lower = 0, upper = 0, minY = 0, maxY = 0;
		boolean down, lowerAvail, upperAvail;
		
		try
		{
			lower = Hooks.getLowerWorld(worldId);
			minY = Hooks.getMinY(worldId);
			lowerAvail = true;
		}
		catch(V10verlapException e)
		{
			lowerAvail = false;
		}
		try
		{
			upper = Hooks.getUpperWorld(worldId);
			maxY = Hooks.getMaxY(worldId);
			upperAvail = true;
		}
		catch(V10verlapException e)
		{
			upperAvail = false;
		}
		
		BlockPos oldWorldSpawnPos = relativeToSpawn ? event.world.getSpawnPoint() : null;
		double oldScale = Hooks.getScale(worldId);
		int to, blockY, oldY;
		double x, z, newScale;
		BlockPos pos, newWorldSpawnPos = null;
		NBTTagCompound data;
		
		for(Entity entity: playerOnly ? event.world.playerEntities : event.world.loadedEntityList)
		{
			data = entity.getEntityData();
			if(noFallDamage && data.hasKey(ENTITY_FALL_TAG))
			{
				if(entity.onGround)
					data.setBoolean(ENTITY_FALL_TAG, true);
				else if(data.getBoolean(ENTITY_FALL_TAG))
					data.removeTag(ENTITY_FALL_TAG);
			}
			
			if((!lowerAvail && !upperAvail) || entity.isDead || entity.isRiding() || (playerOnly && entity.isBeingRidden()))
				continue;
			
			oldY = data.getInteger(ENTITY_OLD_Y);
			blockY = MathHelper.floor(entity.posY);
			if(oldY == blockY)
				continue;
			data.setInteger(ENTITY_OLD_Y, blockY);
			
			down = oldY > blockY;
			if(down)
			{
				if(lowerAvail && blockY <= minY)
				{
					try
					{
						blockY = Hooks.getMaxY(lower) - 1;
					}
					catch(V10verlapException e)
					{
						LogManager.getLogger("##NAME##").error("Invalid link between DIM" + worldId + " and DIM" + lower + ". Canceling teleport!");
						lowerAvail = false;
						continue;
					}
					to = lower;
				}
				else
					continue;
			}
			else if(upperAvail && blockY >= maxY)
			{
				try
				{
					blockY = Hooks.getMinY(upper) + 1;
				}
				catch(V10verlapException e)
				{
					LogManager.getLogger("##NAME##").error("Invalid link between DIM" + worldId + " and DIM" + upper + ". Canceling teleport!");
					upperAvail = false;
					continue;
				}
				to = upper;
			}
			else
				continue;
			
			if (!ForgeHooks.onTravelToDimension(entity, to))
			{
				LogManager.getLogger("##NAME##").info("Another plugin blocked the teleport from DIM" + worldId + " to DIM" + to);
				continue;
			}
			
			WorldServer ws = server.getWorld(to);
			if(ws == null)
			{
				LogManager.getLogger("##NAME##").info("Can't load DIM" + to);
				if(down)
					lowerAvail = false;
				else
					upperAvail = false;
				continue;
			}
			
			x = entity.posX;
			z = entity.posZ;
			if(relativeToSpawn)
			{
				newWorldSpawnPos = ws.getSpawnPoint();
				x -= oldWorldSpawnPos.getX();
				z -= oldWorldSpawnPos.getZ();
			}
			
			newScale = Hooks.getScale(to);
			if(oldScale != newScale)
			{
				x *= oldScale;
				z *= oldScale;
				x /= newScale;
				z /= newScale;
			}
			
			if(relativeToSpawn)
			{
				x += newWorldSpawnPos.getX();
				z += newWorldSpawnPos.getZ();
			}
			
			if(down && noFallDamage)
				data.setBoolean(ENTITY_FALL_TAG, false);
			
			if(entity instanceof EntityPlayerMP)
			{
				if(!whitelist.isEmpty())
				{
					pos = new BlockPos(x, blockY, z);
					checkWhitelist(ws, pos);
					pos = pos.up();
					if(placeTmpBlocks > 0 && (checkWhitelist(ws, pos) || ws.isAirBlock(pos)))
					{
						pos = pos.up();
						if(!whitelistSideCheck(ws, pos))
						{
							IBlockState bs = ws.getBlockState(pos);
							if(bs.getBlock() instanceof BlockFalling)
								placeTmpBlock(ws, pos, bs);
						}
					}
				}
				
				if(!down && placeTmpBlocks > 0)
				{
					pos = new BlockPos(x, blockY, z).down();
					if(ws.isAirBlock(pos))
						placeTmpBlock(ws, pos, Blocks.AIR.getDefaultState());
				}
			}
			
			metaData.add(new TeleportMetadata(entity, event.world, ws, x, blockY, z));
		}
	}
	
	@SubscribeEvent
	public void onServerTick(ServerTickEvent event) {
		if(event.phase == TickEvent.Phase.START)
			return;
		
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
					resetBlock(entry.getKey());
					iter.remove();
				}
				else
					entry.setValue(c);
			}
		}
		
		for(TeleportMetadata meta: metaData)
			this.teleport(meta);
		metaData.clear();
	}
	
	private void placeTmpBlock(WorldServer world, BlockPos pos, IBlockState oldState)
	{
		world.setBlockState(pos, Blocks.GLASS.getDefaultState());
		blocks.put(new V10verlapBlock(world.provider.getDimension(), pos, oldState), placeTmpBlocks);
	}
	
	private boolean checkWhitelist(WorldServer world, BlockPos pos)
	{
		boolean ret = false;
		IBlockState bs = world.getBlockState(pos);
		Block block = bs.getBlock();
		int id = Block.getIdFromBlock(block);
		if(whitelist.containsKey(id))
		{
			boolean[] mask = whitelist.get(id);
			if(mask != null)
			{
				if(mask[block.getMetaFromState(bs)])
					ret = true;
			}
			else
				ret = true;
			
			if(ret)
				world.setBlockState(pos, Blocks.AIR.getDefaultState());
		}
		if(placeTmpBlocks > 0 && (ret || world.isAirBlock(pos)))
		{
			whitelistSideCheck(world, pos.north());
			whitelistSideCheck(world, pos.east());
			whitelistSideCheck(world, pos.south());
			whitelistSideCheck(world, pos.west());
		}
		return ret;
	}
	
	private boolean whitelistSideCheck(WorldServer world, BlockPos pos)
	{
		IBlockState bs = world.getBlockState(pos);
		if(bs.getBlock() instanceof BlockLiquid)
		{
			placeTmpBlock(world, pos, bs);
			return true;
		}
		return false;
	}
	
	private void resetBlock(V10verlapBlock block)
	{
		WorldServer ws = server.getWorld(block.dim);
		if(ws.getBlockState(block.pos).getMaterial() == Material.GLASS)
			ws.setBlockState(block.pos, block.oldState);
	}
	
	private void teleport(TeleportMetadata meta)
	{
		if(playerOnly)
		{
			server.getPlayerList().transferPlayerToDimension((EntityPlayerMP)meta.entity, meta.to.provider.getDimension(), new V10verlapTeleporter(meta));
			return;
		}
		
		List<Entity> passengers = meta.entity.getPassengers();
		if(!passengers.isEmpty())
		{
			for(Entity passenger: passengers)
			{
				passenger.dismountRidingEntity();
				teleport(new TeleportMetadata(passenger, meta.from, meta.to, meta.x, meta.y, meta.z));
			}
		}
		
		if(meta.entity instanceof EntityPlayerMP)
			server.getPlayerList().transferPlayerToDimension((EntityPlayerMP)meta.entity, meta.to.provider.getDimension(), new V10verlapTeleporter(meta));
		else
		{
			((WorldServer)meta.from).getEntityTracker().untrack(meta.entity);
			meta.from.removeEntityDangerously(meta.entity);
		    
			meta.entity.isDead = false;
			meta.entity.world = meta.to;
			meta.entity.dimension = meta.to.provider.getDimension();
			meta.entity.setPosition(meta.x, meta.y, meta.z);
		    
			meta.to.getChunkFromChunkCoords(((int)Math.floor(meta.x)) >> 4, ((int)Math.floor(meta.z)) >> 4).addEntity(meta.entity);
			meta.to.loadedEntityList.add(meta.entity);
			meta.to.onEntityAdded(meta.entity);
		}
		
		if(!passengers.isEmpty())
			for(Entity passenger: passengers)
				passenger.startRiding(meta.entity, true);
	}
	
	class TeleportMetadata
	{
		private final Entity entity;
		private final World from;
		final WorldServer to;
		final double x, z;
		final int y;
		
		private TeleportMetadata(Entity entity, World from, WorldServer to, double x, int y, double z)
		{
			this.entity = entity;
			this.from = from;
			this.to= to;
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}
}
