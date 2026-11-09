# SubhuMusic Web

A standalone browser website for SubhuMusic. This lives in the \`website/\` folder so the Android app and the existing Windows desktop player remain separate.

## Features

- Responsive dark music-discovery interface for desktop and mobile browsers.
- Search for public music-catalogue results.
- Play selected results using YouTube's official embedded player.
- Save favourites and recent tracks in the current browser.
- Open a track directly in YouTube Music.

## Run locally

Requires Node.js 20.9 or newer.

\`\`\`bash
cd website
npm install
npm run dev
\`\`\`

Open http://localhost:3000.

To create a production build:

\`\`\`bash
npm run build
npm start
\`\`\`

## Deployment

Configure the Vercel project with this repository and set its **Root Directory** to \`website\`. The site does not need a Google API key or any environment variables for the initial version.

## Important limitations

- Search uses the community-maintained \`youtubei.js\` library against YouTube Music's undocumented internal catalogue interface. It is not an official YouTube API and can change or be rate-limited.
- Playback uses YouTube's official embedded player. YouTube controls playback availability, ads, region restrictions, and account requirements.
- Favourites and recent tracks are stored locally in the browser. They are not synced with the Android app or a Google account.
- This website does not collect Google passwords or copy the Android app's private cookies.

SubhuMusic is an independent project and is not affiliated with or endorsed by YouTube or Google.
