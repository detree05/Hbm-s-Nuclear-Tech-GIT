package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockOreFluid;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.util.fauxpointtwelve.DirPos;

import api.hbm.energymk2.IEnergyReceiverMK2;
import api.hbm.fluid.IFluidStandardTransceiver;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import com.hbm.tileentity.TileEntityLoadedBase;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityWaterExtractionPlant extends TileEntityLoadedBase implements IEnergyReceiverMK2, IFluidStandardTransceiver {

	private static final long MAX_POWER = 10_000L;
	private static final int POWER_REQ = 3_000;
	private static final int DELAY = 20;
	private static final int TANK_SIZE = 128_000;

	private long power;
	private final FluidTank water = new FluidTank(Fluids.SUBSURFACE_WATER, TANK_SIZE);

	@Override
	public void updateEntity() {
		if(worldObj.isRemote) {
			return;
		}

		if(worldObj.getTotalWorldTime() % 20 == 0) {
			for(DirPos pos : getConPos()) {
				this.trySubscribe(worldObj, pos.getX(), pos.getY(), pos.getZ(), pos.getDir());
			}
		}

		for(DirPos pos : getConPos()) {
			if(water.getFill() > 0) {
				this.sendFluid(water, worldObj, pos.getX(), pos.getY(), pos.getZ(), pos.getDir());
			}
		}

		if(power >= POWER_REQ && water.getFill() < water.getMaxFill() && worldObj.getTotalWorldTime() % DELAY == 0) {
			if(tryExtract()) {
				power -= POWER_REQ;
			}
		}

		networkPackNT(25);
	}

	private boolean tryExtract() {
		int bedrockY = ensurePipeToBedrock();
		if(bedrockY < 0) {
			return false;
		}

		Block block = worldObj.getBlock(xCoord, bedrockY, zCoord);
		if(block != ModBlocks.ore_bedrock_subsurface_water) {
			return false;
		}

		BlockOreFluid ore = (BlockOreFluid) block;
		int meta = worldObj.getBlockMetadata(xCoord, bedrockY, zCoord);
		water.setTankType(ore.getPrimaryFluid(meta));
		int add = ore.getPrimaryFluidAmount(meta) * 10;
		water.setFill(Math.min(water.getFill() + add, water.getMaxFill()));
		return true;
	}

	private int ensurePipeToBedrock() {
		for(int y = yCoord - 1; y >= 0; y--) {
			Block block = worldObj.getBlock(xCoord, y, zCoord);
			if(block == ModBlocks.subsurface_water_pipe) {
				continue;
			}
			if(block == ModBlocks.ore_bedrock_subsurface_water) {
				return y;
			}
			if(block.getExplosionResistance(null) < 1000) {
				worldObj.setBlock(xCoord, y, zCoord, ModBlocks.subsurface_water_pipe);
			}
			return -1;
		}

		return -1;
	}

	private DirPos[] getConPos() {
		return new DirPos[] {
				new DirPos(xCoord + 1, yCoord, zCoord, ForgeDirection.EAST),
				new DirPos(xCoord - 1, yCoord, zCoord, ForgeDirection.WEST),
				new DirPos(xCoord, yCoord, zCoord + 1, ForgeDirection.SOUTH),
				new DirPos(xCoord, yCoord, zCoord - 1, ForgeDirection.NORTH)
		};
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.power = nbt.getLong("power");
		this.water.readFromNBT(nbt, "water");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setLong("power", power);
		this.water.writeToNBT(nbt, "water");
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeLong(this.power);
		this.water.serialize(buf);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		this.power = buf.readLong();
		this.water.deserialize(buf);
	}

	@Override
	public long getPower() {
		return power;
	}

	@Override
	public long getMaxPower() {
		return MAX_POWER;
	}

	@Override
	public void setPower(long power) {
		this.power = power;
	}

	@Override
	public FluidTank[] getSendingTanks() {
		return new FluidTank[] { water };
	}

	@Override
	public FluidTank[] getReceivingTanks() {
		return FluidTank.EMPTY_ARRAY;
	}

	@Override
	public FluidTank[] getAllTanks() {
		return new FluidTank[] { water };
	}
}
