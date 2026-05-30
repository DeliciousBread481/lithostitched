package dev.worldgen.lithostitched.worldgen.structure;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import dev.worldgen.lithostitched.worldgen.poolelement.DelegatingPoolElement;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class LithostitchedTemplates implements Iterable<StructurePoolElement> {
    protected final List<WeightedEntry> entries;

    public LithostitchedTemplates() {
        this.entries = Lists.newArrayList();
    }

    public synchronized LithostitchedTemplates add(StructurePoolElement element, int weight) {
        this.entries.add(new WeightedEntry(element, this.entries.size(), weight));
        return this;
    }

    public List<StructurePoolElement> shuffle(RandomSource random) {
        long startTime = System.currentTimeMillis();
        int entryCount = this.entries.size();
        
        if (entryCount > 1000) {
            System.err.println("[Lithostitched WARNING] Large template pool detected: " + entryCount + " entries");
            Thread.dumpStack();
        }
        
        System.out.println("[Lithostitched] shuffle() starting with " + entryCount + " entries");
        
        try {
            List<WeightedEntry> shuffled = Lists.newArrayList(this.entries.stream().map(WeightedEntry::copy).toList());
            shuffled.forEach(entry -> entry.setRandom(random.nextFloat()));
            
            System.out.println("[Lithostitched] Starting sort operation on " + shuffled.size() + " entries");
            shuffled.sort(Comparator.comparingDouble(WeightedEntry::getRandWeight));
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            System.out.println("[Lithostitched] Sort operation completed in " + duration + "ms");
            
            if (duration > 5000) {
                System.err.println("[Lithostitched WARNING] Sort took " + duration + "ms for " + entryCount + " entries");
                Thread.dumpStack();
            }
            
            return shuffled.stream().map(WeightedEntry::getElement).toList();
        } finally {
            long finalTime = System.currentTimeMillis();
            System.out.println("[Lithostitched] shuffle() took " + (finalTime - startTime) + "ms total");
        }
    }

    public Stream<StructurePoolElement> stream() {
        return this.entries.stream().map(WeightedEntry::getElement);
    }

    @Override
    @NotNull
    public Iterator<StructurePoolElement> iterator() {
        return Iterators.transform(this.entries.iterator(), WeightedEntry::getElement);
    }

    public static class WeightedEntry {
        final StructurePoolElement element;
        final int index;
        final int weight;
        private double randWeight;
        private final boolean prioritized;

        WeightedEntry(StructurePoolElement element, int index, int weight) {
            this.element = element;
            this.index = index;
            this.weight = weight;
            this.prioritized = element instanceof DelegatingPoolElement delegating && delegating.prioritized();
        }

        private WeightedEntry copy() {
            return new WeightedEntry(this.element, this.index, this.weight);
        }

        private double getRandWeight() {
            return this.randWeight;
        }

        void setRandom(float value) {
            this.randWeight = -Math.pow(value, (1.0F / (float) this.weight)) + (this.prioritized ? -2 : 0);
        }

        public StructurePoolElement getElement() {
            return this.element;
        }

        public int getIndex() {
            return this.index;
        }

        @Override
        public String toString() {
            return this.weight + ":" + this.element;
        }
    }
}

