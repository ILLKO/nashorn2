(function(self) {
    'use strict';

    if (self.fetch) {
        return
    }

    var FetchOnAkka = Packages.js.FetchOnAkka$.MODULE$;
    var FutureConverters = Packages.scala.compat.java8.FutureConverters$.MODULE$;

    self.fetch = function(url) {
        print("fetching");
            var f = FetchOnAkka.fetch(url)

            return FutureConverters.toJava(f);
    };

})(typeof self !== 'undefined' ? self : this);
