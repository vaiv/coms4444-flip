package flip.g4;

import java.util.Set;
import java.util.List;
import java.util.ListIterator;
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
class PostWallStrategy{
    private int debugCount = 0;
    // Store details about the game
    private Player mPlayer;
    private PieceStore pieceStore;
    private boolean isPlayer1;
    private WallStrategy mWallStrategy;

    // Wall co-ordinates and number of pieces
    public double    wallXLocation;
    public Point[]   idealPostWallLocations;

    // Index of pieces that can build wall the fastest
    private Integer[] fastestPostWallBuilders;

    private List<Integer> completedPieces = new ArrayList<Integer>();
    private List<Integer> prewallPieces   = new ArrayList<Integer>();
    private List<Integer> wallPieces      = new ArrayList<Integer>();
    private List<Integer> postWallPieces  = new ArrayList<Integer>();
    private List<Integer> postpostWallPieces = new ArrayList<Integer>();

    private List<PostWallRunnerStrategy> runners = new ArrayList<PostWallRunnerStrategy>();

    public PostWallStrategy(Player mPlayer, HashMap<Integer, Point> pieces){
        this.mPlayer = mPlayer;
        this.pieceStore = mPlayer.pieceStore;
        this.isPlayer1 = mPlayer.isPlayer1;
        this.mWallStrategy = mPlayer.mWallStrategy;

        
        this.wallXLocation = this.mWallStrategy.wallXLocation;
        this.calculatePostWallPositions();

        // Split players by location with respect to wall
        for(Integer pieceID: this.mWallStrategy.fastestWallBuilders)
            this.wallPieces.add(pieceID);

        Log.log("Fastest wall builders " + wallPieces.toString());

        for (Integer pieceID: this.pieceStore.myPieces.keySet()){
            if (this.wallPieces.contains(pieceID)) continue;
            Point loc = this.pieceStore.myPieces.get(pieceID);
            if(this.isPlayer1){
                if (loc.x < -18.9)
                    completedPieces.add(pieceID);
                else if( loc.x < this.wallXLocation){
                    prewallPieces.add(pieceID);
                } else
                    postpostWallPieces.add(pieceID);
            } else {
                if (loc.x > 21.1)
                    completedPieces.add(pieceID);
                else if( loc.x > this.wallXLocation)
                    prewallPieces.add(pieceID);
                else
                    postpostWallPieces.add(pieceID);
            }
        }

        Log.log("Pre  wall pieces " + prewallPieces.toString());
        Log.log("Post wall pieces " + postpostWallPieces.toString());
        Log.log("Completed pieces " + completedPieces.toString());

        this.calculatePostWallStrategy(this.postpostWallPieces);

        for(int i=0; i<10; i++){
            Integer pID = this.fastestPostWallBuilders[i];
            postpostWallPieces.remove(pID);
            postWallPieces.add(pID);
        }
    }

    /**
     * Initializes this.numWallPieces and idealWallLocations
     */
    private void calculatePostWallPositions(){
        this.idealPostWallLocations = new Point[10];
        for(int i=0; i<10; i++){
            Point wallLoc = this.mWallStrategy.idealWallLocations[i];
            this.idealPostWallLocations[i] = new Point(
                wallLoc.x + ((this.isPlayer1)? 1: -1),
                wallLoc.y + Math.sqrt(3)
            );
        }

        Log.log(Arrays.toString(this.idealPostWallLocations));
    }

    /**
     * Figure out the distance from each coin to ideal wall locations
     * 
     * @param pieces
     * @return
     */
    private double[][] calculateCostMatrix(List<Integer> pieces){

        double[][] costMatrix = new double[pieces.size()][10];
        for (int i=0; i < pieces.size(); i++){
            for (int j=0; j < 10; j++)
                costMatrix[i][j] = Utilities.numMoves(
                    this.idealPostWallLocations[j],
                    this.pieceStore.myPieces.get(pieces.get(i)));
        }
        return costMatrix;
    }

    /**
     * 
     * @param playerPieces
     * @param ignored
     * @return
     */
    private void calculatePostWallStrategy( List<Integer> pieces){

        double[][] costMatrix = this.calculateCostMatrix(pieces);

        // Solve hungarian algorithm for minimum placement in O(n^3)
        HungarianAlgorithm solver = new HungarianAlgorithm(costMatrix);
        int[] solution = solver.execute();

        // Parse calculated solution and expected wall cost
        this.fastestPostWallBuilders = new Integer[11];

        for(int pieceID=0; pieceID < pieces.size(); pieceID++){
            if(solution[pieceID] >= 0)
                this.fastestPostWallBuilders[solution[pieceID]] = pieces.get(pieceID);
        }

        Log.log("Post wall strategy : " + Arrays.toString(this.fastestPostWallBuilders));
    }
    
    private boolean switcheroo(List<Pair<Integer, Point>> moves, Integer postWallPieceIdx, Integer wallPieceIdx){
        Integer pieceID     = this.fastestPostWallBuilders[postWallPieceIdx];
        Integer wallPieceID = this.mWallStrategy.fastestWallBuilders[wallPieceIdx];

        Point wallLoc = this.mWallStrategy.idealWallLocations[wallPieceIdx];
        
        Pair<Integer, Point> move;
        // Check for wall move out
        move = new Pair<Integer, Point>(wallPieceID, new Point(
            wallLoc.x + ((isPlayer1) ? -2 : 2),
            wallLoc.y
        ));

        if(Utilities.check_validity(move, this.mPlayer.playerPieces, this.mPlayer.opponentPieces)){

            // Log.log(String.format("SWITCHING OUT %d for %d", wallPieceID, pieceID));
            Pair<Integer, Point> replaceMove = new Pair<Integer, Point>(pieceID, new Point(wallLoc));
            moves.add(move);
            moves.add(replaceMove);

            this.pieceStore.movePiece(move);
            this.pieceStore.movePiece(replaceMove);

            this.wallPieces.remove(wallPieceID);
            this.runners.add(new PostWallRunnerStrategy(this.mPlayer, wallPieceID));
            this.prewallPieces.add(wallPieceID);
            
            this.postWallPieces.remove(pieceID);
            this.wallPieces.add(pieceID);

            Integer newPiece = null;
            Double newPieceDist = Double.MAX_VALUE;
            for(Integer pID: this.postpostWallPieces){
                Double newDist = Utilities.numMoves(
                    this.pieceStore.myPieces.get(pID),
                    this.idealPostWallLocations[postWallPieceIdx]);
                if(newPiece == null ||  newDist < newPieceDist){
                    newPiece = pID;
                    newPieceDist = newDist;
                }
            }

            if(newPiece != null){
                this.postpostWallPieces.remove(newPiece);
                this.postWallPieces.add(newPiece);
            }
            this.mWallStrategy.fastestWallBuilders[wallPieceIdx] = pieceID;
            this.fastestPostWallBuilders[postWallPieceIdx] = newPiece;
            
            return true;
        }
        return false;
    }

    public void getPostWallMove(List<Pair<Integer, Point>> moves, Integer numMoves){
        this.debugCount ++;

        // update statuses
        Integer prevSize = moves.size();
        // if(moves.size() != 0) Log.log("Post wall received moves "+moves.toString());

        // perform move
        Integer pieceID = 0;
        while (moves.size() < numMoves && pieceID < 10){
            if(this.fastestPostWallBuilders[pieceID] == null){
                pieceID++;
                continue;
            }

            Point a = this.pieceStore.myPieces.get(this.fastestPostWallBuilders[pieceID]);
            Point b = this.idealPostWallLocations[pieceID];

            // If in position, try to make switch
            if(Board.almostEqual(Board.getdist(a, b), 0)){
                if(moves.size() == 0)
                    if(!switcheroo(moves, pieceID, pieceID))
                        switcheroo(moves, pieceID, pieceID+1);
                pieceID++;
            } else {
                Pair<Integer, Point> move = Utilities.getNextMove(
                    a, b, 
                    this.fastestPostWallBuilders[pieceID],
                    this.pieceStore.myPieces,
                    this.pieceStore.oppPieces
                );

                if(move != null){
                    moves.add(move);
                    this.pieceStore.movePiece(move);
                } else pieceID++;
            }
        }

        if (moves.size()==0) {
            if(this.mWallStrategy.WALL_COMPLETED || this.mWallStrategy.WALL_BREACHED){
                Integer prev = moves.size() - 1;
                while(prev != moves.size() && moves.size() < numMoves){
                    // Choose a random move for pre wall pieces
                    ListIterator<PostWallRunnerStrategy> iter = this.runners.listIterator();
                    while(iter.hasNext()){
                        PostWallRunnerStrategy rID = iter.next();
                        rID.getRunnerMove(moves, numMoves);

                        if(rID.JOURNEY_COMPLETE){
                            iter.remove();
                            this.completedPieces.add(rID.runner);
                            this.prewallPieces.remove(rID.runner);
                        }
                    }
                    prev = moves.size();
                }
            }

            
        }

        // Integer moveCount = moves.size()-prevSize;
        // if(prevSize != 2){
        //     Log.log("Post wall returning move count "+String.valueOf(moveCount));
        //     Log.log(moves.toString());
        // }
    }
}
