package flip.g2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import flip.sim.Point;
import flip.sim.Board;
import flip.sim.Log;
import flip.sim.Player;
import javafx.util.Pair;

import java.io.*;

public class Simulator {

    private static final String ROOT = "flip";
    private static final Integer n_pieces = 30;
    private static final Integer seed = 42;
    private static final Integer numFeatures = 8;
    private static final Integer iterations = 10;
    private static final Integer experiments = 2;
    private static final Integer turns = 1000;
    private static final double maxUpdate = 0.01;
    private static final double delta = 0.01;
    private static final double learningRate = 0.01;
    private static final long TIMEOUT = 1000;
    private static Random random;

    public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        String o[] = new String[] { "g1", "g3", "g4", "g6", "g7", "g8", "g9beginner", "g9greedy"};
        List<String> opponents = Arrays.asList(o);
        random = new Random();

        PlayerParameters params = PlayerParameters.generateRandomParameters();
        Log.log("Initial parameters\\n");
        System.out.println("Initial Params");
        System.out.println(params);
        Log.log(params.toString());
        for (int i = 0; i < iterations; i++) {
            ArrayList<Double> gradients = new ArrayList<Double>();

            Board board = new Board(n_pieces, random.nextInt(100));
            double initGreedyProb = 0.3;
            double endGreedyProb = 0.7;
            double greedyProb = (initGreedyProb * (iterations.doubleValue() - i) / iterations.doubleValue())
                    + (endGreedyProb * (i / iterations.doubleValue()));

            System.out.println("Greedy Prob: " + greedyProb);

            HashMap<Integer, Point> player1Pieces = board.player1;
            HashMap<Integer, Point> player2Pieces = board.player2;
            board.player1 = deepClone(player1Pieces);
            board.player2 = deepClone(player2Pieces);
            double avgZero = runExperiment(board, opponents, params, greedyProb);
            System.out.println("Average victory: " + avgZero);
            for(int featureIdx = 0; featureIdx < numFeatures; featureIdx++) {
                //-delta
                params.setFeature(featureIdx, params.getFeature(featureIdx) - delta);
                board.player1 = deepClone(player1Pieces);
                board.player2 = deepClone(player2Pieces);
                double avgMinus = runExperiment(board, opponents, params, greedyProb);

                //+delta
                params.setFeature(featureIdx, params.getFeature(featureIdx) + 2 * delta);
                board.player1 = deepClone(player1Pieces);
                board.player2 = deepClone(player2Pieces);
                double avgPlus = runExperiment(board, opponents, params, greedyProb);

                //restore to zero change
                params.setFeature(featureIdx, params.getFeature(featureIdx) - delta);
                double lowerSlope = (avgZero - avgMinus) / delta;
                double upperSlope = (avgPlus - avgZero) / delta;

                if(Math.signum(lowerSlope) == Math.signum(upperSlope)) {
                    double slope = (0.5 * (avgZero - avgMinus) / delta) + (0.5 * (avgPlus - avgZero) / delta);
                    gradients.add(slope);
                } else {
                    gradients.add(0.0);
                }
            }

            for(int featureIdx = 0; featureIdx < numFeatures; featureIdx++) {
                double weightUpdate = Math.max(Math.min(gradients.get(featureIdx) * learningRate, maxUpdate), -maxUpdate);
                params.setFeature(featureIdx, params.getFeature(featureIdx) + weightUpdate);
            }
        }
        Log.log("\\nFinal parameters\\n" + params);
        System.out.println("Final params:");
        System.out.println(params);
        System.exit(0);
    }


    private static double runExperiment(Board board, List<String> opponents, PlayerParameters params, double greedyProb) {
        double averageDelta = 0.0;
        for (int i = 0; i < experiments; i++) {
            PlayerWrapper player1 = null;
            PlayerWrapper player2 = null;

            for (String opponent : opponents) {
                try {
                    player1 = loadPlayerWrapper("g2", "g2");
                    player2 = loadPlayerWrapper(cleanName(opponent), opponent);

                    ((flip.g2.Player) player1.getPlayer()).setGreedyProb(greedyProb);
                    ((flip.g2.Player) player1.getPlayer()).setParams(params);
                    List<Integer> scores = runGame(board, player1, player2);
                    averageDelta += scores.get(0) - scores.get(1);
                } catch (Exception ex) {
                    Log.log("Unable to load players. " + ex.getMessage());
                    System.out.println("Unable to load players. " + ex.getMessage());
                    break;
                } finally {
                    if (player1 != null) player1.destroy();
                    if (player2 != null) player2.destroy();
                }
            }
        }
        System.gc();
        return averageDelta / (experiments * n_pieces * opponents.size());
    }

    private static List<Integer> runGame(Board game, PlayerWrapper player1, PlayerWrapper player2) {

        HashMap<Integer, Point> player1_pieces = game.get_pieces(true);
        HashMap<Integer, Point> player2_pieces = game.get_pieces(false);

        try {
            player1.init(deepClone(player1_pieces), n_pieces, turns, true, Board.get_diameter_piece());
            player2.init(deepClone(player2_pieces), n_pieces, turns, false, Board.get_diameter_piece());
        } catch (Exception ex) {
            Log.log("Unable to begin game, players failed to initialize. " + ex.getMessage());
            return null;
        }

        Integer player1Score = 0;
        Integer player2Score = 0;

        for (int j = 0; j < turns; j++) {
            player1Score = Math.max(playTurn(player1, player2, game, j == 0 ? 1 : 2, true), player1Score);

            if (Objects.equals(player1Score, n_pieces)) {
                break;
            }

            player2Score = Math.max(playTurn(player1, player2, game, 2, false), player2Score);

            if (Objects.equals(player2Score, n_pieces)) {
                break;
            }
        }
        List<Integer> scores = new ArrayList<>();
        scores.add(player1Score);
        scores.add(player2Score);
        return scores;
    }

    private static String cleanName(String s) {
        String res;
        if (s.contains("_")) {
            Integer idx = s.lastIndexOf("_");
            res = s.substring(0, idx);
        } else {
            return s;
        }

        return res;
    }

    private static Integer playTurn(PlayerWrapper player1, PlayerWrapper player2, Board game, Integer num_moves, boolean isplayer1) {
        Integer score = 0;
        List<Pair<Integer, Point>> moves;
        HashMap<Integer, Point> player1_pieces = game.get_pieces(true);
        HashMap<Integer, Point> player2_pieces = game.get_pieces(false);

        try {
            if (isplayer1) {
                moves = player1.getMoves(num_moves, deepClone(player1_pieces), deepClone(player2_pieces), true);
            } else {
                moves = player2.getMoves(num_moves, deepClone(player2_pieces), deepClone(player1_pieces), false);
            }

            if (moves.size() > num_moves) {
                Log.record("More moves than requested returned. Only first " + num_moves.toString() + " were considered.");
            }

            Integer i = 0;
            for (Pair<Integer, Point> move : moves) {
                i++;
                if (i > num_moves) {
                    break;
                }

                if (game.check_valid_move(move, isplayer1)) {
                    Log.record("Player " + (isplayer1 ? "1" : "2") + " moved piece " + move.getKey() + " to " + move.getValue());
                    game.move_piece(move, isplayer1);
                } else {
                    Log.record("Player " + (isplayer1 ? "1" : "2") + " could not move piece " + move.getKey() + " to " + move.getValue() + ". Invalid move.");
                }
            }

            score = game.get_score(isplayer1);

        } catch (Exception ex) {
            Log.record("Player " + (isplayer1 ? "1" : "2") + "turn ended with an exception. " + ex.getMessage());
        }

        return score;
    }

    private static PlayerWrapper loadPlayerWrapper(String name, String mod_name) throws Exception {
        Log.log("Loading player " + name);
        Player p = loadPlayer(name);
        if (p == null) {
            Log.log("Cannot load player " + name);
            System.exit(1);
        }

        return new PlayerWrapper(p, mod_name, TIMEOUT);
    }

    public static Player loadPlayer(String name) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        String sep = File.separator;
        Set<File> player_files = directory(ROOT + sep + name, ".java");
        File class_file = new File(ROOT + sep + name + sep + "Player.class");
        long class_modified = class_file.exists() ? class_file.lastModified() : -1;
        if (class_modified < 0 || class_modified < last_modified(player_files)
                || class_modified < last_modified(directory(ROOT + sep + "sim", ".java"))) {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                throw new IOException("Cannot find Java compiler");
            }
            StandardJavaFileManager manager = compiler.
                    getStandardFileManager(null, null, null);
//            long files = player_files.size();
            Log.log("Compiling for player " + name);
            if (!compiler.getTask(null, manager, null, null, null,
                    manager.getJavaFileObjectsFromFiles(player_files)).call()) {
                throw new IOException("Compilation failed");
            }
            class_file = new File(ROOT + sep + name + sep + "Player.class");
            if (!class_file.exists()) {
                throw new FileNotFoundException("Missing class file");
            }
        }
        ClassLoader loader = Simulator.class.getClassLoader();
        if (loader == null) {
            throw new IOException("Cannot find Java class loader");
        }
        @SuppressWarnings("rawtypes")
        Class raw_class = loader.loadClass(ROOT + "." + name + ".Player");
        return (Player) raw_class.newInstance();
    }

    private static long last_modified(Iterable<File> files) {
        long last_date = 0;
        for (File file : files) {
            long date = file.lastModified();
            if (last_date < date) {
                last_date = date;
            }
        }
        return last_date;
    }

    private static Set<File> directory(String path, String extension) {
        Set<File> files = new HashSet<>();
        Set<File> prev_dirs = new HashSet<>();
        prev_dirs.add(new File(path));
        do {
            Set<File> next_dirs = new HashSet<>();
            for (File dir : prev_dirs) {
                for (File file : dir.listFiles()) {
                    if (!file.canRead()) ; else if (file.isDirectory()) {
                        next_dirs.add(file);
                    } else if (file.getPath().endsWith(extension)) {
                        files.add(file);
                    }
                }
            }
            prev_dirs = next_dirs;
        } while (!prev_dirs.isEmpty());
        return files;
    }

    private static <T extends Object> T deepClone(T object) {
        if (object == null) {
            return null;
        }

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(object);
            ByteArrayInputStream bais = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            ObjectInputStream objectInputStream = new ObjectInputStream(bais);
            return (T) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

}
