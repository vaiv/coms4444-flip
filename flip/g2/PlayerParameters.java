package flip.g2;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 *
 * @author juand.correa
 */
class PlayerParameters {

    public static final int numFeatures = 5;
    public List<Double> offenseWeights;
    public List<Double> defenseWeights;

    public Double getFeature(int idx) {
        return (idx < 3 ? offenseWeights : defenseWeights).get(idx % 4);
    }

    public void setFeature(int idx, Double value) {
        (idx < 3 ? offenseWeights : defenseWeights).set(idx % 4, value);
    }

    private static void saveParams(PlayerParameters ish) throws IOException {
        String fileName = "g2Params.txt";
        FileOutputStream fos = new FileOutputStream(fileName);
        try ( ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(ish);
        }
    }

    private static PlayerParameters loadParams() {
        FileInputStream fin = null;
        try {
            String fileName = "g2Params.txt";
            fin = new FileInputStream(fileName);
            PlayerParameters iHandler;
            try ( ObjectInputStream ois = new ObjectInputStream(fin)) {
                iHandler = (PlayerParameters) ois.readObject();
            }
            return iHandler;
        } catch (FileNotFoundException ex) {
            return null;
        } catch (IOException | ClassNotFoundException ex) {
            return null;
        } finally {
            try {
                fin.close();
            } catch (IOException ex) {

            }
        }
    }

    public static PlayerParameters generateRandomParameters() {
        Random random = new Random();
        PlayerParameters params = new PlayerParameters();
        params.offenseWeights = new ArrayList<>();
        params.defenseWeights = new ArrayList<>();
        for (int i = 0; i < numFeatures; i++) {
            double ow = random.nextGaussian() / 100.0;
            double dw = random.nextGaussian() / 100.0;
            params.offenseWeights.add(ow);
            params.defenseWeights.add(dw);
        }
        return params;
    }

    @Override
    public String toString() {
        String txt = "[ ";
        txt += this.offenseWeights.stream().map((v) -> v.toString()).collect(Collectors.joining(", ")) + ", ";
        txt += this.defenseWeights.stream().map((v) -> v.toString()).collect(Collectors.joining(", "));
        txt += " ]";
        return txt;
    }
}
