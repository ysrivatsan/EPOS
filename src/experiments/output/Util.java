/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package experiments.output;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.xpath.functions.Function3Args;
import protopeer.measurement.Aggregate;

/**
 *
 * @author Peter
 */
public class Util {
    public static void writeState(int id, String title, List<IEPOSMeasurement> configMeasurements, PrintStream out) {
        Base64.Encoder encoder = Base64.getEncoder();

        out.println(id);
        out.println(title);
        for (IEPOSMeasurement m : configMeasurements) {
            out.println(m.label);
            out.println(m.globalMeasure);
            out.println(m.localMeasure);
            out.println(m.timeMeasure);
            out.println(encoder.encodeToString(convertToBytes(m.globalMeasurements)));
            out.println(encoder.encodeToString(convertToBytes(m.localMeasurements)));
            out.println(encoder.encodeToString(convertToBytes(m.timeMeasurements)));
            out.println(encoder.encodeToString(convertToBytes(m.fairnessMeasurements)));
            out.println(encoder.encodeToString(convertToBytes(m.iterationMeasurements)));
        }
    }

    public static IEPOSMeasurement readState(BufferedReader br) throws IOException {
        return readStates(br).measurements.get(0);
    }
        
    public static PlotInfo readStates(BufferedReader br) throws IOException {
        Base64.Decoder decoder = Base64.getDecoder();
        PlotInfo out = new PlotInfo();

        out.id = Integer.parseInt(br.readLine());
        out.title = br.readLine();
        out.measurements = new ArrayList<>();
        String line = br.readLine();
        while (line != null) {
            IEPOSMeasurement m = new IEPOSMeasurement();
            m.label = line;
            m.globalMeasure = br.readLine();
            m.localMeasure = br.readLine();
            if("null".equals(m.localMeasure)) {
                m.localMeasure = null;
            }
            line = br.readLine();
            boolean withTime = line.length() < 50;
            if(withTime) {
                m.timeMeasure = line;
                line = br.readLine();
            }
            m.globalMeasurements = (List<Aggregate>) convertFromBytes(decoder.decode(line));
            m.localMeasurements = (List<Aggregate>) convertFromBytes(decoder.decode(br.readLine()));
            if(withTime) {
                m.timeMeasurements = (List<Double>) convertFromBytes(decoder.decode(br.readLine()));
            }
            
            line = br.readLine();
            if(line != null) {
                try {
                    m.fairnessMeasurements = (List<Aggregate>) convertFromBytes(decoder.decode(line));
                } catch(IllegalArgumentException e) {
                    continue;
                }
            } else {
                continue;
            }
            
            line = br.readLine();
            if(line != null) {
                try {
                    m.iterationMeasurements = (List<Aggregate>) convertFromBytes(decoder.decode(line));
                    line = br.readLine();
                } catch(IllegalArgumentException e) {
                    continue;
                }
            } else {
                continue;
            }
            
            out.measurements.add(m);
        }

        return out;
    }

    public static byte[] convertToBytes(Object object) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            return bos.toByteArray();
        } catch (IOException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static Object convertFromBytes(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                ObjectInputStream in = new ObjectInputStream(bis)) {
            return in.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public static class PlotInfo {
        public int id;
        public String title;
        public List<IEPOSMeasurement> measurements;
    }
}
