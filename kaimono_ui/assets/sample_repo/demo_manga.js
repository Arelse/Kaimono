// Demo extension showing the full contract a real source implements.
// Host this file (and index.json) anywhere static files can be served,
// point ExtensionRepo at that index.json, and it becomes installable.
//
// Real sources replace the fake data below with actual `httpGet(url)`
// calls + HTML/JSON parsing for the target site.

class Extension {
  async popular(page) {
    return [
      { id: "demo-1", title: "Sample Title " + page, cover: "", description: "Demo entry", genres: ["Action"], status: "ongoing" }
    ];
  }

  async latest(page) {
    return this.popular(page);
  }

  async search(query, page) {
    return [
      { id: "demo-search-1", title: "Result for " + query, cover: "", status: "ongoing" }
    ];
  }

  async details(id) {
    return { id, title: "Sample Title", description: "Full description here.", genres: ["Action", "Drama"], status: "ongoing" };
  }

  async chunks(id) {
    return [
      { id: id + "-ch1", title: "Chapter 1", number: 1, uploadDate: new Date().toISOString() }
    ];
  }

  async pages(chunkId) {
    // Real source: parse chapter page HTML/JSON returned by httpGet()
    // and return the ordered image URLs found there.
    return [
      "https://picsum.photos/seed/" + chunkId + "-1/800/1200",
      "https://picsum.photos/seed/" + chunkId + "-2/800/1200"
    ];
  }

  async streams(chunkId) {
    return []; // not used by a manga source
  }
}

module.popular = (p) => new Extension().popular(p);
module.latest = (p) => new Extension().latest(p);
module.search = (q, p) => new Extension().search(q, p);
module.details = (id) => new Extension().details(id);
module.chunks = (id) => new Extension().chunks(id);
module.pages = (id) => new Extension().pages(id);
module.streams = (id) => new Extension().streams(id);
