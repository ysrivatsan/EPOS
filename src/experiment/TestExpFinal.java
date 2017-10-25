package experiment;

import agent.dataset.Dataset;
import agent.dataset.GaussianDataset;
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
import java.util.Random;
import java.util.TreeMap;

/**
 *
 * @author Peter
 */
public class TestExpFinal {

    static Queue<Integer> q = new LinkedList<>();
    static Map<Integer, LinkedList<Integer>> g = new HashMap<>();
    static LinkedList<Integer> result = new LinkedList<Integer>();
    public static int node;

    public static void main(String[] args) throws FileNotFoundException, IOException {

        String dir1 = args[0];

        int numChildren = Integer.parseInt(args[1]);
        int iteration = 5;
        double lambda = Double.parseDouble(args[2]);
        boolean graph = false;
        boolean detail = true;
        boolean cost = false;
        boolean indexCost = args[5].contains("true");

        //full_subtree_start = numAgents - 1;
        List<String> Settings_List = Arrays.asList(args[3]);
        List<String> datasets = Arrays.asList(dir1);

        for (String dir : datasets) {
            HashMap<Integer, Integer> selection_map = new HashMap<>();
            Map<Integer, Integer> level_map = new HashMap<>();
            List<Integer> half_tree_list_mod = new ArrayList<>();
            //System.out.println(dir);
            int numAgents = new File(dir + "/Plans").list().length;
            float numagents = numAgents;
            float numchildren = numChildren;
            int full_subtree_start = (int) ((numagents / numchildren) * (numchildren - 1)) + 1;
            graph_create(numAgents, numChildren);
            level_map = Level_Identifier(full_subtree_start, numAgents, numChildren);
            Dataset<Vector> dataset = new agent.dataset.FileVectorDataset(dir + "/Plans");
            String out = dir + "/New_Results/Output_" + numChildren + "_" + lambda;

            for (String Setting : Settings_List) {
                switch (Setting) {
                    case "NoPassing": {
                        int[] Iteration_list = {Integer.parseInt(args[4])};
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
                                    SimpleExperiment.exp(out2, subtree_list.size(), 2, true, possible, true, selection_map_tmp, iteration, graph, cost, detail, lambda, indexCost);
                                    update_Selection_map(out2 + "/Plan_Selections.txt", selection_map, agent_id_map);
                                }
                                cost = false;
                                detail = true;
                            }
                        }
                    }
                    break;
                    case "Holarchy": {
                        int[] Iteration_list = {Integer.parseInt(args[4])};
                        for (int iter : Iteration_list) {
                            List<Integer> half_tree_list = new ArrayList<>();
                            for (int val5 = full_subtree_start; val5 < numAgents; val5++) {
                                half_tree_list = new ArrayList<>();
                                half_tree_list_mod = new ArrayList<>();
                                for (int i = 0; i < numAgents; i++) {
                                    selection_map.put(i, 0);
                                }
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
                                    SimpleExperiment.exp(out2, subtree_list.size(), 2, true, possible, true, selection_map_tmp, iteration, graph, cost, detail, lambda, indexCost);
                                    update_Selection_map(out2 + "/Plan_Selections.txt", selection_map, agent_id_map);
                                }
                            }
                        }
                    }
                    break;
                    case "Passing": {
                        int[] Iteration_list = {Integer.parseInt(args[4])};
                        for (int iter : Iteration_list) {
                            for (int val = full_subtree_start; val < numAgents - 1; val++) {
                                iteration = iter;
                                String out2 = out + "_" + Setting + "_Iterations_" + iteration + "_" + val;
                                SimpleExperiment.exp(true, val, iteration, out2, dir, lambda, indexCost);
                            }
                        }
                    }
                    break;
                    case "gaussian": {
                        Random random = new Random(0);
                        GaussianDataset dataset2 = new GaussianDataset(16, 100, 0, 1, random);
                        dataset2.writeDataset("E:\\Gaussian_1000", 1000);
                    }
                    break;
                    case "tree": {
                        Map<Integer, String> tree_map = new HashMap<>();
                        tree_map.put(numAgents - 1, numAgents - 1 + "");
                        for (int i = numAgents - 1; i >= 0; i--) {
                            if (g.get(i) == null) {
                                break;
                            }
                            for (int j = 0; j < g.get(i).size(); j++) {
                                tree_map.put(g.get(i).get(j), tree_map.get(i) + "/" + g.get(i).get(j));
                               
                            }
                        }
                        for(int i = 0;i<tree_map.size();i++)
                            System.out.println(i+"-"+tree_map.get(i));
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
