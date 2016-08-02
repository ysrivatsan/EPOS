/*
 * Copyright (C) 2016 Evangelos Pournaras
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package experiments.output;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import org.jfree.chart.plot.DefaultDrawingSupplier;

/**
 *
 * @author Peter
 */
public class MyDrawingSupplier extends DefaultDrawingSupplier {
    
    //private static Paint[] paints = {new Color(237,125,49), new Color(91,155,213), new Color(255,192,0), new Color(165,165,165)};
    private static Paint[] paints = {Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA, Color.CYAN};
    private static Stroke[] strokes = {new BasicStroke(), new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 1, new float[]{4.0f,4.0f}, 1)};
    private static Shape[] shapes = {new Ellipse2D.Double()};
    
    public MyDrawingSupplier() {
        super(paints, paints, strokes, strokes, shapes);
    }
}
