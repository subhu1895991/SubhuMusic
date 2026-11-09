"use client";

import { ChangeEvent, FormEvent, useEffect, useMemo, useRef, useState } from "react";

type Track = {
  id: string; title: string; artist: string; duration: string; thumbnail: string;
  source?: "youtube" | "local"; blobKey?: string; fileName?: string;
};
type Playlist = { id: string; name: string; tracks: Track[]; createdAt: number };
type View = "home" | "search" | "library" | "playlists" | "playlist" | "downloads";
type RepeatMode = "off" | "all" | "one";
type YouTubePlayer = {
  loadVideoById: (id: string) => void; playVideo: () => void; pauseVideo: () => void;
  stopVideo: () => void; seekTo: (seconds: number, allowSeekAhead: boolean) => void;
  setVolume: (volume: number) => void; getCurrentTime: () => number; getDuration: () => number;
  getPlayerState: () => number; destroy: () => void;
};
type YouTubeNamespace = {
  Player: new (element: HTMLElement, options: {
    height: string; width: string; videoId: string; playerVars?: Record<string, number>;
    events: {
      onReady: (event: { target: YouTubePlayer }) => void;
      onStateChange: (event: { data: number; target: YouTubePlayer }) => void;
      onError: (event: { data: number }) => void;
    };
  }) => YouTubePlayer;
  PlayerState: { ENDED: number; PLAYING: number; PAUSED: number; CUED: number };
};
declare global {
  interface Window { YT?: YouTubeNamespace; onYouTubeIframeAPIReady?: () => void; }
}

const QUICK_SEARCHES = ["Bengali hits", "Arijit Singh", "Rabindra Sangeet", "Lofi beats", "Hindi classics"];
const AUDIO_ACCEPT = "audio/*,.mp3,.m4a,.aac,.wav,.ogg,.opus,.flac,.webm";
const DB_NAME = "subhumusic-library";
const DB_STORE = "audio-files";

function readJson<T>(key: string, fallback: T): T {
  try { const value = localStorage.getItem(key); return value ? JSON.parse(value) as T : fallback; }
  catch { return fallback; }
}
function saveJson(key: string, value: unknown) {
  try { localStorage.setItem(key, JSON.stringify(value)); } catch { /* Browser storage may be full. */ }
}
function normalizeTracks(value: unknown): Track[] {
  if (!Array.isArray(value)) return [];
  return value.filter((item) => item && typeof item.id === "string" && typeof item.title === "string" && typeof item.artist === "string")
    .map((item) => ({
      id: item.id, title: item.title, artist: item.artist,
      duration: typeof item.duration === "string" ? item.duration : "",
      thumbnail: typeof item.thumbnail === "string" ? item.thumbnail : "",
      source: item.source === "local" ? "local" : "youtube",
      ...(typeof item.blobKey === "string" ? { blobKey: item.blobKey } : {}),
      ...(typeof item.fileName === "string" ? { fileName: item.fileName } : {})
    }));
}
function openAudioDatabase(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    if (!("indexedDB" in window)) { reject(new Error("This browser does not support local music storage.")); return; }
    const request = indexedDB.open(DB_NAME, 1);
    request.onupgradeneeded = () => {
      if (!request.result.objectStoreNames.contains(DB_STORE)) request.result.createObjectStore(DB_STORE, { keyPath: "id" });
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error ?? new Error("Could not open local music storage."));
  });
}
async function storeAudioFile(id: string, blob: Blob, fileName: string) {
  const db = await openAudioDatabase();
  await new Promise<void>((resolve, reject) => {
    const transaction = db.transaction(DB_STORE, "readwrite");
    transaction.objectStore(DB_STORE).put({ id, blob, fileName });
    transaction.oncomplete = () => resolve();
    transaction.onerror = () => reject(transaction.error ?? new Error("Could not save audio file."));
    transaction.onabort = () => reject(transaction.error ?? new Error("Audio import was cancelled."));
  });
  db.close();
}
async function readAudioFile(id: string): Promise<{ blob: Blob; fileName: string } | null> {
  const db = await openAudioDatabase();
  const result = await new Promise<{ blob: Blob; fileName: string } | null>((resolve, reject) => {
    const request = db.transaction(DB_STORE, "readonly").objectStore(DB_STORE).get(id);
    request.onsuccess = () => resolve(request.result ?? null);
    request.onerror = () => reject(request.error ?? new Error("Could not read the audio file."));
  });
  db.close();
  return result;
}
async function deleteAudioFile(id: string) {
  const db = await openAudioDatabase();
  await new Promise<void>((resolve, reject) => {
    const transaction = db.transaction(DB_STORE, "readwrite");
    transaction.objectStore(DB_STORE).delete(id);
    transaction.oncomplete = () => resolve();
    transaction.onerror = () => reject(transaction.error ?? new Error("Could not remove the audio file."));
  });
  db.close();
}
function ensureYouTubeApi(): Promise<void> {
  if (window.YT?.Player) return Promise.resolve();
  return new Promise((resolve) => {
    const previous = window.onYouTubeIframeAPIReady;
    window.onYouTubeIframeAPIReady = () => { previous?.(); resolve(); };
    let script = document.querySelector<HTMLScriptElement>("script[data-subhumusic-youtube-api]");
    if (!script) {
      script = document.createElement("script");
      script.src = "https://www.youtube.com/iframe_api";
      script.async = true;
      script.dataset.subhumusicYoutubeApi = "true";
      document.head.appendChild(script);
    }
  });
}
function shuffleTracks(items: Track[]): Track[] {
  const result = [...items];
  for (let i = result.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1));
    [result[i], result[j]] = [result[j], result[i]];
  }
  return result;
}
function formatTime(seconds: number): string {
  if (!Number.isFinite(seconds) || seconds < 0) return "0:00";
  return Math.floor(seconds / 60) + ":" + String(Math.floor(seconds % 60)).padStart(2, "0");
}
function TrackArtwork({ track, className = "" }: { track: Track; className?: string }) {
  return track.thumbnail
    ? <img className={className} src={track.thumbnail} alt="" loading="lazy" />
    : <div className={"art-fallback " + className} aria-hidden="true">♫</div>;
}

export default function HomePage() {
  const [view, setView] = useState<View>("home");
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<Track[]>([]);
  const [favorites, setFavorites] = useState<Track[]>([]);
  const [recent, setRecent] = useState<Track[]>([]);
  const [localTracks, setLocalTracks] = useState<Track[]>([]);
  const [playlists, setPlaylists] = useState<Playlist[]>([]);
  const [selectedPlaylistId, setSelectedPlaylistId] = useState<string | null>(null);
  const [selected, setSelected] = useState<Track | null>(null);
  const [queue, setQueue] = useState<Track[]>([]);
  const [shuffle, setShuffle] = useState(false);
  const [repeat, setRepeat] = useState<RepeatMode>("off");
  const [showQueue, setShowQueue] = useState(false);
  const [activeMenuTrack, setActiveMenuTrack] = useState<string | null>(null);
  const [playlistName, setPlaylistName] = useState("");
  const [loading, setLoading] = useState(false);
  const [importing, setImporting] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [hasSearched, setHasSearched] = useState(false);
  const [isPlaying, setIsPlaying] = useState(false);
  const [volume, setVolume] = useState(80);
  const [currentTime, setCurrentTime] = useState(0);
  const [durationSeconds, setDurationSeconds] = useState(0);

  const playerHostRef = useRef<HTMLDivElement>(null);
  const playerRef = useRef<YouTubePlayer | null>(null);
  const audioRef = useRef<HTMLAudioElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const selectedRef = useRef<Track | null>(null);
  const queueRef = useRef<Track[]>([]);
  const repeatRef = useRef<RepeatMode>("off");
  const contextRef = useRef<Track[]>([]);
  const historyRef = useRef<Track[]>([]);
  const endedHandlerRef = useRef<() => void>(() => undefined);
  const localObjectUrlRef = useRef<string | null>(null);

  useEffect(() => {
    setFavorites(normalizeTracks(readJson("subhumusic:favorites", [])));
    setRecent(normalizeTracks(readJson("subhumusic:recent", [])));
    setLocalTracks(normalizeTracks(readJson("subhumusic:local-tracks", [])));
    const saved = readJson<Playlist[]>("subhumusic:playlists", []);
    setPlaylists(Array.isArray(saved) ? saved.filter((p) => p && typeof p.id === "string" && typeof p.name === "string")
      .map((p) => ({ ...p, tracks: normalizeTracks(p.tracks) })) : []);
  }, []);
  useEffect(() => { queueRef.current = queue; }, [queue]);
  useEffect(() => { repeatRef.current = repeat; }, [repeat]);

  useEffect(() => {
    let disposed = false;
    ensureYouTubeApi().then(() => {
      if (disposed || !playerHostRef.current || !window.YT?.Player) return;
      playerRef.current = new window.YT.Player(playerHostRef.current, {
        height: "100%", width: "100%", videoId: "",
        playerVars: { autoplay: 0, controls: 1, playsinline: 1, rel: 0, modestbranding: 1 },
        events: {
          onReady: (event) => {
            event.target.setVolume(volume);
            const current = selectedRef.current;
            if (current && current.source !== "local") event.target.loadVideoById(current.id);
          },
          onStateChange: (event) => {
            const states = window.YT?.PlayerState;
            if (event.data === states?.PLAYING) setIsPlaying(true);
            if (event.data === states?.PAUSED) setIsPlaying(false);
            if (event.data === states?.ENDED) { setIsPlaying(false); endedHandlerRef.current(); }
          },
          onError: (event) => {
            setIsPlaying(false);
            setError(event.data === 101 || event.data === 150
              ? "This video cannot be played in an embedded player. Try another result or open it in YouTube Music."
              : "YouTube could not play this track. Try another result.");
          }
        }
      });
    }).catch(() => { if (!disposed) setError("The embedded player could not be loaded. Check your connection and refresh."); });
    return () => {
      disposed = true;
      playerRef.current?.destroy();
      playerRef.current = null;
    };
  }, []);

  useEffect(() => {
    if (!selected) return;
    let cancelled = false;
    setError("");
    setCurrentTime(0);
    setDurationSeconds(0);

    if (selected.source === "local" && selected.blobKey) {
      playerRef.current?.pauseVideo();
      const audio = audioRef.current;
      if (!audio) return;
      audio.pause();
      audio.removeAttribute("src");
      audio.load();
      readAudioFile(selected.blobKey).then((stored) => {
        if (cancelled) return;
        if (!stored) {
          setError("This local audio file is missing from this browser. Import it again from your device.");
          setIsPlaying(false);
          return;
        }
        if (localObjectUrlRef.current) URL.revokeObjectURL(localObjectUrlRef.current);
        const url = URL.createObjectURL(stored.blob);
        localObjectUrlRef.current = url;
        audio.src = url;
        audio.volume = volume / 100;
        audio.load();
        void audio.play().then(() => setIsPlaying(true)).catch(() => {
          if (!cancelled) { setIsPlaying(false); setError("Your browser could not play this audio file."); }
        });
      }).catch(() => { if (!cancelled) setError("Could not open this local audio file."); });
    } else {
      audioRef.current?.pause();
      if (localObjectUrlRef.current) {
        URL.revokeObjectURL(localObjectUrlRef.current);
        localObjectUrlRef.current = null;
      }
      if (playerRef.current) {
        playerRef.current.loadVideoById(selected.id);
        playerRef.current.setVolume(volume);
      }
    }
    return () => { cancelled = true; };
  }, [selected]);

  useEffect(() => {
    if (audioRef.current) audioRef.current.volume = volume / 100;
    playerRef.current?.setVolume(volume);
  }, [volume]);

  useEffect(() => {
    const interval = window.setInterval(() => {
      const current = selectedRef.current;
      if (!current) return;
      if (current.source === "local") {
        const audio = audioRef.current;
        if (audio && Number.isFinite(audio.duration)) {
          setCurrentTime(audio.currentTime || 0);
          setDurationSeconds(audio.duration || 0);
        }
      } else if (playerRef.current) {
        try {
          setCurrentTime(playerRef.current.getCurrentTime() || 0);
          setDurationSeconds(playerRef.current.getDuration() || 0);
        } catch { /* Player can briefly be unready. */ }
      }
    }, 500);
    return () => window.clearInterval(interval);
  }, []);

  const selectedPlaylist = playlists.find((p) => p.id === selectedPlaylistId) ?? null;
  const visibleTracks = useMemo(() => {
    if (view === "library") return favorites;
    if (view === "downloads") return localTracks;
    if (view === "playlist") return selectedPlaylist?.tracks ?? [];
    if (view === "search") return results;
    return recent;
  }, [view, favorites, localTracks, selectedPlaylist, results, recent]);

  function updateQueue(next: Track[]) { queueRef.current = next; setQueue(next); }
  function startTrack(track: Track, rememberPrevious = true) {
    const previous = selectedRef.current;
    if (rememberPrevious && previous && previous.id !== track.id) historyRef.current = [...historyRef.current.slice(-39), previous];
    selectedRef.current = track;
    setSelected({ ...track });
    setError("");
    setActiveMenuTrack(null);
  }
  function playTrack(track: Track, context: Track[] = visibleTracks) {
    const index = context.findIndex((item) => item.id === track.id);
    const following = index >= 0 ? context.slice(index + 1).filter((item) => item.id !== track.id) : [];
    contextRef.current = context;
    updateQueue(shuffle ? shuffleTracks(following) : following);
    startTrack(track);
  }
  function playFromQueue(index: number) {
    const track = queueRef.current[index];
    if (!track) return;
    updateQueue(queueRef.current.slice(index + 1));
    startTrack(track);
  }
  function addToQueue(track: Track) {
    if (track.id === selectedRef.current?.id || queueRef.current.some((item) => item.id === track.id)) {
      setSuccess("That track is already playing or waiting in the queue.");
    } else {
      updateQueue([...queueRef.current, track]);
      setSuccess("Added to your play queue.");
    }
    setActiveMenuTrack(null);
  }
  function goNext() {
    if (queueRef.current.length) {
      const [next, ...remaining] = queueRef.current;
      updateQueue(remaining);
      startTrack(next);
      return;
    }
    if (repeatRef.current === "all" && contextRef.current.length) {
      const context = contextRef.current;
      const index = context.findIndex((item) => item.id === selectedRef.current?.id);
      const next = context[(index + 1) % context.length];
      if (next) startTrack(next);
      return;
    }
    setIsPlaying(false);
    if (selectedRef.current?.source === "local") audioRef.current?.pause();
    else playerRef.current?.pauseVideo();
  }
  function goPrevious() {
    if (currentTime > 4) {
      if (selectedRef.current?.source === "local" && audioRef.current) audioRef.current.currentTime = 0;
      else playerRef.current?.seekTo(0, true);
      setCurrentTime(0);
      return;
    }
    const previous = historyRef.current.pop();
    if (previous) startTrack(previous, false);
    else if (selectedRef.current?.source === "local" && audioRef.current) audioRef.current.currentTime = 0;
    else playerRef.current?.seekTo(0, true);
  }
  endedHandlerRef.current = () => {
    if (repeatRef.current === "one") {
      if (selectedRef.current?.source === "local" && audioRef.current) {
        audioRef.current.currentTime = 0;
        void audioRef.current.play();
      } else {
        playerRef.current?.seekTo(0, true);
        playerRef.current?.playVideo();
      }
      setIsPlaying(true);
      return;
    }
    goNext();
  };
  function togglePlay() {
    if (!selectedRef.current) { setError("Choose a track first."); return; }
    if (selectedRef.current.source === "local") {
      const audio = audioRef.current;
      if (!audio) return;
      if (audio.paused) void audio.play().then(() => setIsPlaying(true)).catch(() => setError("Could not resume this audio file."));
      else audio.pause();
    } else if (isPlaying) playerRef.current?.pauseVideo();
    else playerRef.current?.playVideo();
  }
  function changeVolume(value: number) { setVolume(value); }
  function seekTo(value: number) {
    const target = durationSeconds * value / 1000;
    if (selectedRef.current?.source === "local" && audioRef.current) audioRef.current.currentTime = target;
    else playerRef.current?.seekTo(target, true);
    setCurrentTime(target);
  }
  function toggleFavorite(track: Track) {
    setFavorites((current) => {
      const next = current.some((item) => item.id === track.id)
        ? current.filter((item) => item.id !== track.id) : [track, ...current].slice(0, 250);
      saveJson("subhumusic:favorites", next);
      return next;
    });
  }
  function createPlaylist(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const name = playlistName.trim();
    if (!name) return;
    const playlist: Playlist = { id: "playlist-" + Date.now(), name, tracks: [], createdAt: Date.now() };
    const next = [playlist, ...playlists];
    setPlaylists(next); saveJson("subhumusic:playlists", next);
    setPlaylistName(""); setSelectedPlaylistId(playlist.id); setView("playlist");
    setSuccess("Created “" + name + "”.");
  }
  function addToPlaylist(track: Track, playlistId: string) {
    const next = playlists.map((playlist) =>
      playlist.id !== playlistId || playlist.tracks.some((item) => item.id === track.id)
        ? playlist : { ...playlist, tracks: [...playlist.tracks, track] }
    );
    setPlaylists(next); saveJson("subhumusic:playlists", next); setActiveMenuTrack(null);
    setSuccess("Added to “" + (playlists.find((p) => p.id === playlistId)?.name ?? "playlist") + "”.");
  }
  function removeFromPlaylist(track: Track) {
    if (!selectedPlaylist) return;
    const next = playlists.map((p) => p.id === selectedPlaylist.id ? { ...p, tracks: p.tracks.filter((t) => t.id !== track.id) } : p);
    setPlaylists(next); saveJson("subhumusic:playlists", next);
  }
  function deletePlaylist(id: string) {
    const next = playlists.filter((p) => p.id !== id);
    setPlaylists(next); saveJson("subhumusic:playlists", next);
    setSelectedPlaylistId(null); setView("playlists");
  }
  async function importLocalFiles(event: ChangeEvent<HTMLInputElement>) {
    const files = Array.from(event.target.files ?? []);
    event.target.value = "";
    if (!files.length) return;
    setImporting(true); setError(""); setSuccess("");
    const imported: Track[] = [];
    try {
      for (const file of files) {
        if (!file.type.startsWith("audio/") && !/\.(mp3|m4a|aac|wav|ogg|opus|flac|webm)$/i.test(file.name)) continue;
        const key = typeof crypto.randomUUID === "function" ? crypto.randomUUID() : Date.now() + "-" + Math.random().toString(16).slice(2);
        await storeAudioFile(key, file, file.name);
        imported.push({
          id: "local:" + key, title: file.name.replace(/\.[^.]+$/, ""), artist: "Local audio",
          duration: "", thumbnail: "", source: "local", blobKey: key, fileName: file.name
        });
      }
      if (!imported.length) setError("No supported audio files were selected.");
      else {
        const next = [...imported, ...localTracks].slice(0, 300);
        setLocalTracks(next); saveJson("subhumusic:local-tracks", next);
        setSuccess(imported.length + " audio file" + (imported.length === 1 ? "" : "s") + " added to Downloads.");
      }
    } catch (e) { setError(e instanceof Error ? e.message : "Could not import audio files."); }
    finally { setImporting(false); }
  }
  async function downloadLocalTrack(track: Track) {
    if (track.source !== "local" || !track.blobKey) {
      setError("Downloads are available for audio files you imported or have permission to download.");
      return;
    }
    try {
      const stored = await readAudioFile(track.blobKey);
      if (!stored) throw new Error("The original file is no longer stored in this browser.");
      const url = URL.createObjectURL(stored.blob);
      const anchor = document.createElement("a");
      anchor.href = url; anchor.download = stored.fileName || track.fileName || (track.title + ".audio");
      document.body.appendChild(anchor); anchor.click(); anchor.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 2000);
      setSuccess("Original audio file exported to your device.");
    } catch (e) { setError(e instanceof Error ? e.message : "Could not export this audio file."); }
  }
  async function deleteLocalTrack(track: Track) {
    if (!track.blobKey) return;
    try {
      await deleteAudioFile(track.blobKey);
      const next = localTracks.filter((item) => item.id !== track.id);
      setLocalTracks(next); saveJson("subhumusic:local-tracks", next);
      setFavorites((current) => { const updated = current.filter((item) => item.id !== track.id); saveJson("subhumusic:favorites", updated); return updated; });
      const updatedPlaylists = playlists.map((p) => ({ ...p, tracks: p.tracks.filter((item) => item.id !== track.id) }));
      setPlaylists(updatedPlaylists); saveJson("subhumusic:playlists", updatedPlaylists);
      updateQueue(queueRef.current.filter((item) => item.id !== track.id));
      if (selectedRef.current?.id === track.id) {
        selectedRef.current = null; setSelected(null); audioRef.current?.pause(); playerRef.current?.stopVideo();
      }
      setSuccess("Local file removed from this browser.");
    } catch { setError("Could not remove this local file."); }
  }
  function exportPlaylist(playlist: Playlist) {
    const data = {
      app: "SubhuMusic", format: 1, name: playlist.name,
      tracks: playlist.tracks.map(({ id, title, artist, duration, thumbnail, source }) => ({ id, title, artist, duration, thumbnail, source }))
    };
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = (playlist.name.replace(/[^a-z0-9-_ ]/gi, "").trim() || "SubhuMusic-playlist") + ".json";
    document.body.appendChild(anchor); anchor.click(); anchor.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 2000);
  }
  function openView(nextView: View) {
    setView(nextView); setError(""); setSuccess(""); setActiveMenuTrack(null);
    if (nextView === "search") document.getElementById("music-search")?.focus();
  }
  async function runSearch(value = query) {
    const term = value.trim();
    if (!term) return;
    setQuery(term); setView("search"); setLoading(true); setError(""); setSuccess(""); setHasSearched(true);
    try {
      const response = await fetch("/api/search?q=" + encodeURIComponent(term), { cache: "no-store" });
      const data = await response.json();
      if (!response.ok) throw new Error(data?.error || "Search could not be completed.");
      setResults(normalizeTracks(data.items));
    } catch (e) {
      setResults([]); setError(e instanceof Error ? e.message : "Search could not be completed.");
    } finally { setLoading(false); }
  }
  function submitSearch(event: FormEvent<HTMLFormElement>) { event.preventDefault(); void runSearch(); }
  function changeRepeat() { setRepeat((current) => current === "off" ? "all" : current === "all" ? "one" : "off"); }
  function toggleShuffle() {
    setShuffle((current) => {
      const next = !current;
      if (next) updateQueue(shuffleTracks(queueRef.current));
      return next;
    });
  }

  const isFavorite = (track: Track) => favorites.some((item) => item.id === track.id);
  const heading = view === "library" ? "Your favourites" : view === "downloads" ? "Your offline music"
    : view === "playlist" ? selectedPlaylist?.name ?? "Playlist" : view === "playlists" ? "Your playlists"
      : view === "search" ? "Search results" : "Recently played";

  const rowList = (tracks: Track[]) => (
    <div className="track-list">
      {tracks.map((track, index) => (
        <div className={selected?.id === track.id ? "track-row is-playing" : "track-row"} key={track.id}>
          <button className="track-main" onClick={() => playTrack(track, tracks)} aria-label={"Play " + track.title}>
            <span className="track-number">{selected?.id === track.id ? "♫" : String(index + 1).padStart(2, "0")}</span>
            <span className="track-art-wrap"><TrackArtwork track={track} className="track-art" /><span className="art-play">▶</span></span>
            <span className="track-meta"><strong>{track.title}</strong><small>{track.artist}{track.source === "local" ? " · On this device" : ""}</small></span>
          </button>
          <span className="track-duration">{track.duration || "—"}</span>
          <button className={isFavorite(track) ? "heart-button liked" : "heart-button"} onClick={() => toggleFavorite(track)} aria-label={isFavorite(track) ? "Remove from favourites" : "Add to favourites"} title="Like">♥</button>
          <button className="row-action" onClick={() => addToQueue(track)} title="Add to queue">＋</button>
          <div className="row-menu-wrap">
            <button className="row-more" onClick={() => setActiveMenuTrack(activeMenuTrack === track.id ? null : track.id)} aria-label="More track actions">•••</button>
            {activeMenuTrack === track.id && <div className="track-menu">
              <strong>Add to playlist</strong>
              {playlists.length ? playlists.map((p) => <button key={p.id} onClick={() => addToPlaylist(track, p.id)}>{p.name}</button>) : <small>Create a playlist first.</small>}
              {track.source === "local" && <button onClick={() => void downloadLocalTrack(track)}>Download original file</button>}
              {view === "playlist" && <button onClick={() => removeFromPlaylist(track)}>Remove from this playlist</button>}
              {track.source === "local" && <button className="danger-action" onClick={() => void deleteLocalTrack(track)}>Remove local file</button>}
            </div>}
          </div>
        </div>
      ))}
    </div>
  );

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <button className="brand" onClick={() => openView("home")} aria-label="SubhuMusic home"><span className="brand-mark">S<span>♫</span></span><span className="brand-name">Subhu<span>Music</span></span></button>
        <div className="side-label">MENU</div>
        <nav className="side-nav" aria-label="Main navigation">
          <button className={view === "home" ? "nav-item active" : "nav-item"} onClick={() => openView("home")}><span className="nav-icon">⌂</span> Home</button>
          <button className={view === "search" ? "nav-item active" : "nav-item"} onClick={() => openView("search")}><span className="nav-icon">⌕</span> Search</button>
          <button className={view === "library" ? "nav-item active" : "nav-item"} onClick={() => openView("library")}><span className="nav-icon">♥</span> Liked songs</button>
          <button className={view === "playlists" || view === "playlist" ? "nav-item active" : "nav-item"} onClick={() => openView("playlists")}><span className="nav-icon">▤</span> Playlists</button>
          <button className={view === "downloads" ? "nav-item active" : "nav-item"} onClick={() => openView("downloads")}><span className="nav-icon">↓</span> Downloads</button>
        </nav>
        <div className="sidebar-divider" /><div className="side-label">YOUR SPACE</div>
        <button className="collection-link" onClick={() => openView("library")}><span className="collection-art">♥</span><span><strong>Liked songs</strong><small>{favorites.length} saved here</small></span></button>
        <button className="collection-link" onClick={() => openView("downloads")}><span className="collection-art download-art">↓</span><span><strong>Offline music</strong><small>{localTracks.length} local files</small></span></button>
        <div className="sidebar-bottom"><div className="mini-note"><span className="note-dot" /> A little more music, every day.</div><p>SubhuMusic Web</p><small>Made by Subhadip Tantubay</small></div>
      </aside>

      <main className="main-column">
        <div className="mobile-nav">
          <button className="mobile-brand" onClick={() => openView("home")}><span className="brand-mark">S<span>♫</span></span> SubhuMusic</button>
          <div>
            <button className={view === "home" ? "mobile-nav-btn active" : "mobile-nav-btn"} onClick={() => openView("home")}>Home</button>
            <button className={view === "search" ? "mobile-nav-btn active" : "mobile-nav-btn"} onClick={() => openView("search")}>Search</button>
            <button className={view === "library" ? "mobile-nav-btn active" : "mobile-nav-btn"} onClick={() => openView("library")}>Liked</button>
            <button className={view === "playlists" || view === "playlist" ? "mobile-nav-btn active" : "mobile-nav-btn"} onClick={() => openView("playlists")}>Lists</button>
            <button className={view === "downloads" ? "mobile-nav-btn active" : "mobile-nav-btn"} onClick={() => openView("downloads")}>↓</button>
          </div>
        </div>

        <header className="topbar">
          <div className="topbar-copy"><span className="eyebrow">YOUR SOUND. YOUR SPACE.</span><h1>{view === "home" ? "Good music, good mood." : view === "search" ? "Find your next favourite." : view === "downloads" ? "Your music, on this device." : view === "playlists" || view === "playlist" ? "Make a soundtrack of your own." : "Your personal collection."}</h1></div>
          <form className="search-form" onSubmit={submitSearch}><span className="search-symbol">⌕</span><input id="music-search" type="search" value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search songs, artists..." aria-label="Search songs and artists" /><button type="submit" disabled={loading}>{loading ? "..." : "Search"}</button></form>
        </header>

        {view === "home" && <>
          <section className="hero-card"><div className="hero-glow hero-glow-one" /><div className="hero-glow hero-glow-two" />
            <div className="hero-content"><span className="hero-kicker"><span className="hero-kicker-dot" /> THE MUSIC SPACE</span><h2>Every rhythm.<br /><em>One place.</em></h2><p>Search what you love, discover something new, and make this space yours.</p><button className="hero-button" onClick={() => document.getElementById("music-search")?.focus()}>Explore music <span>↗</span></button></div>
            <div className="hero-visual" aria-hidden="true"><div className="vinyl"><div className="vinyl-groove" /><div className="vinyl-label">S<span>♫</span></div></div><div className="sound-bars"><i /><i /><i /><i /><i /><i /><i /></div><span className="visual-star star-a">✦</span><span className="visual-star star-b">✧</span></div><div className="hero-index">01 / DISCOVER</div>
          </section>
          <section className="quick-section"><div className="section-heading"><div><span className="eyebrow">START WITH A VIBE</span><h2>Explore your mood</h2></div></div><div className="quick-chips">{QUICK_SEARCHES.map((item) => <button key={item} onClick={() => void runSearch(item)}>{item}<span>↗</span></button>)}</div></section>
          <section className="feature-shortcuts">
            <button onClick={() => openView("playlists")}><span>▤</span><strong>Make a playlist</strong><small>Collect tracks your way</small></button>
            <button onClick={() => openView("downloads")}><span>↓</span><strong>Import music</strong><small>Play your own audio files</small></button>
            <button onClick={() => setShowQueue(true)}><span>≡</span><strong>Play queue</strong><small>{queue.length} tracks waiting</small></button>
          </section>
        </>}

        {view === "search" && <section className="search-intro"><span className="eyebrow">THE WORLD OF MUSIC</span><h2>{query ? <>Results for <em>“{query}”</em></> : "Search and discover"}</h2><p>Search the public music catalogue. Select a result to play it in the embedded player.</p></section>}
        {view === "library" && <section className="search-intro library-intro"><span className="eyebrow">YOUR COLLECTION</span><h2>Saved with <em>love.</em></h2><p>Your liked tracks are stored in this browser. Use playlists to organise them.</p></section>}
        {view === "downloads" && <section className="search-intro downloads-intro">
          <span className="eyebrow">YOUR OFFLINE COLLECTION</span><h2>Music from <em>your device.</em></h2>
          <p>Import audio files you own or have permission to use. They stay in this browser and can be exported back to your device.</p>
          <div className="download-tools"><button className="hero-button" onClick={() => fileInputRef.current?.click()} disabled={importing}>{importing ? "Importing..." : "＋ Import audio files"}</button><span>{localTracks.length} files stored in this browser</span><input ref={fileInputRef} type="file" accept={AUDIO_ACCEPT} multiple hidden onChange={(e) => void importLocalFiles(e)} /></div>
          <div className="download-warning">YouTube catalogue tracks play through YouTube’s embedded player and are not converted into downloadable audio files.</div>
        </section>}
        {view === "playlists" && <section className="search-intro playlist-intro">
          <span className="eyebrow">CURATE YOUR SOUND</span><h2>Your playlists, <em>your rules.</em></h2><p>Create collections and add music from search results, favourites, or local files.</p>
          <form className="create-playlist-form" onSubmit={createPlaylist}><input value={playlistName} onChange={(e) => setPlaylistName(e.target.value)} maxLength={50} placeholder="Name your new playlist..." aria-label="New playlist name" /><button type="submit" disabled={!playlistName.trim()}>Create playlist</button></form>
          {playlists.length ? <div className="playlist-grid">{playlists.map((p) => <button className="playlist-card" key={p.id} onClick={() => { setSelectedPlaylistId(p.id); setView("playlist"); }}><span className="playlist-card-art">♫</span><strong>{p.name}</strong><small>{p.tracks.length} tracks</small></button>)}</div> : <div className="empty-state compact-empty"><div className="empty-icon">▤</div><strong>No playlists yet.</strong><p>Create your first playlist above, then add tracks using the + menu.</p></div>}
        </section>}
        {view === "playlist" && selectedPlaylist && <section className="search-intro playlist-intro">
          <span className="eyebrow">YOUR PLAYLIST</span><h2>{selectedPlaylist.name}</h2><p>{selectedPlaylist.tracks.length} tracks · Saved in this browser</p>
          <div className="playlist-header-actions"><button className="hero-button" onClick={() => selectedPlaylist.tracks.length && playTrack(selectedPlaylist.tracks[0], selectedPlaylist.tracks)}>▶ Play playlist</button><button className="outline-action" onClick={() => exportPlaylist(selectedPlaylist)}>Export playlist JSON ↓</button><button className="outline-action danger-action" onClick={() => deletePlaylist(selectedPlaylist.id)}>Delete playlist</button></div>
        </section>}
        {view === "playlist" && !selectedPlaylist && <section className="search-intro"><h2>Choose a playlist</h2><button className="hero-button" onClick={() => openView("playlists")}>View playlists</button></section>}

        {(error || success) && <div className={error ? "notice error-notice" : "notice success-notice"} role="status">{error || success}</div>}

        {view !== "playlists" && <section className="track-section">
          <div className="section-heading track-heading"><div><span className="eyebrow">{view === "search" ? "MATCHING TRACKS" : view === "library" ? "SAVED BY YOU" : view === "downloads" ? "IMPORTED AUDIO FILES" : view === "playlist" ? "IN THIS PLAYLIST" : "PICK UP WHERE YOU LEFT OFF"}</span><h2>{heading}</h2></div>
            {view === "home" && recent.length > 0 && <button className="text-button" onClick={() => openView("library")}>Your library ↗</button>}
            {view === "search" && hasSearched && !loading && <span className="result-count">{results.length} results</span>}
            {view === "playlist" && selectedPlaylist && <button className="text-button" onClick={() => openView("playlists")}>All playlists ↗</button>}
          </div>
          {loading ? <div className="empty-state"><div className="loader" /><strong>Finding your music...</strong><p>Searching the catalogue</p></div>
            : visibleTracks.length ? rowList(visibleTracks)
              : <div className="empty-state"><div className="empty-icon">{view === "library" ? "♡" : view === "search" ? "⌕" : view === "downloads" ? "↓" : view === "playlist" ? "▤" : "♫"}</div>
                <strong>{view === "library" ? "Your favourites live here." : view === "search" ? (hasSearched ? "No tracks found." : "Ready when you are.") : view === "downloads" ? "No local music imported yet." : view === "playlist" ? "This playlist is waiting for music." : "Your listening journey starts here."}</strong>
                <p>{view === "library" ? "Tap the heart beside a track to save it to this browser." : view === "search" ? (hasSearched ? "Try a different song title or artist." : "Type a song or artist in the search bar above.") : view === "downloads" ? "Import audio files from your device. They will remain available in this browser." : view === "playlist" ? "Search for music and use the + menu to add tracks here." : "Search for a song and press play to get started."}</p>
                {view === "downloads" ? <button className="empty-action" onClick={() => fileInputRef.current?.click()}>Import audio <span>↗</span></button> : <button className="empty-action" onClick={() => { openView("search"); document.getElementById("music-search")?.focus(); }}>Find music <span>↗</span></button>}
              </div>}
        </section>}
        <footer className="page-footer"><span>SubhuMusic Web · Made with ♡ by Subhadip Tantubay</span><span>Streaming by YouTube · Local audio stays in your browser.</span></footer>
      </main>

      <aside className="player-column">
        <div className="player-topline"><span className="live-dot" /> NOW PLAYING <span className="player-topline-tag">WEB PLAYER</span></div>
        <div className="player-screen">
          <div ref={playerHostRef} className="youtube-player-host" />
          {!selected && <div className="player-placeholder"><div className="placeholder-orbit orbit-one" /><div className="placeholder-orbit orbit-two" /><div className="placeholder-note">♫</div><span>YOUR NEXT SOUNDTRACK</span><strong>Choose a track<br />to begin.</strong></div>}
          {selected?.source === "local" && <div className="local-player-cover"><TrackArtwork track={selected} className="local-player-art" /><span>LOCAL AUDIO</span><strong>{selected.title}</strong><small>Playing from this browser</small></div>}
        </div>
        <audio ref={audioRef} hidden onEnded={() => endedHandlerRef.current()} onPlay={() => setIsPlaying(true)} onPause={() => setIsPlaying(false)} onLoadedMetadata={(e) => setDurationSeconds(e.currentTarget.duration || 0)} />
        <div className="now-playing-info"><span className="eyebrow">ON YOUR DECK</span>
          {selected ? <><h2>{selected.title}</h2><p>{selected.artist}</p><div className="player-actions">
            <button className={isFavorite(selected) ? "player-heart liked" : "player-heart"} onClick={() => toggleFavorite(selected)}>{isFavorite(selected) ? "♥ Saved" : "♡ Save"}</button>
            <button className="player-heart" onClick={() => addToQueue(selected)}>＋ Queue</button>
            {selected.source === "local" ? <button className="player-heart" onClick={() => void downloadLocalTrack(selected)}>↓ Download</button> : <a href={"https://music.youtube.com/watch?v=" + encodeURIComponent(selected.id)} target="_blank" rel="noreferrer">Open in YouTube Music ↗</a>}
          </div></> : <><h2>A little silence?</h2><p>Search for a song and start listening.</p><button className="player-search-link" onClick={() => document.getElementById("music-search")?.focus()}>Search music ↗</button></>}
        </div>
        <div className="player-controls-panel">
          <div className="player-control-buttons">
            <button className={shuffle ? "mode-control active" : "mode-control"} onClick={toggleShuffle} title="Shuffle">⤨</button>
            <button onClick={goPrevious} title="Previous track">|◀</button>
            <button className="control-play" onClick={togglePlay} title={isPlaying ? "Pause" : "Play"}>{isPlaying ? "Ⅱ" : "▶"}</button>
            <button onClick={goNext} title="Next track">▶|</button>
            <button className={repeat !== "off" ? "mode-control active" : "mode-control"} onClick={changeRepeat} title={"Repeat: " + repeat}>{repeat === "one" ? "↻¹" : "↻"}</button>
          </div>
          <div className="progress-row"><span>{formatTime(currentTime)}</span><input type="range" min="0" max="1000" value={durationSeconds ? Math.min(1000, currentTime / durationSeconds * 1000) : 0} onChange={(e) => seekTo(Number(e.target.value))} aria-label="Seek track" /><span>{formatTime(durationSeconds)}</span></div>
          <div className="volume-row"><span>VOLUME</span><input type="range" min="0" max="100" value={volume} onChange={(e) => changeVolume(Number(e.target.value))} aria-label="Volume" /></div>
        </div>
        <div className="queue-head"><div><span className="eyebrow">UP NEXT</span><strong>{queue.length} tracks</strong></div><button className="text-button" onClick={() => setShowQueue((v) => !v)}>{showQueue ? "Hide queue" : "View queue"}</button></div>
        {showQueue && <div className="queue-panel">
          {queue.length ? queue.map((track, index) => <div className="queue-item" key={track.id + "-" + index}>
            <button onClick={() => playFromQueue(index)}><TrackArtwork track={track} className="queue-art" /><span><strong>{track.title}</strong><small>{track.artist}</small></span></button>
            <button className="queue-remove" onClick={() => updateQueue(queueRef.current.filter((_, i) => i !== index))} aria-label="Remove from queue">×</button>
          </div>) : <div className="queue-empty">Your queue is empty. Play a list or add tracks with ＋.</div>}
          {queue.length > 0 && <button className="clear-queue" onClick={() => updateQueue([])}>Clear queue</button>}
        </div>}
        <div className="player-note"><span>ⓘ</span><p>YouTube tracks play in the official embedded player. Imported local files are stored in this browser. YouTube controls stream availability and ads.</p></div>
      </aside>

      {selected && <div className="bottom-dock">
        <div className="dock-track"><TrackArtwork track={selected} className="dock-art" /><div><strong>{selected.title}</strong><small>{selected.artist}</small></div></div>
        <div className="dock-transport"><button onClick={goPrevious} aria-label="Previous">|◀</button><button className="dock-play" onClick={togglePlay} aria-label={isPlaying ? "Pause" : "Play"}>{isPlaying ? "Ⅱ" : "▶"}</button><button onClick={goNext} aria-label="Next">▶|</button></div>
        <div className="dock-queue"><button onClick={() => setShowQueue((v) => !v)}>≡ Queue ({queue.length})</button></div>
      </div>}
    </div>
  );
}
