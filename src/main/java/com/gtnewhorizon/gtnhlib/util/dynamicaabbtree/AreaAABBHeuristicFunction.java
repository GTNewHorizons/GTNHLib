package com.gtnewhorizon.gtnhlib.util.dynamicaabbtree;

import org.joml.primitives.AABBf;


public class AreaAABBHeuristicFunction<T extends Boundable> implements AABBTreeHeuristicFunction<T>
{
   private final AABBf temp;

   public AreaAABBHeuristicFunction()
   {
      temp = new AABBf();
   }

   @Override
   public HeuristicResult getInsertionHeuristic(AABBf left, AABBf right, T object, AABBf objectAABB)
   {
      float diffA = AABBUtils.getArea(left.union(objectAABB, temp)) - AABBUtils.getArea(left);
      float diffB = AABBUtils.getArea(right.union(objectAABB, temp)) - AABBUtils.getArea(right);
      return diffA < diffB ? HeuristicResult.LEFT : HeuristicResult.RIGHT;
   }
}
