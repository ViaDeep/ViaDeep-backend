package pe.viadeep.evidence.domain;

import java.util.Arrays;
import java.util.Optional;

/** Formatos de fotografía aceptados como evidencia (OB2: solo JPEG o PNG). */
public enum ImageFormat {
    JPEG("image/jpeg", "jpg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
    PNG("image/png", "png", new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});

    private final String contentType;
    private final String extension;
    private final byte[] signature;

    ImageFormat(String contentType, String extension, byte[] signature) {
        this.contentType = contentType;
        this.extension = extension;
        this.signature = signature;
    }

    public String contentType() {
        return contentType;
    }

    public String extension() {
        return extension;
    }

    public static ImageFormat fromContentType(String contentType) {
        return Arrays.stream(values())
                .filter(format -> format.contentType.equals(contentType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tipo de contenido desconocido: " + contentType));
    }

    /** Detecta el formato por los primeros bytes del archivo, sin confiar en lo que declara el cliente. */
    public static Optional<ImageFormat> detect(byte[] content) {
        return Arrays.stream(values()).filter(format -> format.matches(content)).findFirst();
    }

    private boolean matches(byte[] content) {
        return content.length >= signature.length
                && Arrays.equals(Arrays.copyOf(content, signature.length), signature);
    }
}
