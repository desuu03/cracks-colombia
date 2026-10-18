package co.uniquindio.crackscolombia.cracks_colombia_api.video.service;

import co.uniquindio.crackscolombia.cracks_colombia_api.shared.exception.ApiException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Reads ISO Base Media File Format metadata without trusting the uploaded MIME type. */
@Component
public class Mp4MetadataReader {

    private static final int MAX_BOXES = 20_000;
    private static final Set<String> ALLOWED_BRANDS = Set.of(
            "mp41", "mp42", "avc1", "hvc1", "hev1", "M4V ", "MSNV", "dash");

    public double readDurationSeconds(Path file) throws IOException {
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            long fileSize = channel.size();
            List<Box> topLevelBoxes = readBoxes(channel, 0, fileSize);
            Box fileType = uniqueBox(topLevelBoxes, "ftyp");
            Box movie = uniqueBox(topLevelBoxes, "moov");
            boolean hasMediaData = topLevelBoxes.stream()
                    .anyMatch(box -> box.type().equals("mdat") && box.contentLength() > 0);
            if (fileType == null || movie == null || !hasMediaData) {
                throw invalidFormat();
            }
            validateFileType(channel, fileType);

            Box movieHeader = null;
            boolean hasVideoTrack = false;
            for (Box child : readBoxes(channel, movie.contentOffset(), movie.endOffset())) {
                if (child.type().equals("mvhd")) {
                    if (movieHeader != null) {
                        throw invalidFormat();
                    }
                    movieHeader = child;
                } else if (child.type().equals("trak") && hasPlayableVideoTrack(channel, child)) {
                    hasVideoTrack = true;
                }
            }
            if (movieHeader == null || !hasVideoTrack) {
                throw invalidFormat();
            }
            return readMovieDuration(channel, movieHeader);
        }
    }

    private void validateFileType(FileChannel channel, Box fileType) throws IOException {
        long contentLength = fileType.contentLength();
        if (contentLength < 8 || contentLength > 4096 || (contentLength - 8) % 4 != 0) {
            throw invalidFormat();
        }
        byte[] bytes = readBytes(channel, fileType.contentOffset(), (int) contentLength);
        String majorBrand = ascii(bytes, 0, 4);
        boolean supportedBrand = isAllowedBrand(majorBrand);
        for (int offset = 8; !supportedBrand && offset + 4 <= bytes.length; offset += 4) {
            supportedBrand = isAllowedBrand(ascii(bytes, offset, 4));
        }
        if (!supportedBrand) {
            throw invalidFormat();
        }
    }

    private boolean isAllowedBrand(String brand) {
        return brand.equals("isom") || brand.matches("iso[2-9]") || ALLOWED_BRANDS.contains(brand);
    }

    private boolean hasPlayableVideoTrack(FileChannel channel, Box track) throws IOException {
        boolean videoHandler = false;
        Box mediaInformation = null;
        for (Box child : readBoxes(channel, track.contentOffset(), track.endOffset())) {
            if (!child.type().equals("mdia")) {
                continue;
            }
            for (Box mediaChild : readBoxes(channel, child.contentOffset(), child.endOffset())) {
                if (mediaChild.type().equals("hdlr") && mediaChild.contentLength() >= 12) {
                    byte[] handler = readBytes(channel, mediaChild.contentOffset() + 8, 4);
                    videoHandler = ascii(handler, 0, 4).equals("vide");
                } else if (mediaChild.type().equals("minf")) {
                    mediaInformation = mediaChild;
                }
            }
        }
        return videoHandler && mediaInformation != null && hasSamples(channel, mediaInformation);
    }

    private boolean hasSamples(FileChannel channel, Box mediaInformation) throws IOException {
        for (Box child : readBoxes(channel, mediaInformation.contentOffset(), mediaInformation.endOffset())) {
            if (!child.type().equals("stbl")) {
                continue;
            }
            boolean hasSampleDescription = false;
            boolean hasSampleEntry = false;
            boolean hasSampleSizes = false;
            for (Box sampleTable : readBoxes(channel, child.contentOffset(), child.endOffset())) {
                if (sampleTable.type().equals("stsd") && sampleTable.contentLength() >= 8) {
                    byte[] header = readBytes(channel, sampleTable.contentOffset(), 8);
                    long entryCount = Integer.toUnsignedLong(
                            ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN).getInt(4));
                    List<Box> entries = readBoxes(channel, sampleTable.contentOffset() + 8,
                            sampleTable.endOffset());
                    hasSampleDescription = entryCount > 0 && entries.size() == entryCount;
                    hasSampleEntry = !entries.isEmpty();
                } else if ((sampleTable.type().equals("stsz") || sampleTable.type().equals("stz2"))
                        && sampleTable.contentLength() >= 12) {
                    byte[] header = readBytes(channel, sampleTable.contentOffset(), 12);
                    long sampleCount = Integer.toUnsignedLong(
                            ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN).getInt(8));
                    hasSampleSizes = sampleCount > 0;
                }
            }
            if (hasSampleDescription && hasSampleEntry && hasSampleSizes) {
                return true;
            }
        }
        return false;
    }

    private double readMovieDuration(FileChannel channel, Box movieHeader) throws IOException {
        if (movieHeader.contentLength() < 20) {
            throw invalidFormat();
        }
        byte[] prefix = readBytes(channel, movieHeader.contentOffset(),
                (int) Math.min(movieHeader.contentLength(), 32));
        ByteBuffer buffer = ByteBuffer.wrap(prefix).order(ByteOrder.BIG_ENDIAN);
        int version = Byte.toUnsignedInt(buffer.get(0));
        long timescale;
        long duration;
        if (version == 0 && prefix.length >= 20) {
            timescale = Integer.toUnsignedLong(buffer.getInt(12));
            duration = Integer.toUnsignedLong(buffer.getInt(16));
        } else if (version == 1 && prefix.length >= 32) {
            timescale = Integer.toUnsignedLong(buffer.getInt(20));
            duration = buffer.getLong(24);
            if (duration < 0) {
                throw invalidFormat();
            }
        } else {
            throw invalidFormat();
        }
        if (timescale == 0 || duration == 0 || duration == 0xffff_ffffL) {
            throw invalidFormat();
        }
        return (double) duration / timescale;
    }

    private List<Box> readBoxes(FileChannel channel, long start, long end) throws IOException {
        if (start < 0 || end < start || end > channel.size()) {
            throw invalidFormat();
        }
        List<Box> boxes = new ArrayList<>();
        long position = start;
        while (position < end) {
            if (boxes.size() >= MAX_BOXES) {
                throw invalidFormat();
            }
            if (end - position < 8) {
                throw invalidFormat();
            }
            ByteBuffer header = ByteBuffer.wrap(readBytes(channel, position, 8)).order(ByteOrder.BIG_ENDIAN);
            long size = Integer.toUnsignedLong(header.getInt());
            byte[] typeBytes = new byte[4];
            header.get(typeBytes);
            String type = new String(typeBytes, StandardCharsets.ISO_8859_1);
            int headerSize = 8;
            if (size == 1) {
                if (end - position < 16) {
                    throw invalidFormat();
                }
                ByteBuffer extended = ByteBuffer.wrap(readBytes(channel, position + 8, 8))
                        .order(ByteOrder.BIG_ENDIAN);
                size = extended.getLong();
                headerSize = 16;
                if (size < 0) {
                    throw invalidFormat();
                }
            } else if (size == 0) {
                size = end - position;
            }
            if (size < headerSize || size > end - position) {
                throw invalidFormat();
            }
            boxes.add(new Box(position, size, headerSize, type));
            position += size;
        }
        return boxes;
    }

    private Box uniqueBox(List<Box> boxes, String type) {
        Box result = null;
        for (Box box : boxes) {
            if (box.type().equals(type)) {
                if (result != null) {
                    throw invalidFormat();
                }
                result = box;
            }
        }
        return result;
    }

    private byte[] readBytes(FileChannel channel, long position, int length) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(length);
        while (buffer.hasRemaining()) {
            int read = channel.read(buffer, position + buffer.position());
            if (read <= 0) {
                throw invalidFormat();
            }
        }
        return buffer.array();
    }

    private String ascii(byte[] bytes, int offset, int length) {
        return new String(bytes, offset, length, StandardCharsets.ISO_8859_1);
    }

    private ApiException invalidFormat() {
        return ApiException.invalidVideo(
                "El archivo debe ser un MP4 valido con una pista de video.");
    }

    private record Box(long offset, long size, int headerSize, String type) {
        long contentOffset() {
            return offset + headerSize;
        }

        long endOffset() {
            return offset + size;
        }

        long contentLength() {
            return size - headerSize;
        }
    }
}
