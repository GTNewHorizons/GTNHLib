package com.gtnewhorizon.gtnhlib.client.model.state;

import static com.gtnewhorizon.gtnhlib.client.model.state.MonopartState.StateMatch;
import static com.gtnewhorizon.gtnhlib.util.JsonUtil.loadBool;
import static com.gtnewhorizon.gtnhlib.util.JsonUtil.loadInt;
import static com.gtnewhorizon.gtnhlib.util.JsonUtil.loadStr;

import java.lang.reflect.Type;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.gtnewhorizon.gtnhlib.client.model.JSONVariant;
import com.gtnewhorizon.gtnhlib.client.model.state.MultipartState.Case;
import com.gtnewhorizon.gtnhlib.client.model.state.MultipartState.Case.Condition;
import com.gtnewhorizon.gtnhlib.client.model.state.MultipartState.Case.MultiCon;
import com.gtnewhorizon.gtnhlib.client.model.state.MultipartState.Case.StateCon;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

public class StateDeserializer implements JsonDeserializer<StateModelMap> {

    @Override
    public StateModelMap deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        final var root = json.getAsJsonObject();

        if (root.has("variants")) {
            return new MonopartState(loadVariants(root));
        }

        if (root.has("multipart")) {
            return new MultipartState(loadMultipart(root));
        }

        throw new JsonParseException("No 'variants' or 'multipart' tag found in blockstate JSON!");
    }

    private Object2ObjectMap<StateMatch, ObjectList<JSONVariant>> loadVariants(JsonObject root) {
        final var variants = root.getAsJsonObject("variants");

        var entries = variants.entrySet();
        var map = new Object2ObjectOpenHashMap<StateMatch, ObjectList<JSONVariant>>(entries.size());

        entries.forEach(v -> {
            var match = v.getKey();
            var variant = v.getValue();
            map.put(new StateMatch(match), loadVariants(variant));
        });

        return map;
    }

    private ObjectList<Case> loadMultipart(JsonObject root) {
        final var multipart = root.getAsJsonArray("multipart");
        final var cases = new ObjectArrayList<Case>(multipart.size());

        for (var c : multipart) {
            final var kase = c.getAsJsonObject();
            final var variants = loadVariants(kase.get("apply"));
            final var condition = loadCondition(kase);
            cases.add(new Case(variants, condition));
        }

        return cases;
    }

    /**
     * Loads a list of jsonVariants from the given element. If it's an array, the list may contain multiple - otherwise
     * the list wil contain one.
     */
    private ObjectList<JSONVariant> loadVariants(JsonElement variants) {
        final ObjectList<JSONVariant> loadedJSONVariants;
        if (variants.isJsonArray()) {
            var models = variants.getAsJsonArray();
            loadedJSONVariants = new ObjectArrayList<>(models.size());

            for (var m : models) {
                var model = m.getAsJsonObject();
                loadedJSONVariants.add(loadVariant(model));
            }
        } else {
            var model = variants.getAsJsonObject();
            loadedJSONVariants = new ObjectArrayList<>(1);
            loadedJSONVariants.add(loadVariant(model));
        }

        return loadedJSONVariants;
    }

    private JSONVariant loadVariant(JsonObject variant) {
        return new JSONVariant(
                new ResourceLocation(loadStr(variant, "model")),
                loadInt(variant, "x", 0),
                loadInt(variant, "y", 0),
                0,
                loadBool(variant, "uvlock", false),
                loadInt(variant, "weight", 1));
    }

    private Condition loadCondition(JsonObject kase) {
        if (!kase.has("when")) {
            return Condition.TRUE;
        }

        final var when = kase.getAsJsonObject("when");
        if (when.has("AND")) {
            final var cons = when.getAsJsonArray("AND");
            final var conditions = LoadMultiCons(cons);
            return new MultiCon(true, conditions);
        } else if (when.has("OR")) {
            final var cons = when.getAsJsonArray("OR");
            final var conditions = LoadMultiCons(cons);
            return new MultiCon(false, conditions);
        }

        return new MultiCon(true, loadStateCons(when));
    }

    /// Given a segment like ```
    /// [
    /// {
    /// "facing": "east",
    /// "beans": "toast"
    /// },
    /// {
    /// "pied": "false"
    /// }
    /// ]
    /// ```
    /// returns a list of the MultiCons `[facing = east && beans = toast], [pied = false]`
    private ObjectArrayList<Condition> LoadMultiCons(JsonArray cons) {
        final var conditions = new ObjectArrayList<Condition>(cons.size());

        for (var con : cons) {
            conditions.add(new MultiCon(true, loadStateCons(con.getAsJsonObject())));
        }
        return conditions;
    }

    /// Given a segment like ```json
    /// {
    /// "facing": "east",
    /// "beans": "toast"
    /// },```
    /// returns a list of StateCon `[facing = east, beans = toast]`
    private static @NotNull ObjectArrayList<Condition> loadStateCons(JsonObject obj) {
        final var conds = obj.entrySet();
        final var condList = new ObjectArrayList<Condition>(conds.size());

        for (var cond : conds) {
            final var stateName = cond.getKey();
            final var stateValues = new ObjectArrayList<>(cond.getValue().getAsString().split("\\|"));

            condList.add(new StateCon(stateName, stateValues));
        }
        return condList;
    }
}
