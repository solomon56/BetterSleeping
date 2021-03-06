package cz.ondraster.bettersleeping.logic;

import cpw.mods.fml.common.gameevent.TickEvent;
import cz.ondraster.bettersleeping.Config;
import cz.ondraster.bettersleeping.api.BetterSleepingAPI;
import cz.ondraster.bettersleeping.api.PlayerData;
import cz.ondraster.bettersleeping.api.PlayerDebuff;
import cz.ondraster.bettersleeping.api.WorldSleepEvent;
import cz.ondraster.bettersleeping.client.gui.SleepOverlay;
import cz.ondraster.bettersleeping.network.MessageUpdateTiredness;
import cz.ondraster.bettersleeping.network.Network;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;

public class DebuffLogic {

   public static void updateClientIfNeeded(EntityPlayer player, PlayerData data) {
      if ((double) (Math.abs(data.getSleepCounter() - data.lastUpdate)) / Config.maximumSleepCounter >
            1.0d / SleepOverlay.MAX_OFFSET && player instanceof EntityPlayerMP) {
         Network.networkChannel
               .sendTo(new MessageUpdateTiredness(data.getSleepCounter()), (EntityPlayerMP) player);
         data.lastUpdate = data.getSleepCounter();
      }
   }

   public static void checkForDebuffs(TickEvent.PlayerTickEvent event, PlayerData data) {
      List<PlayerDebuff> debuffs = BetterSleepingAPI.getDebuffs();
      for (PlayerDebuff debuff : debuffs) {
         if (debuff.enable && data.getSleepCounter() < debuff.tiredLevel) {
            double percentTired = (debuff.tiredLevel - data.getSleepCounter()) / (double) (debuff.tiredLevel);
            int scale = (int) Math.ceil(percentTired * debuff.maxScale) - 1;
            event.player.addPotionEffect(
                  new PotionEffect(debuff.potion.getId(), Config.POTION_DURATION * 2, scale));
         }
      }

      // should fall asleep on the ground
      if (data.getSleepCounter() == 0 && !event.player.isPlayerSleeping() && Config.sleepOnGround) {
         boolean result = MinecraftForge.EVENT_BUS.post(new WorldSleepEvent.SleepOnGround(event.player));

         if (!result) {
            EntityPlayer.EnumStatus sleepResult = event.player.sleepInBedAt((int) event.player.posX, (int) event.player.posY, (int) event
                  .player.posZ);

            if (sleepResult == EntityPlayer.EnumStatus.OK) {
               if (Config.enablePositionReset) {
                  ChunkCoordinates chunkCoordinates =
                        AlternateSleep.getSafePosition(event.player.worldObj, event.player.posX, event.player
                              .posY, event.player.posZ);
                  event.player.setPosition(chunkCoordinates.posX + 0.5f, chunkCoordinates.posY + 0.1f, chunkCoordinates.posZ + 0.5f);
               }

               event.player.addChatMessage(new ChatComponentTranslation("msg.tooTired"));
            }
         }
      }
   }

}
