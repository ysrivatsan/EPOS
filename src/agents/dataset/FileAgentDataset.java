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

import agents.fitnessFunction.costFunction.CostFunction;
import agents.plan.Plan;
import agents.plan.PossiblePlan;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.StringTokenizer;
import org.joda.time.DateTime;

/**
 *
 * @author Peter
 */
public class FileAgentDataset extends OrderedAgentDataset {

    private String config;
    private String id;
    private String format;
    private String planDir;
    private int planSize;

    public FileAgentDataset(String planLocation, String config, String id, String format, int planSize) {
        this(planLocation, config, id, format, planSize, null);
    }

    public FileAgentDataset(String planLocation, String config, String id, String format, int planSize, Comparator<Plan> order) {
        super(order);
        this.config = config;
        this.id = id;
        this.format = format;
        this.planDir = planLocation + File.separator + config + File.separator + id;
        this.planSize = planSize;
    }

    @Override
    List<Plan> getUnorderedPlans(DateTime phase) {
        List<Plan> plans = new ArrayList<>();

        File planFile = new File(planDir + File.separator + phase.toString("yyyy-MM-dd") + format);
        try (Scanner scanner = new Scanner(planFile)) {
            scanner.useLocale(Locale.US);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                plans.add(parsePlan(line, phase));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return plans;
    }

    @Override
    public List<DateTime> getPhases() {
        List<DateTime> phases = new ArrayList<>();

        File[] dates = new File(planDir).listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isHidden() || pathname.getName().charAt(0) == '.') {
                    return false;
                }
                return pathname.isFile();
            }
        });
        for (File date : dates) {
            StringTokenizer dateTokenizer = new StringTokenizer(date.getName(), ".");
            phases.add(DateTime.parse(dateTokenizer.nextToken()));
        }

        return phases;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getConfig() {
        return config;
    }

    private Plan parsePlan(String planStr, DateTime phase) {
        Plan plan = new PossiblePlan();
        plan.init(planSize);
        plan.setCoordinationPhase(phase);

        Scanner scanner = new Scanner(planStr);
        scanner.useLocale(Locale.US);
        scanner.useDelimiter(":");
        double score = scanner.nextDouble();
        plan.setDiscomfort(1.0 - score);

        scanner.useDelimiter(",");
        scanner.skip(":");

        for (int i = 0; scanner.hasNextDouble(); i++) {
            plan.setValue(i, scanner.nextDouble());
        }
        return plan;
    }

    @Override
    public int getPlanSize() {
        return planSize;
    }
}
