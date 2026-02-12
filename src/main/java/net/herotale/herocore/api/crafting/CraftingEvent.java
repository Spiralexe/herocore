package net.herotale.herocore.api.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a crafting event posted on an entity for system processing.
 * Phases: PRE_CRAFT → OUTPUT_PHASE → FINAL.
 */
public class CraftingEvent {

    private final UUID crafter;
    private final RecipeRef recipe;
    private List<ItemRef> inputs;
    private List<ItemRef> outputs;
    private double speedMultiplier;
    private double qualityRoll;
    private boolean cancelled;

    public CraftingEvent(UUID crafter, RecipeRef recipe, List<ItemRef> inputs, List<ItemRef> outputs) {
        this.crafter = Objects.requireNonNull(crafter);
        this.recipe = Objects.requireNonNull(recipe);
        this.inputs = new ArrayList<>(inputs);
        this.outputs = new ArrayList<>(outputs);
        this.speedMultiplier = 1.0;
        this.qualityRoll = Math.random();
        this.cancelled = false;
    }

    public UUID getCrafter() {
        return crafter;
    }

    public RecipeRef getRecipe() {
        return recipe;
    }

    public List<ItemRef> getInputs() {
        return inputs;
    }

    public List<ItemRef> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<ItemRef> outputs) {
        this.outputs = outputs;
    }

    public void addOutput(ItemRef output) {
        this.outputs.add(output);
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public void setSpeedMultiplier(double speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
    }

    public double getQualityRoll() {
        return qualityRoll;
    }

    public void setQualityRoll(double qualityRoll) {
        this.qualityRoll = qualityRoll;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
