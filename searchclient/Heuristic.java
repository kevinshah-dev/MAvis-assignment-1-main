package searchclient;

import java.util.Comparator;


public abstract class Heuristic
        implements Comparator<State>
{
    // Sets current heuristic to use. Options: [goalCount, manhattanDistance]
    public static final String heuristicChoice = "manhattanDistance";

    /*
    * Stores the distance grids of all goals.
    */
    private DistanceGrid[] gridLookup;

    /*
     * Stores a snapshot of current agent positions and their Manhattan
     * distances to their respective goals. Printed from GraphSearch.java
     */
    private static String statusUpdate;

    /*
     * Preprocesses all of the manhattan distances from any position on the level
     * to any goal. Stores manhattan distances by goalID in the `gridLookup` lookup table.
     */
    public Heuristic(State initialState)
    {
        System.err.println("Running with " + heuristicChoice);
        
        if (heuristicChoice.equals("manhattanDistance")) {
        
            gridLookup = new DistanceGrid[State.numGoals];

            int goalX, goalY;
            
            // Generate a `distances` array for every goal in the level
            for (int goal = 0; goal < State.goalCoords.length; goal++) {

                goalY = State.goalCoords[goal][0];
                goalX = State.goalCoords[goal][1];

                // DEBUG
                System.err.println("---------- Goal # " + goal + ": (" + goalX + ", " + goalY + ") ----------");
                System.err.println("Goal ID: " + State.goals[goalX][goalY] + ", goalX: " + goalX + ", goalY: " + goalY);

                DistanceGrid distGrid = new DistanceGrid(State.goals[goalX][goalY], getGoalDistances(goalY, goalX));
                gridLookup[goal] = distGrid;

            }

            // Print all goals in gridLookup
            System.err.println("Goal IDs:");
            for (int i = 0; i < gridLookup.length; i++) {
                System.err.println(gridLookup[i].goalID);
            }
        }
    }

    /*
     * QUESTION 3
     * Our heuristic calculates the Manhattan distance between each agent and
     * its respective goal, the uses the sum of these distances as a heuristic.
     * When running A* or greedy best-first search, the shorter distance nodes
     * will be expanded first.
     */
    public int h(State s)
    {
        // Question 2: Goal count heuristic
        if (heuristicChoice.equals("goalCount")) {
            return s.getGoalCount();

        } else {
            // Question 3: Manhattan distance heuristic
            int sumManhattanDist = 0;
            int goalNum;
            int agentX = 0;
            int agentY = 0;
            int dist = 0;
            int minDist = -1;

            statusUpdate = "";

            /*
             * Search through all stored distance grids. For every grid with a
             * goal ID between 0 and 10 (agents), look up agent distance
             */
            for (int k = 0; k < gridLookup.length; k++) {
                goalNum = (int) gridLookup[k].goalID - 48;
                if (goalNum <= 10) {
                    //System.err.println("Goal num: " + goalNum);
                    agentY = s.agentRows[goalNum];
                    agentX = s.agentCols[goalNum];
                    dist = gridLookup[k].distances[agentY][agentX];

                    statusUpdate += ("| Manhattan distance from agent #" + goalNum + " at (" + agentY + ", " + agentX + ") to goal #" + goalNum + " = " + dist + "\n");
                    sumManhattanDist += dist;
                }
            }

            /*
             * Get box manhattan distances
             */
            char boxID;
            int boxX, boxY;
            for (int i = 0; i < s.boxes.length; i++) {
                for (int j = 0; j < s.boxes[i].length; j++) {
                    if (s.boxes[i][j] != '\0') {
                       // Found a valid box
                       boxID = s.boxes[i][j];
                       minDist = 1000;

                       for (int k = 0; k < gridLookup.length; k++) {
                            if (boxID == gridLookup[k].goalID) {
                                boxX = i;
                                boxY = j;
                                // TODO: Set a more robust minDist initial value
                                //minDist = gridLookup[0].distances[boxX][boxY];
                                dist = gridLookup[k].distances[boxX][boxY];
                                
                                if (dist < minDist) {
                                    minDist = dist;
                                }
                            }
                       }
    
                       // Add this box's minimum distance to a goal to the Man. dist. total
                       statusUpdate += ("Manhattan distance from box " + boxID + " = " + minDist + "\n"); 
                       sumManhattanDist += minDist;
                    }
                }
            }

            // Update search status
            statusUpdate += ("Sum of manhattan distances: " + sumManhattanDist + "\n");

            return sumManhattanDist;
        }
    }


    // ---------- HELPER METHODS -------------------

    public static String getStatusUpdate() {
        return statusUpdate;
    }

    /*
     * Returns an array containing Manhattan distances from any spot on the level
     * to the specified goal coordinates. See documentation about the `distances` array.
     */
    private int[][] getGoalDistances(int goalY, int goalX) {
        // Get level representation as a 2D array
        // Reconstructs level without bordering walls. Coordinate system has
        // (0, 0) as the upper left corner, x and y ascending as you move towards
        // the bottom right corner.
        int levelHeight = State.goals.length;
        int levelWidth = State.goals[0].length;

        char[][] level = new char[levelHeight][levelWidth];

        //DEBUG
        //System.err.println("levelHeight (vertical): " + levelHeight);
        //System.err.println("levelWidth (horizontal): " + levelWidth);

        for (int i = 0; i < levelHeight; i++) {
            for (int j = 0; j < levelWidth; j++) {
                level[i][j] = State.goals[i][j];
            }
        }

        // For every grid position, calculate the Manhattan distance to the goal
        int[][] distances = new int[levelHeight][levelWidth];

        // Temporary variables
        int dx, dy;
        int manhattanDist;

        for (int k = 0; k < levelWidth; k++) {
            distances[0][k] = -1;
        }

        for (int i = 1; i < levelHeight - 1; i++) {
            // Set distance values at border wall positions to -1
            distances[i][0] = -1;

            for (int j = 1; j < levelWidth - 1; j++) {
                dx = Math.abs(j - goalY);
                dy = Math.abs(i - goalX);
                manhattanDist = dx + dy;

                // Store the Manhattan distance in a lookup table with the same 2D array
                // representation as the entire level representation (including borders)
                distances[i][j] = manhattanDist;
            }

            distances[i][levelWidth - 1] = -1;
        }

        for (int k = 0; k < levelWidth; k++) {
            distances[levelHeight - 1][k] = -1;
        }

        // DEBUG: Print distances array
        System.err.println("Distances Array for goal (" + goalX + ", " + goalY + "): ");

        for(int i = 0; i < distances.length; i++) {
            for(int j = 0; j < distances[0].length; j++) {
                System.err.print(distances[i][j] + " ");
            }
            System.err.println("\n");
        }

        return distances;
        
    }

    public abstract int f(State s);

    @Override
    public int compare(State s1, State s2)
    {
        return this.f(s1) - this.f(s2);
    }
}

class HeuristicAStar
        extends Heuristic
{
    public HeuristicAStar(State initialState)
    {
        super(initialState);
    }

    @Override
    public int f(State s)
    {
        return s.g() + this.h(s);
    }

    @Override
    public String toString()
    {
        return "A* evaluation";
    }
}

class HeuristicWeightedAStar
        extends Heuristic
{
    private int w;

    public HeuristicWeightedAStar(State initialState, int w)
    {
        super(initialState);
        this.w = w;
    }

    @Override
    public int f(State s)
    {
        return s.g() + this.w * this.h(s);
    }

    @Override
    public String toString()
    {
        return String.format("WA*(%d) evaluation", this.w);
    }
}

class HeuristicGreedy
        extends Heuristic
{
    public HeuristicGreedy(State initialState)
    {
        super(initialState);
    }

    @Override
    public int f(State s)
    {
        return this.h(s);
    }

    @Override
    public String toString()
    {
        return "greedy evaluation";
    }
}
