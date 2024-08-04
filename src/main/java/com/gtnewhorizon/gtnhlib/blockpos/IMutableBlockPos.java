package com.gtnewhorizon.gtnhlib.blockpos;

public interface IMutableBlockPos extends IBlockPos {

    IMutableBlockPos set(int x, int y, int z);

    IMutableBlockPos set(long packedPos);
}
