// 9/18 deliverable UPDATED SO THAT SINGLE PIECE MOVES TWICE
package flip.g4;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.Collections;
import java.util.Random;
import java.util.HashMap;
import javafx.util.Pair; 
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.*;

import flip.sim.Point;
import flip.sim.Board;
import flip.sim.Log;
import flip.g4.HungarianAlgorithm;
import flip.g4.PieceStore;
import flip.g4.WallStrategy;
import flip.g4.RunnerStrategy;
import flip.g4.SmallNStrategy;
import flip.g4.Utilities;

public class Player implements flip.sim.Player
{
    private int seed = 42;
    public Random random;
    public boolean isPlayer1;
    public Integer n;
    public Double diameter_piece;

    // Strategy data structures
    public HashMap<Integer, Point> playerPieces;
    public HashMap<Integer, Point> opponentPieces;
    public WallStrategy   mWallStrategy;
    public SmallNStrategy mSmallNStrategy;
    public RunnerStrategy mRunnerStrategy;

    public Player(){
        random = new Random(seed);
    }

    // Initialization function.
    // pieces: Location of the pieces for the player.
    // n: Number of pieces available. (default 30 in Makefile)
    // t: Total turns available.
    public void init(HashMap<Integer, Point> pieces, 
        int n, double t, boolean isPlayer1, double diameter_piece) {
        this.n = n;
        this.isPlayer1 = isPlayer1;
        this.diameter_piece = diameter_piece; // default 2
        
        // wall
        this.playerPieces  = pieces;
        this.mWallStrategy = new WallStrategy(this, pieces);
        Log.log("WALL STRATEGY INITIALIZED");

        this.mRunnerStrategy = new RunnerStrategy(this, pieces,
            this.mWallStrategy.runner);
        
        this.mSmallNStrategy = new SmallNStrategy(this, pieces);
    }

    public List<Pair<Integer, Point>> getMoves( Integer numMoves,
        HashMap<Integer, Point> playerPieces,
        HashMap<Integer, Point> opponentPieces,
        boolean isPlayer1 ) {
        
        this.playerPieces   = playerPieces;
        this.opponentPieces = opponentPieces;

        List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();

        if (playerPieces.size() < 12){
            this.mSmallNStrategy.getSmallNMove(moves, numMoves);
            return moves;
        }

        // Runner needs to pass the wall first
        if(this.mRunnerStrategy.status==RunnerStatus.RUNNER_SET){
            try {
                this.mRunnerStrategy.getRunnerMove(moves, numMoves);
            } catch (Exception e) {
                Log.log(e.toString());
            }
        }

        // Wall Strategy
        if (!this.mWallStrategy.WALL_COMPLETED){
            try {
                this.mWallStrategy.getWallMove(moves, numMoves);
            } catch (Exception e) {
                Log.log(e.toString());
                System.out.println(e.getLocalizedMessage());
            }
        }

        // Post wall runner strategy: FIRST RUNNER RUNS
        if(this.mRunnerStrategy.status==RunnerStatus.RUNNER_PASSED_WALL){
            try {
                this.mRunnerStrategy.getRunnerMove(moves, numMoves);
            } catch (Exception e) {
                Log.log(e.toString());
            }
        }
        return moves;
    }

}
