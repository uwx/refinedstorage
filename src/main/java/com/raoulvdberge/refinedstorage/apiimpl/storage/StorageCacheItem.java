package com.raoulvdberge.refinedstorage.apiimpl.storage;

import com.raoulvdberge.refinedstorage.api.network.INetwork;
import com.raoulvdberge.refinedstorage.api.storage.*;
import com.raoulvdberge.refinedstorage.api.util.IStackList;
import com.raoulvdberge.refinedstorage.apiimpl.API;
import net.minecraft.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class StorageCacheItem implements IStorageCache<ItemStack> {
    public static final Consumer<INetwork> INVALIDATE = network -> network.getItemStorageCache().invalidate();

    private INetwork network;
    private CopyOnWriteArrayList<IStorage<ItemStack>> storagesInsert = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<IStorage<ItemStack>> storagesExtract = new CopyOnWriteArrayList<>();
    private IStackList<ItemStack> list = API.instance().createItemStackList();
    private List<IStorageCacheListener<ItemStack>> listeners = new LinkedList<>();
    private List<Pair<ItemStack, Integer>> batchedChanges = new ArrayList<>();

    public StorageCacheItem(INetwork network) {
        this.network = network;
    }

    @Override
    public synchronized void invalidate() {
        storagesInsert.clear();
        storagesExtract.clear();

        // TODO is sort actually called after invalidate? i hope so x)
        network.getNodeGraph().all().stream()
            .filter(node -> node.canUpdate() && node instanceof IStorageProvider)
            .forEach(node -> ((IStorageProvider) node).addItemStorages(storagesInsert, storagesExtract));

        list.clear();

        sort();

        for (IStorage<ItemStack> storage : storagesExtract) {
            if (storage.getAccessType() == AccessType.INSERT) {
                continue;
            }

            for (ItemStack stack : storage.getStacks()) {
                if (!stack.isEmpty()) {
                    add(stack, stack.getCount(), true, false);
                }
            }
        }

        listeners.forEach(IStorageCacheListener::onInvalidated);
    }

    @Override
    public synchronized void add(@Nonnull ItemStack stack, int size, boolean rebuilding, boolean batched) {
        list.add(stack, size);

        if (!rebuilding) {
            if (!batched) {
                listeners.forEach(l -> l.onChanged(stack, size));
            } else {
                batchedChanges.add(Pair.of(stack, size));
            }
        }
    }

    @Override
    public synchronized void remove(@Nonnull ItemStack stack, int size, boolean batched) {
        if (list.remove(stack, size)) {
            if (!batched) {
                listeners.forEach(l -> l.onChanged(stack, -size));
            } else {
                batchedChanges.add(Pair.of(stack, -size));
            }
        }
    }

    @Override
    public void flush() {
        if (!batchedChanges.isEmpty()) {
            batchedChanges.forEach(c -> listeners.forEach(l -> l.onChanged(c.getKey(), c.getValue())));
            batchedChanges.clear();
        }
    }

    @Override
    public void addListener(IStorageCacheListener<ItemStack> listener) {
        listeners.add(listener);

        listener.onAttached();
    }

    @Override
    public void removeListener(IStorageCacheListener<ItemStack> listener) {
        listeners.remove(listener);
    }

    @Override
    public void sort() {
        storagesInsert.sort(IStorage.COMPARATOR_INSERT);
        storagesExtract.sort(IStorage.COMPARATOR_EXTRACT);
    }

    @Override
    public IStackList<ItemStack> getList() {
        return list;
    }

    @Override
    public List<IStorage<ItemStack>> getStoragesInsert() {
        return storagesInsert;
    }

    @Override
    public List<IStorage<ItemStack>> getStoragesExtract() {
        return storagesExtract;
    }
}
