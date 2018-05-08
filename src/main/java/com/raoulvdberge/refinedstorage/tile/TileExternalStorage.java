package com.raoulvdberge.refinedstorage.tile;

import com.raoulvdberge.refinedstorage.api.storage.AccessType;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.externalstorage.NetworkNodeExternalStorage;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.externalstorage.StorageFluidExternal;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.externalstorage.StorageItemExternal;
import com.raoulvdberge.refinedstorage.tile.config.*;
import com.raoulvdberge.refinedstorage.tile.data.TileDataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class TileExternalStorage extends TileNode<NetworkNodeExternalStorage> {
    public static final TileDataParameter<Integer, TileExternalStorage> PRIORITY_INSERT = IPrioritizable.createParameterInsert();
    public static final TileDataParameter<Integer, TileExternalStorage> PRIORITY_EXTRACT = IPrioritizable.createParameterExtract();
    public static final TileDataParameter<Integer, TileExternalStorage> COMPARE = IComparable.createParameter();
    public static final TileDataParameter<Integer, TileExternalStorage> MODE = IFilterable.createParameter();
    public static final TileDataParameter<Integer, TileExternalStorage> TYPE = IType.createParameter();
    public static final TileDataParameter<AccessType, TileExternalStorage> ACCESS_TYPE = IAccessType.createParameter();
    public static final TileDataParameter<Integer, TileExternalStorage> STORED = new TileDataParameter<>(DataSerializers.VARINT, 0, t -> {
        int stored = 0;

        for (StorageItemExternal storage : t.getNode().getItemStorages()) {
            stored += storage.getStored();
        }

        for (StorageFluidExternal storage : t.getNode().getFluidStorages()) {
            stored += storage.getStored();
        }

        return stored;
    });
    public static final TileDataParameter<Integer, TileExternalStorage> CAPACITY = new TileDataParameter<>(DataSerializers.VARINT, 0, t -> {
        int capacity = 0;

        for (StorageItemExternal storage : t.getNode().getItemStorages()) {
            capacity += storage.getCapacity();
        }

        for (StorageFluidExternal storage : t.getNode().getFluidStorages()) {
            capacity += storage.getCapacity();
        }

        return capacity;
    });

    public TileExternalStorage() {
        dataManager.addWatchedParameter(PRIORITY_INSERT);
        dataManager.addWatchedParameter(PRIORITY_EXTRACT);
        dataManager.addWatchedParameter(COMPARE);
        dataManager.addWatchedParameter(MODE);
        dataManager.addWatchedParameter(STORED);
        dataManager.addWatchedParameter(CAPACITY);
        dataManager.addWatchedParameter(TYPE);
        dataManager.addWatchedParameter(ACCESS_TYPE);
    }

    @Override
    @Nonnull
    public NetworkNodeExternalStorage createNode(World world, BlockPos pos) {
        return new NetworkNodeExternalStorage(world, pos);
    }

    @Override
    public String getNodeId() {
        return NetworkNodeExternalStorage.ID;
    }
}
