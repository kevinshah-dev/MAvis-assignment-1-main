package searchclient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;



public class State
{
    private static final Random RNG = new Random(1);

    /*
     * Prints debugging messages if true.
     */
    public static boolean DEBUG = false;

    /*
        The agent rows, columns, and colors are indexed by the agent number.
        For example, this.agentRows[0] is the row location of agent '0'.
    */
    public int[] agentRows;
    public int[] agentCols;
    public static Color[] agentColors;

    /*
        The walls, boxes, and goals arrays are indexed from the top-left of the level, row-major order (row, col).
               Col 0  Col 1  Col 2  Col 3
        Row 0: (0,0)  (0,1)  (0,2)  (0,3)  ...
        Row 1: (1,0)  (1,1)  (1,2)  (1,3)  ...
        Row 2: (2,0)  (2,1)  (2,2)  (2,3)  ...
        ...

        For example, this.walls[2] is an array of booleans for the third row.
        this.walls[row][col] is true if there's a wall at (row, col).

        this.boxes and this.char are two-dimensional arrays of chars. 
        this.boxes[1][2]='A' means there is an A box at (1,2). 
        If there is no box at (1,2), we have this.boxes[1][2]=0 (null character).
        Simiarly for goals. 

    */
    public static boolean[][] walls;
    public char[][] boxes;
    public static char[][] goals;

    // Total number of goals in level
    public static int numGoals;

    // Total number of goals in level
    public static int numAgents;

    /*
     * Array containing x/y coordinates of every goal in the level. Look up
     * coordinates of a goal with: goalCoords[goalNum][0 for y-coordinate, 1 for x-coordinate];
     */
    public static int[][] goalCoords;

    /*
        The box colors are indexed alphabetically. So this.boxColors[0] is the color of A boxes, 
        this.boxColor[1] is the color of B boxes, etc.
    */
    public static Color[] boxColors;
 
    public final State parent;
    public final Action[] jointAction;
    private final int g;

    private int hash = 0;

    // Constructs an initial state.
    // Arguments are not copied, and therefore should not be modified after being passed in.
    public State(int[] agentRows, int[] agentCols, Color[] agentColors, boolean[][] walls,
                 char[][] boxes, Color[] boxColors, char[][] goals
    )
    {
        this.agentRows = agentRows;
        this.agentCols = agentCols;
        this.agentColors = agentColors;
        this.walls = walls;
        this.boxes = boxes;
        this.boxColors = boxColors;
        this.goals = goals;
        this.parent = null;
        this.jointAction = null;
        this.g = 0;

        // Calculates number of goals in level and gets their coordinates
        numGoals = calculateNumGoals();
        numAgents = calculateNumAgents();
        goalCoords = new int[numGoals][2];
        getGoalCoords();
    }


    // Constructs the state resulting from applying jointAction in parent.
    // Precondition: Joint action must be applicable and non-conflicting in parent state.
    private State(State parent, Action[] jointAction)
    {
        // Copy parent
        this.agentRows = Arrays.copyOf(parent.agentRows, parent.agentRows.length);
        this.agentCols = Arrays.copyOf(parent.agentCols, parent.agentCols.length);
        this.boxes = new char[parent.boxes.length][];
        for (int i = 0; i < parent.boxes.length; i++)
        {
            this.boxes[i] = Arrays.copyOf(parent.boxes[i], parent.boxes[i].length);
        }

        // Set own parameters
        this.parent = parent;
        this.jointAction = Arrays.copyOf(jointAction, jointAction.length);
        this.g = parent.g + 1;

        // DEBUG
        // System.err.println("Before update:");
        // printBoxes();

        // Apply each action
        int numAgents = this.agentRows.length;
        for (int agent = 0; agent < numAgents; ++agent)
        {
            Action action = jointAction[agent];
            
            int agentRow = this.agentRows[agent];
            int agentCol = this.agentCols[agent];

            switch (action.type)
            {
                case NoOp:
                    break;

                case Move:
                    this.agentRows[agent] += action.agentRowDelta;
                    this.agentCols[agent] += action.agentColDelta;
                    break;

                case Pull:
                    // Get box's current position and ID
                    int boxCurrentRow = agentRow - action.boxRowDelta;
                    int boxCurrentCol = agentCol - action.boxColDelta;
                    char box = this.boxes[boxCurrentRow][boxCurrentCol];

                    // Update agent's position
                    this.agentRows[agent] += action.agentRowDelta;
                    this.agentCols[agent] += action.agentColDelta;

                    // Get box's destination position
                    int boxDestinationRow = agentRow;
                    int boxDestinationCol = agentCol;

                    // Update box's position
                    this.boxes[boxCurrentRow][boxCurrentCol] = '\0';
                    this.boxes[boxDestinationRow][boxDestinationCol] = box;

                    break;

                case Push:
                    // Get box's current position and ID
                    int boxCurrentRow2 = agentRow + action.agentRowDelta;
                    int boxCurrentCol2 = agentCol + action.agentColDelta;
                    box = this.boxes[boxCurrentRow2][boxCurrentCol2];

                    // Update agent's position
                    this.agentRows[agent] += action.agentRowDelta;
                    this.agentCols[agent] += action.agentColDelta;

                    // Get box's destination position
                    int boxDestinationRow2 = boxCurrentRow2 + action.boxRowDelta;
                    int boxDestinationCol2 = boxCurrentCol2 + action.boxColDelta;

                    // Update box's position
                    this.boxes[boxCurrentRow2][boxCurrentCol2] = '\0';
                    this.boxes[boxDestinationRow2][boxDestinationCol2] = box;

                    break;
            }

            // DEBUG
            // System.err.println("After update:");
            // printBoxes();
        }
    }

    /*
     * Helper debugging method to print the boxes array. No longer used
     */
    private void printBoxes() {
        // Print boxes array
        System.err.println("--------");
        for (int i = 1; i < this.boxes.length - 1; i++) {
            System.err.print("| ");
            for (int j = 1; j < this.boxes[i].length - 1; j++) {
                if (this.boxes[i][j] == '\0') {
                    System.err.print(".");
                } else {
                    System.err.print(this.boxes[i][j]);
                }
            }
            System.err.print(" |\n");
        }
        System.err.println("--------");
    }
    

    public int g()
    {
        return this.g;
    }

    /*
     * Gets the number of goals that are not yet covered by their correct agents.
     * Used for Question 2 goal count heuristic.
     */
    public int getGoalCount() {

        //System.err.println("----Counting goals----");
        //System.err.println(this);

        // agentRows is an array of each agent's row position (agents identified numerically)
        int numAgents = agentRows.length;
        int goalCount = 0;

        // // Calculates the number of agents at their correct goal
        for (int agentNum = 0; agentNum < numAgents; agentNum++) {
            // If selected goal has same row and col as agent
            int agentX = agentRows[agentNum];
            int agentY = agentCols[agentNum];

            // Get numerical goal ID by converting from ASCII
            int goalNum = (int) goals[agentX][agentY] - 48;

            if (goalNum == agentNum) {
                System.err.println("Counted agent goal " + agentNum);
                goalCount++;
            }
        }

        // Calculates the number of goals with a correct box - Exercise 6.1
        for (int i = 0; i < numGoals; i++) {
            int goalX = goalCoords[i][1];
            int goalY = goalCoords[i][0];

            if (goals[goalX][goalY] != '\0') {
                if (goals[goalX][goalY] == boxes[goalX][goalY]) {
                    //System.err.println(" Counted box goals[" + goalX +"][" + goalY + "] = " + goals[goalX][goalY] + ".");
                    //System.err.println(this);

                    goalCount++;
                }
            } 
            
        }

        // Returns number of goals not yet covered by their correct agent
        return (numGoals - goalCount);
    }

    /*
     * Returns the total number of goals in this level. Private helper method called
     * when State is constructed.
     */
    private int calculateNumGoals() {
        int numGoals = 0;

        // Iterate through goals array and count any listing that is not blank as a goal
        for (int i = 0; i < goals.length; i++) {
            for (int j = 0; j < goals[i].length; j++) {
                if (goals[i][j] != '\0') {
                    numGoals++;
                }
            }
        }
        System.err.println("Total num goals: " + numGoals);
        return numGoals;
    }

    private int calculateNumAgents() {
        int numAgents = 0;

        // Iterate through goals array and count any listing that is an integer between 0 - 9
        for (int i = 0; i < goals.length; i++) {
            for (int j = 0; j < goals[i].length; j++) {
                if (goals[i][j] != '\0' && ((int) goals[i][j] - 48) < 10) {
                    numAgents++;
                }
            }
        }
        System.err.println("Total num agents: " + numAgents);
        return numAgents;
    }

    /*
     * Returns the total number of agents with goals in this level.
     */
    public int getNumAgents() {
        return this.numAgents;
    }

    /*
     * Calculates 2D array of x/y coordinates for all goals in level. Updates
     * `goalCoords` class variable. Private helper method called only when
     * State is first constructed.
     */
    private void getGoalCoords() {
        int x, y;
        int k = 0;

         // Iterate through goals array and count any listing that is not blank as a goal
         for (int i = 0; i < goals.length; i++) {
            for (int j = 0; j < goals[i].length; j++) {
        
                if (goals[i][j] != '\0') {
                    x = i;
                    y = j;

                    goalCoords[k][0] = y;
                    goalCoords[k][1] = x;
                    k++;

                    if (DEBUG) {
                        System.err.println("Goal counted: (" + x + ", " + y + ") ");
                        printGoalCoords();
                    }

                }
            }
        }
    }

    /*
     * Prints out the goalCoords array
     */
    private void printGoalCoords() {
        for (int i = 0; i < goalCoords.length; i++) {
            System.err.println(i + ": [" + goalCoords[i][0] +", " + goalCoords[i][1] + "]");
        }
    }

    /*
     * Returns the total number of goals in this level.
     */
    public int getNumGoals() {
        return this.numGoals;
    }

    public boolean isGoalState()
    {
        for (int row = 1; row < this.goals.length - 1; row++)
        {
            for (int col = 1; col < this.goals[row].length - 1; col++)
            {
                char goal = this.goals[row][col];

                if ('A' <= goal && goal <= 'Z' && this.boxes[row][col] != goal)
                {
                    return false;
                }
                else if ('0' <= goal && goal <= '9' &&
                         !(this.agentRows[goal - '0'] == row && this.agentCols[goal - '0'] == col))
                {
                    return false;
                }
            }
        }
        return true;
    }

    public ArrayList<State> getExpandedStates()
    {
        if (DEBUG) {
            System.err.println("------Getting expanded states------");
        }

        int numAgents = this.agentRows.length;

        // Determine list of applicable actions for each individual agent.
        Action[][] applicableActions = new Action[numAgents][];
        for (int agent = 0; agent < numAgents; ++agent)
        {
            ArrayList<Action> agentActions = new ArrayList<>(Action.values().length);
            for (Action action : Action.values())
            {
                if (this.isApplicable(agent, action))
                {
                    if (DEBUG) {
                        System.err.println("Added " + action.name());
                    }
                    agentActions.add(action);
                }
            }
            applicableActions[agent] = agentActions.toArray(new Action[0]);
        }

        // Iterate over joint actions, check conflict and generate child states.
        Action[] jointAction = new Action[numAgents];
        int[] actionsPermutation = new int[numAgents];
        ArrayList<State> expandedStates = new ArrayList<>(16);
        while (true)
        {
            for (int agent = 0; agent < numAgents; ++agent)
            {
                jointAction[agent] = applicableActions[agent][actionsPermutation[agent]];
            }

            if (!this.isConflicting(jointAction))
            {
                expandedStates.add(new State(this, jointAction));
            }

            // Advance permutation
            boolean done = false;
            for (int agent = 0; agent < numAgents; ++agent)
            {
                if (actionsPermutation[agent] < applicableActions[agent].length - 1)
                {
                    ++actionsPermutation[agent];
                    break;
                }
                else
                {
                    actionsPermutation[agent] = 0;
                    if (agent == numAgents - 1)
                    {
                        done = true;
                    }
                }
            }

            // Last permutation?
            if (done)
            {
                break;
            }
        }

        Collections.shuffle(expandedStates, State.RNG);
        return expandedStates;
    }

    private boolean isApplicable(int agent, Action action)
    {
        if (DEBUG) {
            System.err.println("-------- Checking action " + action.name + "------------");
        }

        int agentRow = this.agentRows[agent];
        int agentCol = this.agentCols[agent];
        Color agentColor = this.agentColors[agent];
        Color boxColor;
        int boxRow;
        int boxCol;
        char box;
        int destinationRow;
        int destinationCol;
        switch (action.type)
        {
            case NoOp:
                return true;

            case Move:
                destinationRow = agentRow + action.agentRowDelta;
                destinationCol = agentCol + action.agentColDelta;
                return this.cellIsFree(destinationRow, destinationCol);

               
            case Pull:
                // A pull is appliccable if the cell in the direction the agent is moving is free
                // AND the cell in the direction opposite where the agent is moving contains a box
                // of the same color as the agent.

                // Get current box position
                boxRow = agentRow - action.boxRowDelta;
                boxCol = agentCol - action.boxColDelta;

                // Get agent destination position
                destinationRow = agentRow + action.agentRowDelta;
                destinationCol = agentCol + action.agentColDelta;

                box = this.boxes[boxRow][boxCol];

                // Check that agent is moving into an empty cell
                Boolean agentCellIsFree = this.cellIsFree(destinationRow, destinationCol);

                if (DEBUG) {
                    System.err.println("Agent at ( " + agentRow + ", " + agentCol + ")");
                    System.err.println("Agent destination cell is free: " + agentCellIsFree);
                }

                Boolean boxInCell = false;
                Boolean colorsMatch = false;

                // Check that a box in the right position exists and has the right color
                if (box != '\0') {
                    boxInCell = true;

                    int boxIndex = ((int) box) - 65;
                    boxColor = boxColors[boxIndex];

                    if (boxColor == agentColor) {
                        colorsMatch = true;
                    }
                }

                if (DEBUG && agentCellIsFree && boxInCell && colorsMatch) {
                    System.err.println(action.name + " is valid: " + (agentCellIsFree && boxInCell));
                }

                return agentCellIsFree && boxInCell && colorsMatch;

            case Push:
                // A push is appliccable if the cell in the direction the agent is moving contains
                // a box of the same color as the agent AND the cell in the direction that the box is moving
                // is free.

                // Get current position of box
                boxRow = agentRow + action.agentRowDelta;
                boxCol = agentCol + action.agentColDelta;

                // Get agent destination position
                int agentDestinationRow = agentRow + action.agentRowDelta;
                int agentDestinationCol = agentCol + action.agentColDelta;

                // Get current value (A, B, ...) of box and check there is a box in that position
                box = this.boxes[agentDestinationRow][agentDestinationCol];

                if (box == '\0') {
                    return false;
                }

                // Get box destination position
                int boxDestinationRow = boxRow + action.boxRowDelta;
                int boxDestinationCol = boxCol + action.boxColDelta;

                // Check if box destination cell is free
                Boolean boxCellIsFree = this.cellIsFree(boxDestinationRow, boxDestinationCol);

                if (DEBUG) {
                    System.err.println("Agent at ( " + agentRow + ", " + agentCol + ")");
                    System.err.println("Box at ( " + boxRow + ", " + boxCol + ")");
                    System.err.println("Box destination cell is free: " + boxCellIsFree);
                    System.err.println(this);
                }
                
                // Check that colors of box and agent match
                colorsMatch = false;
                int boxIndex = ((int) box) - 65;
                boxColor = boxColors[boxIndex];

                if (boxColor == agentColor) {
                    colorsMatch = true;
                }

                if (DEBUG && boxCellIsFree && colorsMatch) {
                    System.err.println(action.name + " is valid: " + (boxCellIsFree));
                }

                return boxCellIsFree && colorsMatch;
        }

        // Unreachable:
        return false;
    }

    

    private boolean isConflicting(Action[] jointAction)
    {
        int numAgents = this.agentRows.length;

        int[] destinationRows = new int[numAgents]; // row of new cell to become occupied by action
        int[] destinationCols = new int[numAgents]; // column of new cell to become occupied by action
        int[] boxRows = new int[numAgents]; // current row of box moved by action
        int[] boxCols = new int[numAgents]; // current column of box moved by action

        // Collect cells to be occupied and boxes to be moved
        for (int agent = 0; agent < numAgents; ++agent)
        {
            Action action = jointAction[agent];
            int agentRow = this.agentRows[agent];
            int agentCol = this.agentCols[agent];
            int boxRow = agentRow - action.boxRowDelta;
            int boxCol = agentCol - action.boxColDelta;

            // Update agent and box positions
            switch (action.type)
            {
                case NoOp:
                    break;

                case Move:
                    destinationRows[agent] = agentRow + action.agentRowDelta;
                    destinationCols[agent] = agentCol + action.agentColDelta;
                    boxRows[agent] = agentRow; // Distinct dummy value
                    boxCols[agent] = agentCol; // Distinct dummy value
                    break;
                
                case Pull:
                /*
                 * For every agent with a joint action, add its destination row/col to an array
                 * For every agent, add its target box to an array
                 */
                    destinationRows[agent] = agentRow + action.agentRowDelta;
                    destinationCols[agent] = agentCol + action.agentColDelta;
                    boxRows[agent] = boxRow + action.boxRowDelta;
                    boxCols[agent] = boxCol + action.boxColDelta;
                    //System.err.println("Agent: (" + agentRow + ", " + agentCol + "), Box: (" + boxRow + ", " + boxCol +")");
                    //System.err.println("Agent dest. (" + destinationRows[agent] + ", " + destinationCols[agent] + "), Box dest. (" + boxRows[agent] + ", " + boxCols[agent] +")");
                    
                    break;
           }
        }

        for (int a1 = 0; a1 < numAgents; ++a1)
        {
            if (jointAction[a1] == Action.NoOp)
            {
                continue;
            }

            for (int a2 = a1 + 1; a2 < numAgents; ++a2)
            {
                if (jointAction[a2] == Action.NoOp)
                {
                    continue;
                }

                // Agents moving into same cell?
                if (destinationRows[a1] == destinationRows[a2] && destinationCols[a1] == destinationCols[a2])
                {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean cellIsFree(int row, int col)
    {
        return !this.walls[row][col] && this.boxes[row][col] == 0 && this.agentAt(row, col) == 0;
    }

    private char agentAt(int row, int col)
    {
        for (int i = 0; i < this.agentRows.length; i++)
        {
            if (this.agentRows[i] == row && this.agentCols[i] == col)
            {
                return (char) ('0' + i);
            }
        }
        return 0;
    }

    public Action[][] extractPlan()
    {
        Action[][] plan = new Action[this.g][];
        State state = this;
        while (state.jointAction != null)
        {
            plan[state.g - 1] = state.jointAction;
            state = state.parent;
        }
        return plan;
    }

    @Override
    public int hashCode()
    {
        if (this.hash == 0)
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(this.agentColors);
            result = prime * result + Arrays.hashCode(this.boxColors);
            result = prime * result + Arrays.deepHashCode(this.walls);
            result = prime * result + Arrays.deepHashCode(this.goals);
            result = prime * result + Arrays.hashCode(this.agentRows);
            result = prime * result + Arrays.hashCode(this.agentCols);
            for (int row = 0; row < this.boxes.length; ++row)
            {
                for (int col = 0; col < this.boxes[row].length; ++col)
                {
                    char c = this.boxes[row][col];
                    if (c != 0)
                    {
                        result = prime * result + (row * this.boxes[row].length + col) * c;
                    }
                }
            }
            this.hash = result;
        }
        return this.hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (this.getClass() != obj.getClass())
        {
            return false;
        }
        State other = (State) obj;
        return Arrays.equals(this.agentRows, other.agentRows) &&
               Arrays.equals(this.agentCols, other.agentCols) &&
               Arrays.equals(this.agentColors, other.agentColors) &&
               Arrays.deepEquals(this.walls, other.walls) &&
               Arrays.deepEquals(this.boxes, other.boxes) &&
               Arrays.equals(this.boxColors, other.boxColors) &&
               Arrays.deepEquals(this.goals, other.goals);
    }

    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder();
        for (int row = 0; row < this.walls.length; row++)
        {
            for (int col = 0; col < this.walls[row].length; col++)
            {
                if (this.boxes[row][col] > 0)
                {
                    s.append(this.boxes[row][col]);
                }
                else if (this.walls[row][col])
                {
                    s.append("+");
                }
                else if (this.agentAt(row, col) != 0)
                {
                    s.append(this.agentAt(row, col));
                }
                else
                {
                    s.append(" ");
                }
            }
            s.append("\n");
        }
        return s.toString();
    }
}
