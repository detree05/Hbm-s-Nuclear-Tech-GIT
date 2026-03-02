package com.hbm.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import com.hbm.config.SpaceConfig;
import com.hbm.entity.mob.EntityVoidFightsBack;
import com.hbm.entity.mob.EntityVoidStaresBack;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toclient.AuxParticlePacketNT;
import com.hbm.tileentity.machine.TileEntityMachineExcavator;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public final class DmitriyVoidFightsBackManager {

	private static final String NBT_ROOT = "hbmVoidFightsBack";
	private static final String NBT_TRIGGERED = "triggered";

	private static final int MESSAGES_TOTAL = 10;
	private static final int MESSAGE_INTERVAL_TICKS = 4; // 0.2s
	private static final int PHASE_DELAY_MIN_TICKS = 10 * 20;
	private static final int PHASE_DELAY_MAX_TICKS = 20 * 20;
	private static final int SHAKE_DURATION_TICKS = 30 * 20;
	private static final int SPAWN_COUNT = 50;
	private static final double MIN_SPAWN_DISTANCE = 20.0D;
	private static final double MAX_SPAWN_DISTANCE = 48.0D;
	private static final double KICK_DISTANCE_SQ = 1.0D;
	private static final int DRILL_SABOTEUR_INTERVAL_MIN_TICKS = 5 * 20;
	private static final int DRILL_SABOTEUR_INTERVAL_MAX_TICKS = 20 * 20;
	private static final double DRILL_SABOTEUR_SPAWN_DISTANCE = 30.0D;
	private static final double DRILL_SABOTEUR_SPEED_MIN = 0.12D;
	private static final double DRILL_SABOTEUR_SPEED_MAX = 0.60D; // 3x current values
	private static final double DRILL_SABOTEUR_DISABLE_DISTANCE_SQ = 2.25D;
	private static final double DRILL_SABOTEUR_LOOK_DOT_THRESHOLD = 0.985D;
	private static final double DRILL_SABOTEUR_LOOK_MAX_DISTANCE_SQ = 160.0D * 160.0D;
	private static final int DRILL_SABOTEUR_LOOK_REQUIRED_TICKS = 3 * 20;

	private static final Map<UUID, ActiveEvent> activeEvents = new HashMap<UUID, ActiveEvent>();
	private static final Map<DrillKey, DrillSabotageState> drillSabotageStates = new HashMap<DrillKey, DrillSabotageState>();
	private static boolean drillSabotageEnabled = false;

	private DmitriyVoidFightsBackManager() { }

	public static void tryTriggerFromExcavator(TileEntityMachineExcavator excavator) {
		if(excavator == null || excavator.getWorldObj() == null || excavator.getWorldObj().isRemote) {
			return;
		}

		World world = excavator.getWorldObj();
		if(world.provider == null || world.provider.dimensionId != SpaceConfig.dmitriyDimension) {
			return;
		}

		EntityPlayerMP owner = findOnlineOwner(excavator.getOwnerUUID(), excavator.getOwnerName());
		if(owner == null || owner.worldObj != world) {
			return;
		}

		if(activeEvents.containsKey(owner.getUniqueID()) || hasTriggered(owner)) {
			return;
		}

		markTriggered(owner);
		drillSabotageEnabled = true;

		Random rand = owner.worldObj.rand;
		int stareStartTick = randomTickDelay(rand);
		int chaseStartTick = stareStartTick + randomTickDelay(rand);
		ActiveEvent event = new ActiveEvent(owner.worldObj.provider.dimensionId, stareStartTick, chaseStartTick);
		spawnVoidFightsBack(owner, event);
		activeEvents.put(owner.getUniqueID(), event);

		triggerClientEffects(owner, true, SHAKE_DURATION_TICKS, -1, false);
	}

	public static void tickPlayer(EntityPlayerMP player) {
		if(player == null || player.worldObj == null || player.worldObj.isRemote) {
			return;
		}

		if(!drillSabotageEnabled && hasTriggered(player)) {
			drillSabotageEnabled = true;
		}

		ActiveEvent event = activeEvents.get(player.getUniqueID());
		if(event == null) {
			return;
		}

		if(player.dimension != event.dimension || player.isDead) {
			stopClientEffects(player);
			cleanupEvent(event, player.worldObj);
			activeEvents.remove(player.getUniqueID());
			return;
		}

		event.ticksElapsed++;

		if(event.messagesSent < MESSAGES_TOTAL && event.ticksElapsed >= event.nextMessageTick) {
			sendGhostChat(player);
			event.messagesSent++;
			event.nextMessageTick += MESSAGE_INTERVAL_TICKS;
		}

		if(!event.startedStare && event.ticksElapsed >= event.stareStartTick) {
			event.startedStare = true;
			startStarePhase(player, event);
		}

		if(!event.startedChase && event.ticksElapsed >= event.chaseStartTick) {
			event.startedChase = true;
			startChasePhase(player, event);
		}

		if(event.startedChase && isAnyEntityCloseToPlayer(player, event)) {
			kickPlayer(player);
			cleanupEvent(event, player.worldObj);
			activeEvents.remove(player.getUniqueID());
		}
	}

	public static void clearPlayer(UUID playerId) {
		if(playerId == null) {
			return;
		}
		ActiveEvent event = activeEvents.remove(playerId);
		if(event == null) {
			return;
		}
		MinecraftServer server = MinecraftServer.getServer();
		if(server != null && server.worldServers != null) {
			for(World world : server.worldServers) {
				if(world != null && !world.isRemote && world.provider.dimensionId == event.dimension) {
					cleanupEvent(event, world);
					break;
				}
			}
		}
	}

	public static void clearWorld(World world) {
		if(world == null || world.isRemote) {
			return;
		}

		Iterator<Map.Entry<UUID, ActiveEvent>> iter = activeEvents.entrySet().iterator();
		while(iter.hasNext()) {
			Map.Entry<UUID, ActiveEvent> entry = iter.next();
			ActiveEvent event = entry.getValue();
			if(event.dimension == world.provider.dimensionId) {
				cleanupEvent(event, world);
				iter.remove();
			}
		}

		Iterator<Map.Entry<DrillKey, DrillSabotageState>> sabotageIter = drillSabotageStates.entrySet().iterator();
		while(sabotageIter.hasNext()) {
			Map.Entry<DrillKey, DrillSabotageState> entry = sabotageIter.next();
			if(entry.getKey().dimension == world.provider.dimensionId) {
				sabotageIter.remove();
			}
		}
	}

	public static void tickWorld(World world) {
		if(world == null || world.isRemote || !drillSabotageEnabled || world.provider == null) {
			return;
		}

		long worldTime = world.getTotalWorldTime();
		Set<DrillKey> activeDrills = new HashSet<DrillKey>();

		for(Object obj : world.loadedTileEntityList) {
			if(!(obj instanceof TileEntityMachineExcavator)) {
				continue;
			}
			TileEntityMachineExcavator drill = (TileEntityMachineExcavator) obj;
			if(drill == null || !isExcavatorDrilling(drill)) {
				continue;
			}

			DrillKey key = new DrillKey(world.provider.dimensionId, drill.xCoord, drill.yCoord, drill.zCoord);
			activeDrills.add(key);

			DrillSabotageState state = drillSabotageStates.get(key);
			if(state == null) {
				state = new DrillSabotageState(worldTime + randomSaboteurSpawnDelay(world.rand));
				drillSabotageStates.put(key, state);
			}

			Entity activeSaboteur = state.activeEntityId >= 0 ? world.getEntityByID(state.activeEntityId) : null;
			if(state.activeEntityId >= 0 && !(activeSaboteur instanceof EntityVoidStaresBack && !activeSaboteur.isDead)) {
				state.activeEntityId = -1;
				state.lookTicks = 0;
			}

			if(state.activeEntityId < 0 && worldTime >= state.nextSpawnTick) {
				spawnDrillSaboteur(world, drill, state, worldTime);
			}
		}

		Iterator<Map.Entry<DrillKey, DrillSabotageState>> iter = drillSabotageStates.entrySet().iterator();
		while(iter.hasNext()) {
			Map.Entry<DrillKey, DrillSabotageState> entry = iter.next();
			DrillKey key = entry.getKey();
			if(key.dimension != world.provider.dimensionId) {
				continue;
			}

			DrillSabotageState state = entry.getValue();
			TileEntity te = world.getTileEntity(key.x, key.y, key.z);
			if(!(te instanceof TileEntityMachineExcavator)) {
				killSaboteurIfPresent(world, state);
				iter.remove();
				continue;
			}

			TileEntityMachineExcavator drill = (TileEntityMachineExcavator) te;
			boolean drilling = activeDrills.contains(key);

			if(state.activeEntityId >= 0) {
				Entity entity = world.getEntityByID(state.activeEntityId);
				if(!(entity instanceof EntityVoidStaresBack) || entity.isDead) {
					state.activeEntityId = -1;
					state.lookTicks = 0;
				} else {
					EntityVoidStaresBack saboteur = (EntityVoidStaresBack) entity;
					if(isLookedAtByAnyPlayer(world, saboteur)) {
						state.lookTicks++;
					} else {
						state.lookTicks = 0;
					}

					if(state.lookTicks >= DRILL_SABOTEUR_LOOK_REQUIRED_TICKS) {
						saboteur.startCollapse();
						state.activeEntityId = -1;
						state.lookTicks = 0;
					} else if(moveSaboteurTowardDrill(saboteur, drill)) {
						disableDrill(drill);
						saboteur.startCollapse();
						state.activeEntityId = -1;
						state.lookTicks = 0;
					}
				}
			}

			if(!drilling && state.activeEntityId < 0) {
				iter.remove();
			}
		}
	}

	private static void startStarePhase(EntityPlayerMP player, ActiveEvent event) {
		for(Integer entityId : event.entityIds) {
			Entity entity = player.worldObj.getEntityByID(entityId.intValue());
			if(entity instanceof EntityVoidFightsBack && !entity.isDead) {
				((EntityVoidFightsBack) entity).startStaringAt(player);
			}
		}
	}

	private static void startChasePhase(EntityPlayerMP player, ActiveEvent event) {
		for(Integer entityId : event.entityIds) {
			Entity entity = player.worldObj.getEntityByID(entityId.intValue());
			if(entity instanceof EntityVoidFightsBack && !entity.isDead) {
				((EntityVoidFightsBack) entity).startChasingTarget(player);
			}
		}
	}

	private static boolean isAnyEntityCloseToPlayer(EntityPlayerMP player, ActiveEvent event) {
		for(Integer entityId : event.entityIds) {
			Entity entity = player.worldObj.getEntityByID(entityId.intValue());
			if(entity == null || entity.isDead) {
				continue;
			}
			if(entity.getDistanceSqToEntity(player) <= KICK_DISTANCE_SQ) {
				return true;
			}
		}
		return false;
	}

	private static void spawnVoidFightsBack(EntityPlayerMP player, ActiveEvent event) {
		Random rand = player.worldObj.rand;
		for(int i = 0; i < SPAWN_COUNT; i++) {
			double radius = MIN_SPAWN_DISTANCE + rand.nextDouble() * (MAX_SPAWN_DISTANCE - MIN_SPAWN_DISTANCE);
			double angle = rand.nextDouble() * Math.PI * 2.0D;

			double x = player.posX + Math.cos(angle) * radius;
			double z = player.posZ + Math.sin(angle) * radius;
			double y = player.posY + (rand.nextInt(9) - 4);
			y = Math.max(2.0D, Math.min(y, player.worldObj.getHeight() - 2.0D));

			EntityVoidFightsBack entity = new EntityVoidFightsBack(player.worldObj);
			float bodyYaw = rand.nextFloat() * 360.0F;
			float headYaw = bodyYaw + (rand.nextFloat() * 240.0F - 120.0F);
			float yawToPlayer = (float) (Math.atan2(player.posZ - z, player.posX - x) * 180.0D / Math.PI) - 90.0F;
			if(Math.abs(MathHelper.wrapAngleTo180_float(headYaw - yawToPlayer)) < 25.0F) {
				headYaw = MathHelper.wrapAngleTo180_float(headYaw + 90.0F);
			}
			float headPitch = rand.nextFloat() * 20.0F - 10.0F;
			entity.setPositionAndRotation(x, y, z, bodyYaw, 0.0F);
			entity.setIdleHeadRotation(headYaw, headPitch);
			player.worldObj.spawnEntityInWorld(entity);
			event.entityIds.add(Integer.valueOf(entity.getEntityId()));
		}
	}

	private static void cleanupEvent(ActiveEvent event, World world) {
		if(event == null || world == null) {
			return;
		}

		for(Integer entityId : event.entityIds) {
			Entity entity = world.getEntityByID(entityId.intValue());
			if(entity instanceof EntityVoidFightsBack && !entity.isDead) {
				entity.setDead();
			}
		}
	}

	private static void sendGhostChat(EntityPlayerMP player) {
		String sender = player.getCommandSenderName();
		if(sender == null || sender.isEmpty()) {
			sender = "player";
		}
		String message = createGhostMessage(player.worldObj.rand);
		player.addChatMessage(new ChatComponentText("<" + sender + "> " + message));
	}

	private static String createGhostMessage(Random rand) {
		final int minLength = 8;
		final int maxLength = 26;
		final String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()-_=+[]{};:'\",.?/\\\\|";
		int length = minLength + rand.nextInt(maxLength - minLength + 1);
		StringBuilder builder = new StringBuilder(length + 4);
		for(int i = 0; i < length; i++) {
			builder.append(chars.charAt(rand.nextInt(chars.length())));
			if(i > 2 && i < length - 2 && rand.nextInt(12) == 0) {
				builder.append(' ');
			}
		}
		return builder.toString().trim();
	}

	private static void stopClientEffects(EntityPlayerMP player) {
		triggerClientEffects(player, false, 0, 0, true);
	}

	private static void triggerClientEffects(EntityPlayerMP player, boolean playIntro, int shakeTicks, int loopTicks, boolean stopLoop) {
		NBTTagCompound data = new NBTTagCompound();
		data.setString("type", "void_fights_back_trigger");
		data.setBoolean("playIntro", playIntro);
		data.setInteger("shakeTicks", shakeTicks);
		data.setInteger("loopTicks", loopTicks);
		data.setBoolean("stopLoop", stopLoop);
		PacketDispatcher.wrapper.sendTo(new AuxParticlePacketNT(data, player.posX, player.posY, player.posZ), player);
	}

	private static void kickPlayer(EntityPlayerMP player) {
		if(player == null || player.playerNetServerHandler == null) {
			return;
		}
		player.playerNetServerHandler.kickPlayerFromServer(EntityVoidFightsBack.buildVoidKickMessage());
	}

	private static EntityPlayerMP findOnlineOwner(UUID ownerUuid, String ownerName) {
		MinecraftServer server = MinecraftServer.getServer();
		if(server == null || server.getConfigurationManager() == null) {
			return null;
		}

		for(Object obj : server.getConfigurationManager().playerEntityList) {
			if(!(obj instanceof EntityPlayerMP)) {
				continue;
			}
			EntityPlayerMP player = (EntityPlayerMP) obj;
			if(ownerUuid != null && ownerUuid.equals(player.getUniqueID())) {
				return player;
			}
			if(ownerName != null && ownerName.equalsIgnoreCase(player.getCommandSenderName())) {
				return player;
			}
		}

		return null;
	}

	private static int randomTickDelay(Random rand) {
		return PHASE_DELAY_MIN_TICKS + rand.nextInt(PHASE_DELAY_MAX_TICKS - PHASE_DELAY_MIN_TICKS + 1);
	}

	private static int randomSaboteurSpawnDelay(Random rand) {
		return DRILL_SABOTEUR_INTERVAL_MIN_TICKS + rand.nextInt(DRILL_SABOTEUR_INTERVAL_MAX_TICKS - DRILL_SABOTEUR_INTERVAL_MIN_TICKS + 1);
	}

	private static boolean isExcavatorDrilling(TileEntityMachineExcavator drill) {
		if(drill == null || drill.getWorldObj() == null || drill.getWorldObj().isRemote) {
			return false;
		}
		return drill.enableDrill && drill.operational && drill.getInstalledDrill() != null;
	}

	private static void spawnDrillSaboteur(World world, TileEntityMachineExcavator drill, DrillSabotageState state, long worldTime) {
		Random rand = world.rand;
		double angle = rand.nextDouble() * Math.PI * 2.0D;
		double targetX = drill.xCoord + 0.5D;
		double targetY = drill.yCoord + 1.0D;
		double targetZ = drill.zCoord + 0.5D;
		double spawnX = targetX + Math.cos(angle) * DRILL_SABOTEUR_SPAWN_DISTANCE;
		double spawnZ = targetZ + Math.sin(angle) * DRILL_SABOTEUR_SPAWN_DISTANCE;

		EntityVoidStaresBack saboteur = new EntityVoidStaresBack(world);
		saboteur.setPositionAndRotation(spawnX, targetY, spawnZ, rand.nextFloat() * 360.0F, 0.0F);
		world.spawnEntityInWorld(saboteur);

		state.activeEntityId = saboteur.getEntityId();
		state.lookTicks = 0;
		state.nextSpawnTick = worldTime + randomSaboteurSpawnDelay(world.rand);
	}

	private static boolean moveSaboteurTowardDrill(EntityVoidStaresBack saboteur, TileEntityMachineExcavator drill) {
		double targetX = drill.xCoord + 0.5D;
		double targetY = drill.yCoord + 1.0D;
		double targetZ = drill.zCoord + 0.5D;
		double dx = targetX - saboteur.posX;
		double dy = targetY - saboteur.posY;
		double dz = targetZ - saboteur.posZ;
		double distanceSq = dx * dx + dy * dy + dz * dz;
		if(distanceSq <= DRILL_SABOTEUR_DISABLE_DISTANCE_SQ) {
			return true;
		}

		double distance = Math.sqrt(distanceSq);
		if(distance > 0.0001D) {
			float proximity = 1.0F - MathHelper.clamp_float((float)(distance / DRILL_SABOTEUR_SPAWN_DISTANCE), 0.0F, 1.0F);
			// Accelerate as it gets closer to the drill.
			double speed = DRILL_SABOTEUR_SPEED_MIN + (DRILL_SABOTEUR_SPEED_MAX - DRILL_SABOTEUR_SPEED_MIN) * proximity;
			speed = Math.min(speed, distance);
			double nx = dx / distance;
			double ny = dy / distance;
			double nz = dz / distance;
			saboteur.setPosition(saboteur.posX + nx * speed, saboteur.posY + ny * speed, saboteur.posZ + nz * speed);
			saboteur.rotationYaw = (float)(Math.atan2(nz, nx) * 180.0D / Math.PI) - 90.0F;
			saboteur.renderYawOffset = saboteur.rotationYaw;
			saboteur.rotationYawHead = saboteur.rotationYaw;
		}

		return false;
	}

	private static boolean isLookedAtByAnyPlayer(World world, EntityVoidStaresBack saboteur) {
		for(Object obj : world.playerEntities) {
			if(!(obj instanceof EntityPlayerMP)) {
				continue;
			}
			EntityPlayerMP player = (EntityPlayerMP) obj;
			if(player.isDead || player.getDistanceSqToEntity(saboteur) > DRILL_SABOTEUR_LOOK_MAX_DISTANCE_SQ) {
				continue;
			}

			Vec3 look = player.getLookVec();
			if(look == null) {
				continue;
			}

			Vec3 toEntity = Vec3.createVectorHelper(
				saboteur.posX - player.posX,
				(saboteur.posY + saboteur.height * 0.5D) - (player.posY + player.getEyeHeight()),
				saboteur.posZ - player.posZ
			);
			double dist = toEntity.lengthVector();
			if(dist < 0.0001D) {
				return true;
			}

			toEntity = Vec3.createVectorHelper(toEntity.xCoord / dist, toEntity.yCoord / dist, toEntity.zCoord / dist);
			double dot = look.dotProduct(toEntity);
			if(dot >= DRILL_SABOTEUR_LOOK_DOT_THRESHOLD && player.canEntityBeSeen(saboteur)) {
				return true;
			}
		}

		return false;
	}

	private static void disableDrill(TileEntityMachineExcavator drill) {
		drill.enableDrill = false;
		drill.markDirty();
	}

	private static void killSaboteurIfPresent(World world, DrillSabotageState state) {
		if(state == null || state.activeEntityId < 0) {
			return;
		}
		Entity entity = world.getEntityByID(state.activeEntityId);
		if(entity instanceof EntityVoidStaresBack && !entity.isDead) {
			entity.setDead();
		}
		state.activeEntityId = -1;
		state.lookTicks = 0;
	}

	private static boolean hasTriggered(EntityPlayer player) {
		NBTTagCompound persisted = player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
		NBTTagCompound data = persisted.getCompoundTag(NBT_ROOT);
		return data.getBoolean(NBT_TRIGGERED);
	}

	private static void markTriggered(EntityPlayer player) {
		NBTTagCompound entityData = player.getEntityData();
		NBTTagCompound persisted = entityData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
		NBTTagCompound data = persisted.getCompoundTag(NBT_ROOT);
		data.setBoolean(NBT_TRIGGERED, true);
		persisted.setTag(NBT_ROOT, data);
		entityData.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
	}

	private static final class ActiveEvent {
		private final int dimension;
		private final int stareStartTick;
		private final int chaseStartTick;
		private final List<Integer> entityIds = new ArrayList<Integer>();
		private int ticksElapsed = 0;
		private int nextMessageTick = 0;
		private int messagesSent = 0;
		private boolean startedStare = false;
		private boolean startedChase = false;

		private ActiveEvent(int dimension, int stareStartTick, int chaseStartTick) {
			this.dimension = dimension;
			this.stareStartTick = stareStartTick;
			this.chaseStartTick = chaseStartTick;
		}
	}

	private static final class DrillKey {
		private final int dimension;
		private final int x;
		private final int y;
		private final int z;

		private DrillKey(int dimension, int x, int y, int z) {
			this.dimension = dimension;
			this.x = x;
			this.y = y;
			this.z = z;
		}

		@Override
		public boolean equals(Object obj) {
			if(this == obj) {
				return true;
			}
			if(!(obj instanceof DrillKey)) {
				return false;
			}
			DrillKey other = (DrillKey) obj;
			return this.dimension == other.dimension && this.x == other.x && this.y == other.y && this.z == other.z;
		}

		@Override
		public int hashCode() {
			int result = dimension;
			result = 31 * result + x;
			result = 31 * result + y;
			result = 31 * result + z;
			return result;
		}
	}

	private static final class DrillSabotageState {
		private long nextSpawnTick;
		private int activeEntityId = -1;
		private int lookTicks = 0;

		private DrillSabotageState(long nextSpawnTick) {
			this.nextSpawnTick = nextSpawnTick;
		}
	}
}
