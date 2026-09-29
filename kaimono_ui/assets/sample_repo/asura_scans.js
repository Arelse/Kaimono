const API = "https://api.asurascans.com/api";
const SITE = "https://asurascans.com";
const USER_AGENT = "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36";
const CHAPTER_LIMIT = 100; // limit=500 gets hard 403'd by Cloudflare, confirmed live

async function fetchJson(endpoint) {
  try {
    const res = await httpGet(`${API}${endpoint}`, {
      "Accept": "application/json, text/plain, */*",
      "User-Agent": USER_AGENT,
      "Referer": `${SITE}/`
    });
    return JSON.parse(res);
  } catch (e) {
    return null;
  }
}

function extractCover(data) {
  if (!data) return "";
  const url = data.image_url || data.cover_url || data.thumbnail_url || data.poster_url || data.thumbnail || data.image || data.cover || "";
  if (url && !url.startsWith("http")) {
    return SITE + (url.startsWith("/") ? "" : "/") + url;
  }
  return url;
}

function toEntry(item) {
  const data = item.attributes || item || {};
  const slug = data.slug || data.id?.toString() || "";
  return {
    id: slug,
    title: data.name || data.title || "Untitled",
    cover: extractCover(data),
    description: data.synopsis || data.description || "",
    genres: (data.genres || []).map(g => (typeof g === "string" ? g : g.name || "")).filter(Boolean),
    author: data.author || data.artist || null,
    status: (data.status || "unknown").toLowerCase(),
    // The frontend WebView URL uses the new /comics/ structure
    url: `${SITE}/comics/${slug}`,
  };
}

module.popular = async (page, genre) => {
  let endpoint = `/series?order=popular&page=${page || 1}&limit=20`;
  if (genre) endpoint += `&genre=${encodeURIComponent(genre)}`;
  const json = await fetchJson(endpoint);
  if (!json || (!json.data && !json.series && !json.results)) return [];
  return (json.data || json.series || json.results || []).map(toEntry);
};

module.latest = async (page, genre) => {
  let endpoint = `/series?order=latest&page=${page || 1}&limit=20`;
  if (genre) endpoint += `&genre=${encodeURIComponent(genre)}`;
  const json = await fetchJson(endpoint);
  if (!json || (!json.data && !json.series && !json.results)) return [];
  return (json.data || json.series || json.results || []).map(toEntry);
};

module.search = async (query, page, genre) => {
  let endpoint = `/series?name=${encodeURIComponent(query || "")}&page=${page || 1}&limit=20`;
  if (genre) endpoint += `&genre=${encodeURIComponent(genre)}`;
  const json = await fetchJson(endpoint);
  if (!json || (!json.data && !json.series && !json.results)) return [];
  return (json.data || json.series || json.results || []).map(toEntry);
};

module.details = async (id) => {
  const json = await fetchJson(`/series/${id}`);
  // Real response shape is { series: {...}, recommended_series: [...] },
  // not { data } or top-level id/name â€” that's why this always failed before.
  const item = json && (json.series || json.data);

  if (!item) {
    return {
      id: id,
      title: "Load Error",
      cover: "",
      description: "Failed to load details from /series. The API request may have failed.",
      genres: [],
      author: null,
      status: "unknown",
      url: `${SITE}/comics/${id}`
    };
  }

  return toEntry(item);
};

module.chunks = async (id) => {
  const json = await fetchJson(`/series/${id}/chapters?limit=${CHAPTER_LIMIT}`);
  if (!json) return [];

  const list = json.data || json.chapters || [];
  return list.map(ch => ({
    // Carries both the series slug and chapter number â€” pages() needs
    // /series/{slug}/chapters/{number}, not the chapter's own UUID slug.
    id: `${id}::${ch.number}`,
    title: ch.title || `Chapter ${ch.number}`,
    number: parseFloat(ch.number) || 0,
    uploadDate: ch.published_at || ch.created_at || null,
  }));
};

module.pages = async (chunkId) => {
  const [seriesId, number] = chunkId.split("::");
  const json = await fetchJson(`/series/${seriesId}/chapters/${number}`);
  if (!json) return [];

  const chapter = json.data && json.data.chapter;
  const pages = (chapter && chapter.pages) || [];
  return pages.map(img => (typeof img === "string" ? img : img.url || ""));
};

module.streams = async (chunkId) => [];

module.genres = async () => {
  return [
    { id: "action", name: "Action" },
    { id: "adventure", name: "Adventure" },
    { id: "comedy", name: "Comedy" },
    { id: "fantasy", name: "Fantasy" },
    { id: "martial-arts", name: "Martial Arts" },
    { id: "reincarnation", name: "Reincarnation" },
    { id: "sci-fi", name: "Sci-fi" }
  ];
};
