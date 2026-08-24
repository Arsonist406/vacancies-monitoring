#!/usr/bin/env bash
# PreToolUse hook: blocks git commands that modify/create/alter
# repository history, branches, or state. Read-only commands
# (status, log, diff, show, blame, fetch, etc.) pass through.
set -euo pipefail

input=$(cat)

if command -v jq >/dev/null 2>&1; then
  cmd=$(printf '%s' "$input" | jq -r '.tool_input.command // empty')
else
  cmd=$(printf '%s' "$input" | sed -n 's/.*"command"[[:space:]]*:[[:space:]]*"\(.*\)".*/\1/p')
fi

[ -z "$cmd" ] && exit 0

MUTATING_RE='^(commit|push|pull|merge|rebase|reset|checkout|switch|restore|clean|revert|apply|am|cherry-pick|stash[[:space:]]+(pop|apply|drop|clear)|branch[[:space:]]+-[dDmM]|tag[[:space:]]+-[ad]|add|rm|mv|init|clone|update-ref|symbolic-ref|gc|reflog[[:space:]]+expire|worktree[[:space:]]+(add|remove)|submodule[[:space:]]+(add|update)|filter-branch|filter-repo|config[[:space:]]+(--global|--unset|--replace))([[:space:]]|$)'

IFS='&|;' read -ra parts <<< "$cmd"
for part in "${parts[@]}"; do
  trimmed="$(echo "$part" | sed 's/^[[:space:]]*//')"
  if echo "$trimmed" | grep -qE '(^|[[:space:]])git[[:space:]]'; then
   subcmd=$(echo "$trimmed" \
     | sed -E 's/.*(^|[[:space:]])git[[:space:]]+//' \
     | sed -E 's/^(-[^[:space:]]+[[:space:]]+)*//')
   if echo "$subcmd" | grep -qE "$MUTATING_RE"; then
      echo "Blocked by block-git-mutations.sh: '$trimmed' alters repo history/branches/state. Not permitted (see CLAUDE.md)." >&2
      exit 2
    fi
  fi
done

exit 0
