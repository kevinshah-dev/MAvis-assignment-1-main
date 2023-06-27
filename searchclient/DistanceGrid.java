package searchclient;


public class DistanceGrid
{
        // The character identifier of the target goal (digits 0-9 for agents, letters for boxes).
        public char goalID;

        // The array of Manhattan distances to the target goal in the same shape as the level.
        public int[][] distances;

    /*
     * goalID is the character identifier of the target goal (digits 0-9 for agents,
     * letters for boxes). distances is the array of Manhattan distances to the
     * target goal in the same shape as the level.
     */
    public DistanceGrid(char goalID, int[][] distances) 
    {
        this.goalID = goalID;
        this.distances = distances;
    }
}