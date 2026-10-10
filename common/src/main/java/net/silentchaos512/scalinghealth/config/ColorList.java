package net.silentchaos512.scalinghealth.config;

import com.google.common.collect.ImmutableList;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.silentchaos512.scalinghealth.lib.Color;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ColorList {
    private List<Integer> list;
    private List<? extends String> parsedFrom;
    private final ModConfigSpec.ConfigValue<List<? extends String>> config;

    public ColorList(ModConfigSpec.Builder builder, String path, String comment, int... defaults) {
        // Default list of formatted values
        List<String> defaultList = Arrays
                .stream(defaults)
                .mapToObj(Color::format)
                .collect(Collectors.toList());

        // Load a string list
        this.config = builder
                .comment(comment)
                .defineList(path, defaultList, o -> o instanceof String && Color.validate((String) o));
    }

    /** 1.20.1 recalculated on the client config reload event; this port re-parses when the config list changes. */
    public void recalculate()  {
        parsedFrom = null;
    }

    public List<Integer> get() {
        List<? extends String> raw = config.get();
        if (list == null || raw != parsedFrom) {
            list = ImmutableList.copyOf(raw.stream().map(Color::parseInt).collect(Collectors.toList()));
            parsedFrom = raw;
        }
        return list;
    }
}
