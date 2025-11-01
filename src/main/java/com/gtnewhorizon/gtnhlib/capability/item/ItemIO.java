package com.gtnewhorizon.gtnhlib.capability.item;

import com.gtnewhorizon.gtnhlib.item.WrappedItemIO;

public interface ItemIO extends ItemSource, ItemSink {

    default ItemIO then(ItemSink next) {
        return new WrappedItemIO(this, ItemSink.chain(this, next));
    }
}
