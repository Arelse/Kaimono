const API = "https://api.asurascans.com/api";
const SITE = "https://asurascans.com";
const USER_AGENT = "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36";

// Cache details fetched during directory list so we don't need a separate details API call
const cache = new Map();

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
  const entry = {
    id: slug,
    title: data.name || data.title || "Untitled",
    cover: extractCover(data),
    description: data.synopsis || data.description || "",
    genres: (data.genres || []).map(g => (typeof g === "string" ? g : g.name || "")).filter(Boolean),
    author: data.author || data.artist || null,
    status: (data.status || "unknown").toLowerCase(),
    url: `${SITE}/comics/${slug}`,
  };

  if (slug) {
    cache.set(slug, entry);
  }
  return entry;
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
  // If we already have the details from browsing the directory, use them immediately
  if (cache.has(id)) {
    return cache.get(id);
  }

  // Fallback: try different paths Asura uses for individual series
  let json = await fetchJson(`/series/${id}`);
  if (!json || (!json.data && !json.id && !json.name)) {
    json = await fetchJson(`/comics/${id}`);
  }
  if (!json || (!json.data && !json.id && !json.name)) {
    json = await fetchJson(`/series?slug=${encodeURIComponent(id)}`);
  }

  if (json) {
    const item = Array.isArray(json.data) ? json.data[0] : (json.data || json);
    if (item) return toEntry(item);
  }

  return {
    id: id,
    title: id.replace(/-[a-f0-9]{6,}$/i, "").replace(/-/g, " "),
    cover: "",
    description: "",
    genres: [],
    author: null,
    status: "unknown",
    url: `${SITE}/comics/${id}`
  };
};

module.chunks = async (id) => {
  // Try chapters via /series, then /comics
  let json = await fetchJson(`/series/${id}/chapters?limit=500`);
  if (!json || (!json.data && !json.chapters)) {
    json = await fetchJson(`/comics/${id}/chapters?limit=500`);
  }
  if (!json) return [];

  const list = json.data || json.chapters || [];
  return list.map(ch => ({
    id: ch.slug || ch.id?.toString() || `${id}-chapter-${ch.number || ch.name}`,
    title: ch.title || (ch.name ? `Chapter ${ch.name}` : `Chapter ${ch.number || "?"}`),
    number: parseFloat(ch.number || ch.name) || 0,
    uploadDate: ch.created_at || ch.release_date || null,
  }));
};

module.pages = async (chunkId) => {
  const json = await fetchJson(`/chapters/${chunkId}`);
  if (!json) return [];
  
  const data = json.data || json.chapter || json;
  const pages = data.pages || data.images || [];
  return pages.map(img => (typeof img === "string" ? img : img.url || img.src || ""));
};

module.streams = async (chunkId) => [];

module.genres = async () => [
  { id: "action", name: "Action" },
  { id: "adventure", name: "Adventure" },
  { id: "comedy", name: "Comedy" },
  { id: "fantasy", name: "Fantasy" },
  { id: "martial-arts", name: "Martial Arts" },
  { id: "reincarnation", name: "Reincarnation" },
  { id: "sci-fi", name: "Sci-fi" }
];
