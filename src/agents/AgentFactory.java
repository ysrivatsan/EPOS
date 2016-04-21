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
package agents;

import agents.dataset.FileAgentDataset;
import agents.plan.Plan;
import agents.fitnessFunction.FitnessFunction;
import agents.fitnessFunction.costFunction.CostFunction;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.joda.time.DateTime;
import agents.dataset.AgentDataset;

/**
 *
 * @author Peter
 */
public abstract class AgentFactory implements Cloneable {
    public int numIterations;
    public FitnessFunction fitnessFunction;
    public LocalSearch localSearch;
    public List<CostFunction> measures = new ArrayList<>();
    
    public abstract Agent create(AgentDataset dataSource, String treeStamp, File outFolder, DateTime initialPhase, DateTime previousPhase, Plan costSignal, int historySize);
    
    @Override
    public AgentFactory clone() throws CloneNotSupportedException {
        AgentFactory f = (AgentFactory) super.clone();
        f.measures = new ArrayList<>(measures);
        return f;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + this.numIterations;
        hash = 67 * hash + Objects.hashCode(this.fitnessFunction);
        hash = 67 * hash + Objects.hashCode(this.localSearch);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AgentFactory other = (AgentFactory) obj;
        if (this.numIterations != other.numIterations) {
            return false;
        }
        if (!Objects.equals(this.fitnessFunction, other.fitnessFunction)) {
            return false;
        }
        if (!Objects.equals(this.localSearch, other.localSearch)) {
            return false;
        }
        return true;
    }
}
