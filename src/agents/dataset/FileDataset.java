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
package agents.dataset;

import agents.plan.Plan;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joda.time.DateTime;

/**
 *
 * @author Peter
 */
public class FileDataset implements Dataset {

    private final String location;
    private final String config;
    private final String format = ".plans";
    private final File[] agentDataDirs;
    private int seed;
    private int planSize = -1;
    private final Comparator<Plan> order;

    private Map<Integer, FileAgentDataset> cache = new HashMap<>();

    public static void main(String[] args) {
        String ds = "3";
        FileDataset fd = new FileDataset("C:\\Users\\Peter\\Documents\\EPOS\\input-data\\Archive", ds + ".3");
        List<AgentDataset> ads = fd.getAgentDataSources(1000);
        File dir = new File(fd.location + "\\" + ds);
        dir.mkdir();
        int j = 0;
        for (AgentDataset ad : ads) {
            File adir = new File(dir, "ag" + j);
            adir.mkdir();
            j++;
            for (DateTime t : ad.getPhases()) {
                List<Plan> plans = ad.getPlans(t);
                Plan plan = plans.get(0);
                int size = plan.getNumberOfStates();
                int num = 6;
                try (PrintStream out = new PrintStream(new File(adir, "2014-07-23.plans"))) {
                    for (int offset = -num; offset <= num; offset++) {
                        int comfort = 1 - Math.abs(offset);
                        out.print(comfort + ":" + plan.getValue(num + offset));
                        for (int i = num + 1; i < size - num; i++) {
                            out.print("," + plan.getValue(i + offset));
                        }
                        out.println();
                    }
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(FileDataset.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public FileDataset(String location, String config) {
        this(location, config, null);
    }

    public FileDataset(String location, String config, Comparator<Plan> order) {
        this.location = location;
        this.config = config;
        this.order = order;

        File dir = new File(location + "/" + config);
        this.agentDataDirs = dir.listFiles((File pathname) -> pathname.isDirectory());
        if (agentDataDirs == null) {
            System.out.println("ERROR: directory " + dir.getPath() + " is empty");
            throw new IllegalArgumentException("inFolder is expected to contain a folder for each agent");
        }
    }

    @Override
    public List<AgentDataset> getAgentDataSources(int maxAgents) {
        TreeMap<Double, Integer> indices = new TreeMap<>();
        Random rand = new Random(seed);
        for (int i = 0; i < agentDataDirs.length; i++) {
            indices.put(rand.nextDouble(), i);
        }
        Set<Integer> selected = new TreeSet<>();
        for (Integer i : indices.values()) {
            selected.add(i);
            if (selected.size() == maxAgents) {
                break;
            }
        }

        List<AgentDataset> agents = new ArrayList<>();
        for (int i : selected) {
            if (cache.containsKey(i)) {
                agents.add(cache.get(i));
            } else {
                FileAgentDataset fad = new FileAgentDataset(location, config, agentDataDirs[i].getName(), format, getPlanSize(), order);
                agents.add(fad);
                cache.put(i, fad);
            }
        }
        return agents;
    }

    @Override
    public int getPlanSize() {
        if (planSize < 0) {
            File file = agentDataDirs[0];
            File[] planFiles = file.listFiles((File dir, String name) -> name.endsWith(format));
            file = planFiles[0];
            try (Scanner scanner = new Scanner(file)) {
                scanner.useLocale(Locale.US);
                String line = scanner.nextLine();
                planSize = line.split(",").length;
            } catch (FileNotFoundException ex) {
                Logger.getLogger(FileDataset.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return planSize;
    }

    @Override
    public void init(int num) {
        seed = num;
    }
}
