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
            if(this.updateWallGaps())
                this.status = WallDetectionStatus.WALL_HOLE_DETECTED;
            else
                this.status = WallDetectionStatus.WALL_IMPENETRABLE;
        }
    }

    private boolean checkWallPiece(Point a, Point b){
        // Simple logic. Just checks number of pieces at location X
        return Math.abs(a.x-b.x) < 0.5;
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
                Log.log("WALL DETECTED AT " + String.valueOf(loc.x));
                return true;
            }
            prev = idx;
        }
        return false;
    }

    public boolean updateWallGaps(){
        List<Point> xLocationCount = new ArrayList<Point>();

        Point foundXLoc = new Point(this.detectedXLocation, 0);
        for(Integer idx: this.pieceStore.oppPieces.keySet()){
            Point oppLoc = this.pieceStore.oppPieces.get(idx);

            if(this.checkWallPiece(foundXLoc, oppLoc))
                xLocationCount.add(oppLoc);
        }

        xLocationCount.sort(Comparator.comparingDouble(loc->loc.x));
        this.detectedXLocation = xLocationCount.get(xLocationCount.size()/2).x;

        xLocationCount.sort(Comparator.comparingDouble(loc->loc.y));

        Double maxGap = xLocationCount.get(0).y;
        Double maxGapLocation = xLocationCount.get(0).y / 2;
        for(int i=1; i<xLocationCount.size(); i++){
            Double gap = xLocationCount.get(i).y - xLocationCount.get(i-1).y;
            if (gap > maxGap){
                maxGap = gap;
                maxGapLocation = (xLocationCount.get(i).y + xLocationCount.get(i-1).y) / 2;
            }
        }

        if (maxGap < Math.sqrt(3)) return false;

        this.targetBreach = new Point(this.detectedXLocation, maxGapLocation);
        return true;
    }

    public List<Pair<Integer,Point>> getAntiWallMove(
        List<Pair<Integer,Point>> moves, Integer numMoves){
        
        
		return moves;
	}
}
