# PodPulse Sync Server

Lightweight, high-performance sync backend for **PodPulse**, deployed on Oracle Cloud VPS.

## Features
- **Gpodder 2.0 Compatibility**: Direct compatibility with AntennaPod/PodPulse sync protocol (subscriptions, episode progress, play states).
- **PodPulse Smart Playlists**: Sync custom filter rules, dynamic queues, and smart playlists across devices.
- **AI Summary Cache**: Store and share AI-generated episode summaries and key takeaways.
- **Lightweight**: Pure FastAPI + SQLite on Docker, ultra-low memory footprint (<50MB RAM).

## Deployment on Oracle Cloud VPS
1. Transfer `sync-server/` directory to your VPS:
   ```bash
   scp -i <path-to-your-ssh-key> -r sync-server <user>@<server-host>:~/
   ```
2. SSH into your VPS:
   ```bash
   ssh -i <path-to-your-ssh-key> <user>@<server-host>
   ```
3. Launch with Docker Compose:
   ```bash
   cd sync-server
   docker compose up -d --build
   ```
4. Access the health check:
   ```bash
   curl http://localhost:8000/api/podpulse/health
   ```
