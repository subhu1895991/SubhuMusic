# SubhuMusic Web

A standalone browser website for SubhuMusic in the \`website/\` folder. It is separate from the Android app and Windows desktop player.

## Features

- Responsive music discovery and public catalogue search.
- YouTube's official embedded playback with play/pause, previous/next, seek, volume, shuffle, repeat and play queue.
- Browser-local liked songs and editable playlists.
- Export playlist metadata as a JSON backup.
- Import audio files from your device into browser storage (IndexedDB), play them, and export the original file back to your device.
- Recently played history.
- Responsive desktop and mobile layouts.

## Run locally

Requires Node.js 20.9 or newer.

\`\`\`bash
cd website
npm install
npm run dev
\`\`\`

Open http://localhost:3000.

For a production build:

\`\`\`bash
npm run build
npm start
\`\`\`

## Deployment

The Vercel project should use this repository with Root Directory set to \`website\`, framework Next.js, and Node.js 22.x.

## Storage and account behaviour

- Liked tracks, recent history and playlists are saved in this browser. The live queue lasts for the current page session. Playlists can be exported as JSON backups.
- Imported audio file data is stored in the browser's IndexedDB. Clearing site data or using a different browser/device removes or does not transfer those local files.
- This version does not yet provide account sign-in or cloud sync with the Android app.

## Download policy

The website can export the original audio files a user imported into their own browser. It does not extract or convert YouTube streams into downloadable audio files. YouTube search results play through YouTube's official embedded player; YouTube controls playback availability and download entitlements.

## Catalogue

Search uses the community-maintained \`youtubei.js\` library against YouTube Music's undocumented internal catalogue interface. It is not an official YouTube API and can change or be rate-limited.

SubhuMusic is an independent project and is not affiliated with or endorsed by YouTube or Google.
