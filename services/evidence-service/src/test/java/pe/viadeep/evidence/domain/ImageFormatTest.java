package pe.viadeep.evidence.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ImageFormatTest {

    @Test
    void detectsJpegAndPngBySignature() {
        assertThat(ImageFormat.detect(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00})).contains(ImageFormat.JPEG);
        assertThat(ImageFormat.detect(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0})).contains(ImageFormat.PNG);
    }

    @Test
    void rejectsOtherFilesEvenIfShort() {
        assertThat(ImageFormat.detect("GIF89a".getBytes())).isEmpty();
        assertThat(ImageFormat.detect(new byte[] {(byte) 0xFF})).isEmpty();
        assertThat(ImageFormat.detect(new byte[0])).isEmpty();
    }
}
