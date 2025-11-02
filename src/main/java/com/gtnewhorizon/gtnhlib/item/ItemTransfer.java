package com.gtnewhorizon.gtnhlib.item;

import java.util.function.Consumer;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.blockpos.IBlockPos;
import com.gtnewhorizon.gtnhlib.blockpos.IWorldReferent;
import com.gtnewhorizon.gtnhlib.capability.item.ItemSink;
import com.gtnewhorizon.gtnhlib.capability.item.ItemSource;
import com.gtnewhorizon.gtnhlib.util.ItemUtil;

import lombok.Getter;
import lombok.Setter;

@SuppressWarnings({ "unused", "UnusedReturnValue" })
public class ItemTransfer {

    @Getter
    protected ItemSource source;
    @Getter
    protected ItemSink sink;
    protected ItemSink itemDropping;

    @Setter
    protected int stacksToTransfer = 1;
    @Setter
    protected int maxItemsPerTransfer = 64;
    @Setter
    protected int maxTotalTransferred = Integer.MAX_VALUE;

    @Getter
    protected int totalItemsTransferred = 0;
    @Getter
    protected int totalStacksTransferred = 0;
    @Getter
    protected int prevItemsTransferred = 0;
    @Getter
    protected int prevStacksTransferred = 0;

    protected int[] sourceSlots, sinkSlots;

    @Setter
    protected ItemStackPredicate filter;

    @Setter
    protected Consumer<ItemStack> rejectedStacks;

    public void source(ItemSource source) {
        this.source = source;
    }

    public void source(Object source, ForgeDirection side) {
        this.source = ItemUtil.getItemSource(source, side);
    }

    public void sink(ItemSink sink) {
        this.sink = sink;
    }

    public void sink(Object sink, ForgeDirection side) {
        this.sink = ItemUtil.getItemSink(sink, side);
    }

    public <Coord extends IBlockPos & IWorldReferent> void push(Coord pos, ForgeDirection side) {
        TileEntity self = pos.getWorld().getTileEntity(pos.getX(), pos.getY(), pos.getZ());
        TileEntity adjacent = pos.getWorld()
                .getTileEntity(pos.getX() + side.offsetX, pos.getY() + side.offsetY, pos.getZ() + side.offsetZ);

        push(self, side, adjacent);
    }

    public void push(Object self, ForgeDirection side, Object target) {
        source(self, side);
        sink(target, side.getOpposite());
    }

    public <Coord extends IBlockPos & IWorldReferent> void pull(Coord pos, ForgeDirection side) {
        TileEntity self = pos.getWorld().getTileEntity(pos.getX(), pos.getY(), pos.getZ());
        TileEntity adjacent = pos.getWorld()
                .getTileEntity(pos.getX() + side.offsetX, pos.getY() + side.offsetY, pos.getZ() + side.offsetZ);

        pull(self, side, adjacent);
    }

    public void pull(Object self, ForgeDirection side, Object target) {
        source(target, side.getOpposite());
        sink(self, side);
    }

    public void dropItems(World world, IBlockPos pos) {
        this.itemDropping = new DroppingItemSink(world, pos);
        if (this.rejectedStacks == null) {
            this.rejectedStacks = stack -> itemDropping.store(new InsertionItemStack(stack));
        }
    }

    public <Coord extends IBlockPos & IWorldReferent> void dropItems(Coord pos, ForgeDirection output) {
        dropItems(pos.getWorld(), pos.offset(output));
    }

    public void setSourceSlots(int... sourceSlots) {
        this.sourceSlots = sourceSlots;
    }

    public void setSinkSlots(int... sinkSlots) {
        this.sinkSlots = sinkSlots;
    }

    public int transfer() {
        ItemSink sink = this.sink == null ? itemDropping : this.sink;

        if (source == null) return 0;
        if (sink == null) return 0;
        if (stacksToTransfer == 0) return 0;
        if (maxItemsPerTransfer == 0) return 0;

        source.resetSource();
        sink.resetSink();

        source.setAllowedSourceSlots(sourceSlots);
        sink.setAllowedSinkSlots(sinkSlots);

        InventoryIterator iter = source.sourceIterator();

        // Don't bother supporting iterator-less sources, it'll just be a performance and logic nightmare
        if (iter == null) return 0;

        int itemsTransferred = 0, stacksTransferred = 0;

        InsertionItemStack insertion = new InsertionItemStack();

        outer: while (iter.hasNext() && stacksTransferred < stacksToTransfer
                && itemsTransferred < maxTotalTransferred) {
            ImmutableItemStack available = iter.next();

            if (available == null || available.isEmpty()) continue;

            if (filter != null && !filter.test(available)) continue;

            int availableCount = available.getStackSize();

            // Loop through this slot until we've transferred everything out of it that we can
            while (availableCount > 0) {
                if (itemsTransferred >= maxTotalTransferred) break outer;
                if (stacksTransferred >= stacksToTransfer) break outer;

                int remainingTransferAllowance = maxTotalTransferred - itemsTransferred;

                int toTransferThisOP = Math.min(remainingTransferAllowance, maxItemsPerTransfer);

                ItemStack extracted = iter.extract(Math.min(availableCount, toTransferThisOP), false);
                availableCount -= extracted.stackSize;

                // We couldn't extract anything, even though we should've been able to: go to the next source slot
                if (ItemUtil.isStackEmpty(extracted)) break;

                // This should never happen, but extract() might return a stack that doesn't match the request, so check
                // it again
                if (filter != null && !filter.test(extracted)) {
                    if (!ItemUtil.isStackEmpty(extracted)) {
                        availableCount += extracted.stackSize;

                        // Force insert the stack back into the source. This should only fail if another stack has ended
                        // up in this slot somehow (which is a bug), in which case we just pass it to `rejectedStacks`.
                        int rejected2 = iter.insert(insertion.set(extracted), true);

                        // If there isn't a rejectedStacks handler, the player is just SoL and the items are voided
                        if (rejected2 > 0 && rejectedStacks != null) {
                            rejectedStacks.accept(insertion.toStack(rejected2));
                        }
                    }

                    break;
                }

                // Try to insert the extracted stack
                int rejected = sink.store(insertion.set(extracted));

                if (rejected > 0) {
                    availableCount += rejected;

                    // Force insert the stack back into the source. This should only fail if another stack has ended up
                    // in this slot somehow (which is a bug), in which case we just pass it to `rejectedStacks`.
                    int rejected2 = iter.insert(insertion.set(extracted, rejected), true);

                    // If there isn't a rejectedStacks handler, the player is just SoL and the items are voided
                    if (rejected2 > 0 && rejectedStacks != null) {
                        rejectedStacks.accept(insertion.toStack(rejected2));
                    }
                }

                int transferred = extracted.stackSize - rejected;

                if (transferred <= 0) break;

                itemsTransferred += transferred;

                stacksTransferred++;
            }
        }

        totalItemsTransferred += itemsTransferred;
        totalStacksTransferred += stacksTransferred;
        prevItemsTransferred = itemsTransferred;
        prevStacksTransferred = stacksTransferred;

        return itemsTransferred;
    }
}
