package com.gtnewhorizon.gtnhlib.util.dynamicaabbtree;

import org.joml.primitives.AABBf;

@FunctionalInterface
public interface AABBTreeHeuristicFunction<T extends Boundable>
{
   HeuristicResult getInsertionHeuristic(AABBf left, AABBf right, T object, AABBf objectAABB);

   enum HeuristicResult {
      LEFT,
      RIGHT
   }
}
