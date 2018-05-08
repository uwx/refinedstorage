package com.raoulvdberge.refinedstorage.tile.config;

import com.raoulvdberge.refinedstorage.api.network.node.INetworkNodeProxy;
import com.raoulvdberge.refinedstorage.tile.data.TileDataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.tileentity.TileEntity;

public interface IPrioritizable {
    static <T extends TileEntity & INetworkNodeProxy> TileDataParameter<Integer, T> createParameterInsert() {
        return new TileDataParameter<>(DataSerializers.VARINT, 0, t ->
                ((IPrioritizable) t.getNode()).getInsertPriority(), (t, v) -> ((IPrioritizable) t.getNode()).setInsertPriority(v));
    }

    static <T extends TileEntity & INetworkNodeProxy> TileDataParameter<Integer, T> createParameterExtract() {
        return new TileDataParameter<>(DataSerializers.VARINT, 0, t ->
                ((IPrioritizable) t.getNode()).getExtractPriority(), (t, v) -> ((IPrioritizable) t.getNode()).setExtractPriority(v));
    }

    int getInsertPriority();

    void setInsertPriority(int priority);

    int getExtractPriority();

    void setExtractPriority(int priority);
}
