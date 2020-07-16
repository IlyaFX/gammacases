package ru.ilyafx.gammacases.util;

import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.Random;

@UtilityClass
public class RandomUtil {

    public Random RANDOM = new Random();

    public <T> T weightedRandom(Map<T, Double> weights) {
        double sum = weights.values().stream().mapToDouble(val -> val).sum();
        double rand = RANDOM.nextDouble() * sum;
        double from = 0;
        T last = null;
        for (Map.Entry<T, Double> entry : weights.entrySet()) {
            double weight = entry.getValue();
            if (rand >= from && rand < from + weight)
                return entry.getKey();
            last = entry.getKey();
            from += weight;
        }
        return last;
    }

}
