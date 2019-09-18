package flip.g4;

import java.util.List;
import java.util.HashMap;
import javafx.util.Pair;

import flip.sim.Log;
import flip.sim.Board;
import flip.sim.Point;

import flip.g4.Player;
import flip.g4.Utilities;

enum RunnerStatus {
    RUNNER_SET,
    RUNNER_PASSED_WALL,
    RUNNER_BLOCKED,
    RUNNER_REACHED_END,
    RUNNER_NONE
}

// Abstracted class for wall building
class RunnerStrategy{

    private Player  mPlayer;
    private Integer runner;
    private boolean isPlayer1;

    public RunnerStatus status;

    public RunnerStrategy(Player mPlayer, HashMap<Integer, Point> pieces, Integer runner){
        this.runner    = runner;
        this.mPlayer   = mPlayer;
        this.isPlayer1 = mPlayer.isPlayer1;

        this.updateRunnerStatus();
        Log.log("RUNNER INITIALIZED WITH STATUS "+String.valueOf(this.status) + 
            " FOR PIECE " + String.valueOf(runner)
        );
    }

    private void updateRunnerStatus(){
        if(this.runner == null)
            this.status = RunnerStatus.RUNNER_NONE;
        else if(this.checkRunnerReachedEnd())
            this.status = RunnerStatus.RUNNER_REACHED_END;
        else if(this.checkRunnerPassedWall())
            this.status = RunnerStatus.RUNNER_PASSED_WALL;
        else
            this.status = RunnerStatus.RUNNER_SET;
    }
    
    private boolean checkRunnerPassedWall(){
        Point runnerLoc = this.mPlayer.playerPieces.get(this.runner);

        if(this.isPlayer1)
            return runnerLoc.x < this.mPlayer.mWallStrategy.wallXLocation - 2.0;
        else
            return runnerLoc.x > this.mPlayer.mWallStrategy.wallXLocation + 2.0;
    }

    private boolean checkRunnerReachedEnd(){
        Point runnerLoc = this.mPlayer.playerPieces.get(this.runner);
        return (this.isPlayer1) ? runnerLoc.x < -21.0 : runnerLoc.x > 21.0;
    }
    
    public void getRunnerMove(List<Pair<Integer, Point>> moves, Integer numMoves){
        if(this.status == RunnerStatus.RUNNER_SET){
            // Try passing wall
            Point runnerLoc = this.mPlayer.playerPieces.get(this.runner);
            
            Pair<Integer, Point> move = new Pair<Integer, Point>(this.runner, 
                new Point(runnerLoc.x+((this.isPlayer1)?-2:2), runnerLoc.y
            ));

            if(Utilities.check_validity(move,
                this.mPlayer.playerPieces, this.mPlayer.opponentPieces))
                moves.add(move);
        }

        else if (this.status == RunnerStatus.RUNNER_PASSED_WALL){
            // Dash to end
        }

        RunnerStatus prev = this.status;
        this.updateRunnerStatus();
        if (this.status != prev){
            Log.log("RUNNER STATUS UPDATED FROM " + prev.toString() + 
                    " TO " + this.status.toString());
        }
    }
}
