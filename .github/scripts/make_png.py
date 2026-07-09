#!/usr/bin/env python3
"""
Write a simple vertical-gradient PNG using only the standard library, so the
image note can be seeded without depending on ImageMagick (which isn't reliably
present on the CI runner). Usage: make_png.py <path> [width] [height]
"""
import struct
import sys
import zlib


def write_png(path, w=1000, h=700):
    c0, c1 = (14, 165, 233), (124, 58, 237)  # teal -> purple

    def lerp(a, b, t):
        return int(a + (b - a) * t)

    raw = bytearray()
    for y in range(h):
        t = y / (h - 1)
        px = bytes((lerp(c0[0], c1[0], t), lerp(c0[1], c1[1], t), lerp(c0[2], c1[2], t)))
        raw.append(0)              # filter type 0 for the scanline
        raw.extend(px * w)

    def chunk(typ, data):
        body = typ + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    ihdr = struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0)  # 8-bit RGB
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n")
        f.write(chunk(b"IHDR", ihdr))
        f.write(chunk(b"IDAT", zlib.compress(bytes(raw), 9)))
        f.write(chunk(b"IEND", b""))


if __name__ == "__main__":
    write_png(
        sys.argv[1],
        int(sys.argv[2]) if len(sys.argv) > 2 else 1000,
        int(sys.argv[3]) if len(sys.argv) > 3 else 700,
    )
