# uwx/refinedstorage
This is a fork of [Refined Storage](https://github.com/raoulvdberge/refinedstorage) a Minecraft Forge mod for mass storing items.
This version has separate insert/extract priority settings for storage items, designed so that you can still store items normally
while using a ProjectE Transmutation Table to learn new items as they come in, making it usable for keeping backup copies of all
your items.

For more information, see the original project's [README](https://github.com/raoulvdberge/refinedstorage#refined-storage--).

#### More in-depth explanation
Sequence for inserting an item into the network:
1. If item is in transmutation table's external storage filter list, item is burned into EMC
1. Item is learned if it's not already (this does not cost any items, unlike the real Transmutation Table)
1. Storage is updated to reflect the amount of obtainable items based on the stored EMC amount; if the existing item hasn't been
   burned into EMC yet, it's placed into the network's storage.

Sequence for removing an item from the network:
1. If item is present in storage, item is taken from storage;
1. Otherwise, item is created from the tablet
  * You can test this by adding and removing an item, and looking in the disk drives - depending on the item, as long as it was
    imported early enough and is then stored in a full drive, the color in the drive bay should change.
  * You can also test this by creating a stack of very high EMC items and putting them back in, you can see that the count of
    other available EMC-able items changes when you first pull it out, but not again since the item gets stored properly.

## Using the priority settings
Rather than the priority field being an integer, it's now two integers delimited by a comma (`,`). The increment and decrement
buttons will likely not do what you intend; instead, you should type the values manually into the field.

## Future ideas
Maybe instead of reporting the total amount of creatable items in the Grid (StorageItemTransmutationTable.getStacks), cap it to
only show up to the max stack of that item or if the number doesn't matter for the amount being pulled out, report only 1 item,
that way everything is sorted nicely!

To delete 1 item from the stack when researching it through the tablet (to work similarly to the real Transmutation Table), we
need to modify [StorageItemTransmutationTable.insert](https://github.com/raoulvdberge/refinedstorage/blob/32e22acd7aab435972493cd895b1b24bfe5e1866/src/main/java/com/raoulvdberge/refinedstorage/integration/projecte/StorageItemTransmutationTable.java#L52-L84).
We need to make `handleKnowledge` return a bool when `provider.hasKnowledge()` is false, and then we do this:
```java
//ItemStack retval = actualStack;
if (!simulate) {
    if (IFilterable.canTake(externalStorage.getItemFilters(), externalStorage.getMode(), externalStorage.getCompare(), stack)) { // shouldn't need to copy for the `stack` bit here
        provider.setEmc(provider.getEmc() + emc);
        actualStack = null; // we deleted the item
        
        handleKnowledge(provider, stack.copy());
    } else if (handleKnowledge(provider, stack.copy())) {
        provider.setEmc(provider.getEmc() + (emc / size)); // TODO get the emc value of only 1 item properly, though this works fine
        
        if (size == 1) actualStack = null; // took the last item from the stack

        // take 1 item from the stack
        actualStack.setCount(size-1);
    }

    EntityPlayer player = externalStorage.getWorld().getPlayerEntityByUUID(externalStorage.getOwner());

    if (player != null) {
        provider.sync((EntityPlayerMP) player);
    }
}

return retval;
```

Idea to reduce Refined Storage lag:
- Keep a local EMC cache (preferably use an ObjectIntMap of some sort)
- Use some sort of ServerChatEvent (whatever the equivalent is in 1.12) to clear the cache when `/projecte setEMC` is ran (or
  perhaps just add a custom command that does it?)
