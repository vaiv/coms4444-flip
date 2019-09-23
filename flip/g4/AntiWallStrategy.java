package flip.g4;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import javafx.util.Pair;

import flip.sim.Log;
import flip.sim.Board;
import flip.sim.Point;

import flip.g4.Player;
import flip.g4.PieceStore;
import flip.g4.Utilities;

enum WallDetectionStatus {
    WALL_NOT_DETECTED,  // Initial status. No wall detected
    WALL_DETECTED,      // Wall detected at detectedXLocation
    WALL_HOLE_DETECTED, // Holes in the wall initialized
    WALL_IMPENETRABLE,  // Wall complete and not breached. :(
    WALL_BREACHED       // Piece placed in detectedXLocation
}

// Abstracted class for wall building
class AntiWallStrategy{

    private Player  mPlayer;
    private Integer runner;
    private boolean isPlayer1;

    public PieceStore pieceStore;
    public WallDetectionStatus status;

    public Double detectedXLocation;
    public List<Integer> detectedWallPieces;
    public Point targetBreach;

    public AntiWallStrategy(Player mPlayer, HashMap<Integer, Point> pieces, Integer runner){
        this.runner    = runner;
        this.mPlayer   = mPlayer;
        this.isPlayer1 = mPlayer.isPlayer1;

        this.status     =  WallDetectionStatus.WALL_NOT_DETECTED;
        this.pieceStore = mPlayer.pieceStore;
        Log.log("ANTIWALL INITIALIZED WITH STATUS " + String.valueOf(this.status) + 
            " FOR PIECE " + String.valueOf(runner));
    }

    public void updateStatus(){
        if(this.status == WallDetectionStatus.WALL_NOT_DETECTED)
            this.detectWall();

        if(this.status == WallDetectionStatus.WALL_DETECTED ||
            this.status == WallDetectionStatus.WALL_HOLE_DETECTED){
            this.updateWallGaps();
        }
    }

    private boolean checkWallPiece(Point a, Point b){
        // Simple logic. Just checks number of pieces at location X
        return Math.abs(a.x-b.x) < 0.2;
    }

    public boolean detectWall(){
        Integer prev = -1;
        for(Integer idx: this.pieceStore.movedPieces.keySet()){
            if(idx == prev) continue;

            Integer numPiecesInRow = 0;
            Point loc = this.pieceStore.oppPieces.get(idx);

            // Make sure loc.x makes sense
            //if(loc.x )

            for(Integer idx2: this.pieceStore.oppPieces.keySet()){
                if(this.checkWallPiece(loc, 
                    this.pieceStore.oppPieces.get(idx2)))
                    numPiecesInRow++;
            }

            if (numPiecesInRow >= 3){
                this.status = WallDetectionStatus.WALL_DETECTED;
                this.detectedXLocation = loc.x;
                return true;
            }
            prev = idx;
        }
        return false;
    }

    public void updateWallGaps(){
        List<Point> xLocationCount = new ArrayList<Point>();

        Point foundXLoc = new Point(this.detectedXLocation, 0);
        for(Integer idx: this.pieceStore.oppPieces.keySet()){
            Point oppLoc = this.pieceStore.oppPieces.get(idx);

            if(this.checkWallPiece(foundXLoc, oppLoc))
                xLocationCount.add(oppLoc);
        }

        Integer numWallPieces = xLocationCount.size();
        if(numWallPieces < 3){
            this.status = WallDetectionStatus.WALL_NOT_DETECTED;
            return;
        }

        xLocationCount.sort(Comparator.comparingDouble(loc->loc.x));
        this.detectedXLocation = xLocationCount.get(numWallPieces/2).x;

        xLocationCount.sort(Comparator.comparingDouble(loc->loc.y));

        this.targetBreach = null;
        Double minMovesRequired = 50.0;
        Point runnerLocation = this.pieceStore.myPieces.get(this.runner);

        for(int i=0; i<numWallPieces+1; i++){
            Double a = (i==0) ? -20.0 : xLocationCount.get(i-1).y;
            Double b = (i==numWallPieces) ? 20.0: xLocationCount.get(i).y;

            if(b - a < 2 * Math.sqrt(3)* 2 - 2) continue;
            
            Point targetLocation = new Point(this.detectedXLocation, (a+b)/2);
            Double numMoves = Utilities.numMoves(
                runnerLocation,
                targetLocation
            );

            if(numMoves < minMovesRequired){
                minMovesRequired = numMoves;
                this.targetBreach = targetLocation;
            }
        }

        if(this.targetBreach == null)
            this.status = WallDetectionStatus.WALL_IMPENETRABLE;
        else
            this.status = WallDetectionStatus.WALL_HOLE_DETECTED;
    }

    public List<Pair<Integer,Point>> getAntiWallMove(
        List<Pair<Integer,Point>> moves, Integer numMoves){
        
        while(moves.size() < numMoves){
            Pair<Integer, Point> move = Utilities.getNextMove(
                this.pieceStore.myPieces.get(this.runner),
                this.targetBreach,
                this.runner,
                this.pieceStore.myPieces,
                this.pieceStore.oppPieces
            );

            if(move != null){
                moves.add(move);
                this.pieceStore.movePiece(move);

                if(move.getValue().x == this.targetBreach.x){
                    this.status = WallDetectionStatus.WALL_BREACHED;
                }
            } else{
                break;
            }
        }

		return moves;
	}
}
