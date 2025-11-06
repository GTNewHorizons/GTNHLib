package com.gtnewhorizon.gtnhlib.util.dynamicaabbtree;

import org.joml.primitives.AABBf;

/**
 * Created by pateman.
 */
public interface Boundable {

  AABBf getAABB(AABBf dest);
}
