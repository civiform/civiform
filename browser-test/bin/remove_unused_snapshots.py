#!/usr/bin/env python3
import os
import re

# Paths
snapshots_dir = "../image_snapshots"
src_dir = "../src"

# Get all .png filenames (without extension)
filenames = []
for root, _, files in os.walk(snapshots_dir):
    for f in files:
        if f.lower().endswith(".png"):
            name = os.path.splitext(f)[0]
            # Remove '-mobile' or '-medium' suffix if present
            if name.endswith('-mobile'):
                name = name[:-7]
            elif name.endswith('-medium'):
                name = name[:-7]
            filenames.append(name)

unusedSnapshots = []

# Search for each filename in src_dir
for name in filenames:
    found = False
    # Walk through all files in src_dir
    for root, _, files in os.walk(src_dir):
        for file in files:
            file_path = os.path.join(root, file)
            try:
                with open(file_path, "r", encoding="utf-8",
                          errors="ignore") as f:
                    content = f.read()
                    if re.search(rf"\b{name}\b", content):
                        found = True
                        break
            except Exception:
                continue
    if not found:
        unusedSnapshots.append(name)

# Delete unused snapshot files
deleted_files = []
for root, _, files in os.walk(snapshots_dir):
    for f in files:
        name, ext = os.path.splitext(f)
        if ext.lower() == ".png" and name in unusedSnapshots:
            file_path = os.path.join(root, f)
            try:
                os.remove(file_path)
                deleted_files.append(file_path)
            except Exception as e:
                print(f"Failed to delete {file_path}: {e}")
if deleted_files:
    print("\nDeleted unused snapshot files:")
    for file_path in deleted_files:
        print(file_path)
else:
    print("\nNo unused snapshot files deleted.")
