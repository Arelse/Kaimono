const SITE = "https://asurascans.com";

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

function parseMangaCards(html) {
  const results = [];
  const cardRegex = /<a[^>]+href="(?:\/series\/|https?:\/\/[^\/]+\/series\/)([^"\/]+)"[^>]*>([\s\S]*?)<\/a>/gi;
  let match;

  while ((match = cardRegex.exec(html)) !== null) {
    const slug = match[1];
    const inner = match[2];

    const titleMatch = inner.match(/<span[^>]*class="[^"]*font-bold[^"]*"[^>]*>([\s\S]*?)<\/span>/i) 
                    || inner.match(/<div[^>]*class="[^"]*font-bold[^"]*"[^>]*>([\s\S]*?)<\/div>/i)
                    || inner.match(/title="([^"]+)"/i);
    
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
  try {
    const url = `${SITE}/series?page=${page || 1}&order=popular${genre ? `&genre=${genre}` : ""}`;
    const html = await httpGet(url, { "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)" });
    return parseMangaCards(html);
  } catch (e) { return []; }
};

module.latest = async (page, genre) => {
  try {
    const url = `${SITE}/series?page=${page || 1}&order=update${genre ? `&genre=${genre}` : ""}`;
    const html = await httpGet(url, { "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)" });
    return parseMangaCards(html);
  } catch (e) { return []; }
};

module.search = async (query, page, genre) => {
  try {
    const url = `${SITE}/series?page=${page || 1}&name=${encodeURIComponent(query || "")}${genre ? `&genre=${genre}` : ""}`;
    const html = await httpGet(url, { "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)" });
    return parseMangaCards(html);
  } catch (e) { return []; }
};

module.details = async (id) => {
  try {
    const url = `${SITE}/series/${id}`;
    const html = await httpGet(url, { "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)" });

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
  } catch (e) {
    return {
      id: id,
      title: "Load Error",
      cover: "",
      description: "Failed to load details. The site might have blocked the request.",
      genres: [],
      author: null,
      status: "unknown",
      url: `${SITE}/series/${id}`
    };
  }
};

module.chunks = async (id) => {
  try {
    const url = `${SITE}/series/${id}`;
    const html = await httpGet(url, { "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)" });

    const chapters = [];
    const chRegex = /<a[^>]+href="(?:\/series\/[^\/]+\/chapter\/|https?:\/\/[^\/]+\/series\/[^\/]+\/chapter\/|https?:\/\/[^\/]+\/chapter\/)([^"\/]+)"[^>]*>([\s\S]*?)<\/a>/gi;
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
  } catch (e) { return []; }
};

module.pages = async (chunkId) => {
  try {
    const url = `${SITE}/chapter/${chunkId}`; 
    const html = await httpGet(url, { "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)" });

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
  } catch (e) { return []; }
};

module.streams = async (chunkId) => [];

module.genres = async () => {
  return [
    { id: "action", name: "Action" },
    { id: "adventure", name: "Adventure" },
    { id: "comedy", name: "Comedy" },
    { id: "fantasy", name: "Fantasy" },
    { id: "martial-arts", name: "Martial Arts" },
    { id: "shounen", name: "Shounen" }
  ];
};

