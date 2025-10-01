#!/bin/bash

# Startup script for Compute Engine instance
# Installs Java 21 and Docker

apt-get update
apt-get install -y openjdk-21-jdk docker.io

# Enable Docker for current user
usermod -aG docker $USER

# Set Java environment
echo 'export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc

# Install gcloud CLI (optional, for GCS integration)
curl https://sdk.cloud.google.com | bash
exec -l $SHELL

echo "✅ Compute Engine instance ready for Chess RL training!"
