#!/usr/bin/env python3
import html
import json
import re
import sys
import xml.etree.ElementTree as ET


def parse_style(style: str) -> dict:
    out = {}
    if not style:
        return out
    for part in style.split(";"):
        if not part or "=" not in part:
            continue
        k, v = part.split("=", 1)
        out[k] = v
    return out


def clean_text(value: str) -> str:
    if not value:
        return ""
    value = re.sub(r"<br\s*/?>", "\n", value, flags=re.I)
    value = re.sub(r"<[^>]+>", "", value)
    value = html.unescape(value)
    return value.replace("\r", "").strip()


def main() -> None:
    if len(sys.argv) != 3:
        raise SystemExit("usage: parse_drawio.py input.xml output.json")
    src, dst = sys.argv[1], sys.argv[2]
    root = ET.parse(src).getroot()
    diagrams = []
    for diag in root.findall("diagram"):
        model = diag.find("mxGraphModel")
        cells = []
        for cell in model.findall(".//mxCell"):
            geo = cell.find("mxGeometry")
            g = {}
            if geo is not None:
                for k in ("x", "y", "width", "height"):
                    if k in geo.attrib:
                        g[k] = float(geo.attrib[k])
            cells.append(
                {
                    "id": cell.attrib.get("id"),
                    "vertex": cell.attrib.get("vertex") == "1",
                    "edge": cell.attrib.get("edge") == "1",
                    "parent": cell.attrib.get("parent"),
                    "source": cell.attrib.get("source"),
                    "target": cell.attrib.get("target"),
                    "style": cell.attrib.get("style", ""),
                    "styleMap": parse_style(cell.attrib.get("style", "")),
                    "value": cell.attrib.get("value", ""),
                    "text": clean_text(cell.attrib.get("value", "")),
                    "geometry": g,
                }
            )
        diagrams.append(
            {
                "id": diag.attrib.get("id"),
                "name": diag.attrib.get("name", ""),
                "background": model.attrib.get("background", "#ffffff"),
                "pageWidth": float(model.attrib.get("pageWidth", 1920)),
                "pageHeight": float(model.attrib.get("pageHeight", 1080)),
                "cells": cells,
            }
        )
    with open(dst, "w", encoding="utf-8") as f:
        json.dump(diagrams, f, ensure_ascii=False)


if __name__ == "__main__":
    main()
