const SITE = "https://asurascans.com";
const USER_AGENT = "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36";

function cleanText(str) {
  return (str || "")
    .replace(/<[^>]*>/g, "")
    .replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&#039;/g, "'")
    .replace(/&quot;/g, '"')
    .trim();
}

async function fetchHtml(url) {
  try {
    return await httpGet(url, { 
      "User-Agent": USER_AGENT, 
      "Referer": `${SITE}/` 
    });
  } catch (e) {
    return ""; 
  }
}

function parseMangaCards(html) {
  const results = [];
  const cardRegex = /<a[^>]+href="(?:\/series\/|https?:\/\/[^\/]+\/series\/)([^"\/]+)"[^>]*>([\s\S]*?)<\/a>/gi;
  let match;

  while ((match = cardRegex.exec(html)) !== null) {
    const slug = match[1];
    const inner = match[2];

    const titleMatch = inner.match(/<span[^>]*class="[^"]*font-bold[^"]*"[^>]*>([\s\S]*?)<\/span>/i) 
                    || inner.match(/<h5[^>]*>([\s\S]*?)<\/h5>/i)
                    || inner.match(/title="([^"]+)"/i)
                    || inner.match(/<div[^>]*class="[^"]*font-bold[^"]*"[^>]*>([\s\S]*?)<\/div>/i);
    
    const imgMatch = inner.match(/<img[^>]+src="([^">]+)"/i);

    if (slug && titleMatch) {
      let coverUrl = imgMatch ? imgMatch[1] : "";
      if (coverUrl && !coverUrl.startsWith("http")) {
        coverUrl = SITE + (coverUrl.startsWith("/") ? "" : "/") + coverUrl;
      }

      results.push({
        id: slug,
        title: cleanText(titleMatch[1]),
        cover: coverUrl,
        description: "",
        genres: [],
        author: null,
        status: "unknown",
        url: `${SITE}/series/${slug}`,
      });
    }
  }
  return Array.from(new Map(results.map(item => [item.id, item])).values());
}

module.popular = async (page, genre) => {
  const html = await fetchHtml(`${SITE}/series?page=${page || 1}&order=popular${genre ? `&genre=${genre}` : ""}`);
  return parseMangaCards(html);
};

module.latest = async (page, genre) => {
  const html = await fetchHtml(`${SITE}/series?page=${page || 1}&order=update${genre ? `&genre=${genre}` : ""}`);
  return parseMangaCards(html);
};

module.search = async (query, page, genre) => {
  const html = await fetchHtml(`${SITE}/series?page=${page || 1}&name=${encodeURIComponent(query || "")}${genre ? `&genre=${genre}` : ""}`);
  return parseMangaCards(html);
};

module.details = async (id) => {
  const url = `${SITE}/series/${id}`;
  const html = await fetchHtml(url);

  // If Cloudflare blocks the request, trigger the WebView failsafe
  if (!html || html.includes("Just a moment...") || html.includes("Cloudflare")) {
    return {
      id: id,
      title: "Cloudflare Block",
      cover: "",
      description: "Asura Scans blocked the request. Please tap 'WebView', verify you are human, and refresh this page.",
      genres: [],
      author: null,
      status: "unknown",
      url: url
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

module.streams = async (chunkId) => [];
module.genres = async () => [];
