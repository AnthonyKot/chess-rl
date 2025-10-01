#!/bin/bash

# Google Cloud deployment script for Chess RL training
# Prerequisites: gcloud CLI installed and authenticated

set -e

PROJECT_ID="your-project-id"  # Replace with your GCP project ID
SERVICE_NAME="chess-rl-training"
REGION="us-central1"
IMAGE_NAME="gcr.io/$PROJECT_ID/$SERVICE_NAME"

echo "🚀 Deploying Chess RL to Google Cloud Run..."

# Build and push Docker image
echo "📦 Building Docker image..."
docker build -t $IMAGE_NAME .

echo "☁️ Pushing to Google Container Registry..."
docker push $IMAGE_NAME

# Deploy to Cloud Run
echo "🚀 Deploying to Cloud Run..."
gcloud run deploy $SERVICE_NAME \
  --image $IMAGE_NAME \
  --platform managed \
  --region $REGION \
  --memory 4Gi \
  --cpu 2 \
  --timeout 900 \
  --max-instances 1 \
  --set-env-vars "JAVA_OPTS=-Xmx3g -XX:+UseG1GC" \
  --args="--train,--config,config/cloud-config.yaml,--max-steps,200,--games-per-cycle,100,--max-concurrent-games,8"

echo "✅ Deployment complete!"
echo "🔗 View logs: gcloud run logs tail $SERVICE_NAME --region $REGION"
echo "🛑 Stop service: gcloud run services delete $SERVICE_NAME --region $REGION"
