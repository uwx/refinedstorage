package com.raoulvdberge.refinedstorage.api.storage;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

/**
 * Represents a node that provides the network with storage.
 */
public interface IStorageProvider {
    /**
     * @param storagesInsert the item storages sorted by insert priority
     * @param storagesExtract the item storages sorted by extract priority
     */
    void addItemStorages(List<IStorage<ItemStack>> storagesInsert, List<IStorage<ItemStack>> storagesExtract);

    /**
     * @param storagesInsert the fluid storages sorted by insert priority
     * @param storagesExtract the fluid storages sorted by extract priority
     */
    void addFluidStorages(List<IStorage<FluidStack>> storagesInsert, List<IStorage<FluidStack>> storagesExtract);
}
