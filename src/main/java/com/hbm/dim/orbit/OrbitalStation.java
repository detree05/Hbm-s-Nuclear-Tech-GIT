package com.hbm.dim.orbit;

import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.BlockOrbitalStation;
import com.hbm.dim.CelestialBody;
import com.hbm.dim.CelestialTeleporter;
import com.hbm.dim.SolarSystem;
import com.hbm.dim.SolarSystemWorldSavedData;
import com.hbm.entity.missile.EntityRideableRocket;
import com.hbm.entity.missile.EntityRideableRocket.RocketState;
import com.hbm.handler.ThreeInts;
import com.hbm.items.ItemVOTVdrive.Destination;
import com.hbm.tileentity.machine.TileEntityMachineHTRS5;
import com.hbm.tileentity.machine.TileEntityOrbitalStation;
import com.hbm.util.BufferUtil;

import api.hbm.tile.IPropulsion;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.ForgeDirection;

public class OrbitalStation {

	public String name = ""; // I dub thee

	public CelestialBody orbiting;
	public CelestialBody target;
	private CelestialBody lastAttemptedTarget;

	public StationState state = StationState.ORBIT;
	public int stateTimer;
	public int maxStateTimer = 100;

	public boolean hasStation = false;

	// the coordinates of the station within the dimension
	public int dX;
	public int dZ;

	public boolean hasEngines = true;
	public List<ThreeInts> errorsAt = new ArrayList<ThreeInts>();
	public int errorTimer;

	public float gravityMultiplier = 1;

	public enum StationState {
		ORBIT, // big chillin
		LEAVING, // prepare engines for transfer
		TRANSFER, // going from A to B
		ARRIVING, // spool down engines
	}

	private TileEntityOrbitalStation mainPort;
	private HashMap<ThreeInts, TileEntityOrbitalStation> ports = new HashMap<>();
	private int portIndex = 0;

	private HashSet<IPropulsion> engines = new HashSet<>();

	public static OrbitalStation clientStation = new OrbitalStation(CelestialBody.getBody(0));
	public static List<OrbitalStation> orbitingStations = new ArrayList<OrbitalStation>();

	public static final int STATION_SIZE = 1024; // total area for each station
	public static final int BUFFER_SIZE = 256; // size of the buffer region that drops you out of orbit (preventing seeing other stations)
	public static final int WARNING_SIZE = 32; // size of the region that warns the player about falling out of orbit

	private static final int STATION_CORE_Y = 127;
	private static final int PROPULSION_BOUNDS_RADIUS = 6;



	/**
	 * Space station spatial space
	 * - Stations are spread out over the orbital dimension, in restricted areas
	 * - Attempting to leave the region your station is in will cause you to fall back to the orbited planet
	 * - The current station you are near is fetched using the player's XZ coordinate
	 * - The station will determine what body is being orbited, and therefore what to show in the skybox
	 */

	// For client
	public OrbitalStation(CelestialBody orbiting) {
		this.orbiting = orbiting;
		this.target = orbiting;
		this.lastAttemptedTarget = orbiting;
	}

	// For server
	public OrbitalStation(CelestialBody orbiting, int x, int z) {
		this(orbiting);
		this.dX = x;
		this.dZ = z;
	}

	public void travelTo(World world, CelestialBody target) {
		if(state != StationState.ORBIT) return; // only when at rest can we start a new journey
		if(!canTravel(orbiting, target)) return;

		setState(StationState.LEAVING, getLeaveTime());
		this.target = target;
	}

	public void update(World world) {
		if(!world.isRemote) {
			if(state == StationState.LEAVING) {
				if(stateTimer > maxStateTimer) {
					setState(StationState.TRANSFER, getTransferTime());
				}
			} else if(state == StationState.TRANSFER) {
				if(stateTimer > maxStateTimer) {
					if(target == SolarSystem.kerbol && relocateToKerbol(world)) {
						return;
					}
					setState(StationState.ARRIVING, getArriveTime());
					orbiting = target;
				}
			} else if(state == StationState.ARRIVING) {
				if(stateTimer > maxStateTimer) {
					setState(StationState.ORBIT, 0);
				}
			}

			SolarSystemWorldSavedData.get(world).markDirty();

			hasEngines = engines.size() > 0;

			errorTimer--;
			if(errorTimer <= 0) {
				errorsAt = new ArrayList<ThreeInts>();
				errorTimer = 0;
			}
		}

		stateTimer++;
	}

	public void tickServer(World world) {
		if(state == StationState.LEAVING) {
			if(stateTimer > maxStateTimer) {
				ensureMainPort(world);
				if(mainPort != null) {
					setState(StationState.TRANSFER, getTransferTime());
				}
			}
		} else if(state == StationState.TRANSFER) {
			if(stateTimer > maxStateTimer) {
				if(target == SolarSystem.kerbol && relocateToKerbol(world)) {
					return;
				}
				setState(StationState.ARRIVING, getArriveTime());
				orbiting = target;
			}
		} else if(state == StationState.ARRIVING) {
			if(stateTimer > maxStateTimer) {
				setState(StationState.ORBIT, 0);
			}
		}

		SolarSystemWorldSavedData.get(world).markDirty();
		stateTimer++;
	}

	private boolean canTravel(CelestialBody from, CelestialBody to) {
		if(engines.size() == 0) return false;

		lastAttemptedTarget = to;

		if(to != null && to.getEnum() == SolarSystem.Body.KERBOL && !SolarSystem.isKerbolBlackhole()) {
			errorsAt = new ArrayList<ThreeInts>();
			errorTimer = 100;
			return false;
		}

		if(to != null && to.getEnum() == SolarSystem.Body.MINMUS && SolarSystem.isMinmusDestroyed()) {
			errorsAt = new ArrayList<ThreeInts>();
			for(IPropulsion engine : engines) {
				TileEntity te = engine.getTileEntity();
				errorsAt.add(new ThreeInts(te.xCoord, te.yCoord, te.zCoord));
			}
			errorTimer = 100;
			return false;
		}

		double deltaV = SolarSystem.getDeltaVBetween(from, to);
		int shipMass = 200_000; // Always static, to not punish building big cool stations
		float totalThrust = getTotalThrust();
		boolean isKerbolTarget = to == SolarSystem.kerbol;
		boolean hasSingularityThruster = false;
		if(isKerbolTarget) {
			for(IPropulsion engine : engines) {
				if(engine instanceof TileEntityMachineHTRS5) {
					hasSingularityThruster = true;
					break;
				}
			}
		}

		boolean canTravel = true;
		errorsAt = new ArrayList<ThreeInts>();

		for(IPropulsion engine : engines) {
			if(isKerbolTarget && !hasSingularityThruster) {
				TileEntity te = engine.getTileEntity();
				canTravel = false;
				errorsAt.add(new ThreeInts(te.xCoord, te.yCoord, te.zCoord));
				errorTimer = 100;
				continue;
			}

			float massPortion = engine.getThrust() / totalThrust;
			if(!engine.canPerformBurn(Math.round(shipMass * massPortion), deltaV)) {
				TileEntity te = engine.getTileEntity();
				canTravel = false;
				errorsAt.add(new ThreeInts(te.xCoord, te.yCoord, te.zCoord));
				errorTimer = 100;
			}
		}

		return canTravel;
	}

	private float getTotalThrust() {
		float thrust = 0;
		for(IPropulsion engine : engines) {
			thrust += engine.getThrust();
		}
		return thrust;
	}

	// Has the side effect of beginning engine burns
	private int getLeaveTime() {
		int leaveTime = 20;
		for(IPropulsion engine : engines) {
			int time = engine.startBurn();
			if(time > leaveTime) leaveTime = time;
		}
		return leaveTime;
	}

	// And this one will end engine burns
	private int getArriveTime() {
		int arriveTime = 20;
		for(IPropulsion engine : engines) {
			int time = engine.endBurn();
			if(time > arriveTime) arriveTime = time;
		}
		return arriveTime;
	}

	private int getTransferTime() {
		if(mainPort == null) return -1;

		int size = calculateSize();
		double distance = SolarSystem.calculateDistanceBetweenTwoBodies(mainPort.getWorldObj(), orbiting, target);
		float thrust = getTotalThrust();

		return calculateTransferTime(distance, size, thrust);
	}

	public static int calculateTransferTime(double distance, int size, float thrust) {
		return (int)(Math.log(1 + (distance * size / thrust * 100)) * 150);
	}

	public void setState(StationState state, int timeUntilNext) {
		this.state = state;
		stateTimer = 0;
		maxStateTimer = timeUntilNext;
	}

	public boolean recallPod(Destination destination) {
		if(!hasStation) return false;
		if(destination.body.getBody() != orbiting) return false;

		for(TileEntityOrbitalStation port : ports.values()) {
			EntityRideableRocket rocket = port.getDocked();

			if(rocket == null || !rocket.isReusable()) continue;

			// ensure the rocket has fuel before sending it off
			RocketState state = rocket.getState();
			if(state != RocketState.AWAITING && state != RocketState.LANDED) continue;

			// and make sure it doesn't have a rider!!
			if(rocket.riddenByEntity != null) continue;

			rocket.recallPod(destination);
			return true;
		}

		return false;
	}

	public static void addPropulsion(IPropulsion propulsion) {
		TileEntity te = propulsion.getTileEntity();
		OrbitalStation station = getStationFromPosition(te.xCoord, te.zCoord);
		station.engines.add(propulsion);
	}

	public static void removePropulsion(IPropulsion propulsion) {
		TileEntity te = propulsion.getTileEntity();
		OrbitalStation station = getStationFromPosition(te.xCoord, te.zCoord);
		station.engines.remove(propulsion);
	}

	public void addPort(TileEntityOrbitalStation port) {
		ports.put(new ThreeInts(port.xCoord, port.yCoord, port.zCoord), port);
		if(port.getBlockType() == ModBlocks.orbital_station) mainPort = port;
	}

	public void removePort(TileEntityOrbitalStation port) {
		ports.remove(new ThreeInts(port.xCoord, port.yCoord, port.zCoord));
	}

	public TileEntityOrbitalStation getPort() {
		if(ports.size() == 0) return null;

		// First, find any port that's available
		int index = 0;
		for(TileEntityOrbitalStation port : ports.values()) {
			if(!port.hasDocked && !port.isReserved) {
				portIndex = index;
				return port;
			}
			index++;
		}

		// Failing that, round robin on the occupied ports
		portIndex++;
		if(portIndex >= ports.size()) portIndex = 0;

		return ports.values().toArray(new TileEntityOrbitalStation[ports.size()])[portIndex];
	}

	public static TileEntityOrbitalStation getPort(int x, int z) {
		return getStationFromPosition(x, z).getPort();
	}

	// I can't stop pronouncing this as hors d'oeuvre
	private static final ForgeDirection[] horDir = new ForgeDirection[] { ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.EAST };

	// calculates the top down area of the station
	// super fucking fast but like, don't call it every frame
	public int calculateSize() {
		if(mainPort == null) return 0;
		World world = mainPort.getWorldObj();

		int minX, maxX;
		int minZ, maxZ;
		minX = maxX = mainPort.xCoord;
		minZ = maxZ = mainPort.zCoord;

		Stack<ThreeInts> stack = new Stack<ThreeInts>();
		stack.push(new ThreeInts(mainPort.xCoord, mainPort.yCoord, mainPort.zCoord));

		HashSet<ThreeInts> visited = new HashSet<ThreeInts>();

		while(!stack.isEmpty()) {
			ThreeInts pos = stack.pop();
			visited.add(pos);

			if(pos.x < minX) minX = pos.x;
			if(pos.x > maxX) maxX = pos.x;
			if(pos.z < minZ) minZ = pos.z;
			if(pos.z > maxZ) maxZ = pos.z;

			for(ForgeDirection dir : horDir) {
				ThreeInts nextPos = pos.getPositionAtOffset(dir);

				if(!visited.contains(nextPos) && isInStation(world, nextPos)) {
					stack.push(nextPos);
				}
			}
		}

		return (maxX - minX + 1) * (maxZ - minZ + 1);
	}

	private boolean isInStation(World world, ThreeInts pos) {
		if(world.getHeightValue(pos.x, pos.z) > 1) return true;
		return Math.abs(pos.x - mainPort.xCoord) < 5 && Math.abs(pos.z - mainPort.zCoord) < 5; // minimum station size
	}

	public double getUnscaledProgress(float partialTicks) {
		if(state == StationState.ORBIT) return 0;
		return MathHelper.clamp_double(((double)stateTimer + partialTicks) / (double)maxStateTimer, 0, 1);
	}

	public double getTransferProgress(float partialTicks) {
		if(state != StationState.TRANSFER) return 0;
		return easeInOutCirc(getUnscaledProgress(partialTicks));
	}

	private double easeInOutCirc(double t) {
		return t < 0.5
			? (1 - Math.sqrt(1 - Math.pow(2 * t, 3))) / 2
			: (Math.sqrt(1 - Math.pow(-2 * t + 2, 3)) + 1) / 2;
	}

	// Finds a space station for a given set of coordinates
	public static OrbitalStation getStationFromPosition(int x, int z) {
		SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get();
		OrbitalStation station = data.getStationFromPosition(x, z);

		// Fallback for when a station doesn't exist (should only occur when using debug wand!)
		if(station == null) {
			station = data.addStation(MathHelper.floor_float((float)x / STATION_SIZE), MathHelper.floor_float((float)z / STATION_SIZE), CelestialBody.getBody(0));
		}

		return station;
	}

	public static OrbitalStation getStation(int x, int z) {
		return getStationFromPosition(x * STATION_SIZE, z * STATION_SIZE);
	}

	public static boolean isKerbolAttempt(TileEntity te) {
		if(te == null) return false;
		if(te.getWorldObj() != null && te.getWorldObj().isRemote) {
			return OrbitalStation.clientStation != null && OrbitalStation.clientStation.lastAttemptedTarget == SolarSystem.kerbol;
		}

		OrbitalStation station = getStationFromPosition(te.xCoord, te.zCoord);
		return station != null && station.lastAttemptedTarget == SolarSystem.kerbol;
	}

	public static boolean isMinmusAttempt(TileEntity te) {
		if(te == null) return false;
		if(!SolarSystem.isMinmusDestroyed()) return false;
		if(te.getWorldObj() != null && te.getWorldObj().isRemote) {
			return OrbitalStation.clientStation != null
				&& OrbitalStation.clientStation.lastAttemptedTarget != null
				&& OrbitalStation.clientStation.lastAttemptedTarget.getEnum() == SolarSystem.Body.MINMUS;
		}

		OrbitalStation station = getStationFromPosition(te.xCoord, te.zCoord);
		return station != null
			&& station.lastAttemptedTarget != null
			&& station.lastAttemptedTarget.getEnum() == SolarSystem.Body.MINMUS;
	}

	public void serialize(ByteBuf buf) {
		buf.writeInt(orbiting.dimensionId);
		buf.writeInt(target.dimensionId);
		buf.writeInt(lastAttemptedTarget != null ? lastAttemptedTarget.dimensionId : orbiting.dimensionId);
		buf.writeInt(state.ordinal());
		buf.writeInt(stateTimer);
		buf.writeInt(maxStateTimer);
		buf.writeBoolean(hasEngines);
		buf.writeFloat(gravityMultiplier);

		BufferUtil.writeString(buf, name);

		buf.writeInt(errorsAt.size());
		for(ThreeInts error : errorsAt) {
			buf.writeInt(error.x);
			buf.writeInt(error.y);
			buf.writeInt(error.z);
		}
	}

	public static OrbitalStation deserialize(ByteBuf buf) {
		OrbitalStation station = new OrbitalStation(CelestialBody.getBody(buf.readInt()));
		station.target = CelestialBody.getBody(buf.readInt());
		station.lastAttemptedTarget = CelestialBody.getBody(buf.readInt());
		station.state = StationState.values()[buf.readInt()];
		station.stateTimer = buf.readInt();
		station.maxStateTimer = buf.readInt();
		station.hasEngines = buf.readBoolean();
		station.gravityMultiplier = buf.readFloat();

		station.name = BufferUtil.readString(buf);

		station.errorsAt = new ArrayList<ThreeInts>();
		int count = buf.readInt();
		for(int i = 0; i < count; i++) {
			int x = buf.readInt();
			int y = buf.readInt();
			int z = buf.readInt();

			station.errorsAt.add(new ThreeInts(x, y, z));
		}
		return station;
	}

	public static void spawn(World world, int x, int z) {
		int y = 127;
		if(world.getBlock(x, y, z) == ModBlocks.orbital_station) return;

		BlockOrbitalStation block = (BlockOrbitalStation) ModBlocks.orbital_station;

		BlockDummyable.safeRem = true;
		world.setBlock(x, y, z, block, 12, 3);
		block.fillSpace(world, x, y, z, ForgeDirection.NORTH, 0);
		BlockDummyable.safeRem = false;
	}

	// Mark the station as travelable
	public static void addStation(int x, int z, CelestialBody body) {
		SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get();
		OrbitalStation station = data.getStationFromPosition(x * STATION_SIZE, z * STATION_SIZE);

		if(station == null) {
			station = data.addStation(x, z, body);
		}

		station.orbiting = station.target = body;
		station.hasStation = true;
	}

	private static class StationBounds {
		private final int minX;
		private final int maxX;
		private final int minY;
		private final int maxY;
		private final int minZ;
		private final int maxZ;
		private final HashSet<Long> columns;

		private StationBounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ, HashSet<Long> columns) {
			this.minX = minX;
			this.maxX = maxX;
			this.minY = minY;
			this.maxY = maxY;
			this.minZ = minZ;
			this.maxZ = maxZ;
			this.columns = columns;
		}
	}

	private boolean relocateToKerbol(World world) {
		ensureMainPort(world);
		if(mainPort == null) return false;

		loadStationZoneChunks(world);
		StationBounds bounds = getStationBounds(world);
		if(bounds == null) return false;

		int targetDimensionId = SolarSystem.kerbol.dimensionId;
		WorldServer targetWorld = DimensionManager.getWorld(targetDimensionId);
		if(targetWorld == null) {
			DimensionManager.initDimension(targetDimensionId);
			targetWorld = DimensionManager.getWorld(targetDimensionId);
		}
		if(targetWorld == null) return false;

		loadChunks(targetWorld, bounds);

		int coreX = getCoreX();
		int coreZ = getCoreZ();
		int groundY = targetWorld.getTopSolidOrLiquidBlock(coreX, coreZ);
		int yOffset = Math.max(1, groundY - 1) - bounds.minY;

		copyStation(world, targetWorld, bounds, yOffset);
		teleportStationEntities(world, targetWorld, bounds, yOffset);
		clearStation(world, bounds);

		SolarSystemWorldSavedData.get(world).removeStationForce(this);
		return true;
	}

	private StationBounds getStationBounds(World world) {
		if(mainPort == null) return null;

		int minX = mainPort.xCoord;
		int maxX = mainPort.xCoord;
		int minZ = mainPort.zCoord;
		int maxZ = mainPort.zCoord;

		Stack<ThreeInts> stack = new Stack<ThreeInts>();
		stack.push(new ThreeInts(mainPort.xCoord, 0, mainPort.zCoord));

		HashSet<Long> visited = new HashSet<Long>();

		while(!stack.isEmpty()) {
			ThreeInts pos = stack.pop();
			long key = pack(pos.x, pos.z);
			if(visited.contains(key)) continue;
			visited.add(key);

			if(pos.x < minX) minX = pos.x;
			if(pos.x > maxX) maxX = pos.x;
			if(pos.z < minZ) minZ = pos.z;
			if(pos.z > maxZ) maxZ = pos.z;

			for(ForgeDirection dir : horDir) {
				ThreeInts nextPos = pos.getPositionAtOffset(dir);
				long nextKey = pack(nextPos.x, nextPos.z);
				if(!visited.contains(nextKey) && isInStation(world, nextPos)) {
					stack.push(nextPos);
				}
			}
		}

		if(visited.isEmpty()) return null;

		if(!engines.isEmpty()) {
			for(IPropulsion engine : engines) {
				if(engine == null) continue;
				TileEntity te = engine.getTileEntity();
				if(te == null) continue;

				int ex = te.xCoord;
				int ez = te.zCoord;
				for(int x = ex - PROPULSION_BOUNDS_RADIUS; x <= ex + PROPULSION_BOUNDS_RADIUS; x++) {
					for(int z = ez - PROPULSION_BOUNDS_RADIUS; z <= ez + PROPULSION_BOUNDS_RADIUS; z++) {
						long key = pack(x, z);
						if(visited.add(key)) {
							if(x < minX) minX = x;
							if(x > maxX) maxX = x;
							if(z < minZ) minZ = z;
							if(z > maxZ) maxZ = z;
						}
					}
				}
			}
		}

		int minY = Integer.MAX_VALUE;
		int maxY = Integer.MIN_VALUE;
		int height = world.getHeight();

		for(long column : visited) {
			int x = unpackX(column);
			int z = unpackZ(column);
			for(int y = 0; y < height; y++) {
				Block block = world.getBlock(x, y, z);
				if(block == Blocks.air) continue;
				if(y < minY) minY = y;
				if(y > maxY) maxY = y;
			}
		}

		if(minY == Integer.MAX_VALUE || maxY == Integer.MIN_VALUE) return null;

		return new StationBounds(minX, maxX, minY, maxY, minZ, maxZ, visited);
	}

	private void copyStation(World sourceWorld, World targetWorld, StationBounds bounds, int yOffset) {
		BlockDummyable.safeRem = true;
		for(long column : bounds.columns) {
			int x = unpackX(column);
			int z = unpackZ(column);
			for(int y = bounds.minY; y <= bounds.maxY; y++) {
				Block block = sourceWorld.getBlock(x, y, z);
				if(block == Blocks.air) continue;

				int meta = sourceWorld.getBlockMetadata(x, y, z);
				int targetY = y + yOffset;
				targetWorld.setBlock(x, targetY, z, block, meta, 2);

				TileEntity sourceTe = sourceWorld.getTileEntity(x, y, z);
				if(sourceTe != null) {
					NBTTagCompound nbt = new NBTTagCompound();
					sourceTe.writeToNBT(nbt);
					nbt.setInteger("x", x);
					nbt.setInteger("y", targetY);
					nbt.setInteger("z", z);

					TileEntity targetTe = targetWorld.getTileEntity(x, targetY, z);
					if(targetTe != null) {
						targetTe.readFromNBT(nbt);
						targetTe.markDirty();
					} else {
						TileEntity created = TileEntity.createAndLoadEntity(nbt);
						if(created != null) {
							targetWorld.setTileEntity(x, targetY, z, created);
						}
					}
				}
			}
		}
		BlockDummyable.safeRem = false;
	}

	private void teleportStationEntities(World sourceWorld, World targetWorld, StationBounds bounds, int yOffset) {
		AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
			bounds.minX, bounds.minY, bounds.minZ,
			bounds.maxX + 1, bounds.maxY + 1, bounds.maxZ + 1
		);

		List<EntityPlayer> players = sourceWorld.getEntitiesWithinAABB(EntityPlayer.class, box);
		for(EntityPlayer player : players) {
			CelestialTeleporter.teleport(player, targetWorld.provider.dimensionId, player.posX, player.posY + yOffset, player.posZ, false);
		}

		List<EntityRideableRocket> rockets = sourceWorld.getEntitiesWithinAABB(EntityRideableRocket.class, box);
		for(EntityRideableRocket rocket : rockets) {
			CelestialTeleporter.teleport(rocket, targetWorld.provider.dimensionId, rocket.posX, rocket.posY + yOffset, rocket.posZ, false);
		}
	}

	private void clearStation(World world, StationBounds bounds) {
		BlockDummyable.safeRem = true;
		for(long column : bounds.columns) {
			int x = unpackX(column);
			int z = unpackZ(column);
			for(int y = bounds.minY; y <= bounds.maxY; y++) {
				if(world.getBlock(x, y, z) != Blocks.air) {
					world.setBlock(x, y, z, Blocks.air, 0, 2);
				}
			}
		}
		BlockDummyable.safeRem = false;
	}

	private void loadChunks(World world, StationBounds bounds) {
		for(int x = bounds.minX; x <= bounds.maxX; x += 16) {
			for(int z = bounds.minZ; z <= bounds.maxZ; z += 16) {
				world.getChunkFromBlockCoords(x, z);
			}
		}
	}

	private void loadStationZoneChunks(World world) {
		int minX = dX * STATION_SIZE;
		int minZ = dZ * STATION_SIZE;
		int maxX = minX + STATION_SIZE - 1;
		int maxZ = minZ + STATION_SIZE - 1;

		for(int x = minX; x <= maxX; x += 16) {
			for(int z = minZ; z <= maxZ; z += 16) {
				world.getChunkFromBlockCoords(x, z);
			}
		}
	}

	private void ensureMainPort(World world) {
		if(mainPort != null) return;

		int coreX = getCoreX();
		int coreZ = getCoreZ();
		world.getChunkFromBlockCoords(coreX, coreZ);

		TileEntity te = world.getTileEntity(coreX, STATION_CORE_Y, coreZ);
		if(te instanceof TileEntityOrbitalStation) {
			mainPort = (TileEntityOrbitalStation) te;
		}
	}

	private int getCoreX() {
		return dX * STATION_SIZE + (STATION_SIZE / 2);
	}

	private int getCoreZ() {
		return dZ * STATION_SIZE + (STATION_SIZE / 2);
	}

	public boolean isCoreChunkLoaded(World world) {
		int chunkX = getCoreX() >> 4;
		int chunkZ = getCoreZ() >> 4;
		return world.getChunkProvider().chunkExists(chunkX, chunkZ);
	}

	private long pack(int x, int z) {
		return (((long) x) << 32) ^ (z & 0xffffffffL);
	}

	private int unpackX(long packed) {
		return (int) (packed >> 32);
	}

	private int unpackZ(long packed) {
		return (int) packed;
	}

}
