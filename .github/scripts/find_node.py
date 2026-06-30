#!/usr/bin/env python3
"""
Locate a node in a `uiautomator dump` hierarchy and print the centre x/y of its
bounds, so the screenshot driver can tap real on-screen elements instead of
guessing at hard-coded coordinates (which break across screen sizes/layouts).

Reads the dumped XML on stdin. Usage:

    find_node.py <attr> <value> [--contains] [--clickable] [--index N]

  <attr>   one of: text | desc | id | class
  <value>  the value to match (for `id`, a suffix like "appTitle" is enough)
  --contains   substring match instead of exact (ignored for `id`, which is
               always suffix/substring matched)
  --clickable  only consider nodes whose clickable="true"
  --index N    pick the Nth match (0-based, negatives count from the end)

Prints "<cx> <cy>" on success; exits non-zero (no output) when not found.
"""
import re
import sys
import xml.etree.ElementTree as ET

ATTR_MAP = {"text": "text", "desc": "content-desc", "id": "resource-id", "class": "class"}


def main() -> int:
    args = sys.argv[1:]
    contains = clickable = False
    index = 0
    positional = []
    i = 0
    while i < len(args):
        a = args[i]
        if a == "--contains":
            contains = True
        elif a == "--clickable":
            clickable = True
        elif a == "--index":
            i += 1
            index = int(args[i])
        else:
            positional.append(a)
        i += 1

    if len(positional) < 2:
        sys.stderr.write("usage: find_node.py <attr> <value> [opts]\n")
        return 2
    attr_key, value = positional[0], positional[1]
    xml_attr = ATTR_MAP.get(attr_key, attr_key)

    data = sys.stdin.read()
    try:
        root = ET.fromstring(data)
    except ET.ParseError:
        # Loose-escape stray ampersands and retry once.
        data = re.sub(r"&(?!amp;|lt;|gt;|quot;|apos;|#)", "&amp;", data)
        root = ET.fromstring(data)

    matches, exact = [], []
    for node in root.iter("node"):
        av = node.get(xml_attr, "")
        if xml_attr == "resource-id":
            is_exact = av == value or av.endswith("/" + value)
            ok = is_exact or value in av
        elif contains:
            is_exact = av == value
            ok = value in av
        else:
            is_exact = ok = av == value
        if ok and (not clickable or node.get("clickable") == "true"):
            matches.append(node)
            if is_exact:
                exact.append(node)
    # Prefer exact id/text matches (e.g. id "alignment" over "alignmentLeft").
    if exact:
        matches = exact

    if not matches:
        return 1
    if index < 0:
        index += len(matches)
    if index < 0 or index >= len(matches):
        return 1

    m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", matches[index].get("bounds", ""))
    if not m:
        return 1
    x1, y1, x2, y2 = map(int, m.groups())
    print((x1 + x2) // 2, (y1 + y2) // 2)
    return 0


if __name__ == "__main__":
    sys.exit(main())
