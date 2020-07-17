package ru.ilyafx.gammacases.animation;

import org.bukkit.plugin.Plugin;
import ru.ilyafx.gammacases.type.Case;
import ru.ilyafx.gammacases.type.CaseItem;
import ru.ilyafx.gammacases.util.V3;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AnimationProvider {

    CompletableFuture<Void> startAnimation(Plugin plugin, V3 location, List<CaseItem> items, CaseItem droppedItem);

}
