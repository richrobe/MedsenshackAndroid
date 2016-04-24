package de.fau.lme.plotview;

import android.graphics.Paint;

/**
 * Created by gradl on 13.10.2015.
 */
public class AxisPaint {
    public Paint defaultPaint;
    public Paint background;
    public Paint majorLine;
    public Paint ticks;
    public Paint title;
    public Paint text;

    public AxisPaint(Paint paint, Paint text) {
        defaultPaint = paint;
        background = paint;
        majorLine = paint;
        ticks = paint;
        title = text;
        this.text = text;

        background = Plot.generatePlotPaint(1f, 255, 192, 192, 192);
        background.setStyle(Paint.Style.FILL_AND_STROKE);
    }
}
