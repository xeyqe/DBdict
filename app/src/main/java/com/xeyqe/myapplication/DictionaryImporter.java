package com.xeyqe.myapplication;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Streams UTF-8 TSV or a gzip-compressed StarDict tar archive. No archive paths are extracted. */
final class DictionaryImporter {
    private static final long MAX_BYTES = 512L * 1024 * 1024;
    // Large reference dictionaries can expand well beyond their archive size.
    // Data is written to a temporary file and read one definition at a time.
    private static final long MAX_DICTIONARY_BYTES = 2L * 1024 * 1024 * 1024;
    private static final int MAX_FIELD = 1024 * 1024;
    private static final int MAX_ENTRIES = 1_000_000;

    interface Sink { void add(String word, String meaning, String dictionary) throws IOException; }

    static int read(InputStream source, File cache, String name, Sink sink) throws IOException {
        BufferedInputStream input = new BufferedInputStream(source);
        input.mark(2);
        int first = input.read(), second = input.read();
        input.reset();
        if (first == 0x1f && second == 0x8b) {
            File directory = Files.createTempDirectory(cache.toPath(), "dictionary-").toFile();
            try (InputStream gzip = new GZIPInputStream(input)) {
                return readArchive(gzip, directory, name, sink);
            } finally {
                File[] files = directory.listFiles();
                if (files != null) for (File file : files) Files.deleteIfExists(file.toPath());
                Files.deleteIfExists(directory.toPath());
            }
        }
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new LimitedInputStream(input),
                StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)))) {
            StringBuilder line = new StringBuilder();
            boolean firstCharacter = true;
            boolean afterCarriageReturn = false;
            int lineNumber = 1;
            int ch;
            while ((ch = reader.read()) != -1) {
                if (firstCharacter) {
                    firstCharacter = false;
                    if (ch == '\ufeff') continue;
                }
                if (afterCarriageReturn && ch == '\n') {
                    afterCarriageReturn = false;
                    continue;
                }
                afterCarriageReturn = ch == '\r';
                if (ch == '\n' || ch == '\r') {
                    count += textEntry(line.toString(), name, lineNumber++, sink);
                    line.setLength(0);
                    if (count > MAX_ENTRIES) throw invalid("Too many entries");
                } else {
                    if (line.length() >= MAX_FIELD) throw invalid("Line is too long");
                    if (ch == 0) throw invalid("NUL character in text dictionary");
                    line.append((char) ch);
                }
            }
            count += textEntry(line.toString(), name, lineNumber, sink);
        }
        if (count > MAX_ENTRIES) throw invalid("Too many entries");
        if (count == 0) throw invalid("No entries found");
        return count;
    }

    private static int textEntry(String line, String name, int lineNumber, Sink sink) throws IOException {
        if (line.startsWith("##") || line.trim().isEmpty()) return 0;
        int tab = line.indexOf('\t');
        if (tab <= 0 || line.substring(0, tab).trim().isEmpty()
                || line.substring(tab + 1).trim().isEmpty()) {
            throw invalid("Expected a word and tab-separated meaning on line " + lineNumber);
        }
        sink.add(line.substring(0, tab), line.substring(tab + 1).replace("\\n", "<br>"), name);
        return 1;
    }

    /** Enforce the file limit in UTF-8 bytes, rather than decoded Java characters. */
    private static final class LimitedInputStream extends FilterInputStream {
        private long total;

        LimitedInputStream(InputStream input) { super(input); }

        @Override public int read() throws IOException {
            int value = in.read();
            if (value != -1) record(1);
            return value;
        }

        @Override public int read(byte[] bytes, int offset, int length) throws IOException {
            int count = in.read(bytes, offset, length);
            if (count > 0) record(count);
            return count;
        }

        private void record(int count) throws IOException {
            if ((total += count) > MAX_BYTES) throw invalid("Text file is too large");
        }
    }

    private static int readArchive(InputStream input, File directory, String name, Sink sink) throws IOException {
        Map<String, File> members = new LinkedHashMap<>();
        byte[] header = new byte[512];
        long total = 0;
        int headers = 0;
        String longName = null;
        while (true) {
            readFully(input, header);
            boolean empty = true;
            for (byte b : header) if (b != 0) { empty = false; break; }
            if (empty) {
                if (longName != null) throw invalid("Missing member after long filename");
                // Consume the trailer too: GZIPInputStream checks its CRC only at EOF.
                long trailer = 0;
                int next;
                while ((next = input.read()) != -1) {
                    if (next != 0 || ++trailer > MAX_FIELD) throw invalid("Invalid archive trailer");
                }
                if (trailer < 512) throw invalid("Truncated tar trailer");
                break;
            }
            if (++headers > 10_000) throw invalid("Too many archive members");
            long checksum = 0;
            for (int i = 0; i < header.length; i++) checksum += i >= 148 && i < 156 ? 32 : header[i] & 255;
            if (checksum != octal(header, 148, 8)) throw invalid("Invalid tar checksum");
            long size = octal(header, 124, 12);
            total += size;
            if (size > MAX_BYTES || total > MAX_BYTES) throw invalid("Archive is too large");
            int type = header[156];
            if (type == 'L') {
                // GNU tar stores a long filename in a separate record before its file.
                if (longName != null || size <= 0 || size > MAX_FIELD) throw invalid("Invalid long filename");
                byte[] nameBytes = new byte[(int) size];
                readFully(input, nameBytes);
                longName = cstring(nameBytes, 0, nameBytes.length);
                if (longName.isEmpty()) throw invalid("Empty long filename");
                copy(input, null, (512 - size % 512) % 512);
                continue;
            }
            String path = longName;
            longName = null;
            if (path == null) {
                path = cstring(header, 0, 100);
                String prefix = cstring(header, 345, 155);
                if (!prefix.isEmpty()) path = prefix + "/" + path;
            }
            // Store data under generated names, never under untrusted archive paths.
            if ((type == 0 || type == '0') && (path.endsWith(".ifo") || path.endsWith(".idx")
                    || path.endsWith(".idx.gz") || path.endsWith(".dict") || path.endsWith(".dict.dz"))) {
                if (members.containsKey(path)) throw invalid("Duplicate archive member");
                File file = new File(directory, "member-" + members.size());
                try (OutputStream output = new FileOutputStream(file)) { copy(input, output, size); }
                members.put(path, file);
            } else {
                copy(input, null, size);
            }
            copy(input, null, (512 - size % 512) % 512);
        }
        List<String> dictionaries = new ArrayList<>();
        for (String path : members.keySet()) if (path.endsWith(".ifo")) dictionaries.add(path);
        if (dictionaries.isEmpty()) throw invalid("No StarDict dictionaries found");
        int count = 0;
        for (String path : dictionaries) {
            Map<String, String> info = readInfo(members.get(path));
            String stem = path.substring(0, path.length() - 4);
            String label = dictionaries.size() == 1 ? name : name + " / " + info.get("bookname");
            File index = component(members, stem, ".idx", ".idx.gz");
            File data = component(members, stem, ".dict", ".dict.dz");
            File expanded = new File(directory, "expanded");
            try (InputStream source = open(data, !members.containsKey(stem + ".dict"));
                 OutputStream output = new FileOutputStream(expanded)) {
                copyLimited(source, output);
            }
            long dictionaryCount = 0;
            long indexBytes = 0;
            boolean wide = "64".equals(info.get("idxoffsetbits"));
            try (DataInputStream idx = new DataInputStream(open(index, !members.containsKey(stem + ".idx")));
                 RandomAccessFile definitions = new RandomAccessFile(expanded, "r")) {
                int start;
                while ((start = idx.read()) != -1) {
                    ByteArrayOutputStream word = new ByteArrayOutputStream();
                    while (start != 0) {
                        if (word.size() >= 1024 || start == -1) throw invalid("Invalid index word");
                        word.write(start);
                        start = idx.read();
                    }
                    long offset = wide ? idx.readLong() : Integer.toUnsignedLong(idx.readInt());
                    long size = Integer.toUnsignedLong(idx.readInt());
                    indexBytes += word.size() + 1 + (wide ? 8 : 4) + 4;
                    if (offset < 0 || size > MAX_FIELD || offset > definitions.length() - size) throw invalid("Invalid definition offset");
                    byte[] definition = new byte[(int) size];
                    definitions.seek(offset);
                    definitions.readFully(definition);
                    String meaning = meaning(definition, info.getOrDefault("sametypesequence", ""));
                    if (word.size() == 0 || meaning.isEmpty()) throw invalid("Empty dictionary entry");
                    sink.add(utf8(word.toByteArray()), meaning, label);
                    dictionaryCount++;
                    if (++count > MAX_ENTRIES) throw invalid("Too many entries");
                }
            }
            if (!Long.toString(dictionaryCount).equals(info.get("wordcount"))
                    || !Long.toString(indexBytes).equals(info.get("idxfilesize"))) throw invalid("Index does not match dictionary metadata");
        }
        if (count == 0) throw invalid("No entries found");
        return count;
    }

    private static Map<String, String> readInfo(File file) throws IOException {
        if (file.length() > MAX_FIELD) throw invalid("Metadata is too large");
        String[] lines = utf8(Files.readAllBytes(file.toPath())).split("\r?\n");
        if (lines.length == 0 || !"StarDict's dict ifo file".equals(lines[0])) throw invalid("Invalid StarDict metadata");
        Map<String, String> info = new HashMap<>();
        for (String line : lines) {
            int equals = line.indexOf('=');
            if (equals > 0) info.put(line.substring(0, equals), line.substring(equals + 1));
        }
        if (!("2.4.2".equals(info.get("version")) || "3.0.0".equals(info.get("version")))
                || !info.containsKey("bookname") || (info.containsKey("idxoffsetbits")
                && !"32".equals(info.get("idxoffsetbits")) && !"64".equals(info.get("idxoffsetbits")))) throw invalid("Unsupported StarDict metadata");
        return info;
    }

    private static String meaning(byte[] bytes, String sequence) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        StringBuilder result = new StringBuilder();
        int field = 0;
        while (buffer.hasRemaining()) {
            char type = sequence.isEmpty() ? (char) (buffer.get() & 255)
                    : field < sequence.length() ? sequence.charAt(field) : 0;
            if ("mghty".indexOf(type) < 0) throw invalid("Unsupported StarDict definition type");
            int start = buffer.position();
            boolean last = !sequence.isEmpty() && field == sequence.length() - 1;
            int end = start;
            while (buffer.hasRemaining()) {
                if (buffer.get() == 0) break;
                end++;
            }
            if (!last && (end == bytes.length)) throw invalid("Unterminated definition");
            if (last && buffer.hasRemaining()) throw invalid("Unexpected definition data");
            String text = utf8(Arrays.copyOfRange(bytes, start, end));
            if (type != 'g' && type != 'h') text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            if (result.length() > 0) result.append("<br>");
            result.append(text.replace("\n", "<br>"));
            field++;
        }
        if (!sequence.isEmpty() && field != sequence.length()) throw invalid("Missing definition fields");
        return result.toString();
    }

    private static File component(Map<String, File> files, String stem, String plain, String compressed) throws IOException {
        File result = files.get(stem + plain);
        if (result == null) result = files.get(stem + compressed);
        if (result == null) throw invalid("Missing StarDict component");
        return result;
    }

    private static InputStream open(File file, boolean compressed) throws IOException {
        InputStream stream = new BufferedInputStream(new FileInputStream(file));
        if (!compressed) return stream;
        try { return new GZIPInputStream(stream); }
        catch (IOException error) { stream.close(); throw error; }
    }

    private static String utf8(byte[] bytes) throws CharacterCodingException {
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString();
    }

    private static String cstring(byte[] bytes, int offset, int length) throws IOException {
        int end = offset;
        while (end < offset + length && bytes[end] != 0) end++;
        return utf8(Arrays.copyOfRange(bytes, offset, end));
    }

    private static long octal(byte[] bytes, int offset, int length) throws IOException {
        String value = cstring(bytes, offset, length).trim();
        try { return value.isEmpty() ? 0 : Long.parseLong(value, 8); }
        catch (NumberFormatException error) { throw invalid("Invalid tar size"); }
    }

    private static void readFully(InputStream input, byte[] bytes) throws IOException {
        new DataInputStream(input).readFully(bytes);
    }

    private static void copy(InputStream input, OutputStream output, long size) throws IOException {
        if (size < 0) throw invalid("Invalid size");
        byte[] bytes = new byte[8192];
        while (size > 0) {
            int read = input.read(bytes, 0, (int) Math.min(size, bytes.length));
            if (read == -1) throw new EOFException("Truncated archive");
            if (output != null) output.write(bytes, 0, read);
            size -= read;
        }
    }

    private static void copyLimited(InputStream input, OutputStream output) throws IOException {
        byte[] bytes = new byte[8192];
        long total = 0;
        int read;
        while ((read = input.read(bytes)) != -1) {
            if ((total += read) > MAX_DICTIONARY_BYTES) throw invalid("Dictionary is too large");
            output.write(bytes, 0, read);
        }
    }

    private static IOException invalid(String message) { return new IOException(message); }
    private DictionaryImporter() { }
}
