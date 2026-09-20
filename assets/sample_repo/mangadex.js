// Real working source backed by MangaDex's official public API
// (https://api.mangadex.org) — built for third-party apps, no scraping.

const API = "https://api.mangadex.org";
const COVERS = "https://uploads.mangadex.org/covers";

function pickTitle(attrs) {
  if (!attrs || !attrs.title) return "Untitled";
  return attrs.title.en || Object.values(attrs.title)[0] || "Untitled";
}

function pickDescription(attrs) {
  if (!attrs || !attrs.description) return "";
  return attrs.description.en || Object.values(attrs.description)[0] || "";
}

function coverUrlFor(manga) {
  const rel = (manga.relationships || []).find(r => r.type === "cover_art");
  if (!rel || !rel.attributes || !rel.attributes.fileName) return "";
  return `${COVERS}/${manga.id}/${rel.attributes.fileName}.256.jpg`;
}

function authorFor(manga) {
  const rel = (manga.relationships || []).find(r => r.type === "author");
  return rel && rel.attributes ? rel.attributes.name : null;
}

function toEntry(manga) {
  const attrs = manga.attributes || {};
  return {
    id: manga.id,
    title: pickTitle(attrs),
    cover: coverUrlFor(manga),
    description: pickDescription(attrs),
    genres: (attrs.tags || []).map(t => (t.attributes && t.attributes.name && t.attributes.name.en) || "").filter(Boolean),
    author: authorFor(manga),
    status: attrs.status || "unknown",
  };
}

async function fetchMangaList(query) {
  const res = await httpGet(`${API}/manga?${query}`, { "Accept": "application/json" });
  const json = JSON.parse(res);
  return (json.data || []).map(toEntry);
}

module.popular = async (page) => {
  const offset = (page - 1) * 20;
  return fetchMangaList(`order[followedCount]=desc&limit=20&offset=${offset}&contentRating[]=safe&contentRating[]=suggestive&includes[]=cover_art&includes[]=author`);
};

module.latest = async (page) => {
  const offset = (page - 1) * 20;
  return fetchMangaList(`order[latestUploadedChapter]=desc&limit=20&offset=${offset}&contentRating[]=safe&contentRating[]=suggestive&includes[]=cover_art&includes[]=author`);
};

module.search = async (query, page) => {
  const offset = (page - 1) * 20;
  return fetchMangaList(`title=${encodeURIComponent(query)}&limit=20&offset=${offset}&contentRating[]=safe&contentRating[]=suggestive&includes[]=cover_art&includes[]=author`);
};

module.details = async (id) => {
  const res = await httpGet(`${API}/manga/${id}?includes[]=cover_art&includes[]=author`, { "Accept": "application/json" });
  const json = JSON.parse(res);
  return toEntry(json.data);
};

module.chunks = async (id) => {
  const res = await httpGet(
    `${API}/manga/${id}/feed?translatedLanguage[]=en&order[chapter]=asc&limit=500&contentRating[]=safe&contentRating[]=suggestive`,
    { "Accept": "application/json" }
  );
  const json = JSON.parse(res);
  return (json.data || []).map(ch => ({
    id: ch.id,
    title: ch.attributes.title || `Chapter ${ch.attributes.chapter || "?"}`,
    number: parseFloat(ch.attributes.chapter) || 0,
    uploadDate: ch.attributes.publishAt || null,
  }));
};

module.pages = async (chunkId) => {
  const res = await httpGet(`${API}/at-home/server/${chunkId}`, { "Accept": "application/json" });
  const json = JSON.parse(res);
  const base = json.baseUrl;
  const hash = json.chapter.hash;
  return (json.chapter.data || []).map(filename => `${base}/data/${hash}/${filename}`);
};

module.streams = async (chunkId) => {
  return []; // manga source, no video
};
