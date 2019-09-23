package flip.group5;
import java.util.*;
import java.util.List;

import javafx.util.Pair;

import flip.sim.Point;
import flip.sim.Board;
import flip.sim.Log;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Player implements flip.sim.Player
{
    private int seed = 42;
    private Random random;
    private boolean isplayer1;
    private Integer n;
    private Double diameter_piece;
    private boolean dispatch_runner_mode = true;
    private boolean defense_mode = false;  // when true, we play all moves to build wall, else all moves are offense
    private boolean make_run_mode = false;
    private Point goal;  // to be set after wall is made
    private Integer runnerID; // dito as above
    private int turn_counter = 0;
    private double prefer_theta = isplayer1 ? -70 : 70;

    private static double BLOCKADE_LINE = 21;
    private static final double EPSILON = 1E-7;
    private static final double B1  = -17.30;
    private static final double B2  = 17.30;
    private static final double B3  = -13.84;
    private static final double B4  = 13.84;
    private static final double B5  = -10.38;
    private static final double B6  = 10.38;
    private static final double B7  = -6.92;
    private static final double B8  = 6.92;
    private static final double B9  = -3.46;
    private static final double B10 = 3.46;
    private static final double B11 = 0.00;

    private static final List<Double> BLOCKADE_YCOORD = new ArrayList<>(Arrays.asList(B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11));
    private List<Pair<Integer, Point>> blockadeList = new ArrayList<>();
    private Map<Integer, Integer> blockadeMap = new HashMap<>();
    public Player()
    {
        random = new Random(seed);
    }

    public void init(HashMap<Integer, Point> pieces, int n, double t, boolean isplayer1, double diameter_piece)
    {
        this.n = n;
        this.isplayer1 = isplayer1;
        this.diameter_piece = diameter_piece;
        this.setBlockadeLine(pieces);
        this.initializeBlockadeList(isplayer1);
        this.computeBlockadeMapMWBM(pieces);
    }

    /**
    * Core getMoves function.  While we still have moves to make, add defense or offense moves depending on global mode
    */
    public List<Pair<Integer, Point>> getMoves(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
        this.turn_counter++;
        if (this.n <= 11) { // just play forward if we can't play wall strategy
            return getMovesSmallN(num_moves, player_pieces, opponent_pieces, isplayer1);
        }

        // n > 11, play wall strategy, which has 3 modes or game states
        if (this.dispatch_runner_mode) {  //get runner in front of barrier line
            identifyRunner(player_pieces, isplayer1);
            refreshGameState(player_pieces, isplayer1);
            return getRunnerMoves(num_moves, player_pieces, opponent_pieces, isplayer1);
        }

        else if (this.defense_mode) {  // Play defense.  Stop when barrier is formed.
            refreshGameState(player_pieces, isplayer1);
            if (this.defense_mode) {
                return getDefenseMoves(player_pieces, opponent_pieces, isplayer1);
            }
        }

        else {   // play offense if runner is still not in the endzone
            refreshGameState(player_pieces, isplayer1);
            if (this.make_run_mode) {
                return getRunnerMoves(num_moves, player_pieces, opponent_pieces, isplayer1);
            }
        }
        return null;  // our wall should be up and runner at goal
    }

    private void refreshGameState(HashMap<Integer, Point> player_pieces, boolean isplayer1) {
        if (isRunnerPastDefense(this.runnerID, player_pieces, isplayer1) && this.dispatch_runner_mode) {  // runner is far enough out, now build the wall starting next turn!
            this.dispatch_runner_mode = false;
            this.defense_mode = true;
        }
        if (this.defense_mode && isBlockadeComplete(player_pieces)) {
            this.defense_mode = false;
            this.make_run_mode = true;}
        if (isPointInEndZone(player_pieces.get(runnerID), isplayer1)) {this.make_run_mode = false;}
    }

    private void identifyRunner(HashMap<Integer, Point> player_pieces, boolean isplayer1) {
        if (this.runnerID == null) {
            this.runnerID = getClosestRunnerNotInBlockade(player_pieces, isplayer1);  // identify runner
        }
    }

    private void setBlockadeLine(Map<Integer, Point> pieces) {
        for (Point p : pieces.values()) {
            this.BLOCKADE_LINE = Math.min(BLOCKADE_LINE, Math.abs(p.x)-diameter_piece);
        }
    }

    // Function to handle game with small n.  Let's look into why this seems poor.
    private List<Pair<Integer, Point>> getMovesSmallN(int num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
        if (weHaveCOMAdvantage(player_pieces, opponent_pieces, isplayer1)) {
            return getCOMAdvantageMoves(player_pieces, opponent_pieces, isplayer1);
        }

        List<Pair<Integer, Point>> moves = tryMovingAllStraight(num_moves, player_pieces, opponent_pieces, isplayer1);
        if (moves.size() == num_moves) {
            return moves;
        }
        else {
            return handleAllStuck(num_moves, moves, player_pieces, opponent_pieces, isplayer1);
        }
    }

    private List<Pair<Integer, Point>> getCOMAdvantageMoves(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
        List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();
        int trials = 30;
        int i = 0;
        while (moves.size() != 2 && i < trials) {
            i++;
            Pair<Integer, Point> move = getForwardMove(player_pieces, opponent_pieces, isplayer1);
            addMove(moves, move, player_pieces);
        }
        return moves;
    }

    // get a move that takes some random peice not in the endzone forward
    private Pair<Integer, Point> getForwardMove(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
        for (int i=0; i < this.n; i++) {
            Point piece = player_pieces.get(i);
            if (!didPointScore(piece, isplayer1)) { // move piece
                Pair<Integer, Point> move = getSingleMove(i, player_pieces, 0, isplayer1);
                if (check_validity(move, player_pieces, opponent_pieces)) {
                    return move;
                }
                move = getAroundWithPreferredAngle(i, move, player_pieces, opponent_pieces, isplayer1, 0);
                if (check_validity(move, player_pieces, opponent_pieces) && move != null) {
                    return move;
                }
                else {
                    return forceForwardMove(i, player_pieces, opponent_pieces, isplayer1);
                }
            }
        }
        return null;
    }

    private Pair<Integer, Point> forceForwardMove(int piece_id, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
        for (int theta=0; theta<90; theta++) {
            Pair<Integer, Point> proposal_move = getSingleMove(piece_id, player_pieces, theta, isplayer1);
            if (check_validity(proposal_move, player_pieces, opponent_pieces)) {
                return proposal_move;
            }
            else {
                proposal_move = getSingleMove(piece_id, player_pieces, -theta, isplayer1);
                if (check_validity(proposal_move, player_pieces, opponent_pieces)) {
                    return proposal_move;
                }
            }
        }
        return null;
    }

    // Can't move any peice straight, decide what to do
    // We move around our own peices if we are very close to scoring
    // otherwise we are willing to move around opponent peices unless it's a 1 vs 1 case and we calculate a disadvantage
    private List<Pair<Integer, Point>> handleAllStuck(int num_moves, List<Pair<Integer, Point>> moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
        // first priority, get peices in that are close to goal
        Set<Integer> stuck = new HashSet<Integer>();
        int i = 0;
        int trials = this.n * 2;
        while (moves.size() != num_moves && i < trials) {
            i = i + 1;
            int id = getFurthestForward(player_pieces, stuck, isplayer1);
            Point proposed_piece = player_pieces.get(id);
            if (isPointInEndZone(proposed_piece, isplayer1)) {
                stuck.add(id);
            }
            else if (isPieceCloseToEndZone(proposed_piece, isplayer1)) { // run in it regardless of who's blocking
                boolean moveAdded = false;
                Pair<Integer, Point> invalid_move = new Pair<Integer, Point>(id, new Point(proposed_piece.x + 2, proposed_piece.y));
                Pair<Integer, Point> move = getAroundFanning(id, invalid_move, player_pieces, opponent_pieces, isplayer1, 0);
                if (move != null && check_validity(move, player_pieces, opponent_pieces)) {  //otherwise this is giving me invalid but not null moves.  Figure this out.
                    addMove(moves, move, player_pieces);
                    moveAdded = true;
                }
                else {  //double check that we can't find a forward move
                    move = findAnyForwardMove(id, player_pieces, opponent_pieces, isplayer1);
                    if (move != null && check_validity(move, player_pieces, opponent_pieces)) {
                         addMove(moves, move, player_pieces);
                         moveAdded = true;
                    }
                }
                if (!moveAdded) {
                    stuck.add(id);
                }
            }
        }
        if (moves.size() < num_moves) {
            Pair<Integer, Point> move = makeRoom(player_pieces, opponent_pieces, isplayer1);
            addMove(moves, move, player_pieces);
        }
        if (moves.size() < num_moves) {
            Pair<Integer, Point> move = moveAroundSelf(player_pieces, opponent_pieces, isplayer1);
            addMove(moves, move, player_pieces);

        }
        return moves;
    }


    private Pair<Integer, Point> moveAroundSelf(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
        System.out.print("CALLING MOVE AROUND SELF");
        // find piece that's blocked by self
        Set<Integer> stuck = new HashSet<Integer>();  // becomes a set of pieces to not move, not really stuck per say
        for(int i=0; i<50; i++) {
            int piece_id = getFurthestBack(player_pieces, stuck, isplayer1);
            Point proposed_piece = player_pieces.get(piece_id);
            boolean condition1 = isSelfBlockingUs(player_pieces, proposed_piece, isplayer1);
            boolean condition2 = isOpponentBlockingUs(player_pieces, opponent_pieces, proposed_piece, isplayer1);
            if (condition1 && !condition2) {
                Pair<Integer, Point> invalid_move = getSingleMove(piece_id, player_pieces, 0, isplayer1);
                Pair<Integer, Point> move = getAroundWithPreferredAngle(piece_id, invalid_move, player_pieces, opponent_pieces, isplayer1, 0);
                return move;
            }
            else {
                stuck.add(piece_id);
            }
        }
        return null;
    }

    private Pair<Integer, Point> makeRoom(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
        for (int id=0; id<this.n; id++) {
            Point piece = player_pieces.get(id);
            if (isPointInEndZone(piece, isplayer1) && isBlockingOwnPiece(player_pieces, piece, isplayer1) && shouldPieceEverMove(piece, isplayer1)) {
                return findAnyForwardMove(id, player_pieces, opponent_pieces, isplayer1);
                }
            }
        return null;
    }

    private boolean shouldPieceEverMove(Point piece, boolean isplayer1) {
        double threshhold = 5;
        return isplayer1 ? piece.x > -20 - threshhold : piece.x < 20 + threshhold;
    }

    private Pair<Integer, Point> findAnyForwardMove(int piece_id, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
        for (int theta = -90; theta < 90; theta++) {
            Pair<Integer, Point> move = getSingleMove(piece_id, player_pieces, theta, isplayer1);
            if (check_validity(move, player_pieces, opponent_pieces)) {
                return move;
            }
        }
        return null;
    }


    boolean isBlockingOwnPiece(HashMap<Integer, Point> player_pieces, Point piece, boolean isplayer1) {
        double x_behind = isplayer1 ? piece.x + 2.5 : piece.x - 2.5;
        Point upperLeft = new Point(x_behind - 2, piece.y - 3);
        Point lowerRight = new Point(x_behind + 4, piece.y + 4);
        int count = countInBounds(player_pieces, upperLeft, lowerRight);
        return count > 0;
    }

    private Pair<Integer, Point> getAroundFanning(Integer runner_id, Pair<Integer, Point> invalid_move, HashMap<Integer, Point> player_pieces,
                             HashMap<Integer, Point> opponent_pieces, boolean isplayer1, double preferred_angle) {
        for (double theta=0; theta<360; theta++) {
            Pair<Integer, Point> move = getAroundWithPreferredAngle(runner_id, invalid_move, player_pieces, opponent_pieces, isplayer1, theta);
            if (!isMoveNull(move) && check_validity(move, player_pieces, opponent_pieces)) {
                return move;
            }
        }
        return null;
    }

    private boolean isPieceCloseToEndZone(Point piece, boolean isplayer1) {
        double threshhold = 15;
        return isplayer1 ? piece.x < -threshhold : piece.x > threshhold;
    }

    private boolean isOpponentBlockingUs(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, Point proposed_piece, boolean isplayer1) {
        double delta = isplayer1 ? -2 : + 2;
        Point desired_point = new Point(proposed_piece.x + delta, proposed_piece.y);
        for (int i=0; i<this.n; i++) {
            Point opponent_piece = opponent_pieces.get(i);
            double dx = opponent_piece.x - desired_point.x;
            double dy = opponent_piece.y - desired_point.y;
            double r = 2;
            boolean blocking_condition = (dx*dx + dy*dy < r*r);
            if (blocking_condition) {
                return true;
            }
        }
        return false;
    }

    private boolean isSelfBlockingUs(HashMap<Integer, Point> player_pieces, Point proposed_piece, boolean isplayer1) {
        double delta = isplayer1 ? -2 : + 2;
        Point desired_point = new Point(proposed_piece.x + delta, proposed_piece.y);
        for (int i=0; i<this.n; i++) {
            Point own_piece = player_pieces.get(i);
            double dx = own_piece.x - desired_point.x;
            double dy = own_piece.y - desired_point.y;
            double r = 2;
            boolean blocking_condition = (dx*dx + dy*dy < r*r);
            if (blocking_condition) {
                return true;
            }
        }
        return false;
    }

    private List<Pair<Integer, Point>> tryMovingAllStraight(int num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
        Set<Integer> stuck = new HashSet<Integer>();
        List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();

        int i = 0;
        int trials = this.n * 2;
        while (moves.size() != num_moves && i < trials) {
            i++;
            int proposed_piece;
            if (allPeicesOut(player_pieces, isplayer1)) {
                proposed_piece = getFurthestForward(player_pieces, stuck, isplayer1);
            }
            else {
                proposed_piece = getFurthestBack(player_pieces, stuck, isplayer1);
            }
            Pair<Integer, Point> move = getSingleMove(proposed_piece, player_pieces, 0, isplayer1);
            boolean inEndZone = isPointInEndZone(player_pieces.get(proposed_piece), isplayer1);
            if (check_validity(move, player_pieces, opponent_pieces) && !inEndZone) {
                addMove(moves, move, player_pieces);
            }
            else {
                stuck.add(proposed_piece);
            }
        }
        return moves;
    }

    private boolean allPeicesOut(HashMap<Integer, Point> player_pieces, boolean isplayer1) {
        for (int i=0; i<this.n; i++) {
            double x = player_pieces.get(i).x;
            boolean piece_out = isplayer1 ? x < 21 : x > -21;
            if (!piece_out) {
                return false;
            }
        }
        return true;
    }

    private double meanDistanceFromEndZone(HashMap<Integer, Point> player_pieces, boolean isplayer1) {
        double COM = getCenterOfMass(player_pieces, "x", new Point(-60, -20), new Point(60, 20));
        double meanDistance = isplayer1 ? COM + 20 : 20 - COM;
        return meanDistance;
    }

    private boolean weHaveCOMAdvantage(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
        double threshhold = .5;
        double our_distance = meanDistanceFromEndZone(player_pieces, isplayer1);
        double opponent_distance = meanDistanceFromEndZone(opponent_pieces, !isplayer1);
        return our_distance < opponent_distance - threshhold;
    }

    // adds move and updates our pieces!
    private void addMove(List<Pair<Integer, Point>> moves, Pair<Integer, Point> move, HashMap<Integer, Point> player_pieces) {
        if (move != null) {
            moves.add(move);
            player_pieces.put(move.getKey(), move.getValue());
        }
    }

    // is a point in the end zone?  Mainly used to stop runners when they are far enough.
    private boolean isPointInEndZone(Point point, boolean isplayer1) {
        return isplayer1 ? point.x < -22 : point.x > 22;
    }

    private boolean didPointScore(Point point, boolean isplayer1) {
        return isplayer1 ? point.x < -21.01 : point.x > 22.01;
    }

    // returns random move, code from TA
    private Pair<Integer, Point> getRandomMove(Integer piece_id, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
        Point curr_position = player_pieces.get(piece_id);
        Point new_position = new Point(curr_position);
        double theta = -Math.PI/2 + (Math.PI * random.nextDouble());
        Pair<Integer, Point> move = getSingleMove(piece_id, player_pieces, theta, isplayer1);
        return check_validity(move, player_pieces, opponent_pieces) ? move : null;
    }

    // Very useful function for determining a single move based on a decided theta and piece to move.
    private Pair<Integer, Point> getSingleMove(int piece_id, HashMap<Integer, Point> player_pieces, double theta, boolean isplayer1) {
        int orientation = isplayer1 ? -1 : 1;
        double dx = diameter_piece * Math.cos(theta) * orientation;
        double dy = diameter_piece * Math.sin(theta) * orientation;
        Point new_position = new Point(player_pieces.get(piece_id));
        new_position.x += dx; new_position.y += dy;
        Pair<Integer, Point> move = new Pair<Integer, Point>(piece_id, new_position);
        return move;
    }

    private Pair<Integer, Point> getSingleValidMove(int piece_id, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, double theta, boolean isplayer1) {
        Pair<Integer, Point> move = getSingleMove(piece_id, player_pieces, theta, isplayer1);
        if (check_validity(move, player_pieces, opponent_pieces)) {
            return move;
        }
        return null;
    }

    //getRunnerMoves tries to go straight, and if not, call getAround function.
    public List<Pair<Integer, Point>> getRunnerMoves(Integer num_moves, HashMap<Integer, Point> player_pieces,
                                                     HashMap<Integer, Point> opponent_pieces, boolean isplayer1)
    {
        List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();
        List<Pair<Integer, Point>> nearby_oppo = new ArrayList<Pair<Integer, Point>>();
        int num_trails = 30;
        int i = 0;
        //we can default this to be 0 for now
        double preferred_angle = 0;


        while(moves.size()!=num_moves && i< num_trails) {
            //Integer piece_id = random.nextInt(n);
            Integer piece_id = this.runnerID;
            Pair<Integer, Point> move = getSingleMove(piece_id, player_pieces, preferred_angle, isplayer1);
            if(check_validity(move, player_pieces, opponent_pieces)) {
                addMove(moves, move, player_pieces);
            }

            else {
                //can't move to the preferred angle we try to getAround
                //if the piece is far away from wall, use getAroundWithPreferredAngle
                Pair<Integer, Point> move_around;
                if (isplayer1 ? player_pieces.get(piece_id).x >= -15 : player_pieces.get(piece_id).x <= 15){
                    move_around = getAroundWithPreferredAngle(piece_id, move, player_pieces, opponent_pieces, isplayer1, preferred_angle);
                    //-10000 means there is no valid move possible
                    if (move_around.getValue().x != -10000.0 && move_around.getValue().y != -10000.0 ){
                        addMove(moves, move_around,player_pieces);
                    }
                }
                else{
                    // if a piece is close to the wall, switch to force move
                    updateRunnerMomentum(piece_id, player_pieces, isplayer1);
                    move_around = forceMove(piece_id, player_pieces, opponent_pieces, prefer_theta, isplayer1, 1);
                    if (move_around!=null){
                        addMove(moves,move_around,player_pieces);
                    }
                }
            }
            i++;
        }
        return moves;
    }

    private void updateRunnerMomentum(int piece_id, HashMap<Integer, Point> player_pieces, boolean isplayer1) {
        //specify which wall does it hit
        //hit the down wall, want to go up
        if (player_pieces.get(piece_id).y >= 17){
            this.prefer_theta = isplayer1 ? Math.abs(this.prefer_theta) : -1 * Math.abs(this.prefer_theta);
        }
        //hit the upper wall
        else if (player_pieces.get(piece_id).y <= -17){
            this.prefer_theta = isplayer1 ? -1 * Math.abs(this.prefer_theta) : Math.abs(this.prefer_theta);
        }
    }

    private boolean isMoveNull(Pair<Integer, Point> move) {
        return (move.getValue().x == -10000);
    }

    private Integer getClosestRunnerNotInBlockade(HashMap<Integer, Point> player_pieces, boolean isplayer1) {
        int best_runner = 0;
        double best_x = isplayer1 ? 1000 : -1000;
        for (int i = 0; i < this.n; i++) {
            double x = player_pieces.get(i).x;
            boolean condition1 = !this.blockadeMap.values().contains(i);
            boolean condition2 = isplayer1 ? (x < best_x) : (x > best_x);
            if (condition1 && condition2) {
                best_runner = i;
                best_x = x;
            }
        }
        return best_runner;
    }

    private boolean isRunnerPastDefense(Integer piece_id, HashMap<Integer, Point> player_pieces, boolean isplayer1) {
        double curr_x = player_pieces.get(piece_id).x;
        return isplayer1 ? (curr_x < 20 - this.diameter_piece) : (curr_x > -20 + this.diameter_piece);
    }


    // CORE DEFENSE MOVES FUNCTION.  GET PAIR OF BLOCKADE MOVES
    private List<Pair<Integer, Point>> getDefenseMoves(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
        this.computeBlockadeImportance(opponent_pieces, isplayer1);
        return getMovesForBlockade(player_pieces, opponent_pieces, isplayer1);
    }

    // SEE ALL HELPER / STATE FEATURE FUNCTIONS BELOW

    // calculate whether a particular piece is in some rectangular region
    private boolean inBounds(Point piece, Point upperLeft, Point lowerRight) {
        return piece.x > upperLeft.x && piece.x < lowerRight.x && piece.y > upperLeft.y && piece.y < lowerRight.y;
    }

    // counts the number of pieces in a rectangular region. Pieces could either be opponent's HashMap or your own.
    private int countInBounds(HashMap<Integer, Point> pieces, Point upperLeft, Point lowerRight) {
        int num_in_bounds = 0;
        for (int i=0; i<this.n; i++) {
            if (inBounds(pieces.get(i), upperLeft, lowerRight)) {num_in_bounds += 1;}
        }
        return num_in_bounds;
    }

    // Can get center of mass in x or y dimension.  Will only consider pieces in some rectangular region specified
    // by upperLeft and lowerRight
    private double getCenterOfMass(HashMap<Integer, Point> pieces, String dim, Point upperLeft, Point lowerRight) {
        double sum = 0;
        int num_in_bounds = 0;
        for (int i=0; i<this.n; i++) {
            Point piece = pieces.get(i);
            if (!inBounds(piece, upperLeft, lowerRight)) {}
            else {
                num_in_bounds += 1;
                double coord = (dim == "x") ? piece.x : piece.y;
                sum += coord;
            }
        }
        return sum / num_in_bounds;
    }

    // find the our own furthest back peice that's not currently unable to move directly forward
    private int getFurthestBack(HashMap<Integer, Point> player_pieces, Set<Integer> stuck, boolean isplayer1) {
        double furthest_back = isplayer1 ? -1000 : 1000.0;
        int piece_to_return = 0;
        for (int j = 0; j < n; j++) {
            double x_location = player_pieces.get(j).x;
            boolean condition1 = x_location < furthest_back && !stuck.contains(j) && !isplayer1;
            boolean condition2 = x_location > furthest_back && !stuck.contains(j) && isplayer1;
            if (condition1 || condition2) {
                piece_to_return = j;
                furthest_back = x_location;
            }
        }
        return piece_to_return;
    }

    private int getFurthestForward(Map<Integer, Point> player_pieces, Set<Integer> stuck, boolean isplayer1) {
        double furthest_forward = isplayer1 ? 1000 : -1000;
        int piece_to_return = 0;
        for (int j = 0; j < n; j++) {
            double x_location = player_pieces.get(j).x;
            boolean condition1 = x_location > furthest_forward && !stuck.contains(j) && !isplayer1;
            boolean condition2 = x_location < furthest_forward && !stuck.contains(j) && isplayer1;
            if (condition1 || condition2) {
                piece_to_return = j;
                furthest_forward = x_location;
            }
        }
        return piece_to_return;
    }

    // This function breaks the map into k horizontal slices in front of location x.  It returns an array with
    // the number of opponent pieces in each of those slices.  This will be a useful high level feature.
    private int[] getOpponentDensity(int k, double x, boolean opponent_is_player1, HashMap<Integer, Point> opponentPieces) {
        int[] densities = new int[k];
        double x_end = opponent_is_player1 ? 60 : -60;
        double x_left = opponent_is_player1 ? x : x_end;
        double x_right = opponent_is_player1 ? x_end : x;
        for (int i=0; i<k; i++) {
            double boundWidth = 40.0 / k;
            double upperBound = -20 + boundWidth * i;
            double lowerBound = upperBound + boundWidth;
            densities[i] = countInBounds(opponentPieces, new Point(x_left, upperBound), new Point(x_right, lowerBound));
        }
        return densities;
    }

    // get goal point for runner based on opponent density array.
    // might want to modify depending on other factors that come up
    private Point locateGoal(int[] opponentDensity, boolean opponent_isplayer1) {
        int num_bins = opponentDensity.length;
        int min_idx = findMinIdx(opponentDensity);
        double bin_width = 40.0 / num_bins;
        double y = -10 + bin_width * min_idx;
        double x = opponent_isplayer1 ? 21 : -21;
        return new Point(x,y);
    }

    // get idx of maximum number in array.  What's an inbuilt function for this?
    private int findMinIdx(int[] array) {
        int min_num = 1000;  // or positive infinity
        int min_idx = 0;
        for (int i = 0; i<array.length; i++) {
            if (array[i] < min_num) {
                min_num = array[i];
                min_idx = i;
            }
        }
        return min_idx;
    }

    // Below are Ethan's functions

    private List<Pair<Integer, Point>> getMovesForBlockade(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
        if (this.n < 11) return new ArrayList<>();
        List<Pair<Integer, Point>> moves = new ArrayList<>();

        try {
            for (Pair<Integer, Point> block: this.blockadeList) {
                Integer block_id = block.getKey();
                Point block_point = block.getValue();
                Integer piece_id = this.blockadeMap.get(block_id);
                Point piece_point = player_pieces.get(piece_id);
                if (!isBlockComplete(block_id, block_point, piece_id, piece_point)) {
                    List<Pair<Integer, Point>> ms = moveCurrentToTarget(piece_id, piece_point, block_point, player_pieces, opponent_pieces);
                    moves.addAll(ms);
                    if (moves.size() >= 2) break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return moves;
    }

    private void initializeBlockadeList(boolean isplayer1) {
        for (int i = 0; i < BLOCKADE_YCOORD.size(); i++) {
            Point blockade = new Point(isplayer1 ? BLOCKADE_LINE-EPSILON : -BLOCKADE_LINE+EPSILON, BLOCKADE_YCOORD.get(i));
            this.blockadeList.add(new Pair(i, blockade));
        }
    }

    private void computeBlockadeMapMWBM(HashMap<Integer, Point> player_pieces) {
        Integer offset = 1000; // this is a hack to make MWBM realize blockade points and piece points are distinct
        List<Pair<Integer, Point>> player_pieces_list = player_pieces.entrySet().stream().map(e -> new Pair<>(e.getKey(), e.getValue())).collect(Collectors.toList());
        Collections.sort(player_pieces_list, (p1, p2) -> new Double(p1.getValue().x).compareTo(new Double(p2.getValue().x)));
        if (!this.isplayer1) Collections.reverse(player_pieces_list);
        Map<String, Map<String, Double>> mwbmMap = new HashMap<>();
        for (int i = offset; i < offset + this.blockadeList.size(); i++) {
            Map<String, Double> map = new HashMap<>();
            Pair<Integer, Point> blockade = this.blockadeList.get(i-offset);
            for (int j = 1; j < this.blockadeList.size() + 1; j++) {
                Pair<Integer, Point> piece = player_pieces_list.get(j);
                Double distance = getDistance(blockade.getValue(), piece.getValue());
                map.put("" + piece.getKey(), distance);
            }
            mwbmMap.put("" + i, map);
        }
        MWBM mwbm = new MWBM();
        try {
            MWBM.Result result = mwbm.apply(mwbmMap);
            Map<String, String> assignment = result.assignment;
            this.blockadeMap = new HashMap<>();
            for (Map.Entry<String, String> entry: assignment.entrySet()) {
                this.blockadeMap.put(Integer.parseInt(entry.getKey()) - offset, Integer.parseInt(entry.getValue()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Construct the blockade points and return a priority list of the blockade points we want to fill
    private void computeBlockadeImportance(Map<Integer, Point> opponent_pieces, boolean isplayer1) {
        int opponent_piece_id = getFurthestForward(opponent_pieces, new HashSet<>(), !isplayer1);
        Point point = opponent_pieces.get(opponent_piece_id);
        Collections.sort(this.blockadeList, new Comparator<Pair<Integer, Point>>() {
            @Override
            public int compare(Pair<Integer, Point> p1, Pair<Integer, Point> p2) {
                double p1d = getDistance(p1.getValue(), point);
                double p2d = getDistance(p2.getValue(), point);
                return Double.compare(p1d, p2d);
            }
        });
        // Want to also prioritize creating the blockade with the furthest foward pieces

    }

    // Given a current point and a target point, move the current point towards the target point
    // If this cannot be done in a single move, move the current point directly towards the target
    // The calling method will have to store the second move if it can only make one more move

    public List<Pair<Integer, Point>> moveCurrentToTarget(Integer id, Point current, Point target, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces) {
        List<Pair<Integer, Point>> moves = new ArrayList<>();
        double tmcx = target.x-current.x;
        double tmcy = target.y-current.y;
        double d = Math.sqrt(tmcx*tmcx + tmcy*tmcy);
        double theta = Math.atan(tmcy/tmcx);
        Pair<Integer, Point> m_valid = getSingleValidMove(id, player_pieces, opponent_pieces, theta, this.isplayer1);
        Pair<Integer, Point> m = getSingleMove(id, player_pieces, theta, this.isplayer1);
        Pair<Integer, Point> m_pref = getAroundWithPreferredAngle(id, m, player_pieces, opponent_pieces, this.isplayer1, theta);
        Pair<Integer, Point> m_force = forceMove(id, player_pieces, opponent_pieces, theta, this.isplayer1, 1);
        Point old_point = player_pieces.get(id);

        if (d < EPSILON) {
            ; // do nothing
        } else if (d >= diameter_piece - EPSILON && d < diameter_piece + EPSILON) {
            if (m_valid != null) {
                System.out.println("Block Success");
                moves.add(m_valid);
            }
        } else if (d > EPSILON && d <= 2*diameter_piece) {
            moves.addAll(moveCurrentToTargetCloseTwice(new Pair<Integer, Point>(id, current), target, player_pieces, opponent_pieces));
        } else if (d > 2*diameter_piece && d <= 3*diameter_piece) {
            if (m_valid != null) {
                moves.add(m_valid);
                // player_pieces.put(id, m_valid.getValue());
                // moves.addAll(moveCurrentToTargetCloseTwice(m_valid, target, player_pieces, opponent_pieces));
                // player_pieces.put(id, old_point);
            } else if (m_pref != null) {
                moves.add(m_pref);
            } else {
                moves.add(m_force);
            }
        } else if (d > 3*diameter_piece) {
            if (m_valid != null) {    
                moves.add(m_valid);
                player_pieces.put(id, m_valid.getValue());
                Pair<Integer, Point> m2 = getSingleValidMove(id, player_pieces, opponent_pieces, theta, this.isplayer1);
                if (m2 != null) {    
                    moves.add(m2);
                }
                player_pieces.put(id, old_point);
            } else if (m_pref != null) {
                moves.add(m_pref);
            } else {
                moves.add(m_force);
            }
        }
        return moves;
    }

    public Pair<Integer, Point> moveCurrentToTargetClose(Pair<Integer, Point> current, Point target, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces) {
        Integer current_id = current.getKey();
        Point current_point = current.getValue();
        double d = getDistance(current_point, target);
        System.out.println("Distance: " + d);
        // don't bother calculating if you were properly moved into position
        if (d > diameter_piece - EPSILON && d < diameter_piece + EPSILON) {
            return new Pair<Integer, Point>(current_id, target);
        }
        double tmcx = target.x-current_point.x;
        double tmcy = target.y-current_point.y;
        double tmcx2 = tmcx/2;
        double tmcy2 = tmcy/2;
        double tpp2 = Math.atan(tmcy/tmcx);
        double tmp2 = Math.acos(Math.sqrt(tmcx2*tmcx2 + tmcy2*tmcy2)/2);
        double theta = tpp2 + tmp2;
        double phi = tpp2 - tmp2;
        // if you are blocked, take the other angle first
        Pair<Integer, Point> move = getSingleValidMove(current_id, player_pieces, opponent_pieces, theta, this.isplayer1);
        if (move != null) {
            return move;
        } else {
            move = getSingleValidMove(current_id, player_pieces, opponent_pieces, phi, this.isplayer1);
            if (move != null) {
                return move;
            } else {
                Pair<Integer, Point> best_approx_move = getSingleMove(current_id, player_pieces, tpp2, this.isplayer1);
                Pair<Integer, Point> best_valid_approx_move = getSingleValidMove(current_id, player_pieces, opponent_pieces, tpp2, this.isplayer1);
                if (best_valid_approx_move != null) {
                    return best_valid_approx_move;
                } else {
                    Pair<Integer, Point> approx_move = getAroundWithPreferredAngle(current_id, best_approx_move, player_pieces, opponent_pieces, this.isplayer1, tpp2);
                    if (approx_move != null) {
                        return approx_move;
                    } else {
                        System.out.println("Forcing Move");
                        return forceMove(current_id, player_pieces, opponent_pieces, tpp2, this.isplayer1, 1);
                    }
                }
            }
        }
    }

    private Pair<Integer, Point> forceMove(int piece_id, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, double preference_theta, boolean isplayer1, double increment) {
        for (int delta=0; delta<=180; delta+=increment) {
            Pair<Integer, Point> proposal_move = getSingleMove(piece_id, player_pieces, preference_theta + delta, isplayer1);
            if (check_validity(proposal_move, player_pieces, opponent_pieces)) {
                return proposal_move;
            }
            else {
                proposal_move = getSingleMove(piece_id, player_pieces, preference_theta - delta, isplayer1);
                if (check_validity(proposal_move, player_pieces, opponent_pieces)) {
                    return proposal_move;
               }
            }
        }
        return null;
    }

    public List<Pair<Integer, Point>> moveCurrentToTargetCloseTwice(Pair<Integer, Point> current, Point target, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces) {
        List<Pair<Integer, Point>> moves = new ArrayList<>();
        Point old_point = current.getValue();
        Pair<Integer, Point> m1 = moveCurrentToTargetClose(current, target, player_pieces, opponent_pieces);
        if (m1 == null) {
            System.out.println("why was this move rejected?1");
            return moves;
        }
        moves.add(m1);
        player_pieces.put(current.getKey(), m1.getValue());
        Pair<Integer, Point> m2 = moveCurrentToTargetClose(m1, target, player_pieces, opponent_pieces);
        if (m2 == null) {
            System.out.println("why was this move rejected?2");
            return moves;
        }
        moves.add(m2);
        player_pieces.put(current.getKey(), old_point);
        return moves;
    }

    private boolean isBlockadeComplete(HashMap<Integer, Point> player_pieces) {
        for (Pair<Integer, Point> block : this.blockadeList) {
            Integer block_id = block.getKey();
            Point block_point = block.getValue();
            Integer piece_id = this.blockadeMap.get(block_id);
            Point piece_point = player_pieces.get(piece_id);
            if (!isBlockComplete(block_id, block_point, piece_id, piece_point)) return false;
        }
        System.out.println("Blockade Complete in moves: " + this.turn_counter);
        return true;
    }

    private boolean isBlockComplete(Integer block_id, Point block_point, Integer piece_id, Point piece_point) {
        return getDistance(block_point, piece_point) < EPSILON;
    }

    public Point getNewPointFromOldPointAndAngle(Point current, double theta) {
        Point new_position = new Point(current);
        double delta_x = diameter_piece * Math.cos(theta);
        double delta_y = diameter_piece * Math.sin(theta);
        new_position.x += this.isplayer1 ? -delta_x : delta_x;
        new_position.y += this.isplayer1 ? -delta_y : delta_y;
        return new_position;
    }

    private double getDistance(Point p1, Point p2) {
        return getDistance(p1, p2, 1);
    }

    private double getDistance(Point p1, Point p2, int y_scale) {
        double x_diff = p2.x-p1.x;
        double y_diff = (p2.y-p1.y)/y_scale;
        return Math.sqrt(x_diff*x_diff + y_diff*y_diff);
    }

    private HashMap<Integer, Point> deepCopy(HashMap<Integer, Point> map) {
        HashMap<Integer, Point> copy = new HashMap<>();
        for (Map.Entry<Integer, Point> entry: map.entrySet()) {
            copy.put(new Integer(entry.getKey()), new Point(entry.getValue()));
        }
        return copy;
    }

    // Utility Methods

    // Given your piece and all other pieces, return the farthest forward point (can get stuck locally)
    public Point getBestNonConflictingLocalMove(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, Integer piece) {
        double theta = -Math.PI/2;
        double delta_x = diameter_piece * Math.cos(theta);
        double delta_y = diameter_piece * Math.sin(theta);
        return new Point(0.0, 0.0);
    }

    // Given your piece and all other pieces, return the next move on the shortest path leading to the end (should not get stuck locally)
    public Point getBestNonConflictingGlobalMove(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, Integer piece) {
        return new Point(0.0, 0.0);
    }

    //this function takes in the runner piece current position, obstacle position, and calculate theta
    //if usualway is true, we are minimizing the off-course angle. i.e., if opponent on left, flip to right and vice versa
    //if usualway is false, then flip to the other side
        public double getAroundTheta(Integer runner_id, Point curr_position, Point obstacle, boolean usualway, boolean isplayer1, double preferred_angle){
        double m1 = Math.abs(curr_position.y - obstacle.y);
        double m2 = Math.abs(curr_position.x - obstacle.x);
        double m3 = Math.sqrt(m1*m1 + m2*m2);
        double cos_a2 = (m3*m3 + m2*m2 - m1*m1) / (2 * m2 * m3);
        double cos_a1 = m3*m3 / (diameter_piece * diameter_piece * m3);
        //a1 should be greater than a2
        double theta1;
        //opponent on right
        if (isplayer1 ? obstacle.y < curr_position.y : obstacle.y > curr_position.y) {
            //move to the left theta1 negative
            theta1 = Math.acos(cos_a2) - Math.acos(cos_a1);
            if (!usualway){
                //move to the right theta1 positive
                theta1 = Math.acos(cos_a2) + Math.acos(cos_a1);
            }
            //opponent on left
        } else {
            theta1 = Math.acos(cos_a1) - Math.acos(cos_a2);
            if (!usualway){
                theta1 = -Math.acos(cos_a2) - Math.acos(cos_a1);
            }
        }

        return theta1 + preferred_angle;

    }


    // Jingya's FUNCTIONS

    //This is our version of check collision, it does basically the same thing as check_collision
    //It detects collisions and put them in a list
    public List<Pair<Integer,Point>> ourCheckCollision(HashMap<Integer, Point> m, Pair<Integer, Point> move){
        List<Pair<Integer, Point>> nearby_oppo = new ArrayList<Pair<Integer, Point>>();
        for (HashMap.Entry<Integer, Point> entry : m.entrySet()) {
            if (Board.getdist(move.getValue(), entry.getValue()) + 1E-7 < diameter_piece) {

                // Double dist = getdist(move.getValue(), entry.getValue()) + eps;
                //System.out.println("First time collision detected between pieces " + invalid_move.getKey().toString() + " and " + entry.getKey().toString() + "distance was " + dist.toString());
                Pair<Integer, Point> collide = new Pair<Integer, Point>(entry.getKey(), entry.getValue());
                nearby_oppo.add(collide);

            }
        }
        return nearby_oppo;
    }

    public Pair<Integer, Point> getAroundWithPreferredAngle(Integer runner_id, Pair<Integer, Point> invalid_move, HashMap<Integer, Point> player_pieces,
                                                            HashMap<Integer, Point> opponent_pieces, boolean isplayer1, double preferred_angle){
        int i = 0;
        Point curr_position = player_pieces.get(runner_id);
        Point new_position = new Point(curr_position);
        //can't move straight we do something when there is only one obstacle ahead
        //check for collision, add opponent pieces into nearyby_oppo
        List<Pair<Integer, Point>> nearby_oppo = ourCheckCollision(opponent_pieces,invalid_move);
        List<Pair<Integer, Point>> nearby_self = ourCheckCollision(player_pieces, invalid_move);
        //concatenate two lists
        nearby_oppo.addAll(nearby_self);

        if (nearby_oppo.size() == 0) {return getSingleMove(runner_id, player_pieces, preferred_angle, isplayer1);}  // just move freely

        // else we need to find tangent move
        Point obstacle = new Point(nearby_oppo.get(0).getValue());

        //figure out the nearest oppo
        for (i = 0; i < nearby_oppo.size(); i++) {
            double min_distance = 100000;
            double distance = Math.sqrt(Math.pow(curr_position.x - nearby_oppo.get(i).getValue().x, 2)
                    + Math.pow(curr_position.y - nearby_oppo.get(i).getValue().y, 2));
            if (distance < min_distance) {
                min_distance = distance;
                obstacle = nearby_oppo.get(i).getValue();
            }
        }
        //get the nearest point in nearby_oppo and flip to avoid that
        //TODO: sometimes invalid move passed in is NaN, needs to figure out way
        double theta1 = getAroundTheta(runner_id, curr_position, obstacle, true, isplayer1, preferred_angle);
        Pair<Integer, Point> move2 = getSingleValidMove(runner_id, player_pieces, opponent_pieces, theta1, isplayer1);
        if (move2 != null) {return move2;}
        else {
            theta1 = getAroundTheta(runner_id, curr_position, obstacle, false, isplayer1, preferred_angle);
            Pair<Integer, Point> move3 = getSingleValidMove(runner_id, player_pieces, opponent_pieces, theta1, isplayer1);
            if (move3 != null) {
                return move3;
            } else {
                return getNullMove(runner_id);
            }
        }//want to return null move here. I don't know to return null directly.
        //version 2: we can give a final shot by go to a randomnized angle, but I don't use it for now
    }

    Pair<Integer, Point> getNullMove(int piece_id) {
        Point null_pos = new Point(-10000, -10000);
        Pair<Integer, Point> move_null = new Pair<Integer, Point>(piece_id, null_pos);
        return move_null;
    }

    // PROGRAMED BY TA
    public boolean check_validity(Pair<Integer, Point> move, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces)
    {
        boolean valid = true;

        // check if move is adjacent to previous position.
        if(!Board.almostEqual(Board.getdist(player_pieces.get(move.getKey()), move.getValue()), diameter_piece))
        {
            return false;
        }
        // check for collisions
        valid = valid && !Board.check_collision(player_pieces, move);
        valid = valid && !Board.check_collision(opponent_pieces, move);

        // check within bounds
        valid = valid && Board.check_within_bounds(move);
        return valid;
    }
}