package com.konoseyouka.cmmfixer.pack;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public final class PackMerger {
    public static final String OUTPUT = "CMMFix-merged-1.20.1.zip";
    private static final String CEM = "assets/minecraft/optifine/cem/";

    private static final long
            MAX_ENTRY = 32L * 1024 * 1024,
            MAX_TOTAL = 256L * 1024 * 1024;

    private static final Map<String, List<String>> FAMILIES
            = Map.ofEntries(
                    Map.entry("camel",       List.of("camel/")),
                    Map.entry("dragon",      List.of("enderdragon/")),
                    Map.entry("frog",        List.of("frog/")),
                    Map.entry("hoglin",      List.of("hoglin/hoglin")),
                    Map.entry("zoglin",      List.of("hoglin/zoglin")),
                    Map.entry("parrot",      List.of("parrot/")),
                    Map.entry("polar_bear",  List.of("bear/polarbear")),
                    Map.entry("puffer_fish", List.of("fish/pufferfish")),
                    Map.entry("salmon",      List.of("fish/salmon")),
                    Map.entry("sniffer",     List.of("sniffer/")),
                    Map.entry("tadpole",     List.of("tadpole/")),
                    Map.entry("turtle",      List.of("turtle/")));

    public static List<String> merge(Path base, Path newer, Path output) throws IOException {
        if (output.toAbsolutePath().normalize().equals(base.toAbsolutePath().normalize())
                || output.toAbsolutePath().normalize().equals(newer.toAbsolutePath().normalize()))
            throw new IOException("Output must not overwrite an input archive");

        Map<String, byte[]> old = read(base), source = read(newer);
        if (!old.containsKey(CEM + "spider.jem")
                || old.containsKey(CEM + "sniffer.jem")
                || !source.containsKey(CEM + "sniffer.jem")
                || !source.containsKey(CEM + "dragon.jem"))
            throw new IOException("Expected CMMRP 1.0 base and CMMRP 1.2.v5 donor");

        List<String> added = new ArrayList<>();
        Set<String> imports = new TreeSet<>();
        for (var family : new TreeMap<>(FAMILIES).entrySet()) {
            String mob = family.getKey();
            if (old.keySet().stream().anyMatch(n -> modelFamily(n, mob))) continue;

            boolean found = false;
            for (String name : source.keySet()) {
                if (modelFamily(name, mob)) {
                    imports.add(name);
                    found = true;
                }

                for (String texture : family.getValue()) {
                    if (name.startsWith("assets/minecraft/textures/entity/" + texture)
                            || name.startsWith("assets/minecraft/optifine/random/entity/" + texture)
                            || name.startsWith("assets/minecraft/optifine/anim/blinks/" + texture)
                            || name.startsWith("assets/minecraft/optifine/anim/glints/" + texture))
                        imports.add(name);
                }
            }
            if (found) added.add(mob);
        }

        // テクスチャ・モデル・アニメーションの明示的な依存関係を推移的にたどる。
        ArrayDeque<String> queue = new ArrayDeque<>(imports);
        while (!queue.isEmpty()) {
            String name = queue.remove();
            byte[] data = source.get(name);
            if (data == null) throw new IOException("Missing resource: " + name);

            if (name.endsWith(".jem") || name.endsWith(".jpm")) {
                try { collectJsonReferences(JsonParser.parseString(new String(data, StandardCharsets.UTF_8)), name, source, imports, queue); }
                catch (JsonParseException ex) { throw new IOException("Invalid model: " + name, ex); }
            }
            else if (name.endsWith(".properties")) {
                Properties props = new Properties();
                props.load(new ByteArrayInputStream(data));

                // 1.2.v5で、ホッキョクグマのアニメーションに「entity/」ディレクトリが欠落。
                for (String key : List.of("from", "to")) {
                    String ref = props.getProperty(key);
                    if (ref != null
                            && ref.startsWith("optifine/random/")
                            && !ref.startsWith("optifine/random/entity/")
                            && !source.containsKey("assets/minecraft/"+ref)) {
                        String fixed = ref.replace("optifine/random/", "optifine/random/entity/");
                        if (source.containsKey("assets/minecraft/" + fixed)) {
                            props.setProperty(key, fixed);
                            data = new String(data, StandardCharsets.ISO_8859_1)
                                    .replace(ref, fixed)
                                    .getBytes(StandardCharsets.ISO_8859_1);
                            source.put(name, data);
                        }
                    }
                }
                for (String key : List.of("from", "to")) {
                    if (props.containsKey(key)) addReference(props.getProperty(key), name, source, imports, queue);
                }
            }
        }

        for (String name : imports) {
            // 一部の新しいテクスチャは、古いパック内でプレースホルダーとなっている可能性がある。モデルと併せて置き換える。
            old.put(name, source.get(name));
            if (source.containsKey(name + ".mcmeta")) old.put(name + ".mcmeta", source.get(name + ".mcmeta"));
        }

        old.put("pack.mcmeta",
                "{\"pack\":{\"pack_format\":15,\"description\":\"Cute Mob Models Fix: merged for Minecraft 1.20.1\"}}"
                        .getBytes(StandardCharsets.UTF_8));

        JsonObject manifest = new JsonObject();
        manifest.addProperty("schema", 1);
        manifest.addProperty("base", base.getFileName().toString());
        manifest.addProperty("donor", newer.getFileName().toString());
        manifest.add("addedModels", new Gson().toJsonTree(added));

        old.put("assets/cute_mob_models_fix/merged.json",
                new GsonBuilder().setPrettyPrinting().create().toJson(manifest)
                        .getBytes(StandardCharsets.UTF_8));

        Files.createDirectories(output.toAbsolutePath().getParent());
        Path temp = Files.createTempFile(output.toAbsolutePath().getParent(), "cmm-merge-", ".tmp");

        try {
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(temp))) {
                for (var entry : new TreeMap<>(old).entrySet()) {
                    ZipEntry ze = new ZipEntry(entry.getKey());
                    ze.setTime(0);
                    zip.putNextEntry(ze);
                    zip.write(entry.getValue());
                    zip.closeEntry();
                }
            }
            try {
                Files.move(temp,
                        output,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp,
                        output,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
        finally { Files.deleteIfExists(temp); }

        return List.copyOf(added);
    }

    private static boolean modelFamily(String path, String mob) {
        if (!path.startsWith(CEM)) return false;
        String rest = path.substring(CEM.length());

        return rest.matches(java.util.regex.Pattern.quote(mob) + "(?:[0-9]+|_(?:small|medium|big))?\\.(?:jem|properties)");
    }

    private static void collectJsonReferences(
            JsonElement json,
            String file,
            Map<String, byte[]> source,
            Set<String> imports,
            ArrayDeque<String> queue
    ) throws IOException {
        if (json.isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray())
                collectJsonReferences(e, file, source, imports, queue);
        } else if (json.isJsonObject()) {
            for (var e : json.getAsJsonObject().entrySet()) {
                if ((e.getKey().equals("texture") || e.getKey().equals("model"))
                        && e.getValue().isJsonPrimitive())
                    addReference(e.getValue().getAsString(), file, source, imports, queue);
                else
                    collectJsonReferences(e.getValue(), file, source, imports, queue);
            }
        }
    }

    private static void addReference(
            String ref,
            String owner,
            Map<String, byte[]> source,
            Set<String> imports,
            ArrayDeque<String> queue
    ) throws IOException {
        String path;

        if (ref.startsWith("~/"))
            path = "assets/minecraft/optifine/" + ref.substring(2);
        else if (ref.contains(":"))
            path = "assets/" + ref.replace(':', '/');
        else if (ref.startsWith("textures/") || ref.startsWith("optifine/"))
            path = "assets/minecraft/" + ref;
        else path = owner.substring(0, owner.lastIndexOf('/') + 1) + ref;

        path = Path.of(path).normalize().toString().replace('\\', '/');

        if (!path.startsWith("assets/"))
            throw new IOException("Unsafe resource reference: " + ref);
        if (!path.endsWith(".png") && !path.endsWith(".jpm"))
            path += ".jpm";
        if (!source.containsKey(path))
            throw new IOException("Unresolved dependency " + ref + " in " + owner);
        if (imports.add(path))
            queue.add(path);
    }

    private static Map<String, byte[]> read(Path file) throws IOException {
        Map<String, byte[]> result = new HashMap<>();
        long total = 0;
        try (ZipFile zip = new ZipFile(file.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (e.isDirectory()) continue;

                String name = e.getName();
                if (name.startsWith("/")
                        || name.contains("\\")
                        || name.contains(":")
                        || Arrays.asList(name.split("/")).contains(".."))
                    throw new IOException("Unsafe ZIP entry: " + name);

                try (InputStream in = zip.getInputStream(e)) {
                    byte[] bytes = in.readNBytes((int)MAX_ENTRY + 1);
                    total += bytes.length;

                    if (bytes.length > MAX_ENTRY || total > MAX_TOTAL)
                        throw new IOException("Pack exceeds size limit");
                    if (result.putIfAbsent(name, bytes) != null)
                        throw new IOException("Duplicate ZIP entry: " + name);
                }
            }
        }
        return result;
    }

    private PackMerger() {}
}
