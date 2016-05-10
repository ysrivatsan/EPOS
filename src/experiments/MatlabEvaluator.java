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
package experiments;

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
    
    public static void main(String[] args) {
        int id = Integer.parseInt(args[0]);
        List<String> experiments = new ArrayList<>();
        for(int i = 1; i < args.length; i++) {
            experiments.add(args[i]);
        }
        
        new MatlabEvaluator().evaluateLogs(id, experiments, System.out);
    }

    @Override
    void evaluate(int id, String title, String measure, List<String> labels, List<List<Aggregate>> iterationAggregates, PrintStream out) {
        printMatrix("XAvg" + id, iterationAggregates, a -> a.getAverage(), out);
        printMatrix("XMax" + id, iterationAggregates, a -> a.getMax(), out);
        printMatrix("XMin" + id, iterationAggregates, a -> a.getMin(), out);
        printMatrix("XStd" + id, iterationAggregates, a -> a.getStdDev(), out);
        //printMatrix("XNum" + id, iterationAggregates, a -> (double)a.getNumValues(), out);

        out.println("figure(" + id + ");");
        out.println("plot(XAvg" + id + "');");
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
}
