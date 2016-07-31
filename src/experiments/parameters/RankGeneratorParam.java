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
package experiments.parameters;

import agents.Agent;
import agents.network.*;
import java.util.Random;
import java.util.function.BiFunction;

/**
 *
 * @author Peter
 */
public class RankGeneratorParam extends MapParam<BiFunction<Integer, Agent, Double>> {

    public RankGeneratorParam() {
        map.put("RandomRank", (idx, agent) -> agent.getRandom().nextDouble());
        map.put("IndexRank", (idx, agent) -> (double) idx);
        map.put("StdRank", new StdRankGenerator());
        map.put("NumPlanRank", new NumPlanRankGenerator());
        map.put("SparsityRank", new SparsityRankGenerator());
        map.put("NumPlanStdRank", new NumPlanStdRankGenerator());
        map.put("NumPlanSparsityStdRank", new NumPlanSparsityStdRankGenerator());
        map.put("NumPlanInvSparsityStdRank", new NumPlanInvSparsityStdRankGenerator());
    }
}
