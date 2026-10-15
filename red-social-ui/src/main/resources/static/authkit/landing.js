/*
 * Landing pública — comportamiento de la portada del Edificio C.
 *
 * 1) Slider de imágenes (izquierda).
 *    Las imágenes viven en /authkit/slides/. Para reemplazarlas basta con
 *    soltar archivos con el mismo nombre base y distinta extensión, por
 *    ejemplo: slide-1.png, slide-2.jpg, slide-3.webp ... y se usan
 *    automáticamente (el .svg queda solo como respaldo).
 *
 * 2) Mapa MapLibre (derecha), siguiendo la especificación del mapa de /buscar:
 *    - MapLibre GL JS 6 (ES module) servido localmente en /maplibre/.
 *    - Worker ES module registrado con setWorkerUrl.
 *    - Estilo de calles: OpenFreeMap Liberty (satélite: Esri World Imagery).
 *    - Recolor de vías (ROAD_RECOLORS) y pitch 55° desde zoom 18.
 *    - Controles NavigationControl + ScaleControl, atribución compacta.
 *    - Fallback a teselas raster OSM si el estilo falla o pasan 8 s.
 *    - Centrado y bloqueado al polígono del Edificio C (maxBounds).
 */
(function () {
    "use strict";

    /* ------------------------------------------------------------------ *
     * Zona permitida (polígono del Edificio C · UNET)
     * ------------------------------------------------------------------ */
    var ZONE_RING = [
        [-72.2031715, 7.7925594],
        [-72.2029124, 7.7922196],
        [-72.1991019, 7.7917439],
        [-72.1981417, 7.792129],
        [-72.1976082, 7.7959194],
        [-72.201373, 7.7963497],
        [-72.2021122, 7.7944772],
        [-72.2031715, 7.7925594]
    ];

    function zoneBounds(ring, padRatio) {
        var lngMin = Infinity, lngMax = -Infinity, latMin = Infinity, latMax = -Infinity;
        ring.forEach(function (c) {
            if (c[0] < lngMin) lngMin = c[0];
            if (c[0] > lngMax) lngMax = c[0];
            if (c[1] < latMin) latMin = c[1];
            if (c[1] > latMax) latMax = c[1];
        });
        var pad = padRatio || 0;
        var dlng = (lngMax - lngMin) * pad;
        var dlat = (latMax - latMin) * pad;
        return [[lngMin - dlng, latMin - dlat], [lngMax + dlng, latMax + dlat]];
    }

    function zoneCenter(ring) {
        var b = zoneBounds(ring, 0);
        return [(b[0][0] + b[1][0]) / 2, (b[0][1] + b[1][1]) / 2];
    }

    /* ------------------------------------------------------------------ *
     * Slider
     * ------------------------------------------------------------------ */
    var SWAP_EXT = ["png", "jpg", "jpeg", "webp", "avif", "svg"];

    function resolveSlideImage(img) {
        var base = img.getAttribute("data-rs-base");
        if (!base) return;
        var current = (img.getAttribute("src") || "").split(".").pop();
        var i = 0;
        (function next() {
            if (i >= SWAP_EXT.length) return;
            var ext = SWAP_EXT[i++];
            if (ext === current) { next(); return; }
            var probe = new Image();
            probe.onload = function () { img.src = base + "." + ext; };
            probe.onerror = next;
            probe.src = base + "." + ext;
        })();
    }

    function initSlider(slider) {
        if (slider.dataset.rsReady === "1") return;
        var slides = Array.prototype.slice.call(slider.querySelectorAll("[data-rs-slide]"));
        if (!slides.length) return;
        slider.dataset.rsReady = "1";

        slides.forEach(function (s) {
            var img = s.querySelector(".rs-slide-img");
            if (img) resolveSlideImage(img);
        });

        var dotsWrap = slider.querySelector(".rs-slider-dots");
        var index = slides.findIndex(function (s) { return s.classList.contains("is-active"); });
        if (index < 0) index = 0;
        var timer = null;
        var paused = false;

        var dots = slides.map(function (_, i) {
            var b = document.createElement("button");
            b.type = "button";
            b.className = "rs-dot";
            b.setAttribute("aria-label", "Ir a la imagen " + (i + 1));
            b.addEventListener("click", function () { go(i, true); });
            if (dotsWrap) dotsWrap.appendChild(b);
            return b;
        });

        function render() {
            slides.forEach(function (s, i) { s.classList.toggle("is-active", i === index); });
            dots.forEach(function (d, i) { d.classList.toggle("is-active", i === index); });
        }

        function go(i, user) {
            index = (i + slides.length) % slides.length;
            render();
            if (user) restart();
        }

        function next() { go(index + 1, false); }

        function restart() {
            if (timer) clearTimeout(timer);
            if (paused) return;
            timer = setTimeout(function () { next(); restart(); }, 5500);
        }

        var nextBtn = slider.querySelector(".rs-next");
        var prevBtn = slider.querySelector(".rs-prev");
        if (nextBtn) nextBtn.addEventListener("click", function () { go(index + 1, true); });
        if (prevBtn) prevBtn.addEventListener("click", function () { go(index - 1, true); });

        slider.addEventListener("mouseenter", function () { paused = true; if (timer) clearTimeout(timer); });
        slider.addEventListener("mouseleave", function () { paused = false; restart(); });
        slider.addEventListener("focusin", function () { paused = true; if (timer) clearTimeout(timer); });
        slider.addEventListener("focusout", function () { paused = false; restart(); });
        document.addEventListener("visibilitychange", function () {
            if (document.hidden) { if (timer) clearTimeout(timer); }
            else { restart(); }
        });

        slider.addEventListener("keydown", function (e) {
            if (e.key === "ArrowRight") go(index + 1, true);
            if (e.key === "ArrowLeft") go(index - 1, true);
        });

        render();
        restart();
    }

    /* ------------------------------------------------------------------ *
     * Mapa — configuración según la especificación del mapa de /buscar
     * ------------------------------------------------------------------ */
    var MAPLIBRE_MODULE = "/maplibre/maplibre-gl.js";
    var MAPLIBRE_WORKER = "/maplibre/maplibre-gl-worker.js";

    var STYLE_STREET = "https://tiles.openfreemap.org/styles/liberty";
    var STYLE_DARK = "https://tiles.openfreemap.org/styles/dark";

    var STYLE_SATELLITE = {
        version: 8,
        sources: {
            satellite: {
                type: "raster",
                tiles: ["https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"],
                tileSize: 256,
                maxzoom: 19,
                attribution: "Esri · Maxar · Earthstar Geographics"
            }
        },
        layers: [{ id: "satellite", type: "raster", source: "satellite" }]
    };

    var STYLE_OSM_FALLBACK = {
        version: 8,
        sources: {
            osm: {
                type: "raster",
                tiles: ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
                tileSize: 256,
                maxzoom: 19,
                attribution: "© OpenStreetMap"
            }
        },
        layers: [{ id: "osm", type: "raster", source: "osm" }]
    };

    function buildStyle(layer) {
        if (layer === "satellite") return STYLE_SATELLITE;
        if (layer === "dark") return STYLE_DARK;
        return STYLE_STREET;
    }

    /* ROAD_RECOLORS: repinta vías de Liberty a blanco/gris */
    function isRoadLayer(id) {
        if (!/^(road|bridge|tunnel)_/.test(id)) return false;
        if (id.indexOf("rail") !== -1) return false;
        if (id.indexOf("hatching") !== -1) return false;
        if (id.indexOf("arrow") !== -1) return false;
        if (id.indexOf("pattern") !== -1) return false;
        if (id.indexOf("name") !== -1) return false;
        if (id.indexOf("shield") !== -1) return false;
        if (id.indexOf("path") !== -1) return false;
        return true;
    }

    function applyRoadRecolors(map, layer) {
        if (layer !== "street") return;
        var style = map.getStyle();
        if (!style || !style.layers) return;
        style.layers.forEach(function (l) {
            if (l.type !== "line" || !isRoadLayer(l.id)) return;
            try {
                map.setPaintProperty(l.id, "line-color", l.id.indexOf("casing") !== -1 ? "#d7dbe0" : "#ffffff");
            } catch (e) { /* capa sin line-color */ }
        });
    }

    function applyPitch(map) {
        var target = map.getZoom() >= 18 ? 55 : 0;
        if (Math.abs(map.getPitch() - target) > 0.01) {
            map.easeTo({ pitch: target, duration: 300 });
        }
    }

    function addZoneOverlay(map) {
        if (map.getSource("rs-zone")) return;
        map.addSource("rs-zone", {
            type: "geojson",
            data: {
                type: "Feature",
                properties: {},
                geometry: { type: "Polygon", coordinates: [ZONE_RING] }
            }
        });
        map.addLayer({
            id: "rs-zone-fill",
            type: "fill",
            source: "rs-zone",
            paint: { "fill-color": "#5b8dee", "fill-opacity": 0.06 }
        });
        map.addLayer({
            id: "rs-zone-line",
            type: "line",
            source: "rs-zone",
            paint: { "line-color": "#7ea6f5", "line-width": 1.2, "line-opacity": 0.5 }
        });
    }

    function showMapFallback(container) {
        if (container.querySelector(".rs-map-fallback")) return;
        var div = document.createElement("div");
        div.className = "rs-map-fallback";
        div.innerHTML = '<span class="rs-map-fallback-dot"></span><span>Edificio C · UNET — zona centrada</span>';
        container.appendChild(div);
    }

    var mapLoading = false;

    async function initMap() {
        var container = document.getElementById("rs-hero-map");
        if (!container || container.dataset.rsInit === "1") return;

        if (!window.__rsMaplibre) {
            if (mapLoading) return;
            mapLoading = true;
            try {
                window.__rsMaplibre = await import(MAPLIBRE_MODULE);
            } catch (e) {
                mapLoading = false;
                showMapFallback(container);
                return;
            }
            mapLoading = false;
        }

        var maplibregl = window.__rsMaplibre;
        if (!maplibregl || !maplibregl.Map) { showMapFallback(container); return; }
        container.dataset.rsInit = "1";

        if (typeof maplibregl.setWorkerUrl === "function") {
            maplibregl.setWorkerUrl(MAPLIBRE_WORKER);
        }

        var layer = "street";
        var center = zoneCenter(ZONE_RING);
        var bounds = zoneBounds(ZONE_RING, 0);
        var maxBounds = zoneBounds(ZONE_RING, 0.06);

        var map = new maplibregl.Map({
            container: container,
            style: buildStyle(layer),
            center: center,
            zoom: 16.5,
            pitch: 0,
            bearing: 0,
            maxZoom: 20,
            minZoom: 16,
            maxBounds: maxBounds,
            dragRotate: false,
            pitchWithRotate: false,
            touchPitch: false,
            renderWorldCopies: false,
            attributionControl: { compact: true }
        });

        map.addControl(new maplibregl.NavigationControl({ visualizePitch: true }), "bottom-right");
        map.addControl(new maplibregl.ScaleControl({ unit: "metric" }), "bottom-left");

        var fallbackDone = false;
        var fallbackTimer = null;
        var sawTile = false;

        function scheduleFallback() {
            fallbackDone = false;
            sawTile = false;
            if (fallbackTimer) clearTimeout(fallbackTimer);
            fallbackTimer = setTimeout(function () {
                if (!sawTile && !fallbackDone) {
                    fallbackDone = true;
                    map.setStyle(STYLE_OSM_FALLBACK);
                }
            }, 8000);
        }

        map.on("load", function () {
            applyPitch(map);
        });

        map.on("style.load", function () {
            map.fitBounds(bounds, { padding: 28, maxZoom: 18, duration: 0 });
            applyRoadRecolors(map, layer);
            applyPitch(map);
            addZoneOverlay(map);
        });

        map.on("zoom", function () { applyPitch(map); });

        map.on("sourcedata", function (e) {
            if (e.isSourceLoaded || e.sourceDataType === "metadata") sawTile = true;
        });

        map.on("error", function (e) {
            if (fallbackDone) return;
            var msg = e && e.error ? String(e.error.message || e.error) : "";
            if (/style|liberty|openfreemap/i.test(msg)) {
                fallbackDone = true;
                map.setStyle(STYLE_OSM_FALLBACK);
            }
        });

        scheduleFallback();

        var ro = new ResizeObserver(function () { map.resize(); });
        ro.observe(container);
    }

    /* ------------------------------------------------------------------ *
     * Auto-recarga en desarrollo (localhost)
     * Si cambia landing.html, landing.js u overrides.css, la página se
     * recarga sola. Desactivada fuera de localhost.
     * ------------------------------------------------------------------ */
    function initDevAutoReload() {
        var host = location.hostname;
        if (host !== "localhost" && host !== "127.0.0.1") return;
        if (window.__rsAutoReload) return;
        window.__rsAutoReload = true;

        var files = ["/authkit/landing.html", "/authkit/landing.js", "/authkit/overrides.css"];
        var seen = {};

        function fp(text) {
            return text.length + ":" + text.slice(0, 80) + ":" + text.slice(-80);
        }

        function tick() {
            files.forEach(function (url) {
                fetch(url + "?rs=" + Date.now(), { cache: "no-store" })
                    .then(function (r) { return r.ok ? r.text() : null; })
                    .then(function (t) {
                        if (t == null) return;
                        var v = fp(t);
                        if (seen[url] === undefined) seen[url] = v;
                        else if (seen[url] !== v) { seen[url] = v; location.reload(); }
                    })
                    .catch(function () {});
            });
        }

        tick();
        setInterval(tick, 1500);
    }

    /* ------------------------------------------------------------------ *
     * Arranque
     * ------------------------------------------------------------------ */
    function run() {
        initDevAutoReload();
        var slider = document.querySelector("[data-rs-slider]");
        if (slider) initSlider(slider);
        initMap();
    }

    var queued = false;
    function boot() {
        if (queued) return;
        queued = true;
        requestAnimationFrame(function () { queued = false; run(); });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", boot);
    } else {
        boot();
    }

    var observer = new MutationObserver(boot);
    observer.observe(document.documentElement, { childList: true, subtree: true });
})();
