package searchclient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
// hlo


public class GraphSearch {

    public static Action[][] search(State initialState, Frontier frontier)
    {
        int iterations = 0;

        frontier.add(initialState);
        HashSet<State> expandedNodes = new HashSet<>();

        //System.err.println("Initial node:\n" + initialState.toString());

        while (true) {

            // Check if frontier is empty. If true, return null
            if (frontier.isEmpty()) {
                return null;
            }

            // Pick a node from the frontier
            // Remove that node from the frontier (pop)
            State n = frontier.pop();

            //Print a status message every 10000 iteration
            if (++iterations % 10000 == 0) {
                printSearchStatusWithState(expandedNodes, frontier, n);
            }
            // Print search status at the beginning of every search
            else if (iterations == 1) {
                System.err.println("----------- INITIAL STATUS -----------");
                printSearchStatusWithState(expandedNodes, frontier, n);
                System.err.println("-------------------------------------");
            }
 
            // Check if that node is the goal state. If true, return a the actions needed to reach that state.
            // If false, add that node to explored nodes
            if (n.isGoalState()) {
                printSearchStatusWithState(expandedNodes, frontier, n);

                // Print final status
                System.err.println("----------- FINAL STATUS -----------");
                printSearchStatus(expandedNodes, frontier);
                System.err.println(n);
                System.err.println("-------------------------------------");

                return n.extractPlan();
            }

            expandedNodes.add(n);
            
            // Expand that node (n): For every child node of n, if the child is
            // not already in the frontier and is not in explored nodes, add it
            // to the frontier.
            for (State m : n.getExpandedStates()) {
                if (!frontier.contains(m) && !expandedNodes.contains(m)) {
                    frontier.add(m);
                }
            }

        }

    }

    private static long startTime = System.nanoTime();

    private static void printSearchStatus(HashSet<State> explored, Frontier frontier)
    {
        String statusTemplate = "#Expanded: %,8d, #Frontier: %,8d, #Generated: %,8d, Time: %3.3f s\n%s\n";
        double elapsedTime = (System.nanoTime() - startTime) / 1_000_000_000d;
        System.err.format(statusTemplate, explored.size(), frontier.size(), explored.size() + frontier.size(),
                          elapsedTime, Memory.stringRep());
    }

    private static void printSearchStatusWithState(HashSet<State> explored, Frontier frontier, State currentState)
    {
        printSearchStatus(explored, frontier);

        if (Heuristic.heuristicChoice.equals("goalCount")) {
            // Print goal count status
            System.err.println("Current goal count: " + currentState.getGoalCount());
        }
        else {
            // Print current Manhattan distances:
            System.err.println("Snapshot of Manhattan distances:\n" + Heuristic.getStatusUpdate());
        }

    }
}
