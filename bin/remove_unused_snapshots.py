#!/usr/bin/env python3
import os
import re
import subprocess

try:
    repo_root = subprocess.check_output(
        ["git", "rev-parse", "--show-toplevel"], text=True).strip()
    os.chdir(repo_root)
except Exception:
    print("Error: Could not change to git repository root")
    exit(1)

# Paths
snapshots_dir = "./browser-test/image_snapshots"
src_dir = "./browser-test/src"

# Get all .png filenames (without extension)
filenames = []
for root, _, files in os.walk(snapshots_dir):
    # Skip any files in the question_lifecycle_test
    # directories because their filenames
    # use a replacement value and that makes it hard to find uses
    # of those filenames in the test files.
    if ("question_lifecycle_test" in root):
        continue
    for f in files:
        if f.lower().endswith(".png"):
            name = os.path.splitext(f)[0]
            # Remove '-mobile' or '-medium' suffix if present
            if name.endswith('-mobile'):
                continue
            elif name.endswith('-medium'):
                continue
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
                    if re.search(rf'\b{name}\b', content):
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
        name = name.replace('-mobile', '').replace('-medium', '')
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
