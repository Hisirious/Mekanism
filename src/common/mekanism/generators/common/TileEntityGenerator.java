package mekanism.generators.common;

import buildcraft.api.power.IPowerProvider;
import buildcraft.api.power.IPowerReceptor;
import buildcraft.api.power.PowerFramework;
import buildcraft.api.power.PowerProvider;

import com.google.common.io.ByteArrayDataInput;

import dan200.computer.api.IComputerAccess;
import dan200.computer.api.IPeripheral;

import ic2.api.Direction;
import ic2.api.ElectricItem;
import ic2.api.EnergyNet;
import ic2.api.IElectricItem;
import ic2.api.IEnergySource;
import ic2.api.IEnergyStorage;
import universalelectricity.core.UniversalElectricity;
import universalelectricity.core.electricity.ElectricInfo;
import universalelectricity.core.electricity.ElectricityManager;
import universalelectricity.core.implement.IConductor;
import universalelectricity.core.implement.IElectricityReceiver;
import universalelectricity.core.implement.IJouleStorage;
import universalelectricity.core.vector.Vector3;
import universalelectricity.prefab.tile.TileEntityConductor;
import mekanism.common.Mekanism;
import mekanism.common.TileEntityElectricBlock;
import net.minecraft.src.*;
import net.minecraftforge.common.ForgeDirection;

public abstract class TileEntityGenerator extends TileEntityElectricBlock implements IEnergySource, IEnergyStorage, IPowerReceptor, IJouleStorage, IElectricityReceiver, IPeripheral
{
	/** Output per tick this generator can transfer. */
	public int output;
	
	/** BuildCraft power provider. */
	public IPowerProvider powerProvider;
	
	/** Whether or not this block is in it's active state. */
	public boolean isActive;
	
	/** The previous active state for this block. */
	public boolean prevActive;
	
	/**
	 * Generator -- a block that produces energy. It has a certain amount of fuel it can store as well as an output rate.
	 * @param name - full name of this generator
	 * @param maxEnergy - how much energy this generator can store
	 * @param maxFuel - how much fuel this generator can store
	 */
	public TileEntityGenerator(String name, int maxEnergy, int out)
	{
		super(name, maxEnergy);
		
		output = out;
		isActive = false;
		
		if(PowerFramework.currentFramework != null)
		{
			powerProvider = PowerFramework.currentFramework.createPowerProvider();
			powerProvider.configure(0, 2, 2000, 1, (int)(MAX_ELECTRICITY*UniversalElectricity.TO_BC_RATIO));
		}
	}
	
	@Override
	public void onUpdate()
	{	
		if(electricityStored > 0)
		{
			TileEntity tileEntity = Vector3.getTileEntityFromSide(worldObj, Vector3.get(this), ForgeDirection.getOrientation(facing));
			
			if(Mekanism.hooks.IC2Loaded)
			{
				if(electricityStored >= output)
				{
					setJoules(electricityStored - (output - EnergyNet.getForWorld(worldObj).emitEnergyFrom(this, output))*UniversalElectricity.IC2_RATIO);
				}
			}
			
			if(tileEntity != null)
			{
				if(isPowerReceptor(tileEntity))
				{
					IPowerReceptor receptor = (IPowerReceptor)tileEntity;
	            	double electricityNeeded = Math.min(receptor.getPowerProvider().getMinEnergyReceived(), receptor.getPowerProvider().getMaxEnergyReceived())*UniversalElectricity.BC3_RATIO;
	            	float transferEnergy = (float)Math.max(Math.min(Math.min(electricityNeeded, electricityStored), 80000), 0);
	            	receptor.getPowerProvider().receiveEnergy((float)(transferEnergy*UniversalElectricity.TO_BC_RATIO), ForgeDirection.getOrientation(facing).getOpposite());
	            	setJoules(electricityStored - (int)transferEnergy);
				}
				else if(tileEntity instanceof TileEntityConductor)
				{
					double joulesNeeded = ElectricityManager.instance.getElectricityRequired(((IConductor) tileEntity).getNetwork());
					double transferAmps = Math.max(Math.min(Math.min(ElectricInfo.getAmps(joulesNeeded, getVoltage()), ElectricInfo.getAmps(electricityStored*UniversalElectricity.IC2_RATIO, getVoltage())), 80), 0);
					if (!worldObj.isRemote)
					{
						ElectricityManager.instance.produceElectricity(this, (IConductor)tileEntity, transferAmps, getVoltage());
					}
					setJoules(electricityStored - (int)(ElectricInfo.getJoules(transferAmps, getVoltage())*UniversalElectricity.TO_IC2_RATIO));
				}
			}
		}
	}
	
	/**
	 * Gets the boost this generator can receive in it's current location.
	 * @return environmental boost
	 */
	public abstract int getEnvironmentBoost();
	
	/**
	 * Whether or not this generator can operate.
	 * @return if the generator can operate
	 */
	public abstract boolean canOperate();
	
	/**
	 * Whether or not the declared Tile Entity is an instance of a BuildCraft power receptor.
	 * @param tileEntity - tile entity to check
	 * @return if the tile entity is a power receptor
	 */
	public boolean isPowerReceptor(TileEntity tileEntity)
	{
		if(tileEntity instanceof IPowerReceptor) 
		{
			IPowerReceptor receptor = (IPowerReceptor)tileEntity;
			IPowerProvider provider = receptor.getPowerProvider();
			return provider != null && provider.getClass().getSuperclass().equals(PowerProvider.class);
		}
		return false;
	}
	
	/**
	 * Gets the scaled energy level for the GUI.
	 * @param i - multiplier
	 * @return
	 */
	public int getScaledEnergyLevel(int i)
	{
		return (int)(electricityStored*i / MAX_ELECTRICITY);
	}
	
	/**
	 * Sets this block's active state to a new value.
	 * @param active - new active state
	 */
    public void setActive(boolean active)
    {
    	isActive = active;
    	
    	if(prevActive != active)
    	{
    		sendPacket();
    	}
    	
    	prevActive = active;
    }
	
	@Override
	public double getVoltage()
	{
		return 120;
	}
	
	@Override
	public void setJoules(double joules, Object... data)
	{
		electricityStored = Math.max(Math.min(joules, getMaxJoules()), 0);
	}
	
	@Override
	public String getType() 
	{
		return getInvName();
	}

	@Override
	public boolean canAttachToSide(int side) 
	{
		return true;
	}

	@Override
	public void attach(IComputerAccess computer, String computerSide) {}

	@Override
	public void detach(IComputerAccess computer) {}
	
	@Override
	public void setPowerProvider(IPowerProvider provider)
	{
		powerProvider = provider;
	}

	@Override
	public IPowerProvider getPowerProvider() 
	{
		return powerProvider;
	}

	@Override
	public void doWork() {}

	@Override
	public int powerRequest() 
	{
		return getPowerProvider().getMaxEnergyReceived();
	}
	
	@Override
	public double getMaxJoules(Object... data) 
	{
		return MAX_ELECTRICITY*UniversalElectricity.IC2_RATIO;
	}
	
	@Override
	public double getJoules(Object... data) 
	{
		return electricityStored*UniversalElectricity.IC2_RATIO;
	}
	
	@Override
	public int getMaxEnergyOutput()
	{
		return output;
	}
	
	@Override
	public boolean emitsEnergyTo(TileEntity receiver, Direction direction)
	{
		return direction.toForgeDirection() == ForgeDirection.getOrientation(facing);
	}
	
	@Override
	public int getStored() 
	{
		return (int)(electricityStored*UniversalElectricity.IC2_RATIO);
	}

	@Override
	public int getCapacity() 
	{
		return (int)(MAX_ELECTRICITY*UniversalElectricity.IC2_RATIO);
	}

	@Override
	public int getOutput() 
	{
		return output;
	}
	
	@Override
	public boolean canConnect(ForgeDirection side) 
	{
		return side == ForgeDirection.getOrientation(facing);
	}
	
	@Override
	public void onReceive(TileEntity sender, double amps, double voltage, ForgeDirection side) {}

	@Override
	public double wattRequest() 
	{
		return 0;
	}

	@Override
	public boolean canReceiveFromSide(ForgeDirection side) 
	{
		return false;
	}
	
	@Override
    public void readFromNBT(NBTTagCompound nbtTags)
    {
        super.readFromNBT(nbtTags);

        isActive = nbtTags.getBoolean("isActive");
    }

	@Override
    public void writeToNBT(NBTTagCompound nbtTags)
    {
        super.writeToNBT(nbtTags);
        
        nbtTags.setBoolean("isActive", isActive);
    }
}