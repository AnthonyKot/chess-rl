#!/bin/bash

# Alternative: Google Cloud Compute Engine deployment
# For longer training runs (hours/days) without timeout limits

set -e

PROJECT_ID="your-project-id"  # Replace with your GCP project ID
INSTANCE_NAME="chess-rl-training"
ZONE="us-central1-a"
MACHINE_TYPE="e2-standard-4"  # 4 vCPUs, 16GB RAM

echo "🚀 Creating Compute Engine instance for Chess RL training..."

# Create instance
gcloud compute instances create $INSTANCE_NAME \
  --zone=$ZONE \
  --machine-type=$MACHINE_TYPE \
  --image-family=ubuntu-2004-lts \
  --image-project=ubuntu-os-cloud \
  --boot-disk-size=50GB \
  --boot-disk-type=pd-standard \
  --tags=chess-rl \
  --metadata-from-file startup-script=startup-script.sh

echo "⏳ Waiting for instance to be ready..."
sleep 60

# Copy project files
echo "📁 Uploading project files..."
gcloud compute scp --recurse . $INSTANCE_NAME:~/chess-rl --zone=$ZONE

# Start training
echo "🎯 Starting training on remote instance..."
gcloud compute ssh $INSTANCE_NAME --zone=$ZONE --command="
  cd ~/chess-rl && 
  chmod +x gradlew &&
  ./gradlew :integration:runCli --args='--train --config config/cloud-config.yaml --max-steps 200 --games-per-cycle 100'
"

echo "✅ Training started on Compute Engine!"
echo "🔗 SSH: gcloud compute ssh $INSTANCE_NAME --zone=$ZONE"
echo "🛑 Stop: gcloud compute instances delete $INSTANCE_NAME --zone=$ZONE"
