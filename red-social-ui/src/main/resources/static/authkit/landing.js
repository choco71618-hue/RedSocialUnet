/*
 * Landing pública — comportamiento de la portada.
 *
 * 1) Auto-recarga en desarrollo (localhost).
 * 2) Slider de videos del hero en ABANICO (fan): las cards se reparten en
 *    arco alrededor de la activa. Se muestra el preview (poster JPG) y el
 *    video (.mp4) NO se carga hasta tocar play (preload="none" + data-src).
 *    Se desplaza solo cada pocos segundos y se pausa al pasar el mouse.
 */
(function () {
    "use strict";

    /* ------------------------------------------------------------------ *
     * Auto-recarga en desarrollo (localhost)
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
     * Slider de videos del hero (abanico)
     * ------------------------------------------------------------------ */
    function initClips() {
        var root = document.querySelector("[data-clips]");
        if (!root || root.__rsClipsReady) return false;

        var clips = Array.prototype.slice.call(root.querySelectorAll("[data-clip]"));
        if (!clips.length) return false;

        root.__rsClipsReady = true;

        var track = root.querySelector("[data-clips-track]");
        var dotsBox = root.querySelector("[data-clips-dots]");

        /* Duplicar los videos para que el abanico quede siempre lleno a ambos
           lados (con offset circular). */
        if (track) {
            clips.forEach(function (fig) {
                var clone = fig.cloneNode(true);
                clone.classList.remove("is-active", "is-playing", "is-near", "is-center");
                clone.removeAttribute("style");
                var v = clone.querySelector("video");
                if (v) { v.removeAttribute("src"); v.load(); }
                track.appendChild(clone);
            });
            clips = Array.prototype.slice.call(root.querySelectorAll("[data-clip]"));
        }

        var total = clips.length;
        var current = 0;
        var dotEls = [];

        var MAX_SIDE = 3;
        var SPACING = 104;
        var ANGLE = 7;
        var SCALE_STEP = 0.09;
        var ARC = 4;

        function videoOf(fig) {
            return fig.querySelector("video");
        }

        function loadClip(fig) {
            var video = videoOf(fig);
            if (!video) return null;
            if (!video.getAttribute("src")) {
                var src = fig.getAttribute("data-src");
                if (src) video.setAttribute("src", src);
            }
            return video;
        }

        function pauseOthers(fig) {
            clips.forEach(function (other) {
                if (other === fig) return;
                var v = videoOf(other);
                if (v && !v.paused) v.pause();
                other.classList.remove("is-playing");
            });
        }

        function play(fig) {
            pauseOthers(fig);
            var video = loadClip(fig);
            if (!video) return;
            var promise = video.play();
            if (promise && promise.catch) promise.catch(function () {});
        }

        function pause(fig) {
            var video = videoOf(fig);
            if (video) video.pause();
            fig.classList.remove("is-playing");
        }

        function anyPlaying() {
            return clips.some(function (fig) {
                var v = videoOf(fig);
                return v && !v.paused && !v.ended;
            });
        }

        function layout() {
            var half = Math.floor(total / 2);
            clips.forEach(function (fig, i) {
                var raw = i - current;
                var offset = ((raw + half) % total + total) % total - half;
                var abs = Math.abs(offset);
                if (abs > MAX_SIDE) {
                    fig.classList.remove("is-near", "is-center");
                    fig.style.transform = "translateX(-50%) translateY(320px) scale(0.6)";
                    fig.style.zIndex = "0";
                    fig.style.opacity = "0";
                    fig.style.pointerEvents = "none";
                    return;
                }
                var x = offset * SPACING;
                var rot = offset * ANGLE;
                var scale = 1.05 - abs * SCALE_STEP;
                var y = abs * abs * ARC;
                fig.style.transform =
                    "translateX(calc(-50% + " + x + "px)) translateY(" + y + "px) rotate(" +
                    rot + "deg) scale(" + scale + ")";
                fig.style.zIndex = String(40 - abs);
                fig.style.opacity = "1";
                fig.style.pointerEvents = "auto";
                fig.classList.add("is-near");
                fig.classList.toggle("is-center", offset === 0);
            });
        }

        function setCurrent(index) {
            current = ((index % clips.length) + clips.length) % clips.length;
            dotEls.forEach(function (dot, i) {
                dot.classList.toggle("is-active", i === current);
                dot.setAttribute("aria-selected", i === current ? "true" : "false");
            });
            layout();
        }

        function go(index) {
            setCurrent(index);
        }

        if (dotsBox) {
            clips.forEach(function (fig, i) {
                var dot = document.createElement("button");
                dot.type = "button";
                dot.className = "rs-clip-dot";
                dot.setAttribute("role", "tab");
                dot.setAttribute("aria-label", "Video " + (i + 1));
                dot.addEventListener("click", function () { go(i); });
                dotsBox.appendChild(dot);
                dotEls.push(dot);
            });
        }

        var prev = root.querySelector("[data-clips-prev]");
        var next = root.querySelector("[data-clips-next]");
        if (prev) prev.addEventListener("click", function () { go(current - 1); });
        if (next) next.addEventListener("click", function () { go(current + 1); });

        clips.forEach(function (fig, i) {
            var playBtn = fig.querySelector("[data-clip-play]");
            if (playBtn) {
                playBtn.addEventListener("click", function (e) {
                    e.stopPropagation();
                    go(i);
                    play(fig);
                });
            }
            var toggle = fig.querySelector("[data-clip-toggle]");
            if (toggle) {
                toggle.addEventListener("click", function (e) {
                    e.stopPropagation();
                    pause(fig);
                });
            }
            fig.addEventListener("click", function () {
                var video = videoOf(fig);
                go(i);
                if (video && !video.paused) pause(fig);
                else play(fig);
            });
            var video = videoOf(fig);
            if (video) {
                video.addEventListener("play", function () {
                    pauseOthers(fig);
                    fig.classList.add("is-playing");
                });
                video.addEventListener("pause", function () { fig.classList.remove("is-playing"); });
                video.addEventListener("ended", function () { fig.classList.remove("is-playing"); });
            }
        });

        var startX = null;
        root.addEventListener("touchstart", function (e) {
            startX = e.touches[0].clientX;
        }, { passive: true });
        root.addEventListener("touchend", function (e) {
            if (startX === null) return;
            var dx = e.changedTouches[0].clientX - startX;
            startX = null;
            if (Math.abs(dx) > 40) go(current + (dx < 0 ? 1 : -1));
        }, { passive: true });

        setCurrent(0);

        var hovering = false;
        var autoTimer = setInterval(function () {
            if (hovering || anyPlaying()) return;
            go(current + 1);
        }, 3200);
        root.addEventListener("mouseenter", function () { hovering = true; });
        root.addEventListener("mouseleave", function () { hovering = false; });
        root.addEventListener("focusin", function () { hovering = true; });
        root.addEventListener("focusout", function () { hovering = false; });

        if ("IntersectionObserver" in window) {
            var io = new IntersectionObserver(function (entries) {
                entries.forEach(function (entry) {
                    if (!entry.isIntersecting) {
                        clips.forEach(pause);
                    }
                });
            }, { threshold: 0.3 });
            io.observe(root);
        }

        window.addEventListener("resize", layout);

        return true;
    }

    /* ------------------------------------------------------------------ *
     * Sección "Servicios UNET": imagen izquierda + slider vertical derecho
     * ------------------------------------------------------------------ */
    function initServices() {
        var list = document.querySelector("[data-services-list]");
        var track = document.querySelector("[data-services-track]");
        if (!list || !track || list.__rsSvcReady) return false;

        var base = Array.prototype.slice.call(track.querySelectorAll("[data-service]"));
        var imgs = Array.prototype.slice.call(
            document.querySelectorAll("[data-services-media] .rs-service-img"));
        if (!base.length || !imgs.length) return false;

        list.__rsSvcReady = true;

        /* Duplicar la lista dos veces (3 copias) para loop infinito y para que
           el slider arranque lleno: la primera copia queda por encima de la
           card activa. */
        for (var s = 0; s < 2; s++) {
            base.forEach(function (card) {
                var clone = card.cloneNode(true);
                clone.classList.remove("is-active");
                track.appendChild(clone);
            });
        }
        var cards = Array.prototype.slice.call(track.querySelectorAll("[data-service]"));
        var N = base.length;
        var current = N;
        var hovering = false;
        var wrapTimer = null;

        function center(index, instant) {
            var card = cards[index];
            if (!card) return;
            var y = -(card.offsetTop + card.offsetHeight / 2);
            if (instant) {
                track.style.transition = "none";
                track.style.transform = "translateY(" + y + "px)";
                void track.offsetHeight;
                track.style.transition = "";
            } else {
                track.style.transform = "translateY(" + y + "px)";
            }
        }

        function applyActive() {
            cards.forEach(function (c, i) { c.classList.toggle("is-active", i === current); });
            var imgIndex = ((current % N) + N) % N;
            imgs.forEach(function (m, i) { m.classList.toggle("is-active", i === imgIndex); });
        }

        function normalize() {
            if (current >= 2 * N) current -= N;
            else if (current < N) current += N;
            else return;
            applyActive();
            center(current, true);
        }

        function select(index, instant) {
            current = index;
            applyActive();
            center(current, instant);
            clearTimeout(wrapTimer);
            if (!instant) wrapTimer = setTimeout(normalize, 700);
        }

        function isVisible() {
            var r = list.getBoundingClientRect();
            var vh = window.innerHeight || document.documentElement.clientHeight;
            return r.top < vh && r.bottom > 0;
        }

        cards.forEach(function (card, i) {
            card.addEventListener("click", function () { select(i); });
        });

        setInterval(function () {
            if (hovering || document.hidden) return;
            select(current + 1);
        }, 3600);

        list.addEventListener("mouseenter", function () { hovering = true; });
        list.addEventListener("mouseleave", function () { hovering = false; });
        window.addEventListener("resize", function () { center(current, true); });

        select(current, true);
        return true;
    }

    function run() {
        initDevAutoReload();
        var doneClips = false;
        var doneServices = false;

        function tick() {
            if (!doneClips) doneClips = initClips();
            if (!doneServices) doneServices = initServices();
            return doneClips && doneServices;
        }

        if (tick()) return;
        var tries = 0;
        var timer = setInterval(function () {
            if (tick() || ++tries > 40) clearInterval(timer);
        }, 150);
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", run);
    } else {
        run();
    }
})();
