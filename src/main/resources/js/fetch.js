(function(self) {
    'use strict';

    if (self.fetch) {
        return
    }

    var FetchOnAkka = Packages.js.FetchOnAkka$.MODULE$;

    self.fetch = function(url) {
        print("fetching");
        return FetchOnAkka.fetch(url);
    };

})(typeof self !== 'undefined' ? self : this);
