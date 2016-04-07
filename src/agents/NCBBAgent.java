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

import agents.plan.Plan;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import messages.NCBBCost;
import messages.NCBBSearch;
import messages.NCBBStop;
import messages.NCBBYD;
import org.joda.time.DateTime;
import protopeer.Finger;
import protopeer.measurement.MeasurementLog;
import protopeer.network.Message;
import protopeer.network.NetworkAddress;

/**
 *
 * @author Peter
 */
public class NCBBAgent extends Agent {

    public NCBBAgent(String experimentID, String plansLocation, String planConfigurations, String treeStamp, String agentMeterID, DateTime initialPhase, String plansFormat, int planSize, Plan costSignal) {
        super(plansLocation, planConfigurations, treeStamp, agentMeterID, initialPhase, plansFormat, planSize);
    }

    @Override
    void runPhase() {

    }

    @Override
    void measure(MeasurementLog log, int epochNumber) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void handleIncomingMessage(Message message) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
/*
    private Map<Plan, Double> costs;
    private Plan resultValue;
    private double bound;
    private Map<NetworkAddress, Plan> context = new HashMap<>();
    private Map<NetworkAddress, Plan> anncdVals = new HashMap<>();

    private BlockingQueue<Message> messageBuffer = new LinkedBlockingQueue<>();

    private void init() {
        bound = 0.0;
    }
    
    private void mainLoop() throws InterruptedException {
        if (!isRoot()) {
            updateContext();
        }
        while (true) {
            search();
            if (isRoot() || updateContext()) {
                break;
            }
        }
        costs.put(resultValue, 0.0);
        for (Finger y : children) {
            subtreeSearch();
        }
        for (Finger y : children) {
            NCBBStop msg = new NCBBStop();
            getPeer().sendMessage(y.getNetworkAddress(), msg);
        }
    }

    private boolean updateContext() throws InterruptedException {
        while (true) {
            Message msg = messageBuffer.poll(10, TimeUnit.SECONDS);
            if (msg instanceof NCBBSearch) {
                NCBBSearch m = (NCBBSearch) msg;
                bound = m.bound;
                return false;
            } else if (msg instanceof NCBBYD) {
                NCBBYD m = (NCBBYD) msg;
                context.put(m.y, m.d);
                
                NCBBCost response = new NCBBCost();
                response.cost = 0.0;
                getPeer().sendMessage(m.y, response);
            } else if (msg instanceof NCBBStop) {
                return true;
            }
        }
    }

    private void search() {
        List<Finger> idle = new ArrayList<>(children);
        costs.clear();
        Map<Double,Double> unexplored = new HashMap<>();
        anncdVals.clear();
        double minCost = LB(getPeer().getNetworkAddress(),context,indx);
    }

    private void subtreeSearch() {

    }
    
    private double LB(NetworkAddress x, Map<NetworkAddress, Plan> context, int k) {
        return 0.0;
    }*/
}
