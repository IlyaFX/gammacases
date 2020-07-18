package ru.ilyafx.gammacases.type;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;
import ru.ilyafx.gammacases.GammaCases;
import ru.ilyafx.gammacases.animation.AnimationProvider;
import ru.ilyafx.gammacases.database.Database;
import ru.ilyafx.gammacases.util.RandomUtil;
import ru.ilyafx.gammacases.util.V3;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
public class Case {

    private final String id;
    private final Supplier<AnimationProvider> animationProvider;
    private final Map<String, Supplier<List<CaseItem>>> itemsSupplier;

    public boolean canOpen(String boxId) {
        return itemsSupplier.containsKey(boxId);
    }

    public void open(Player player, String boxId, Database.CaseData data, int limit) {
        AnimationProvider provider = Objects.requireNonNull(animationProvider.get(), "Bad animation for case " + id);
        List<CaseItem> items = Optional.ofNullable(itemsSupplier.get(boxId)).orElse(Collections::emptyList).get();
        if (items == null || items.isEmpty())
            throw new RuntimeException("Null items");
        items = new ArrayList<>(items);
        Collections.shuffle(items);
        items = items.stream().limit(limit).collect(Collectors.toList());
        data.lock();
        CaseItem dropped = RandomUtil.weightedRandom(items.stream().collect(Collectors.toMap(item -> item, CaseItem::getChance)));
        provider.startAnimation(GammaCases.getInstance(), data.getV3(), items, dropped).thenAccept(v -> {
            dropped.execute(player);
            data.unlock();
        });
    }

}
