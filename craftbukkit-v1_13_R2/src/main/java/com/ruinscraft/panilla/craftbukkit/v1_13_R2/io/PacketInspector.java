package com.ruinscraft.panilla.craftbukkit.v1_13_R2.io;

import com.ruinscraft.panilla.api.IPanilla;
import com.ruinscraft.panilla.api.IPanillaPlayer;
import com.ruinscraft.panilla.api.exception.EntityNbtNotPermittedException;
import com.ruinscraft.panilla.api.exception.NbtNotPermittedException;
import com.ruinscraft.panilla.api.io.IPacketInspector;
import com.ruinscraft.panilla.api.nbt.checks.NbtChecks;
import com.ruinscraft.panilla.craftbukkit.v1_13_R2.nbt.NbtTagCompound;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;

import java.lang.reflect.Field;
import java.util.UUID;

public class PacketInspector implements IPacketInspector {

    private final IPanilla panilla;

    public PacketInspector(IPanilla panilla) {
        this.panilla = panilla;
    }

    @Override
    public void checkPacketPlayInSetCreativeSlot(Object _packet) throws NbtNotPermittedException {
        if (_packet instanceof PacketPlayInSetCreativeSlot) {
            PacketPlayInSetCreativeSlot packet = (PacketPlayInSetCreativeSlot) _packet;

            int slot = packet.b();
            ItemStack itemStack = packet.getItemStack();

            if (itemStack == null || !itemStack.hasTag()) return;

            NbtChecks.checkPacketPlayIn(slot, new NbtTagCompound(
                            itemStack.getTag()), itemStack.getItem().getClass().getSimpleName(),
                    packet.getClass().getSimpleName(), panilla);
        }
    }

    @Override
    public void checkPacketPlayOutSetSlot(Object _packet) throws NbtNotPermittedException {
        if (_packet instanceof PacketPlayOutSetSlot) {
            PacketPlayOutSetSlot packet = (PacketPlayOutSetSlot) _packet;

            try {
                Field slotField = PacketPlayOutSetSlot.class.getDeclaredField("b");
                Field itemStackField = PacketPlayOutSetSlot.class.getDeclaredField("c");

                slotField.setAccessible(true);
                itemStackField.setAccessible(true);

                int slot = (int) slotField.get(packet);
                ItemStack itemStack = (ItemStack) itemStackField.get(packet);

                if (itemStack == null || !itemStack.hasTag()) return;

                NbtChecks.checkPacketPlayOut(slot, new NbtTagCompound(
                                itemStack.getTag()), itemStack.getItem().getClass().getSimpleName(),
                        packet.getClass().getSimpleName(), panilla);
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void checkPacketPlayOutSpawnEntity(Object _packet) throws EntityNbtNotPermittedException {
        if (_packet instanceof PacketPlayOutSpawnEntity) {
            PacketPlayOutSpawnEntity packet = (PacketPlayOutSpawnEntity) _packet;

            try {
                Field typeField = PacketPlayOutSpawnEntity.class.getDeclaredField("b");

                typeField.setAccessible(true);

                UUID entityId = (UUID) typeField.get(packet);
                org.bukkit.entity.Entity bukkitEntity = org.bukkit.Bukkit.getEntity(entityId);
                CraftEntity craftEntity = (CraftEntity) bukkitEntity;
                Entity entity = craftEntity.getHandle();

                if (entity != null) {
                    if (entity instanceof EntityItem) {
                        EntityItem item = (EntityItem) entity;

                        if (!item.getItemStack().hasTag()) {
                            return;
                        }

                        String failed = NbtChecks.checkAll(new NbtTagCompound(item.getItemStack().getTag()), item.getItemStack().getClass().getSimpleName(), panilla);

                        if (failed != null) {
                            throw new EntityNbtNotPermittedException(packet.getClass().getSimpleName(), false, entityId);
                        }
                    }
                }
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void sendPacketPlayOutSetSlotAir(IPanillaPlayer player, int slot) {
        CraftPlayer craftPlayer = (CraftPlayer) player.getHandle();
        EntityPlayer entityPlayer = craftPlayer.getHandle();
        PacketPlayOutSetSlot packet = new PacketPlayOutSetSlot(0, slot, new ItemStack(Blocks.AIR));
        entityPlayer.playerConnection.sendPacket(packet);
    }

    @Override
    public void removeEntity(UUID entityId) {
        Bukkit.getServer().getEntity(entityId).remove();
    }

    @Override
    public void removeEntityLegacy(int entityId) {
        throw new IllegalStateException("Cannot use #removeEntityLegacy on 1.13");
    }

}
