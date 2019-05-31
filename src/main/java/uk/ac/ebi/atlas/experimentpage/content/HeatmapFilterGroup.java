package uk.ac.ebi.atlas.experimentpage.content;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.jetbrains.annotations.NotNull;
import uk.ac.ebi.atlas.model.experiment.sdrf.Factor;

import java.lang.reflect.Type;

@AutoValue
public abstract class HeatmapFilterGroup {
    public abstract String getName();
    public abstract boolean getPrimary();
    public abstract String getSelected();
    public abstract ImmutableSetMultimap<String, String> getGroupings();

    public static HeatmapFilterGroup create(@NotNull String name,
                                            boolean primary,
                                            @NotNull String selected,
                                            @NotNull Multimap<String, String> groupings) {
        return new AutoValue_HeatmapFilterGroup(
                Factor.normalize(name),
                primary,
                selected,
                ImmutableSetMultimap.<String, String>builder().putAll(groupings).build());
    }

    private static class GsonTypeAdapter implements JsonSerializer<HeatmapFilterGroup> {
        @Override
        public JsonElement serialize(HeatmapFilterGroup src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("name", src.getName());
            jsonObject.addProperty("primary", src.getPrimary());
            jsonObject.addProperty("selected", src.getSelected());

            // Each element in the groupings array is an array of two elements, e.g. [kidney, [g2, g6, g11]]
            JsonArray groupings = new JsonArray();
            src.getGroupings().keySet()
                    .forEach(factorOrSampleCharacteristicValue -> {
                        JsonArray grouping = new JsonArray();
                        // First element
                        grouping.add(factorOrSampleCharacteristicValue);
                        // Second element
                        JsonArray secondElement = new JsonArray();
                        src.getGroupings().get(factorOrSampleCharacteristicValue).forEach(secondElement::add);
                        grouping.add(secondElement);
                        groupings.add(grouping);
                    });
            jsonObject.add("groupings", groupings);

            return jsonObject;
        }
    }
    private static final GsonTypeAdapter GSON_TYPE_ADAPTER = new GsonTypeAdapter();

    public static GsonTypeAdapter getGsonTypeAdapter() {
        return GSON_TYPE_ADAPTER;
    }
}
