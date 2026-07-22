#!/usr/bin/env python3
"""Analyze an Ebean SQL debug log to find redundant DB access per HTTP request.

Attribution is positional: io.ebean.SQL lines that appear before a
`loggingfilter - METHOD\tPATH\t...` line are attributed to that request.
(No thread id is present in the log, so interleaving of truly-concurrent
requests can't be disentangled; a browser-test run is mostly sequential.)
"""
import re
import sys
from collections import Counter, defaultdict

ANSI = re.compile(r"\x1b\[[0-9;]*m")

# Requests we don't care about (static assets, fonts, images, favicon...)
IGNORE_PATH = re.compile(
    r"^/assets/|\.(js|css|map|svg|png|jpe?g|gif|ico|woff2?|ttf|webp)(\?|$)",
    re.IGNORECASE,
)

req_re = re.compile(r"loggingfilter - (\S+)\t(\S+)\t(\d+)ms\t(\d+)")
sql_re = re.compile(r"io\.ebean\.SQL - (?:txn\[(\d+)\] )?(.*)")


def normalize(sql):
    """Strip comments, bind values, micros, and collapse literals so that the
    same statement shape maps to one key."""
    s = sql
    s = re.sub(r"--\s*bind\(.*?\)\s*", "", s)      # trailing --bind(...)
    s = re.sub(r"-{1,2}\s*bind\(.*?\)\s*", "", s)  # -- bind(...) variants
    s = re.sub(r"--micros\([\d,]+\)", "", s)
    s = re.sub(r"/\*.*?\*/", "", s)                # /* Model.method */ comment
    s = re.sub(r"\s+", " ", s).strip().rstrip(";")
    return s


def label(sql):
    """Human label: keep the /* comment */ tag if present, plus op + table."""
    m = re.search(r"/\*\s*(.*?)\s*\*/", sql)
    tag = m.group(1) if m else ""
    n = normalize(sql)
    op = n.split(" ", 1)[0].lower()
    tbl = ""
    mt = re.search(r"\bfrom\s+(\w+)", n) or re.search(r"\binto\s+(\w+)", n) or \
        re.search(r"\bupdate\s+(\w+)", n)
    if mt:
        tbl = mt.group(1)
    core = f"{op} {tbl}".strip()
    return f"{core}  [{tag}]" if tag else core


def main(path):
    with open(path, encoding="utf-8", errors="replace") as f:
        lines = [ANSI.sub("", ln).rstrip("\r\n") for ln in f]

    pending = []   # list of (raw_sql, txn) since last request boundary
    requests = []  # (method, path, ms, status, sqls)
    for ln in lines:
        ms = sql_re.search(ln)
        if ms:
            txn, body = ms.group(1), ms.group(2)
            pending.append((body, txn))
            continue
        mr = req_re.search(ln)
        if mr:
            method, pathp, dur, status = mr.groups()
            requests.append((method, pathp, int(dur), status, pending))
            pending = []

    # Aggregate redundancy across all interesting requests.
    interesting = [r for r in requests
                   if not IGNORE_PATH.search(r[1]) and r[4]]

    print(f"Total requests logged:        {len(requests)}")
    print(f"Interesting (non-asset) reqs: {len(interesting)}")
    total_sql = sum(len(r[4]) for r in interesting)
    print(f"SQL statements in those reqs: {total_sql}\n")

    # Per-request redundancy: count exact-duplicate normalized statements.
    per_req = []
    for method, pathp, dur, status, sqls in interesting:
        norm_counts = Counter(normalize(s) for s, _ in sqls)
        # exact same statement + same bind = truly redundant
        exact_counts = Counter((normalize(s), s) for s, _ in sqls)
        redundant = sum(c - 1 for c in norm_counts.values() if c > 1)
        per_req.append((redundant, len(sqls), method, pathp, dur, status,
                        norm_counts, exact_counts, sqls))

    per_req.sort(key=lambda x: (-x[0], -x[1]))

    print("=" * 100)
    print("TOP REQUESTS BY REDUNDANT (repeated) STATEMENT COUNT")
    print("=" * 100)
    for (redundant, nsql, method, pathp, dur, status,
         norm_counts, exact_counts, sqls) in per_req[:25]:
        if redundant == 0:
            continue
        print(f"\n{method} {pathp}  ({nsql} queries, {redundant} redundant, "
              f"{dur}ms, {status})")
        # show statements repeated within this request
        rows = [(c, stmt) for stmt, c in norm_counts.items() if c > 1]
        rows.sort(reverse=True)
        for c, stmt in rows[:12]:
            # how many of those repeats are byte-identical binds?
            same_bind = max(
                (ec for (n, _), ec in exact_counts.items() if n == stmt),
                default=0)
            kind = "identical bind" if same_bind > 1 else "diff binds (N+1?)"
            short = (stmt[:110] + "…") if len(stmt) > 110 else stmt
            print(f"    {c:3d}x  ({kind})  {short}")

    # Global view: which statement shapes are most repeated-within-a-request.
    print("\n" + "=" * 100)
    print("STATEMENT SHAPES MOST OFTEN REPEATED WITHIN A SINGLE REQUEST")
    print("(summed extra executions beyond the first, across all requests)")
    print("=" * 100)
    global_redundant = Counter()
    for (_, _, _, _, _, _, norm_counts, _, _) in per_req:
        for stmt, c in norm_counts.items():
            if c > 1:
                global_redundant[stmt] += c - 1
    for stmt, extra in global_redundant.most_common(20):
        short = (stmt[:120] + "…") if len(stmt) > 120 else stmt
        print(f"  +{extra:4d}  {short}")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else
         "/Users/shane/src/civiform/db_trace.txt")
