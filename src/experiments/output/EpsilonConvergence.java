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

/**
 *
 * @author Peter
 */
public class EpsilonConvergence implements Convergence {
    private double epsilon;
    
    public EpsilonConvergence(double epsilon) {
        this.epsilon = epsilon;
    }
    
    public int convergence(double[] signal) {
        double x0 = signal[0];
        
        double xmin = signal[0];
        for(int i = 1; i < signal.length; i++) {
            xmin = Math.min(xmin, signal[i]);
        }
        
        double threshold = xmin + (x0 - xmin)*epsilon;
        
        int iter;
        for(iter = 0; iter < signal.length; iter++) {
            if(signal[iter] <= threshold) {
                break;
            }
        }
        return iter;
    }
}
