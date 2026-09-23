// Asura Scans module using the JSON API
const API = "https://api.asurascans.com/api";
const SITE = "https://asurascans.com";

function toEntry(item) {
  const data = item.attributes || item;
  return {
    id: item.slug || item.id?.toString() || "",
    title: data.name || data.title || "Untitled",
    cover: data.cover_url || data.poster_url || data.thumbnail || "",
    description: data.description || data.synopsis || "",
    genres: (data.genres || []).map(g => (typeof g === "string" ? g : g.name || "")).filter(Boolean),
    author: data.author || data.artist || null,
    status: (data.status || "unknown").toLowerCase(),
    url: `${SITE}/series/${item.slug || item.id}`,
  };
}

async function fetchJson(endpoint) {
  const res = await httpGet(`${API}${endpoint}`, {
    "Accept": "application/json",
    "Referer": `${SITE}/`,
  });
  return JSON.parse(res);
}

module.popular = async (page, genre) => {
  let endpoint = `/series?order=popular&page=${page || 1}&limit=20`;
  if (genre) endpoint += `&genre=${encodeURIComponent(genre)}`;
  const json = await fetchJson(endpoint);
  return (json.data || json.series || json.results || []).map(toEntry);
};

module.latest = async (page, genre) => {
  let endpoint = `/series?order=latest&page=${page || 1}&limit=20`;
  if (genre) endpoint += `&genre=${encodeURIComponent(genre)}`;
  const json = await fetchJson(endpoint);
  return (json.data || json.series || json.results || []).map(toEntry);
};

module.search = async (query, page, genre) => {
  let endpoint = `/series?name=${encodeURIComponent(query || "")}&page=${page || 1}&limit=20`;
  if (genre) endpoint += `&genre=${encodeURIComponent(genre)}`;
  const json = await fetchJson(endpoint);
  return (json.data || json.series || json.results || []).map(toEntry);
};

module.details = async (id) => {
  const json = await fetchJson(`/series/${id}`);
  const item = json.data || json;
  return toEntry(item);
};

module.chunks = async (id) => {
  const json = await fetchJson(`/series/${id}/chapters?limit=500`);
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
  const data = json.data || json.chapter || json;
  const pages = data.pages || data.images || [];
  return pages.map(img => (typeof img === "string" ? img : img.url || img.src || ""));
};

module.streams = async (chunkId) => {
  return [];
};

module.genres = async () => {
  try {
    const json = await fetchJson(`/genres`);
    const list = json.data || json.genres || [];
    return list.map(g => ({
      id: g.slug || g.id?.toString() || g.name,
      name: g.name,
    }));
  } catch {
    return [
      { id: "action", name: "Action" },
      { id: "adventure", name: "Adventure" },
      { id: "comedy", name: "Comedy" },
      { id: "fantasy", name: "Fantasy" },
      { id: "martial-arts", name: "Martial Arts" },
      { id: "reincarnation", name: "Reincarnation" },
      { id: "sci-fi", name: "Sci-fi" },
    ];
  }
};
