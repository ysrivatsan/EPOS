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
import agents.EPOSAgent;
import dsutil.generic.state.ArithmeticListState;
import dsutil.generic.state.ArithmeticState;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.joda.time.DateTime;

/**
 *
 * @author Peter
 */
public class PlanOld extends ArithmeticListState {

    protected static enum Type {
        POSSIBLE_PLAN,
        COMBINATIONAL_PLAN,
        AGGREGATE_PLAN,
        GLOBAL_PLAN
    }

    private static enum Information {
        TYPE,
        COORDINATION_PHASE,
        DISCOMFORT,
        AGENT_METER_ID,
        CONFIGURATION
    }
    
    public PlanOld() {
        super(new ArrayList<>());
    }

    public PlanOld(PlanOld.Type type, Agent agent) {
        super(new ArrayList<>());
        this.setType(type);
        //agent.initPlan2(this);
    }
    
    public PlanOld(PlanOld.Type type, Agent agent, String planStr) {
        super(new ArrayList<>());
        this.setType(type);
        //agent.initPlan2(this, planStr);
    }

    // property getter/setter
    public Type getType() {
        return (Type) this.getProperty(Information.TYPE);
    }

    private void setType(Type type) {
        this.addProperty(Information.TYPE, type);
    }

    public DateTime getCoordinationPhase() {
        return (DateTime) this.getProperty(Information.COORDINATION_PHASE);
    }

    public void setCoordinationPhase(DateTime coordinationPhase) {
        this.addProperty(Information.COORDINATION_PHASE, coordinationPhase);
    }

    public double getDiscomfort() {
        return (Double) this.getProperty(Information.DISCOMFORT);
    }

    public void setDiscomfort(double discomfort) {
        this.addProperty(Information.DISCOMFORT, discomfort);
    }

    public String getAgentMeterID() {
        return (String) this.getProperty(Information.AGENT_METER_ID);
    }

    public void setAgentMeterID(String agentMeterID) {
        this.addProperty(Information.AGENT_METER_ID, agentMeterID);
    }

    public String getConfiguration() {
        return (String) this.getProperty(Information.CONFIGURATION);
    }

    public void setConfiguration(String config) {
        this.addProperty(Information.CONFIGURATION, config);
    }

    // Operations
    public double sum() {
        double sum = 0.0;
        for (ArithmeticState state : getArithmeticStates()) {
            sum += state.getValue();
        }
        return sum;
    }

    public double avg() {
        return sum() / getArithmeticStates().size();
    }
    
    public double dot(PlanOld other) {
        double dot = 0;
        for(int i=0; i<getNumberOfStates(); i++) {
            dot += getArithmeticState(i).getValue() * other.getArithmeticState(i).getValue();
        }
        return dot;
    }

    public double entropy() {
        double sum = sum();
        double entropy = 0.0;
        for (ArithmeticState state : getArithmeticStates()) {
            double p = state.getValue() / sum;
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
        for (ArithmeticState state : getArithmeticStates()) {
            sumSquare += Math.pow((state.getValue() - average), 2.0);
        }
        double variance = sumSquare / getArithmeticStates().size();
        double stdDev = Math.sqrt(variance);
        return stdDev;
    }

    public double variance() {
        double average = avg();
        double sumSquare = 0.0;
        for (ArithmeticState state : getArithmeticStates()) {
            sumSquare += Math.pow((state.getValue() - average), 2.0);
        }
        return sumSquare / (getArithmeticStates().size()-1);
    }

    public double relativeStdDeviation() {
        double average = avg();
        double sumSquare = 0.0;
        for (ArithmeticState state : getArithmeticStates()) {
            sumSquare += Math.pow((state.getValue() - average), 2.0);
        }
        double variance = sumSquare / getArithmeticStates().size();
        double stDev = Math.sqrt(variance);
        return stDev / average;
    }

    public double max() {
        double maximum = Double.MIN_VALUE;
        for (ArithmeticState state : getArithmeticStates()) {
            if (state.getValue() > maximum) {
                maximum = state.getValue();
            }
        }
        return maximum;
    }

    public double min() {
        double minimum = Double.MAX_VALUE;
        for (ArithmeticState state : getArithmeticStates()) {
            if (state.getValue() < minimum) {
                minimum = state.getValue();
            }
        }
        return minimum;
    }

    /**
     * Computes the correlation coefficient of two energy plans
     *
     * @param energyPlan2X the second energy plan used for the computation of the
     * correlation coefficient
     * @return the correlation coefficient
     */
    public double correlationCoefficient(PlanOld other) {
        List<ArithmeticState> energyPlan2X = this.getArithmeticStates();
        List<ArithmeticState> energyPlan2Y = other.getArithmeticStates();
//        double corrCoeff=Double.NaN;
//        int n=energyPlan2X.size();
//        double x=0.0;
//        double y=0.0;
//        double xy=0.0;
//        double x2=0.0;
//        double y2=0.0;
//        double sqrt01=0.0;
//        double sqrt02=0.0;
//        for(ArithmeticState state01:energyPlan2X){
//            x+=state01.getValue();
//        }
//        for(ArithmeticState state02:energyPlan2Y){
//            y+=state02.getValue();
//        }
//        for(int i=0;i<n;i++){
//            xy+=energyPlan2X.get(i).getValue()*energyPlan2Y.get(i).getValue();
//        }
//        for(ArithmeticState state01:energyPlan2X){
//            x2+=Math.pow(state01.getValue(), 2.0);
//        }
//        for(ArithmeticState state02:energyPlan2Y){
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
        double mean_x = energyPlan2X.get(0).getValue();
        double mean_y = energyPlan2Y.get(0).getValue();
        for (int i = 2; i < energyPlan2X.size() + 1; i += 1) {
            double sweep = Double.valueOf(i - 1) / i;
            double delta_x = energyPlan2X.get(i - 1).getValue() - mean_x;
            double delta_y = energyPlan2Y.get(i - 1).getValue() - mean_y;
            sum_sq_x += delta_x * delta_x * sweep;
            sum_sq_y += delta_y * delta_y * sweep;
            sum_coproduct += delta_x * delta_y * sweep;
            mean_x += delta_x / i;
            mean_y += delta_y / i;
        }
        double pop_sd_x = (double) Math.sqrt(sum_sq_x / energyPlan2X.size());
        double pop_sd_y = (double) Math.sqrt(sum_sq_y / energyPlan2Y.size());
        double cov_x_y = sum_coproduct / energyPlan2X.size();
        result = cov_x_y / (pop_sd_x * pop_sd_y);
        return result;
    }

    public double rootMeanSquareError(PlanOld other) {
        List<ArithmeticState> energyPlan2X = this.getArithmeticStates();
        List<ArithmeticState> energyPlan2Y = other.getArithmeticStates();
        double squaredError = 0;
        for (int i = 0; i < energyPlan2X.size(); i++) {
            squaredError += Math.pow(energyPlan2X.get(i).getValue() - energyPlan2Y.get(i).getValue(), 2);
        }
        double meanSquaredError = squaredError / energyPlan2X.size();
        double rootMeanSquaredError = Math.sqrt(meanSquaredError);
        return rootMeanSquaredError;
    }
    
    public void set(PlanOld other) {
        for (int i = 0; i < this.getNumberOfStates(); i++) {
            this.setArithmeticState(i, other.getArithmeticState(i).getValue());
        }
        this.setDiscomfort(other.getDiscomfort());
    }

    public void add(PlanOld other) {
        for (int i = 0; i < this.getNumberOfStates(); i++) {
            double aggregateConsumption = this.getArithmeticState(i).getValue() + other.getArithmeticState(i).getValue();
            this.setArithmeticState(i, aggregateConsumption);
        }
    }

    public void add(double value) {
        for (int i = 0; i < this.getNumberOfStates(); i++) {
            double aggregateConsumption = this.getArithmeticState(i).getValue() + value;
            this.setArithmeticState(i, aggregateConsumption);
        }
    }

    public void subtract(PlanOld other) {
        for (int i = 0; i < this.getNumberOfStates(); i++) {
            double aggregateConsumption = this.getArithmeticState(i).getValue() - other.getArithmeticState(i).getValue();
            this.setArithmeticState(i, aggregateConsumption);
        }
    }

    public void subtract(double value) {
        for (int i = 0; i < this.getNumberOfStates(); i++) {
            double aggregateConsumption = this.getArithmeticState(i).getValue() - value;
            this.setArithmeticState(i, aggregateConsumption);
        }
    }

    public void multiply(PlanOld other) {
        for (int i = 0; i < this.getNumberOfStates(); i++) {
            double aggregateConsumption = this.getArithmeticState(i).getValue() * other.getArithmeticState(i).getValue();
            this.setArithmeticState(i, aggregateConsumption);
        }
    }

    public void multiply(double factor) {
        for (int i = 0; i < this.getNumberOfStates(); i++) {
            double aggregateConsumption = this.getArithmeticState(i).getValue() * factor;
            this.setArithmeticState(i, aggregateConsumption);
        }
    }

    public void reverse() {
        double average = avg();
        for (int i = 0; i < this.getNumberOfStates(); i++) {
            double value = this.getArithmeticState(i).getValue();
            value = 2*average - value;
            this.setArithmeticState(i, value);
        }
    }
    
    
    private boolean isEqual(ArithmeticListState planA, ArithmeticListState planB) {
        for (int i = 0; i < planA.getArithmeticStates().size(); i++) {
            if (planA.getArithmeticState(i).getValue() != planB.getArithmeticState(i).getValue()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return getArithmeticStates().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof PlanOld)) {
            return false;
        }
        final PlanOld other = (PlanOld) obj;
        for (int i = 0; i < this.getArithmeticStates().size(); i++) {
            if (this.getArithmeticState(i).getValue() != other.getArithmeticState(i).getValue()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append('[');
        Iterator<ArithmeticState> iter = getArithmeticStates().iterator();
        if(iter.hasNext()) {
            out.append(iter.next().getValue());
        }
        while(iter.hasNext()) {
            out.append(',');
            out.append(iter.next().getValue());
        }
        out.append(']');
        return out.toString();
    }
}
