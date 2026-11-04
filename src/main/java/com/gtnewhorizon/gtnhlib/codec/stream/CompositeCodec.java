package com.gtnewhorizon.gtnhlib.codec.stream;

import java.util.function.Function;

final class CompositeCodec<B, C> implements StreamCodec<B, C> {

    private final StreamCodec<? super B, Object>[] codecs;
    private final Function<C, Object>[] getters;
    private final Function<Object[], C> assembler;

    @SuppressWarnings("unchecked")
    CompositeCodec(Function<Object[], C> assembler, Object... codecGetterPairs) {
        int count = codecGetterPairs.length / 2;
        this.codecs = new StreamCodec[count];
        this.getters = new Function[count];
        for (int field = 0; field < count; field++) {
            this.codecs[field] = (StreamCodec<? super B, Object>) codecGetterPairs[field * 2];
            this.getters[field] = (Function<C, Object>) codecGetterPairs[field * 2 + 1];
        }
        this.assembler = assembler;
    }

    @Override
    public C decode(B input) {
        Object[] fields = new Object[codecs.length];
        for (int field = 0; field < fields.length; field++) {
            fields[field] = codecs[field].decode(input);
        }
        return assembler.apply(fields);
    }

    @Override
    public void encode(B output, C value) {
        for (int field = 0; field < codecs.length; field++) {
            codecs[field].encode(output, getters[field].apply(value));
        }
    }
}
