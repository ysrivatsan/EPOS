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
        
        String dir3 = "E:\\Java_Workspace\\EPOS-master\\EPOS-master\\datasets\\energy";
        String dir2 = "E:\\Java_Workspace\\EPOS-master\\EPOS-master\\datasets\\gaussian";
        String dir1 = "E:\\Java_Workspace\\EPOS-master\\EPOS-master\\datasets\\bicycle";

        int numChildren = 2;
        int iteration = 5;
        boolean graph = false;
        boolean detail = true;
        boolean cost = false;

        Map<Integer, Integer> level_map = new HashMap<Integer, Integer>();
        List<Integer> half_tree_list_mod = new ArrayList<>();
        HashMap<Integer, Integer> selection_map = new HashMap<>();

//        List<Integer> half_tree_list = new ArrayList<>();
//        List<String> Settings_List = Arrays.asList("Full","Half_Right","Half_Left");
//        for (String Setting : Settings_List) {
//            for (int i = 0; i < numAgents; i++) {
//                selection_map.put(i, 0);
//            }
//            int set = 0;
//            if (Setting.contains("Half_Right")) {
//                set = 1;
//            } else if (Setting.contains("Half_Left")) {
//                set = 2;
//            }
//            half_tree_list = subtree_calc(numAgents - 1 - set);
//            for (int val : half_tree_list) {
//                if (val > full_subtree_start) {
//                    half_tree_list_mod.add(val);
//                }
//            }
//            if (!Setting.contains("Full")) {
//                half_tree_list_mod.add(numAgents - 1);
//            }
        //System.out.println(half_tree_list_mod);
        //full_subtree_start = numAgents - 1;
        List<String> Settings_List = Arrays.asList("Exp_1", "Exp_2", "Exp_3");
        List<String> datasets = Arrays.asList(dir1, dir2, dir3);

        for (String dir : datasets) {
            int numAgents = new File(dir + "/Plans").list().length;
            int full_subtree_start = ((numAgents / numChildren) * (numChildren - 1)) + 1;
            graph_create(numAgents, numChildren);
            level_map = Level_Identifier(full_subtree_start, numAgents, numChildren);
            Dataset<Vector> dataset = new agent.dataset.FileVectorDataset(dir + "/Plans");
            String out = dir + "/Output";

            for (String Setting : Settings_List) {
                switch (Setting) {
                    case "Exp_1": {
                        for (int val3 = full_subtree_start; val3 < numAgents - 1; val3++) {
                            iteration =5;
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
                                    String out2 = out + "_" + Setting + "_" +val3+"_"+ node;

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
                    case "Exp_2": {
                        for (int val3 = full_subtree_start; val3 < numAgents - 1; val3++) {
                            iteration = 5;
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
                                 String out2 = out + "_" + Setting + "_" +val3+"_"+ node;

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
                    break;
                    case "Exp_3": {
                        for (int val4 = full_subtree_start; val4 < numAgents - 1; val4++) {
                            for (int val3 = val4 + 1; val3 < numAgents - 1; val3++) {
                                iteration =5;
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
                                         String out2 = out + "_" + Setting + "_" +val3+"_"+val4+"_"+ node;

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

//        for (int val4 = full_subtree_start; val4 < numAgents - 1; val4++) {
//
//            for (int val3 = val4 + 1; val3 < numAgents - 1; val3++) {
//
//                half_tree_list_mod = new ArrayList<>();
////                half_tree_list = new ArrayList<>();
//                selection_map = new HashMap<>();
//                for (int i = 0; i < numAgents; i++) {
//                    selection_map.put(i, 0);
//                }
//
//                half_tree_list_mod.add(val4);
//                half_tree_list_mod.add(val3);
//                half_tree_list_mod.add(numAgents - 1);
//
//                System.out.println("----" + half_tree_list_mod);
//
//                for (int val2 : half_tree_list_mod) {
//                    node = val2;
//                    HashMap<Integer, Integer> selection_map_tmp = new HashMap<>();
//                    String Setting = "exp2_size2_" + val4 + "_" + val3;
//                    String out2 = out + "_" + Setting + "_" + node;
//
//                    if (node == numAgents - 1) {
//                        iteration = 50;
//                        //graph = true;
//                        cost = true;
//                        detail = true;
//                        out2 = out2 + "_Final";
//                        selection_map_tmp = selection_map;
//                    }
//
//                    List<Integer> subtree_list = subtree_calc(node);
//                    List<List<Plan<Vector>>> possible = new ArrayList<>();
//                    HashMap<Integer, Integer> agent_id_map = new HashMap<>();
//                    possible = Create_Possible_Plans_Selection_Map(subtree_list, dataset, agent_id_map, numAgents, selection_map_tmp, selection_map);
//                    SimpleExperiment.exp(out2, subtree_list.size(), 2, true, possible, true, selection_map_tmp, iteration, graph, cost, detail);
//                    update_Selection_map(out2 + "/Plan_Output/Plan_Selections.txt", selection_map, agent_id_map);
//                }
//                cost = false;
//                detail = false;
////            
//            }
////        }
//        }
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
}
