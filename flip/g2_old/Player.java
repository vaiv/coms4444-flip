package flip.g2_old;

import java.util.List;
import java.util.HashMap;
import javafx.util.Pair;
import java.util.ArrayList;

import flip.sim.Point;
import flip.sim.Board;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class Player implements flip.sim.Player {

    private int seed = 42;
    private boolean isPlayer1;
    private double greedyProb;
    private Integer n;
    private Double pieceDiameter;
    private PlayerParameters params;

    public Player() {
        params = new PlayerParameters();
        params.offenseWeights = Arrays.asList(0.10817225568178673, 0.19579378351437962, 0.12078409912487767, -0.012847822989732294, -2.3869762884957217E-4);
        params.defenseWeights = Arrays.asList(-0.17835669952853897, 0.022650662584658726, 0.05629321514794808, -0.03559470879552093, -0.0054959345337099764);
    }

    PlayerParameters getParams() {
        return params;
    }

    void setParams(PlayerParameters params) {
        this.params = params;
    }

    public HashMap<Integer, Point> flip(HashMap<Integer, Point> pieces) {
        HashMap<Integer, Point> flippedPieces = new HashMap<>();
        pieces.forEach((key, value) -> flippedPieces.put(key, new Point(-value.x, value.y)));
        return flippedPieces;
    }

    /**
     *
     * @param pieces initial location of the pieces for the player.
     * @param n number of pieces available.
     * @param t total turns available.
     * @param isPlayer1 true if this player is the first to move
     * @param pieceDiameter diameter of each piece
     */
    @Override
    public void init(HashMap<Integer, Point> pieces, int n, double t, boolean isPlayer1, double pieceDiameter) {
        this.n = n;
        this.isPlayer1 = isPlayer1;
        this.pieceDiameter = pieceDiameter;
        this.greedyProb = 0.25;
    }

    public void setGreedyProb(double newProb) {
        this.greedyProb = newProb;
    }

    public Integer sample(List<Double> pdf) {
        if (Math.random() < this.greedyProb) {
            int greatestIdx = 0;
            double greatestProb = 0.;
            for (int i = 0; i < pdf.size(); i++) {
                if (pdf.get(i) > greatestProb) {
                    greatestProb = pdf.get(i);
                    greatestIdx = i;
                }
            }

            return greatestIdx;
        }

        List<Double> cdf = new ArrayList<>();
        for (int i = 0; i < pdf.size(); i++) {
            cdf.add(i == 0 ? pdf.get(i) : pdf.get(i) + cdf.get(i - 1));
        }

        double randomProb = Math.random();

        int bestIdx = 0;
        while (bestIdx < cdf.size()) {
            if (randomProb < cdf.get(bestIdx)) {
                break;
            }
            bestIdx++;
        }
        return bestIdx;
    }

    public List<Double> playerValences(List<Double> weights, HashMap<Integer, Point> playerPieces) {
        ArrayList<ArrayList<Double>> playerInputs = new ArrayList<>();
        for (int i = 0; i < playerPieces.keySet().size(); i++) {
            ArrayList<Double> playerInput = new ArrayList<>();
            Point p = playerPieces.get(i);

            // X-axis features
            double normalizedDistance = (80.0 - this.getDistanceToTarget(p)) / 80.0;
            playerInput.add(normalizedDistance);

            int quadrant = p.x <= -20 ? 0 : (p.x < 20) ? 1 : 2;
            for (int j = 0; j < 3; j++) {
                playerInput.add(quadrant == j ? 1.0 : 0.0);
            }

            playerInputs.add(playerInput);
        }
        alliedPiecesBehind(playerPieces, playerInputs);

        ArrayList<Double> scores = new ArrayList<>();
        for (int j = 0; j < playerInputs.size(); j++) {
            ArrayList<Double> inputArr = playerInputs.get(j);
            double score = 0;
            for (int z = 0; z < inputArr.size(); z++) {
                score += inputArr.get(z) * weights.get(z);
            }
            scores.add(score);
        }

        return scores;
    }

    List<Double> softmax(List<Double> scores) {
        List<Double> scoresExp = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            scoresExp.add(Math.exp(scores.get(i)));
        }

        double scoresExpSum = 0.0;
        for (int i = 0; i < scores.size(); i++) {
            scoresExpSum += scoresExp.get(i);
        }

        List<Double> scoresSoftmax = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            scoresSoftmax.add(scoresExp.get(i) / scoresExpSum);
        }

        return scoresSoftmax;
    }

    /**
     *
     * @param numMoves number of movements to return.
     * @param playerPieces location of this player's pieces.
     * @param opponentPieces location of the opponent's pieces.
     * @param isPlayer1 true if this player is/was the first one to move.
     * @return list of moves to play
     */
    @Override
    public List<Pair<Integer, Point>> getMoves(
            Integer numMoves,
            HashMap<Integer, Point> playerPieces,
            HashMap<Integer, Point> opponentPieces,
            boolean isPlayer1) {

        HashMap<Integer, Point> pp = playerPieces;
        HashMap<Integer, Point> op = opponentPieces;
        if (isPlayer1) {
            pp = this.flip(pp);
            op = this.flip(op);
        }

        List<Pair<Integer, Point>> moves = new ArrayList<>();

        List<Double> offenseValences = this.playerValences(params.offenseWeights, pp);
        List<Double> defenseValences = this.playerValences(params.defenseWeights, pp);
        List<Double> maxValences = new ArrayList<>();
        for (int i = 0; i < offenseValences.size(); i++) {
            maxValences.add(Math.max(offenseValences.get(i), defenseValences.get(i)));
        }

        List<Double> maxValencesSoftmax = this.softmax(maxValences);

        for (int i = 0; i < numMoves; i++) {
            int sampledId = this.sample(maxValencesSoftmax);

            Double offensiveScore = offenseValences.get(sampledId);
            Double defensiveScore = defenseValences.get(sampledId);
            List<Double> strategyScores = new ArrayList();
            strategyScores.add(offensiveScore);
            strategyScores.add(defensiveScore);
            int sampledStrategy = this.sample(strategyScores);

            double theta = 0.0; // offensive strategy is to go straight ahead if possible
            if (sampledStrategy == 1) {
                theta = this.defensiveTheta(pp.get(sampledId), op);
            }

            Pair<Integer, Point> newMove = this.getPositionToMove(
                    new Pair<>(sampledId, pp.get(sampledId)), pp, op, theta);
            pp.put(sampledId, newMove.getValue());

            if (isPlayer1) {
                newMove.getValue().x = -newMove.getValue().x;
            }
            moves.add(newMove);
        }

        return moves;
    }

    public double defensiveTheta(Point playerPiece, HashMap<Integer, Point> opponentPieces) {
        double shortestDistance = 9999.0;
        double shortestX = 0.0;
        double shortestY = 0.0;

        for (int i = 0; i < opponentPieces.keySet().size(); i++) {
            Point op = opponentPieces.get(i);
            if (op.x <= playerPiece.x || Math.abs(op.y - playerPiece.y) >= 20) {
                continue;
            } else {
                double distance = Math.sqrt(Math.pow(playerPiece.x - op.x, 2) + Math.pow(playerPiece.y - op.y, 2));

                if (distance <= shortestDistance) {
                    shortestX = op.x;
                    shortestY = op.y;
                    shortestDistance = distance;
                }
            }
        }

        double theta = 0.0;  // if no opponent pieces ahead of us, just book it
        if (shortestDistance <= 120.0) {
            double deltaX = shortestX - playerPiece.x;
            double deltaY = shortestY - playerPiece.y;
            theta = Math.atan2(deltaY, deltaX);
        }
        return theta;
    }

    public boolean checkValidity(Pair<Integer, Point> move, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces) {
        boolean valid = true;

        // check if move is adjacent to previous position.
        if (!Board.almostEqual(Board.getdist(player_pieces.get(move.getKey()), move.getValue()), pieceDiameter)) {
            return false;
        }
        // check for collisions
        valid = valid && !Board.check_collision(player_pieces, move);
        valid = valid && !Board.check_collision(opponent_pieces, move);

        // check within bounds
        valid = valid && Board.check_within_bounds(move);
        return valid;

    }

    /**
     * Return the list of pieces that should be considered when deciding which
     * one to move
     *
     * @param playerPieces
     * @return A filtered list of pieces
     */
    protected HashMap<Integer, Point> filterPiecesToMove(HashMap<Integer, Point> playerPieces) {
        // filter out pieces that are already in the opponents area
        double min, max, eps = 1E-7;
        if (this.isPlayer1) {
            min = -60.0;
            max = -20.0;
        } else {
            min = 20.0;
            max = 60.0;
        }
        HashMap<Integer, Point> piecesToMove = new HashMap<>();
        playerPieces.entrySet().stream().filter((entry)
                -> (entry.getValue().x - this.pieceDiameter / 2 + eps < min || entry.getValue().x + this.pieceDiameter / 2 - eps > max)
        ).forEachOrdered((entry) -> {
            piecesToMove.put(entry.getKey(), entry.getValue());
        });
        return piecesToMove;
    }

    protected void alliedPiecesBehind(HashMap<Integer, Point> playerPieces, ArrayList<ArrayList<Double>> playerInputs) {
        Point[] xSorted = playerPieces.values().stream().sorted((p1, p2) -> Double.compare(p1.x, p2.x)).toArray(Point[]::new);
        for (int i = 0; i < playerPieces.size(); i++) {
            Point p = playerPieces.get(i);
            boolean pieceBehind = false;
            int first = 0;
            int last = playerPieces.size();
            int mid = (first + last) / 2;
            while (first <= last) {
                if (xSorted[mid].x + pieceDiameter < p.x) {
                    first = mid + 1;
                } else if (xSorted[mid] == p || xSorted[mid].x >= p.x) {
                    last = mid - 1;
                } else {
                    // found another piece in the range, next we go left and
                    // right checking if they collide in the y-axis
                    int j = mid;
                    do {
                        if (Math.abs(xSorted[j].y - p.y) <= pieceDiameter) {
                            pieceBehind = true;
                            break;
                        }
                        j++;
                    } while (xSorted[j].x < p.x);
                    j = mid--;
                    while (xSorted[j].x > p.x - pieceDiameter) {
                        if (Math.abs(xSorted[j].y - p.y) <= pieceDiameter) {
                            pieceBehind = true;
                            break;
                        }
                        j++;
                    }
                    break;
                }
                mid = (first + last) / 2;
            }
            playerInputs.get(i).add(pieceBehind ? 1.0 : -1.0);
        }
    }

    protected double getDistanceToTarget(Point point) {
        return Math.max(20 - point.x, 0.0);
    }

    /**
     * Computes the distances from each point to the target line in the board
     *
     * @param playerPieces
     * @return A map with the distance of each point passed
     */
    protected HashMap<Integer, Double> getDistancesToTarget(HashMap<Integer, Point> playerPieces) {
        HashMap<Integer, Double> distanceMap = new HashMap<>();
        playerPieces.forEach((key, value) -> distanceMap.put(key, Math.max(20 - value.x, 0.0)));
        return distanceMap;
    }

    /**
     * Computes the distances from each point to the first obstacle to move
     * forward
     *
     * @param playerPieces
     * @param opponentPieces
     * @return A map with the distances corresponding to each point
     */
    protected HashMap<Integer, Double> getDistanceToFirstObstacle(HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces) {
        HashMap<Integer, Double> distanceMap = new HashMap<>();
        playerPieces.forEach((key, value) -> distanceMap.put(key, 0.0));

        return distanceMap;
    }

    /**
     * Given a piece decides to what position to move it (implicitly the angle)
     *
     * @param piece
     * @param playerPieces
     * @param opponentPieces
     * @param theta
     * @return the position where the piece will be after moving
     */
    protected Pair<Integer, Point> getPositionToMove(
            Pair<Integer, Point> piece, HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces, double theta) {
        double window = 0.0;
        while (window < Math.PI) {
            for (int i = -1; i < 2; i += 2) {
                double theta_adj = theta + i * window;
                double delta_x = 2 * Math.cos(theta_adj);
                double delta_y = 2 * Math.sin(theta_adj);
                Point newPos = new Point(piece.getValue().x + delta_x, piece.getValue().y + delta_y);
                Pair<Integer, Point> newMove = new Pair(piece.getKey(), newPos);
                if (this.checkValidity(newMove, playerPieces, opponentPieces)) {
                    return newMove;
                }
            }
            window += 0.01;
        }

        return null;
    }

    public LinkedHashMap<Integer, Double> sortHashMapByValues(
            HashMap<Integer, Double> passedMap) {
        List<Integer> mapKeys = new ArrayList<>(passedMap.keySet());
        List<Double> mapValues = new ArrayList<>(passedMap.values());
        Collections.sort(mapValues);
        Collections.sort(mapKeys);

        LinkedHashMap<Integer, Double> sortedMap
                = new LinkedHashMap<>();

        Iterator<Double> valueIt = mapValues.iterator();
        while (valueIt.hasNext()) {
            Double val = valueIt.next();
            Iterator<Integer> keyIt = mapKeys.iterator();

            while (keyIt.hasNext()) {
                Integer key = keyIt.next();
                Double comp1 = passedMap.get(key);
                Double comp2 = val;

                if (comp1.equals(comp2)) {
                    keyIt.remove();
                    sortedMap.put(key, val);
                    break;
                }
            }
        }
        return sortedMap;
    }
}
