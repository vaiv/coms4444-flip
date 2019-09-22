package flip.g4;

import java.util.List;
import java.util.HashMap;
import javafx.util.Pair;

import flip.sim.Log;
import flip.sim.Board;
import flip.sim.Point;

import flip.g4.Player;
import flip.g4.Utilities;

enum WallDetectionStatus {
    WALL_DETECTED,
    WALL_HOLE_DETECTING,
    WALL_BREACHED
}

// Abstracted class for wall building
class AntiWallStrategy{

    private Player  mPlayer;
    private Integer runner;
    private boolean isPlayer1;

    public RunnerStatus status;

    public AntiWallStrategy(Player mPlayer, HashMap<Integer, Point> pieces, Integer runner){
        this.runner    = runner;
        this.mPlayer   = mPlayer;
        this.isPlayer1 = mPlayer.isPlayer1;

        this.updateRunnerStatus();
        Log.log("RUNNER INITIALIZED WITH STATUS "+String.valueOf(this.status) + 
            " FOR PIECE " + String.valueOf(runner)
        );
    }

    public boolean detectWall(){
        return false;
    }
    
}
