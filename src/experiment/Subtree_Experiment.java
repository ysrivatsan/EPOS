package experiment;

import agent.dataset.Dataset;
import java.io.FileReader;
import data.Plan;
import data.Vector;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 *
 * @author Peter
 */
public class Subtree_Experiment {

    static Queue<Integer> q = new LinkedList<>();
    static Map<Integer, LinkedList<Integer>> g = new HashMap<>();
    static LinkedList<Integer> result = new LinkedList<Integer>();
    public static int node;

    public static void main(String[] args) throws FileNotFoundException, IOException {

        String dir3 = "C:\\Users\\syadhuna\\Downloads\\EPOS-master\\EPOS-master\\datasets\\energy";
        String dir2 = "C:\\Users\\syadhuna\\Downloads\\EPOS-master\\EPOS-master\\datasets\\gaussian";
        String dir1 = "C:\\Users\\syadhuna\\Downloads\\EPOS-master\\EPOS-master\\datasets\\bicycle";

        int numChildren = 2;
        int iteration = 5;
        boolean graph = false;
        boolean detail = true;
        boolean cost = false;

        //full_subtree_start = numAgents - 1;
        List<String> Settings_List = Arrays.asList("Passing");
        List<String> datasets = Arrays.asList(dir1,dir2,dir3);

        for (String dir : datasets) {
            HashMap<Integer, Integer> selection_map = new HashMap<>();
            Map<Integer, Integer> level_map = new HashMap<>();
            List<Integer> half_tree_list_mod = new ArrayList<>();

            int numAgents = new File(dir + "/Plans").list().length;
            int full_subtree_start = ((numAgents / numChildren) * (numChildren - 1)) + 1;
            graph_create(numAgents, numChildren);
            level_map = Level_Identifier(full_subtree_start, numAgents, numChildren);
            Dataset<Vector> dataset = new agent.dataset.FileVectorDataset(dir + "/Plans");
            String out = dir + "/New_Results/Output";

            for (String Setting : Settings_List) {
                switch (Setting) {
                    case "Exp_1": {
                        for (int val3 = full_subtree_start; val3 < numAgents - 1; val3++) {
                            iteration = 5;
                            if (level_map.get(val3) == 2) {

                                half_tree_list_mod = new ArrayList<>();
                                selection_map = new HashMap<>();

                                for (int i = 0; i < numAgents; i++) {
                                    selection_map.put(i, 0);
                                }

                                half_tree_list_mod.add(val3);
                                half_tree_list_mod.add(numAgents - 1);
                                Collections.sort(half_tree_list_mod);

                                for (int val2 : half_tree_list_mod) {
                                    node = val2;
                                    HashMap<Integer, Integer> selection_map_tmp = new HashMap<>();
                                    String out2 = out + "_" + Setting + "_" + val3 + "_" + node;

                                    if (node == numAgents - 1) {
                                        iteration = 50;
                                        //graph = true;
                                        cost = true;
                                        //detail = true;
                                        //out2 = out2 + "_Final";
                                        selection_map_tmp = selection_map;
                                    }

                                    List<Integer> subtree_list = subtree_calc(node);
                                    List<List<Plan<Vector>>> possible = new ArrayList<>();
                                    HashMap<Integer, Integer> agent_id_map = new HashMap<>();
                                    possible = Create_Possible_Plans_Selection_Map(subtree_list, dataset, agent_id_map, numAgents, selection_map_tmp, selection_map);
                                    SimpleExperiment.exp(out2, subtree_list.size(), numChildren, true, possible, true, selection_map_tmp, iteration, graph, cost, detail);
                                    update_Selection_map(out2 + "/Plan_Output/Plan_Selections.txt", selection_map, agent_id_map);
                                }
                                cost = false;
                                detail = true;
                            }
                        }
                    }
                    break;
                    case "No_Passing": {
                        int[] Iteration_list = {2, 3};
                        for (int iter : Iteration_list) {
                            for (int val3 = full_subtree_start; val3 < numAgents - 1; val3++) {
                                iteration = iter;
                                half_tree_list_mod = new ArrayList<>();
                                selection_map = new HashMap<>();

                                for (int i = 0; i < numAgents; i++) {
                                    selection_map.put(i, 0);
                                }

                                half_tree_list_mod.add(val3);
                                half_tree_list_mod.add(numAgents - 1);
                                Collections.sort(half_tree_list_mod);

                                for (int val2 : half_tree_list_mod) {
                                    node = val2;
                                    HashMap<Integer, Integer> selection_map_tmp = new HashMap<>();
                                    String out2 = out + "_" + Setting + "_Iteration_" + iteration + "_" + val3 + "_" + node;

                                    if (node == numAgents - 1) {
                                        iteration = 50;
                                        //graph = true;
                                        cost = true;
                                        detail = true;
                                        out2 = out2 + "_Completed";
                                        selection_map_tmp = selection_map;
                                    }

                                    List<Integer> subtree_list = subtree_calc(node);
                                    List<List<Plan<Vector>>> possible = new ArrayList<>();
                                    HashMap<Integer, Integer> agent_id_map = new HashMap<>();
                                    possible = Create_Possible_Plans_Selection_Map(subtree_list, dataset, agent_id_map, numAgents, selection_map_tmp, selection_map);
                                    SimpleExperiment.exp(out2, subtree_list.size(), 2, true, possible, true, selection_map_tmp, iteration, graph, cost, detail);
                                    update_Selection_map(out2 + "/Plan_Output/Plan_Selections.txt", selection_map, agent_id_map);
                                }
                                cost = false;
                                detail = true;
                            }
                        }
                    }
                    break;
                    case "Exp_3": {
                        for (int val4 = full_subtree_start; val4 < numAgents - 1; val4++) {
                            for (int val3 = val4 + 1; val3 < numAgents - 1; val3++) {
                                iteration = 5;
                                if (level_map.get(val4) == level_map.get(val3)) {
                                    half_tree_list_mod = new ArrayList<>();
                                    selection_map = new HashMap<>();

                                    for (int i = 0; i < numAgents; i++) {
                                        selection_map.put(i, 0);
                                    }

                                    half_tree_list_mod.add(val4);
                                    half_tree_list_mod.add(val3);
                                    half_tree_list_mod.add(numAgents - 1);
                                    Collections.sort(half_tree_list_mod);
                                    for (int val2 : half_tree_list_mod) {
                                        node = val2;
                                        HashMap<Integer, Integer> selection_map_tmp = new HashMap<>();
                                        String out2 = out + "_" + Setting + "_" + val3 + "_" + val4 + "_" + node;

                                        if (node == numAgents - 1) {
                                            iteration = 50;
                                            //graph = true;
                                            cost = true;
                                            detail = true;
                                            out2 = out2 + "_Final";
                                            selection_map_tmp = selection_map;
                                        }

                                        List<Integer> subtree_list = subtree_calc(node);
                                        List<List<Plan<Vector>>> possible = new ArrayList<>();
                                        HashMap<Integer, Integer> agent_id_map = new HashMap<>();
                                        possible = Create_Possible_Plans_Selection_Map(subtree_list, dataset, agent_id_map, numAgents, selection_map_tmp, selection_map);
                                        SimpleExperiment.exp(out2, subtree_list.size(), 2, true, possible, true, selection_map_tmp, iteration, graph, cost, detail);
                                        update_Selection_map(out2 + "/Plan_Output/Plan_Selections.txt", selection_map, agent_id_map);
                                    }
                                    cost = false;
                                    detail = true;
                                }
                            }
                        }
                    }
                    break;
                    case "Exp_4": {
                        for (int val4 = full_subtree_start; val4 < numAgents - 1; val4++) {
                            for (int val3 = val4 + 1; val3 < numAgents - 1; val3++) {
                                for (int val5 = val3 + 1; val5 < numAgents - 1; val5++) {
                                    iteration = 5;
                                    if (level_map.get(val4) == level_map.get(val3) && level_map.get(val4) == level_map.get(val5)) {
                                        half_tree_list_mod = new ArrayList<>();
                                        selection_map = new HashMap<>();

                                        for (int i = 0; i < numAgents; i++) {
                                            selection_map.put(i, 0);
                                        }

                                        half_tree_list_mod.add(val4);
                                        half_tree_list_mod.add(val3);
                                        half_tree_list_mod.add(val5);
                                        half_tree_list_mod.add(numAgents - 1);
                                        Collections.sort(half_tree_list_mod);
                                        for (int val2 : half_tree_list_mod) {
                                            node = val2;
                                            HashMap<Integer, Integer> selection_map_tmp = new HashMap<>();
                                            String out2 = out + "_" + Setting + "_" + val4 + "_" + val3 + "_" + val5 + "_" + node;

                                            if (node == numAgents - 1) {
                                                iteration = 50;
                                                //graph = true;
                                                cost = true;
                                                detail = true;
                                                out2 = out2 + "_Final";
                                                selection_map_tmp = selection_map;
                                            }

                                            List<Integer> subtree_list = subtree_calc(node);
                                            List<List<Plan<Vector>>> possible = new ArrayList<>();
                                            HashMap<Integer, Integer> agent_id_map = new HashMap<>();
                                            possible = Create_Possible_Plans_Selection_Map(subtree_list, dataset, agent_id_map, numAgents, selection_map_tmp, selection_map);
                                            SimpleExperiment.exp(out2, subtree_list.size(), 2, true, possible, true, selection_map_tmp, iteration, graph, cost, detail);
                                            update_Selection_map(out2 + "/Plan_Output/Plan_Selections.txt", selection_map, agent_id_map);
                                        }
                                        cost = false;
                                        detail = true;
                                    }
                                }
                            }
                        }
                    }
                    break;
                    case "Exp_5": {
                        List<Integer> level_list = new ArrayList<>();
                        level_list.addAll(level_map.values());

                        Map<Integer, Integer> level_counts = new HashMap<>();
                        int temp;
                        temp = level_list.get(0);
                        int count = 1;
                        for (int levels : level_list) {
                            if (temp != levels) {
                                level_counts.put(temp, count);
                                temp = levels;
                                count = 1;
                            } else {
                                count++;
                            }
                        }
                        List<Integer> no_of_choices = Arrays.asList(3);
                        for (int r : no_of_choices) {
                            for (int level : level_counts.keySet()) {

                                int n = level_counts.get(level);
                                int num_combinations = 0;
                                num_combinations = Combination(n, r);
                                int target = num_combinations / 10;
                                if (num_combinations > 1000) {
                                    target = 1000;
                                }
                                List<Integer> full_exp = new ArrayList<>();
                                List<Integer> random_Selections = new ArrayList<>();
                                for (int j = 0; j < num_combinations; j++) {
                                    full_exp.add(j);
                                }
                                for (int j = 0; j < target; j++) {
                                    Collections.shuffle(full_exp);
                                    if (!random_Selections.contains(full_exp.get(0))) {
                                        random_Selections.add(full_exp.get(0));
                                    } else {
                                        j--;
                                    }
                                }
                                int selection_count = -1;
                                for (int val4 = full_subtree_start; val4 < numAgents - 1; val4++) {
                                    for (int val3 = val4 + 1; val3 < numAgents - 1; val3++) {
                                        for (int val5 = val3 + 1; val5 < numAgents - 1; val5++) {
                                            iteration = 5;
                                            if (level_map.get(val4) == level_map.get(val3) && level_map.get(val4) == level_map.get(val5) && level_map.get(val3) == level) {
                                                selection_count++;
                                                if (random_Selections.contains(selection_count)) {
                                                    half_tree_list_mod = new ArrayList<>();
                                                    selection_map = new HashMap<>();

                                                    for (int i = 0; i < numAgents; i++) {
                                                        selection_map.put(i, 0);
                                                    }

                                                    half_tree_list_mod.add(val4);
                                                    half_tree_list_mod.add(val3);
                                                    half_tree_list_mod.add(val5);
                                                    half_tree_list_mod.add(numAgents - 1);
                                                    Collections.sort(half_tree_list_mod);
                                                    for (int val2 : half_tree_list_mod) {
                                                        node = val2;
                                                        HashMap<Integer, Integer> selection_map_tmp = new HashMap<>();
                                                        String out2 = out + "_" + Setting + "_" + val4 + "_" + val3 + "_" + val5 + "_" + node;

                                                        if (node == numAgents - 1) {
                                                            iteration = 50;
                                                            //graph = true;
                                                            cost = true;
                                                            detail = true;
                                                            out2 = out2 + "_Final";
                                                            selection_map_tmp = selection_map;
                                                        }

                                                        List<Integer> subtree_list = subtree_calc(node);
                                                        List<List<Plan<Vector>>> possible = new ArrayList<>();
                                                        HashMap<Integer, Integer> agent_id_map = new HashMap<>();
                                                        possible = Create_Possible_Plans_Selection_Map(subtree_list, dataset, agent_id_map, numAgents, selection_map_tmp, selection_map);
                                                        SimpleExperiment.exp(out2, subtree_list.size(), 2, true, possible, true, selection_map_tmp, iteration, graph, cost, detail);
                                                        update_Selection_map(out2 + "/Plan_Output/Plan_Selections.txt", selection_map, agent_id_map);
                                                    }
                                                    cost = false;
                                                    detail = true;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    break;
                    case "Exp_6": {
                        List<Integer> level_list = new ArrayList<>();
                        level_list.addAll(level_map.values());

                        Map<Integer, Integer> level_counts = new HashMap<>();
                        int temp;
                        temp = level_list.get(0);
                        int count = 1;
                        for (int levels : level_list) {
                            if (temp != levels) {
                                level_counts.put(temp, count);
                                temp = levels;
                                count = 1;
                            } else {
                                count++;
                            }
                        }
                        List<Integer> no_of_choices = Arrays.asList(4);
                        for (int r : no_of_choices) {
                            for (int level : level_counts.keySet()) {

                                int n = level_counts.get(level);
                                int num_combinations = 0;
                                num_combinations = Combination(n, r);
                                int target = num_combinations / 10;
                                if (num_combinations > 1000) {
                                    target = 1000;
                                }
                                List<Integer> full_exp = new ArrayList<>();
                                List<Integer> random_Selections = new ArrayList<>();
                                for (int j = 0; j < num_combinations; j++) {
                                    full_exp.add(j);
                                }
                                for (int j = 0; j < target; j++) {
                                    Collections.shuffle(full_exp);
                                    if (!random_Selections.contains(full_exp.get(0))) {
                                        random_Selections.add(full_exp.get(0));
                                    } else {
                                        j--;
                                    }
                                }
                                int selection_count = -1;
                                for (int val4 = full_subtree_start; val4 < numAgents - 1; val4++) {
                                    for (int val3 = val4 + 1; val3 < numAgents - 1; val3++) {
                                        for (int val5 = val3 + 1; val5 < numAgents - 1; val5++) {
                                            for (int val6 = val5 + 1; val6 < numAgents - 1; val6++) {
                                                iteration = 5;
                                                if (level_map.get(val4) == level_map.get(val3) && level_map.get(val4) == level_map.get(val5) && level_map.get(val4) == level_map.get(val6) && level_map.get(val3) == level) {
                                                    selection_count++;
                                                    if (random_Selections.contains(selection_count)) {
                                                        half_tree_list_mod = new ArrayList<>();
                                                        selection_map = new HashMap<>();

                                                        for (int i = 0; i < numAgents; i++) {
                                                            selection_map.put(i, 0);
                                                        }

                                                        half_tree_list_mod.add(val4);
                                                        half_tree_list_mod.add(val3);
                                                        half_tree_list_mod.add(val5);
                                                        half_tree_list_mod.add(val6);
                                                        half_tree_list_mod.add(numAgents - 1);
                                                        Collections.sort(half_tree_list_mod);
                                                        for (int val2 : half_tree_list_mod) {
                                                            node = val2;
                                                            HashMap<Integer, Integer> selection_map_tmp = new HashMap<>();
                                                            String out2 = out + "_" + Setting + "_" + val4 + "_" + val3 + "_" + val5 + "_" + val6 + "_" + node;

                                                            if (node == numAgents - 1) {
                                                                iteration = 50;
                                                                //graph = true;
                                                                cost = true;
                                                                detail = true;
                                                                out2 = out2 + "_Final";
                                                                selection_map_tmp = selection_map;
                                                            }

                                                            List<Integer> subtree_list = subtree_calc(node);
                                                            List<List<Plan<Vector>>> possible = new ArrayList<>();
                                                            HashMap<Integer, Integer> agent_id_map = new HashMap<>();
                                                            possible = Create_Possible_Plans_Selection_Map(subtree_list, dataset, agent_id_map, numAgents, selection_map_tmp, selection_map);
                                                            SimpleExperiment.exp(out2, subtree_list.size(), 2, true, possible, true, selection_map_tmp, iteration, graph, cost, detail);
                                                            update_Selection_map(out2 + "/Plan_Output/Plan_Selections.txt", selection_map, agent_id_map);
                                                        }
                                                        cost = false;
                                                        detail = true;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    break;
                    case "Holarchy": {
                        int[] Iteration_list = {2, 3, 5, 10, 15, 20};
                        for (int iter : Iteration_list) {
                            List<Integer> half_tree_list = new ArrayList<>();
//                        List<String> Settings_List_2 = Arrays.asList("Full");
                            for (int val5 = full_subtree_start; val5 < numAgents; val5++) {
                                half_tree_list = new ArrayList<>();
                                half_tree_list_mod = new ArrayList<>();
                                for (int i = 0; i < numAgents; i++) {
                                    selection_map.put(i, 0);
                                }
//                            int set = 0;
//                            if (Setting_2.contains("Half_Right")) {
//                                set = 1;
//                            } else if (Setting_2.contains("Half_Left")) {
//                                set = 2;
//                            }
//                            half_tree_list = subtree_calc(numAgents - 1 - set);
//                            for (int val : half_tree_list) {
//                                if (val > full_subtree_start) {
//                                    half_tree_list_mod.add(val);
//                                }
//                            }
//                            if (!Setting_2.contains("Full")) {
//                                half_tree_list_mod.add(numAgents - 1);
//                            }

                                half_tree_list = subtree_calc(val5);

                                for (int val : half_tree_list) {
                                    if (val >= full_subtree_start) {
                                        half_tree_list_mod.add(val);
                                    }

                                }
                                if (!half_tree_list_mod.contains(numAgents - 1)) {
                                    half_tree_list_mod.add(numAgents - 1);
                                }

                                Collections.sort(half_tree_list_mod);

                                for (int val2 : half_tree_list_mod) {

                                    iteration = iter;
                                    node = val2;
                                    HashMap<Integer, Integer> selection_map_tmp = new HashMap<>();
                                    String out2 = out + "_" + Setting + "_Iterations_" + iteration + "_" + val5 + "_" + val2 + "_" + node;

                                    if (node == numAgents - 1) {
                                        iteration = 50;
                                        //graph = true;
                                        cost = true;
                                        detail = true;
                                        out2 = out2 + "_Completed";
                                        selection_map_tmp = selection_map;
                                    }

                                    List<Integer> subtree_list = subtree_calc(node);
                                    List<List<Plan<Vector>>> possible = new ArrayList<>();
                                    HashMap<Integer, Integer> agent_id_map = new HashMap<>();
                                    possible = Create_Possible_Plans_Selection_Map(subtree_list, dataset, agent_id_map, numAgents, selection_map_tmp, selection_map);
                                    SimpleExperiment.exp(out2, subtree_list.size(), 2, true, possible, true, selection_map_tmp, iteration, graph, cost, detail);
                                    update_Selection_map(out2 + "/Plan_Output/Plan_Selections.txt", selection_map, agent_id_map);
                                }
                            }
                        }
                    }
                    break;
                    case "Passing": {
                        int[] Iteration_list = {2, 3};
                        for (int iter : Iteration_list) {
                            for (int val = full_subtree_start; val < numAgents - 1; val++) {
                                iteration = iter;
                                String out2 = out + "_" + Setting + "_Iterations_" + iteration + "_" + val;
                                SimpleExperiment.exp(true, val, iteration, out2, dir);
                            }
                        }
                    }
                    break;
                    case "Test": {
                        for (int i = numAgents - 2; i < numAgents; i++) {
                            System.out.println(subtree_calc(i));
                        }
                    }
                }
            }
        }
    }

    public static void graph_create(int numAgents, int numChildren) {
        int n = numAgents;
        q.add(n);
        n--;
        while (!q.isEmpty()) {
            int v = q.poll();
            int nn = n;
            for (int j = 1; j <= Math.min(numChildren, nn); j++) {
                if (g.containsKey(v - 1)) {
                    g.get(v - 1).add(n - 1);
                } else {
                    LinkedList<Integer> tmp = new LinkedList<>();
                    tmp.add(n - 1);
                    g.put(v - 1, tmp);
                }
                q.add(n);
                n--;
            }
        }

    }

    private static void depth_first_search(int v) {
        result.add(v);
        if (g.containsKey(v)) {
            for (int u : g.get(v)) {
                depth_first_search(u);
            }
        }
    }

    private static List<Integer> subtree_calc(int node) {
        List<Integer> tmp = new ArrayList<>();
        result.clear();
        depth_first_search(node);
        for (int value : result) {
            tmp.add(value);
        }
        Collections.sort(tmp);
        return tmp;
    }

    private static List<List<Plan<Vector>>> Create_Possible_Plans_Selection_Map(List<Integer> subtree_list, Dataset<Vector> dataset, HashMap<Integer, Integer> agent_id_map, int numAgents, HashMap<Integer, Integer> selection_map_tmp, HashMap<Integer, Integer> selection_map) {
        int i = 0;
        List<List<Plan<Vector>>> possible = new ArrayList<>();
        for (int value : subtree_list) {
            possible.add(dataset.getPlans(value));
            agent_id_map.put(i, value);
            if (node != numAgents - 1) {
                selection_map_tmp.put(i, selection_map.get(value));
            }
            i++;
        }

        return possible;
    }

    private static void update_Selection_map(String out, HashMap<Integer, Integer> selection_map, HashMap<Integer, Integer> agent_id_map) {

        BufferedReader br = null;
        FileReader fr = null;
        String[] values = new String[2];

        try {
            fr = new FileReader(out);
            br = new BufferedReader(fr);
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {

                values = sCurrentLine.split(":");
                int sub_agent_id = Integer.parseInt(values[0]);
                int selection = Integer.parseInt(values[1]);
                int orig_id = agent_id_map.get(sub_agent_id);
                selection_map.put(orig_id, selection);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            try {
                if (br != null) {
                    br.close();
                }
                if (fr != null) {
                    fr.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static Map<Integer, Integer> Level_Identifier(int full_subtree_start, int numAgents, double numChildren) {
        Map<Integer, Integer> level_map = new HashMap<>();
        for (int i = numAgents - 1; i >= full_subtree_start; i--) {
            int level_count = 1;
            int subtree_size = subtree_calc(i).size();
            while (subtree_size >= Math.pow(numChildren, level_count)) {
                level_count++;
            }
            level_map.put(i, level_count--);
        }
        return level_map;
    }

    private static int Combination(int n, int r) {
        int diff = 1;
        for (int i = n - r + 1; i <= n; i++) {
            diff *= i;
        }
        int fact_r = 1;
        for (int j = 1; j <= r; j++) {
            fact_r *= j;
        }
        return diff / fact_r;
    }
}
