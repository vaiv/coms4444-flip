/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package flip.g2;

import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 *
 * @author juand.correa
 */
public class AStar<V, T extends SearchNode<V>, S> {

    private final Predicate<T> goalTest;
    private final Function<T, S> solutionFn;
    private final Function<T, List<T>> childrenFn;
    private final Function<T, Double> heuristicFn;
    private final Function<T, Double> costFn;

    public AStar(
            Predicate<T> goalTest,
            Function<T, S> solutionFn,
            Function<T, List<T>> childrenFn,
            Function<T, Double> heuristicFn,
            Function<T, Double> costFn) {
        this.goalTest = goalTest;
        this.solutionFn = solutionFn;
        this.childrenFn = childrenFn;
        this.heuristicFn = heuristicFn;
        this.costFn = costFn;
    }

    public S search(T initial) {
        Queue<T> frontier = new PriorityQueue<>(1, (n1, n2)
                -> Double.compare(costFn.apply(n1) + heuristicFn.apply(n1), costFn.apply(n2) + heuristicFn.apply(n2))
        );
        Set<V> explored = new HashSet<>();
        frontier.add(initial);
        while (!frontier.isEmpty()) {
            T node = frontier.poll();
            if (explored.contains(node.getState())) {
                continue;
            }
            if (goalTest.test(node)) {
                return solutionFn.apply(node);
            }
            explored.add(node.getState());
            for (T child : childrenFn.apply(node)) {
                if (!explored.contains(child.getState())) {
                    frontier.offer(child);
                }
            }
        }
        return null;
    }
}
