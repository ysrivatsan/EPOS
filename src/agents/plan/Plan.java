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
package agents.plan;

import agents.Agent;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import org.joda.time.DateTime;

/**
 *
 * @author Peter
 */
public class Plan implements Serializable {

    private double[] values;
    private Type type;
    private DateTime coordinationPhase;
    private double discomfort;
    private String agentMeterID;
    private String configuration;

    protected static enum Type {
        POSSIBLE_PLAN,
        COMBINATIONAL_PLAN,
        AGGREGATE_PLAN,
        GLOBAL_PLAN
    }

    public Plan() {
    }

    public Plan(Plan.Type type, Agent agent) {
        this.setType(type);
        agent.initPlan(this);
    }

    public Plan(Plan.Type type, Agent agent, String planStr) {
        this.setType(type);
        agent.initPlan(this, planStr);
    }
    
    public void init(int length) {
        values = new double[length];
    }
    
    public void setValue(int idx, double value) {
        values[idx] = value;
    }
    
    /**
     * not recommended - generates new copy of the array
     * @param value 
     */
    public void addValue(double value) {
        values = Arrays.copyOf(values, values.length+1);
        values[values.length-1] = value;
    }
    
    public double getValue(int idx) {
        return values[idx];
    }

    // property getter/setter
    public Type getType() {
        return type;
    }

    private void setType(Type type) {
        this.type = type;
    }

    public DateTime getCoordinationPhase() {
        return coordinationPhase;
    }

    public void setCoordinationPhase(DateTime coordinationPhase) {
        this.coordinationPhase = coordinationPhase;
    }

    public double getDiscomfort() {
        return discomfort;
    }

    public void setDiscomfort(double discomfort) {
        this.discomfort = discomfort;
    }

    public String getAgentMeterID() {
        return agentMeterID;
    }

    public void setAgentMeterID(String agentMeterID) {
        this.agentMeterID = agentMeterID;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String config) {
        this.configuration = config;
    }
    
    public int getNumberOfStates() {
        return values.length;
    }

    // Operations
    public double sum() {
        double sum = 0.0;
        for (double state : values) {
            sum += state;
        }
        return sum;
    }

    public double avg() {
        return sum() / values.length;
    }

    public double dot(Plan other) {
        double dot = 0;
        for (int i = 0; i < values.length; i++) {
            dot += values[i] * other.values[i];
        }
        return dot;
    }

    public double entropy() {
        double sum = sum();
        double entropy = 0.0;
        for (double state : values) {
            double p = state / sum;
            if (p == 0.0) {
                entropy += 0.0;
            } else {
                entropy += p * Math.log(p);
            }
        }
        return -entropy;
    }

    public double stdDeviation() {
        double average = avg();
        double sumSquare = 0.0;
        for (double state : values) {
            sumSquare += Math.pow((state - average), 2.0);
        }
        double variance = sumSquare / values.length;
        double stdDev = Math.sqrt(variance);
        return stdDev;
    }

    public double variance() {
        double average = avg();
        double sumSquare = 0.0;
        for (double state : values) {
            sumSquare += Math.pow((state - average), 2.0);
        }
        return sumSquare / (values.length - 1);
    }

    public double relativeStdDeviation() {
        double average = avg();
        double sumSquare = 0.0;
        for (double state : values) {
            sumSquare += Math.pow((state - average), 2.0);
        }
        double variance = sumSquare / values.length;
        double stDev = Math.sqrt(variance);
        return stDev / average;
    }

    public double max() {
        double maximum = Double.MIN_VALUE;
        for (double state : values) {
            if (state > maximum) {
                maximum = state;
            }
        }
        return maximum;
    }

    public double min() {
        double minimum = Double.MAX_VALUE;
        for (double state : values) {
            if (state < minimum) {
                minimum = state;
            }
        }
        return minimum;
    }

    /**
     * Computes the correlation coefficient of two energy plans
     *
     * @param energyPlanX the second energy plan used for the computation of the
     * correlation coefficient
     * @return the correlation coefficient
     */
    public double correlationCoefficient(Plan other) {
        double[] energyPlanX = values;
        double[] energyPlanY = other.values;
//        double corrCoeff=Double.NaN;
//        int n=energyPlanX.size();
//        double x=0.0;
//        double y=0.0;
//        double xy=0.0;
//        double x2=0.0;
//        double y2=0.0;
//        double sqrt01=0.0;
//        double sqrt02=0.0;
//        for(ArithmeticState state01:energyPlanX){
//            x+=state01.getValue();
//        }
//        for(ArithmeticState state02:energyPlanY){
//            y+=state02.getValue();
//        }
//        for(int i=0;i<n;i++){
//            xy+=energyPlanX.get(i).getValue()*energyPlanY.get(i).getValue();
//        }
//        for(ArithmeticState state01:energyPlanX){
//            x2+=Math.pow(state01.getValue(), 2.0);
//        }
//        for(ArithmeticState state02:energyPlanY){
//            y2+=Math.pow(state02.getValue(), 2.0);
//        }
//        sqrt01=Math.sqrt(n*x2-Math.pow(x,2.0));
//        sqrt02=Math.sqrt(n*y2-Math.pow(y,2.0));
//        corrCoeff=(n*xy-x*y)/(sqrt01*sqrt02);
//        return corrCoeff;
        double result = 0;
        double sum_sq_x = 0;
        double sum_sq_y = 0;
        double sum_coproduct = 0;
        double mean_x = energyPlanX[0];
        double mean_y = energyPlanY[0];
        for (int i = 2; i < energyPlanX.length + 1; i += 1) {
            double sweep = Double.valueOf(i - 1) / i;
            double delta_x = energyPlanX[i - 1] - mean_x;
            double delta_y = energyPlanY[i - 1] - mean_y;
            sum_sq_x += delta_x * delta_x * sweep;
            sum_sq_y += delta_y * delta_y * sweep;
            sum_coproduct += delta_x * delta_y * sweep;
            mean_x += delta_x / i;
            mean_y += delta_y / i;
        }
        double pop_sd_x = (double) Math.sqrt(sum_sq_x / energyPlanX.length);
        double pop_sd_y = (double) Math.sqrt(sum_sq_y / energyPlanY.length);
        double cov_x_y = sum_coproduct / energyPlanX.length;
        result = cov_x_y / (pop_sd_x * pop_sd_y);
        return result;
    }

    public double rootMeanSquareError(Plan other) {
        double[] energyPlanX = values;
        double[] energyPlanY = other.values;
        double squaredError = 0;
        for (int i = 0; i < energyPlanX.length; i++) {
            squaredError += Math.pow(energyPlanX[i] - energyPlanY[i], 2);
        }
        double meanSquaredError = squaredError / energyPlanX.length;
        double rootMeanSquaredError = Math.sqrt(meanSquaredError);
        return rootMeanSquaredError;
    }

    public void set(Plan other) {
        System.arraycopy(other.values, 0, values, 0, values.length);
        this.setDiscomfort(other.getDiscomfort());
    }

    public void set(double value) {
        Arrays.fill(values, value);
    }

    public void add(Plan other) {
        for (int i = 0; i < values.length; i++) {
            values[i] += other.values[i];
        }
    }

    public void add(double value) {
        for (int i = 0; i < values.length; i++) {
            values[i] += value;
        }
    }

    public void subtract(Plan other) {
        for (int i = 0; i < values.length; i++) {
            values[i] -= other.values[i];
        }
    }

    public void subtract(double value) {
        for (int i = 0; i < values.length; i++) {
            values[i] -= value;
        }
    }

    public void multiply(Plan other) {
        for (int i = 0; i < values.length; i++) {
            values[i] *= other.values[i];
        }
    }

    public void multiply(double factor) {
        for (int i = 0; i < values.length; i++) {
            values[i] *= factor;
        }
    }
    
    public void pow(double x) {
        for (int i = 0; i < values.length; i++) {
            values[i] = Math.pow(values[i], x);
        }
    }

    public void reverse() {
        double average = avg();
        for (int i = 0; i < values.length; i++) {
            values[i] = 2 * average - values[i];
        }
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Plan)) {
            return false;
        }
        final Plan other = (Plan) obj;
        for (int i = 0; i < values.length; i++) {
            if (values[i] != other.values[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append('[');
        if (values.length > 0) {
            out.append(values[0]);
        }
        for (int i = 1; i < values.length; i++) {
            out.append(',');
            out.append(values[i]);
        }
        out.append(']');
        return out.toString();
    }
}
