#!/usr/bin/env bash
#
# Publishes the screenshots captured by ui_screenshots.sh so they render
# *inline* — in the PR thread and in the run's job summary — with no download.
#
# GitHub renders images only from a URL (base64 data: images are stripped from
# both summaries and comments, and there's no API to upload to user-attachments).
# So the PNGs are pushed to an orphan `ci-screenshots` branch under runs/<run_id>/
# and referenced via their raw.githubusercontent.com URLs.
#
# Posts/updates a single sticky PR comment (matched by a hidden marker) when run
# on a pull request; always writes the job summary too.
#
# Requires: GH_TOKEN (the workflow GITHUB_TOKEN), `contents: write` and, for the
# comment, `pull-requests: write`. PR_NUMBER is set only on pull_request runs.
set -euo pipefail

REPO="${GITHUB_REPOSITORY:?}"
RUN_ID="${GITHUB_RUN_ID:?}"
SHA="${GITHUB_SHA:0:7}"
BRANCH="ci-screenshots"
DEST="runs/${RUN_ID}"
OUT_DIR="${OUT_DIR:-screenshots}"
MANIFEST="$OUT_DIR/manifest.tsv"
MARKER="<!-- launch0-ui-screenshots -->"
API="https://api.github.com"

if [[ ! -s "$MANIFEST" ]]; then
  echo "No screenshots manifest at $MANIFEST — nothing to publish." >&2
  exit 0
fi

# ---- push PNGs to the ci-screenshots branch ----------------------------------
TMP="$(mktemp -d)"
REMOTE="https://x-access-token:${GH_TOKEN}@github.com/${REPO}.git"

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

# ---- build the markdown body -------------------------------------------------
RAW="https://raw.githubusercontent.com/${REPO}/${BRANCH}/${DEST}"
RUN_URL="${GITHUB_SERVER_URL:-https://github.com}/${REPO}/actions/runs/${RUN_ID}"
BODY="$(mktemp)"
{
  echo "$MARKER"
  echo "## 📱 Launch0 UI walkthrough"
  echo ""
  echo "Screens captured on an Android emulator from the debug APK built for commit \`${SHA}\` ([run](${RUN_URL}))."
  echo ""
  while IFS=$'\t' read -r file caption; do
    [[ -n "$file" ]] || continue
    echo "### ${caption}"
    echo ""
    echo "<img alt=\"${caption}\" width=\"300\" src=\"${RAW}/${file}\" />"
    echo ""
  done < "$MANIFEST"
} > "$BODY"

# ---- job summary -------------------------------------------------------------
cat "$BODY" >> "${GITHUB_STEP_SUMMARY:-/dev/stdout}"

# ---- sticky PR comment -------------------------------------------------------
if [[ -z "${PR_NUMBER:-}" ]]; then
  echo "No PR_NUMBER (not a pull_request run) — skipping PR comment."
  exit 0
fi

auth=(-H "Authorization: Bearer ${GH_TOKEN}" -H "Accept: application/vnd.github+json")
payload="$(jq -Rs '{body: .}' < "$BODY")"

# Find an existing sticky comment by its marker.
existing_id="$(curl -fsSL "${auth[@]}" \
  "${API}/repos/${REPO}/issues/${PR_NUMBER}/comments?per_page=100" \
  | jq -r --arg m "$MARKER" 'map(select(.body|contains($m))) | (.[0].id // empty)')"

if [[ -n "$existing_id" ]]; then
  echo "Updating existing PR comment $existing_id."
  curl -fsSL -X PATCH "${auth[@]}" \
    "${API}/repos/${REPO}/issues/comments/${existing_id}" \
    -d "$payload" >/dev/null
else
  echo "Creating new PR comment."
  curl -fsSL -X POST "${auth[@]}" \
    "${API}/repos/${REPO}/issues/${PR_NUMBER}/comments" \
    -d "$payload" >/dev/null
fi

echo "Published ${DEST} to $BRANCH; updated job summary and PR #${PR_NUMBER} comment."
