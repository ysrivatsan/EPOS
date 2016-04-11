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

import dsutil.generic.RankPriority;
import dsutil.protopeer.services.topology.trees.DescriptorType;
import dsutil.protopeer.services.topology.trees.TreeType;
import java.util.function.IntFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import protopeer.Experiment;
import protopeer.Peer;
import protopeer.servers.bootstrap.SimplePeerIdentifierGenerator;
import tree.BalanceType;
import tree.centralized.client.TreeClient;
import tree.centralized.server.TreeServer;

/**
 *
 * @author Peter
 */
public abstract class NetworkArchitecture implements Cloneable {
    
    public abstract void addPeerlets(Peer peer, int peerIndex, int numNodes);
    
    @Override
    public NetworkArchitecture clone() {
        try {
            return (NetworkArchitecture) super.clone();
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(NetworkArchitecture.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
