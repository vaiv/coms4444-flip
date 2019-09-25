package flip.g4;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import javafx.util.Pair;
import flip.sim.Board;
import flip.sim.Log;
import flip.sim.Point;
import flip.g4.Player;
import flip.g4.Utilities;

// Abstracted class for wall building
class WallStrategy{
    private int debugCount = 0;
    // Store details about the game
    private Player mPlayer;
    public  PieceStore pieceStore;

    // Wall co-ordinates and number of pieces
    public int       numWallPieces;
    public double    wallXLocation;
    public Point[]   idealWallLocations;

    // Index of pieces that can build wall the fastest
    public Integer[] fastestWallBuilders;

    // Status flags for wall completion
    public boolean   WALL_COMPLETED;
    public boolean   WALL_BREACHED;
    public List<Integer> breachers;

    // private Integer[] movesLeft;
    public Integer   totalMovesLeft;
    public Integer   runner;

    // Statistics for fun
    public Integer numMovesRequired;

    public WallStrategy(Player mPlayer, HashMap<Integer, Point> pieces){
        this.mPlayer = mPlayer;
        this.pieceStore = mPlayer.pieceStore;
        this.numMovesRequired = 0;
        this.calculateWallPositions();

        // Calculate number of ideal runners
        HashSet<Integer> runners = new HashSet<>();
        Pair<Integer, Integer[]> p = this.calculateWallStrategy(pieces,runners);

        List<Integer> idxs = Utilities.rankXProgress(pieces, mPlayer.isPlayer1);
        runners.add(idxs.get(0));
        Pair<Integer, Integer[]> q = this.calculateWallStrategy(pieces,runners);

        if (p.getKey() - q.getKey() <= 5){ // Arbitrary decision
            this.totalMovesLeft = q.getKey();
            this.fastestWallBuilders = q.getValue();
            this.runner = idxs.get(0); // TODO
            // this.mPlayer.myPlayers.runners.add(); Add runners
        } else {
            this.totalMovesLeft = p.getKey();
            this.fastestWallBuilders = p.getValue();
        }

        Point[] tmp = new Point[this.numWallPieces];
        for(int i=0; i<this.numWallPieces; i++){
            tmp[i] = this.mPlayer.playerPieces.get(this.fastestWallBuilders[i]);
        }

        // Log.log(Arrays.toString(tmp));
        this.WALL_BREACHED = false;
        this.breachers = new ArrayList<Integer>();
    }

    /**
     * Initializes this.numWallPieces and idealWallLocations
     */
    private void calculateWallPositions(){
        // Generate co-ordinates for ideal wall
        this.numWallPieces      = 11;
        this.wallXLocation      = (this.mPlayer.isPlayer1)? 21.5: -21.5;
        this.idealWallLocations = new Point[this.numWallPieces];

        for(int i=0; i<this.numWallPieces; i++){
            // Calculations done assuming perfect placement 
            // For 1cm coins on 40cm board
            this.idealWallLocations[i] = new Point(
                this.wallXLocation,
                2*i*Math.sqrt(3) + Math.sqrt(3) - 19
            );
        }

    }

    /**
     * Figure out the distance from each coin to ideal wall locations
     * 
     * @param pieces
     * @return
     */
    private double[][] calculateCostMatrix(
        HashMap<Integer, Point> mPieces, Set<Integer> ignored){

        double[][] costMatrix = new double[this.mPlayer.n][this.numWallPieces];
        for (int i=0; i < this.mPlayer.n; i++){
            for (int j=0; j < this.numWallPieces; j++)
                if (ignored.contains(i))
                    costMatrix[i][j] = 40.0;
                else 
                    costMatrix[i][j] = Utilities.numMoves(
                        this.idealWallLocations[j], mPieces.get(i));
        }
        return costMatrix;
    }

    /**
     * 
     * @param playerPieces
     * @param ignored
     * @return
     */
    private Pair<Integer, Integer[]> calculateWallStrategy(
        HashMap<Integer, Point> mPieces, Set<Integer> ignored){

        double[][] costMatrix = this.calculateCostMatrix(mPieces, ignored);

        // Solve hungarian algorithm for minimum placement in O(n^3)
        HungarianAlgorithm solver = new HungarianAlgorithm(costMatrix);
        int[] solution = solver.execute(); //TODO: Change to Integer[]

        // Parse calculated solution and expected wall cost
        Double    wallCost = 0.0;
        Integer[] bestPieceMatching = new Integer[11];

        for(int pieceID=0; pieceID < this.mPlayer.n; pieceID++){
            if(solution[pieceID] >= 0){
                wallCost += costMatrix[pieceID][solution[pieceID]];
                bestPieceMatching[solution[pieceID]] = pieceID;
            }
        }

        return new Pair<Integer, Integer[]>(
            wallCost.intValue(), bestPieceMatching);
    }
    
    /**
     * 
     * @param opponentPieces
     * @return
     */
    private Integer[] getWallPriority(HashMap<Integer, Point> opponentPieces){
        Integer[] distanceToWall = new Integer[11];
        for(int i=0; i<11; i++)
            distanceToWall[i] = Integer.MAX_VALUE;

        // Iterate through opponent pieces to prioritize wall
        for(int j=0; j<this.mPlayer.n; j++){
            for (int i=0; i<11; i++){
                Double numMoves = Utilities.numMoves(
                    opponentPieces.get(j), this.idealWallLocations[i]);
                
                if (numMoves < distanceToWall[i])
                    distanceToWall[i] = numMoves.intValue();
            }
        }

        return Utilities.argsort(distanceToWall);
    }

    public void getWallMove(List<Pair<Integer, Point>> moves, Integer numMoves){
        this.debugCount ++;

        int idx = 0;
        Integer[] idxPriority = this.getWallPriority(this.mPlayer.opponentPieces);

        while (moves.size() < numMoves && idx < this.numWallPieces){
            Integer pieceID = idxPriority[idx];

            Pair<Integer, Point> move = Utilities.getNextMove(
                this.pieceStore.myPieces.get(this.fastestWallBuilders[pieceID]),
                this.idealWallLocations[pieceID],
                this.fastestWallBuilders[pieceID],
                this.pieceStore.myPieces,
                this.pieceStore.oppPieces
            );

            if(move != null){
                moves.add(move);
                // movesLeft[pieceID]--;
                this.totalMovesLeft--;
                this.pieceStore.movePiece(move);
            } else idx++;
        }

        this.numMovesRequired += moves.size();
        if (moves.size()==0) {
            List<Integer> piecesLeft = new ArrayList<Integer>();
            for(int i=0; i<11; i++){
                if(!Board.almostEqual(
                    Board.getdist(
                        idealWallLocations[i],
                        this.pieceStore.myPieces.get(this.fastestWallBuilders[i])
                    ), 0))
                    piecesLeft.add(i);
            }

            if (piecesLeft.size() == 0){
                this.WALL_COMPLETED = true;
                Log.log(String.format(
                    "WALL COMPLETED in %d moves", this.numMovesRequired));
            } else {
                for (Integer oppPieceID: this.pieceStore.oppPieces.keySet()){
                    Point loc = this.pieceStore.oppPieces.get(oppPieceID);

                    if ((this.mPlayer.isPlayer1)? loc.x > 20.5: loc.x < -20.5){
                        if (!breachers.contains(oppPieceID))
                            breachers.add(oppPieceID);

                        if(!this.WALL_BREACHED)
                            Log.log("Detected wall breached");

                        this.WALL_BREACHED = true;
                    }
                }

                if (!this.WALL_BREACHED){
                    if(piecesLeft.size() == 2){
                        Integer idx1 = piecesLeft.get(0);
                        Integer idx2 = piecesLeft.get(1);

                        Integer tmp = this.fastestWallBuilders[idx1];
                        this.fastestWallBuilders[idx1] = this.fastestWallBuilders[idx2];
                        this.fastestWallBuilders[idx2] = tmp;
                    } else {
                        HashSet<Integer> runners = new HashSet<>();
                        runners.add(this.runner);
                        this.calculateWallStrategy(this.pieceStore.myPieces, runners);
                    }
                }
            }
        }
    }
}
