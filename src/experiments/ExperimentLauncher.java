/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package experiments;

import agents.EPOSAgent;
import dsutil.generic.RankPriority;
import dsutil.protopeer.services.topology.trees.DescriptorType;
import dsutil.protopeer.services.topology.trees.TreeType;
import org.joda.time.DateTime;
import protopeer.util.quantities.Time;

/**
 *
 * @author Peter
 */
public abstract class ExperimentLauncher {
    //Simulation Parameters
    int treeInstances;
    int runDuration;
    

    public abstract EPOSExperiment createExperiment(int num);
    
    public final void run() {
        for(int i=0;i<treeInstances;i++){
            final EPOSExperiment test = createExperiment(i);
            test.initEPOS();
            test.runSimulation(Time.inSeconds(runDuration));
        }
    }
}
