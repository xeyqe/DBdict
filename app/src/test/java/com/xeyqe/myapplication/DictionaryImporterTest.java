package com.xeyqe.myapplication;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.*;

public class DictionaryImporterTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void readsUtf8TextAndEscapedNewlines() throws Exception {
        List<String> rows = new ArrayList<>();
        int count = read(bytes("\ufeff## comment\r\nHund\tpes\\nzvíře\r\nKočka\tcat"), rows);
        assertEquals(2, count);
        assertEquals("Hund|pes<br>zvíře|Test", rows.get(0));
        assertEquals("Kočka|cat|Test", rows.get(1));
    }

    @Test public void supportsAllCommonLineEndings() throws Exception {
        for (String newline : new String[]{"\n", "\r\n", "\r"}) {
            List<String> rows = new ArrayList<>();
            assertEquals(2, read(bytes("Hund\tpes" + newline + "Kočka\tcat" + newline), rows));
            assertEquals("Hund|pes|Test", rows.get(0));
            assertEquals("Kočka|cat|Test", rows.get(1));
        }
    }

    @Test public void stripsOnlyTheInitialBomAndPreservesDefinitionTabsAndMarkup() throws Exception {
        List<String> rows = new ArrayList<>();
        assertEquals(1, read(bytes("\ufeffword\t<b>meaning</b>\tmore\ufefftext"), rows));
        assertEquals("word|<b>meaning</b>\tmore\ufefftext|Test", rows.get(0));
    }

    @Test public void rejectsMalformedAndBlankFieldsInsteadOfSilentlyDroppingEntries() throws Exception {
        for (String bad : new String[]{"missing tab", "\tmeaning", "word\t", "   \tmeaning", "word\t   ", "word\tbad\0text"}) {
            assertThrows(IOException.class, () -> read(bytes("valid\tentry\n" + bad), new ArrayList<>()));
        }
    }

    @Test public void ignoresCommentsAndBlankLines() throws Exception {
        List<String> rows = new ArrayList<>();
        assertEquals(1, read(bytes("## comment\n\n  \n\t\nword\tmeaning\n"), rows));
        assertEquals("word|meaning|Test", rows.get(0));
    }

    @Test public void rejectsMalformedUtf8AndOverlongLines() throws Exception {
        byte[] invalid = new byte[]{'w', '\t', (byte) 0xc3, 0x28};
        assertThrows(IOException.class, () -> read(invalid, new ArrayList<>()));
        assertThrows(IOException.class, () -> read(bytes("word\t" + "a".repeat(1024 * 1024)), new ArrayList<>()));
    }

    @Test public void enforcesEntryLimitOnLastLineWithoutNewline() throws Exception {
        byte[] input = bytes("w\tm\n".repeat(1_000_000) + "last\tmeaning");
        assertThrows(IOException.class, () -> DictionaryImporter.read(new ByteArrayInputStream(input),
                temporary.getRoot(), "Test", (word, meaning, name) -> { }));
    }

    @Test public void importsBothDirectionsAndCompressedComponents() throws Exception {
        Map<String, byte[]> members = new LinkedHashMap<>();
        dictionary(members, "folder/german-czech", "Německo-Český", "g", false, false,
                "Hund", "<b>pes</b>\nzvíře");
        dictionary(members, "folder/czech-german", "Česko-Německý", "m", true, true,
                "pes", "Hund & Tier");
        List<String> rows = new ArrayList<>();
        assertEquals(2, read(archive(members), rows));
        assertEquals("Hund|<b>pes</b><br>zvíře|Test / Německo-Český", rows.get(0));
        assertEquals("pes|Hund &amp; Tier|Test / Česko-Německý", rows.get(1));
        assertArrayEquals(new String[0], temporary.getRoot().list());
    }

    @Test public void readsTypedFieldsWithoutSameTypeSequence() throws Exception {
        Map<String, byte[]> members = new LinkedHashMap<>();
        dictionary(members, "dict", "Book", "", false, false, "word", "mplain\0h<b>bold</b>\0");
        List<String> rows = new ArrayList<>();
        read(archive(members), rows);
        assertEquals("word|plain<br><b>bold</b>|Test", rows.get(0));
    }

    @Test public void rejectsMissingComponentsAndCleansTemporaryFiles() throws Exception {
        Map<String, byte[]> members = new LinkedHashMap<>();
        dictionary(members, "dict", "Book", "m", false, false, "word", "meaning");
        members.remove("dict.idx");
        assertThrows(IOException.class, () -> read(archive(members), new ArrayList<>()));
        assertArrayEquals(new String[0], temporary.getRoot().list());
    }

    @Test public void rejectsBadOffsetsMetadataAndUnsupportedFields() throws Exception {
        Map<String, byte[]> members = new LinkedHashMap<>();
        dictionary(members, "dict", "Book", "m", false, false, "word", "meaning");
        byte[] validIndex = members.get("dict.idx").clone();
        Arrays.fill(members.get("dict.idx"), 5, 9, (byte) 0xff);
        assertThrows(IOException.class, () -> read(archive(members), new ArrayList<>()));
        members.put("dict.idx", validIndex);
        members.put("dict.ifo", bytes(new String(members.get("dict.ifo"), StandardCharsets.UTF_8).replace("wordcount=1", "wordcount=2")));
        assertThrows(IOException.class, () -> read(archive(members), new ArrayList<>()));
        dictionary(members, "dict", "Book", "W", false, false, "word", "binary");
        assertThrows(IOException.class, () -> read(archive(members), new ArrayList<>()));
    }

    @Test public void rejectsEmptyBinaryAndTruncatedInput() throws Exception {
        assertThrows(IOException.class, () -> read(new byte[0], new ArrayList<>()));
        assertThrows(IOException.class, () -> read(new byte[]{(byte) 0xff, 0, 1}, new ArrayList<>()));
        byte[] archive = archive(Collections.singletonMap("dict.ifo", bytes("invalid")));
        assertThrows(IOException.class, () -> read(Arrays.copyOf(archive, archive.length / 2), new ArrayList<>()));
    }

    @Test public void neverExtractsArchiveMemberPaths() throws Exception {
        Map<String, byte[]> members = new LinkedHashMap<>();
        dictionary(members, "../../dict", "Book", "m", false, false, "word", "meaning");
        List<String> rows = new ArrayList<>();
        assertEquals(1, read(archive(members), rows));
        assertArrayEquals(new String[0], temporary.getRoot().list());
    }

    @Test public void rejectsCorruptGzipTrailer() throws Exception {
        Map<String, byte[]> members = new LinkedHashMap<>();
        dictionary(members, "dict", "Book", "m", false, false, "word", "meaning");
        byte[] archive = archive(members);
        archive[archive.length - 8] ^= 1;
        assertThrows(IOException.class, () -> read(archive, new ArrayList<>()));
    }

    @Test public void importsGnuLongFilenameForCompressedDictionaryData() throws Exception {
        Map<String, byte[]> members = new LinkedHashMap<>();
        // Like the PONS archive: only the .dict.dz path exceeds tar's 100-byte name field.
        String stem = "folder/" + "a".repeat(87);
        dictionary(members, stem, "Book", "h", false, false, "word", "<b>meaning</b>");
        List<String> rows = new ArrayList<>();
        assertEquals(1, read(archive(members), rows));
        assertEquals("word|<b>meaning</b>|Test", rows.get(0));
        assertArrayEquals(new String[0], temporary.getRoot().list());
    }

    @Test public void longFilenameOverridesHeaderEvenWhenUtf8IsTruncated() throws Exception {
        Map<String, byte[]> members = new LinkedHashMap<>();
        // The 100-byte fallback header ends halfway through a UTF-8 character.
        String stem = "a".repeat(99) + "český";
        dictionary(members, stem, "Book", "m", false, false, "word", "meaning");
        assertEquals(1, read(archive(members), new ArrayList<>()));
    }

    @Test public void rejectsDanglingGnuLongFilenameAndCleansUp() throws Exception {
        ByteArrayOutputStream tar = new ByteArrayOutputStream();
        tarMember(tar, bytes("././@LongLink"), 'L', bytes("dictionary.dict.dz\0"));
        tar.write(new byte[1024]);
        assertThrows(IOException.class, () -> read(gzip(tar.toByteArray()), new ArrayList<>()));
        assertArrayEquals(new String[0], temporary.getRoot().list());
    }

    @Test public void importsDictionaryDataBeyond512MiBWithoutLoadingItIntoMemory() throws Exception {
        Map<String, byte[]> members = new LinkedHashMap<>();
        dictionary(members, "large", "Large reference dictionary", "m", false, false, "word", "meaning");
        int offset = 512 * 1024 * 1024;
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
            byte[] padding = new byte[8192];
            for (int written = 0; written < offset; written += padding.length) gzip.write(padding);
            gzip.write(bytes("meaning"));
        }
        members.put("large.dict.dz", compressed.toByteArray());
        // Point the entry at the end of the expanded dictionary, beyond the former limit.
        java.nio.ByteBuffer.wrap(members.get("large.idx")).putInt(5, offset);
        List<String> rows = new ArrayList<>();
        assertEquals(1, read(archive(members), rows));
        assertEquals("word|meaning|Test", rows.get(0));
        assertArrayEquals(new String[0], temporary.getRoot().list());
    }

    private int read(byte[] input, List<String> rows) throws IOException {
        return DictionaryImporter.read(new ByteArrayInputStream(input), temporary.getRoot(), "Test",
                (word, meaning, name) -> rows.add(word + "|" + meaning + "|" + name));
    }

    private static void dictionary(Map<String, byte[]> members, String stem, String book, String sequence,
                                   boolean compressedIndex, boolean wide, String word, String meaning) throws IOException {
        byte[] data = bytes(meaning);
        ByteArrayOutputStream index = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(index);
        output.write(bytes(word));
        output.writeByte(0);
        if (wide) output.writeLong(0); else output.writeInt(0);
        output.writeInt(data.length);
        members.put(stem + ".ifo", bytes("StarDict's dict ifo file\nversion=3.0.0\nbookname=" + book
                + "\nwordcount=1\nidxfilesize=" + index.size() + "\nidxoffsetbits=" + (wide ? 64 : 32)
                + "\nsametypesequence=" + sequence + "\n"));
        members.put(stem + (compressedIndex ? ".idx.gz" : ".idx"), compressedIndex ? gzip(index.toByteArray()) : index.toByteArray());
        members.put(stem + ".dict.dz", gzip(data));
    }

    private static byte[] archive(Map<String, byte[]> members) throws IOException {
        ByteArrayOutputStream tar = new ByteArrayOutputStream();
        for (Map.Entry<String, byte[]> member : members.entrySet()) {
            byte[] name = bytes(member.getKey());
            if (name.length > 100) {
                tarMember(tar, bytes("././@LongLink"), 'L', Arrays.copyOf(name, name.length + 1));
            }
            tarMember(tar, name, '0', member.getValue());
        }
        tar.write(new byte[1024]);
        return gzip(tar.toByteArray());
    }

    private static void tarMember(ByteArrayOutputStream tar, byte[] name, char type, byte[] data) throws IOException {
        byte[] header = new byte[512];
        System.arraycopy(name, 0, header, 0, Math.min(name.length, 100));
        byte[] size = bytes(String.format(Locale.ROOT, "%011o", data.length));
        System.arraycopy(size, 0, header, 124, size.length);
        header[156] = (byte) type;
        Arrays.fill(header, 148, 156, (byte) ' ');
        long sum = 0;
        for (byte b : header) sum += b & 255;
        byte[] checksum = bytes(String.format(Locale.ROOT, "%06o\0 ", sum));
        System.arraycopy(checksum, 0, header, 148, checksum.length);
        tar.write(header);
        tar.write(data);
        tar.write(new byte[(512 - data.length % 512) % 512]);
    }

    private static byte[] gzip(byte[] input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) { gzip.write(input); }
        return output.toByteArray();
    }

    private static byte[] bytes(String text) { return text.getBytes(StandardCharsets.UTF_8); }
}
