#!/bin/bash

# Configuration
REMOTE_USER="nvidia"
REMOTE_HOST="10.9.71.101"
REMOTE_REPO_PATH="/home/nvidia/Documents/photonvision"
REMOTE_NAME="orin"
BRANCH="main"

# Add the remote (if it doesn't exist)
if ! git remote get-url "$REMOTE_NAME" &> /dev/null; then
    echo "Adding remote '$REMOTE_NAME'..."
    git remote add "$REMOTE_NAME" "ssh://${REMOTE_USER}@${REMOTE_HOST}/${REMOTE_REPO_PATH}"
else
    echo "Remote '$REMOTE_NAME' already exists."
fi

# Push the branch to the remote
echo "Pushing branch '$BRANCH' to '$REMOTE_NAME'..."
git push "$REMOTE_NAME" "$BRANCH"

echo "✅ Deployment to $REMOTE_HOST complete."

