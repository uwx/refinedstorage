package com.raoulvdberge.refinedstorage.apiimpl.network.node.externalstorage;

import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerGroup;
import com.raoulvdberge.refinedstorage.RS;
import com.raoulvdberge.refinedstorage.api.network.INetwork;
import com.raoulvdberge.refinedstorage.api.storage.AccessType;
import com.raoulvdberge.refinedstorage.api.storage.IStorage;
import com.raoulvdberge.refinedstorage.api.storage.IStorageProvider;
import com.raoulvdberge.refinedstorage.api.util.IComparer;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.IGuiStorage;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNode;
import com.raoulvdberge.refinedstorage.apiimpl.storage.StorageCacheFluid;
import com.raoulvdberge.refinedstorage.apiimpl.storage.StorageCacheItem;
import com.raoulvdberge.refinedstorage.capability.CapabilityNetworkNodeProxy;
import com.raoulvdberge.refinedstorage.integration.projecte.IntegrationProjectE;
import com.raoulvdberge.refinedstorage.integration.projecte.StorageItemTransmutationTable;
import com.raoulvdberge.refinedstorage.integration.storagedrawers.StorageItemItemRepository;
import com.raoulvdberge.refinedstorage.inventory.ItemHandlerBase;
import com.raoulvdberge.refinedstorage.inventory.ItemHandlerFluid;
import com.raoulvdberge.refinedstorage.inventory.ItemHandlerListenerNetworkNode;
import com.raoulvdberge.refinedstorage.tile.TileExternalStorage;
import com.raoulvdberge.refinedstorage.tile.config.*;
import com.raoulvdberge.refinedstorage.tile.data.TileDataParameter;
import com.raoulvdberge.refinedstorage.util.AccessTypeUtils;
import com.raoulvdberge.refinedstorage.util.StackUtils;
import com.raoulvdberge.refinedstorage.util.WorldUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NetworkNodeExternalStorage extends NetworkNode implements IStorageProvider, IGuiStorage, IComparable, IFilterable, IPrioritizable, IType, IAccessType {
    @CapabilityInject(IDrawerGroup.class)
    private static final Capability<IDrawerGroup> DRAWER_GROUP_CAPABILITY = null;

    public static final String ID = "external_storage";

    private static final String NBT_INSERT_PRIORITY = "PriorityInsert";
    private static final String NBT_EXTRACT_PRIORITY = "PriorityExtract";
    private static final String NBT_COMPARE = "Compare";
    private static final String NBT_MODE = "Mode";
    private static final String NBT_TYPE = "Type";

    private ItemHandlerBase itemFilters = new ItemHandlerBase(9, new ItemHandlerListenerNetworkNode(this));
    private ItemHandlerFluid fluidFilters = new ItemHandlerFluid(9, new ItemHandlerListenerNetworkNode(this));

    private int insertPriority = 0;
    private int extractPriority = 0;
    private int compare = IComparer.COMPARE_NBT | IComparer.COMPARE_DAMAGE;
    private int mode = IFilterable.WHITELIST;
    private int type = IType.ITEMS;
    private AccessType accessType = AccessType.INSERT_EXTRACT;
    private int networkTicks;

    private List<StorageItemExternal> itemStorages = new CopyOnWriteArrayList<>();
    private List<StorageFluidExternal> fluidStorages = new CopyOnWriteArrayList<>();

    public NetworkNodeExternalStorage(World world, BlockPos pos) {
        super(world, pos);
    }

    @Override
    public int getEnergyUsage() {
        return RS.INSTANCE.config.externalStorageUsage + ((itemStorages.size() + fluidStorages.size()) * RS.INSTANCE.config.externalStoragePerStorageUsage);
    }

    @Override
    public void onConnectedStateChange(INetwork network, boolean state) {
        super.onConnectedStateChange(network, state);

        updateStorage(network);
    }

    @Override
    public void update() {
        super.update();

        if (network != null) {
            if (networkTicks++ == 0) {
                updateStorage(network);

                return;
            }

            for (StorageItemExternal storage : itemStorages) {
                storage.detectChanges(network);
            }

            boolean fluidChangeDetected = false;

            for (StorageFluidExternal storage : fluidStorages) {
                if (storage.updateCache()) {
                    fluidChangeDetected = true;
                }
            }

            if (fluidChangeDetected) {
                network.getFluidStorageCache().invalidate();
            }
        }
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public NBTTagCompound writeConfiguration(NBTTagCompound tag) {
        super.writeConfiguration(tag);

        StackUtils.writeItems(itemFilters, 0, tag);
        StackUtils.writeItems(fluidFilters, 1, tag);

        tag.setInteger(NBT_INSERT_PRIORITY, insertPriority);
        tag.setInteger(NBT_EXTRACT_PRIORITY, extractPriority);
        tag.setInteger(NBT_COMPARE, compare);
        tag.setInteger(NBT_MODE, mode);
        tag.setInteger(NBT_TYPE, type);

        AccessTypeUtils.writeAccessType(tag, accessType);

        return tag;
    }

    @Override
    public void readConfiguration(NBTTagCompound tag) {
        super.readConfiguration(tag);

        StackUtils.readItems(itemFilters, 0, tag);
        StackUtils.readItems(fluidFilters, 1, tag);

        if (tag.hasKey(NBT_INSERT_PRIORITY)) {
            insertPriority = tag.getInteger(NBT_INSERT_PRIORITY);
        }

        if (tag.hasKey(NBT_EXTRACT_PRIORITY)) {
            extractPriority = tag.getInteger(NBT_EXTRACT_PRIORITY);
        }

        if (tag.hasKey(NBT_COMPARE)) {
            compare = tag.getInteger(NBT_COMPARE);
        }

        if (tag.hasKey(NBT_MODE)) {
            mode = tag.getInteger(NBT_MODE);
        }

        if (tag.hasKey(NBT_TYPE)) {
            type = tag.getInteger(NBT_TYPE);
        }

        accessType = AccessTypeUtils.readAccessType(tag);
    }

    @Override
    public int getCompare() {
        return compare;
    }

    @Override
    public void setCompare(int compare) {
        this.compare = compare;

        markDirty();
    }

    @Override
    public int getMode() {
        return mode;
    }

    @Override
    public void setMode(int mode) {
        this.mode = mode;

        markDirty();
    }

    @Override
    public int getInsertPriority() {
        return insertPriority;
    }

    @Override
    public void setInsertPriority(int priority) {
        this.insertPriority = priority;
        recalc();
    }

    @Override
    public int getExtractPriority() {
        return extractPriority;
    }

    @Override
    public void setExtractPriority(int priority) {
        this.extractPriority = priority;
        recalc();
    }

    private void recalc() {
        markDirty();

        if (network != null) {
            network.getItemStorageCache().sort();
            network.getFluidStorageCache().sort();
        }
    }

    public void updateStorage(INetwork network) {
        itemStorages.clear();
        fluidStorages.clear();

        TileEntity facing = getFacingTile();

        if (type == IType.ITEMS) {
            if (facing != null) {
                if (DRAWER_GROUP_CAPABILITY != null && facing.hasCapability(DRAWER_GROUP_CAPABILITY, getDirection().getOpposite())) {
                    itemStorages.add(new StorageItemItemRepository(this, () -> {
                        TileEntity f = getFacingTile();

                        return (f != null && f.hasCapability(DRAWER_GROUP_CAPABILITY, getDirection().getOpposite())) ? f.getCapability(DRAWER_GROUP_CAPABILITY, getDirection().getOpposite()) : null;
                    }));
                } else if (!(facing.hasCapability(CapabilityNetworkNodeProxy.NETWORK_NODE_PROXY_CAPABILITY, getDirection().getOpposite()) && facing.getCapability(CapabilityNetworkNodeProxy.NETWORK_NODE_PROXY_CAPABILITY, getDirection().getOpposite()).getNode() instanceof IStorageProvider)) {
                    IItemHandler itemHandler = WorldUtils.getItemHandler(facing, getDirection().getOpposite());

                    if (itemHandler != null) {
                        itemStorages.add(new StorageItemItemHandler(this, () -> WorldUtils.getItemHandler(getFacingTile(), getDirection().getOpposite())));
                    }
                }
            } else if (IntegrationProjectE.isLoaded() && world.getBlockState(pos.offset(getDirection())).getBlock().getUnlocalizedName().equals("tile.pe_transmutation_stone")) {
                itemStorages.add(new StorageItemTransmutationTable(this));
            }
        } else if (type == IType.FLUIDS) {
            IFluidHandler fluidHandler = WorldUtils.getFluidHandler(facing, getDirection().getOpposite());

            if (fluidHandler != null) {
                fluidStorages.add(new StorageFluidExternal(this, () -> WorldUtils.getFluidHandler(getFacingTile(), getDirection().getOpposite())));
            }
        }

        network.getNodeGraph().addPostRebuildHandler(StorageCacheItem.INVALIDATE);
        network.getNodeGraph().addPostRebuildHandler(StorageCacheFluid.INVALIDATE);
    }

    @Override
    public void addItemStorages(List<IStorage<ItemStack>> storagesInsert, List<IStorage<ItemStack>> storagesExtract) {
        storagesInsert.addAll(this.itemStorages);
        storagesExtract.addAll(this.itemStorages);
    }

    @Override
    public void addFluidStorages(List<IStorage<FluidStack>> storagesInsert, List<IStorage<FluidStack>> storagesExtract) {
        storagesInsert.addAll(this.fluidStorages);
        storagesExtract.addAll(this.fluidStorages);
    }

    @Override
    public String getGuiTitle() {
        return "gui.refinedstorage:external_storage";
    }

    @Override
    public TileDataParameter<Integer, ?> getRedstoneModeParameter() {
        return TileExternalStorage.REDSTONE_MODE;
    }

    @Override
    public TileDataParameter<Integer, ?> getCompareParameter() {
        return TileExternalStorage.COMPARE;
    }

    @Override
    public TileDataParameter<Integer, ?> getFilterParameter() {
        return TileExternalStorage.MODE;
    }

    @Override
    public TileDataParameter<Integer, ?> getInsertPriorityParameter() {
        return TileExternalStorage.PRIORITY_INSERT;
    }

    @Override
    public TileDataParameter<Integer, ?> getExtractPriorityParameter() {
        return TileExternalStorage.PRIORITY_EXTRACT;
    }

    @Override
    public TileDataParameter<Boolean, ?> getVoidExcessParameter() {
        return null;
    }

    @Override
    public TileDataParameter<AccessType, ?> getAccessTypeParameter() {
        return TileExternalStorage.ACCESS_TYPE;
    }

    @Override
    public String getVoidExcessType() {
        return null;
    }

    @Override
    public int getStored() {
        return TileExternalStorage.STORED.getValue();
    }

    @Override
    public int getCapacity() {
        return TileExternalStorage.CAPACITY.getValue();
    }

    @Override
    public AccessType getAccessType() {
        return accessType;
    }

    @Override
    public void setAccessType(AccessType type) {
        this.accessType = type;

        if (network != null) {
            network.getItemStorageCache().invalidate();
            network.getFluidStorageCache().invalidate();
        }

        markDirty();
    }

    @Override
    public TileDataParameter<Integer, ?> getTypeParameter() {
        return TileExternalStorage.TYPE;
    }

    @Override
    public int getType() {
        return world.isRemote ? TileExternalStorage.TYPE.getValue() : type;
    }

    @Override
    public void setType(int type) {
        this.type = type;

        markDirty();

        if (network != null) {
            updateStorage(network);
        }
    }

    @Override
    public IItemHandler getFilterInventory() {
        return getType() == IType.ITEMS ? itemFilters : fluidFilters;
    }

    public ItemHandlerBase getItemFilters() {
        return itemFilters;
    }

    public ItemHandlerFluid getFluidFilters() {
        return fluidFilters;
    }

    public List<StorageItemExternal> getItemStorages() {
        return itemStorages;
    }

    public List<StorageFluidExternal> getFluidStorages() {
        return fluidStorages;
    }
}
