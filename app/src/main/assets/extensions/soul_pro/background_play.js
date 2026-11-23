// Background Play Fix: Override visibility and blur listeners that pause YouTube / media
(function() {
    try {
        // 1. Prevent pages from detecting when tab/app goes into background
        Object.defineProperty(document, 'hidden', { get: () => false, configurable: true });
        Object.defineProperty(document, 'visibilityState', { get: () => 'visible', configurable: true });
        Object.defineProperty(document, 'webkitVisibilityState', { get: () => 'visible', configurable: true });

        // Block visibilitychange event dispatching to page listeners
        const blockEvent = function(e) {
            if (e.type === 'visibilitychange' || e.type === 'webkitvisibilitychange') {
                e.stopImmediatePropagation();
            }
        };
        window.addEventListener('visibilitychange', blockEvent, true);
        document.addEventListener('visibilitychange', blockEvent, true);

        // 2. Prevent window blur from pausing media
        const origAddEventListener = EventTarget.prototype.addEventListener;
        EventTarget.prototype.addEventListener = function(type, listener, options) {
            if (type === 'blur' && this === window) {
                return;
            }
            if (type === 'visibilitychange' || type === 'webkitvisibilitychange') {
                const wrappedListener = function(event) {
                    // Swallow background pause calls
                };
                return origAddEventListener.call(this, type, wrappedListener, options);
            }
            return origAddEventListener.call(this, type, listener, options);
        };
    } catch(e) {}
})();
