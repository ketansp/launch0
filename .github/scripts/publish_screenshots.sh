#!/usr/bin/env bash
#
# Publishes the screenshots captured by ui_screenshots.sh so they render
# *inline on the workflow run page* — no artifact download needed.
#
# GitHub's job-summary renderer strips base64 `data:` images, so inline images
# need a real https URL. This pushes the PNGs to an orphan `ci-screenshots`
# branch (kept out of the main history) under runs/<run_id>/, then writes the
# job summary referencing their raw.githubusercontent.com URLs.
#
# Requires: GH_TOKEN env (the workflow's GITHUB_TOKEN) and `contents: write`.
set -euo pipefail

REPO="${GITHUB_REPOSITORY:?}"
RUN_ID="${GITHUB_RUN_ID:?}"
SHA="${GITHUB_SHA:0:7}"
BRANCH="ci-screenshots"
DEST="runs/${RUN_ID}"
OUT_DIR="${OUT_DIR:-screenshots}"
MANIFEST="$OUT_DIR/manifest.tsv"

if [[ ! -s "$MANIFEST" ]]; then
  echo "No screenshots manifest at $MANIFEST — nothing to publish." >&2
  exit 0
fi

TMP="$(mktemp -d)"
REMOTE="https://x-access-token:${GH_TOKEN}@github.com/${REPO}.git"

# Check out the existing screenshots branch, or start a fresh orphan one.
if git clone --quiet --depth 1 --branch "$BRANCH" "$REMOTE" "$TMP" 2>/dev/null; then
  echo "Reusing existing $BRANCH branch."
else
  echo "Creating new orphan $BRANCH branch."
  git clone --quiet --depth 1 "$REMOTE" "$TMP"
  git -C "$TMP" checkout --orphan "$BRANCH"
  git -C "$TMP" rm -rqf . >/dev/null 2>&1 || true
fi

mkdir -p "$TMP/$DEST"
cp "$OUT_DIR"/*.png "$TMP/$DEST/"

git -C "$TMP" config user.name "github-actions[bot]"
git -C "$TMP" config user.email "41898282+github-actions[bot]@users.noreply.github.com"
git -C "$TMP" add "$DEST"
git -C "$TMP" commit -q -m "UI screenshots for run ${RUN_ID} (${SHA})"

for i in 1 2 3 4; do
  git -C "$TMP" push -q origin "$BRANCH" && break || { echo "push retry $i"; sleep $((2**i)); }
done

# ---- job summary -------------------------------------------------------------
RAW="https://raw.githubusercontent.com/${REPO}/${BRANCH}/${DEST}"
{
  echo "## Launch0 UI walkthrough"
  echo ""
  echo "Captured on an Android emulator from the debug APK built in this run (commit \`${SHA}\`)."
  echo ""
  while IFS=$'\t' read -r file caption; do
    [[ -n "$file" ]] || continue
    echo "### ${caption}"
    echo ""
    echo "<img alt=\"${caption}\" width=\"300\" src=\"${RAW}/${file}\" />"
    echo ""
  done < "$MANIFEST"
} >> "${GITHUB_STEP_SUMMARY:-/dev/stdout}"

echo "Published ${DEST} to $BRANCH and wrote the job summary."
