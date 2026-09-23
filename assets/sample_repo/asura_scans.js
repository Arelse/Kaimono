const API = "https://api.asurascans.com/api";
const SITE = "https://asurascans.com";
const USER_AGENT = "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36";

async function fetchJson(endpoint) {
  try {
    const res = await httpGet(`${API}${endpoint}`, {
      "Accept": "application/json, text/plain, */*",
      "User-Agent": USER_AGENT,
      "Referer": `${SITE}/`
    });
    return JSON.parse(res);
  } catch (e) { return null; }
}

async function fetchHtml(url) {
  try {
    return await httpGet(url, { "User-Agent": USER_AGENT, "Referer": `${SITE}/` });
  } catch (e) { return ""; }
}

function cleanText(str) {
  return (str || "").replace(/<[^>]*>/g, "").replace(/&[^;]+;/g, "").trim();
}

function extractCover(data) {
  if (!data) return "";
  const url = data.image_url || data.cover_url || data.thumbnail_url || data.poster_url || data.thumbnail || data.image || data.cover || "";
  if (url && !url.startsWith("http")) return SITE + (url.startsWith("/") ? "" : "/") + url;
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
    genres: [],
    author: null,
    status: "unknown",
    url: `${SITE}/series/${slug}`,
  };
}

async function fallbackBlocker() {
  return [{
    id: "cloudflare-bypass",
    title: "⚠️ Tap here to bypass Cloudflare",
    cover: "",
    description: "Cloudflare is blocking the connection.",
    url: SITE
  }];
}

module.popular = async (page, genre) => {
  let ep = `/series?order=popular&page=${page || 1}&limit=20`;
  if (genre) ep += `&genre=${encodeURIComponent(genre)}`;
  const json = await fetchJson(ep);
  if (!json || (!json.data && !json.series && !json.results)) return fallbackBlocker();
  return (json.data || json.series || json.results || []).map(toEntry);
};

module.latest = async (page, genre) => {
  let ep = `/series?order=latest&page=${page || 1}&limit=20`;
  if (genre) ep += `&genre=${encodeURIComponent(genre)}`;
  const json = await fetchJson(ep);
  if (!json || (!json.data && !json.series && !json.results)) return fallbackBlocker();
  return (json.data || json.series || json.results || []).map(toEntry);
};

module.search = async (query, page, genre) => {
  let ep = `/series?name=${encodeURIComponent(query || "")}&page=${page || 1}&limit=20`;
  if (genre) ep += `&genre=${encodeURIComponent(genre)}`;
  const json = await fetchJson(ep);
  if (!json || (!json.data && !json.series && !json.results)) return fallbackBlocker();
  return (json.data || json.series || json.results || []).map(toEntry);
};

module.details = async (id) => {
  if (id === "cloudflare-bypass") {
     return { 
       id, 
       title: "Cloudflare Blocked", 
       cover: "", 
       description: "Please tap the 'WebView' button above, verify you are human, let the site load, and then go back and refresh the main page.", 
       genres: [], author: null, status: "unknown", url: SITE 
     };
  }
  
  const url = `${SITE}/series/${id}`;
  const html = await fetchHtml(url);

  if (!html || html.includes("Just a moment...") || html.includes("Cloudflare")) {
    return {
      id: id,
      title: "Cloudflare Block",
      cover: "",
      description: "Asura Scans blocked the request. Please tap 'WebView', wait for the page to load, and then pull down to refresh.",
      genres: [], author: null, status: "unknown", url: url
    };
  }

  const titleMatch = html.match(/<span class="text-xl font-bold[^"]*">([\s\S]*?)<\/span>/i) || html.match(/<h1[^>]*>([\s\S]*?)<\/h1>/i) || ["", id];
  const coverMatch = html.match(/<img[^>]+alt="poster"[^>]+src="([^">]+)"/i) || html.match(/<img[^>]+class="[^"]*rounded-md[^"]*"[^>]+src="([^">]+)"/i);
  const descMatch = html.match(/<span class="font-medium text-sm text-[#a2a2a2][^"]*">([\s\S]*?)<\/span>/i) || html.match(/<p[^>]*>([\s\S]*?)<\/p>/i);
  
  return {
    id: id,
    title: cleanText(titleMatch[1]),
    cover: coverMatch ? coverMatch[1] : "",
    description: descMatch ? cleanText(descMatch[1]) : "",
    genres: [],
    author: null,
    status: "unknown",
    url: url,
  };
};

module.chunks = async (id) => {
  if (id === "cloudflare-bypass") return [];
  const html = await fetchHtml(`${SITE}/series/${id}`);
  if (!html) return [];

  const chapters = [];
  const chRegex = /<a[^>]+href="(?:\/series\/[^\/]+\/chapter\/|https?:\/\/[^\/]+\/series\/[^\/]+\/chapter\/|https?:\/\/[^\/]+\/chapter\/|\/chapter\/)([^"\/]+)"[^>]*>([\s\S]*?)<\/a>/gi;
  let match;

  while ((match = chRegex.exec(html)) !== null) {
    const chSlug = match[1];
    const inner = match[2];

    const titleMatch = inner.match(/<h3[^>]*>([\s\S]*?)<\/h3>/i) || inner.match(/Chapter\s*[\d.]+/i);
    const num = parseFloat(chSlug.replace(/[^0-9.]/g, "")) || 0;

    chapters.push({
      id: chSlug,
      title: titleMatch ? cleanText(titleMatch[0] || titleMatch[1]) : `Chapter ${num}`,
      number: num,
      uploadDate: null,
    });
  }
  return chapters.reverse();
};

module.pages = async (chunkId) => {
  const html = await fetchHtml(`${SITE}/chapter/${chunkId}`);
  const pages = [];
  
  const imgRegex = /<img[^>]+src="(https?:\/\/[^">]+)"[^>]+alt="chapter-[^"]*"[^>]*>/gi;
  let match;

  while ((match = imgRegex.exec(html)) !== null) {
    pages.push(match[1]);
  }

  if (pages.length === 0) {
    const altRegex = /<img[^>]+src="(https?:\/\/[^">]+(?:ggpht|asura|storage)[^">]+)"/gi;
    while ((match = altRegex.exec(html)) !== null) {
      pages.push(match[1]);
    }
  }
  return pages;
};

module.streams = async () => [];
module.genres = async () => [];
