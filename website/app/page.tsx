"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";

type Track = {
  id: string;
  title: string;
  artist: string;
  duration: string;
  thumbnail: string;
};

type View = "home" | "search" | "library";

const QUICK_SEARCHES = ["Bengali hits", "Arijit Singh", "Rabindra Sangeet", "Lofi beats", "Hindi classics"];

function readSavedTracks(key: string): Track[] {
  try {
    const value = localStorage.getItem(key);
    if (!value) return [];
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed.filter((item) => item && typeof item.id === "string") : [];
  } catch {
    return [];
  }
}

function TrackArtwork({ track, className = "" }: { track: Track; className?: string }) {
  return track.thumbnail ? (
    // Remote catalogue artwork is supplied by the search result.
    // eslint-disable-next-line @next/next/no-img-element
    <img className={className} src={track.thumbnail} alt="" loading="lazy" />
  ) : (
    <div className={"art-fallback " + className} aria-hidden="true">♫</div>
  );
}

export default function HomePage() {
  const [view, setView] = useState<View>("home");
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<Track[]>([]);
  const [favorites, setFavorites] = useState<Track[]>([]);
  const [recent, setRecent] = useState<Track[]>([]);
  const [selected, setSelected] = useState<Track | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [hasSearched, setHasSearched] = useState(false);

  useEffect(() => {
    setFavorites(readSavedTracks("subhumusic:favorites"));
    setRecent(readSavedTracks("subhumusic:recent"));
  }, []);

  const visibleTracks = useMemo(() => {
    if (view === "library") return favorites;
    if (view === "home") return recent;
    return results;
  }, [view, favorites, recent, results]);

  async function runSearch(value = query) {
    const term = value.trim();
    if (!term) return;

    setQuery(term);
    setView("search");
    setLoading(true);
    setError("");
    setHasSearched(true);

    try {
      const response = await fetch("/api/search?q=" + encodeURIComponent(term), {
        cache: "no-store"
      });
      const data = await response.json();

      if (!response.ok) {
        throw new Error(data?.error || "Search could not be completed.");
      }

      setResults(Array.isArray(data.items) ? data.items : []);
    } catch (searchError) {
      setResults([]);
      setError(searchError instanceof Error ? searchError.message : "Search could not be completed.");
    } finally {
      setLoading(false);
    }
  }

  function playTrack(track: Track) {
    setSelected(track);
    setRecent((current) => {
      const next = [track, ...current.filter((item) => item.id !== track.id)].slice(0, 12);
      try {
        localStorage.setItem("subhumusic:recent", JSON.stringify(next));
      } catch {
        // Playback should continue even when browser storage is unavailable.
      }
      return next;
    });
  }

  function toggleFavorite(track: Track) {
    setFavorites((current) => {
      const exists = current.some((item) => item.id === track.id);
      const next = exists
        ? current.filter((item) => item.id !== track.id)
        : [track, ...current].slice(0, 100);

      try {
        localStorage.setItem("subhumusic:favorites", JSON.stringify(next));
      } catch {
        setError("Your browser could not save this favourite. Check its storage settings.");
      }
      return next;
    });
  }

  function submitSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void runSearch();
  }

  function openView(nextView: View) {
    setView(nextView);
    setError("");
    if (nextView === "search") {
      document.getElementById("music-search")?.focus();
    }
  }

  const heading = view === "library"
    ? "Your favourites"
    : view === "search"
      ? "Search results"
      : "Recently played";

  const isFavorite = (track: Track) => favorites.some((item) => item.id === track.id);

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <button className="brand" onClick={() => openView("home")} aria-label="SubhuMusic home">
          <span className="brand-mark">S<span>♫</span></span>
          <span className="brand-name">Subhu<span>Music</span></span>
        </button>

        <div className="side-label">MENU</div>
        <nav className="side-nav" aria-label="Main navigation">
          <button className={view === "home" ? "nav-item active" : "nav-item"} onClick={() => openView("home")}>
            <span className="nav-icon">⌂</span> Home
          </button>
          <button className={view === "search" ? "nav-item active" : "nav-item"} onClick={() => openView("search")}>
            <span className="nav-icon">⌕</span> Search
          </button>
          <button className={view === "library" ? "nav-item active" : "nav-item"} onClick={() => openView("library")}>
            <span className="nav-icon">▤</span> Your library
          </button>
        </nav>

        <div className="sidebar-divider" />
        <div className="side-label">YOUR SPACE</div>
        <button className="collection-link" onClick={() => openView("library")}>
          <span className="collection-art">♥</span>
          <span><strong>Liked songs</strong><small>{favorites.length} saved here</small></span>
        </button>

        <div className="sidebar-bottom">
          <div className="mini-note"><span className="note-dot" /> A little more music, every day.</div>
          <p>SubhuMusic Web</p>
          <small>Made by Subhadip Tantubay</small>
        </div>
      </aside>

      <main className="main-column">
        <div className="mobile-nav">
          <button className="mobile-brand" onClick={() => openView("home")}>
            <span className="brand-mark">S<span>♫</span></span> SubhuMusic
          </button>
          <div>
            <button className={view === "home" ? "mobile-nav-btn active" : "mobile-nav-btn"} onClick={() => openView("home")}>Home</button>
            <button className={view === "search" ? "mobile-nav-btn active" : "mobile-nav-btn"} onClick={() => openView("search")}>Search</button>
            <button className={view === "library" ? "mobile-nav-btn active" : "mobile-nav-btn"} onClick={() => openView("library")}>Library</button>
          </div>
        </div>

        <header className="topbar">
          <div className="topbar-copy">
            <span className="eyebrow">YOUR SOUND. YOUR SPACE.</span>
            <h1>{view === "home" ? "Good music, good mood." : view === "search" ? "Find your next favourite." : "Your personal collection."}</h1>
          </div>
          <form className="search-form" onSubmit={submitSearch}>
            <span className="search-symbol">⌕</span>
            <input
              id="music-search"
              type="search"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search songs, artists..."
              aria-label="Search songs and artists"
            />
            <button type="submit" disabled={loading}>{loading ? "..." : "Search"}</button>
          </form>
        </header>

        {view === "home" && (
          <section className="hero-card">
            <div className="hero-glow hero-glow-one" />
            <div className="hero-glow hero-glow-two" />
            <div className="hero-content">
              <span className="hero-kicker"><span className="hero-kicker-dot" /> THE MUSIC SPACE</span>
              <h2>Every rhythm.<br /><em>One place.</em></h2>
              <p>Search what you love, discover something new, and make this space yours.</p>
              <button className="hero-button" onClick={() => document.getElementById("music-search")?.focus()}>
                Explore music <span>↗</span>
              </button>
            </div>
            <div className="hero-visual" aria-hidden="true">
              <div className="vinyl">
                <div className="vinyl-groove" />
                <div className="vinyl-label">S<span>♫</span></div>
              </div>
              <div className="sound-bars"><i /><i /><i /><i /><i /><i /><i /></div>
              <span className="visual-star star-a">✦</span>
              <span className="visual-star star-b">✧</span>
            </div>
            <div className="hero-index">01 / DISCOVER</div>
          </section>
        )}

        {view === "home" && (
          <section className="quick-section">
            <div className="section-heading">
              <div><span className="eyebrow">START WITH A VIBE</span><h2>Explore your mood</h2></div>
            </div>
            <div className="quick-chips">
              {QUICK_SEARCHES.map((item) => (
                <button key={item} onClick={() => void runSearch(item)}>{item}<span>↗</span></button>
              ))}
            </div>
          </section>
        )}

        {view === "search" && (
          <section className="search-intro">
            <span className="eyebrow">THE WORLD OF MUSIC</span>
            <h2>{query ? <>Results for <em>“{query}”</em></> : "Search and discover"}</h2>
            <p>Search the public music catalogue. Select a result to play it in the embedded player.</p>
          </section>
        )}

        {view === "library" && (
          <section className="search-intro library-intro">
            <span className="eyebrow">YOUR COLLECTION</span>
            <h2>Saved with <em>love.</em></h2>
            <p>Your favourites are stored in this browser, so they stay on this device.</p>
          </section>
        )}

        {error && <div className="notice error-notice" role="alert">{error}</div>}

        <section className="track-section">
          <div className="section-heading track-heading">
            <div>
              <span className="eyebrow">{view === "search" ? "MATCHING TRACKS" : view === "library" ? "SAVED BY YOU" : "PICK UP WHERE YOU LEFT OFF"}</span>
              <h2>{heading}</h2>
            </div>
            {view === "home" && recent.length > 0 && <button className="text-button" onClick={() => openView("library")}>Your library ↗</button>}
            {view === "search" && hasSearched && !loading && <span className="result-count">{results.length} results</span>}
          </div>

          {loading ? (
            <div className="empty-state"><div className="loader" /><strong>Finding your music...</strong><p>Searching the catalogue</p></div>
          ) : visibleTracks.length > 0 ? (
            <div className="track-list">
              {visibleTracks.map((track, index) => (
                <div className={selected?.id === track.id ? "track-row is-playing" : "track-row"} key={track.id}>
                  <button className="track-main" onClick={() => playTrack(track)} aria-label={"Play " + track.title}>
                    <span className="track-number">{selected?.id === track.id ? "♫" : String(index + 1).padStart(2, "0")}</span>
                    <span className="track-art-wrap"><TrackArtwork track={track} className="track-art" /><span className="art-play">▶</span></span>
                    <span className="track-meta"><strong>{track.title}</strong><small>{track.artist}</small></span>
                  </button>
                  <span className="track-duration">{track.duration || "—"}</span>
                  <button
                    className={isFavorite(track) ? "heart-button liked" : "heart-button"}
                    onClick={() => toggleFavorite(track)}
                    aria-label={isFavorite(track) ? "Remove from favourites" : "Add to favourites"}
                    title={isFavorite(track) ? "Remove from favourites" : "Add to favourites"}
                  >♥</button>
                  <button className="row-more" onClick={() => playTrack(track)} aria-label="Play track">•••</button>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-state">
              <div className="empty-icon">{view === "library" ? "♡" : view === "search" ? "⌕" : "♫"}</div>
              <strong>{view === "library" ? "Your favourites live here." : view === "search" ? (hasSearched ? "No tracks found." : "Ready when you are.") : "Your listening journey starts here."}</strong>
              <p>{view === "library" ? "Tap the heart beside a track to save it to this browser." : view === "search" ? (hasSearched ? "Try a different song title or artist." : "Type a song or artist in the search bar above.") : "Search for a song and press play to get started."}</p>
              <button className="empty-action" onClick={() => { openView("search"); document.getElementById("music-search")?.focus(); }}>
                Find music <span>↗</span>
              </button>
            </div>
          )}
        </section>

        <footer className="page-footer">
          <span>SubhuMusic Web · Made with ♡ by Subhadip Tantubay</span>
          <span>Music playback is provided by YouTube’s embedded player.</span>
        </footer>
      </main>

      <aside className="player-column">
        <div className="player-topline"><span className="live-dot" /> NOW PLAYING <span className="player-topline-tag">WEB PLAYER</span></div>
        <div className="player-screen">
          {selected ? (
            <iframe
              key={selected.id}
              src={"https://www.youtube-nocookie.com/embed/" + encodeURIComponent(selected.id) + "?autoplay=1&playsinline=1&rel=0"}
              title={"YouTube player for " + selected.title}
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
              referrerPolicy="strict-origin-when-cross-origin"
              allowFullScreen
            />
          ) : (
            <div className="player-placeholder">
              <div className="placeholder-orbit orbit-one" />
              <div className="placeholder-orbit orbit-two" />
              <div className="placeholder-note">♫</div>
              <span>YOUR NEXT SOUNDTRACK</span>
              <strong>Choose a track<br />to begin.</strong>
            </div>
          )}
        </div>

        <div className="now-playing-info">
          <span className="eyebrow">ON YOUR DECK</span>
          {selected ? (
            <>
              <h2>{selected.title}</h2>
              <p>{selected.artist}</p>
              <div className="player-actions">
                <button className={isFavorite(selected) ? "player-heart liked" : "player-heart"} onClick={() => toggleFavorite(selected)}>
                  {isFavorite(selected) ? "♥ Saved" : "♡ Save"}
                </button>
                <a href={"https://music.youtube.com/watch?v=" + encodeURIComponent(selected.id)} target="_blank" rel="noreferrer">
                  Open in YouTube Music ↗
                </a>
              </div>
            </>
          ) : (
            <>
              <h2>A little silence?</h2>
              <p>Search for a song and start listening.</p>
              <button className="player-search-link" onClick={() => document.getElementById("music-search")?.focus()}>Search music ↗</button>
            </>
          )}
        </div>

        <div className="player-note">
          <span>ⓘ</span>
          <p>Playback runs through YouTube’s official embedded player. Availability and ads are controlled by YouTube.</p>
        </div>
      </aside>

      {selected && (
        <div className="bottom-dock">
          <div className="dock-track">
            <TrackArtwork track={selected} className="dock-art" />
            <div><strong>{selected.title}</strong><small>{selected.artist}</small></div>
          </div>
          <div className="dock-center"><span className="dock-wave"><i /><i /><i /><i /><i /></span><span>Playing on YouTube</span></div>
          <a href={"https://music.youtube.com/watch?v=" + encodeURIComponent(selected.id)} target="_blank" rel="noreferrer">Open ↗</a>
        </div>
      )}
    </div>
  );
}
