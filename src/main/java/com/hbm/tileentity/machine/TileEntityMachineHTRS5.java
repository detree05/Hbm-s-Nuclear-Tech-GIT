package com.hbm.tileentity.machine;

import java.util.List;

import com.hbm.blocks.BlockDummyable;
import com.hbm.dim.CelestialBody;
import com.hbm.dim.SolarSystem;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.inventory.fluid.trait.FT_Rocket;
import com.hbm.items.ModItems;
import com.hbm.main.MainRegistry;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.i18n.I18nUtil;
import com.hbm.util.fauxpointtwelve.DirPos;

import api.hbm.energymk2.IEnergyReceiverMK2;
import api.hbm.fluid.IFluidStandardReceiver;
import api.hbm.tile.IPropulsion;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityMachineHTRS5 extends TileEntityMachineBase implements IPropulsion, IFluidStandardReceiver, IEnergyReceiverMK2 {

	public FluidTank[] tanks;

	public long power;
	public static long maxPower = 1_000_000_000;

	private static final int EXHAUST_OFFSET = 5;
	private static final int CONNECTOR_OFFSET = 4;
	private static final float WINDUP_TICKS = 90F;

	private boolean isOn;
	private boolean wasCatalyst;
	private float speed;
	public double lastTime;
	public double time;
	private float soundtime;
	private float burnSoundtime;
	private AudioWrapper magnetAudio;
	private AudioWrapper burnAudio;

	private boolean hasRegistered;
	private boolean hasCatalystClient;
	private float spinSpeed;
	private float spinAngle;
	private float prevSpinSpeed;
	private float prevSpinAngle;

	private int fuelCost;

	public TileEntityMachineHTRS5() {
		super(1);
		tanks = new FluidTank[1];
		tanks[0] = new FluidTank(Fluids.DMAT, 1_024_000);
	}

	@Override
	public void updateEntity() {
		if(!CelestialBody.inOrbit(worldObj)) return;

		if(!worldObj.isRemote) {
			if(slots.length > 0 && slots[0] != null) {
				if(slots[0].getItem() != ModItems.black_hole) {
					slots[0] = null;
					markDirty();
				} else if(slots[0].stackSize > 1) {
					slots[0].stackSize = 1;
					markDirty();
				}
			}
			hasCatalystClient = hasCatalyst();
			if(!hasRegistered) {
				if(isFacingPrograde()) registerPropulsion();
				hasRegistered = true;
				isOn = false;
			}

			for(DirPos pos : getConPos()) {
				for(FluidTank tank : tanks) {
					trySubscribe(tank.getTankType(), worldObj, pos.getX(), pos.getY(), pos.getZ(), pos.getDir());
				}
			}

			if(isOn) {
				burnSoundtime++;

				if(burnSoundtime == 1) {
					this.worldObj.playSoundEffect(this.xCoord, this.yCoord, this.zCoord, "hbm:misc.lpwstart", 1.5F, 1F);
				} else if(burnSoundtime > 20) {
					burnSoundtime = 20;
				}
			} else {
				burnSoundtime--;

				if(burnSoundtime == 19) {
					this.worldObj.playSoundEffect(this.xCoord, this.yCoord, this.zCoord, "hbm:misc.lpwstop", 2.0F, 1F);
				} else if(burnSoundtime <= 0) {
					burnSoundtime = 0;
				}
			}

			boolean hasCatalyst = hasCatalyst();
			if(hasCatalyst) {
				if(!wasCatalyst) {
					this.worldObj.playSoundEffect(this.xCoord, this.yCoord, this.zCoord, "hbm:misc.htrs5start", 1.5F, 1F);
				}
				soundtime = Math.min(WINDUP_TICKS, soundtime + 1F);
			} else {
				if(wasCatalyst) {
					this.worldObj.playSoundEffect(this.xCoord, this.yCoord, this.zCoord, "hbm:misc.htrs5stop", 2.0F, 1F);
				}
				soundtime = Math.max(0F, soundtime - 1F);
			}
			wasCatalyst = hasCatalyst;

			networkPackNT(250);
		} else {
			prevSpinSpeed = spinSpeed;
			prevSpinAngle = spinAngle;

			float targetSpeed = hasCatalystClient ? 1F : 0F;
			if(spinSpeed < targetSpeed) {
				spinSpeed = Math.min(targetSpeed, spinSpeed + 0.01F);
			} else if(spinSpeed > targetSpeed) {
				spinSpeed = Math.max(targetSpeed, spinSpeed - 0.01F);
			}

			if(targetSpeed == 0F) {
				spinSpeed *= 0.98F;
			}

			spinAngle += spinSpeed * 4F;
			if(targetSpeed == 0F && spinSpeed < 0.01F) {
				// Ease back to default angle smoothly when stopped.
				float wrap = spinAngle % 360F;
				if(wrap > 180F) wrap -= 360F;
				spinAngle -= wrap * 0.05F;
				if(Math.abs(wrap) < 0.1F) {
					spinAngle = 0F;
				}
			}

			if(isOn) {
				speed += 0.01D;
				if(speed > 1) speed = 1;

				if(soundtime >= WINDUP_TICKS) {
					ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset).getRotation(ForgeDirection.UP);

					NBTTagCompound data = new NBTTagCompound();
					ForgeDirection right = dir.getRotation(ForgeDirection.UP).getOpposite();
					double lateral = 0.35D;
					data.setDouble("posX", xCoord + 0.5D + dir.offsetX * EXHAUST_OFFSET + right.offsetX * lateral);
					data.setDouble("posY", yCoord + 0.5D);
					data.setDouble("posZ", zCoord + 0.5D + dir.offsetZ * EXHAUST_OFFSET + right.offsetZ * lateral);
					data.setString("type", "htrs5Contrail");
					data.setFloat("scale", 3);
					data.setDouble("moX", dir.offsetX * 10);
					data.setDouble("moY", 0);
					data.setDouble("moZ", dir.offsetZ * 10);
					data.setInteger("maxAge", 40 + worldObj.rand.nextInt(40));
					MainRegistry.proxy.effectNT(data);
				}
			} else {
				speed -= 0.01D;
				if(speed < 0) speed = 0;
			}

			if(isOn && burnSoundtime > 18) {
				if(burnAudio == null) {
					burnAudio = createAudioLoop();
					burnAudio.startSound();
				} else if(!burnAudio.isPlaying()) {
					burnAudio = rebootAudio(burnAudio);
				}

				burnAudio.updateVolume(getVolume(1F));
				burnAudio.keepAlive();
			} else if(burnAudio != null) {
				burnAudio.stopSound();
				burnAudio = null;
			}

			if(hasCatalystClient && soundtime >= WINDUP_TICKS) {
				if(magnetAudio == null) {
					magnetAudio = createMagnetLoop();
					magnetAudio.startSound();
				} else if(!magnetAudio.isPlaying()) {
					magnetAudio = rebootAudio(magnetAudio);
				}

				magnetAudio.updateVolume(getVolume(1F));
				magnetAudio.keepAlive();
			} else if(magnetAudio != null) {
				magnetAudio.stopSound();
				magnetAudio = null;
			}
		}

		lastTime = time;
		time += speed;
	}

	@Override
	public AudioWrapper createAudioLoop() {
		return MainRegistry.proxy.getLoopedSound("hbm:misc.htrloop", xCoord, yCoord, zCoord, 0.25F, 27.5F, 1.0F, 20);
	}

	public AudioWrapper createMagnetLoop() {
		return MainRegistry.proxy.getLoopedSound("hbm:misc.htrs5loop", xCoord, yCoord, zCoord, 0.25F, 27.5F, 1.0F, 20);
	}

	@Override
	public void invalidate() {
		super.invalidate();

		if(hasRegistered) {
			unregisterPropulsion();
			hasRegistered = false;
		}

		if(magnetAudio != null) {
			magnetAudio.stopSound();
			magnetAudio = null;
		}

		if(burnAudio != null) {
			burnAudio.stopSound();
			burnAudio = null;
		}
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();

		if(hasRegistered) {
			unregisterPropulsion();
			hasRegistered = false;
		}

		if(magnetAudio != null) {
			magnetAudio.stopSound();
			magnetAudio = null;
		}

		if(burnAudio != null) {
			burnAudio.stopSound();
			burnAudio = null;
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeBoolean(isOn);
		buf.writeFloat(soundtime);
		buf.writeFloat(burnSoundtime);
		buf.writeInt(fuelCost);
		buf.writeLong(power);
		buf.writeBoolean(hasCatalystClient);
		for(int i = 0; i < tanks.length; i++) tanks[i].serialize(buf);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		isOn = buf.readBoolean();
		soundtime = buf.readFloat();
		burnSoundtime = buf.readFloat();
		fuelCost = buf.readInt();
		power = buf.readLong();
		hasCatalystClient = buf.readBoolean();
		for(int i = 0; i < tanks.length; i++) tanks[i].deserialize(buf);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setBoolean("on", isOn);
		nbt.setLong("power", power);
		for(int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(nbt, "t" + i);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		isOn = nbt.getBoolean("on");
		power = nbt.getLong("power");
		for(int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(nbt, "t" + i);
		if(tanks[0].getTankType() != Fluids.DMAT) {
			tanks[0].setTankType(Fluids.DMAT);
		}
		if(slots.length > 0 && slots[0] != null) {
			if(slots[0].getItem() != ModItems.black_hole) {
				slots[0] = null;
			} else if(slots[0].stackSize > 1) {
				slots[0].stackSize = 1;
			}
		}
	}

	public boolean isFacingPrograde() {
		return ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset) == ForgeDirection.SOUTH;
	}

	AxisAlignedBB bb = null;

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		if(bb == null) bb = AxisAlignedBB.getBoundingBox(xCoord - 4, yCoord - 1, zCoord - 4, xCoord + 5, yCoord + 2, zCoord + 5);
		return bb;
	}

	private DirPos[] getConPos() {
		ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset);
		ForgeDirection startDir = dir.getRotation(ForgeDirection.UP).getOpposite();
		ForgeDirection rot = startDir.getRotation(ForgeDirection.UP);

		int baseX = xCoord + startDir.offsetX * CONNECTOR_OFFSET;
		int baseZ = zCoord + startDir.offsetZ * CONNECTOR_OFFSET;
		int nearX = baseX - startDir.offsetX;
		int nearZ = baseZ - startDir.offsetZ;

		int[] conX = new int[] {
			baseX,
			baseX,
			baseX,
			baseX,
			baseX - rot.offsetX,
			nearX + rot.offsetX,
			nearX,
			nearX,
			nearX - rot.offsetX
		};
		int yBottom = yCoord - 3;
		int yMid = yCoord;
		int yTop = yCoord + 1;
		int[] conY = new int[] {
			yMid,
			yMid,
			yTop,
			yBottom,
			yTop,
			yMid,
			yTop,
			yBottom,
			yTop
		};
		int[] conZ = new int[] {
			baseZ,
			baseZ + rot.offsetZ,
			baseZ,
			baseZ,
			baseZ - rot.offsetZ,
			nearZ + rot.offsetZ,
			nearZ,
			nearZ,
			nearZ - rot.offsetZ
		};
		ForgeDirection[] facing = new ForgeDirection[] {
			startDir,
			rot,
			ForgeDirection.UP,
			ForgeDirection.DOWN,
			rot.getOpposite(),
			rot,
			ForgeDirection.UP,
			ForgeDirection.DOWN,
			rot.getOpposite()
		};

		DirPos[] conPos = new DirPos[conX.length];
		for(int i = 0; i < conPos.length; i++) {
			int pipeX = conX[i] + facing[i].offsetX;
			int pipeY = conY[i] + facing[i].offsetY;
			int pipeZ = conZ[i] + facing[i].offsetZ;
			conPos[i] = new DirPos(pipeX, pipeY, pipeZ, facing[i].getOpposite());
		}

		return conPos;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}

	@Override
	public TileEntity getTileEntity() {
		return this;
	}

	@Override
	public boolean canPerformBurn(int shipMass, double deltaV) {
		if(!hasCatalyst()) return false;

		FT_Rocket trait = tanks[0].getTankType().getTrait(FT_Rocket.class);
		int isp = trait != null ? trait.getISP() : 300;

		fuelCost = SolarSystem.getFuelCost(deltaV, shipMass, isp);

		for(FluidTank tank : tanks) {
			if(tank.getFill() < fuelCost) return false;
		}

		return true;
	}

	@Override
	public void addErrors(List<String> errors) {
		if(!hasCatalyst()) {
			errors.add(EnumChatFormatting.RED + I18nUtil.resolveKey(getBlockType().getUnlocalizedName() + ".name") + " - Missing Miniature Black Hole");
		}

		for(FluidTank tank : tanks) {
			if(tank.getFill() < fuelCost) {
				errors.add(EnumChatFormatting.RED + I18nUtil.resolveKey(getBlockType().getUnlocalizedName() + ".name") + " - Insufficient fuel: needs " + fuelCost + "mB");
			}
		}
	}

	@Override
	public float getThrust() {
		return 1_600_000_000.0F; // F1 thrust
	}

	@Override
	public int startBurn() {
		isOn = true;
		for(FluidTank tank : tanks) {
			tank.setFill(tank.getFill() - fuelCost);
		}
		return 180;
	}

	@Override
	public int endBurn() {
		isOn = false;
		return 180; // Cooldown
	}

	@Override
	public String getName() {
		return "container.htrs5";
	}

	@Override
	public int getInventoryStackLimit() {
		return 1;
	}

	@Override
	public void setInventorySlotContents(int slot, net.minecraft.item.ItemStack stack) {
		if(slot == 0 && stack != null) {
			if(stack.getItem() != ModItems.black_hole) {
				return;
			}
			stack = stack.copy();
			stack.stackSize = 1;
		}
		super.setInventorySlotContents(slot, stack);
	}

	@Override
	public boolean isItemValidForSlot(int slot, net.minecraft.item.ItemStack stack) {
		return slot == 0 && stack != null && stack.getItem() == ModItems.black_hole;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		return new int[] { 0 };
	}

	@Override
	public boolean canExtractItem(int slot, net.minecraft.item.ItemStack stack, int side) {
		return slot == 0;
	}

	@Override
	public FluidTank[] getAllTanks() {
		return tanks;
	}

	@Override
	public FluidTank[] getReceivingTanks() {
		return tanks;
	}

	@Override
	public long getPower() {
		return power;
	}

	@Override
	public void setPower(long power) {
		this.power = power;
	}

	@Override
	public long getMaxPower() {
		return maxPower;
	}

	public boolean hasCatalyst() {
		if(worldObj != null && worldObj.isRemote) {
			return hasCatalystClient;
		}
		return slots.length > 0 && slots[0] != null && slots[0].getItem() == ModItems.black_hole;
	}

	@SideOnly(Side.CLIENT)
	public float getSpinAngle(float interp) {
		float delta = MathHelper.wrapAngleTo180_float(spinAngle - prevSpinAngle);
		return prevSpinAngle + delta * interp;
	}
}
