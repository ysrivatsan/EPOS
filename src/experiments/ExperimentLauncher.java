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
import protopeer.SimulatedExperiment;
import protopeer.measurement.LogReplayer;
import protopeer.measurement.MeasurementFileDumper;
import protopeer.measurement.MeasurementLog;
import protopeer.util.quantities.Time;
import util.Util;

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
    boolean inMemoryLog = true;
    private MeasurementLog log;
    
    public abstract IEPOSExperiment createExperiment(int num);
    
    public void run() {
        run(null);
    }
    
    void run(MeasurementLog infoLog) {
        initLog();
        storeInfoLog(infoLog);
        
        for(int i=0;i<numExperiments;i++){
            final IEPOSExperiment experiment = createExperiment(i);
            experiment.initEPOS();
            experiment.runSimulation(Time.inSeconds(runDuration));
            
            storePeerLogs(experiment);
        }
    }
    
    private void initLog() {
        if(inMemoryLog) {
            log = new MeasurementLog();
        } else {
            File logFolder = new File(peersLog);
            Util.clearDirectory(logFolder);
            logFolder.mkdir();
        }
    }
    
    private void storeInfoLog(MeasurementLog infoLog) {
        if(infoLog != null) {
            if(inMemoryLog) {
                log.mergeWith(infoLog);
            } else {
                MeasurementFileDumper logger = new MeasurementFileDumper(peersLog + "/info");
                logger.measurementEpochEnded(infoLog, infoLog.getMaxEpochNumber()+1);
            }
        }
    }
    
    private void storePeerLogs(SimulatedExperiment experiment) {
        if(inMemoryLog) {
            Vector<Peer> peers = experiment.getPeers();
            for(Peer peer : peers) {
                log.mergeWith(peer.getMeasurementLogger().getMeasurementLog());
            }
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
