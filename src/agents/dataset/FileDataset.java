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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Peter
 */
public class FileDataset implements Dataset {

    private final String location;
    private final String config;
    private final String format = ".plans";
    private final File[] agentDataDirs;
    private int planSize = -1;

    public FileDataset(String location, String config) {
        this.location = location;
        this.config = config;

        File dir = new File(location + "/" + config);
        this.agentDataDirs = dir.listFiles((File pathname) -> pathname.isDirectory());
        if (agentDataDirs == null) {
            System.out.println("ERROR: directory " + dir.getPath() + " is empty");
            throw new IllegalArgumentException("inFolder is expected to contain a folder for each agent");
        }
    }

    @Override
    public List<AgentDataset> getAgentDataSources(int maxAgents) {
        List<AgentDataset> agents = new ArrayList<>();
        for (int i = 0; i < agentDataDirs.length && i < maxAgents; i++) {
            agents.add(new FileAgentDataset(location, config, agentDataDirs[i].getName(), format, getPlanSize()));
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
    }
}
