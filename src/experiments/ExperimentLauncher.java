/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package experiments;

import protopeer.util.quantities.Time;

/**
 *
 * @author Peter
 */
public abstract class ExperimentLauncher {
    //Simulation Parameters
    int numExperiments;
    int runDuration;
    
    public abstract EPOSExperiment createExperiment(int num);
    
    public final void run() {
        for(int i=0;i<numExperiments;i++){
            final EPOSExperiment test = createExperiment(i);
            test.initEPOS();
            test.runSimulation(Time.inSeconds(runDuration));
            evaluateRun(i);
        }
    }
    
    public void evaluateRun(int i) {
    }
}
