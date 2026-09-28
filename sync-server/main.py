import os
import time
import json
import sqlite3
from typing import List, Optional, Dict, Any
from fastapi import FastAPI, Request, Response, HTTPException, status, Query, Depends
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

DB_PATH = os.getenv("DB_PATH", "/data/podpulse.db")
SERVER_NAME = "PodPulse Sync Server"

app = FastAPI(title="PodPulse Sync Service", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origin_regex=r".*",
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

def get_db():
    os.makedirs(os.path.dirname(os.path.abspath(DB_PATH)), exist_ok=True)
    conn = sqlite3.connect(DB_PATH, timeout=15.0)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode=WAL;")
    return conn

def init_db():
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS users (
        username TEXT PRIMARY KEY,
        password_hash TEXT NOT NULL,
        created_at INTEGER NOT NULL
    )
    """)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS devices (
        username TEXT NOT NULL,
        device_id TEXT NOT NULL,
        caption TEXT DEFAULT '',
        type TEXT DEFAULT 'mobile',
        updated_at INTEGER NOT NULL,
        PRIMARY KEY (username, device_id)
    )
    """)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS subscriptions (
        username TEXT NOT NULL,
        url TEXT NOT NULL,
        is_deleted INTEGER DEFAULT 0,
        updated_at INTEGER NOT NULL,
        PRIMARY KEY (username, url)
    )
    """)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS episode_actions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT NOT NULL,
        podcast TEXT NOT NULL,
        episode TEXT NOT NULL,
        action TEXT NOT NULL,
        position INTEGER DEFAULT 0,
        total INTEGER DEFAULT 0,
        started INTEGER DEFAULT 0,
        device TEXT DEFAULT '',
        timestamp TEXT NOT NULL,
        created_at INTEGER NOT NULL
    )
    """)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS smart_playlists (
        id TEXT NOT NULL,
        username TEXT NOT NULL,
        name TEXT NOT NULL,
        query_rules TEXT NOT NULL,
        updated_at INTEGER NOT NULL,
        PRIMARY KEY (username, id)
    )
    """)
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS ai_summaries (
        episode_url TEXT PRIMARY KEY,
        title TEXT,
        summary TEXT NOT NULL,
        key_takeaways TEXT,
        created_at INTEGER NOT NULL
    )
    """)
    conn.commit()
    conn.close()

@app.on_event("startup")
def on_startup():
    init_db()

@app.get("/api/podpulse/health")
def health_check():
    return {"status": "ok", "service": "PodPulse Sync", "timestamp": int(time.time())}

# --- Gpodder 2.0 Auth API ---

@app.post("/api/2/auth/{username}/login.json")
async def login(username: str, request: Request, response: Response):
    # Support basic auth or body credentials; auto-provision user on first login if not set
    auth = request.headers.get("Authorization")
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("SELECT username FROM users WHERE username = ?", (username,))
    row = cursor.fetchone()
    if not row:
        cursor.execute("INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)",
                       (username, "auto", int(time.time())))
        conn.commit()
    conn.close()
    
    session_id = f"session_{username}_{int(time.time())}"
    response.set_cookie(key="sessionid", value=session_id, httponly=True)
    return {"status": "success", "sessionid": session_id}

@app.post("/api/2/auth/{username}/logout.json")
def logout(username: str, response: Response):
    response.delete_cookie(key="sessionid")
    return {"status": "success"}

# --- Gpodder 2.0 Devices API ---

class DeviceUpdate(BaseModel):
    caption: Optional[str] = ""
    type: Optional[str] = "mobile"

@app.get("/api/2/devices/{username}.json")
def list_devices(username: str):
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("SELECT device_id, caption, type, subscriptions FROM (SELECT d.device_id, d.caption, d.type, 0 as subscriptions FROM devices d WHERE d.username = ?)", (username,))
    rows = cursor.fetchall()
    devices = [{"id": r["device_id"], "caption": r["caption"], "type": r["type"], "subscriptions": r["subscriptions"]} for r in rows]
    conn.close()
    return devices

@app.post("/api/2/devices/{username}/{deviceid}.json")
def update_device(username: str, deviceid: str, device: DeviceUpdate):
    conn = get_db()
    cursor = conn.cursor()
    now = int(time.time())
    cursor.execute("""
        INSERT INTO devices (username, device_id, caption, type, updated_at)
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT(username, device_id) DO UPDATE SET caption=?, type=?, updated_at=?
    """, (username, deviceid, device.caption or "", device.type or "mobile", now,
          device.caption or "", device.type or "mobile", now))
    conn.commit()
    conn.close()
    return {"status": "success"}

# --- Gpodder 2.0 Subscriptions API ---

class SubscriptionDelta(BaseModel):
    add: Optional[List[str]] = []
    remove: Optional[List[str]] = []

@app.get("/api/2/subscriptions/{username}/{deviceid}.json")
def get_device_subscriptions(username: str, deviceid: str, since: Optional[int] = Query(None)):
    conn = get_db()
    cursor = conn.cursor()
    now = int(time.time())
    
    if since is not None:
        cursor.execute("SELECT url FROM subscriptions WHERE username = ? AND is_deleted = 0 AND updated_at > ?", (username, since))
        added = [r["url"] for r in cursor.fetchall()]
        cursor.execute("SELECT url FROM subscriptions WHERE username = ? AND is_deleted = 1 AND updated_at > ?", (username, since))
        removed = [r["url"] for r in cursor.fetchall()]
        conn.close()
        return {"add": added, "remove": removed, "timestamp": now}
    else:
        cursor.execute("SELECT url FROM subscriptions WHERE username = ? AND is_deleted = 0", (username,))
        urls = [r["url"] for r in cursor.fetchall()]
        conn.close()
        return urls

@app.post("/api/2/subscriptions/{username}/{deviceid}.json")
def update_subscriptions(username: str, deviceid: str, delta: SubscriptionDelta):
    conn = get_db()
    cursor = conn.cursor()
    now = int(time.time())
    
    for url in (delta.add or []):
        cursor.execute("""
            INSERT INTO subscriptions (username, url, is_deleted, updated_at)
            VALUES (?, ?, 0, ?)
            ON CONFLICT(username, url) DO UPDATE SET is_deleted=0, updated_at=?
        """, (username, url, now, now))
        
    for url in (delta.remove or []):
        cursor.execute("""
            INSERT INTO subscriptions (username, url, is_deleted, updated_at)
            VALUES (?, ?, 1, ?)
            ON CONFLICT(username, url) DO UPDATE SET is_deleted=1, updated_at=?
        """, (username, url, now, now))
        
    conn.commit()
    conn.close()
    return {"timestamp": now, "update_urls": []}

# --- Gpodder 2.0 Episode Actions API ---

class EpisodeActionItem(BaseModel):
    podcast: str
    episode: str
    action: str
    position: Optional[int] = 0
    total: Optional[int] = 0
    started: Optional[int] = 0
    device: Optional[str] = ""
    timestamp: str

@app.get("/api/2/episodes/{username}.json")
def get_episode_actions(username: str, since: Optional[int] = Query(0), podcast: Optional[str] = None):
    conn = get_db()
    cursor = conn.cursor()
    now = int(time.time())
    query = "SELECT podcast, episode, action, position, total, started, device, timestamp FROM episode_actions WHERE username = ?"
    params: List[Any] = [username]
    if since and since > 0:
        query += " AND created_at >= ?"
        params.append(since)
    if podcast:
        query += " AND podcast = ?"
        params.append(podcast)
    query += " ORDER BY created_at ASC LIMIT 1000"
    
    cursor.execute(query, params)
    rows = cursor.fetchall()
    actions = [dict(r) for r in rows]
    conn.close()
    return {"actions": actions, "timestamp": now}

@app.post("/api/2/episodes/{username}.json")
def upload_episode_actions(username: str, actions: List[EpisodeActionItem]):
    conn = get_db()
    cursor = conn.cursor()
    now = int(time.time())
    for item in actions:
        cursor.execute("""
            INSERT INTO episode_actions (username, podcast, episode, action, position, total, started, device, timestamp, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (username, item.podcast, item.episode, item.action, item.position or 0, item.total or 0,
              item.started or 0, item.device or "", item.timestamp, now))
    conn.commit()
    conn.close()
    return {"timestamp": now, "update_urls": []}

# --- PodPulse Extended API (Playlists & AI Summaries) ---

class SmartPlaylist(BaseModel):
    id: str
    name: str
    query_rules: Dict[str, Any]

@app.get("/api/podpulse/playlists/{username}")
def get_smart_playlists(username: str):
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("SELECT id, name, query_rules, updated_at FROM smart_playlists WHERE username = ?", (username,))
    rows = cursor.fetchall()
    conn.close()
    return [{"id": r["id"], "name": r["name"], "query_rules": json.loads(r["query_rules"]), "updated_at": r["updated_at"]} for r in rows]

@app.post("/api/podpulse/playlists/{username}")
def save_smart_playlist(username: str, playlist: SmartPlaylist):
    conn = get_db()
    cursor = conn.cursor()
    now = int(time.time())
    cursor.execute("""
        INSERT INTO smart_playlists (id, username, name, query_rules, updated_at)
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT(username, id) DO UPDATE SET name=?, query_rules=?, updated_at=?
    """, (playlist.id, username, playlist.name, json.dumps(playlist.query_rules), now,
          playlist.name, json.dumps(playlist.query_rules), now))
    conn.commit()
    conn.close()
    return {"status": "saved", "id": playlist.id, "timestamp": now}

class AISummaryPayload(BaseModel):
    episode_url: str
    title: Optional[str] = ""
    summary: str
    key_takeaways: Optional[List[str]] = []

@app.get("/api/podpulse/summaries")
def get_summary(episode_url: str = Query(...)):
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("SELECT episode_url, title, summary, key_takeaways, created_at FROM ai_summaries WHERE episode_url = ?", (episode_url,))
    row = cursor.fetchone()
    conn.close()
    if not row:
        raise HTTPException(status_code=404, detail="Summary not found")
    return {
        "episode_url": row["episode_url"],
        "title": row["title"],
        "summary": row["summary"],
        "key_takeaways": json.loads(row["key_takeaways"]) if row["key_takeaways"] else [],
        "created_at": row["created_at"]
    }

@app.post("/api/podpulse/summaries")
def store_summary(payload: AISummaryPayload):
    conn = get_db()
    cursor = conn.cursor()
    now = int(time.time())
    cursor.execute("""
        INSERT INTO ai_summaries (episode_url, title, summary, key_takeaways, created_at)
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT(episode_url) DO UPDATE SET summary=?, key_takeaways=?, created_at=?
    """, (payload.episode_url, payload.title, payload.summary, json.dumps(payload.key_takeaways), now,
          payload.summary, json.dumps(payload.key_takeaways), now))
    conn.commit()
    conn.close()
    return {"status": "stored", "episode_url": payload.episode_url, "timestamp": now}
