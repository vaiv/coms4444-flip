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

import flip.g4.WallStrategy;
import flip.g4.RunnerStrategy;
import flip.g4.AntiWallStrategy;
import flip.g4.SmallNStrategy;
import flip.g4.SuperSmallNStrategy;

import flip.g4.Utilities;
import flip.g4.PieceStore;
import flip.g4.HungarianAlgorithm;

public class Player implements flip.sim.Player
{
    private int seed = 42;
    public Random random;
    public boolean isPlayer1;
    public Integer n;
    public Double diameter_piece;

    // Store pieces for easier calculation
    public PieceStore pieceStore;

    // Strategy data structures
    public HashMap<Integer, Point> playerPieces;
    public HashMap<Integer, Point> opponentPieces;

    // Strategy classes
    public WallStrategy   mWallStrategy;
    public RunnerStrategy mRunnerStrategy;
    public AntiWallStrategy mAntiWallStrategy;

    public SmallNStrategy mSmallNStrategy;
    public SuperSmallNStrategy mSuperSmallNStrategy;

    public Player(){
        random = new Random();
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
        
        // Piece Storage
        this.playerPieces = pieces;
        this.pieceStore   = new PieceStore(this, pieces);

        // wall
        this.mWallStrategy     = new WallStrategy(this, pieces);
        this.mRunnerStrategy   = new RunnerStrategy(this, pieces, this.mWallStrategy.runner);
        this.mAntiWallStrategy = new AntiWallStrategy(this, pieces, this.mWallStrategy.runner);
        
        this.mSmallNStrategy = new SmallNStrategy(this, pieces);
        this.mSuperSmallNStrategy = new SuperSmallNStrategy(this, pieces);
    }

    public List<Pair<Integer, Point>> getMoves( Integer numMoves,
        HashMap<Integer, Point> playerPieces,
        HashMap<Integer, Point> opponentPieces,
        boolean isPlayer1 ) {
        
        this.playerPieces   = playerPieces;
        this.opponentPieces = opponentPieces;

        // Update the piece store location
        // The moved pieces can be accessed using pieceStore.movedPieces
        this.pieceStore.updatePieces(playerPieces, opponentPieces);

        List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();

        // Priority 1 : N = 1
        if (playerPieces.size() < 2){
            this.mSuperSmallNStrategy.getSuperSmallNMove(moves, numMoves);
        }
        
        // Priority 2: N < 15
        else if (playerPieces.size() < 15){
            this.mSmallNStrategy.getSmallNMove(moves, numMoves);
        }

        // Priority 3: Runner+Wall+AntiWall Strategy
        else {
        try {
            this.mAntiWallStrategy.updateStatus();

            // If wall is detected, send runner immediately to disrupt
            if(this.mAntiWallStrategy.status == WallDetectionStatus.WALL_HOLE_DETECTED){
                this.mAntiWallStrategy.getAntiWallMove(moves, numMoves);
            }

            // Runner needs to pass the wall first
            if(this.mRunnerStrategy.status==RunnerStatus.RUNNER_SET)
                this.mRunnerStrategy.getRunnerMove(moves, numMoves);

            // Wall Strategy
            if (!this.mWallStrategy.WALL_COMPLETED)
                this.mWallStrategy.getWallMove(moves, numMoves);

            // Post wall runner strategy: FIRST RUNNER RUNS
            if(this.mRunnerStrategy.status==RunnerStatus.RUNNER_PASSED_WALL)
                this.mRunnerStrategy.getRunnerMove(moves, numMoves);

        } catch (Exception e) {
            e.printStackTrace();
        }
        }
        return moves;
    }

}
