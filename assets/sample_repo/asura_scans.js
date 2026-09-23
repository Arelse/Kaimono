const API = "https://api.asurascans.com/api";
const SITE = "https://asurascans.com";
const USER_AGENT = "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36";

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
    cache.set(slug, { entry, raw: item });
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
  if (cache.has(id)) {
    return cache.get(id).entry;
  }

  let json = await fetchJson(`/series/${id}`);
  if (!json || (!json.data && !json.id && !json.name)) {
    json = await fetchJson(`/comics/${id}`);
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
  // Check if chapters are embedded in cache first
  if (cache.has(id)) {
    const raw = cache.get(id).raw;
    const embedded = raw.chapters || raw.chapterList || (raw.attributes && raw.attributes.chapters);
    if (Array.isArray(embedded) && embedded.length > 0) {
      return parseChapterList(embedded, id);
    }
  }

  // Try multiple chapter endpoints sequentially
  let json = await fetchJson(`/series/${id}/chapters?limit=500`);
  if (!json || (!json.data && !json.chapters && !Array.isArray(json))) {
    json = await fetchJson(`/comics/${id}/chapters?limit=500`);
  }
  if (!json || (!json.data && !json.chapters && !Array.isArray(json))) {
    json = await fetchJson(`/series/${id}`);
  }
  if (!json || (!json.data && !json.chapters && !Array.isArray(json))) {
    json = await fetchJson(`/comics/${id}`);
  }

  if (!json) return [];

  const list = json.chapters || json.data?.chapters || json.chapterList || (Array.isArray(json) ? json : json.data || json.results || []);
  
  if (!Array.isArray(list)) return [];

  return parseChapterList(list, id);
};

function parseChapterList(list, id) {
  return list.map(ch => {
    const chData = ch.attributes || ch;
    const chSlug = chData.slug || chData.id?.toString() || chData.chapter_slug || `${id}-chapter-${chData.number || chData.name}`;
    const chNum = parseFloat(chData.number || chData.chapter || chData.name) || 0;
    const chTitle = chData.title || (chData.name ? `Chapter ${chData.name}` : `Chapter ${chData.number || chData.chapter || "?"}`);
    
    return {
      id: chSlug,
      title: chTitle,
      number: chNum,
      uploadDate: chData.created_at || chData.release_date || chData.updated_at || null,
    };
  });
}

module.pages = async (chunkId) => {
  let json = await fetchJson(`/chapters/${chunkId}`);
  if (!json) {
    json = await fetchJson(`/chapter/${chunkId}`);
  }
  if (!json) return [];
  
  const data = json.data || json.chapter || json;
  const pages = data.pages || data.images || data.chapter_images || [];
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
