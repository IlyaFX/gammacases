package ru.ilyafx.gammacases.type;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;
import ru.ilyafx.gammacases.GammaCases;
import ru.ilyafx.gammacases.animation.AnimationProvider;
import ru.ilyafx.gammacases.util.RandomUtil;
import ru.ilyafx.gammacases.util.V3;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
public class Case {

    private final String id;
    private final Supplier<AnimationProvider> animationProvider;
    private final Map<String, CaseItem> items;
    private final Map<String, Key> keys;

    public void open(Player player, V3 location, GammaCases.CaseData data) {
        AnimationProvider provider = Objects.requireNonNull(animationProvider.get(), "Bad animation for case " + id);
        data.lock();
        CaseItem dropped = RandomUtil.weightedRandom(items.values().stream().collect(Collectors.toMap(item -> item, CaseItem::getChance)));
        provider.startAnimation(GammaCases.getInstance(), location, this, dropped).thenAccept(v -> {
            dropped.execute(player);
            data.unlock();
        });
    }

}
