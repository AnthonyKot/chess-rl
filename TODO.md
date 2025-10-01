# Chess RL Cloud Deployment TODO

## 📋 Step-by-Step Google Cloud Deployment Guide

### 🔧 **Setup Phase (15 minutes)**

- [ ] **Install Google Cloud CLI**: `curl https://sdk.cloud.google.com | bash`
- [ ] **Authenticate with Google Cloud**: `gcloud auth login`
- [ ] **Create a new GCP project or get existing project ID**
- [ ] **Set project**: `gcloud config set project YOUR_PROJECT_ID`
- [ ] **Enable required APIs**: `gcloud services enable run.googleapis.com container.googleapis.com`

### 🚀 **Deployment Phase (10 minutes)**

- [ ] **Edit PROJECT_ID in deploy-cloud.sh** (replace `your-project-id`)
- [ ] **Test Docker build locally**: `docker build -t chess-rl-test .`
- [ ] **Deploy to Cloud Run**: `./deploy-cloud.sh`

### 📊 **Monitoring Phase**

- [ ] **Monitor training**: `gcloud run logs tail chess-rl-training --region us-central1`
- [ ] **Stop service when done**: `gcloud run services delete chess-rl-training --region us-central1`

## 💰 **Cost Estimate**
- **Setup**: Free
- **Training**: ~$0.20-0.50/hour when running
- **Idle time**: $0 (Cloud Run scales to zero)

## 🎯 **Expected Results**
- **Training speed**: ~100 games/hour with 8 concurrent games
- **Time to improvement**: 2-4 hours to see wins vs baseline
- **Checkpoints**: Saved every 5 cycles automatically

## 🔄 **Alternative: Compute Engine**
If you prefer a VM (no 15-minute timeout limit):
- Use `./cloud-compute.sh` instead of `./deploy-cloud.sh`
- Cost: ~$0.50-2.00/hour but no time limits

## 📁 **Files Created**
- `Dockerfile` - Container configuration
- `cloud-config.yaml` - Cloud-optimized training config
- `deploy-cloud.sh` - Cloud Run deployment script
- `cloud-compute.sh` - Compute Engine deployment script
- `startup-script.sh` - VM setup script

## 🚨 **Important Notes**
1. Replace `your-project-id` with your actual GCP project ID
2. Make sure you have billing enabled on your GCP project
3. Cloud Run has a 15-minute timeout limit - use Compute Engine for longer training
4. Checkpoints are saved locally - consider adding GCS integration for persistence

## 🆘 **Troubleshooting**
- **Build fails**: Check Java 21 installation in Dockerfile
- **Deployment fails**: Verify project ID and API enablement
- **Training stops**: Check Cloud Run timeout (15 min) - switch to Compute Engine
- **No improvement**: Try supervised bootstrap first with `teacher.ndjson`

---
*Generated: $(date)*
