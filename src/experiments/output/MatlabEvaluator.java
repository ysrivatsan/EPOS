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

import experiments.output.IEPOSEvaluator;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import protopeer.measurement.Aggregate;

/**
 *
 * @author Peter
 */
public class MatlabEvaluator extends IEPOSEvaluator {

    @Override
    void evaluate(int id, String title, String measure, List<String> labels, List<List<Aggregate>> iterationAggregates, PrintStream out) {
        int num = iterationAggregates.size();
        String xavg = "XAvg" + id;
        String xmax = "XMax" + id;
        String xmin = "XMin" + id;
        String xstd = "XStd" + id;
        String xcon = "XCon" + id;
        
        printMatrix(xavg, iterationAggregates, a -> a.getAverage(), out);
        printMatrix(xmax, iterationAggregates, a -> a.getMax(), out);
        printMatrix(xmin, iterationAggregates, a -> a.getMin(), out);
        printMatrix(xstd, iterationAggregates, a -> a.getStdDev(), out);
        //printMatrix("XNum" + id, iterationAggregates, a -> (double)a.getNumValues(), out);
        
        printConvergence(xcon, iterationAggregates, out);
        
        StringBuilder indexFix = new StringBuilder();
        for(int i = num-1; i > 0; i--) {
            indexFix.append('-');
            indexFix.append(i);
            indexFix.append(';');
        }
        indexFix.append(0);

        out.println("figure(" + id + ");");
        out.println("plot(XAvg" + id + "');");
        out.println("hold on; plot(" + xcon + "," + xavg + "(" + xcon + "*" + num + "+["+indexFix+"]),'ko'); hold off;");
        out.println("xlabel('iteration');");
        out.println("ylabel('" + measure + "');");
        out.print("legend('" + toMatlabString(labels.get(0)) + "'");
        for (int i = 1; i < labels.size(); i++) {
            out.print(",'" + toMatlabString(labels.get(i)) + "'");
        }
        out.println(");");
        out.println("title('" + toMatlabString(title) + "');");
    }

    private String toMatlabString(String str) {
        return str.replace("_", "\\_");
    }

    private void printMatrix(String name, List<List<Aggregate>> iterationAggregates, Function<Aggregate, Double> function, PrintStream out) {
        out.println(name + " = [");
        for (List<Aggregate> log : iterationAggregates) {
            out.print(function.apply(log.get(0)));
            for (int i = 1; i < log.size(); i++) {
                double val = function.apply(log.get(i));
                if(val == Double.POSITIVE_INFINITY) {
                    out.print(", inf");
                } else if(val == Double.NEGATIVE_INFINITY) {
                    out.print(", -inf");
                } else {
                    out.print(", " + val);
                }
            }
            out.println(";");
        }
        out.println("];");
    }
    
    private void printConvergence(String name, List<List<Aggregate>> iterationAggregates, PrintStream out) {
        Convergence con = new EpsilonConvergence(0.1);
        out.print(name + " = [");
        for (List<Aggregate> log : iterationAggregates) {
            double[] signal = new double[log.size()];
            for(int i = 0; i < signal.length; i++) {
                signal[i] = log.get(i).getAverage();
            }
            int convergence = con.convergence(signal);
            out.print(convergence + " ");
        }
        out.println("]';");
    }
}
