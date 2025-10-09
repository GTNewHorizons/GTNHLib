package com.gtnewhorizon.gtnhlib.capability.item;

public interface IItemIO extends IItemSource, IItemSink {

    default IItemIO then(IItemSink next) {
        return new WrappedItemIO(this, IItemSink.chain(this, next));
    }
}
