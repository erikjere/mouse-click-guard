#!/usr/bin/env bash
# Opens, or updates, the runelite/plugin-hub pull request that points the hub at a commit of this plugin.
#
# Needs an authenticated gh CLI (gh auth login, or GH_TOKEN with the public_repo scope).
# Optional environment:
#   COMMIT      commit to publish (default: HEAD, which must already be pushed)
#   REPOSITORY  https clone URL of this plugin's repository (default: the gh repo of the current directory)
#   PR_BODY     file with the pull request description (default: docs/plugin-hub-pr.md for a new plugin)
set -euo pipefail

PLUGIN=mouse-click-guard
UPSTREAM=runelite/plugin-hub
ROOT=$(cd "$(dirname "$0")/.." && pwd)

prop() { sed -n "s/^$1=//p" "$ROOT/runelite-plugin.properties"; }
VERSION=$(prop version)
DISPLAY_NAME=$(prop displayName)
COMMIT=${COMMIT:-$(git -C "$ROOT" rev-parse HEAD)}
SELF=$(cd "$ROOT" && gh repo view --json nameWithOwner -q .nameWithOwner)
REPOSITORY=${REPOSITORY:-https://github.com/$SELF.git}

if ! gh api "repos/$SELF/commits/$COMMIT" --silent 2>/dev/null; then
  echo "Commit $COMMIT is not on GitHub yet; push it first." >&2
  exit 1
fi

OWNER=$(gh api user -q .login)
FORK=$OWNER/plugin-hub
if ! gh repo view "$FORK" --json name >/dev/null 2>&1; then
  gh repo fork "$UPSTREAM" --clone=false
  sleep 5
fi
gh repo sync "$FORK" --source "$UPSTREAM" --branch master

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
gh auth setup-git >/dev/null
gh repo clone "$FORK" "$WORK/plugin-hub" -- --depth 1 --quiet
cd "$WORK/plugin-hub"

NEW_PLUGIN=true
[ -f "plugins/$PLUGIN" ] && NEW_PLUGIN=false

BRANCH=$PLUGIN-$VERSION
git checkout -q -b "$BRANCH"
printf 'repository=%s\ncommit=%s\n' "$REPOSITORY" "$COMMIT" > "plugins/$PLUGIN"
if git diff --quiet && [ "$NEW_PLUGIN" = false ]; then
  echo "plugins/$PLUGIN already points at $COMMIT; nothing to do."
  exit 0
fi

if [ "$NEW_PLUGIN" = true ]; then
  TITLE="Add $DISPLAY_NAME"
  BODY_FILE=${PR_BODY:-$ROOT/docs/plugin-hub-pr.md}
else
  TITLE="Update $DISPLAY_NAME to v$VERSION"
  BODY_FILE=${PR_BODY:-$WORK/body.md}
  [ -f "$BODY_FILE" ] || printf 'Updates %s to v%s.\n\nGenerated-by: Claude Code\n' "$DISPLAY_NAME" "$VERSION" > "$BODY_FILE"
fi

git add "plugins/$PLUGIN"
git -c user.name="${GIT_AUTHOR_NAME:-$OWNER}" -c user.email="${GIT_AUTHOR_EMAIL:-$OWNER@users.noreply.github.com}" \
  commit -q -m "$TITLE"
# The branch is per version, so re-running for the same version updates the open PR, as the hub prefers.
git push -q --force origin "$BRANCH"

EXISTING=$(gh pr list --repo "$UPSTREAM" --head "$BRANCH" --author "$OWNER" --state open --json url -q '.[0].url')
if [ -n "$EXISTING" ]; then
  echo "Updated $EXISTING"
else
  gh pr create --repo "$UPSTREAM" --base master --head "$OWNER:$BRANCH" --title "$TITLE" --body-file "$BODY_FILE"
fi
