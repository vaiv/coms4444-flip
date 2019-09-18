package flip.group5;

import java.util.*;
import java.util.stream.Collectors;

public class MWBM {

    public Result mwbm(Map<String, Map<String, Double>> input) throws Exception {
        return apply(input);
    }

    public Result apply(Map<String, Map<String, Double>> input) throws Exception {
        // A bipartite graph can be split into 2 node sets
        Set<String> lhsNodes = new HashSet<>(input.keySet());
        Set<String> rhsNodes = new HashSet<>();

        // Put each node in a set
        fillNodeSets(input, lhsNodes, rhsNodes);

        // Get lists for indexing
        List<String> lhsNodesList = new ArrayList<>(lhsNodes);
        List<String> rhsNodesList = new ArrayList<>(rhsNodes);

        // Calculate the weights matrix and run the Hungarian Algorithm
        System.out.println("LHS: " + lhsNodesList.size() + " RHS: " + rhsNodesList);
        System.out.println("input: " + input.size() + " input keyset: " + input.keySet().size());
        double[][] weights = calculateWeights(input, lhsNodesList, rhsNodesList);
        int[] output = new HungarianAlgorithm(weights).execute();

        // Parse the output
        Map<String, String> matches = getMatchPairs(output, lhsNodesList, rhsNodesList);
        double totalWeight = calculateTotalWeight(output, weights);

        // Return the result
        return new Result(matches, totalWeight);
    }

    private void fillNodeSets(Map<String, Map<String, Double>> input, Set<String> lhsNodes, Set<String> rhsNodes) {
        input.forEach((node, neighbours) -> neighbours.forEach((neighbour, weight) -> {
            if (lhsNodes.contains(node)) {
                lhsNodes.remove(neighbour);
                rhsNodes.add(neighbour);
            }
        }));
    }

    private double[][] calculateWeights(Map<String, Map<String, Double>> input,
                                        List<String> lhsNodesList, List<String> rhsNodesList) {
        int n = lhsNodesList.size();
        System.out.println("N: " + n);
        double[][] weights = new double[n][n];

        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights[i].length; j++) {
                System.out.print(weights[i][j] + ": ");
            }
            System.out.println();
        }

        for (int i = 0; i < n; i++) {
            String node1 = lhsNodesList.get(i);
            Map<String, Double> edges = input.get(node1);

            for (Map.Entry<String, Double> entry : edges.entrySet()) {
                String node2 = entry.getKey();
                double weight = entry.getValue();
                int j = rhsNodesList.indexOf(node2);
                weights[i][j] = weight;
            }
        }

        return weights;
    }

    private double calculateTotalWeight(int[] output, double[][] weights) {
        double weight = 0;

        for (int i = 0; i < output.length; i++) {
            weight += weights[i][output[i]];
        }

        return weight;
    }

    private Map<String, String> getMatchPairs(int[] output, List<String> lhsNodesList, List<String> rhsNodesList) {
        Map<String, String> matches = new HashMap<>();
        int n = lhsNodesList.size();

        for (int i = 0; i < n; i++) {
            matches.put(lhsNodesList.get(i), rhsNodesList.get(output[i]));
        }

        return matches;
    }

    public static class Result {

        public final Map<String, String> assignment;
        public final double weight;

        public Result(Map<String, String> assignment, double weight) {
            this.assignment = assignment;
            this.weight = weight;
        }

    }

}