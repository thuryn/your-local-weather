package org.thosp.yourlocalweather.utils;

import org.thosp.charting.components.XAxis;
import org.thosp.charting.renderer.XAxisRenderer;
import org.thosp.charting.utils.Transformer;
import org.thosp.charting.utils.ViewPortHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Custom XAxisRenderer that places X-axis ticks at the exact midday timestamps
 * instead of relying on MPAndroidChart's evenly-spaced interval calculation
 * (which clamps label counts to 25 by default and can skip days).
 */
public class CustomXAxisRenderer extends XAxisRenderer {

    private final List<Float> sortedLabelPositions;

    public CustomXAxisRenderer(ViewPortHandler viewPortHandler,
                               XAxis xAxis,
                               Transformer trans,
                               Set<Long> labelTimestamps) {
        super(viewPortHandler, xAxis, trans);
        if (labelTimestamps == null || labelTimestamps.isEmpty()) {
            this.sortedLabelPositions = Collections.emptyList();
        } else {
            List<Long> sorted = new ArrayList<>(labelTimestamps);
            Collections.sort(sorted);
            this.sortedLabelPositions = new ArrayList<>(sorted.size());
            for (Long ts : sorted) {
                if (ts != null) {
                    this.sortedLabelPositions.add(ts.floatValue());
                }
            }
        }
    }

    @Override
    protected void computeAxisValues(float min, float max) {
        if (sortedLabelPositions.isEmpty()) {
            super.computeAxisValues(min, max);
            return;
        }
        List<Float> visible = new ArrayList<>(sortedLabelPositions.size());
        for (Float pos : sortedLabelPositions) {
            if (pos >= min && pos <= max) {
                visible.add(pos);
            }
        }
        mXAxis.mEntries = new float[visible.size()];
        for (int i = 0; i < visible.size(); i++) {
            mXAxis.mEntries[i] = visible.get(i);
        }
        mXAxis.mEntryCount = visible.size();
        mXAxis.mCenteredEntries = new float[0];
        computeSize();
    }
}
