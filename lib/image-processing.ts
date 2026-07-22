// Dependency-free image validation and metadata stripping for admin-uploaded
// media. Deliberately supports only PNG and JPEG - the two formats real
// admin-exported banner/story images and phone-camera photos actually use.
// No SVG support: SVG is XML and can carry <script>, making it unsafe to
// accept as an "image" upload at all.

export type SniffedImageType = "png" | "jpeg";

export const MAX_UPLOAD_BYTES = 8_000_000; // 8 MB
export const MAX_IMAGE_DIMENSION = 4000; // px, per side

export function sniffImageType(bytes: Uint8Array): SniffedImageType | null {
  if (
    bytes.length >= 8 &&
    bytes[0] === 0x89 &&
    bytes[1] === 0x50 &&
    bytes[2] === 0x4e &&
    bytes[3] === 0x47 &&
    bytes[4] === 0x0d &&
    bytes[5] === 0x0a &&
    bytes[6] === 0x1a &&
    bytes[7] === 0x0a
  ) {
    return "png";
  }
  if (bytes.length >= 3 && bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff) {
    return "jpeg";
  }
  return null;
}

export function readImageDimensions(
  bytes: Uint8Array,
  type: SniffedImageType,
): { width: number; height: number } | null {
  return type === "png" ? readPngDimensions(bytes) : readJpegDimensions(bytes);
}

function readPngDimensions(bytes: Uint8Array): { width: number; height: number } | null {
  if (bytes.length < 24) return null;
  const type = String.fromCharCode(bytes[12], bytes[13], bytes[14], bytes[15]);
  if (type !== "IHDR") return null;
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  const width = view.getUint32(16, false);
  const height = view.getUint32(20, false);
  if (width <= 0 || height <= 0) return null;
  return { width, height };
}

function readJpegDimensions(bytes: Uint8Array): { width: number; height: number } | null {
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  let offset = 2; // past SOI (FF D8)
  while (offset + 4 <= bytes.length) {
    if (bytes[offset] !== 0xff) {
      offset++;
      continue;
    }
    let marker = bytes[offset + 1];
    offset += 2;
    while (marker === 0xff && offset < bytes.length) {
      marker = bytes[offset];
      offset++;
    }
    // Markers with no length field: SOI, EOI, RSTn, TEM.
    if (marker === 0xd8 || marker === 0xd9 || marker === 0x01 || (marker >= 0xd0 && marker <= 0xd7)) {
      if (marker === 0xd9) break;
      continue;
    }
    if (offset + 2 > bytes.length) break;
    const segmentLength = view.getUint16(offset, false);
    const isSof =
      (marker >= 0xc0 && marker <= 0xc3) ||
      (marker >= 0xc5 && marker <= 0xc7) ||
      (marker >= 0xc9 && marker <= 0xcb) ||
      (marker >= 0xcd && marker <= 0xcf);
    if (isSof) {
      if (offset + 7 > bytes.length) return null;
      const height = view.getUint16(offset + 3, false);
      const width = view.getUint16(offset + 5, false);
      if (width <= 0 || height <= 0) return null;
      return { width, height };
    }
    if (marker === 0xda) break; // SOS - scan data follows, no more markers to trust
    if (segmentLength < 2) break;
    offset += segmentLength;
  }
  return null;
}

const PNG_METADATA_CHUNKS_TO_STRIP = new Set(["tEXt", "zTXt", "iTXt", "tIME", "eXIf"]);

function stripPngMetadata(bytes: Uint8Array): Uint8Array {
  const out: number[] = Array.from(bytes.slice(0, 8)); // signature
  let offset = 8;
  while (offset + 8 <= bytes.length) {
    const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
    const length = view.getUint32(offset, false);
    const type = String.fromCharCode(
      bytes[offset + 4],
      bytes[offset + 5],
      bytes[offset + 6],
      bytes[offset + 7],
    );
    const chunkTotalLength = 12 + length; // length + type + data + crc
    if (offset + chunkTotalLength > bytes.length) break;
    if (!PNG_METADATA_CHUNKS_TO_STRIP.has(type)) {
      for (let i = 0; i < chunkTotalLength; i++) out.push(bytes[offset + i]);
    }
    offset += chunkTotalLength;
    if (type === "IEND") break;
  }
  return new Uint8Array(out);
}

// Strips APP1 (EXIF/XMP) and APP13 (Photoshop IPTC) segments. Keeps APP0
// (JFIF density) and APP2 (ICC color profile) intact since neither carries
// personal metadata and removing ICC can visibly shift color.
function stripJpegMetadata(bytes: Uint8Array): Uint8Array {
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  const out: number[] = [bytes[0], bytes[1]]; // SOI
  let offset = 2;
  while (offset + 4 <= bytes.length) {
    if (bytes[offset] !== 0xff) {
      out.push(bytes[offset]);
      offset++;
      continue;
    }
    const marker = bytes[offset + 1];
    if (marker === 0xd8 || marker === 0xd9 || marker === 0x01 || (marker >= 0xd0 && marker <= 0xd7)) {
      out.push(0xff, marker);
      offset += 2;
      continue;
    }
    if (offset + 4 > bytes.length) {
      out.push(bytes[offset]);
      offset++;
      continue;
    }
    const segmentLength = view.getUint16(offset + 2, false);
    const isMetadataSegment = marker === 0xe1 || marker === 0xed; // APP1, APP13
    if (!isMetadataSegment) {
      for (let i = 0; i < 2 + segmentLength; i++) out.push(bytes[offset + i]);
    }
    offset += 2 + segmentLength;
    if (marker === 0xda) {
      // Start of scan: copy the remainder (compressed data + EOI) verbatim.
      for (let i = offset; i < bytes.length; i++) out.push(bytes[i]);
      break;
    }
  }
  return new Uint8Array(out);
}

export function stripImageMetadata(bytes: Uint8Array, type: SniffedImageType): Uint8Array {
  return type === "png" ? stripPngMetadata(bytes) : stripJpegMetadata(bytes);
}

export const IMAGE_MIME_TYPES: Record<SniffedImageType, string> = {
  png: "image/png",
  jpeg: "image/jpeg",
};

export const IMAGE_EXTENSIONS: Record<SniffedImageType, string> = {
  png: "png",
  jpeg: "jpg",
};
