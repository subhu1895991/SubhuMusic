# SubhuMusic Web

A standalone browser website for SubhuMusic in the `website/` folder. It is separate from the Android app and Windows desktop player.

## Features

- Search public music catalogue.
- Video mode with YouTube's official embedded player.
- Selectable Video and Audio interface modes for YouTube tracks, both staying inside the official embedded player; imported local audio plays directly in SubhuMusic.
- Browser-local liked songs, recently played history, editable playlists, shuffle, repeat and play queue.
- Import local audio into browser storage and export the original file back to your device.
- Optional Google sign-in and cloud sync for liked tracks, playlists and recently played history through Supabase.

## Run locally

Requires Node.js 20.9 or newer.

```bash
cd website
npm install
npm run dev
```

Open http://localhost:3000.

## Supabase cloud sync setup

1. Create or select a dedicated Supabase project for SubhuMusic.
2. Run the SQL in `supabase/migrations/20261109_create_subhumusic_libraries.sql` in that project's SQL Editor.
3. Add these Vercel environment variables for the `subhumusic-web` project (Production, Preview and Development):
   - `NEXT_PUBLIC_SUPABASE_URL` — the project's API URL.
   - `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY` — the project's publishable key (not a service-role key).
4. In Supabase Auth, enable the Google provider. Create a **Web application OAuth client** in Google Cloud, add your website URL to authorised JavaScript origins, and add the Supabase Auth callback URL to authorised redirect URIs. Add the client ID and secret to Supabase's Google provider settings.
5. In Supabase Auth URL Configuration, set the Site URL to your website and allow the website origin as a redirect URL.
6. Redeploy the Vercel project.

Google OAuth cannot be activated with source code alone; it needs a Google OAuth client ID and client secret configured in Supabase. Never store the Google client secret or Supabase service-role key in the browser or in Git.

### What syncs

Liked YouTube tracks, playlists and recently played entries sync per authenticated Supabase user. Imported audio file bytes stay in the current browser's IndexedDB and do not upload to the cloud. The queue and playback position are session-local.

Each account's row is protected by Row Level Security and can only be selected or changed by that signed-in user.

## YouTube playback note

Video and Audio are selectable interface modes for YouTube catalogue tracks, and both keep playback inside the official embedded YouTube player. Audio mode does not extract or hide the video stream; the embedded video remains visible. Imported local audio files play directly through SubhuMusic's audio player. The separate Open in YouTube Music link remains available when you choose to use YouTube Music. YouTube controls stream availability, account requirements and ads. The website does not provide YouTube MP3 extraction or downloads.

## Catalogue

Search uses the community-maintained `youtubei.js` library against YouTube Music's undocumented internal catalogue interface. It is not an official YouTube API and can change or be rate-limited.

SubhuMusic is an independent project and is not affiliated with or endorsed by YouTube or Google.
