#!/bin/bash
# Usage: ./push_to_github.sh https://github.com/<USERNAME>/<REPO_NAME>.git

if [ -z "$1" ]; then
    echo "Usage: ./push_to_github.sh <GITHUB_REPO_URL>"
    echo "Example: ./push_to_github.sh https://github.com/myuser/nova-agent.git"
    exit 1
fi

REPO_URL=$1
echo "Setting remote origin to $REPO_URL..."
git remote remove origin 2>/dev/null
git remote add origin "$REPO_URL"
git branch -M main
echo "Pushing main branch to GitHub..."
git push -u origin main
echo "Successfully pushed Nova Agent to GitHub!"
