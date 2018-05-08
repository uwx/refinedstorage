package com.raoulvdberge.refinedstorage.tile;

import com.raoulvdberge.refinedstorage.api.storage.AccessType;
import com.raoulvdberge.refinedstorage.api.storage.IStorageDisk;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.diskdrive.NetworkNodeDiskDrive;
import com.raoulvdberge.refinedstorage.tile.config.*;
import com.raoulvdberge.refinedstorage.tile.data.TileDataParameter;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TileDiskDrive extends TileNode<NetworkNodeDiskDrive> {
    public static final TileDataParameter<Integer, TileDiskDrive> PRIORITY_INSERT = IPrioritizable.createParameterInsert();
    public static final TileDataParameter<Integer, TileDiskDrive> PRIORITY_EXTRACT = IPrioritizable.createParameterExtract();
    public static final TileDataParameter<Integer, TileDiskDrive> COMPARE = IComparable.createParameter();
    public static final TileDataParameter<Integer, TileDiskDrive> MODE = IFilterable.createParameter();
    public static final TileDataParameter<Integer, TileDiskDrive> TYPE = IType.createParameter();
    public static final TileDataParameter<Boolean, TileDiskDrive> VOID_EXCESS = IExcessVoidable.createParameter();
    public static final TileDataParameter<AccessType, TileDiskDrive> ACCESS_TYPE = IAccessType.createParameter();
    public static final TileDataParameter<Integer, TileDiskDrive> STORED = new TileDataParameter<>(DataSerializers.VARINT, 0, t -> {
        int stored = 0;

        for (IStorageDisk storage : t.getNode().getItemStorages()) {
            if (storage != null) {
                stored += storage.getStored();
            }
        }

        for (IStorageDisk storage : t.getNode().getFluidStorages()) {
            if (storage != null) {
                stored += storage.getStored();
            }
        }

        return stored;
    });
    public static final TileDataParameter<Integer, TileDiskDrive> CAPACITY = new TileDataParameter<>(DataSerializers.VARINT, 0, t -> {
        int capacity = 0;

        for (IStorageDisk storage : t.getNode().getItemStorages()) {
            if (storage != null) {
                if (storage.getCapacity() == -1) {
                    return -1;
                }

                capacity += storage.getCapacity();
            }
        }

        for (IStorageDisk storage : t.getNode().getFluidStorages()) {
            if (storage != null) {
                if (storage.getCapacity() == -1) {
                    return -1;
                }

                capacity += storage.getCapacity();
            }
        }

        return capacity;
    });

    private static final String NBT_DISK_STATE = "DiskState_%d";

    public static final int DISK_STATE_NORMAL = 0;
    public static final int DISK_STATE_NEAR_CAPACITY = 1;
    public static final int DISK_STATE_FULL = 2;
    public static final int DISK_STATE_DISCONNECTED = 3;
    public static final int DISK_STATE_NONE = 4;

    private Integer[] diskState = new Integer[8];

    public TileDiskDrive() {
        dataManager.addWatchedParameter(PRIORITY_INSERT);
        dataManager.addWatchedParameter(PRIORITY_EXTRACT);
        dataManager.addWatchedParameter(COMPARE);
        dataManager.addWatchedParameter(MODE);
        dataManager.addWatchedParameter(TYPE);
        dataManager.addWatchedParameter(VOID_EXCESS);
        dataManager.addWatchedParameter(ACCESS_TYPE);
        dataManager.addWatchedParameter(STORED);
        dataManager.addWatchedParameter(CAPACITY);

        initDiskState(diskState);
    }

    @Override
    public NBTTagCompound writeUpdate(NBTTagCompound tag) {
        super.writeUpdate(tag);

        writeDiskState(tag, 8, getNode().getNetwork() != null, getNode().getItemStorages(), getNode().getFluidStorages());

        return tag;
    }

    @Override
    public void readUpdate(NBTTagCompound tag) {
        super.readUpdate(tag);

        readDiskState(tag, diskState);
    }

    public Integer[] getDiskState() {
        return diskState;
    }

    public static void writeDiskState(NBTTagCompound tag, int disks, boolean connected, IStorageDisk[] itemStorages, IStorageDisk[] fluidStorages) {
        for (int i = 0; i < disks; ++i) {
            int state = DISK_STATE_NONE;

            if (itemStorages[i] != null || fluidStorages[i] != null) {
                if (!connected) {
                    state = DISK_STATE_DISCONNECTED;
                } else {
                    state = getDiskState(
                        itemStorages[i] != null ? itemStorages[i].getStored() : fluidStorages[i].getStored(),
                        itemStorages[i] != null ? itemStorages[i].getCapacity() : fluidStorages[i].getCapacity()
                    );
                }
            }

            tag.setInteger(String.format(NBT_DISK_STATE, i), state);
        }
    }

    public static void readDiskState(NBTTagCompound tag, Integer[] diskState) {
        for (int i = 0; i < diskState.length; ++i) {
            diskState[i] = tag.getInteger(String.format(NBT_DISK_STATE, i));
        }
    }

    public static void initDiskState(Integer[] diskState) {
        for (int i = 0; i < diskState.length; ++i) {
            diskState[i] = DISK_STATE_NONE;
        }
    }

    public static int getDiskState(int stored, int capacity) {
        if (stored == capacity) {
            return DISK_STATE_FULL;
        } else if ((int) ((float) stored / (float) capacity * 100F) >= 85) {
            return DISK_STATE_NEAR_CAPACITY;
        } else {
            return DISK_STATE_NORMAL;
        }
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(getNode().getDisks());
        }

        return super.getCapability(capability, facing);
    }

    @Override
    @Nonnull
    public NetworkNodeDiskDrive createNode(World world, BlockPos pos) {
        return new NetworkNodeDiskDrive(world, pos);
    }

    @Override
    public String getNodeId() {
        return NetworkNodeDiskDrive.ID;
    }
}
