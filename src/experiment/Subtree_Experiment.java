package experiment;

import agent.dataset.Dataset;
import java.io.FileReader;
import data.Plan;
import data.Vector;
import static experiment.SimpleExperiment.dir;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import util.TreeArchitecture;

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

        Dataset<Vector> dataset = new agent.dataset.FileVectorDataset(dir + "/Plans");
        int numAgents = 200;
        int numChildren = 2;
        String out = dir + "/Output";
        int iteration = 5;
        boolean graph = false;
        boolean cost = false;
        List<Integer> half_tree_list = new ArrayList<>();
        List<Integer> half_tree_list_mod = new ArrayList<>();
        HashMap<Integer, Integer> selection_map = new HashMap<>();
        List<String> Settings_List = Arrays.asList("Full","Half_Right","Half_Left");

        int full_subtree_start = ((numAgents / numChildren) * (numChildren - 1)) + 1;

        graph_create(numAgents, numChildren);

        for (String Setting : Settings_List) {

            for (int i = 0; i < numAgents; i++) {
                selection_map.put(i, 0);
            }

            int set = 0;
            if (Setting.contains("Half_Right")) {
                set = 1;
            } else if (Setting.contains("Half_Left")) {
                set = 2;
            }
            half_tree_list = subtree_calc(numAgents - 1 - set);

            for (int val : half_tree_list) {
                if (val > full_subtree_start) {
                    half_tree_list_mod.add(val);
                }
            }
            if (!Setting.contains("Full")) {
                half_tree_list_mod.add(numAgents - 1);
            }

            System.out.println(half_tree_list_mod);
            //full_subtree_start = numAgents - 1;
            //for(int val2 = full_subtree_start;val2<numAgents;val2++){
            for (int val2 : half_tree_list_mod) {
                node = val2;
                HashMap<Integer, Integer> selection_map_tmp = new HashMap<>();
                if (node == numAgents - 1) {
                    iteration = 50;
                    //graph = true;
                    cost = true;
                    selection_map_tmp = selection_map;
                }

                String out2 = out + "_" + Setting + "_" + node;
                List<Integer> subtree_list = subtree_calc(node);
                List<List<Plan<Vector>>> possible = new ArrayList<>();
                HashMap<Integer, Integer> agent_id_map = new HashMap<>();
                possible = Create_Possible_Plans_Selection_Map(subtree_list, dataset, agent_id_map, numAgents, selection_map_tmp, selection_map);
                SimpleExperiment.exp(out2, subtree_list.size(), 2, true, possible, true, selection_map_tmp, iteration, graph, cost);
                update_Selection_map(out2 + "/Plan_Output/Plan_Selections.txt", selection_map, agent_id_map);
            }
            half_tree_list_mod.clear();
            half_tree_list.clear();
            selection_map.clear();
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
}
