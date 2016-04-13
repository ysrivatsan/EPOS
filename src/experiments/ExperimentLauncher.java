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
    
    public abstract IEPOSExperiment createExperiment(int num);
    
    public void run() {
        for(int i=0;i<numExperiments;i++){
            final IEPOSExperiment test = createExperiment(i);
            test.initEPOS();
            test.runSimulation(Time.inSeconds(runDuration));
            evaluateRun(i);
        }
    }
    
    public void evaluateRun(int i) {
    }
}
