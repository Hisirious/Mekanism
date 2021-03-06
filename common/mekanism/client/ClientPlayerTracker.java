package mekanism.client;

import mekanism.common.Mekanism;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import cpw.mods.fml.common.IPlayerTracker;

public class ClientPlayerTracker implements IPlayerTracker
{
	@Override
	public void onPlayerLogin(EntityPlayer player) {}

	@Override
	public void onPlayerLogout(EntityPlayer player)
	{
		Mekanism.jetpackOn.remove(player);
		
		if(player.username.equals(Minecraft.getMinecraft().thePlayer.username))
		{
			if(Mekanism.voiceServerEnabled)
			{
				if(MekanismClient.voiceClient != null)
				{
					MekanismClient.voiceClient.disconnect();
					MekanismClient.voiceClient = null;
				}
			}
			
			ClientTickHandler.tickingSet.clear();
			Mekanism.jetpackOn.clear();
			Mekanism.gasmaskOn.clear();
			Mekanism.proxy.unloadSoundHandler();
		}
	}

	@Override
	public void onPlayerChangedDimension(EntityPlayer player)
	{
		Mekanism.jetpackOn.remove(player);
	}

	@Override
	public void onPlayerRespawn(EntityPlayer player) {}
}
