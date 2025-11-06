package com.gtnewhorizon.gtnhlib.util.dynamicaabbtree;

public class DefaultAABBOverlapFilter<T extends Boundable & Identifiable> implements AABBOverlapFilter<T>
{
   @Override
   public boolean test(T t)
   {
      return true;
   }
}
