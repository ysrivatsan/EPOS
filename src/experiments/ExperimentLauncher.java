/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package experiments;

import java.io.File;
import java.io.IOException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import protopeer.Peer;
import protopeer.measurement.LogReplayer;
import protopeer.measurement.MeasurementFileDumper;
import protopeer.measurement.MeasurementLog;
import protopeer.util.quantities.Time;

/**
 *
 * @author Peter
 */
public abstract class ExperimentLauncher {

    //Simulation Parameters
    int numExperiments;
    int runDuration;
    
    //logging parameters
    String peersLog;
    boolean inMemoryLog = false;
    private MeasurementLog log = new MeasurementLog();
    
    public abstract IEPOSExperiment createExperiment(int num);
    
    public void run() {
        for(int i=0;i<numExperiments;i++){
            final IEPOSExperiment test = createExperiment(i);
            test.initEPOS();
            test.runSimulation(Time.inSeconds(runDuration));
            
            if(inMemoryLog) {
                Vector<Peer> peers = test.getPeers();
                for(Peer peer : peers) {
                    log.mergeWith(peer.getMeasurementLogger().getMeasurementLog());
                }
            }
        }
    }
    
    void storeInfoLog(MeasurementLog log) {
        if(inMemoryLog) {
            this.log.mergeWith(log);
        } else {
            MeasurementFileDumper logger = new MeasurementFileDumper(peersLog + "/info");
            logger.measurementEpochEnded(log, log.getMaxEpochNumber());
        }
    }
    
    public MeasurementLog getMeasurementLog() {
        if(!inMemoryLog) {
            LogReplayer replayer = new LogReplayer();
            log = new MeasurementLog();
            for(File f : new File(peersLog).listFiles()) {
                try {
                    MeasurementLog l = replayer.loadLogFromFile(f.getPath());
                    log.mergeWith(l);
                } catch (IOException | ClassNotFoundException ex) {
                    Logger.getLogger(ExperimentLauncher.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return log;
    }
}
