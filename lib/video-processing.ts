// Dependency-free video validation for admin-uploaded Story content.
// Deliberately supports only MP4 (the format every phone/editing tool
// exports by default) and only for story-type content blocks - banners stay
// image-only. Videos are NOT re-encoded or metadata-stripped here (unlike
// images): MP4 metadata atoms (moov/udta) require real container parsing to
// strip safely, which is out of scope for this dependency-free pipeline.
// This is a known, accepted limitation - do not assume video uploads are
// metadata-scrubbed the way image uploads are.

export const MAX_VIDEO_UPLOAD_BYTES = 50_000_000; // 50 MB

export function sniffVideoType(bytes: Uint8Array): "mp4" | null {
  // MP4/MOV-family containers: 4-byte box size, then ASCII "ftyp" at offset 4.
  if (
    bytes.length >= 12 &&
    bytes[4] === 0x66 && // f
    bytes[5] === 0x74 && // t
    bytes[6] === 0x79 && // y
    bytes[7] === 0x70 // p
  ) {
    return "mp4";
  }
  return null;
}

export const VIDEO_MIME_TYPES: Record<"mp4", string> = {
  mp4: "video/mp4",
};

export const VIDEO_EXTENSIONS: Record<"mp4", string> = {
  mp4: "mp4",
};
