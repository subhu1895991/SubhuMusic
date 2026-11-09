import { Innertube } from "youtubei.js";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

type MusicResult = {
  id: string;
  title: string;
  artist: string;
  duration: string;
  thumbnail: string;
};

let clientPromise: ReturnType<typeof Innertube.create> | null = null;

async function getMusicClient() {
  if (!clientPromise) {
    clientPromise = Innertube.create({
      lang: "en",
      enable_session_cache: true
    }).catch((error) => {
      clientPromise = null;
      throw error;
    });
  }

  return clientPromise;
}

function safeText(value: unknown, fallback = ""): string {
  return typeof value === "string" ? value.trim().slice(0, 240) : fallback;
}

export async function GET(request: Request) {
  const url = new URL(request.url);
  const query = (url.searchParams.get("q") ?? "").trim().slice(0, 100);

  if (!query) {
    return Response.json({ items: [] satisfies MusicResult[] });
  }

  try {
    const music = await getMusicClient();
    const search = await music.music.search(query, { type: "song" });
    const rows = (search.songs?.contents ?? []) as Array<Record<string, any>>;

    const items = rows
      .map((item): MusicResult => {
        const artists = Array.isArray(item.artists)
          ? item.artists.map((artist: { name?: string }) => safeText(artist?.name)).filter(Boolean).join(", ")
          : "";

        const thumbnails = Array.isArray(item.thumbnails) ? item.thumbnails : [];
        const thumbnail = thumbnails.length
          ? safeText(thumbnails[thumbnails.length - 1]?.url)
          : "";

        return {
          id: safeText(item.id),
          title: safeText(item.title, "Untitled track"),
          artist: artists || safeText(item.author?.name) || safeText(item.subtitle?.text, "Unknown artist"),
          duration: safeText(item.duration?.text),
          thumbnail
        };
      })
      .filter((item) => /^[a-zA-Z0-9_-]{11}$/.test(item.id) && item.title)
      .slice(0, 24);

    return Response.json(
      { items },
      {
        headers: {
          "Cache-Control": "public, s-maxage=120, stale-while-revalidate=300"
        }
      }
    );
  } catch (error) {
    console.error("SubhuMusic search failed:", error);
    return Response.json(
      { items: [], error: "Music search is temporarily unavailable. Please try again." },
      { status: 502 }
    );
  }
}
