package flip.g1;

import java.util.*;
import java.util.List;
import javafx.util.Pair;

import flip.sim.Point;
import flip.sim.Board;
import flip.sim.Log;

import flip.g1.Hungarian;


public class Player implements flip.sim.Player {
    private int seed = 99;
    private Random random;
    private boolean isplayer1;
    private Integer n;
    private Double diameter_piece;
    private Integer threshold = 21;
    private Integer boundary = 20;
    private Integer height_to_count_players = 2;
    private Double distance = 3.73; // ~= 2 + sqrt(3)
    private Double density_cone_height = 4.0;
    private Double density_lane_gap = 1.0;
    private Integer sign;
    private Integer wall_back = 20 - 2;
    private Integer wall_front = 20 + 4;
    private Integer threshold_to_attack_wall = 3;
    private Double same_x_diff = 1.0;
    private Double same_y_diff = 3.5;
    private Integer dis_to_move_head = 6;
    private double eps = 1E-7;
    private double height_caused_to_dodge = 2;

    // Wall stuff
    private int[] wallMatching;
    private double wallX;
    double[] wallPointCenters11 = {
            -17.268, -13.804, -10.34, -6.876, -3.412, 0.052, 3.516, 6.98, 10.444, 13.908, 17.372
    };
    boolean[] coinsSet = new boolean[11];

    private HashMap<Integer, Point> pieces;


    private enum State {
        INITIAL_STATE,
        WALL_BUILDING,
        WALL_BUILT,
        WALL_FLIP,
        WALL_FLIP_PREPARE,
        FORWARD,
        FINDING_GAP,
        IN_GAP,
        CHICKEN_GAME,
        STUCK,
        BFS,
        RANDOM
    };

    private State currentState = State.INITIAL_STATE;

    public Player() {
        random = new Random(seed);
    }

    // Initialization function.
    // pieces: Location of the pieces for the player.
    // n: Number of pieces available.
    // t: Total turns available.
    public void init(HashMap<Integer, Point> pieces, int n, double t, boolean isplayer1, double diameter_piece) {
        this.n = n;
        this.isplayer1 = isplayer1;
        this.sign = isplayer1 ? -1 : 1;
        this.diameter_piece = diameter_piece;
        this.pieces = pieces;
        this.wallX = -sign * 20.0;
        if(n >= 11) {
            this.calculateMatching();
        }
    }

    public void calculateMatching() {
        if(n < 11) {
            throw new Error("Can't build a wall with less than 11 pieces");
        }
        double[][] costMatrix = new double[11][n];
        for (int i = 0; i < 11; i++) {
            for (int j = 0; j < n; j++) {
                Point coin = pieces.get(j);
                Point wallPoint = new Point(wallX, wallPointCenters11[i]);
                costMatrix[i][j] = getDistance(coin, wallPoint);
            }
        }
        Hungarian hg = new Hungarian(costMatrix);
        this.wallMatching = hg.execute();
    }

    public List<Pair<Integer, Point>> getMoves(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
        updateState(num_moves, player_pieces, opponent_pieces);

        List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();

        while (moves.size() < num_moves) {
            switch (currentState) {
                case WALL_BUILDING: {
                    moves = getWallMoves(moves, num_moves, player_pieces, opponent_pieces);
                    break;
                }
                case FINDING_GAP: {
                    moves = getGapFindMoves(moves, num_moves, player_pieces, opponent_pieces);
                    break;
                }
                case FORWARD: {
                    moves = getDensityMoves(moves, num_moves, player_pieces, opponent_pieces);
                    break;
                }
                case WALL_FLIP_PREPARE: {
                    moves = getWallFlipPrepareMoves(moves, num_moves, player_pieces, opponent_pieces);
                    break;
                }
                case CHICKEN_GAME: {
                    moves = getChichenGameMoves(moves, num_moves, player_pieces, opponent_pieces);
                    break;
                }
                case BFS: {
                    moves = getBFSMoves(moves, num_moves, player_pieces, opponent_pieces);
                    break;
                }
                default:
                case RANDOM: {
                    moves = getRandomMoves(moves, num_moves, player_pieces, opponent_pieces);
                    break;
                }
            }
            if (moves.size() < num_moves) {
                moves = getRandomMoves(moves, num_moves, player_pieces, opponent_pieces);
            }
        }
        return moves;
    }

    public void updateState(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces) {
        switch (currentState) {
            case INITIAL_STATE: {
                if (n == 1) {
                    currentState = State.CHICKEN_GAME;
                }
                else if (n >= 2 && n <= 5) {
                    currentState = State.BFS;
                }
                else if (checkIfWallStrategyShouldBeUsed()) {
                    currentState = State.WALL_BUILDING;
                } else if(checkIfGapStrategyShouldBeUsed()) {
                    currentState = State.FINDING_GAP;
                } else {
                    currentState = State.FORWARD;
                }
                break;
            }
            case WALL_BUILT: {
                if (checkIfWallFlipCanBeDone()) {
                    currentState = State.WALL_FLIP;
                } else if (checkIfWallFlipShouldBePrepared(player_pieces)) {
                    currentState = State.WALL_FLIP_PREPARE;
                } else {
                    currentState = State.FORWARD;
                }
                break;
            }
        }
    }

    public boolean checkIfWallStrategyShouldBeUsed() {
        return n >= 12;
    }

    public boolean checkIfWallFlipCanBeDone() {
        return false;
    }

    public boolean checkIfGapStrategyShouldBeUsed() {
        return true;
    }

    public List<Point> getGapStrategyShouldBeUsed(HashMap<Integer, Point> opponent_pieces) {
        //Add all the points at the valid range of formming wall
        List<Point> wallList = new ArrayList<>();
        for (Point point : opponent_pieces.values()){
            if ((!isplayer1 && point.x > wall_back && point.x < wall_front) || (isplayer1 && point.x < -wall_back && point.x > -wall_front))
                wallList.add(point);
        }
        return wallList;
    }

    public boolean checkIfWallFlipShouldBePrepared(HashMap<Integer, Point> player_pieces) {
        for (Point coin : player_pieces.values()) {
            if (Math.abs(coin.x) > 20) {
                return true;
            }
        }
        return false;
    }

    public List<Pair<Integer, Point>> getGapFindMoves(
            List<Pair<Integer, Point>> moves,
            Integer num_moves,
            HashMap<Integer, Point> player_pieces,
            HashMap<Integer, Point> opponent_pieces
    ) {
        List<Point> list = getGapStrategyShouldBeUsed(opponent_pieces);
        Collections.sort(list, (a, b) -> Double.compare(b.y, a.y));
        // Add the bottom line
        list.add(new Point(list.get(list.size() - 1).x, -20));
        double maxGap = -1;
        double top = -1;
        double down = -1;
        Point pre = new Point(list.get(0).x, 20);
        double x = -1;
        for (int i = 0; i < list.size(); i++) {
            double gap = pre.y - list.get(i).y;
            if (gap > maxGap) {
                top = pre.y;
                down = list.get(i).y;
                maxGap = gap;
                x = isplayer1 ? Math.min(pre.x, list.get(i).x) : Math.max(pre.x, list.get(i).x);
            }
            pre = list.get(i);
        }
        Point gapPoint = new Point(x, (top + down) / 2);
        // For now, just move the quickest one that falls into the maximum gap.
        int minId = -1;
        double minDis = Integer.MAX_VALUE;
        for (Map.Entry<Integer, Point> entry : player_pieces.entrySet()) {
            Point curr_position = entry.getValue();
            Pair<Integer, Point> move = new Pair<Integer, Point>(entry.getKey(), new Point(curr_position.x + sign * 2, curr_position.y));
            if (shouldStop(player_pieces, opponent_pieces, entry.getKey()) || !check_validity(move, player_pieces, opponent_pieces)) continue;
            Point p = entry.getValue();
            double dist = getDistance(p, gapPoint);
            if (dist < minDis) {
                minDis = dist;
                minId = entry.getKey();
            }
        }
        Log.log("Gap Point: " + gapPoint.x +","+gapPoint.y);
        return findMovesToPoint(moves, minId, player_pieces.get(minId), gapPoint, num_moves, player_pieces, opponent_pieces);
    }

    public List<Pair<Integer, Point>> getWallMoves(
            List<Pair<Integer, Point>> moves,
            Integer num_moves,
            HashMap<Integer, Point> player_pieces,
            HashMap<Integer, Point> opponent_pieces
    ) {
        Integer[] coinPriority = getWallCoinPriorityByDistance(opponent_pieces);
        for (int i = 0; i < coinPriority.length; i++) {
            int coinIndex = coinPriority[i];
            if (coinsSet[coinIndex]) {
                continue;
            }
            int coin = wallMatching[coinIndex];
            double y = wallPointCenters11[coinIndex];
            Point wallPiece = new Point(wallX, y);
            Point myPiece = player_pieces.get(coin);
            moves = findMovesToPoint(
                    moves, coin, myPiece, wallPiece, num_moves, player_pieces, opponent_pieces
            );
            if (moves.size() >= num_moves) {
                break;
            }
        }
        if(moves.size() == 0) {
            currentState = State.WALL_BUILT;
        }
        return moves;
    }

    private Integer[] getWallCoinPriority(HashMap<Integer, Point> pieces) {
        double densityDelta = 3.0;
        Integer[] laneCounts = new Integer[11];
        for (int i = 0; i < 11; i++) {
            laneCounts[i] = 0;
        }
        for (int i = 0; i < 11; i++) {
            double wallY = wallPointCenters11[i];
            for(Point oponentCoin: pieces.values()) {
                if((oponentCoin.y > (wallY - densityDelta)) && (oponentCoin.y < (wallY + densityDelta))) {
                    laneCounts[i]++;
                }
            }
        }
        ArrayIndexComparator comparator = new ArrayIndexComparator(laneCounts);
        Integer[] indices = comparator.createIndexArray();
        Arrays.sort(indices, comparator);
        for(int i = 0; i < indices.length / 2; i++) {
            int temp = indices[i];
            indices[i] = indices[indices.length - i - 1];
            indices[indices.length - i - 1] = temp;
        }
        return indices;
    }

    private Integer[] getWallCoinPriorityByDistance(HashMap<Integer, Point> pieces) {
        double densityDelta = 3.0;
        Double[] laneCounts = new Double[11];
        for (int i = 0; i < 11; i++) {
            laneCounts[i] = 200.0;
        }
        for (int i = 0; i < 11; i++) {
            double wallY = wallPointCenters11[i];
            for(Point oponentCoin: pieces.values()) {
                double distance = getDistance(oponentCoin, new Point(wallX, wallY));
                if(distance < laneCounts[i]) {
                    laneCounts[i] = distance;
                }
            }
        }
        ArrayIndexDoubleComparator comparator = new ArrayIndexDoubleComparator(laneCounts);
        Integer[] indices = comparator.createIndexArray();
        Arrays.sort(indices, comparator);
//        for(int i = 0; i < indices.length / 2; i++) {
//            int temp = indices[i];
//            indices[i] = indices[indices.length - i - 1];
//            indices[indices.length - i - 1] = temp;
//        }
        return indices;
    }

    public List<Pair<Integer, Point>>  getBFSMoves(
            List<Pair<Integer, Point>> moves,
            Integer num_moves,
            HashMap<Integer, Point> player_pieces,
            HashMap<Integer, Point> opponent_pieces
    ) {
        //Find the opponent that cross the their boundary most far, move our opposite coin first.
        double low1 = Integer.MAX_VALUE, low2 = Integer.MAX_VALUE;
        int low1Id = -1, low2Id= -1;
        for (int i = 0; i < n; i++) {
            Point curr_position = player_pieces.get(i);
            Pair<Integer, Point> move1 = new Pair<Integer, Point>(i, new Point(curr_position.x + sign * 2, curr_position.y));
            if (shouldStop(player_pieces, opponent_pieces, i) || !check_validity(move1, player_pieces, opponent_pieces)) continue;
            // Measure the danger of distance to attach our boundary
            double danger = Integer.MAX_VALUE;
            for (Point point : opponent_pieces.values()) {
                double curDanger = danger;
                if (curr_position.y - height_caused_to_dodge < point.y && curr_position.y + height_caused_to_dodge > point.y) {
                    double disToOurBoundary = isplayer1 ? (20 - point.x) : (point.x + 20);
                    double disOutOfOurBoundary = isplayer1 ? (20 - curr_position.x) : (curr_position.x + 20);
                    curDanger = Math.min(disToOurBoundary + disOutOfOurBoundary, curDanger);
                }
                danger = Math.min(danger, curDanger);
            }
            danger = danger == Integer.MAX_VALUE ? Integer.MAX_VALUE - 1 : danger;
            if (low1 > danger) {
                low2 = low1;
                low2Id = low1Id;
                low1 = danger;
                low1Id = i;
            } else if (low2 > danger) {
                low2 = danger;
                low2Id = i;
            }
        }
        if (low1Id != -1) {
            Point move1 = new Point(player_pieces.get(low1Id).x + sign * 2, player_pieces.get(low1Id).y);
            moves.add(new Pair<Integer, Point>(low1Id, move1));
            player_pieces.put(low1Id, move1);
            Pair<Integer, Point> nextMove = new Pair<Integer, Point>(low1Id, new Point(move1.x + sign * 2, move1.y));
            if (check_validity(nextMove, player_pieces, opponent_pieces) && (low2Id == -1 || low1 + 2 < low2)
                && !shouldStop(player_pieces, opponent_pieces, low1Id))
                moves.add(nextMove);
            else if (low2Id != -1)
                moves.add(new Pair<Integer, Point>(low2Id, new Point(player_pieces.get(low2Id).x + sign * 2, player_pieces.get(low2Id).y)));
        }
        if (moves.size() == num_moves) return moves;
        //Find the coin with more distance to cross the other one, put it as higher priority to cross.
        double high1 = -1, high2 = -1;
        int high1Id = -1, high2Id= -1;
        for (int i = 0; i < n; i++) {
            Point curr_position = player_pieces.get(i);
            if (shouldStop(player_pieces, opponent_pieces, i)) continue;
            double dis = -1;
            for (Point point : opponent_pieces.values()) {
                if (curr_position.y - height_caused_to_dodge < point.y && curr_position.y + height_caused_to_dodge > point.y)
                    dis = Math.min(dis, Math.abs(getDistance(curr_position, point)));
            }
            if (high1 < dis) {
                high2 = high1;
                high2Id = high1Id;
                high1 = dis;
                high1Id = i;
            } else if (high2 < dis) {
                high2 = dis;
                high2Id = i;
            }
        }
        if (high1Id != -1) {
            Pair<Integer, Point> optimalMove = findNextPathToGetOverBlock(high1Id, opponent_pieces, player_pieces, new Point(sign*60, player_pieces.get(high1Id).y));
            if (optimalMove != null)
                moves.add(optimalMove);
            if (moves.size() == num_moves && high2Id != -1) {
                Pair<Integer, Point> optimalMove2 = findNextPathToGetOverBlock(high2Id, opponent_pieces, player_pieces, new Point(sign*60, player_pieces.get(high2Id).y));
                if (optimalMove2 != null)
                    moves.add(optimalMove2);
            }
        }
        return moves;
    }

    public List<Pair<Integer, Point>> getChichenGameMoves(
            List<Pair<Integer, Point>> moves,
            Integer num_moves,
            HashMap<Integer, Point> player_pieces,
            HashMap<Integer, Point> opponent_pieces
    ) {
        for (int i = 0; i < n; i++) {
            if (shouldStop(player_pieces, opponent_pieces, i)) continue;
            Point curr_position = player_pieces.get(i);
            Pair<Integer, Point> move1 = new Pair<Integer, Point>(i, new Point(curr_position.x + sign * 2, curr_position.y));
            if (check_validity(move1, player_pieces, opponent_pieces)) {
                moves.add(move1);
                player_pieces.put(i, move1.getValue());
                Pair<Integer, Point> move2 = new Pair<Integer, Point>(i, new Point(curr_position.x + sign * 4, curr_position.y));
                if (!check_validity(move2, player_pieces, opponent_pieces)) {
                    Point opponent = getBlockingOpponent(move2.getValue(), opponent_pieces);
                    Log.log("After one move, Dist c = " + (Math.abs(opponent.x - curr_position.x - sign * 2) - 2) + " I am player" +
                            (isplayer1 ? "1" : "2"));
                    Pair<Integer, Point> move3 = findPointTangentToTwoCoins(i, move1.getValue(), opponent, player_pieces, opponent_pieces, new Point(sign*60, move1.getValue().y));
                    if (move3 != null)
                        moves.add(move3);
                }
                else
                    moves.add(move2);
            }
            else {
                Point opponent = getBlockingOpponent(move1.getValue(), opponent_pieces);
                Log.log("First move to dodge, Dist c = " + (Math.abs(opponent.x - curr_position.x) - 2)+
                        " I am player" +
                        (isplayer1 ? "1" : "2"));
                Pair<Integer, Point> move2 = findPointTangentToTwoCoins(i, curr_position, opponent, player_pieces, opponent_pieces, new Point(sign*60, curr_position.y));
                if (move2 != null) {
                    moves.add(move2);
                    Point point2 = move2.getValue();
                    player_pieces.put(i, point2);
                    Pair<Integer, Point> move3 = new Pair<Integer, Point>(i, new Point(point2.x + sign * 2, point2.y));
                    if (!check_validity(move3, player_pieces, opponent_pieces)) {
                        Pair<Integer, Point> move4 = findPointTangentToTwoCoins(i, point2, getBlockingOpponent(move3.getValue(), opponent_pieces),
                                player_pieces, opponent_pieces, new Point(sign*60, point2.y));
                        if (move4 != null)
                            moves.add(move4);
                        else
                            moves.add(move3);
                    }
                }
            }
        }
        return moves;
    }

    private Pair<Integer, Double> findClosesPoint(
            HashMap<Integer, Point> player_pieces, double x, double y, List<Integer> usedCoins
    ) {
        double minDistance = 120;
        Integer minCoinIndex = -1;
        for (int i = 0; i < n; i++) {
            Point point = player_pieces.get(i);
            if (usedCoins.contains(i)) {
                continue;
            }
            distance = Math.sqrt(Math.pow(x - point.x, 2) + Math.pow(y - point.y, 2));
            if (distance < minDistance) {
                minDistance = distance;
                minCoinIndex = i;
            }
        }
        return new Pair(minCoinIndex, minDistance);
    }

    public Point getPointInDirection(Point start, Point goal) {
        double distance = getDistance(start, goal);
        return new Point(
                start.x + 2 * (goal.x - start.x) / distance,
                start.y + 2 * (goal.y - start.y) / distance
        );
    }

    // Inspired by group 4
    public List<Pair<Integer, Point>> findMovesToPoint(
            List<Pair<Integer, Point>> moves,
            Integer coin,
            Point start,
            Point goal,
            Integer numMoves,
            HashMap<Integer, Point> playerPieces,
            HashMap<Integer, Point> opponentPieces
    ) {
        double distance = getDistance(start, goal);
        if (Board.almostEqual(distance, 0)) {
            return moves;
        } else if (Board.almostEqual(distance, 2)) {
            moves.add(new Pair<Integer, Point>(coin, goal));
        } else if (distance < diameter_piece * 2) {
            double x1 = 0.5 * (goal.x + start.x);
            double y1 = 0.5 * (goal.y + start.y);

            double sqrt_const = Math.sqrt(16 / (distance * distance) - 1) / 2;
            double x2 = sqrt_const * (goal.y - start.y);
            double y2 = sqrt_const * (start.x - goal.x);

            Pair<Integer, Point> move = new Pair<Integer, Point>(coin, new Point(x1 + x2, y1 + y2));

            if (check_validity(move, playerPieces, opponentPieces)) {
                moves.add(move);
                playerPieces.put(coin, move.getValue());
                Pair<Integer, Point> move2 = new Pair<Integer, Point>(coin, new Point(goal.x, goal.y));
                if (check_validity(move2, playerPieces, opponentPieces))
                    moves.add(move2);
            } else {
                move = findNextPathToGetOverBlock(coin, playerPieces, opponentPieces, goal);
                if (check_validity(move, playerPieces, opponentPieces)) {
                    moves.add(move);
                    playerPieces.put(coin, move.getValue());
                    Pair<Integer, Point> move2 = new Pair<Integer, Point>(coin, new Point(goal.x, goal.y));
                    if (check_validity(move2, playerPieces, opponentPieces))
                        moves.add(move2);
                }
            }
        } else {
            Point start2 = getPointInDirection(start, goal);
            Pair<Integer, Point> move = new Pair<Integer, Point>(coin, start2);

            if (check_validity(move, playerPieces, opponentPieces)) {
                moves.add(move);
                playerPieces.put(coin, move.getValue());
                if (distance > diameter_piece * 3 && moves.size() == 1) {
                    Point start3 = getPointInDirection(start2, goal);
                    Pair<Integer, Point> move4 = new Pair<Integer, Point>(coin, start3);

                    if (check_validity(move4, playerPieces, opponentPieces)) {
                        moves.add(move4);
                    }
                }
            }
            else {
                move = findNextPathToGetOverBlock(coin, playerPieces, opponentPieces, goal);
                moves.add(move);
                playerPieces.put(coin, move.getValue());
                if (distance > diameter_piece * 3 && moves.size() == 1) {
                    Point start3 = getPointInDirection(start2, goal);
                    Pair<Integer, Point> move4 = new Pair<Integer, Point>(coin, start3);

                    if (check_validity(move4, playerPieces, opponentPieces)) {
                        moves.add(move4);
                    }
                    else {
                        move4 = findNextPathToGetOverBlock(coin, playerPieces, opponentPieces, goal);
                        moves.add(move4);
                    }
                }
            }
        }
        return moves;
    }

    public double getDistance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }

    public double getAngle(Point v1, Point v2) {
        return Math.atan2(v1.x * v2.y - v1.y * v2.x, v1.x * v2.x + v1.y * v2.y);
    }

    public double getAbsoluteAngle(Point v) {
        Point u = new Point(0, 1);
        return getAngle(v, u);
    }

    public List<Pair<Integer, Point>> getDensityMoves(
            List<Pair<Integer, Point>> moves,
            Integer num_moves,
            HashMap<Integer, Point> player_pieces,
            HashMap<Integer, Point> opponent_pieces
    ) {
        int low1 = Integer.MAX_VALUE, low2 = Integer.MAX_VALUE, low1Id = -1, low2Id = -1;

        for (int i = 0; i < n; i++) {
            Point curr_position = player_pieces.get(i);
            Pair<Integer, Point> move = new Pair<Integer, Point>(i, new Point(curr_position.x + sign * 2, curr_position.y));
            if (shouldStop(player_pieces, opponent_pieces, i) || !check_validity(move, player_pieces, opponent_pieces)) continue;
            int count = 0;
            for (Point point : opponent_pieces.values()) {
                // Two lines parameters
                double k1 = -sign * distance / density_cone_height;
                double b1 = curr_position.y - k1 * (curr_position.x - sign * density_lane_gap);
                double k2 = sign * distance / density_cone_height;
                double b2 = curr_position.y - k2 * (curr_position.x - sign * density_lane_gap);
                double x = point.x;
                double y = point.y;
                boolean condition1 = y < (curr_position.y + distance);
                boolean condition2 = y > (curr_position.y - distance);
                boolean condition3 = (x * k1 + b1 - y) < 0;
                boolean condition4 = (x * k2 + b2 - y) > 0;
                if (condition1 && condition2 && condition3 && condition4)
                    count++;
            }
            if (low1 > count) {
                low2 = low1;
                low2Id = low1Id;
                low1 = count;
                low1Id = i;
            } else if (low2 > count) {
                low2 = count;
                low2Id = i;
            }
        }
        if (low1Id != -1) {
            Point point1 = player_pieces.get(low1Id);
            Pair<Integer, Point> move1 = new Pair<Integer, Point>(low1Id, new Point(point1.x + sign * 2, point1.y));
            moves.add(move1);
            Pair<Integer, Point> move2 = new Pair<Integer, Point>(low1Id, new Point(point1.x + sign * 4, point1.y));
            //We should check if the current node has got into the stop area, if so, do movement for the other nodes.
            if (check_validity(move2, player_pieces, opponent_pieces)
                    && ((isplayer1 && point1.x + sign * 2 > -threshold) || (!isplayer1 && point1.x + sign * 2 < threshold)))
                moves.add(move2);
            else if (low2Id != -1) {
                Point point2 = player_pieces.get(low2Id);
                Pair<Integer, Point> move3 = new Pair<Integer, Point>(low2Id, new Point(point2.x + sign * 2, point2.y));
                moves.add(move3);
            }
        }
        return moves;
    }

    public List<Pair<Integer, Point>> getWallFlipPrepareMoves(
            List<Pair<Integer, Point>> moves,
            Integer num_moves,
            HashMap<Integer, Point> player_pieces,
            HashMap<Integer, Point> opponent_pieces
    ) {
        return getRandomMoves(moves, num_moves, player_pieces, opponent_pieces);
    }

    public List<Pair<Integer, Point>> getRandomMoves(
            List<Pair<Integer, Point>> moves,
            Integer num_moves,
            HashMap<Integer, Point> player_pieces,
            HashMap<Integer, Point> opponent_pieces
    ) {
        int num_trials = 30;
        int i = 0;

        while (moves.size() != num_moves && i < num_trials) {

            Integer piece_id = random.nextInt(n);

            if(Arrays.stream(wallMatching).anyMatch(x -> x == piece_id))
                continue;

            Point curr_position = player_pieces.get(piece_id);
            if (((isplayer1 && curr_position.x < -threshold)
                    || (!isplayer1 && curr_position.x > threshold))) continue;
            Point new_position = new Point(curr_position);
            double theta = -Math.PI / 2 + Math.PI * random.nextDouble();
            double delta_x = diameter_piece * Math.cos(theta);
            double delta_y = diameter_piece * Math.sin(theta);

            Double val = (Math.pow(delta_x, 2) + Math.pow(delta_y, 2));

            new_position.x = isplayer1 ? new_position.x - delta_x : new_position.x + delta_x;
            new_position.y += delta_y;
            Pair<Integer, Point> move = new Pair<Integer, Point>(piece_id, new_position);

            Double dist = Board.getdist(player_pieces.get(move.getKey()), move.getValue());

            if (check_validity(move, player_pieces, opponent_pieces))
                moves.add(move);
            i++;
        }

        return moves;
    }

    public List<Pair<Integer, Point>> getSmartStackMoves(
            List<Pair<Integer, Point>> moves,
            Integer num_moves,
            HashMap<Integer, Point> player_pieces,
            HashMap<Integer, Point> opponent_pieces
    ) {
        // Attempt brute escape for each node.
        // Potential optimization: prioritize closest stuck nodes or least-density nodes first!
        // Another potential optimization: if it breaks free, prioritize moving the free one over another stuck one.
        for (int id = 0; id < n; id++) {
            if (moves.size() == num_moves) {
                break;
            }
            Point curr_position = player_pieces.get(id);
            if (((isplayer1 && curr_position.x < -threshold)
                    || (!isplayer1 && curr_position.x > threshold))) continue;
            double theta_up = 0;
            double theta_down = 0;
            double delta_x = 0;
            double delta_y_up = 0;
            double delta_y_down = 0;
            Point new_position_up = new Point(curr_position);
            Point new_position_down = new Point(curr_position);
            if (isplayer1) {
                // going left...
                // try up 135 degrees
                theta_up = (3 / 4) * Math.PI;
                // Try down 225 degrees
                theta_down = (5 / 4) * Math.PI;

                delta_x = diameter_piece * Math.cos(theta_up);
                delta_y_up = diameter_piece * Math.sin(theta_up);
                delta_y_down = diameter_piece * Math.sin(theta_down);

                new_position_up.x -= delta_x;
                new_position_down.x -= delta_x;

                new_position_up.y += delta_y_up;
                new_position_down.y += delta_y_down;

            } else {
                // going right.
                // Try up 45 degrees
                theta_up = (1 / 4) * Math.PI;
                // Try down 315 degrees
                theta_down = (7 / 8) * Math.PI;

                delta_x = diameter_piece * Math.cos(theta_up);
                delta_y_up = diameter_piece * Math.sin(theta_up);
                delta_y_down = diameter_piece * Math.sin(theta_down);

                new_position_up.x += delta_x;
                new_position_down.x += delta_x;

                new_position_up.y += delta_y_up;
                new_position_down.y += delta_y_down;


            }
            Pair<Integer, Point> move_up = new Pair<Integer, Point>(id, new_position_up);
            Pair<Integer, Point> move_down = new Pair<Integer, Point>(id, new_position_down);


            if (check_validity(move_up, player_pieces, opponent_pieces))
                moves.add(move_up);
            else if (check_validity(move_down, player_pieces, opponent_pieces))
                moves.add(move_down);
        }
        return moves;
    }

    public boolean check_validity(Pair<Integer, Point> move, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces) {
        boolean valid = true;

        // check if move is adjacent to previous position.
        if (!Board.almostEqual(Board.getdist(player_pieces.get(move.getKey()), move.getValue()), diameter_piece)) {
            return false;
        }
        // check for collisions
        valid = valid && !Board.check_collision(player_pieces, move);
        valid = valid && !Board.check_collision(opponent_pieces, move);

        // check within bounds
        valid = valid && Board.check_within_bounds(move);
        return valid;

    }


    boolean shouldStop(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, int coinId) {
        int count_behind_players = 0;
        Point curr_position = player_pieces.get(coinId);
        //count the number of nodes incoming
        for (Point point : player_pieces.values()) {
            double y = point.y;
            double x = point.x;
            if (y > curr_position.y - height_to_count_players && y < curr_position.y + height_to_count_players
                    && ((isplayer1 && x > curr_position.x && x < curr_position.x + dis_to_move_head)
                    || (!isplayer1 && x < curr_position.x && x > curr_position.x - dis_to_move_head)))
                count_behind_players++;
        }
        //Choose 2.5 not 2 to allow some intersection that stops the coin moving into the stop area.
        return (isplayer1 && curr_position.x < -threshold - count_behind_players * 2.5)
                || (!isplayer1 && curr_position.x > threshold + count_behind_players * 2.5);
    }

    Pair<Integer, Point> findPointTangentToTwoCoins(int coinId, Point cur, Point opponent, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces,
                                                    Point goal) {
        double x1 = cur.x, y1 = cur.y, x2 = opponent.x, y2 = opponent.y;
        double x = 0, y = 0;
        if (y1 == y2) {
            x = (x1 + x2) / 2;
            y = y1 + Math.sqrt(4 - (x - x1)*(x - x1));
            Pair<Integer, Point> move = new Pair<Integer, Point>(coinId, new Point(x, y));
            if (check_validity(move, player_pieces, opponent_pieces))
                return move;
            y = y1 - Math.sqrt(4 - (x - x1)*(x - x1));
            move = new Pair<Integer, Point>(coinId, new Point(x, y));
            if (check_validity(move, player_pieces, opponent_pieces))
                return move;
        }
        else {
            double x_mid = (x1 + x2) / 2;
            double y_mid = (y1 + y2) / 2;
            double k = -(x1 - x2) / (y1 - y2);
            double b = y_mid - k*x_mid;
            double a1 = k*k + 1;
            double b1 = 2*(k*(b - y1) - x1);
            double c1 = (b - y1)*(b - y1) + x1*x1 - 4;
            double root1 = (-b1 + Math.sqrt(Math.pow(b1, 2) - 4*a1*c1)) / (2*a1);
            double root2 = (-b1 - Math.sqrt(Math.pow(b1, 2) - 4*a1*c1)) / (2*a1);
            double root1_y = root1*k + b;
            double root2_y = root2*k + b;
            Point point1 = new Point(root1, root1_y);
            Point point2 = new Point(root2, root2_y);
            Pair<Integer, Point> move1 = new Pair<Integer, Point>(coinId, point1);
            Pair<Integer, Point> move2 = new Pair<Integer, Point>(coinId, point2);
            if (getDistance(point1, goal) < getDistance(point2, goal) && check_validity(move1, player_pieces, opponent_pieces))
                return move1;
            else if (check_validity(move2, player_pieces, opponent_pieces))
                return move2;
        }
        // Any moves are blocked by some other coins.
        return null;
    }

    /**
     *
     * @param cur is the invalid move
     * @param opponent_pieces
     * @return opponent pieces that blocks your move
     */
    Point getBlockingOpponent(Point cur, HashMap<Integer, Point> opponent_pieces) {
        for (HashMap.Entry<Integer, Point> entry : opponent_pieces.entrySet()) {
            if (Board.getdist(cur, entry.getValue()) + eps < 2)
                return entry.getValue();
        }
        return null;
    }

    /**
     *
     * @param cur is the current position
     * @param opponent_pieces
     * @return all the possible coins that blocks your next move
     */
    List<Point> getBlockingOpponents(Point cur, HashMap<Integer, Point> opponent_pieces, HashMap<Integer, Point> player_pieces) {
        List<Point> blockingList = new ArrayList<>();
        for (HashMap.Entry<Integer, Point> entry : opponent_pieces.entrySet()) {
            if (Board.getdist(cur, entry.getValue()) + eps < 4)
                blockingList.add(entry.getValue());
        }
        for (HashMap.Entry<Integer, Point> entry : player_pieces.entrySet()) {
            if (Board.getdist(cur, entry.getValue()) + eps < 4 && Board.getdist(cur, entry.getValue()) != 0 )
                blockingList.add(entry.getValue());
        }
        return blockingList;
    }

    Pair<Integer, Point> findNextPathToGetOverBlock(int coinId, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces,
                                                    Point goal) {
        List<Point> blockingList = getBlockingOpponents(player_pieces.get(coinId), opponent_pieces, player_pieces);
        Pair<Integer, Point> optimalMove = null;
        for (Point opponent : blockingList) {
            Pair<Integer, Point> move = findPointTangentToTwoCoins(coinId, player_pieces.get(coinId), opponent, player_pieces, opponent_pieces, goal);
            if (move != null)
                optimalMove = (optimalMove == null || Board.getdist(goal, move.getValue()) < Board.getdist(goal, optimalMove.getValue())) ? move : optimalMove;
        }
        return optimalMove;
    }





}

class ArrayIndexComparator implements Comparator<Integer>
{
    private final Integer[] array;

    public ArrayIndexComparator(Integer[] array)
    {
        this.array = array;
    }

    public Integer[] createIndexArray()
    {
        Integer[] indexes = new Integer[array.length];
        for (int i = 0; i < array.length; i++)
        {
            indexes[i] = i; // Autoboxing
        }
        return indexes;
    }

    @Override
    public int compare(Integer index1, Integer index2)
    {
        // Autounbox from Integer to int to use as array indexes
        return array[index1].compareTo(array[index2]);
    }
}

class ArrayIndexDoubleComparator implements Comparator<Integer>
{
    private final Double[] array;

    public ArrayIndexDoubleComparator(Double[] array)
    {
        this.array = array;
    }

    public Integer[] createIndexArray()
    {
        Integer[] indexes = new Integer[array.length];
        for (int i = 0; i < array.length; i++)
        {
            indexes[i] = i; // Autoboxing
        }
        return indexes;
    }

    @Override
    public int compare(Integer index1, Integer index2)
    {
        // Autounbox from Integer to int to use as array indexes
        return array[index1].compareTo(array[index2]);
    }
}