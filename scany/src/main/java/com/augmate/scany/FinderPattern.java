package com.augmate.scany;

import com.google.zxing.ResultPoint;

public final class FinderPattern extends ResultPoint {

    private final float estimatedModuleSize;
    private final int count;

    FinderPattern(float posX, float posY, float estimatedModuleSize) {
        this(posX, posY, estimatedModuleSize, 1);
    }

    private FinderPattern(float posX, float posY, float estimatedModuleSize, int count) {
        super(posX, posY);
        this.estimatedModuleSize = estimatedModuleSize;
        this.count = count;
    }

    public float getEstimatedModuleSize() {
        return estimatedModuleSize;
    }

    public int getCount() {
        return count;
    }

  /*
  void incrementCount() {
    this.count++;
  }
   */

    /**
     * <p>Determines if this finder pattern "about equals" a finder pattern at the stated
     * position and size -- meaning, it is at nearly the same center with nearly the same size.</p>
     */
    public boolean aboutEquals(float moduleSize, float i, float j) {
        if (Math.abs(i - getY()) <= moduleSize && Math.abs(j - getX()) <= moduleSize) {
            float moduleSizeDiff = Math.abs(moduleSize - estimatedModuleSize);
            return moduleSizeDiff <= 1.0f || moduleSizeDiff <= estimatedModuleSize;
        }
        return false;
    }

    /**
     * Combines this object's current estimate of a finder pattern position and module size
     * with a new estimate. It returns a new {@code FinderPattern} containing a weighted average
     * based on count.
     */
    FinderPattern combineEstimate(float i, float j, float newModuleSize) {
        int combinedCount = count + 1;
        float combinedX = (count * getX() + j) / combinedCount;
        float combinedY = (count * getY() + i) / combinedCount;
        float combinedModuleSize = (count * estimatedModuleSize + newModuleSize) / combinedCount;
        return new FinderPattern(combinedX, combinedY, combinedModuleSize, combinedCount);
    }

}