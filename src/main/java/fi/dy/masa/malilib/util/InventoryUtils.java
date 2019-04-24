package fi.dy.masa.malilib.util;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.container.Container;
import net.minecraft.container.PlayerContainer;
import net.minecraft.container.Slot;
import net.minecraft.container.SlotActionType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.BasicInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.DefaultedList;

public class InventoryUtils
{
    private static final DefaultedList<ItemStack> EMPTY_LIST = DefaultedList.create();

    /**
     * Check whether the stacks are identical otherwise, but ignoring the stack size
     * @param stack1
     * @param stack2
     * @return
     */
    public static boolean areStacksEqual(ItemStack stack1, ItemStack stack2)
    {
        return ItemStack.areEqualIgnoreTags(stack1, stack2) && ItemStack.areTagsEqual(stack1, stack2);
    }

    /**
     * Checks whether the stacks are identical otherwise, but ignoring the stack size,
     * and if the item is damageable, then ignoring the durability too.
     * @param stack1
     * @param stack2
     * @return
     */
    public static boolean areStacksEqualIgnoreDurability(ItemStack stack1, ItemStack stack2)
    {
        return ItemStack.areEqualIgnoreDurability(stack1, stack2) && ItemStack.areTagsEqual(stack1, stack2);
    }

    /**
     * Swaps the stack from the slot <b>slotNum</b> to the given hotbar slot <b>hotbarSlot</b>
     * @param container
     * @param slot
     * @param hotbarSlot
     */
    public static void swapSlots(Container container, int slotNum, int hotbarSlot)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.interactionManager.method_2906(container.syncId, slotNum, hotbarSlot, SlotActionType.SWAP, mc.player);
    }

    /**
     * Assuming that the slot is from the ContainerPlayer container,
     * returns whether the given slot number is one of the regular inventory slots.
     * This means that the crafting slots and armor slots are not valid.
     * @param slotNumber
     * @param allowOffhand
     * @return
     */
    public static boolean isRegularInventorySlot(int slotNumber, boolean allowOffhand)
    {
        return slotNumber > 8 && (allowOffhand || slotNumber < 45);
    }

    /**
     * Finds an empty slot in the player inventory. Armor slots are not valid for the return value of this method.
     * Whether or not the offhand slot is valid, depends on the <b>allowOffhand</b> argument.
     * @param containerPlayer
     * @param allowOffhand
     * @param reverse
     * @return the slot number, or -1 if none were found
     */
    public static int findEmptySlotInPlayerInventory(Container containerPlayer, boolean allowOffhand, boolean reverse)
    {
        final int startSlot = reverse ? containerPlayer.slotList.size() - 1 : 0;
        final int endSlot = reverse ? -1 : containerPlayer.slotList.size();
        final int increment = reverse ? -1 : 1;

        for (int slotNum = startSlot; slotNum != endSlot; slotNum += increment)
        {
            Slot slot = containerPlayer.slotList.get(slotNum);
            ItemStack stackSlot = slot.getStack();

            // Inventory crafting, armor and offhand slots are not valid
            if (stackSlot.isEmpty() && isRegularInventorySlot(slot.id, allowOffhand))
            {
                return slot.id;
            }
        }

        return -1;
    }

    /**
     * Finds a slot with an identical item than <b>stackReference</b>, ignoring the durability
     * of damageable items. Does not allow crafting or armor slots or the offhand slot
     * in the ContainerPlayer container.
     * @param container
     * @param stackReference
     * @param reverse
     * @return the slot number, or -1 if none were found
     */
    public static int findSlotWithItem(Container container, ItemStack stackReference, boolean reverse)
    {
        final int startSlot = reverse ? container.slotList.size() - 1 : 0;
        final int endSlot = reverse ? -1 : container.slotList.size();
        final int increment = reverse ? -1 : 1;
        final boolean isPlayerInv = container instanceof PlayerContainer;

        for (int slotNum = startSlot; slotNum != endSlot; slotNum += increment)
        {
            Slot slot = container.slotList.get(slotNum);

            if ((isPlayerInv == false || isRegularInventorySlot(slot.id, false)) &&
                areStacksEqualIgnoreDurability(slot.getStack(), stackReference))
            {
                return slot.id;
            }
        }

        return -1;
    }

    /**
     * Swap the given item to the player's main hand, if that item is found
     * in the player's inventory.
     * @param stackReference
     * @param mc
     * @return true if an item was swapped to the main hand, false if it was already in the hand, or was not found in the inventory
     */
    public static boolean swapItemToMainHand(ItemStack stackReference, MinecraftClient mc)
    {
        PlayerEntity player = mc.player;
        boolean isCreative = player.abilities.creativeMode;

        // Already holding the requested item
        if (areStacksEqual(stackReference, player.getMainHandStack()))
        {
            return false;
        }

        if (isCreative)
        {
            player.inventory.addPickBlock(stackReference);
            mc.interactionManager.clickCreativeStack(player.getMainHandStack(), 36 + player.inventory.selectedSlot); // sendSlotPacket
            return true;
        }
        else
        {
            int slot = findSlotWithItem(player.playerContainer, stackReference, true);

            if (slot != -1)
            {
                int currentHotbarSlot = player.inventory.selectedSlot;
                mc.interactionManager.method_2906(player.playerContainer.syncId, slot, currentHotbarSlot, SlotActionType.SWAP, mc.player);
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the given Shulker Box (or other storage item with the
     * same NBT data structure) currently contains any items.
     * @param stackShulkerBox
     * @return
     */
    public static boolean shulkerBoxHasItems(ItemStack stackShulkerBox)
    {
        CompoundTag nbt = stackShulkerBox.getTag();

        if (nbt != null && nbt.containsKey("BlockEntityTag", Constants.NBT.TAG_COMPOUND))
        {
            CompoundTag tag = nbt.getCompound("BlockEntityTag");

            if (tag.containsKey("Items", Constants.NBT.TAG_LIST))
            {
                ListTag tagList = tag.getList("Items", Constants.NBT.TAG_COMPOUND);
                return tagList.size() > 0;
            }
        }

        return false;
    }

    /**
     * Returns the list of items currently stored in the given Shulker Box
     * (or other storage item with the same NBT data structure).
     * Does not keep empty slots.
     * @param stackShulkerBox
     * @return
     */
    public static DefaultedList<ItemStack> getStoredItems(ItemStack stackIn)
    {
        CompoundTag nbt = stackIn.getTag();

        if (nbt != null && nbt.containsKey("BlockEntityTag", Constants.NBT.TAG_COMPOUND))
        {
            CompoundTag tagBlockEntity = nbt.getCompound("BlockEntityTag");

            if (tagBlockEntity.containsKey("Items", Constants.NBT.TAG_LIST))
            {
                DefaultedList<ItemStack> items = DefaultedList.create();
                ListTag tagList = tagBlockEntity.getList("Items", Constants.NBT.TAG_COMPOUND);
                final int count = tagList.size();

                for (int i = 0; i < count; ++i)
                {
                    ItemStack stack = ItemStack.fromTag(tagList.getCompoundTag(i));

                    if (stack.isEmpty() == false)
                    {
                        items.add(stack);
                    }
                }

                return items;
            }
        }

        return DefaultedList.create();
    }

    /**
     * Returns the list of items currently stored in the given Shulker Box
     * (or other storage item with the same NBT data structure).
     * Preserves empty slots.
     * @param stackShulkerBox
     * @param slotCount the maximum number of slots, and thus also the size of the list to create
     * @return
     */
    public static DefaultedList<ItemStack> getStoredItems(ItemStack stackIn, int slotCount)
    {
        CompoundTag nbt = stackIn.getTag();

        if (nbt != null && nbt.containsKey("BlockEntityTag", Constants.NBT.TAG_COMPOUND))
        {
            CompoundTag tagBlockEntity = nbt.getCompound("BlockEntityTag");

            if (tagBlockEntity.containsKey("Items", Constants.NBT.TAG_LIST))
            {
                ListTag tagList = tagBlockEntity.getList("Items", Constants.NBT.TAG_COMPOUND);
                final int count = tagList.size();
                int maxSlot = -1;

                if (slotCount <= 0)
                {
                    for (int i = 0; i < count; ++i)
                    {
                        CompoundTag tag = tagList.getCompoundTag(i);
                        int slot = tag.getByte("Slot");

                        if (slot > maxSlot)
                        {
                            maxSlot = slot;
                        }
                    }

                    slotCount = maxSlot + 1;
                }

                DefaultedList<ItemStack> items = DefaultedList.create(slotCount, ItemStack.EMPTY);

                for (int i = 0; i < count; ++i)
                {
                    CompoundTag tag = tagList.getCompoundTag(i);
                    ItemStack stack = ItemStack.fromTag(tag);
                    int slot = tag.getByte("Slot");

                    if (slot >= 0 && slot < items.size() && stack.isEmpty() == false)
                    {
                        items.set(slot, stack);
                    }
                }

                return items;
            }
        }

        return EMPTY_LIST;
    }

    /**
     * Returns a map of the stored item counts in the given Shulker Box
     * (or other storage item with the same NBT data structure).
     * @param stackShulkerBox
     * @return
     */
    public static Object2IntOpenHashMap<ItemType> getStoredItemCounts(ItemStack stackShulkerBox)
    {
        Object2IntOpenHashMap<ItemType> map = new Object2IntOpenHashMap<>();
        DefaultedList<ItemStack> items = getStoredItems(stackShulkerBox);

        for (int slot = 0; slot < items.size(); ++slot)
        {
            ItemStack stack = items.get(slot);

            if (stack.isEmpty() == false)
            {
                map.addTo(new ItemType(stack), stack.getAmount());
            }
        }

        return map;
    }

    /**
     * Returns a map of the stored item counts in the given inventory.
     * This also counts the contents of any Shulker Boxes
     * (or other storage item with the same NBT data structure).
     * @param player
     * @return
     */
    public static Object2IntOpenHashMap<ItemType> getInventoryItemCounts(Inventory inv)
    {
        Object2IntOpenHashMap<ItemType> map = new Object2IntOpenHashMap<>();
        final int slots = inv.getInvSize();

        for (int slot = 0; slot < slots; ++slot)
        {
            ItemStack stack = inv.getInvStack(slot);

            if (stack.isEmpty() == false)
            {
                map.addTo(new ItemType(stack, false, true), stack.getAmount());

                if (stack.getItem() instanceof BlockItem &&
                    ((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock &&
                    shulkerBoxHasItems(stack))
                {
                    Object2IntOpenHashMap<ItemType> boxCounts = getStoredItemCounts(stack);

                    for (ItemType type : boxCounts.keySet())
                    {
                        map.addTo(type, boxCounts.getInt(type));
                    }
                }
            }
        }

        return map;
    }

    /**
     * Returns the given list of items wrapped as an InventoryBasic
     * @param items
     * @return
     */
    public static Inventory getAsInventory(DefaultedList<ItemStack> items)
    {
        BasicInventory inv = new BasicInventory(items.size());

        for (int slot = 0; slot < items.size(); ++slot)
        {
            inv.setInvStack(slot, items.get(slot));
        }

        return inv;
    }
}
