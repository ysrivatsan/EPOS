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
package agents.fitnessFunction;

import agents.fitnessFunction.iterative.PlanCombinator;
import agents.fitnessFunction.iterative.Factor;
import agents.Agent;
import agents.plan.Plan;
import agents.AgentPlans;
import agents.fitnessFunction.costFunction.IterativeCostFunction;
import java.util.List;

/**
 * minimize variance (submodular/convex compared to std deviation) weight B
 * according to optimum without aggregate and equal Bi
 *
 * @author Peter
 */
public class IterMinCostAdamGmA extends IterMinCost {

    private final Factor factor;
    
    private final double alpha = 0.1;
    private final double beta1 = 0.0; //0.6
    private final double beta2 = 0.5; //0.999
    private final double epsilon = 1e-8;
    private Plan theta;
    private Plan m;
    private Plan v;
    
    /* Adam algorithm:
    Recommended in the paper (for their ML applications)
    theta0 ... initial iterative cost
    alpha = 0.001
    beta1 = 0.9
    beta2 = 0.999
    epsilon = 1e-8

    m0 = 0
    v0 = 0
    t = 0
    while theta_t not converged do
        t = t+1
        gt = gradient(theta_(t-1))
        m_t = beta1*m_(t-1)+(1-beta1)*gt
        v_t = beta2*v_(t-1)+(1-beta2)*gt.^2
        mhat_t = m_t/(1-beta1^t)
        vhat_t = v_t/(1-beta2^t)
        theta_t = theta_(t-1) - alpha*mhat_t/(sqrt(vhat_t)+epsilon)
    end
    */
    
    public IterMinCostAdamGmA(IterativeCostFunction costFunc, Factor factor) {
        super(costFunc);
        this.factor = factor;
    }

    @Override
    public void afterIteration(AgentPlans current, Plan costSignal, int iteration, int numNodes) {
        Plan p = current.global.clone();
        p.subtract(current.aggregate);
        
        int t = iteration + 1;
        
        Plan g = costFunc.calcGradient(p, costSignal);
        
        Plan g1 = g.clone();
        g1.multiply(1-beta1);
        if(m == null) {
            m = g1;
        } else {
            m.multiply(beta1);
            m.add(g1);
        }
        
        Plan g2 = g.clone();
        g2.pow(2);
        g2.multiply(1-beta2);
        if(v == null) {
            v = g2;
        } else {
            v.multiply(beta2);
            v.add(g2);
        }
        
        Plan mhat = m.clone();
        mhat.multiply(1/(1-Math.pow(beta1, t)));
        
        Plan vhat = v.clone();
        vhat.multiply(1/(1-Math.pow(beta2, t)));
        
        //theta_t = theta_(t-1) - alpha*mhat_t/(sqrt(vhat_t)+epsilon)
        vhat.pow(0.5);
        vhat.add(epsilon);
        vhat.pow(-1);
        mhat.multiply(vhat);
        //mhat.multiply(alpha);
        
        // we want to ascent (add penalty) -> add instead of subtract
        if(theta == null) {
            theta = mhat;
        } else {
            theta.add(mhat);
        }
    }

    @Override
    public int select(Agent agent, Plan aggregate, List<Plan> plans, Plan costSignal, int numNodes, int numNodesSubtree, int layer, double avgChildren, int iteration) {
        Plan iterativeCost = null;
        
        if (iteration > 0 && theta != null) {
            iterativeCost = theta.clone();
            double alpha = factor.calcFactor(iterativeCost, plans, numNodes, numNodesSubtree, layer, avgChildren);
            iterativeCost.multiply(alpha);
        }
        
        return select(agent, aggregate, plans, costSignal, iterativeCost);
    }

    @Override
    public String toString() {
        return "IterMinCostAdamGmA " + factor;
    }
}
