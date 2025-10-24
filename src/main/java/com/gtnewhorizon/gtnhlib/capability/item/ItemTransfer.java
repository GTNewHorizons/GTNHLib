package com.gtnewhorizon.gtnhlib.capability.item;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.blockpos.IBlockPos;
import com.gtnewhorizon.gtnhlib.blockpos.IWorldReferent;
import com.gtnewhorizon.gtnhlib.util.ItemUtil;

import lombok.Getter;
import lombok.Setter;

@SuppressWarnings({ "unused", "UnusedReturnValue" })
public class ItemTransfer {

    @Getter
    protected IItemSource source;
    @Getter
    protected IItemSink sink;
    protected IItemSink itemDropping;

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

    public void source(IItemSource source) {
        this.source = source;
    }

    public void source(Object source, ForgeDirection side) {
        this.source = ItemUtil.getItemSource(source, side, true);
    }

    public void sink(IItemSink sink) {
        this.sink = sink;
    }

    public void sink(Object sink, ForgeDirection side) {
        this.sink = ItemUtil.getItemSink(sink, side, true);
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
        IItemSink sink = this.sink == null ? itemDropping : this.sink;

        if (source == null) return 0;
        if (sink == null) return 0;
        if (stacksToTransfer == 0) return 0;
        if (maxItemsPerTransfer == 0) return 0;

        source.setAllowedSourceSlots(sourceSlots);
        sink.setAllowedSinkSlots(sinkSlots);

        InventorySourceIterator iter = source.iterator();

        int itemsTransferred = 0, stacksTransferred = 0;

        outer: while (iter.hasNext() && stacksTransferred < stacksToTransfer
                && itemsTransferred < maxTotalTransferred) {
            ImmutableItemStack available = iter.next();

            if (available == null) continue;

            ItemStack toExtract = available.toStack();

            if (ItemUtil.isStackEmpty(toExtract)) continue;

            if (filter != null && !filter.test(toExtract)) continue;

            // Loop through this slot until we've transferred everything out of it that we can
            while (toExtract.stackSize > 0) {
                if (itemsTransferred >= maxTotalTransferred) break outer;
                if (stacksTransferred >= stacksToTransfer) break outer;

                int remainingTransferAllowance = maxTotalTransferred - itemsTransferred;

                int toTransferThisOP = Math.min(remainingTransferAllowance, maxItemsPerTransfer);

                ItemStack extracted = iter.extract(Math.min(toExtract.stackSize, toTransferThisOP));
                toExtract.stackSize -= extracted.stackSize;

                // We couldn't extract anything, even though we should've been able to: go to the next source slot
                if (ItemUtil.isStackEmpty(extracted)) break;

                // This should never happen, but extract() might return a stack that doesn't match the request, so check
                // it again
                if (filter != null && !filter.test(extracted)) {
                    if (!ItemUtil.isStackEmpty(extracted)) {
                        iter.insert(extracted);
                    }

                    break;
                }

                ItemStack rejected = sink.store(ItemUtil.copy(extracted));

                if (!ItemUtil.isStackEmpty(rejected)) {
                    iter.insert(rejected);
                    toExtract.stackSize += rejected.stackSize;
                }

                int transferred = extracted.stackSize - (rejected == null ? 0 : rejected.stackSize);

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
