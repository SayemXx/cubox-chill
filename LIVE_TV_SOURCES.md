# Live TV sources and behavior

Reviewed November 21, 2026. The Live TV tab has BD, India, Sports, Cartoons, Movies, Music, News and Add new. The default source for each category is the corresponding IPTV-org community playlist:

- BD: https://iptv-org.github.io/iptv/countries/bd.m3u
- India: https://iptv-org.github.io/iptv/countries/in.m3u
- Sports: https://iptv-org.github.io/iptv/categories/sports.m3u
- Cartoons (Kids feed): https://iptv-org.github.io/iptv/categories/kids.m3u
- Movies: https://iptv-org.github.io/iptv/categories/movies.m3u
- Music: https://iptv-org.github.io/iptv/categories/music.m3u
- News: https://iptv-org.github.io/iptv/categories/news.m3u
- Project, provenance and removal requests: https://github.com/iptv-org/iptv
- Directory license: https://github.com/iptv-org/iptv/blob/master/LICENSE

The original three playlists returned HTTP 200 during research (45, 873 and 537 entries respectively, before app deduplication/filtering). This checks directory availability, not every channel or its rights in every country. IPTV-org describes its entries as user-submitted publicly available streams. Directory licensing does not grant rights to the underlying broadcasts. No paid-channel access, geoblock bypass or fixed uptime is promised.

## Official picks

Official picks are separate from community sources. Direct feeds are provided for India TV, NDTV 24x7, NDTV India, India Today and Red Bull TV. Their manifests returned valid HLS during research. Public CDN links were cross-checked against the IPTV-org directory and the broadcasters' viewing pages. Their availability may change independently of the app.

- India TV: https://www.indiatvnews.com/livetv
- NDTV 24x7: https://www.ndtv.com/livetv-ndtv24x7
- NDTV India: https://ndtv.in/livetv
- India Today: https://www.indiatoday.in/livetv
- Red Bull TV: https://www.redbull.tv/en_US/epg

BTV's official site lists BTV, BTV News, BTV Chattogram and Sangsad Television. The public page's direct URLs returned HTTP 401 without a broadcaster session. These entries therefore open the official site, and are explicitly labeled as website links rather than native player streams. Jamuna TV and Channel 24 likewise open their official viewing pages.

- BTV: https://btvlive.gov.bd/
- Jamuna TV: https://www.jamunanews.tv/live
- Channel 24: https://www.channel24bd.tv/live

Sports includes official service links for FIFA+ on DAZN and DD Sports/WAVES alongside the direct Red Bull TV feed. Accounts and regional rights may apply. These links are not represented as direct channel streams.

- FIFA+ information: https://www.dazn.com/en-GB/help/articles/36029102113053-fifa-on-dazn-everything-you-need-to-know
- WAVES: https://prasarbharati.gov.in/en/waves/

## Import, storage and playback

Users can import HTTP/HTTPS M3U playlist URLs (including GitHub blob URLs, converted to raw content), direct HLS URLs, or M3U files through Android's document picker. Only HTTP/HTTPS streams are accepted; unsupported DRM directives are skipped. Imports are bounded at 5 MB and 10,000 channels, with up to 30 saved custom playlists. Relative URLs use the final redirected playlist address. Playlist credentials remain in private app storage and must never be included in error messages or analytics.

Channel lists use a 24-hour disk cache, immediately display saved data, and retain it when refresh fails. Logos use the existing artwork cache. Only 50 channel rows are rendered per page. Favorites, search, source selection and removal are available. Live playback uses the existing player, starts at the default live position instead of a movie resume timestamp, offers LIVE to return to the edge, and retries three consecutive playback errors. Provider/region failures ultimately show a clear error. External official pages require an installed browser or compatible app; the app does not scrape protected video or extract YouTube streams.

## v0.7.1 category expansion

The four new category playlists returned valid EXTM3U content on November 21, 2026: Kids 458, Movies 782, Music 784, News 1,073 entries before deduplication/filtering. Cartoons uses the broader Kids feed, which also includes children's programming. These are live channels, not the on-demand Movies library. The News category also offers existing official news picks. Categories without curated official entries do not show an empty Official picks source. All categories share search, favorites, cached lists and custom imports. The horizontal category strip retains the selected category in view. Counts and stream availability can change.
