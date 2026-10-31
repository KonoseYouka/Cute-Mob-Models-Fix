package com.konoseyouka.cmmfixer.pack;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.HexFormat;
import java.util.zip.ZipFile;

public final class PackBootstrap {
    public static final String PACK_ID = "cute_mob_models_fix:required";
    public static final String REVISION = "cmmfix-pack-2";

    public record Release(String file, String url, String sha256, long bytes) {}

    public static final Release BASE = new Release(
            "CMMRP_Release_1.0_[1.20-1.20.1].zip",
            "https://mediafilez.forgecdn.net/files/4613/894/CMMRP_Release_1.0_%5B1.20-1.20.1%5D.zip",
            "fc036ba03a76a8c73f7ce63236dd0b75b24f944ffa4b516e32f4eaa643a7d3f5",
            1421107);

    public static final Release DONOR = new Release(
            "CMMRP_Release_1.2.v5_[1.20.6-1.21.1].zip",
            "https://mediafilez.forgecdn.net/files/6193/918/CMMRP_Release_1.2.v5_%5B1.20.6-1.21.1%5D.zip",
            "41a1cb8f38e0e654ba86b19e9221213fd58681f6535833b6efa3034d29cbed00",
            1799340);

    public static synchronized Path prepare(Path inputs, Path resourcePacks) throws IOException {
        Files.createDirectories(inputs);

        Path base = obtain(inputs,BASE), donor = obtain(inputs,DONOR);
        Path output = resourcePacks.resolve(PackMerger.OUTPUT);
        Path stamp = inputs.resolve("merged.sha256");

        String expected = REVISION + ":" + BASE.sha256() + ":" + DONOR.sha256() + ":";

        if (Files.isRegularFile(output)
                && Files.isRegularFile(stamp)
                && Files.readString(stamp).equals(expected+sha256(output)))
            return output;

        PackMerger.merge(base, donor, output);
        Files.writeString(stamp, expected + sha256(output));

        return output;
    }

    private static Path obtain(Path inputs, Release release) throws IOException {
        Path target = inputs.resolve(release.file());
        if (valid(target, release)) return target;

        Path temp = Files.createTempFile(inputs,"download-",".tmp");

        try {
            URL url = URI.create(release.url()).toURL();
            if (!url.getProtocol().equals("https")
                    || !url.getHost().equals("mediafilez.forgecdn.net"))
                throw new IOException("Unexpected download origin");

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("User-Agent","CuteMobModelsFix/1.1");

            try {
                if (connection.getResponseCode() != 200)
                    throw new IOException("CurseForge download returned HTTP "+connection.getResponseCode());
                long deadline = System.nanoTime() + 60_000_000_000L;

                try (InputStream in = connection.getInputStream();
                     OutputStream out = Files.newOutputStream(temp)) {
                    byte[] buffer = new byte[16384];
                    long total = 0;
                    int count;

                    while ((count = in.read(buffer)) != -1) {
                        total += count;
                        if (total > release.bytes()
                                || System.nanoTime()>deadline)
                            throw new IOException("Download exceeded expected size or time limit");

                        out.write(buffer,0,count);
                    }
                }
            }
            finally { connection.disconnect(); }

            if (!valid(temp, release))
                throw new IOException("SHA-256/size mismatch for "+release.file());

            try (ZipFile ignored = new ZipFile(temp.toFile())) {
                /* ZIPでないものを拒否 */
            }

            try {
                Files.move(temp,
                        target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp,
                        target,
                        StandardCopyOption.REPLACE_EXISTING);
            }
            return target;
        }
        finally { Files.deleteIfExists(temp); }
    }

    public static boolean valid(Path file, Release release) throws IOException {
        return Files.isRegularFile(file)
                && Files.size(file) == release.bytes()
                && sha256(file).equals(release.sha256());
    }

    public static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] bytes = new byte[16384];
                int count;
                while ((count = in.read(bytes)) != -1)
                    digest.update(bytes, 0, count);
            }
            return HexFormat.of().formatHex(digest.digest());
        }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }

    private PackBootstrap() {}
}
